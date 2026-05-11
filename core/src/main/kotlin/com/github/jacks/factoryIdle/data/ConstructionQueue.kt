package com.github.jacks.factoryIdle.data

data class ConstructionEntry(
    val type: BuildingType,
    val totalTime: Float,
    var remainingTime: Float
)

class ConstructionQueue {
    val entries: MutableList<ConstructionEntry> = mutableListOf()

    fun enqueue(type: BuildingType, duration: Float) {
        entries.add(ConstructionEntry(type, duration, duration))
    }

    val active: ConstructionEntry? get() = entries.firstOrNull()

    fun advance(delta: Float): ConstructionEntry? {
        val entry = active ?: return null
        entry.remainingTime -= delta
        return if (entry.remainingTime <= 0f) { entries.removeFirst(); entry } else null
    }

    /** Restores a saved entry with its original total time and remaining time. Used on load. */
    fun restore(type: BuildingType, totalTime: Float, remainingTime: Float) {
        entries.add(ConstructionEntry(type, totalTime, remainingTime))
    }
}
