package com.github.jacks.factoryIdle.components

import com.github.jacks.factoryIdle.data.BuildingType
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class BuildingComponent(
    val type: BuildingType
) : Component<BuildingComponent> {
    override fun type() = BuildingComponent
    companion object : ComponentType<BuildingComponent>()
}
