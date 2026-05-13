package com.github.jacks.factoryIdle.ui.models

import com.github.jacks.factoryIdle.data.ResearchGoal
import com.github.jacks.factoryIdle.data.ResearchManager

class ResearchModel(
    private val researchManager: ResearchManager,
    private val facilityCount: () -> Int
) {
    private val changeListeners = mutableListOf<() -> Unit>()
    private val updateListeners = mutableListOf<() -> Unit>()

    val activeGoal: ResearchGoal? get() = researchManager.activeGoal
    val progressFraction: Float get() = researchManager.progressFraction()

    init {
        researchManager.onChanged { changeListeners.forEach { it() } }
    }

    fun onChanged(listener: () -> Unit) { changeListeners.add(listener) }
    fun onUpdate(listener: () -> Unit)  { updateListeners.add(listener) }

    fun isCompleted(id: String)            = researchManager.isCompleted(id)
    fun isAvailable(goal: ResearchGoal)    = researchManager.isAvailable(goal)
    fun setActive(goal: ResearchGoal)      = researchManager.setActive(goal)
    fun goalsForTier(tier: Int)            = researchManager.allGoals.filter { it.tier == tier }
    fun activeFacilityCount(): Int         = facilityCount()

    fun update() { updateListeners.forEach { it() } }
}
