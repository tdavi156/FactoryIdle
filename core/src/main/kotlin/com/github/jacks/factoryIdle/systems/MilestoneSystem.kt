package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.data.Milestone
import com.github.quillraven.fleks.IntervalSystem

/**
 * Checks pending milestone conditions each tick and fires their rewards.
 *
 * Milestones read from LifetimeMiningStats (not current pool amounts) via the closures
 * captured in each Milestone's condition lambda. Once fired, a milestone is removed
 * from the pending list and never re-evaluated.
 *
 * Execution order: runs LAST — after all production systems have updated stats for this tick.
 */
class MilestoneSystem(milestones: List<Milestone>) : IntervalSystem() {

    private val pending: MutableList<Milestone> = milestones.toMutableList()

    override fun onTick() {
        // Iterate a snapshot to allow safe removal while checking
        val snapshot = pending.toList()
        for (milestone in snapshot) {
            if (milestone.condition()) {
                milestone.reward()
                pending.remove(milestone)
            }
        }
    }
}
