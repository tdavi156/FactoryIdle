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
import com.github.jacks.factoryIdle.data.BuildingType
import com.github.jacks.factoryIdle.data.GroupState
import com.github.jacks.factoryIdle.data.Recipe
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.iconKey
import com.github.jacks.factoryIdle.ui.mdIconKey
import com.github.jacks.factoryIdle.ui.smIconKey
import com.github.jacks.factoryIdle.ui.models.BuildMenuEntry
import com.github.jacks.factoryIdle.ui.models.FactoryModel
import com.github.jacks.factoryIdle.ui.models.PlacedBuildingData
import com.github.quillraven.fleks.Entity
import ktx.log.logger
import ktx.scene2d.Scene2DSkin

class FactoryView(private val model: FactoryModel) : Table() {

    companion object {
        private val log = logger<FactoryView>()
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
        // Root: horizontal split
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

        for (entry in model.buildMenuEntries) {
            buildMenuContent.add(buildMenuRow(entry)).expandX().fillX().pad(2f)
            buildMenuContent.row()
        }

        // Unassigned counts
        val anyUnassigned = model.buildMenuEntries.any { it.unassignedCount > 0 }
        if (anyUnassigned) {
            buildMenuContent.add(divider()).expandX().fillX().height(1f).padTop(6f).padBottom(2f)
            buildMenuContent.row()
            for (entry in model.buildMenuEntries) {
                if (entry.unassignedCount > 0) {
                    val lbl = Label(
                        "${entry.type.displayName}: ${entry.unassignedCount} unassigned",
                        skin,
                        Labels.SMALL()
                    )
                    lbl.color.set(0.47f, 0.50f, 0.56f, 1f)   // text_dim #7a8090
                    buildMenuContent.add(lbl).left().padLeft(4f).padTop(2f)
                    buildMenuContent.row()
                }
            }
        }

        buildMenuContent.add(Actor()).expand()  // spacer pushes content to top
    }

    private fun buildMenuRow(entry: BuildMenuEntry): Table {
        val row = Table()

        // Building icon
        val icon = Image(skin.getDrawable(entry.type.iconKey()))
        row.add(icon).size(CARD_ICON_SIZE).padRight(8f)

        // Name + cost column
        val infoCol = Table()
        val nameLabel = Label(entry.type.displayName, skin, Labels.BODY())
        infoCol.add(nameLabel).left().expandX().fillX()
        infoCol.row()

        val costRow = Table()
        for ((resource, qty) in entry.cost) {
            val rscIcon = Image(skin.getDrawable(resource.smIconKey()))
            costRow.add(rscIcon).size(16f).padRight(2f)
            val costLabel = Label("$qty", skin, Labels.SMALL())
            if (!entry.canAfford) costLabel.color.set(0.75f, 0.22f, 0.17f, 1f)  // #c0392b
            costRow.add(costLabel).padRight(6f)
        }
        infoCol.add(costRow).left()

        row.add(infoCol).expandX().fillX().padRight(8f)

        // Build button
        val btn = TextButton("Build", skin, Buttons.ACCENT())
        btn.isDisabled = !entry.canAfford
        btn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (!btn.isDisabled) {
                    log.debug { "Construction coming in Step 7 — ${entry.type.displayName}" }
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
            // entity no longer exists — fall through to list
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

        // Building icon
        val icon = Image(skin.getDrawable(data.type.iconKey()))
        card.add(icon).size(CARD_ICON_SIZE).padRight(8f).top()

        // Name + recipe column
        val infoCol = Table()
        val nameLabel = Label(data.type.displayName, skin, Labels.BODY())
        infoCol.add(nameLabel).left().expandX().fillX()
        infoCol.row()

        val recipeLabel = recipeLabel(data)
        infoCol.add(recipeLabel).left()

        card.add(infoCol).expandX().fillX().top()

        // Status dot (right side)
        val dot = Image(statusDot(data.groupState))
        card.add(dot).size(STATUS_DOT_SIZE).top().padLeft(8f)

        card.row()

        // Satisfaction bar — spans full width
        if (data.groupState != GroupState.STALLED && data.currentSatisfaction > 0f) {
            val bar = SatisfactionBar(
                fillDrawable  = satisfactionFill(data.currentSatisfaction),
                trackDrawable = skin.getDrawable(Drawables.PROGRESS_TRACK()),
                satisfaction  = data.currentSatisfaction
            )
            card.add(bar).colspan(3).expandX().fillX().height(SAT_BAR_HEIGHT).padTop(6f)
            card.row()
        }

        // Click to open detail
        card.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.selectBuilding(data.entity)
            }
        })

        return card
    }

    private fun recipeLabel(data: PlacedBuildingData): Label {
        val text = when {
            data.recipe != null -> {
                val outKey = data.recipe.outputs.keys.firstOrNull()
                outKey?.displayName ?: "Unknown recipe"
            }
            data.assignedResource != null -> data.assignedResource.displayName
            else -> "No recipe"
        }
        val lbl = Label(text, skin, Labels.SMALL())
        if (data.recipe == null && data.assignedResource == null)
            lbl.color.set(0.47f, 0.50f, 0.56f, 1f)
        return lbl
    }

    // ── Detail Panel ──────────────────────────────────────────────────────────

    private fun showDetailPanel(data: PlacedBuildingData) {
        val panel = Table()
        panel.top().left()

        // Header row: back button + building name
        val backBtn = TextButton("← Back", skin, Buttons.DEFAULT())
        backBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.selectBuilding(null)
            }
        })
        val nameLabel = Label(data.type.displayName, skin, Labels.HEADING())

        val headerRow = Table()
        headerRow.add(backBtn).width(80f).height(28f).padRight(12f)
        headerRow.add(nameLabel).expandX().fillX().left()
        panel.add(headerRow).expandX().fillX().padBottom(10f)
        panel.row()

        // State indicator
        val stateText = stateDescription(data)
        val stateLabel = Label(stateText, skin, Labels.BODY())
        stateLabel.color.set(stateColor(data.groupState))
        panel.add(stateLabel).left().padBottom(8f)
        panel.row()

        panel.add(divider()).expandX().fillX().height(1f).padBottom(8f)
        panel.row()

        // Recipe / resource picker
        val pickerHeader = Label(
            if (data.type == BuildingType.BASIC_MINER) "Assign Resource" else "Assign Recipe",
            skin, Labels.BODY_BOLD()
        )
        panel.add(pickerHeader).left().padBottom(4f)
        panel.row()

        addPickerRows(panel, data)
        panel.row()

        panel.add(divider()).expandX().fillX().height(1f).padTop(4f).padBottom(8f)
        panel.row()

        // Per-input satisfaction breakdown
        if (data.resourceSatisfaction.isNotEmpty()) {
            val breakdownHeader = Label("Input Satisfaction", skin, Labels.BODY_BOLD())
            panel.add(breakdownHeader).left().padBottom(4f)
            panel.row()
            addSatisfactionBreakdown(panel, data)
            panel.row()
        }

        // Fuel state
        if (data.hasFuelConsumer) {
            val fuelText = if (data.isFuelStarved) "Fuel: STARVED" else "Fuel: OK"
            val fuelLabel = Label(fuelText, skin, Labels.SMALL())
            if (data.isFuelStarved) fuelLabel.color.set(0.90f, 0.49f, 0.13f, 1f)  // #e67e22
            panel.add(fuelLabel).left().padBottom(8f)
            panel.row()
        }

        panel.add(divider()).expandX().fillX().height(1f).padBottom(8f)
        panel.row()

        // Pause toggle
        val pauseText = if (data.paused) "Unpause" else "Pause"
        val pauseBtn = TextButton(pauseText, skin, Buttons.DEFAULT())
        pauseBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.togglePause(data.entity)
            }
        })
        panel.add(pauseBtn).width(100f).height(32f).left()
        panel.row()

        panel.add(Actor()).expand()  // spacer

        val scroll = ScrollPane(panel, skin).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
        }
        rightContent.add(scroll).expand().fill()
    }

    private fun addPickerRows(panel: Table, data: PlacedBuildingData) {
        if (data.type == BuildingType.BASIC_MINER) {
            for (resource in Resource.values()) {
                if (resource.category.name != "RAW") continue
                addResourcePickerRow(panel, data.entity, resource, data.assignedResource == resource)
            }
        } else {
            val recipes = model.recipesFor(data.type)
            if (recipes.isEmpty()) {
                val noRecipesLabel = Label("No recipes available", skin, Labels.SMALL())
                noRecipesLabel.color.set(0.47f, 0.50f, 0.56f, 1f)
                panel.add(noRecipesLabel).left().padBottom(4f)
                panel.row()
            } else {
                for (recipe in recipes) {
                    addRecipePickerRow(panel, data.entity, recipe, data.recipe == recipe)
                }
            }
        }
    }

    private fun addRecipePickerRow(
        panel: Table,
        entity: Entity,
        recipe: Recipe,
        selected: Boolean
    ) {
        val outputResource = recipe.outputs.keys.firstOrNull()
        val outputName = outputResource?.displayName ?: "Unknown"

        val row = Table()
        if (outputResource != null) {
            val icon = Image(skin.getDrawable(outputResource.smIconKey()))
            row.add(icon).size(20f).padRight(6f)
        }
        val lbl = Label(outputName, skin, Labels.BODY())
        row.add(lbl).expandX().fillX().left()

        if (!selected) {
            val btn = TextButton("Assign", skin, Buttons.DEFAULT())
            btn.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    model.assignRecipe(entity, recipe)
                }
            })
            row.add(btn).width(72f).height(28f)
        } else {
            val selectedLabel = Label("✓", skin, Labels.BODY())
            selectedLabel.color.set(0.15f, 0.68f, 0.38f, 1f)
            row.add(selectedLabel).width(72f)
        }

        panel.add(row).expandX().fillX().padBottom(3f)
        panel.row()
    }

    private fun addResourcePickerRow(
        panel: Table,
        entity: Entity,
        resource: Resource,
        selected: Boolean
    ) {
        val row = Table()
        val icon = Image(skin.getDrawable(resource.smIconKey()))
        row.add(icon).size(20f).padRight(6f)
        val lbl = Label(resource.displayName, skin, Labels.BODY())
        row.add(lbl).expandX().fillX().left()

        if (!selected) {
            val btn = TextButton("Assign", skin, Buttons.DEFAULT())
            btn.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    model.assignMinerResource(entity, resource)
                }
            })
            row.add(btn).width(72f).height(28f)
        } else {
            val selectedLabel = Label("✓", skin, Labels.BODY())
            selectedLabel.color.set(0.15f, 0.68f, 0.38f, 1f)  // #27ae60
            row.add(selectedLabel).width(72f)
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

            val icon = Image(skin.getDrawable(resource.smIconKey()))

            val desc = Label(
                "${resource.displayName}  %.1f/s avail, %.1f/s needed ($pct%%)"
                    .format(availPerSec, neededPerSec),
                skin,
                Labels.SMALL()
            )
            if (sat < 1f) desc.color.set(0.90f, 0.49f, 0.13f, 1f)

            val breakdownRow = Table()
            breakdownRow.add(icon).size(16f).padRight(6f)
            breakdownRow.add(desc).expandX().fillX().left()
            panel.add(breakdownRow).expandX().fillX().padBottom(2f)
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

    private fun stateColor(state: GroupState): com.badlogic.gdx.graphics.Color =
        when (state) {
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
