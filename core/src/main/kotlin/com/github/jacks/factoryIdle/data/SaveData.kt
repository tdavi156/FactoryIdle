package com.github.jacks.factoryIdle.data

import kotlinx.serialization.Serializable

@Serializable
data class SaveData(
    val version: Int = 1,
    val savedAt: Long = 0L,
    val globalPool: Map<String, Float>,
    val lifetimeStats: Map<String, Float>,
    val unlockedBuildings: List<String>,
    val unlockedResources: List<String>,
    val placedBuildings: List<PlacedBuildingSaveData>,
    val craftingQueue: List<CraftQueueEntryData> = emptyList(),
    val completedMilestones: Set<String>,
    val activeResearch: String? = null,
    val researchProgress: Float = 0f,
    val completedResearch: Set<String> = emptySet()
)

@Serializable
data class PlacedBuildingSaveData(
    val type: String,
    val assignedRecipe: String?,
    val cycleProgress: Float,
    val fractionalAccumulator: Float,
    val paused: Boolean
)

@Serializable
data class CraftQueueEntryData(
    val displayName: String,
    val iconKey: String,
    val remainingTime: Float,
    val totalTime: Float,
    val consumed: Map<String, Float>,
    val outputType: String,
    val outputKey: String,
    val outputAmount: Float = 1f
)
