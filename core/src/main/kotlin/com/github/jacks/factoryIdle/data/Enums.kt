package com.github.jacks.factoryIdle.data

enum class ResourceCategory(val displayName: String) {
    RAW("Raw"), PROCESSED("Processed"), COMPONENT("Component"), SCIENCE("Science")
}

enum class Resource(val category: ResourceCategory, val displayName: String, val isFlow: Boolean = false) {
    IRON_ORE      (ResourceCategory.RAW,       "Iron Ore"),
    COAL          (ResourceCategory.RAW,       "Coal"),
    STONE         (ResourceCategory.RAW,       "Stone"),
    COPPER_ORE    (ResourceCategory.RAW,       "Copper Ore"),
    IRON_PLATE    (ResourceCategory.PROCESSED, "Iron Plate"),
    COPPER_PLATE  (ResourceCategory.PROCESSED, "Copper Plate"),
    COPPER_WIRE   (ResourceCategory.COMPONENT, "Copper Wire"),
    IRON_GEAR     (ResourceCategory.COMPONENT, "Iron Gear"),
    RED_SCIENCE   (ResourceCategory.SCIENCE,   "Red Science Pack"),
    ORANGE_SCIENCE(ResourceCategory.SCIENCE,   "Orange Science Pack"),
    YELLOW_SCIENCE(ResourceCategory.SCIENCE,   "Yellow Science Pack"),
    GREEN_SCIENCE (ResourceCategory.SCIENCE,   "Green Science Pack"),
    BLUE_SCIENCE  (ResourceCategory.SCIENCE,   "Blue Science Pack"),
    PURPLE_SCIENCE(ResourceCategory.SCIENCE,   "Purple Science Pack")
}

enum class BuildingType(val displayName: String) {
    STONE_FURNACE    ("Stone Furnace"),
    BASIC_MINER      ("Basic Miner"),
    ASSEMBLER_MK1    ("Assembler Mk1"),
    RESEARCH_FACILITY("Research Facility"),
    MINER_MK1        ("Miner Mk1")
}

enum class GroupState { RUNNING, STALLED, FUEL_STARVED, PAUSED, NO_RECIPE }

enum class GroupPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST }
