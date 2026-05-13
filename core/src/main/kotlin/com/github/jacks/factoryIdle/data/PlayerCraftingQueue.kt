package com.github.jacks.factoryIdle.data

/** What a completed craft produces. */
sealed class CraftOutput {
    data class ResourceOutput(val resource: Resource, val amount: Float) : CraftOutput()
    data class BuildingOutput(val type: BuildingType) : CraftOutput()
}

data class CraftQueueEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val displayName: String,
    val iconKey: String,
    var remainingTime: Float,
    val totalTime: Float,
    val consumed: Map<Resource, Float>,
    val output: CraftOutput
)

class PlayerCraftingQueue {
    val entries: MutableList<CraftQueueEntry> = mutableListOf()
    val active: CraftQueueEntry? get() = entries.firstOrNull()

    /** Consume inputs and enqueue. Returns false if pool cannot satisfy inputs. */
    fun enqueue(entry: CraftQueueEntry, pool: GlobalResourcePool): Boolean {
        entry.consumed.forEach { (resource, amount) -> pool.remove(resource, amount) }
        entries.add(entry)
        return true
    }

    /** Cancel active craft and refund consumed inputs. */
    fun cancelActive(pool: GlobalResourcePool) {
        val entry = entries.removeFirstOrNull() ?: return
        entry.consumed.forEach { (resource, amount) -> pool.add(resource, amount) }
    }

    /** Cancel a queued (non-active) entry at the given index and refund inputs. */
    fun cancelQueued(index: Int, pool: GlobalResourcePool) {
        if (index < 1 || index >= entries.size) return
        val entry = entries.removeAt(index)
        entry.consumed.forEach { (resource, amount) -> pool.add(resource, amount) }
    }

    /**
     * Advance the active entry by delta seconds.
     * Returns the completed entry if one finishes this frame, null otherwise.
     */
    fun advance(delta: Float): CraftQueueEntry? {
        val entry = active ?: return null
        entry.remainingTime -= delta
        return if (entry.remainingTime <= 0f) entries.removeFirst() else null
    }
}
