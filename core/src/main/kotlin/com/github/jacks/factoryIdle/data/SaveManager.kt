package com.github.jacks.factoryIdle.data

import com.badlogic.gdx.Gdx
import com.github.jacks.factoryIdle.components.BuildingComponent
import com.github.jacks.factoryIdle.components.FuelConsumerComponent
import com.github.jacks.factoryIdle.components.ProducerComponent
import com.github.jacks.factoryIdle.components.ProductionSatisfactionComponent
import com.github.jacks.factoryIdle.screens.GameScreen
import com.github.jacks.factoryIdle.systems.MilestoneSystem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val SAVE_FILE = "save.json"
private const val OFFLINE_CAP_SECONDS = 28_800.0  // 8 hours

object SaveManager {

    fun save(screen: GameScreen) {
        try {
            val data = buildSaveData(screen)
            Gdx.files.local(SAVE_FILE).writeString(json.encodeToString(data), false)
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Save failed: ${e.message}")
        }
    }

    fun load(): SaveData? {
        val file = Gdx.files.local(SAVE_FILE)
        if (!file.exists()) return null
        return try {
            json.decodeFromString<SaveData>(file.readString())
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Load failed: ${e.message}")
            null
        }
    }

    fun applyLoad(data: SaveData, screen: GameScreen) {
        restorePool(data, screen)
        restoreLifetimeStats(data, screen)
        restoreUnlocks(data, screen)
        restoreEntities(data, screen)
        restoreConstructionQueue(data, screen)
        restoreMilestones(data, screen)
    }

    /** Computes offline gain per resource from placed building recipes, capped at 8 hours. */
    fun computeOfflineGains(data: SaveData): Map<Resource, Float> {
        if (data.savedAt == 0L) return emptyMap()
        val elapsedSeconds = minOf(
            (System.currentTimeMillis() - data.savedAt) / 1000.0,
            OFFLINE_CAP_SECONDS
        ).toFloat()
        if (elapsedSeconds <= 0f) return emptyMap()

        val netRates = mutableMapOf<Resource, Float>()
        for (entry in data.placedBuildings) {
            val type = buildingTypeOrNull(entry.type) ?: continue
            val recipeId = entry.assignedRecipe ?: continue

            // Reconstruct recipe from registry to get rates
            val recipes = RecipeRegistry().recipesFor(type)
            val recipe = recipes.find { it.outputs.keys.firstOrNull()?.name == recipeId } ?: continue

            for ((resource, amount) in recipe.outputs) {
                netRates[resource] = (netRates[resource] ?: 0f) + (amount / recipe.duration)
            }
            for ((resource, amount) in recipe.inputs) {
                netRates[resource] = (netRates[resource] ?: 0f) - (amount / recipe.duration)
            }
        }

        return netRates
            .filter { (_, rate) -> rate > 0f }
            .mapValues { (_, rate) -> rate * elapsedSeconds }
    }

    fun applyOfflineGains(gains: Map<Resource, Float>, pool: GlobalResourcePool) {
        for ((resource, amount) in gains) {
            pool.add(resource, amount)
        }
    }

    fun formatOfflineDuration(savedAt: Long): String {
        val totalSeconds = minOf(
            (System.currentTimeMillis() - savedAt) / 1000L,
            OFFLINE_CAP_SECONDS.toLong()
        )
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0   -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else        -> "${seconds}s"
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun buildSaveData(screen: GameScreen): SaveData {
        val milestoneSystem = screen.entityWorld.system<MilestoneSystem>()

        val placedBuildings = mutableListOf<PlacedBuildingSaveData>()
        with(screen.entityWorld) {
            family { all(BuildingComponent) }.forEach { entity ->
                val building = entity[BuildingComponent]
                val producer = entity.getOrNull(ProducerComponent)
                val sat      = entity.getOrNull(ProductionSatisfactionComponent)
                val recipeId = producer?.recipe?.outputs?.keys?.firstOrNull()?.name

                placedBuildings.add(
                    PlacedBuildingSaveData(
                        type                 = building.type.name,
                        assignedRecipe       = recipeId,
                        cycleProgress        = producer?.progress ?: 0f,
                        fractionalAccumulator = sat?.fractionalAccumulator ?: 0f,
                        paused               = false
                    )
                )
            }
        }

        val queueEntries = screen.constructionQueue.entries.map { entry ->
            ConstructionEntrySaveData(
                type          = entry.type.name,
                remainingTime = entry.remainingTime
            )
        }

        return SaveData(
            version              = 1,
            savedAt              = System.currentTimeMillis(),
            globalPool           = screen.globalResourcePool.snapshot(),
            lifetimeStats        = screen.lifetimeMiningStats.snapshot(),
            unlockedBuildings    = screen.unlockRegistry.unlockedBuildingTypes().map { it.name },
            unlockedResources    = screen.unlockRegistry.unlockedResources().map { it.name },
            placedBuildings      = placedBuildings,
            constructionQueue    = queueEntries,
            completedMilestones  = milestoneSystem.completedMilestoneIds()
        )
    }

    private fun restorePool(data: SaveData, screen: GameScreen) {
        val map = data.globalPool.mapNotNull { (key, value) ->
            resourceOrNull(key)?.let { it to value }
        }.toMap()
        screen.globalResourcePool.restoreAll(map)
    }

    private fun restoreLifetimeStats(data: SaveData, screen: GameScreen) {
        for ((key, value) in data.lifetimeStats) {
            val resource = resourceOrNull(key) ?: continue
            screen.lifetimeMiningStats.set(resource, value)
        }
    }

    private fun restoreUnlocks(data: SaveData, screen: GameScreen) {
        for (name in data.unlockedResources) {
            val resource = resourceOrNull(name) ?: continue
            screen.unlockRegistry.unlock(resource)
        }
        for (name in data.unlockedBuildings) {
            val type = buildingTypeOrNull(name) ?: continue
            screen.unlockRegistry.unlock(type)
        }
    }

    private fun restoreEntities(data: SaveData, screen: GameScreen) {
        for (entry in data.placedBuildings) {
            val type = buildingTypeOrNull(entry.type) ?: continue
            val recipe = entry.assignedRecipe?.let { recipeId ->
                screen.recipeRegistry.recipesFor(type)
                    .find { it.outputs.keys.firstOrNull()?.name == recipeId }
            }
            screen.createBuildingEntity(
                type                  = type,
                recipe                = recipe,
                cycleProgress         = entry.cycleProgress,
                fractionalAccumulator = entry.fractionalAccumulator
            )
        }
    }

    private fun restoreConstructionQueue(data: SaveData, screen: GameScreen) {
        for (entry in data.constructionQueue) {
            val type = buildingTypeOrNull(entry.type) ?: continue
            val totalTime = screen.recipeRegistry.constructionTimeFor(type)
            screen.constructionQueue.restore(type, totalTime, entry.remainingTime)
        }
    }

    private fun restoreMilestones(data: SaveData, screen: GameScreen) {
        val milestoneSystem = screen.entityWorld.system<MilestoneSystem>()
        for (id in data.completedMilestones) {
            milestoneSystem.markCompleted(id)
        }
    }

    private fun resourceOrNull(name: String): Resource? =
        runCatching { Resource.valueOf(name) }.getOrNull()

    private fun buildingTypeOrNull(name: String): BuildingType? =
        runCatching { BuildingType.valueOf(name) }.getOrNull()
}
