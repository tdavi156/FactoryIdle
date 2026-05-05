package com.github.jacks.factoryIdle.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
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
import com.github.jacks.factoryIdle.ui.models.NavigationModel
import com.github.jacks.factoryIdle.ui.views.FactoryView
import com.github.jacks.factoryIdle.ui.views.NavSidebarView
import com.github.jacks.factoryIdle.ui.views.PowerView
import com.github.jacks.factoryIdle.ui.views.ProgressView
import com.github.jacks.factoryIdle.ui.views.ResearchView
import com.github.jacks.factoryIdle.ui.views.ResourceBarView
import com.github.jacks.factoryIdle.ui.views.SettingsView
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import ktx.app.KtxScreen
import ktx.log.logger
import ktx.scene2d.actors
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

    private val navigationModel  = NavigationModel()
    private val resourceBarView  = ResourceBarView()
    private val factoryView      = FactoryView()
    private val powerView        = PowerView()
    private val researchView     = ResearchView()
    private val progressView     = ProgressView()
    private val settingsView     = SettingsView()
    private val navSidebarView   = NavSidebarView(
        navigationModel, factoryView, powerView, researchView, progressView, settingsView
    )

    init {
        navigationModel.register(factoryView, powerView, researchView, progressView, settingsView)

        stage.actors {
            table {
                setFillParent(true)

                add(resourceBarView).expandX().fillX().height(RESOURCE_BAR_HEIGHT)
                row()

                val contentStack = Stack()
                contentStack.addActor(factoryView)
                contentStack.addActor(powerView)
                contentStack.addActor(researchView)
                contentStack.addActor(progressView)
                contentStack.addActor(settingsView)

                val innerTable = Table()
                innerTable.add(navSidebarView).fillY().width(NAV_WIDTH)
                innerTable.add(contentStack).expand().fill().prefWidth(0f).minWidth(0f)

                add(innerTable).expand().fill()
            }
        }

        navigationModel.show(factoryView)
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
        const val RESOURCE_BAR_HEIGHT = 52f
        const val NAV_WIDTH = 64f
        val log = logger<FactoryIdle>()
    }
}
