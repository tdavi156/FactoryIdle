package com.github.jacks.factoryIdle.data

data class ResearchGoal(
    val id: String,
    val name: String,
    val tier: Int,
    val cost: Map<Resource, Int>,
    val reward: () -> Unit
)
