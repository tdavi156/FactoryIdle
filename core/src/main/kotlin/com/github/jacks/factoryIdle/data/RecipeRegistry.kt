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
                inputs = mapOf(Resource.IRON_ORE to 2f),
                outputs = mapOf(Resource.IRON_PLATE to 1f),
                duration = 5f
            )
        ),
        BuildingType.BASIC_MINER to listOf(
            Recipe(inputs = emptyMap(), outputs = mapOf(Resource.IRON_ORE   to 1f), duration = 4f),
            Recipe(inputs = emptyMap(), outputs = mapOf(Resource.COAL       to 1f), duration = 4f),
            Recipe(inputs = emptyMap(), outputs = mapOf(Resource.STONE      to 1f), duration = 4f),
            Recipe(inputs = emptyMap(), outputs = mapOf(Resource.COPPER_ORE to 1f), duration = 4f)
        )
    )

    private val constructionCosts: Map<BuildingType, Map<Resource, Int>> = mapOf(
        BuildingType.STONE_FURNACE to mapOf(Resource.STONE to 5),
        BuildingType.BASIC_MINER   to mapOf(Resource.STONE to 5, Resource.IRON_PLATE to 5)
    )

    private val constructionTimes: Map<BuildingType, Float> = mapOf(
        BuildingType.STONE_FURNACE to 5f,
        BuildingType.BASIC_MINER   to 8f
    )

    fun recipesFor(type: BuildingType): List<Recipe> = recipes.getOrDefault(type, emptyList())

    fun constructionCostFor(type: BuildingType): Map<Resource, Int> =
        constructionCosts.getOrDefault(type, emptyMap())

    fun constructionTimeFor(type: BuildingType): Float =
        constructionTimes.getOrDefault(type, 5f)
}
