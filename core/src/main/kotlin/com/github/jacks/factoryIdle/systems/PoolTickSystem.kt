package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroupComponent
import com.github.jacks.factoryIdle.components.ProductionSatisfactionComponent
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupPriority
import com.github.jacks.factoryIdle.data.Resource
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family

class PoolTickSystem : IntervalSystem() {

    private val globalPool = world.inject<GlobalResourcePool>()
    private val satisfactionFamily = world.family { all(ProductionSatisfactionComponent) }

    override fun onTick() {
        // Collect active entities
        val active = mutableListOf<Entity>()
        satisfactionFamily.forEach { entity ->
            val paused = entity has BuildingGroupComponent && entity[BuildingGroupComponent].paused
            if (!paused) active.add(entity)
        }

        data class Demand(val entity: Entity, val rate: Float, val priority: GroupPriority)

        // resource → sorted demand list
        val demandMap = mutableMapOf<Resource, MutableList<Demand>>()
        for (entity in active) {
            val sat = entity[ProductionSatisfactionComponent]
            val priority = if (entity has BuildingGroupComponent) entity[BuildingGroupComponent].priority else GroupPriority.NORMAL
            for ((resource, rate) in sat.declaredRates) {
                if (rate > 0f) {
                    demandMap.getOrPut(resource) { mutableListOf() }
                        .add(Demand(entity, rate, priority))
                }
            }
        }

        // entity → resource → satisfaction
        val perEntityPerResource = mutableMapOf<Entity, MutableMap<Resource, Float>>()

        for ((resource, demands) in demandMap) {
            var remaining = globalPool.get(resource)

            for (tier in GroupPriority.entries.reversed()) {
                val tierDemands = demands.filter { it.priority == tier }
                if (tierDemands.isEmpty()) continue

                val tierTotal = tierDemands.sumOf { it.rate.toDouble() }.toFloat()
                val satisfaction = if (remaining >= tierTotal) {
                    remaining -= tierTotal
                    1f
                } else {
                    val s = if (tierTotal > 0f) remaining / tierTotal else 0f
                    remaining = 0f
                    s
                }

                for (d in tierDemands) {
                    perEntityPerResource.getOrPut(d.entity) { mutableMapOf() }[resource] = satisfaction
                }
            }
        }

        // Write back: resourceSatisfaction map + currentSatisfaction (min across all declared)
        for (entity in active) {
            val sat = entity[ProductionSatisfactionComponent]
            sat.resourceSatisfaction.clear()

            if (sat.declaredRates.isEmpty()) {
                sat.currentSatisfaction = 1f
                continue
            }

            val perResource = perEntityPerResource[entity]
            var minSat = 1f
            for (resource in sat.declaredRates.keys) {
                val s = perResource?.getOrDefault(resource, 0f) ?: 0f
                sat.resourceSatisfaction[resource] = s
                if (s < minSat) minSat = s
            }
            sat.currentSatisfaction = minSat
        }

        // Subtract actually consumed amounts from global pool (rate * satisfaction * deltaTime)
        for ((resource, demands) in demandMap) {
            var consumed = 0f
            for (d in demands) {
                val s = perEntityPerResource[d.entity]?.getOrDefault(resource, 0f) ?: 0f
                consumed += d.rate * s * deltaTime
            }
            globalPool.subtract(resource, consumed)
        }
    }
}
