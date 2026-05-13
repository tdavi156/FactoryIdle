package com.github.jacks.factoryIdle.data

data class Milestone(
    val id: String,
    val description: String,
    val condition: () -> Boolean,
    val reward: () -> Unit
)

fun buildPhase1Milestones(
    pool: GlobalResourcePool,
    stats: LifetimeMiningStats,
    unlocks: UnlockRegistry
): List<Milestone> = listOf(
    Milestone(
        id = "start",
        description = "Begin your factory",
        condition = { true },
        reward = { unlocks.unlock(Resource.IRON_ORE) }
    ),
    Milestone(
        id = "coal_unlock",
        description = "Mine 10 iron ore",
        condition = { stats.get(Resource.IRON_ORE) >= 10f },
        reward = { unlocks.unlock(Resource.COAL) }
    ),
    Milestone(
        id = "stone_unlock",
        description = "Mine 10 coal and 20 iron ore",
        condition = { stats.get(Resource.COAL) >= 10f && stats.get(Resource.IRON_ORE) >= 20f },
        reward = {
            unlocks.unlock(Resource.STONE)
            unlocks.unlock(Resource.COPPER_ORE)
        }
    ),
    Milestone(
        id = "furnace_unlock",
        description = "Mine 30 iron ore, 20 coal, and 10 stone",
        condition = {
            stats.get(Resource.IRON_ORE) >= 30f &&
            stats.get(Resource.COAL) >= 20f &&
            stats.get(Resource.STONE) >= 10f
        },
        reward = {
            unlocks.unlock(BuildingType.STONE_FURNACE)
            unlocks.unlock(Resource.IRON_PLATE)
            unlocks.unlock(Resource.COPPER_PLATE)
        }
    ),
    Milestone(
        id = "miner_unlock",
        description = "Produce 10 iron plates",
        condition = { stats.get(Resource.IRON_PLATE) >= 10f },
        reward = {
            unlocks.unlock(BuildingType.BASIC_MINER)
            unlocks.unlock(Resource.IRON_GEAR)
            unlocks.unlock(Resource.COPPER_WIRE)
            unlocks.unlock(Resource.RED_SCIENCE)
            unlocks.unlock(BuildingType.RESEARCH_FACILITY)
        }
    )
)
