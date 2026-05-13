package com.github.jacks.factoryIdle.data

class ResearchManager(private val unlockRegistry: UnlockRegistry) {

    var activeGoal: ResearchGoal? = null
        private set
    var progress: Float = 0f
        private set
    val completedIds: MutableSet<String> = mutableSetOf()
    val allGoals: List<ResearchGoal> = buildAllResearch(unlockRegistry)

    private val changeListeners = mutableListOf<() -> Unit>()

    fun onChanged(listener: () -> Unit) { changeListeners.add(listener) }

    fun setActive(goal: ResearchGoal) {
        if (isCompleted(goal.id)) return
        activeGoal = goal
        progress = 0f
        changeListeners.forEach { it() }
    }

    fun addProgress(amount: Float) {
        val goal = activeGoal ?: return
        progress += amount
        val totalRequired = goal.cost.values.first().toFloat()
        if (progress >= totalRequired) {
            goal.reward()
            completedIds.add(goal.id)
            activeGoal = null
            progress = 0f
            changeListeners.forEach { it() }
        }
    }

    fun isCompleted(id: String) = id in completedIds

    fun isAvailable(goal: ResearchGoal): Boolean {
        if (goal.tier == 1) return true
        return allGoals.filter { it.tier == goal.tier - 1 }.all { isCompleted(it.id) }
    }

    fun progressFraction(): Float {
        val goal = activeGoal ?: return 0f
        val total = goal.cost.values.first().toFloat()
        return (progress / total).coerceIn(0f, 1f)
    }

    fun restoreState(
        completedResearch: Set<String>,
        activeResearchId: String?,
        researchProgress: Float
    ) {
        completedIds.clear()
        completedIds.addAll(completedResearch)
        activeGoal = activeResearchId?.let { id -> allGoals.find { it.id == id } }
        progress = researchProgress
    }
}
