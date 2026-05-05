package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroup
import com.github.jacks.factoryIdle.components.Miner
import com.github.jacks.factoryIdle.components.ProductionSatisfaction
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import kotlin.math.floor

class MinerSystem : IteratingSystem(
    family { all(Miner, ProductionSatisfaction) }
) {
    private val globalPool = world.inject<GlobalResourcePool>()
    private val lifetimeStats = world.inject<LifetimeMiningStats>()

    companion object {
        private const val CYCLE_DURATION = 4f
    }

    override fun onTickEntity(entity: Entity) {
        if (entity has BuildingGroup && entity[BuildingGroup].paused) return

        val miner = entity[Miner]
        val sat = entity[ProductionSatisfaction]

        if (miner.assignedResource == null) {
            miner.groupState = GroupState.NO_RECIPE
            return
        }

        // Miners have no recipe inputs; satisfaction = fuel satisfaction (set by PoolTickSystem)
        miner.progress += deltaTime

        if (miner.progress >= CYCLE_DURATION) {
            miner.progress = 0f

            sat.fractionalAccumulator += sat.currentSatisfaction

            val whole = floor(sat.fractionalAccumulator).toInt()
            if (whole > 0) {
                val resource = miner.assignedResource!!
                globalPool.add(resource, whole.toFloat())
                lifetimeStats.add(resource, whole.toFloat())
                sat.fractionalAccumulator -= whole.toFloat()
            }

            miner.groupState = if (sat.currentSatisfaction > 0f) GroupState.RUNNING else GroupState.STALLED
        }
    }
}
