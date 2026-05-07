package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroupComponent
import com.github.jacks.factoryIdle.components.FuelConsumerComponent
import com.github.jacks.factoryIdle.components.ProducerComponent
import com.github.jacks.factoryIdle.components.ProductionSatisfactionComponent
import com.github.jacks.factoryIdle.data.GroupState
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family

class FuelSystem : IteratingSystem(
    family { all(FuelConsumerComponent, ProductionSatisfactionComponent) }
) {
    override fun onTickEntity(entity: Entity) {
        if (entity has BuildingGroupComponent && entity[BuildingGroupComponent].paused) return
        if (entity.getOrNull(ProducerComponent)?.groupState == GroupState.NO_RECIPE) return

        val fuel = entity[FuelConsumerComponent]
        val sat  = entity[ProductionSatisfactionComponent]

        val fuelSatisfaction = sat.resourceSatisfaction.getOrDefault(fuel.fuelType, 0f)

        if (fuelSatisfaction <= 0f) {
            entity.getOrNull(ProducerComponent)?.groupState = GroupState.FUEL_STARVED
        }
    }
}
