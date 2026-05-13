package com.github.jacks.factoryIdle.data

fun buildAllResearch(unlockRegistry: UnlockRegistry): List<ResearchGoal> =
    tier1Goals(unlockRegistry) +
    tier2Goals() +
    tier3Goals() +
    tier4Goals() +
    tier5Goals() +
    tier6Goals()

private fun tier1Goals(unlockRegistry: UnlockRegistry): List<ResearchGoal> = listOf(
    ResearchGoal(
        id     = "assembler_mk1",
        name   = "Assembler Mk1",
        tier   = 1,
        cost   = mapOf(Resource.RED_SCIENCE to 10),
        reward = { unlockRegistry.unlock(BuildingType.ASSEMBLER_MK1) }
    ),
    ResearchGoal(
        id     = "miner_mk1",
        name   = "Miner Mk1",
        tier   = 1,
        cost   = mapOf(Resource.RED_SCIENCE to 10),
        reward = { unlockRegistry.unlock(BuildingType.MINER_MK1) }
    )
)

private fun tier2Goals(): List<ResearchGoal> = emptyList()
private fun tier3Goals(): List<ResearchGoal> = emptyList()
private fun tier4Goals(): List<ResearchGoal> = emptyList()
private fun tier5Goals(): List<ResearchGoal> = emptyList()
private fun tier6Goals(): List<ResearchGoal> = emptyList()
