package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroup
import com.github.jacks.factoryIdle.components.FuelConsumer
import com.github.jacks.factoryIdle.components.Miner
import com.github.jacks.factoryIdle.components.Producer
import com.github.jacks.factoryIdle.components.ProductionSatisfaction
import com.github.jacks.factoryIdle.data.GroupState
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family

class FuelSystem : IteratingSystem(
    family { all(FuelConsumer, ProductionSatisfaction) }
) {
    override fun onTickEntity(entity: Entity) {
        if (entity has BuildingGroup && entity[BuildingGroup].paused) return

        val fuel = entity[FuelConsumer]
        val sat = entity[ProductionSatisfaction]

        val fuelSatisfaction = sat.resourceSatisfaction.getOrDefault(fuel.fuelType, 0f)

        if (fuelSatisfaction <= 0f) {
            entity.getOrNull(Producer)?.groupState = GroupState.FUEL_STARVED
            entity.getOrNull(Miner)?.groupState = GroupState.FUEL_STARVED
        }
    }
}
