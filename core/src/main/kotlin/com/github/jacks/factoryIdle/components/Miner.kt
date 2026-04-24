package com.github.jacks.factoryIdle.components

import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.Resource
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class Miner(
    var assignedResource: Resource? = null,
    var progress: Float = 0f,
    var groupState: GroupState = GroupState.NO_RECIPE
) : Component<Miner> {
    override fun type() = Miner
    companion object : ComponentType<Miner>()
}
