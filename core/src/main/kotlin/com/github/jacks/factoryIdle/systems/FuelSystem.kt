package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroup
import com.github.jacks.factoryIdle.components.FuelConsumer
import com.github.jacks.factoryIdle.components.Miner
import com.github.jacks.factoryIdle.components.Producer
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family

/**
 * Drains fuel buffers over time and tops them up from the global pool.
 *
 * Fuel is a composable concern separate from recipe inputs. Both Miners and Furnaces share
 * this system via the FuelConsumer component.
 *
 * Execution order: runs LAST among the production systems. Its FUEL_STARVED state overrides
 * any STALLED state set by ProductionSystem or MinerSystem — FUEL_STARVED is the more
 * specific diagnosis and takes precedence.
 *
 * Max fuel buffer is 6 units (enough for ~3 minutes at Stone Furnace rate of 1/30 per second).
 */
class FuelSystem : IteratingSystem(
    family { all(FuelConsumer) }
) {
    private val globalPool = world.inject<GlobalResourcePool>()

    companion object {
        private const val MAX_FUEL_BUFFER = 6f
    }

    override fun onTickEntity(entity: Entity) {
        // Skip paused groups (Phase 2)
        if (entity has BuildingGroup && entity[BuildingGroup].paused) return

        val fuel = entity[FuelConsumer]

        // Drain
        fuel.fuelBuffer -= fuel.consumeRate * deltaTime
        fuel.fuelBuffer = maxOf(0f, fuel.fuelBuffer)

        // Mark fuel-starved on any Producer or Miner on this entity
        if (fuel.fuelBuffer <= 0f) {
            entity.getOrNull(Producer)?.groupState = GroupState.FUEL_STARVED
            entity.getOrNull(Miner)?.groupState = GroupState.FUEL_STARVED
        }

        // Top up from global pool
        val needed = MAX_FUEL_BUFFER - fuel.fuelBuffer
        if (needed > 0f) {
            val available = globalPool.get(fuel.fuelType)
            if (available > 0f) {
                val transfer = minOf(needed, available)
                globalPool.subtract(fuel.fuelType, transfer)
                fuel.fuelBuffer += transfer
            }
        }
    }
}
