package com.github.jacks.factoryIdle.components

import com.github.jacks.factoryIdle.data.Resource
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class FuelConsumerComponent(
    val fuelType: Resource,
    val consumeRate: Float,       // units per second (e.g. 1/30f for Stone Furnace)
    var fuelBuffer: Float = 0f
) : Component<FuelConsumerComponent> {
    override fun type() = FuelConsumerComponent
    companion object : ComponentType<FuelConsumerComponent>()
}
