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
    private val completed: MutableSet<String> = mutableSetOf()

    override fun onTick() {
        val snapshot = pending.toList()
        for (milestone in snapshot) {
            if (milestone.condition()) {
                milestone.reward()
                completed.add(milestone.id)
                pending.remove(milestone)
            }
        }
    }

    fun completedMilestoneIds(): Set<String> = completed

    /** Marks a milestone as already completed without firing its reward — used on save load. */
    fun markCompleted(id: String) {
        val milestone = pending.find { it.id == id } ?: return
        completed.add(id)
        pending.remove(milestone)
    }
}
