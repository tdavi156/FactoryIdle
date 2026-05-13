package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.github.jacks.factoryIdle.data.ResearchGoal
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.models.ResearchModel
import ktx.scene2d.Scene2DSkin

class ResearchView(private val model: ResearchModel) : Table() {

    companion object {
        private const val PROGRESS_HEIGHT = 8f
    }

    private val skin = Scene2DSkin.defaultSkin

    private val activeSection = Table()
    private val goalRows      = Table()
    private var progressBar: ProgressFillBar? = null

    init {
        top().left()

        val heading = Label("Research", skin, Labels.HEADING())
        add(heading).left().pad(12f, 12f, 8f, 12f)
        row()

        add(activeSection).expandX().fillX().padLeft(12f).padRight(12f)
        row()

        val availableHeader = Label("Available Research", skin, Labels.BODY_BOLD())
        add(availableHeader).left().padLeft(12f).padTop(12f).padBottom(4f)
        row()

        goalRows.top().left()
        val scroll = ScrollPane(goalRows, skin).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
        }
        add(scroll).expandX().fillX().prefHeight(300f)
        row()

        add(Actor()).expand()

        model.onChanged { rebuildActiveSection(); rebuildGoalRows() }
        model.onUpdate  { progressBar?.progress = model.progressFraction }

        rebuildActiveSection()
        rebuildGoalRows()
    }

    private fun rebuildActiveSection() {
        activeSection.clearChildren()
        progressBar = null
        val goal = model.activeGoal
        if (goal == null) {
            activeSection.add(Label("No research active", skin, Labels.SMALL())).left()
            return
        }

        activeSection.top().left()
        activeSection.add(Label(goal.name, skin, Labels.BODY_BOLD())).left().expandX().fillX()
        activeSection.row()

        val bar = ProgressFillBar(
            fillDrawable  = skin.getDrawable(Drawables.PROGRESS_FILL_BLUE()),
            trackDrawable = skin.getDrawable(Drawables.PROGRESS_TRACK()),
            progress      = model.progressFraction
        )
        progressBar = bar
        activeSection.add(bar).expandX().fillX().height(PROGRESS_HEIGHT).padTop(3f).padBottom(4f)
        activeSection.row()

        val count = model.activeFacilityCount()
        val rateText = if (count > 0) "%.3f/s  (%d facilit%s)".format(
            count.toFloat() / 60f, count, if (count == 1) "y" else "ies"
        ) else "No Research Facilities placed"
        activeSection.add(Label(rateText, skin, Labels.SMALL())).left()
    }

    private fun rebuildGoalRows() {
        goalRows.clearChildren()
        for (goal in model.goalsForTier(1)) {
            goalRows.add(buildGoalRow(goal)).expandX().fillX().padBottom(6f)
            goalRows.row()
        }
    }

    private fun buildGoalRow(goal: ResearchGoal): Table {
        val row = Table()
        row.left()

        val isCompleted = model.isCompleted(goal.id)
        val isActive    = model.activeGoal?.id == goal.id
        val isAvailable = model.isAvailable(goal)

        val nameLabel = Label(goal.name, skin, Labels.BODY())
        row.add(nameLabel).left().expandX().fillX()

        val costStr = goal.cost.entries.joinToString("  ") { (res, qty) -> "${qty}× ${res.displayName}" }
        row.add(Label(costStr, skin, Labels.SMALL())).padLeft(8f)

        when {
            isCompleted -> row.add(Label("Done",   skin, Labels.DIM())).padLeft(8f)
            isActive    -> row.add(Label("Active", skin, Labels.DIM())).padLeft(8f)
            isAvailable -> {
                val btn = TextButton("Research", skin, Buttons.ACCENT())
                btn.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        model.setActive(goal)
                    }
                })
                row.add(btn).padLeft(8f)
            }
            else        -> row.add(Label("Locked", skin, Labels.DIM())).padLeft(8f)
        }
        return row
    }

    private inner class ProgressFillBar(
        private val fillDrawable: Drawable,
        private val trackDrawable: Drawable,
        var progress: Float = 0f
    ) : Actor() {
        override fun draw(batch: Batch, parentAlpha: Float) {
            val c = color
            batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)
            trackDrawable.draw(batch, x, y, width, height)
            if (progress > 0f)
                fillDrawable.draw(batch, x, y, width * progress.coerceIn(0f, 1f), height)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }
}
