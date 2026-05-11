package com.github.jacks.factoryIdle.ui.models

import com.github.jacks.factoryIdle.data.Resource

class MiningModel(private val bar: ResourceBarModel) {
    fun unlockedRawResources(): List<Resource> = bar.unlockedRawResources()
    fun isHandMining(resource: Resource): Boolean = bar.isHandMining(resource)
    fun miningProgress(resource: Resource): Float = bar.handMiningProgress(resource)
    fun toggleMining(resource: Resource) = bar.toggleMining(resource)
    fun onUpdate(listener: () -> Unit) = bar.onUpdate(listener)
    fun onStructureChanged(listener: () -> Unit) = bar.onStructureChanged(listener)
}
