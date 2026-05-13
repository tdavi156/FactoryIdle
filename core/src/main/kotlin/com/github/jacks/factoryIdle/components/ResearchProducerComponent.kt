package com.github.jacks.factoryIdle.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class ResearchProducerComponent(
    var lastGoalId: String? = null
) : Component<ResearchProducerComponent> {
    override fun type() = ResearchProducerComponent
    companion object : ComponentType<ResearchProducerComponent>()
}
