package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroupComponent
import com.github.jacks.factoryIdle.components.ProducerComponent
import com.github.jacks.factoryIdle.components.ProductionSatisfactionComponent
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import kotlin.math.floor

class ProductionSystem : IteratingSystem(
    family { all(ProducerComponent, ProductionSatisfactionComponent) }
) {
    private val globalPool    = world.inject<GlobalResourcePool>()
    private val lifetimeStats = world.inject<LifetimeMiningStats>()

    override fun onTickEntity(entity: Entity) {
        if (entity has BuildingGroupComponent && entity[BuildingGroupComponent].paused) return

        val producer = entity[ProducerComponent]
        val sat      = entity[ProductionSatisfactionComponent]
        val recipe   = producer.recipe

        if (recipe == null) {
            producer.groupState = GroupState.NO_RECIPE
            return
        }

        // Timer always runs regardless of satisfaction
        producer.progress += deltaTime

        if (producer.progress >= recipe.duration) {
            producer.progress = 0f

            // Accumulate fractional output for this cycle
            sat.fractionalAccumulator += sat.currentSatisfaction

            // Award whole cycles worth of output
            val whole = floor(sat.fractionalAccumulator).toInt()
            if (whole > 0) {
                for ((resource, amount) in recipe.outputs) {
                    val total = amount * whole
                    globalPool.add(resource, total)
                    lifetimeStats.add(resource, total)
                }
                sat.fractionalAccumulator -= whole.toFloat()
            }

            producer.groupState = if (sat.currentSatisfaction > 0f) GroupState.RUNNING else GroupState.STALLED
        }
    }
}
