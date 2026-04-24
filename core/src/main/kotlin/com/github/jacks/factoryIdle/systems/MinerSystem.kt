package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroup
import com.github.jacks.factoryIdle.components.FuelConsumer
import com.github.jacks.factoryIdle.components.Miner
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family

/**
 * Advances miner progress each tick and deposits resources directly to the global pool.
 *
 * Miners have no recipe input buffer — they use a FuelConsumer component only.
 * Rate: 1 unit of the assigned RAW resource per 4 seconds.
 *
 * Execution order: runs AFTER ProductionSystem and BEFORE FuelSystem.
 * FuelSystem may override a STALLED state with FUEL_STARVED after this system runs.
 */
class MinerSystem : IteratingSystem(
    family { all(Miner) }
) {
    private val globalPool = world.inject<GlobalResourcePool>()
    private val lifetimeStats = world.inject<LifetimeMiningStats>()

    override fun onTickEntity(entity: Entity) {
        // Skip paused groups (Phase 2)
        if (entity has BuildingGroup && entity[BuildingGroup].paused) return

        val miner = entity[Miner]

        if (miner.assignedResource == null) {
            miner.groupState = GroupState.NO_RECIPE
            return
        }

        // If this miner has a fuel component and the fuel buffer is empty, it cannot run
        val fuel = entity.getOrNull(FuelConsumer)
        if (fuel != null && fuel.fuelBuffer <= 0f) {
            miner.groupState = GroupState.FUEL_STARVED
            return
        }

        miner.progress += deltaTime

        if (miner.progress >= 4f) {
            val resource = miner.assignedResource!!
            globalPool.add(resource, 1f)
            lifetimeStats.add(resource, 1f)
            miner.progress = 0f
            miner.groupState = GroupState.RUNNING
        }
    }
}
