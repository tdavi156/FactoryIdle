package com.github.jacks.factoryIdle.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.github.jacks.factoryIdle.FactoryIdle
import com.github.jacks.factoryIdle.components.BuildingComponent
import com.github.jacks.factoryIdle.components.FuelConsumerComponent
import com.github.jacks.factoryIdle.components.ProducerComponent
import com.github.jacks.factoryIdle.components.ProductionSatisfactionComponent
import com.github.jacks.factoryIdle.components.ResearchProducerComponent
import com.github.jacks.factoryIdle.data.BuildingType
import com.github.jacks.factoryIdle.data.CraftOutput
import com.github.jacks.factoryIdle.data.GlobalResourcePool
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.LifetimeMiningStats
import com.github.jacks.factoryIdle.data.PlayerCraftingQueue
import com.github.jacks.factoryIdle.data.Recipe
import com.github.jacks.factoryIdle.data.RecipeRegistry
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.data.ResearchManager
import com.github.jacks.factoryIdle.data.SaveManager
import com.github.jacks.factoryIdle.data.UnassignedPool
import com.github.jacks.factoryIdle.data.UnlockRegistry
import com.github.jacks.factoryIdle.data.buildPhase1Milestones
import com.github.jacks.factoryIdle.systems.FuelSystem
import com.github.jacks.factoryIdle.systems.MilestoneSystem
import com.github.jacks.factoryIdle.systems.PoolTickSystem
import com.github.jacks.factoryIdle.systems.ProductionSystem
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.models.CraftingModel
import com.github.jacks.factoryIdle.ui.models.FactoryModel
import com.github.jacks.factoryIdle.ui.models.MiningModel
import com.github.jacks.factoryIdle.ui.models.NavigationModel
import com.github.jacks.factoryIdle.ui.models.ResearchModel
import com.github.jacks.factoryIdle.ui.models.ResourceBarModel
import com.github.jacks.factoryIdle.ui.views.CraftingView
import com.github.jacks.factoryIdle.ui.views.FactoryView
import com.github.jacks.factoryIdle.ui.views.MiningView
import com.github.jacks.factoryIdle.ui.views.NavSidebarView
import com.github.jacks.factoryIdle.ui.views.PowerView
import com.github.jacks.factoryIdle.ui.views.ProgressView
import com.github.jacks.factoryIdle.ui.views.QueueWidget
import com.github.jacks.factoryIdle.ui.views.ResearchView
import com.github.jacks.factoryIdle.ui.views.ResourceBarView
import com.github.jacks.factoryIdle.ui.views.SettingsView
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import ktx.app.KtxScreen
import ktx.log.logger
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.actors
import ktx.scene2d.table

class GameScreen(game: FactoryIdle) : KtxScreen {

    private val stage = Stage(ScreenViewport())

    internal val globalResourcePool  = GlobalResourcePool()
    internal val lifetimeMiningStats = LifetimeMiningStats()
    internal val unlockRegistry      = UnlockRegistry()
    internal val unassignedPool      = UnassignedPool()
    internal val recipeRegistry      = RecipeRegistry()
    internal val playerCraftingQueue = PlayerCraftingQueue()
    internal val researchManager     = ResearchManager(unlockRegistry)

    internal val entityWorld: World = configureWorld {
        injectables {
            add(globalResourcePool)
            add(lifetimeMiningStats)
            add(unlockRegistry)
            add(recipeRegistry)
            add(playerCraftingQueue)
            add(researchManager)
        }
        systems {
            add(PoolTickSystem())
            add(ProductionSystem())
            add(FuelSystem())
            add(MilestoneSystem(buildPhase1Milestones(globalResourcePool, lifetimeMiningStats, unlockRegistry)))
        }
    }

    private val navigationModel  = NavigationModel()
    private val resourceBarModel = ResourceBarModel(
        pool             = globalResourcePool,
        lifetimeStats    = lifetimeMiningStats,
        unlockRegistry   = unlockRegistry,
        hasActiveDemand  = { resource ->
            var found = false
            with(entityWorld) {
                family { all(ProductionSatisfactionComponent) }.forEach { entity ->
                    if (!found && entity[ProductionSatisfactionComponent].declaredRates.containsKey(resource)) {
                        found = true
                    }
                }
            }
            found
        }
    )
    private val craftingModel    = CraftingModel(playerCraftingQueue, recipeRegistry, unlockRegistry, globalResourcePool, resourceBarModel)
    private val factoryModel     = FactoryModel(entityWorld, globalResourcePool, unlockRegistry, unassignedPool, recipeRegistry, craftingModel)
    private val resourceBarView  = ResourceBarView(resourceBarModel)
    private val miningModel      = MiningModel(resourceBarModel)
    private val miningView       = MiningView(miningModel)
    private val craftingView     = CraftingView(craftingModel)
    private val factoryView      = FactoryView(factoryModel)
    private val powerView        = PowerView()
    private val researchModel    = ResearchModel(researchManager) {
        var count = 0
        with(entityWorld) {
            family { all(ResearchProducerComponent) }.forEach { _ -> count++ }
        }
        count
    }
    private val researchView     = ResearchView(researchModel)
    private val progressView     = ProgressView()
    private val settingsView     = SettingsView(resourceBarModel)
    private val queueWidget      = QueueWidget(craftingModel, resourceBarModel)
    private val navSidebarView   = NavSidebarView(
        navigationModel, miningView, craftingView, factoryView, powerView, researchView, progressView, settingsView
    )

    private var timeSinceLastSave = 0f

    init {
        navigationModel.register(miningView, craftingView, factoryView, powerView, researchView, progressView, settingsView)

        stage.actors {
            table {
                setFillParent(true)

                add(resourceBarView).expandX().fillX().height(RESOURCE_BAR_HEIGHT)
                row()

                val contentStack = Stack()
                contentStack.addActor(miningView)
                contentStack.addActor(craftingView)
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

        stage.addActor(queueWidget)

        navigationModel.show(miningView)

        // Attempt to restore a saved game
        val saveData = SaveManager.load()
        if (saveData != null) {
            val gains = SaveManager.computeOfflineGains(saveData)
            SaveManager.applyLoad(saveData, this)
            SaveManager.applyOfflineGains(gains, globalResourcePool)
            if (gains.isNotEmpty()) {
                showOfflineProgressDialog(saveData.savedAt, gains)
            }
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        playerCraftingQueue.advance(delta)?.let { completed ->
            when (val out = completed.output) {
                is CraftOutput.BuildingOutput  -> createBuildingEntity(out.type)
                is CraftOutput.ResourceOutput  -> globalResourcePool.add(out.resource, out.amount)
            }
        }
        entityWorld.update(delta)
        resourceBarModel.update(delta)
        craftingModel.update()
        researchModel.update()
        factoryModel.update(delta)
        stage.act(delta)
        stage.draw()

        timeSinceLastSave += delta
        if (timeSinceLastSave >= AUTOSAVE_INTERVAL) {
            timeSinceLastSave = 0f
            SaveManager.save(this)
            showSavedIndicator()
        }
    }

    override fun pause() {
        SaveManager.save(this)
    }

    internal fun createBuildingEntity(
        type: BuildingType,
        recipe: Recipe? = null,
        cycleProgress: Float = 0f,
        fractionalAccumulator: Float = 0f,
        lastResearchGoalId: String? = null
    ) {
        val useFuel = type != BuildingType.ASSEMBLER_MK1 && type != BuildingType.RESEARCH_FACILITY

        val declaredRates = mutableMapOf<Resource, Float>()
        if (useFuel) declaredRates[Resource.COAL] = COAL_FUEL_RATE
        if (recipe != null) {
            for ((resource, amount) in recipe.inputs) {
                declaredRates[resource] = amount / recipe.duration
            }
        }

        val initialState = if (recipe != null) GroupState.RUNNING else GroupState.NO_RECIPE

        entityWorld.entity {
            it += BuildingComponent(type)
            it += ProducerComponent(recipe = recipe, progress = cycleProgress, groupState = initialState)
            if (useFuel) it += FuelConsumerComponent(Resource.COAL, COAL_FUEL_RATE)
            it += ProductionSatisfactionComponent(
                declaredRates         = declaredRates,
                fractionalAccumulator = fractionalAccumulator
            )
            if (type == BuildingType.RESEARCH_FACILITY) {
                it += ResearchProducerComponent(lastGoalId = lastResearchGoalId)
            }
        }
        unassignedPool.add(type, 1)
    }

    private fun showSavedIndicator() {
        val skin = Scene2DSkin.defaultSkin
        val label = Label("Saved", skin, Labels.SMALL())
        label.color = Color.valueOf("27ae60")
        label.setPosition(
            stage.width - label.prefWidth - 12f,
            12f
        )
        label.addAction(
            Actions.sequence(
                Actions.delay(2f),
                Actions.fadeOut(0.5f),
                Actions.removeActor()
            )
        )
        stage.addActor(label)
    }

    private fun showOfflineProgressDialog(savedAt: Long, gains: Map<Resource, Float>) {
        val skin = Scene2DSkin.defaultSkin
        val duration = SaveManager.formatOfflineDuration(savedAt)

        val dialog = Dialog("While you were away ($duration)", skin)
        dialog.contentTable.pad(16f)

        val lines = gains.entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (resource, amount) ->
                "+${formatAmount(amount)} ${resource.displayName}"
            }

        val contentLabel = Label(lines, skin, Labels.BODY())
        dialog.contentTable.add(contentLabel).left().row()

        dialog.button("OK", true)
        dialog.show(stage)
    }

    override fun dispose() {
        entityWorld.dispose()
        stage.dispose()
    }

    companion object {
        const val RESOURCE_BAR_HEIGHT = 120f
        const val NAV_WIDTH = 64f
        // ~2 coal/min per building — tuned from design doc "3 buildings × ~2 coal/min"
        const val COAL_FUEL_RATE = 2f / 60f
        const val AUTOSAVE_INTERVAL = 60f
        val log = logger<FactoryIdle>()

        private fun formatAmount(amount: Float): String {
            val whole = amount.toLong()
            return if (whole >= 1000) {
                val s = whole.toString()
                buildString {
                    val offset = s.length % 3
                    s.forEachIndexed { i, c ->
                        if (i > 0 && (i - offset) % 3 == 0) append(',')
                        append(c)
                    }
                }
            } else {
                whole.toString()
            }
        }
    }
}
