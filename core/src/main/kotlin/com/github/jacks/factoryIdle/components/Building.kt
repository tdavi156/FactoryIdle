package com.github.jacks.factoryIdle.components

import com.github.jacks.factoryIdle.data.BuildingType
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class Building(
    val type: BuildingType
) : Component<Building> {
    override fun type() = Building
    companion object : ComponentType<Building>()
}
