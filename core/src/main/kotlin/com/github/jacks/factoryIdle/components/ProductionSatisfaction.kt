package com.github.jacks.factoryIdle.components

import com.github.jacks.factoryIdle.data.Resource
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class ProductionSatisfaction(
    val declaredRates: MutableMap<Resource, Float> = mutableMapOf(),
    var currentSatisfaction: Float = 1f,
    val resourceSatisfaction: MutableMap<Resource, Float> = mutableMapOf(),
    var fractionalAccumulator: Float = 0f
) : Component<ProductionSatisfaction> {
    override fun type() = ProductionSatisfaction
    companion object : ComponentType<ProductionSatisfaction>()
}
