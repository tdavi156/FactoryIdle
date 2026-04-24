package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroup
import com.github.jacks.factoryIdle.components.ResourceBuffer
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family

/**
 * Pulls resources from the global pool into each entity's local ResourceBuffer each tick.
 *
 * The buffer is anti-jitter only — it holds 3 cycles of inputs so that tick-boundary
 * timing never starves a building that theoretically has enough in the global pool.
 *
 * Execution order: runs FIRST each tick so buffers are fresh before ProductionSystem reads them.
 *
 * Phase 2 note: replace the default EMPTY_COMPARATOR constructor arg with a compareEntity { }
 * block that sorts descending by BuildingGroup.priority (HIGHEST first). No other logic changes.
 */
class BufferFillSystem : IteratingSystem(
    family { all(ResourceBuffer) }
) {
    private val globalPool = world.inject<GlobalResourcePool>()

    override fun onTickEntity(entity: Entity) {
        // Skip paused groups (Phase 2 — BuildingGroup not present in Phase 1)
        if (entity has BuildingGroup && entity[BuildingGroup].paused) return

        val buffer = entity[ResourceBuffer]
        for ((resource, capacity) in buffer.capacity) {
            val current = buffer.contents.getOrDefault(resource, 0f)
            val shortage = capacity - current
            if (shortage <= 0f) continue

            val available = globalPool.get(resource)
            if (available <= 0f) continue

            val transfer = minOf(shortage, available)
            globalPool.subtract(resource, transfer)
            buffer.contents[resource] = current + transfer
        }
    }
}
