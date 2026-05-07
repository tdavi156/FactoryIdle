package com.github.jacks.factoryIdle.components

import com.github.jacks.factoryIdle.data.GroupPriority
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class BuildingGroupComponent(
    val id: String,
    val type: com.github.jacks.factoryIdle.data.BuildingType,
    var name: String,
    var count: Int = 0,
    var priority: GroupPriority = GroupPriority.NORMAL,
    var paused: Boolean = false
) : Component<BuildingGroupComponent> {
    override fun type() = BuildingGroupComponent
    companion object : ComponentType<BuildingGroupComponent>()
}
