package com.github.jacks.factoryIdle.ui.models

import com.badlogic.gdx.Gdx
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.data.ResourceCategory
import com.github.jacks.factoryIdle.data.UnlockRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class DisplayMode { COUNT_ONLY, RATE_ONLY, COUNT_RATE, COUNT_RATE_TTZ }

enum class ProblemLevel { HEALTHY, WARN, BAD }

@Serializable
enum class Density { COMFORTABLE, COMPACT }

@Serializable
data class ResourceDisplaySettings(
    val displayMode: DisplayMode = DisplayMode.COUNT_ONLY,
    val density: Density = Density.COMFORTABLE,
    val hiddenResources: MutableSet<String> = mutableSetOf()
)

private val settingsJson = Json { ignoreUnknownKeys = true }
private const val SETTINGS_FILE = "settings.json"

class ResourceBarModel(
    private val pool: GlobalResourcePool,
    private val lifetimeStats: LifetimeMiningStats,
    private val unlockRegistry: UnlockRegistry,
    private val hasActiveDemand: (Resource) -> Boolean = { false }
) {
    companion object {
        private const val MINING_CYCLE    = 2.0f
        private const val SAMPLE_INTERVAL = 1.0f
        private const val SAMPLE_COUNT    = 60
    }

    private var settings: ResourceDisplaySettings = loadSettings()

    val displayMode: DisplayMode get() = settings.displayMode
    val density: Density get() = settings.density

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
    private val updateListeners    = mutableListOf<() -> Unit>()
    private val structureListeners = mutableListOf<() -> Unit>()
    private val modeListeners      = mutableListOf<(DisplayMode) -> Unit>()
    private val collapseListeners  = mutableListOf<(ResourceCategory, Boolean) -> Unit>()

    fun onUpdate(listener: () -> Unit)                                            { updateListeners.add(listener) }
    fun onStructureChanged(listener: () -> Unit)                                  { structureListeners.add(listener) }
    fun onDisplayModeChanged(listener: (DisplayMode) -> Unit)                     { modeListeners.add(listener) }
    fun onCategoryCollapseChanged(listener: (ResourceCategory, Boolean) -> Unit)  { collapseListeners.add(listener) }

    private var prevUnlocked: Set<Resource> = emptySet()

    // --- Settings persistence ---

    private fun loadSettings(): ResourceDisplaySettings {
        val file = Gdx.files.local(SETTINGS_FILE)
        if (!file.exists()) return ResourceDisplaySettings()
        return try {
            settingsJson.decodeFromString<ResourceDisplaySettings>(file.readString())
        } catch (e: Exception) {
            ResourceDisplaySettings()
        }
    }

    private fun saveSettings() {
        try {
            Gdx.files.local(SETTINGS_FILE).writeString(settingsJson.encodeToString(settings), false)
        } catch (e: Exception) {
            Gdx.app.error("ResourceBarModel", "Settings save failed: ${e.message}")
        }
    }

    // --- Queries ---

    fun unlockedRawResources(): List<Resource> =
        unlockRegistry.unlockedResources().filter { it.category == ResourceCategory.RAW }

    fun visibleResources(): List<Resource> = Resource.values().filter { r ->
        if (!unlockRegistry.isUnlocked(r)) return@filter false
        val hasMass = when (r.category) {
            ResourceCategory.RAW -> true
            else -> pool.get(r) > 0f
        }
        if (!hasMass) return@filter false
        // Hidden flag is overridden when there is a problem (auto-promotion)
        if (r.name in settings.hiddenResources) {
            getProblemLevel(r) != ProblemLevel.HEALTHY
        } else {
            true
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

    fun getProblemLevel(resource: Resource): ProblemLevel {
        val amount = pool.get(resource)
        val rate = getRate(resource)          // per minute
        val ratePerSec = rate / 60f

        // Time-to-zero in seconds (only meaningful when draining)
        val ttz = if (ratePerSec < 0f) amount / (-ratePerSec) else Float.MAX_VALUE

        // BAD
        if (amount == 0f && hasActiveDemand(resource)) return ProblemLevel.BAD
        if (rate < -1.0f && ttz < 60f) return ProblemLevel.BAD

        // WARN
        if (rate < -0.05f && ttz in 60f..300f) return ProblemLevel.WARN
        if (amount < 10f && hasActiveDemand(resource)) return ProblemLevel.WARN

        return ProblemLevel.HEALTHY
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

    /**
     * Toggle continuous mining for a resource.
     * - If already mining this resource: stop it.
     * - Otherwise: stop any other active mining and start this one.
     */
    fun toggleMining(resource: Resource) {
        if (resource in miningProgress) {
            miningProgress.remove(resource)
        } else {
            miningProgress.clear()
            miningProgress[resource] = 0f
        }
    }

    fun toggleDisplayMode() {
        val next = when (settings.displayMode) {
            DisplayMode.COUNT_ONLY    -> DisplayMode.RATE_ONLY
            DisplayMode.RATE_ONLY     -> DisplayMode.COUNT_RATE
            DisplayMode.COUNT_RATE    -> DisplayMode.COUNT_RATE_TTZ
            DisplayMode.COUNT_RATE_TTZ -> DisplayMode.COUNT_ONLY
        }
        settings = settings.copy(displayMode = next)
        saveSettings()
        modeListeners.forEach { it(next) }
    }

    fun setDensity(density: Density) {
        if (settings.density == density) return
        settings = settings.copy(density = density)
        saveSettings()
        structureListeners.forEach { it() }
    }

    fun hideResource(resource: Resource) {
        settings.hiddenResources.add(resource.name)
        saveSettings()
        structureListeners.forEach { it() }
    }

    fun showResource(resource: Resource) {
        settings.hiddenResources.remove(resource.name)
        saveSettings()
        structureListeners.forEach { it() }
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
        for (resource in miningProgress.keys.toList()) {
            val next = (miningProgress[resource] ?: 0f) + delta
            if (next >= MINING_CYCLE) {
                pool.add(resource, 1f)
                lifetimeStats.add(resource, 1f)
                miningProgress[resource] = next - MINING_CYCLE  // carry remainder, keep cycling
            } else {
                miningProgress[resource] = next
            }
        }
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
