package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.Recipe
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.smallIconKey
import com.github.jacks.factoryIdle.ui.models.BuildMenuEntry
import com.github.jacks.factoryIdle.ui.models.FactoryModel
import com.github.jacks.factoryIdle.ui.models.PlacedBuildingData
import com.github.jacks.factoryIdle.ui.models.QueueDisplayEntry
import com.github.quillraven.fleks.Entity
import ktx.scene2d.Scene2DSkin

class FactoryView(private val model: FactoryModel) : Table() {

    companion object {
        private const val LEFT_PANEL_WIDTH = 280f
        private const val CARD_ICON_SIZE   = 32f
        private const val STATUS_DOT_SIZE  = 12f
        private const val SAT_BAR_HEIGHT   = 4f
        private const val CARD_PAD         = 8f
        private const val PANEL_PAD        = 6f
    }

    private val skin = Scene2DSkin.defaultSkin

    // Left panel — build menu
    private val buildMenuContent = Table()
    private val buildMenuScroll  = ScrollPane(buildMenuContent, skin).apply {
        setScrollingDisabled(true, false)
        setOverscroll(false, false)
    }

    // Right panel — building list OR detail panel
    private val rightContent = Table()

    init {
        val leftPanel = Table()
        leftPanel.background = skin.getDrawable(Drawables.PANEL_BG())
        leftPanel.add(buildMenuScroll).expand().fill().pad(PANEL_PAD)

        val rightPanel = Table()
        rightPanel.background = skin.getDrawable(Drawables.PANEL_DARK())
        rightPanel.add(rightContent).expand().fill().pad(PANEL_PAD)

        add(leftPanel).width(LEFT_PANEL_WIDTH).fillY().pad(4f)
        add(rightPanel).expand().fill().pad(4f)

        model.onChanged {
            rebuildBuildMenu()
            rebuildRight()
        }

        rebuildBuildMenu()
        rebuildRight()
    }

    // ── Build Menu ────────────────────────────────────────────────────────────

    private fun rebuildBuildMenu() {
        buildMenuContent.clearChildren()
        buildMenuContent.top().left()

        // Construction queue section
        if (model.queueEntries.isNotEmpty()) {
            val hdr = Label("Under Construction", skin, Labels.BODY_BOLD())
            hdr.color.set(0.87f, 0.72f, 0.23f, 1f)
            buildMenuContent.add(hdr).left().padBottom(4f)
            buildMenuContent.row()

            for (qe in model.queueEntries) {
                buildMenuContent.add(queueRow(qe)).expandX().fillX().pad(2f)
                buildMenuContent.row()
            }

            buildMenuContent.add(divider()).expandX().fillX().height(1f).padTop(6f).padBottom(6f)
            buildMenuContent.row()
        }

        for (entry in model.buildMenuEntries) {
            buildMenuContent.add(buildMenuRow(entry)).expandX().fillX().pad(2f)
            buildMenuContent.row()
        }

        val anyUnassigned = model.buildMenuEntries.any { it.unassignedCount > 0 }
        if (anyUnassigned) {
            buildMenuContent.add(divider()).expandX().fillX().height(1f).padTop(6f).padBottom(2f)
            buildMenuContent.row()
            for (entry in model.buildMenuEntries) {
                if (entry.unassignedCount > 0) {
                    val lbl = Label(
                        "${entry.type.displayName}: ${entry.unassignedCount} unassigned",
                        skin, Labels.SMALL()
                    )
                    lbl.color.set(0.47f, 0.50f, 0.56f, 1f)
                    buildMenuContent.add(lbl).left().padLeft(4f).padTop(2f)
                    buildMenuContent.row()
                }
            }
        }

        buildMenuContent.add(Actor()).expand()
    }

    private fun queueRow(qe: QueueDisplayEntry): Table {
        val row = Table()

        val icon = Image(skin.getDrawable(qe.iconKey))
        row.add(icon).size(24f).padRight(6f).top()

        val infoCol = Table()
        infoCol.add(Label(qe.type.displayName, skin, Labels.BODY())).left().expandX().fillX()
        infoCol.row()

        val bar = SatisfactionBar(
            fillDrawable  = skin.getDrawable(Drawables.PROGRESS_FILL_GREEN()),
            trackDrawable = skin.getDrawable(Drawables.PROGRESS_TRACK()),
            satisfaction  = qe.progress
        )
        infoCol.add(bar).expandX().fillX().height(SAT_BAR_HEIGHT).padTop(3f)

        row.add(infoCol).expandX().fillX()

        val timeLbl = Label("%.1fs".format(qe.remainingTime), skin, Labels.SMALL())
        timeLbl.color.set(0.47f, 0.50f, 0.56f, 1f)
        row.add(timeLbl).padLeft(8f).width(36f).right()

        return row
    }

    private fun buildMenuRow(entry: BuildMenuEntry): Table {
        val row = Table()

        val icon = Image(skin.getDrawable(entry.type.smallIconKey()))
        row.add(icon).size(CARD_ICON_SIZE).padRight(8f)

        val infoCol = Table()
        infoCol.add(Label(entry.type.displayName, skin, Labels.BODY())).left().expandX().fillX()
        infoCol.row()

        val costRow = Table()
        for ((resource, qty) in entry.cost) {
            val rscIcon = Image(skin.getDrawable(resource.smallIconKey()))
            costRow.add(rscIcon).size(16f).padRight(2f)
            val costLabel = Label("$qty", skin, Labels.SMALL())
            if (!entry.canAfford) costLabel.color.set(0.75f, 0.22f, 0.17f, 1f)
            costRow.add(costLabel).padRight(6f)
        }
        infoCol.add(costRow).left()

        row.add(infoCol).expandX().fillX().padRight(8f)

        val btn = TextButton("Build", skin, Buttons.ACCENT())
        btn.isDisabled = !entry.canAfford
        btn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (!btn.isDisabled) {
                    model.buildBuilding(entry)
                }
            }
        })
        row.add(btn).width(72f).height(32f)

        return row
    }

    // ── Right panel ───────────────────────────────────────────────────────────

    private fun rebuildRight() {
        rightContent.clearChildren()
        rightContent.top()

        val sel = model.selectedEntity
        if (sel != null) {
            val data = model.selectedEntityData()
            if (data != null) {
                showDetailPanel(data)
                return
            }
            model.selectBuilding(null)
        }

        showBuildingList()
    }

    private fun showBuildingList() {
        val buildings = model.placedBuildings
        if (buildings.isEmpty()) {
            val lbl = Label("No buildings yet. Build your first one →", skin, Labels.BODY())
            lbl.color.set(0.47f, 0.50f, 0.56f, 1f)
            rightContent.add(lbl).expand().center()
            return
        }

        val listTable = Table()
        listTable.top()
        for (data in buildings) {
            listTable.add(buildingCard(data)).expandX().fillX().pad(3f)
            listTable.row()
        }

        val scroll = ScrollPane(listTable, skin).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
        }
        rightContent.add(scroll).expand().fill()
    }

    // ── Building Card ─────────────────────────────────────────────────────────

    private fun buildingCard(data: PlacedBuildingData): Table {
        val card = Table()
        card.background = cardBackground(data.groupState)
        card.pad(CARD_PAD)

        val icon = Image(skin.getDrawable(data.type.smallIconKey()))
        card.add(icon).size(CARD_ICON_SIZE).padRight(8f).top()

        val infoCol = Table()
        infoCol.add(Label(data.type.displayName, skin, Labels.BODY())).left().expandX().fillX()
        infoCol.row()
        infoCol.add(recipeLabel(data)).left()

        card.add(infoCol).expandX().fillX().top()

        val dot = Image(statusDot(data.groupState))
        card.add(dot).size(STATUS_DOT_SIZE).top().padLeft(8f)

        card.row()

        if (data.groupState != GroupState.STALLED && data.currentSatisfaction > 0f) {
            val bar = SatisfactionBar(
                fillDrawable  = satisfactionFill(data.currentSatisfaction),
                trackDrawable = skin.getDrawable(Drawables.PROGRESS_TRACK()),
                satisfaction  = data.currentSatisfaction
            )
            card.add(bar).colspan(3).expandX().fillX().height(SAT_BAR_HEIGHT).padTop(6f)
            card.row()
        }

        card.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.selectBuilding(data.entity)
            }
        })

        return card
    }

    private fun recipeLabel(data: PlacedBuildingData): Label {
        val text = data.recipe?.outputs?.keys?.firstOrNull()?.displayName ?: "No recipe"
        val lbl = Label(text, skin, Labels.SMALL())
        if (data.recipe == null) lbl.color.set(0.47f, 0.50f, 0.56f, 1f)
        return lbl
    }

    // ── Detail Panel ──────────────────────────────────────────────────────────

    private fun showDetailPanel(data: PlacedBuildingData) {
        val panel = Table()
        panel.top().left()

        // Header
        val backBtn = TextButton("← Back", skin, Buttons.DEFAULT())
        backBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) { model.selectBuilding(null) }
        })
        val headerRow = Table()
        headerRow.add(backBtn).width(80f).height(28f).padRight(12f)
        headerRow.add(Label(data.type.displayName, skin, Labels.HEADING())).expandX().fillX().left()
        panel.add(headerRow).expandX().fillX().padBottom(10f)
        panel.row()

        // State
        val stateLabel = Label(stateDescription(data), skin, Labels.BODY())
        stateLabel.color.set(stateColor(data.groupState))
        panel.add(stateLabel).left().padBottom(8f)
        panel.row()

        panel.add(divider()).expandX().fillX().height(1f).padBottom(8f)
        panel.row()

        // Recipe picker
        panel.add(Label("Assign Recipe", skin, Labels.BODY_BOLD())).left().padBottom(4f)
        panel.row()
        addRecipePicker(panel, data)
        panel.row()

        panel.add(divider()).expandX().fillX().height(1f).padTop(4f).padBottom(8f)
        panel.row()

        // Per-input satisfaction breakdown
        if (data.resourceSatisfaction.isNotEmpty()) {
            panel.add(Label("Input Satisfaction", skin, Labels.BODY_BOLD())).left().padBottom(4f)
            panel.row()
            addSatisfactionBreakdown(panel, data)
            panel.row()
        }

        // Fuel state
        if (data.hasFuelConsumer) {
            val fuelText = if (data.isFuelStarved) "Fuel: STARVED" else "Fuel: OK"
            val fuelLabel = Label(fuelText, skin, Labels.SMALL())
            if (data.isFuelStarved) fuelLabel.color.set(0.90f, 0.49f, 0.13f, 1f)
            panel.add(fuelLabel).left().padBottom(8f)
            panel.row()
        }

        panel.add(divider()).expandX().fillX().height(1f).padBottom(8f)
        panel.row()

        // Pause toggle
        val pauseBtn = TextButton(if (data.paused) "Unpause" else "Pause", skin, Buttons.DEFAULT())
        pauseBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) { model.togglePause(data.entity) }
        })
        panel.add(pauseBtn).width(100f).height(32f).left()
        panel.row()

        panel.add(Actor()).expand()

        val scroll = ScrollPane(panel, skin).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
        }
        rightContent.add(scroll).expand().fill()
    }

    private fun addRecipePicker(panel: Table, data: PlacedBuildingData) {
        val recipes = model.recipesFor(data.type)
        if (recipes.isEmpty()) {
            val lbl = Label("No recipes available", skin, Labels.SMALL())
            lbl.color.set(0.47f, 0.50f, 0.56f, 1f)
            panel.add(lbl).left().padBottom(4f)
            panel.row()
            return
        }

        for (recipe in recipes) {
            addRecipeRow(panel, data.entity, recipe, data.recipe == recipe)
        }
    }

    private fun addRecipeRow(panel: Table, entity: Entity, recipe: Recipe, selected: Boolean) {
        val outputResource = recipe.outputs.keys.firstOrNull()
        val row = Table()

        if (outputResource != null) {
            row.add(Image(skin.getDrawable(outputResource.smallIconKey()))).size(20f).padRight(6f)
        }
        row.add(Label(outputResource?.displayName ?: "Unknown", skin, Labels.BODY()))
            .expandX().fillX().left()

        if (!selected) {
            val btn = TextButton("Assign", skin, Buttons.DEFAULT())
            btn.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    model.assignRecipe(entity, recipe)
                }
            })
            row.add(btn).width(72f).height(28f)
        } else {
            val check = Label("✓", skin, Labels.BODY())
            check.color.set(0.15f, 0.68f, 0.38f, 1f)
            row.add(check).width(72f)
        }

        panel.add(row).expandX().fillX().padBottom(3f)
        panel.row()
    }

    private fun addSatisfactionBreakdown(panel: Table, data: PlacedBuildingData) {
        for ((resource, sat) in data.resourceSatisfaction) {
            val declaredRate = data.recipe?.inputs?.get(resource) ?: 0f
            val duration     = data.recipe?.duration ?: 1f
            val neededPerSec = if (duration > 0f) declaredRate / duration else 0f
            val availPerSec  = neededPerSec * sat
            val pct          = (sat * 100f).toInt()

            val row = Table()
            row.add(Image(skin.getDrawable(resource.smallIconKey()))).size(16f).padRight(6f)
            val desc = Label(
                "${resource.displayName}  %.1f/s avail, %.1f/s needed ($pct%%)".format(availPerSec, neededPerSec),
                skin, Labels.SMALL()
            )
            if (sat < 1f) desc.color.set(0.90f, 0.49f, 0.13f, 1f)
            row.add(desc).expandX().fillX().left()

            panel.add(row).expandX().fillX().padBottom(2f)
            panel.row()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun stateDescription(data: PlacedBuildingData): String {
        val pct = (data.currentSatisfaction * 100f).toInt()
        return when (data.groupState) {
            GroupState.RUNNING      -> "RUNNING at $pct%"
            GroupState.STALLED      -> {
                val starved = data.resourceSatisfaction.entries
                    .filter { it.value == 0f }
                    .joinToString(", ") { it.key.displayName }
                if (starved.isNotEmpty()) "STALLED — waiting for $starved" else "STALLED"
            }
            GroupState.FUEL_STARVED -> "FUEL STARVED"
            GroupState.PAUSED       -> "PAUSED"
            GroupState.NO_RECIPE    -> "Idle — no recipe assigned"
        }
    }

    private fun stateColor(state: GroupState): com.badlogic.gdx.graphics.Color = when (state) {
        GroupState.RUNNING      -> com.badlogic.gdx.graphics.Color.valueOf("27ae60")
        GroupState.STALLED      -> com.badlogic.gdx.graphics.Color.valueOf("f39c12")
        GroupState.FUEL_STARVED -> com.badlogic.gdx.graphics.Color.valueOf("e67e22")
        GroupState.PAUSED       -> com.badlogic.gdx.graphics.Color.valueOf("c0392b")
        GroupState.NO_RECIPE    -> com.badlogic.gdx.graphics.Color.valueOf("7a8090")
    }

    private fun cardBackground(state: GroupState): Drawable = when (state) {
        GroupState.RUNNING      -> skin.getDrawable(Drawables.CARD_BG_RUNNING())
        GroupState.STALLED      -> skin.getDrawable(Drawables.CARD_BG_STALLED())
        GroupState.FUEL_STARVED -> skin.getDrawable(Drawables.CARD_BG_FUEL_STARVED())
        GroupState.PAUSED       -> skin.getDrawable(Drawables.CARD_BG_PAUSED())
        GroupState.NO_RECIPE    -> skin.getDrawable(Drawables.CARD_BG_IDLE())
    }

    private fun statusDot(state: GroupState): Drawable = when (state) {
        GroupState.RUNNING      -> skin.getDrawable(Drawables.STATUS_RUNNING())
        GroupState.STALLED      -> skin.getDrawable(Drawables.STATUS_STALLED())
        GroupState.FUEL_STARVED -> skin.getDrawable(Drawables.STATUS_FUEL_STARVED())
        GroupState.PAUSED       -> skin.getDrawable(Drawables.STATUS_PAUSED())
        GroupState.NO_RECIPE    -> skin.getDrawable(Drawables.STATUS_IDLE())
    }

    private fun satisfactionFill(satisfaction: Float): Drawable = when {
        satisfaction >= 1f   -> skin.getDrawable(Drawables.PROGRESS_FILL_GREEN())
        satisfaction >= 0.6f -> skin.getDrawable(Drawables.PROGRESS_FILL_AMBER())
        else                 -> skin.getDrawable(Drawables.PROGRESS_FILL_RED())
    }

    private fun divider(): Image = Image(skin.getDrawable(Drawables.PX_DIVIDER()))

    // ── SatisfactionBar widget ────────────────────────────────────────────────

    inner class SatisfactionBar(
        private val fillDrawable: Drawable,
        private val trackDrawable: Drawable,
        var satisfaction: Float = 1f
    ) : com.badlogic.gdx.scenes.scene2d.ui.Widget() {
        override fun getPrefHeight() = SAT_BAR_HEIGHT

        override fun draw(batch: Batch, parentAlpha: Float) {
            val c = color
            batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)
            trackDrawable.draw(batch, x, y, width, height)
            if (satisfaction > 0f)
                fillDrawable.draw(batch, x, y, width * satisfaction.coerceIn(0f, 1f), height)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }
}
