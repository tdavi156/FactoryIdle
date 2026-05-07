package com.github.jacks.factoryIdle.ui.models

import com.github.jacks.factoryIdle.components.Building
import com.github.jacks.factoryIdle.components.BuildingGroup
import com.github.jacks.factoryIdle.components.FuelConsumer
import com.github.jacks.factoryIdle.components.Miner
import com.github.jacks.factoryIdle.components.Producer
import com.github.jacks.factoryIdle.components.ProductionSatisfaction
import com.github.jacks.factoryIdle.data.BuildingType
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.Recipe
import com.github.jacks.factoryIdle.data.RecipeRegistry
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.data.UnassignedPool
import com.github.jacks.factoryIdle.data.UnlockRegistry
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
    val assignedResource: Resource?,
    val hasFuelConsumer: Boolean,
    val isFuelStarved: Boolean,
    val resourceSatisfaction: Map<Resource, Float>,
    val paused: Boolean
)

class FactoryModel(
    private val world: World,
    private val pool: GlobalResourcePool,
    private val unlockRegistry: UnlockRegistry,
    private val unassignedPool: UnassignedPool,
    private val recipeRegistry: RecipeRegistry
) {
    private val buildingFamily = world.family { all(Building) }

    private var _buildMenuEntries: List<BuildMenuEntry> = emptyList()
    private var _placedBuildings: List<PlacedBuildingData> = emptyList()

    val buildMenuEntries: List<BuildMenuEntry> get() = _buildMenuEntries
    val placedBuildings: List<PlacedBuildingData> get() = _placedBuildings

    var selectedEntity: Entity? = null
        private set

    private val changeListeners = mutableListOf<() -> Unit>()

    fun onChanged(listener: () -> Unit) { changeListeners.add(listener) }

    fun update(delta: Float) {
        val newMenu = buildBuildMenu()
        val newBuildings = buildPlacedBuildings()

        if (newMenu != _buildMenuEntries || newBuildings != _placedBuildings) {
            _buildMenuEntries = newMenu
            _placedBuildings = newBuildings
            changeListeners.forEach { it() }
        }
    }

    fun selectBuilding(entity: Entity?) {
        selectedEntity = entity
        changeListeners.forEach { it() }
    }

    fun assignRecipe(entity: Entity, recipe: Recipe) {
        with(world) {
            if (!(entity has Producer)) return
            val producer = entity[Producer]
            producer.recipe = recipe
            producer.progress = 0f
            producer.groupState = GroupState.NO_RECIPE

            val sat = entity.getOrNull(ProductionSatisfaction) ?: return
            sat.declaredRates.clear()
            sat.fractionalAccumulator = 0f
            for ((resource, amount) in recipe.inputs) {
                sat.declaredRates[resource] = amount / recipe.duration
            }
        }
    }

    fun assignMinerResource(entity: Entity, resource: Resource) {
        with(world) {
            if (!(entity has Miner)) return
            val miner = entity[Miner]
            miner.assignedResource = resource
            miner.progress = 0f
            miner.groupState = GroupState.NO_RECIPE

            val sat = entity.getOrNull(ProductionSatisfaction) ?: return
            sat.declaredRates.clear()
            sat.fractionalAccumulator = 0f
        }
    }

    fun togglePause(entity: Entity) {
        with(world) {
            val group = entity.getOrNull(BuildingGroup) ?: return
            group.paused = !group.paused
            if (group.paused) {
                entity.getOrNull(ProductionSatisfaction)?.declaredRates?.clear()
            }
        }
        changeListeners.forEach { it() }
    }

    fun selectedEntityData(): PlacedBuildingData? =
        selectedEntity?.let { sel -> _placedBuildings.firstOrNull { it.entity == sel } }

    fun recipesFor(type: BuildingType): List<Recipe> = recipeRegistry.recipesFor(type)

    private fun buildBuildMenu(): List<BuildMenuEntry> =
        unlockRegistry.unlockedBuildingTypes().map { type ->
            val cost = recipeRegistry.constructionCostFor(type)
            BuildMenuEntry(
                type = type,
                cost = cost,
                canAfford = cost.all { (res, qty) -> pool.has(res, qty.toFloat()) },
                unassignedCount = unassignedPool.count(type)
            )
        }

    private fun buildPlacedBuildings(): List<PlacedBuildingData> {
        val result = mutableListOf<PlacedBuildingData>()
        with(world) {
            buildingFamily.forEach { entity ->
                val building = entity[Building]
                val producer = entity.getOrNull(Producer)
                val miner    = entity.getOrNull(Miner)
                val fuel     = entity.getOrNull(FuelConsumer)
                val sat      = entity.getOrNull(ProductionSatisfaction)
                val group    = entity.getOrNull(BuildingGroup)

                val rawState = producer?.groupState ?: miner?.groupState ?: GroupState.NO_RECIPE
                val paused   = group?.paused ?: false

                result.add(
                    PlacedBuildingData(
                        entity               = entity,
                        type                 = building.type,
                        groupState           = if (paused) GroupState.PAUSED else rawState,
                        currentSatisfaction  = sat?.currentSatisfaction ?: 0f,
                        recipe               = producer?.recipe,
                        assignedResource     = miner?.assignedResource,
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
}
