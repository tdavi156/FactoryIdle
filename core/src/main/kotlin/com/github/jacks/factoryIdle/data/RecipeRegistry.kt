package com.github.jacks.factoryIdle.data

// Recipe is a plain data definition — not an ECS component
data class Recipe(
    val inputs: Map<Resource, Float>,
    val outputs: Map<Resource, Float>,
    val duration: Float
)

class RecipeRegistry {
    private val recipes: Map<BuildingType, List<Recipe>> = mapOf(
        BuildingType.STONE_FURNACE to listOf(
            Recipe(
                inputs  = mapOf(Resource.IRON_ORE to 2f),
                outputs = mapOf(Resource.IRON_PLATE to 1f),
                duration = 5f
            ),
            Recipe(
                inputs  = mapOf(Resource.COPPER_ORE to 2f),
                outputs = mapOf(Resource.COPPER_PLATE to 1f),
                duration = 5f
            )
        ),
        BuildingType.BASIC_MINER to listOf(
            Recipe(inputs = emptyMap(), outputs = mapOf(Resource.IRON_ORE   to 1f), duration = 4f),
            Recipe(inputs = emptyMap(), outputs = mapOf(Resource.COAL       to 1f), duration = 4f),
            Recipe(inputs = emptyMap(), outputs = mapOf(Resource.STONE      to 1f), duration = 4f),
            Recipe(inputs = emptyMap(), outputs = mapOf(Resource.COPPER_ORE to 1f), duration = 4f)
        ),
        BuildingType.ASSEMBLER_MK1 to listOf(
            Recipe(
                inputs  = mapOf(Resource.IRON_PLATE to 2f),
                outputs = mapOf(Resource.IRON_GEAR to 1f),
                duration = 4f
            ),
            Recipe(
                inputs  = mapOf(Resource.COPPER_PLATE to 1f),
                outputs = mapOf(Resource.COPPER_WIRE to 2f),
                duration = 4f
            ),
            Recipe(
                inputs  = mapOf(Resource.IRON_PLATE to 1f, Resource.COPPER_WIRE to 1f),
                outputs = mapOf(Resource.RED_SCIENCE to 1f),
                duration = 5f
            )
        )
    )

    private val constructionCosts: Map<BuildingType, Map<Resource, Int>> = mapOf(
        BuildingType.STONE_FURNACE     to mapOf(Resource.STONE to 5),
        BuildingType.BASIC_MINER       to mapOf(Resource.STONE to 5, Resource.IRON_PLATE to 5),
        BuildingType.ASSEMBLER_MK1     to mapOf(Resource.IRON_PLATE to 5, Resource.IRON_GEAR to 3, Resource.COPPER_WIRE to 3),
        BuildingType.RESEARCH_FACILITY to mapOf(Resource.IRON_PLATE to 10, Resource.IRON_GEAR to 5, Resource.COPPER_WIRE to 5),
        BuildingType.MINER_MK1         to mapOf(Resource.IRON_PLATE to 5, Resource.IRON_GEAR to 3)
    )

    private val constructionTimes: Map<BuildingType, Float> = mapOf(
        BuildingType.STONE_FURNACE     to 5f,
        BuildingType.BASIC_MINER       to 8f,
        BuildingType.ASSEMBLER_MK1     to 15f,
        BuildingType.RESEARCH_FACILITY to 20f,
        BuildingType.MINER_MK1         to 10f
    )

    // Always available for hand-crafting in the crafting queue without a building
    private val handCraftable: List<Recipe> = listOf(
        Recipe(
            inputs  = mapOf(Resource.IRON_PLATE to 2f),
            outputs = mapOf(Resource.IRON_GEAR to 1f),
            duration = 4f
        ),
        Recipe(
            inputs  = mapOf(Resource.COPPER_PLATE to 1f),
            outputs = mapOf(Resource.COPPER_WIRE to 2f),
            duration = 4f
        ),
        Recipe(
            inputs  = mapOf(Resource.IRON_PLATE to 1f, Resource.COPPER_WIRE to 1f),
            outputs = mapOf(Resource.RED_SCIENCE to 1f),
            duration = 5f
        )
    )

    fun recipesFor(type: BuildingType): List<Recipe> = recipes.getOrDefault(type, emptyList())

    fun constructionCostFor(type: BuildingType): Map<Resource, Int> =
        constructionCosts.getOrDefault(type, emptyMap())

    fun constructionTimeFor(type: BuildingType): Float =
        constructionTimes.getOrDefault(type, 5f)

    fun handCraftableRecipes(): List<Recipe> = handCraftable
}
