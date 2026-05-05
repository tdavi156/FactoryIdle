package com.github.jacks.factoryIdle.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.factoryIdle.FactoryIdle
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.jacks.factoryIdle.data.RecipeRegistry
import com.github.jacks.factoryIdle.data.UnlockRegistry
import com.github.jacks.factoryIdle.data.buildPhase1Milestones
import com.github.jacks.factoryIdle.systems.FuelSystem
import com.github.jacks.factoryIdle.systems.MilestoneSystem
import com.github.jacks.factoryIdle.systems.MinerSystem
import com.github.jacks.factoryIdle.systems.PoolTickSystem
import com.github.jacks.factoryIdle.systems.ProductionSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import ktx.app.KtxScreen
import ktx.log.logger
import ktx.scene2d.actors
import ktx.scene2d.stack
import ktx.scene2d.table

class GameScreen(game: FactoryIdle) : KtxScreen {

    private val stage = Stage(ScreenViewport())

    private val globalResourcePool  = GlobalResourcePool()
    private val lifetimeMiningStats = LifetimeMiningStats()
    private val unlockRegistry      = UnlockRegistry()
    private val recipeRegistry      = RecipeRegistry()

    private val entityWorld: World = configureWorld {
        injectables {
            add(globalResourcePool)
            add(lifetimeMiningStats)
            add(unlockRegistry)
            add(recipeRegistry)
        }
        systems {
            add(PoolTickSystem())
            add(ProductionSystem())
            add(MinerSystem())
            add(FuelSystem())
            add(MilestoneSystem(buildPhase1Milestones(globalResourcePool, lifetimeMiningStats, unlockRegistry)))
        }
    }

    init {
        stage.actors {
            table {
                stack { }
            }
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        entityWorld.update(delta)
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        entityWorld.dispose()
        stage.dispose()
    }

    companion object {
        val log = logger<FactoryIdle>()
    }
}
