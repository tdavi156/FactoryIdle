package com.github.jacks.factoryIdle.ui.models

import com.github.jacks.factoryIdle.components.BuildingComponent
import com.github.jacks.factoryIdle.components.BuildingGroupComponent
import com.github.jacks.factoryIdle.components.FuelConsumerComponent
import com.github.jacks.factoryIdle.components.ProducerComponent
import com.github.jacks.factoryIdle.components.ProductionSatisfactionComponent
import com.github.jacks.factoryIdle.data.BuildingType
import com.github.jacks.factoryIdle.data.CraftOutput
import com.github.jacks.factoryIdle.data.CraftQueueEntry
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.Recipe
import com.github.jacks.factoryIdle.data.RecipeRegistry
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.data.UnassignedPool
import com.github.jacks.factoryIdle.data.UnlockRegistry
import com.github.jacks.factoryIdle.ui.smallIconKey
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

data class BuildMenuEntry(
    val type: BuildingType,
    val cost: Map<Resource, Int>,
    val canAfford: Boolean,
    val unassignedCount: Int
)

data class PlacedBuildingData(
    val entity: Entity,
    val type: BuildingType,
    val groupState: GroupState,
    val currentSatisfaction: Float,
    val recipe: Recipe?,
    val hasFuelConsumer: Boolean,
    val isFuelStarved: Boolean,
    val resourceSatisfaction: Map<Resource, Float>,
    val paused: Boolean
)

data class QueueDisplayEntry(
    val type: BuildingType,
    val iconKey: String,
    val progress: Float,
    val remainingTime: Float
)

class FactoryModel(
    private val world: World,
    private val pool: GlobalResourcePool,
    private val unlockRegistry: UnlockRegistry,
    private val unassignedPool: UnassignedPool,
    private val recipeRegistry: RecipeRegistry,
    private val craftingModel: CraftingModel
) {
    private val buildingFamily = world.family { all(BuildingComponent) }

    private var _buildMenuEntries: List<BuildMenuEntry> = emptyList()
    private var _placedBuildings: List<PlacedBuildingData> = emptyList()
    private var _queueEntries: List<QueueDisplayEntry> = emptyList()

    val buildMenuEntries: List<BuildMenuEntry> get() = _buildMenuEntries
    val placedBuildings: List<PlacedBuildingData> get() = _placedBuildings
    val queueEntries: List<QueueDisplayEntry> get() = _queueEntries

    var selectedEntity: Entity? = null
        private set

    private val changeListeners = mutableListOf<() -> Unit>()

    fun onChanged(listener: () -> Unit) { changeListeners.add(listener) }

    fun update(delta: Float) {
        val newMenu      = buildBuildMenu()
        val newBuildings = buildPlacedBuildings()
        val newQueue     = buildQueueEntries()

        if (newMenu != _buildMenuEntries || newBuildings != _placedBuildings || newQueue != _queueEntries) {
            _buildMenuEntries = newMenu
            _placedBuildings  = newBuildings
            _queueEntries     = newQueue
            changeListeners.forEach { it() }
        }
    }

    fun selectBuilding(entity: Entity?) {
        selectedEntity = entity
        changeListeners.forEach { it() }
    }

    fun buildBuilding(entry: BuildMenuEntry) {
        if (!entry.canAfford) return
        val cost     = entry.cost.mapValues { (_, qty) -> qty.toFloat() }
        val duration = recipeRegistry.constructionTimeFor(entry.type)
        val craftEntry = CraftQueueEntry(
            displayName   = entry.type.displayName,
            iconKey       = entry.type.smallIconKey(),
            remainingTime = duration,
            totalTime     = duration,
            consumed      = cost,
            output        = CraftOutput.BuildingOutput(entry.type)
        )
        craftingModel.enqueue(craftEntry)
        changeListeners.forEach { it() }
    }

    fun assignRecipe(entity: Entity, recipe: Recipe) {
        with(world) {
            if (!(entity has ProducerComponent)) return
            val producer = entity[ProducerComponent]
            producer.recipe   = recipe
            producer.progress = 0f

            val sat = entity.getOrNull(ProductionSatisfactionComponent) ?: return
            sat.declaredRates.clear()
            sat.fractionalAccumulator = 0f

            entity.getOrNull(FuelConsumerComponent)?.let { fuel ->
                sat.declaredRates[fuel.fuelType] = fuel.consumeRate
            }
            for ((resource, amount) in recipe.inputs) {
                sat.declaredRates[resource] = amount / recipe.duration
            }
        }
    }

    fun togglePause(entity: Entity) {
        with(world) {
            val group = entity.getOrNull(BuildingGroupComponent) ?: return
            group.paused = !group.paused
            if (group.paused) {
                entity.getOrNull(ProductionSatisfactionComponent)?.declaredRates?.clear()
            }
        }
        changeListeners.forEach { it() }
    }

    fun selectedEntityData(): PlacedBuildingData? =
        selectedEntity?.let { sel -> _placedBuildings.firstOrNull { it.entity == sel } }

    fun recipesFor(type: BuildingType): List<Recipe> =
        recipeRegistry.recipesFor(type).filter { recipe ->
            recipe.inputs.all { (resource, _) -> unlockRegistry.isUnlocked(resource) }
        }

    private fun buildBuildMenu(): List<BuildMenuEntry> =
        unlockRegistry.unlockedBuildingTypes().map { type ->
            val cost = recipeRegistry.constructionCostFor(type)
            BuildMenuEntry(
                type            = type,
                cost            = cost,
                canAfford       = cost.all { (res, qty) -> pool.has(res, qty.toFloat()) },
                unassignedCount = unassignedPool.count(type)
            )
        }

    private fun buildPlacedBuildings(): List<PlacedBuildingData> {
        val result = mutableListOf<PlacedBuildingData>()
        with(world) {
            buildingFamily.forEach { entity ->
                val building = entity[BuildingComponent]
                val producer = entity.getOrNull(ProducerComponent)
                val fuel     = entity.getOrNull(FuelConsumerComponent)
                val sat      = entity.getOrNull(ProductionSatisfactionComponent)
                val group    = entity.getOrNull(BuildingGroupComponent)

                val rawState = producer?.groupState ?: GroupState.NO_RECIPE
                val paused   = group?.paused ?: false

                result.add(
                    PlacedBuildingData(
                        entity               = entity,
                        type                 = building.type,
                        groupState           = if (paused) GroupState.PAUSED else rawState,
                        currentSatisfaction  = sat?.currentSatisfaction ?: 0f,
                        recipe               = producer?.recipe,
                        hasFuelConsumer      = fuel != null,
                        isFuelStarved        = rawState == GroupState.FUEL_STARVED,
                        resourceSatisfaction = sat?.resourceSatisfaction?.toMap() ?: emptyMap(),
                        paused               = paused
                    )
                )
            }
        }
        return result
    }

    private fun buildQueueEntries(): List<QueueDisplayEntry> =
        craftingModel.playerCraftingQueue.entries
            .filter { it.output is CraftOutput.BuildingOutput }
            .map { entry ->
                val type = (entry.output as CraftOutput.BuildingOutput).type
                QueueDisplayEntry(
                    type          = type,
                    iconKey       = entry.iconKey,
                    progress      = 1f - (entry.remainingTime / entry.totalTime).coerceIn(0f, 1f),
                    remainingTime = entry.remainingTime.coerceAtLeast(0f)
                )
            }
}
