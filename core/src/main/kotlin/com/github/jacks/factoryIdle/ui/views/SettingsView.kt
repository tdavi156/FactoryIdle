package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.models.Density
import com.github.jacks.factoryIdle.ui.models.DisplayMode
import com.github.jacks.factoryIdle.ui.models.ResourceBarModel
import ktx.scene2d.Scene2DSkin

class SettingsView(private val model: ResourceBarModel) : Table() {

    private val skin = Scene2DSkin.defaultSkin

    private val modeCycleBtn      = TextButton(modeLabelText(model.displayMode), skin, Buttons.DEFAULT())
    private val densityComfortBtn = TextButton("Comfortable", skin, Buttons.DEFAULT())
    private val densityCompactBtn = TextButton("Compact", skin, Buttons.DEFAULT())

    init {
        pad(16f)

        add(Label("Settings", skin, Labels.HEADING())).left().expandX().fillX().row()

        addSeparator()

        // --- Resource Panel section ---
        add(Label("Resource Panel", skin, Labels.BODY_BOLD())).left().padTop(12f).padBottom(6f).row()

        // Display Mode row
        val modeRow = Table()
        modeRow.add(Label("Display Mode", skin, Labels.BODY())).expandX().fillX()
        modeCycleBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.toggleDisplayMode()
            }
        })
        modeRow.add(modeCycleBtn)
        add(modeRow).fillX().expandX().padBottom(4f).row()

        // Density row
        val densityRow = Table()
        densityRow.add(Label("Density", skin, Labels.BODY())).expandX().fillX()

        densityComfortBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.setDensity(Density.COMFORTABLE)
            }
        })
        densityCompactBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.setDensity(Density.COMPACT)
            }
        })
        densityRow.add(densityComfortBtn).padRight(4f)
        densityRow.add(densityCompactBtn)
        add(densityRow).fillX().expandX().row()

        // Sync initial checked states
        syncDensityButtons(model.density)

        // Subscribe to model changes to keep buttons in sync
        model.onDisplayModeChanged { mode ->
            modeCycleBtn.setText(modeLabelText(mode))
        }
        model.onStructureChanged {
            syncDensityButtons(model.density)
        }

        // Push everything to the top
        add().expand()
    }

    private fun addSeparator() {
        val line = Table()
        line.background = skin.getDrawable(Drawables.PX_DIVIDER())
        add(line).fillX().expandX().height(1f).padTop(8f).padBottom(4f).row()
    }

    private fun syncDensityButtons(density: Density) {
        densityComfortBtn.isChecked = density == Density.COMFORTABLE
        densityCompactBtn.isChecked = density == Density.COMPACT
    }

    private fun modeLabelText(mode: DisplayMode) = when (mode) {
        DisplayMode.COUNT_ONLY     -> "Count"
        DisplayMode.RATE_ONLY      -> "Rate"
        DisplayMode.COUNT_RATE     -> "Count+Rate"
        DisplayMode.COUNT_RATE_TTZ -> "Count+Rate+TTZ"
    }
}
