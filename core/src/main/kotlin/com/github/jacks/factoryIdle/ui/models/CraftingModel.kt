package com.github.jacks.factoryIdle.ui.models

import com.github.jacks.factoryIdle.data.BuildingType
import com.github.jacks.factoryIdle.data.CraftOutput
import com.github.jacks.factoryIdle.data.CraftQueueEntry
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.PlayerCraftingQueue
import com.github.jacks.factoryIdle.data.Recipe
import com.github.jacks.factoryIdle.data.RecipeRegistry
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.data.ResourceCategory
import com.github.jacks.factoryIdle.data.UnlockRegistry
import com.github.jacks.factoryIdle.ui.smallIconKey

data class RecipeDisplayItem(
    val recipe: Recipe,
    val displayName: String,
    val iconKey: String,
    val inputSummary: List<Pair<String, Float>>,
    val outputSummary: List<Pair<String, Float>>,
    val durationSeconds: Float,
    val canAfford: Boolean,
    val craftOutput: CraftOutput
)

class CraftingModel(
    val playerCraftingQueue: PlayerCraftingQueue,
    private val recipeRegistry: RecipeRegistry,
    private val unlockRegistry: UnlockRegistry,
    private val pool: GlobalResourcePool,
    private val resourceBarModel: ResourceBarModel
) {
    private val queueChangedListeners = mutableListOf<() -> Unit>()
    private val updateListeners       = mutableListOf<() -> Unit>()

    fun onQueueChanged(listener: () -> Unit) { queueChangedListeners.add(listener) }
    fun onUpdate(listener: () -> Unit)       { updateListeners.add(listener) }

    fun update() {
        updateListeners.forEach { it() }
    }

    // ── Recipe lists ──────────────────────────────────────────────────────────

    fun getBuildingRecipes(): List<RecipeDisplayItem> =
        unlockRegistry.unlockedBuildingTypes().map { type ->
            val cost     = recipeRegistry.constructionCostFor(type)
            val duration = recipeRegistry.constructionTimeFor(type)
            val inputs   = cost.mapValues { (_, qty) -> qty.toFloat() }
            val canAfford = cost.all { (res, qty) -> pool.has(res, qty.toFloat()) }
            RecipeDisplayItem(
                recipe        = Recipe(inputs = inputs, outputs = emptyMap(), duration = duration),
                displayName   = type.displayName,
                iconKey       = type.smallIconKey(),
                inputSummary  = cost.map { (res, qty) -> res.smallIconKey() to qty.toFloat() },
                outputSummary = listOf(type.smallIconKey() to 1f),
                durationSeconds = duration,
                canAfford     = canAfford,
                craftOutput   = CraftOutput.BuildingOutput(type)
            )
        }

    fun getIntermediateRecipes(): List<RecipeDisplayItem> {
        val result = mutableListOf<RecipeDisplayItem>()
        for (resource in Resource.values()) {
            if (resource.category != ResourceCategory.COMPONENT) continue
            if (!unlockRegistry.isUnlocked(resource)) continue
            // Intermediate recipes are keyed by a pseudo building type; skip for now
            // (no COMPONENT resources exist in Phase 1)
        }
        return result
    }

    // ── Enqueue ───────────────────────────────────────────────────────────────

    /** Enqueue from CraftingView (via RecipeDisplayItem). */
    fun enqueue(item: RecipeDisplayItem) {
        if (!item.canAfford) return
        val entry = CraftQueueEntry(
            displayName   = item.displayName,
            iconKey       = item.iconKey,
            remainingTime = item.durationSeconds,
            totalTime     = item.durationSeconds,
            consumed      = item.recipe.inputs,
            output        = item.craftOutput
        )
        playerCraftingQueue.enqueue(entry, pool)
        queueChangedListeners.forEach { it() }
    }

    /** Enqueue from FactoryModel (pre-built entry, resources already checked). */
    fun enqueue(entry: CraftQueueEntry) {
        playerCraftingQueue.enqueue(entry, pool)
        queueChangedListeners.forEach { it() }
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    fun cancelActive() {
        playerCraftingQueue.cancelActive(pool)
        queueChangedListeners.forEach { it() }
    }

    fun cancelQueued(index: Int) {
        playerCraftingQueue.cancelQueued(index, pool)
        queueChangedListeners.forEach { it() }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    fun activeEntry(): CraftQueueEntry? = playerCraftingQueue.active

    fun queuedEntries(): List<CraftQueueEntry> =
        if (playerCraftingQueue.entries.size > 1) playerCraftingQueue.entries.drop(1) else emptyList()
}
