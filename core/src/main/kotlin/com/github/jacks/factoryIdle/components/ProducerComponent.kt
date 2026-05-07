package com.github.jacks.factoryIdle.components

import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.Recipe
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class ProducerComponent(
    var recipe: Recipe? = null,
    var progress: Float = 0f,
    var groupState: GroupState = GroupState.NO_RECIPE
) : Component<ProducerComponent> {
    override fun type() = ProducerComponent
    companion object : ComponentType<ProducerComponent>()
}
