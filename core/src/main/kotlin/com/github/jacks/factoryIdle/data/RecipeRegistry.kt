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
        BuildingType.BASIC_MINER to emptyList()
    )

    private val constructionCosts: Map<BuildingType, Map<Resource, Int>> = mapOf(
        BuildingType.STONE_FURNACE to mapOf(Resource.STONE to 5),
        BuildingType.BASIC_MINER   to mapOf(Resource.STONE to 5, Resource.IRON_PLATE to 5)
    )

    fun recipesFor(type: BuildingType): List<Recipe> = recipes.getOrDefault(type, emptyList())

    fun constructionCostFor(type: BuildingType): Map<Resource, Int> =
        constructionCosts.getOrDefault(type, emptyMap())
}
