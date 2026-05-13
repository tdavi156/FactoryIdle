package com.github.jacks.factoryIdle.data

enum class ResourceCategory(val displayName: String) {
    RAW("Raw"), PROCESSED("Processed"), COMPONENT("Component"), SCIENCE("Science")
}

enum class Resource(val category: ResourceCategory, val displayName: String, val isFlow: Boolean = false) {
    IRON_ORE    (ResourceCategory.RAW,       "Iron Ore"),
    COAL        (ResourceCategory.RAW,       "Coal"),
    STONE       (ResourceCategory.RAW,       "Stone"),
    COPPER_ORE  (ResourceCategory.RAW,       "Copper Ore"),
    IRON_PLATE  (ResourceCategory.PROCESSED, "Iron Plate"),
    COPPER_PLATE(ResourceCategory.PROCESSED, "Copper Plate")
}

enum class BuildingType(val displayName: String) {
    STONE_FURNACE("Stone Furnace"),
    BASIC_MINER("Basic Miner")
}

enum class GroupState { RUNNING, STALLED, FUEL_STARVED, PAUSED, NO_RECIPE }

enum class GroupPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST }
