package com.github.jacks.factoryIdle.systems

import com.github.jacks.factoryIdle.components.BuildingGroupComponent
import com.github.jacks.factoryIdle.components.ProducerComponent
import com.github.jacks.factoryIdle.components.ProductionSatisfactionComponent
import com.github.jacks.factoryIdle.components.ResearchProducerComponent
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.jacks.factoryIdle.data.ResearchManager
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import kotlin.math.floor

class ProductionSystem : IteratingSystem(
    family { all(ProducerComponent, ProductionSatisfactionComponent) }
) {
    private val globalPool      = world.inject<GlobalResourcePool>()
    private val lifetimeStats   = world.inject<LifetimeMiningStats>()
    private val researchManager = world.inject<ResearchManager>()

    override fun onTickEntity(entity: Entity) {
        if (entity has BuildingGroupComponent && entity[BuildingGroupComponent].paused) return

        if (entity has ResearchProducerComponent) {
            handleResearchProducer(entity)
            return
        }

        val producer = entity[ProducerComponent]
        val sat      = entity[ProductionSatisfactionComponent]
        val recipe   = producer.recipe

        if (recipe == null) {
            producer.groupState = GroupState.NO_RECIPE
            return
        }

        // Timer always runs regardless of satisfaction
        producer.progress += deltaTime

        if (producer.progress >= recipe.duration) {
            producer.progress = 0f

            sat.fractionalAccumulator += sat.currentSatisfaction

            val whole = floor(sat.fractionalAccumulator).toInt()
            if (whole > 0) {
                for ((resource, amount) in recipe.outputs) {
                    val total = amount * whole
                    globalPool.add(resource, total)
                    lifetimeStats.add(resource, total)
                }
                sat.fractionalAccumulator -= whole.toFloat()
            }

            producer.groupState = if (sat.currentSatisfaction > 0f) GroupState.RUNNING else GroupState.STALLED
        }
    }

    private fun handleResearchProducer(entity: Entity) {
        val goal         = researchManager.activeGoal
        val producer     = entity[ProducerComponent]
        val sat          = entity[ProductionSatisfactionComponent]
        val researchComp = entity[ResearchProducerComponent]

        if (goal == null) {
            if (researchComp.lastGoalId != null) {
                researchComp.lastGoalId = null
                sat.declaredRates.clear()
                producer.progress = 0f
                sat.fractionalAccumulator = 0f
            }
            producer.groupState = GroupState.NO_RECIPE
            return
        }

        // Sync declaredRates and reset if the active goal changed
        if (researchComp.lastGoalId != goal.id) {
            researchComp.lastGoalId = goal.id
            sat.declaredRates.clear()
            goal.cost.keys.forEach { sat.declaredRates[it] = 1f / RESEARCH_CYCLE_DURATION }
            producer.progress = 0f
            sat.fractionalAccumulator = 0f
        }

        producer.progress += deltaTime
        if (producer.progress >= RESEARCH_CYCLE_DURATION) {
            producer.progress -= RESEARCH_CYCLE_DURATION
            sat.fractionalAccumulator += sat.currentSatisfaction
            val whole = floor(sat.fractionalAccumulator).toInt()
            if (whole > 0) {
                researchManager.addProgress(whole.toFloat())
                sat.fractionalAccumulator -= whole.toFloat()
            }
        }
        producer.groupState = if (sat.currentSatisfaction > 0f) GroupState.RUNNING else GroupState.STALLED
    }

    companion object {
        const val RESEARCH_CYCLE_DURATION = 60f
    }
}
