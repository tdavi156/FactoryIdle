package com.github.jacks.factoryIdle.components

import com.github.jacks.factoryIdle.data.Resource
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class ResourceBuffer(
    val capacity: Map<Resource, Float>,
    val contents: MutableMap<Resource, Float> = mutableMapOf()
) : Component<ResourceBuffer> {
    override fun type() = ResourceBuffer
    companion object : ComponentType<ResourceBuffer>()
}
