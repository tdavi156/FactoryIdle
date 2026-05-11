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
    val constructionQueue: List<ConstructionEntrySaveData>,
    val completedMilestones: Set<String>
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
data class ConstructionEntrySaveData(
    val type: String,
    val remainingTime: Float
)
