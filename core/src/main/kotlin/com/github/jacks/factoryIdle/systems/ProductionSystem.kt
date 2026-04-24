package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroup
import com.github.jacks.factoryIdle.components.Producer
import com.github.jacks.factoryIdle.components.ResourceBuffer
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family

/**
 * Advances producer progress each tick and completes production cycles.
 *
 * Reads inputs from the local ResourceBuffer only — never directly from the global pool.
 * Writes outputs to the global pool on cycle completion.
 *
 * Execution order: runs AFTER BufferFillSystem (buffers must be fresh) and BEFORE FuelSystem
 * (FuelSystem may override STALLED with FUEL_STARVED as the more specific diagnosis).
 */
class ProductionSystem : IteratingSystem(
    family { all(Producer, ResourceBuffer) }
) {
    private val globalPool = world.inject<GlobalResourcePool>()
    private val lifetimeStats = world.inject<LifetimeMiningStats>()

    override fun onTickEntity(entity: Entity) {
        // Skip paused groups (Phase 2)
        if (entity has BuildingGroup && entity[BuildingGroup].paused) return

        val producer = entity[Producer]
        val buffer = entity[ResourceBuffer]
        val recipe = producer.recipe

        if (recipe == null) {
            producer.groupState = GroupState.NO_RECIPE
            return
        }

        // Check that the buffer holds enough of every input for one cycle
        for ((resource, required) in recipe.inputs) {
            val held = buffer.contents.getOrDefault(resource, 0f)
            if (held < required) {
                producer.groupState = GroupState.STALLED
                return
            }
        }

        producer.progress += deltaTime

        if (producer.progress >= recipe.duration) {
            // Consume inputs from the buffer
            for ((resource, required) in recipe.inputs) {
                buffer.contents[resource] = buffer.contents.getOrDefault(resource, 0f) - required
            }

            // Write outputs to the global pool and lifetime stats
            for ((resource, amount) in recipe.outputs) {
                globalPool.add(resource, amount)
                lifetimeStats.add(resource, amount)
            }

            producer.progress = 0f
            producer.groupState = GroupState.RUNNING
        }
    }
}
