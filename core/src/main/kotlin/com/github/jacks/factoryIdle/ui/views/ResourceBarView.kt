package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.data.ResourceCategory
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.Fonts
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.models.Density
import com.github.jacks.factoryIdle.ui.models.DisplayMode
import com.github.jacks.factoryIdle.ui.models.ProblemLevel
import com.github.jacks.factoryIdle.ui.models.ResourceBarModel
import com.github.jacks.factoryIdle.ui.smallIconKey
import ktx.scene2d.Scene2DSkin

class ResourceBarView(private val model: ResourceBarModel) : Table() {

    private val skin = Scene2DSkin.defaultSkin

    // Mining widget state (LEFT side — DO NOT TOUCH)
    private val miningTable   = Table()
    private val miningButtons = mutableMapOf<Resource, ImageTextButton>()

    // Right-side panel
    private val panelArea = Table()

    // Mode cycle button lives in the panel header row
    private val modeCycleButton = TextButton(modeLabelText(model.displayMode), skin, Buttons.DEFAULT())

    // Problem filter toggle lives in the panel header row
    private val problemFilterButton = TextButton("⚠ 0", skin, Buttons.DEFAULT())
    private var problemFilterActive = false

    // Per-resource widget maps — rebuilt each time rebuildResourceDisplay() is called
    private val statusDotImages = mutableMapOf<Resource, Image>()
    private val dirArrowLabels  = mutableMapOf<Resource, Label>()
    private val amountLabels    = mutableMapOf<Resource, Label>()
    private val rateLabels      = mutableMapOf<Resource, Label>()
    private val ttzLabels       = mutableMapOf<Resource, Label>()

    init {
        background = skin.getDrawable(Drawables.RESOURCE_BAR_BG())
        pad(4f)

        // Left: mining widget (unchanged from Step 9)
        add(miningTable).fillY()

        // Divider
        add(Image(skin.getDrawable(Drawables.PX_DIVIDER()))).width(1f).fillY().padLeft(6f).padRight(6f)

        // Right: panel area
        add(panelArea).expandX().fillX().fillY()

        // Mode cycle button
        modeCycleButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.toggleDisplayMode()
            }
        })

        // Problem filter toggle
        problemFilterButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                problemFilterActive = !problemFilterActive
                problemFilterButton.isChecked = problemFilterActive
                rebuildResourceDisplay()
            }
        })

        // Subscriptions
        model.onStructureChanged        { rebuild() }
        model.onUpdate                  { updateValues() }
        model.onDisplayModeChanged      { mode ->
            modeCycleButton.setText(modeLabelText(mode))
            updateValues()
        }
        model.onCategoryCollapseChanged { _, _ -> rebuildResourceDisplay() }

        rebuild()
    }

    // --- Build / rebuild ---

    private fun rebuild() {
        rebuildMiningButtons()
        rebuildResourceDisplay()
    }

    private fun rebuildMiningButtons() {
        miningTable.clear()
        miningButtons.clear()

        val fontColor = Color.valueOf("dde0e8")
        var col = 0

        model.unlockedRawResources().forEach { resource ->
            val style = ImageTextButton.ImageTextButtonStyle().apply {
                up       = skin.getDrawable(Drawables.BUTTON_DEFAULT_UP())
                over     = skin.getDrawable(Drawables.BUTTON_DEFAULT_OVER())
                down     = skin.getDrawable(Drawables.BUTTON_DEFAULT_DOWN())
                checked  = skin.getDrawable(Drawables.BUTTON_DEFAULT_DOWN())
                disabled = skin.getDrawable(Drawables.BUTTON_DEFAULT_DISABLED())
                font     = skin.getFont(Fonts.BODY.skinKey)
                this.fontColor = fontColor
                imageUp  = skin.getDrawable(resource.smallIconKey())
            }
            val button = ImageTextButton(resource.displayName, style).apply {
                setProgrammaticChangeEvents(false)
                isChecked = model.isHandMining(resource)
            }
            button.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    model.toggleMining(resource)
                }
            })
            miningButtons[resource] = button

            val padLeft = if (col == 0) 0f else 4f
            miningTable.add(button).width(MINING_BUTTON_W).height(MINING_BUTTON_H).padLeft(padLeft)
            col++
            if (col >= 3) {
                miningTable.row()
                col = 0
            }
        }
    }

    private fun rebuildResourceDisplay() {
        panelArea.clear()
        statusDotImages.clear()
        dirArrowLabels.clear()
        amountLabels.clear()
        rateLabels.clear()
        ttzLabels.clear()

        // Count all non-HEALTHY visible resources for the filter button
        val visible = model.visibleResources()
        val totalProblems = visible.count { model.getProblemLevel(it) != ProblemLevel.HEALTHY }
        problemFilterButton.setText("⚠ $totalProblems")
        problemFilterButton.isVisible = totalProblems > 0
        if (totalProblems == 0 && problemFilterActive) {
            problemFilterActive = false
            problemFilterButton.isChecked = false
        }

        // Header row: mode button (left) + filter button (right)
        val headerRow = Table()
        headerRow.add(modeCycleButton).left()
        headerRow.add(problemFilterButton).right().expandX()
        panelArea.add(headerRow).fillX().expandX().padBottom(2f).row()

        // Determine which categories to show
        val hasScience = visible.any { it.category == ResourceCategory.SCIENCE }
        val categories = mutableListOf(
            ResourceCategory.RAW,
            ResourceCategory.PROCESSED,
            ResourceCategory.COMPONENT
        )
        if (hasScience) categories.add(ResourceCategory.SCIENCE)

        // Columns row
        val columnsRow = Table()
        for ((i, category) in categories.withIndex()) {
            val col = buildColumn(category)
            val padLeft = if (i == 0) 0f else 3f
            columnsRow.add(col).expandX().fillX().fillY().padLeft(padLeft)
        }
        panelArea.add(columnsRow).expandX().fillX().expandY().fillY()
    }

    private fun buildColumn(category: ResourceCategory): Table {
        val columnTable = Table()
        val collapsed   = model.isCategoryCollapsed(category)
        val allInCat    = model.visibleResources().filter { it.category == category }
        val displayRows = if (problemFilterActive)
            allInCat.filter { model.getProblemLevel(it) != ProblemLevel.HEALTHY }
        else
            allInCat

        // Badge counts
        val badCount  = allInCat.count { model.getProblemLevel(it) == ProblemLevel.BAD }
        val warnCount = allInCat.count { model.getProblemLevel(it) == ProblemLevel.WARN }
        val badgeTotal = badCount + warnCount

        // Column header row
        val headerRow = Table()
        val arrowBtn  = TextButton(if (collapsed) ">" else "v", skin, Buttons.DEFAULT()).apply {
            setProgrammaticChangeEvents(false)
        }
        arrowBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.toggleCategory(category)
                arrowBtn.isChecked = false
            }
        })
        headerRow.add(arrowBtn).padRight(2f)
        headerRow.add(Label(category.displayName, skin, Labels.SMALL())).expandX().fillX()

        if (badgeTotal > 0) {
            val badge = Label(badgeTotal.toString(), skin, Labels.SMALL())
            badge.color = if (badCount > 0) Color.valueOf("c0392b") else Color.valueOf("e8a020")
            headerRow.add(badge).padLeft(4f)
        }

        columnTable.add(headerRow).fillX().expandX().row()

        if (!collapsed) {
            val contentTable = Table()
            val rowH = if (model.density == Density.COMFORTABLE) ROW_H_COMFORTABLE else ROW_H_COMPACT

            for (resource in displayRows) {
                val row = buildResourceRow(resource, rowH)
                contentTable.add(row).fillX().expandX().height(rowH).row()
            }

            val scrollPane = ScrollPane(contentTable, ScrollPane.ScrollPaneStyle()).apply {
                setScrollingDisabled(true, false)
                setOverscroll(false, false)
                setFadeScrollBars(true)
            }
            columnTable.add(scrollPane).expandX().fillX().expandY().fillY()
        }

        return columnTable
    }

    private fun buildResourceRow(resource: Resource, rowH: Float): Table {
        val row = Table()

        // Status dot
        val dotKey = when (model.getProblemLevel(resource)) {
            ProblemLevel.HEALTHY -> Drawables.STATUS_RUNNING()
            ProblemLevel.WARN    -> Drawables.STATUS_STALLED()
            ProblemLevel.BAD     -> Drawables.STATUS_PAUSED()
        }
        val dot = Image(skin.getDrawable(dotKey))
        statusDotImages[resource] = dot
        row.add(dot).size(DOT_SIZE).padLeft(2f)

        // Direction arrow
        val arrow = Label(directionArrow(model.getRate(resource)), skin, Labels.SMALL())
        dirArrowLabels[resource] = arrow
        row.add(arrow).padLeft(2f)

        // Resource icon
        row.add(Image(skin.getDrawable(resource.smallIconKey()))).size(ICON_SIZE).padLeft(3f)

        // Resource name
        row.add(Label(resource.displayName, skin, Labels.BODY())).padLeft(3f).expandX().fillX()

        // Amount
        val amtLabel = Label("", skin, Labels.BODY_BOLD())
        amountLabels[resource] = amtLabel
        row.add(amtLabel).padLeft(5f)

        // Rate
        val rateLabel = Label("", skin, Labels.BODY())
        rateLabels[resource] = rateLabel
        row.add(rateLabel).padLeft(4f)

        // Time-to-zero
        val ttzLabel = Label("", skin, Labels.DIM())
        ttzLabels[resource] = ttzLabel
        row.add(ttzLabel).padLeft(3f).padRight(3f)

        // Right-click context menu
        row.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (button == Input.Buttons.RIGHT) {
                    showContextMenu(resource, event)
                    return true
                }
                return false
            }
        })

        return row
    }

    private fun showContextMenu(resource: Resource, event: InputEvent) {
        val stg = event.stage ?: return
        stg.root.findActor<Table>("ctx_menu")?.remove()

        val menu = Table(skin).apply {
            name = "ctx_menu"
            background = skin.getDrawable(Drawables.TOOLTIP_BG())
            val hideBtn = TextButton("Hide", skin, Buttons.DEFAULT())
            hideBtn.addListener(object : ChangeListener() {
                override fun changed(ev: ChangeEvent, a: Actor) {
                    model.hideResource(resource)
                    this@apply.remove()
                }
            })
            add(hideBtn).pad(4f)
            pack()
            setPosition(event.stageX, event.stageY - prefHeight)
        }
        stg.addActor(menu)

        stg.addCaptureListener(object : InputListener() {
            override fun touchDown(ev: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (ev.target?.isDescendantOf(menu) == false) {
                    menu.remove()
                    stg.removeCaptureListener(this)
                }
                return false
            }
        })
    }

    // --- Per-frame value updates ---

    private fun updateValues() {
        // Mining buttons
        for ((resource, button) in miningButtons) {
            button.isChecked = model.isHandMining(resource)
        }

        val mode = model.displayMode

        for (resource in statusDotImages.keys) {
            val level = model.getProblemLevel(resource)

            // Status dot
            statusDotImages[resource]?.setDrawable(skin.getDrawable(when (level) {
                ProblemLevel.HEALTHY -> Drawables.STATUS_RUNNING()
                ProblemLevel.WARN    -> Drawables.STATUS_STALLED()
                ProblemLevel.BAD     -> Drawables.STATUS_PAUSED()
            }))

            // Direction arrow
            dirArrowLabels[resource]?.setText(directionArrow(model.getRate(resource)))

            // Amount
            amountLabels[resource]?.setText(when {
                mode == DisplayMode.RATE_ONLY -> ""
                resource.isFlow               -> "[#7a8090]~[]"
                else                          -> formatAmount(model.getAmount(resource))
            })

            // Rate
            rateLabels[resource]?.setText(when (mode) {
                DisplayMode.COUNT_ONLY -> ""
                else                   -> formatRate(model.getRate(resource))
            })

            // Time-to-zero (COUNT_RATE_TTZ only)
            ttzLabels[resource]?.setText(when (mode) {
                DisplayMode.COUNT_RATE_TTZ -> formatTTZ(resource)
                else                       -> ""
            })
        }

        // Keep problem filter button count current
        val totalProblems = model.visibleResources().count { model.getProblemLevel(it) != ProblemLevel.HEALTHY }
        problemFilterButton.setText("⚠ $totalProblems")
        problemFilterButton.isVisible = totalProblems > 0
    }

    // --- Helpers ---

    private fun modeLabelText(mode: DisplayMode) = when (mode) {
        DisplayMode.COUNT_ONLY     -> "Count"
        DisplayMode.RATE_ONLY      -> "Rate"
        DisplayMode.COUNT_RATE     -> "Count+Rate"
        DisplayMode.COUNT_RATE_TTZ -> "Count+Rate+TTZ"
    }

    private fun directionArrow(rate: Float): String = when {
        rate > 0.05f  -> "[#27ae60]↑[]"
        rate < -0.05f -> "[#c0392b]↓[]"
        else          -> "[#7a8090]—[]"
    }

    private fun formatAmount(amount: Float): String = when {
        amount >= 1_000_000f -> "%.1fM".format(amount / 1_000_000f)
        amount >= 1_000f     -> "%.1fk".format(amount / 1_000f)
        else                 -> amount.toInt().toString()
    }

    private fun formatRate(rate: Float): String {
        val color = when {
            rate > 0.05f  -> "[#27ae60]"
            rate < -0.05f -> "[#c0392b]"
            else          -> "[#7a8090]"
        }
        val sign = if (rate >= 0f) "+" else ""
        return "$color$sign%.1f/min[]".format(rate)
    }

    private fun formatTTZ(resource: Resource): String {
        val amount = model.getAmount(resource)
        val rate   = model.getRate(resource)    // per minute
        if (rate >= -0.05f) return "[#7a8090]∞[]"

        val ratePerSec = rate / 60f
        val ttzSec = amount / (-ratePerSec)

        val color = when (model.getProblemLevel(resource)) {
            ProblemLevel.BAD  -> "[#c0392b]"
            ProblemLevel.WARN -> "[#e8a020]"
            else              -> "[#dde0e8]"
        }

        val minutes = (ttzSec / 60f).toInt()
        val seconds = (ttzSec % 60f).toInt()
        return if (minutes > 0) "${color}${minutes}m ${seconds}s[]" else "${color}${seconds}s[]"
    }

    companion object {
        private const val MINING_BUTTON_W  = 96f
        private const val MINING_BUTTON_H  = 28f
        private const val ICON_SIZE        = 20f
        private const val DOT_SIZE         = 12f
        private const val ROW_H_COMFORTABLE = 28f
        private const val ROW_H_COMPACT     = 20f
    }
}
