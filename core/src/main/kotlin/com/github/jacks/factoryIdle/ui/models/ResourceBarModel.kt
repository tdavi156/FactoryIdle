package com.github.jacks.factoryIdle.ui.models

import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.data.ResourceCategory
import com.github.jacks.factoryIdle.data.UnlockRegistry

enum class DisplayMode { COUNT, RATE }

class ResourceBarModel(
    private val pool: GlobalResourcePool,
    private val lifetimeStats: LifetimeMiningStats,
    private val unlockRegistry: UnlockRegistry
) {
    companion object {
        private const val MINING_CYCLE    = 2.0f
        private const val SAMPLE_INTERVAL = 1.0f
        private const val SAMPLE_COUNT    = 60
    }

    var displayMode: DisplayMode = DisplayMode.COUNT
        private set

    // Hand mining cycle progress per resource (absent = idle)
    private val miningProgress = mutableMapOf<Resource, Float>()

    // Player preference: which categories are collapsed
    private val collapsedCategories = mutableSetOf<ResourceCategory>()

    // Ring buffer for rate calculation — rateSamples[slot][resource] = pool amount
    private val rateSamples = Array(SAMPLE_COUNT) { mutableMapOf<Resource, Float>() }
    private var rateHead   = 0   // next write slot
    private var rateFilled = 0   // valid sample count (0..SAMPLE_COUNT)
    private var rateTimer  = 0f

    // Change listeners
    private val updateListeners   = mutableListOf<() -> Unit>()
    private val structureListeners = mutableListOf<() -> Unit>()
    private val modeListeners     = mutableListOf<(DisplayMode) -> Unit>()
    private val collapseListeners = mutableListOf<(ResourceCategory, Boolean) -> Unit>()

    fun onUpdate(listener: () -> Unit)                                            { updateListeners.add(listener) }
    fun onStructureChanged(listener: () -> Unit)                                  { structureListeners.add(listener) }
    fun onDisplayModeChanged(listener: (DisplayMode) -> Unit)                     { modeListeners.add(listener) }
    fun onCategoryCollapseChanged(listener: (ResourceCategory, Boolean) -> Unit)  { collapseListeners.add(listener) }

    private var prevUnlocked: Set<Resource> = emptySet()

    // --- Queries ---

    fun unlockedRawResources(): List<Resource> =
        unlockRegistry.unlockedResources().filter { it.category == ResourceCategory.RAW }

    fun visibleResources(): List<Resource> = Resource.values().filter { r ->
        if (!unlockRegistry.isUnlocked(r)) return@filter false
        when (r.category) {
            ResourceCategory.RAW -> true
            else -> pool.get(r) > 0f
        }
    }

    fun getAmount(resource: Resource): Float = pool.get(resource)

    /** Returns net change rate in units per minute over the rolling sample window. */
    fun getRate(resource: Resource): Float {
        if (rateFilled == 0) return 0f
        val oldestSlot = (rateHead - rateFilled + SAMPLE_COUNT) % SAMPLE_COUNT
        val current = pool.get(resource)
        val oldest  = rateSamples[oldestSlot].getOrDefault(resource, current)
        val elapsed = rateFilled * SAMPLE_INTERVAL
        return if (elapsed > 0f) (current - oldest) / elapsed * 60f else 0f
    }

    fun isHandMining(resource: Resource): Boolean = resource in miningProgress

    /** 0.0 = just started, 1.0 = cycle complete. */
    fun handMiningProgress(resource: Resource): Float =
        (miningProgress[resource] ?: 0f) / MINING_CYCLE

    fun isCategoryCollapsed(category: ResourceCategory): Boolean = category in collapsedCategories

    // --- Actions ---

    /** No-op if a cycle is already running for this resource. */
    fun startMining(resource: Resource) {
        if (resource !in miningProgress) miningProgress[resource] = 0f
    }

    fun toggleDisplayMode() {
        displayMode = if (displayMode == DisplayMode.COUNT) DisplayMode.RATE else DisplayMode.COUNT
        modeListeners.forEach { it(displayMode) }
    }

    fun toggleCategory(category: ResourceCategory) {
        val nowCollapsed = if (category in collapsedCategories) {
            collapsedCategories.remove(category); false
        } else {
            collapsedCategories.add(category); true
        }
        collapseListeners.forEach { it(category, nowCollapsed) }
    }

    // --- Per-frame update (called from GameScreen.render before stage.act) ---

    fun update(delta: Float) {
        advanceMining(delta)
        advanceRateSamples(delta)
        checkUnlockChanges()
        updateListeners.forEach { it() }
    }

    private fun advanceMining(delta: Float) {
        val done = mutableListOf<Resource>()
        for ((resource, progress) in miningProgress) {
            val next = progress + delta
            if (next >= MINING_CYCLE) {
                pool.add(resource, 1f)
                lifetimeStats.add(resource, 1f)
                done.add(resource)
            } else {
                miningProgress[resource] = next
            }
        }
        done.forEach { miningProgress.remove(it) }
    }

    private fun advanceRateSamples(delta: Float) {
        rateTimer += delta
        if (rateTimer >= SAMPLE_INTERVAL) {
            rateTimer -= SAMPLE_INTERVAL
            val snap = rateSamples[rateHead]
            snap.clear()
            Resource.values().forEach { snap[it] = pool.get(it) }
            rateHead = (rateHead + 1) % SAMPLE_COUNT
            if (rateFilled < SAMPLE_COUNT) rateFilled++
        }
    }

    private fun checkUnlockChanges() {
        val current = unlockRegistry.unlockedResources().toSet()
        if (current != prevUnlocked) {
            prevUnlocked = current
            structureListeners.forEach { it() }
        }
    }
}
