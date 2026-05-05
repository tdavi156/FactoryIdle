package com.github.jacks.factoryIdle.data

enum class ResourceCategory { RAW, PROCESSED, COMPONENT, SCIENCE }

enum class Resource(val category: ResourceCategory) {
    IRON_ORE(ResourceCategory.RAW),
    COAL(ResourceCategory.RAW),
    STONE(ResourceCategory.RAW),
    COPPER_ORE(ResourceCategory.RAW),
    IRON_PLATE(ResourceCategory.PROCESSED),
    COPPER_PLATE(ResourceCategory.PROCESSED)
}

enum class BuildingType { STONE_FURNACE, BASIC_MINER }

enum class GroupState { RUNNING, STALLED, FUEL_STARVED, PAUSED, NO_RECIPE }

enum class GroupPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST }
