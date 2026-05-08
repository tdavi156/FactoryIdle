package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.data.ResourceCategory
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.models.DisplayMode
import com.github.jacks.factoryIdle.ui.models.ResourceBarModel
import com.github.jacks.factoryIdle.ui.smallIconKey
import ktx.scene2d.Scene2DSkin

class ResourceBarView(private val model: ResourceBarModel) : Table() {

    private val skin = Scene2DSkin.defaultSkin

    // Mining widget state
    private val miningTable = Table()
    private val miningBars  = mutableMapOf<Resource, ProgressBar>()

    // Resource display state
    private val resourceContent = Table()
    private val amountLabels    = mutableMapOf<Resource, Label>()

    // Shared ProgressBar style — built once, reused for all mining bars
    private val miningBarStyle = ProgressBar.ProgressBarStyle().apply {
        background  = skin.getDrawable(Drawables.PROGRESS_TRACK())
        knobBefore  = skin.getDrawable(Drawables.PROGRESS_FILL_GREEN())
    }

    private val toggleButton = TextButton(modeLabel(model.displayMode), skin, Buttons.DEFAULT())

    init {
        background = skin.getDrawable(Drawables.RESOURCE_BAR_BG())
        pad(4f)

        // Left: mining widget
        add(miningTable).fillY()

        // Divider
        add(Image(skin.getDrawable(Drawables.PX_DIVIDER()))).width(1f).fillY().padLeft(6f).padRight(6f)

        // Centre: scrollable resource display
        val scrollStyle = ScrollPane.ScrollPaneStyle()
        val scrollPane  = ScrollPane(resourceContent, scrollStyle).apply {
            setScrollingDisabled(false, true)
            setOverscroll(false, false)
            setFadeScrollBars(true)
        }
        add(scrollPane).expandX().fillX().fillY()

        // Right: display mode toggle
        toggleButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.toggleDisplayMode()
            }
        })
        add(toggleButton).padLeft(6f)

        // Subscriptions
        model.onStructureChanged        { rebuild() }
        model.onUpdate                  { updateValues() }
        model.onDisplayModeChanged      { mode ->
            toggleButton.setText(modeLabel(mode))
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
        miningBars.clear()

        model.unlockedRawResources().forEachIndexed { index, resource ->
            val slot = Table()

            val button = TextButton(resource.displayName, skin, Buttons.DEFAULT()).apply {
                setProgrammaticChangeEvents(false)
            }
            button.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    model.startMining(resource)
                    button.isChecked = false
                }
            })
            slot.add(button).width(MINING_BUTTON_W).height(MINING_BUTTON_H)
            slot.row()

            val bar = ProgressBar(0f, 1f, 0.001f, false, miningBarStyle)
            miningBars[resource] = bar
            slot.add(bar).width(MINING_BUTTON_W).height(MINING_BAR_H)

            if (index > 0) miningTable.add(slot).padLeft(4f).fillY()
            else           miningTable.add(slot).fillY()
        }
    }

    private fun rebuildResourceDisplay() {
        resourceContent.clear()
        amountLabels.clear()

        val byCategory = model.visibleResources().groupBy { it.category }

        var firstCell = true
        for (category in ResourceCategory.values()) {
            val resources = byCategory[category] ?: continue

            val collapsed  = model.isCategoryCollapsed(category)
            val headerText = "${category.displayName} ${if (collapsed) ">" else "v"}"
            val headerBtn  = TextButton(headerText, skin, Buttons.DEFAULT()).apply {
                setProgrammaticChangeEvents(false)
            }
            headerBtn.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    model.toggleCategory(category)
                    headerBtn.isChecked = false
                }
            })
            val leftPad = if (firstCell) 0f else 8f
            resourceContent.add(headerBtn).padLeft(leftPad)
            firstCell = false

            if (!collapsed) {
                resources.forEach { resource ->
                    val icon = Image(skin.getDrawable(resource.smallIconKey()))
                    resourceContent.add(icon).size(ICON_SIZE).padLeft(6f)

                    resourceContent.add(
                        Label(resource.displayName, skin, Labels.BODY())
                    ).padLeft(3f)

                    val amtLabel = Label("0", skin, Labels.BODY_BOLD())
                    amountLabels[resource] = amtLabel
                    resourceContent.add(amtLabel).padLeft(5f).padRight(4f)
                }
            }
        }
    }

    // --- Per-frame value updates ---

    private fun updateValues() {
        for ((resource, bar) in miningBars) {
            bar.value = model.handMiningProgress(resource)
        }

        for ((resource, label) in amountLabels) {
            when (model.displayMode) {
                DisplayMode.COUNT -> label.setText(formatAmount(model.getAmount(resource)))
                DisplayMode.RATE  -> label.setText(formatRate(model.getRate(resource)))
            }
        }
    }

    // --- Helpers ---

    private fun modeLabel(mode: DisplayMode) = if (mode == DisplayMode.COUNT) "Count" else "Rate"

    private fun formatAmount(amount: Float): String = when {
        amount >= 1_000_000f -> "%.1fM".format(amount / 1_000_000f)
        amount >= 1_000f     -> "%.1fk".format(amount / 1_000f)
        else                 -> amount.toInt().toString()
    }

    private fun formatRate(rate: Float): String {
        // LibGDX bitmap font markup — colour applied inline, reset with []
        val color = when {
            rate > 0.05f  -> "[#27ae60]"
            rate < -0.05f -> "[#c0392b]"
            else          -> "[#7a8090]"
        }
        val sign = if (rate >= 0f) "+" else ""
        return "$color$sign%.1f/min[]".format(rate)
    }

    companion object {
        private const val MINING_BUTTON_W = 82f
        private const val MINING_BUTTON_H = 28f
        private const val MINING_BAR_H    = 6f
        private const val ICON_SIZE       = 20f
    }
}
