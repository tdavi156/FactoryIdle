package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.github.jacks.factoryIdle.data.CraftQueueEntry
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.smallIconKey
import com.github.jacks.factoryIdle.ui.models.CraftingModel
import com.github.jacks.factoryIdle.ui.models.ResourceBarModel
import ktx.scene2d.Scene2DSkin

class QueueWidget(
    private val model: CraftingModel,
    private val resourceBarModel: ResourceBarModel
) : Table() {

    companion object {
        const val WIDGET_WIDTH = 220f
        const val MARGIN       = 12f
        private const val MAX_QUEUED_SHOWN = 5
        private const val PROGRESS_HEIGHT  = 8f
        private const val ICON_SIZE_ACTIVE = 32f
        private const val ICON_SIZE_QUEUED = 24f
    }

    private val skin = Scene2DSkin.defaultSkin

    // Progress bar for the active item — updated every frame
    private var progressBar: ProgressFillBar? = null

    init {
        background = skin.getDrawable(Drawables.PANEL_BG())
        rebuild()

        model.onQueueChanged { rebuild() }
        model.onUpdate { updateProgress() }
    }

    override fun act(delta: Float) {
        super.act(delta)
        val s: Stage = stage ?: return
        setSize(WIDGET_WIDTH, prefHeight)
        setPosition(s.width - WIDGET_WIDTH - MARGIN, MARGIN)
    }

    // ── Build structure ───────────────────────────────────────────────────────

    private fun rebuild() {
        clearChildren()
        pad(8f)
        top().left()

        val active  = model.activeEntry()
        val queued  = model.queuedEntries()

        // Mining indicator row
        miningRow()
        row()

        if (active == null) return

        // Active item row
        activeRow(active)
        row()

        // Queued items (up to MAX_QUEUED_SHOWN)
        val shown = queued.take(MAX_QUEUED_SHOWN)
        shown.forEachIndexed { idx, entry ->
            queuedRow(entry, index = idx + 1)  // index 0 = active, so queued start at 1
            row()
        }

        // Overflow label
        val overflow = queued.size - MAX_QUEUED_SHOWN
        if (overflow > 0) {
            val lbl = Label("+ $overflow more", skin, Labels.DIM())
            add(lbl).left().padTop(2f)
            row()
        }
    }

    private fun miningRow() {
        val miningRow = Table()
        val minedResource = resourceBarModel.unlockedRawResources()
            .firstOrNull { resourceBarModel.isHandMining(it) }

        if (minedResource != null) {
            val icon = Image(skin.getDrawable(minedResource.smallIconKey()))
            miningRow.add(icon).size(16f).padRight(4f)
            miningRow.add(Label("Mining: ${minedResource.displayName}", skin, Labels.SMALL())).left()
        } else {
            miningRow.add(Label("Mining: idle", skin, Labels.DIM())).left()
        }
        add(miningRow).expandX().fillX()
    }

    private fun activeRow(active: CraftQueueEntry) {
        val row = Table()

        // Icon
        val icon = Image(skin.getDrawable(active.iconKey))
        row.add(icon).size(ICON_SIZE_ACTIVE).padRight(6f).top()

        // Name + progress bar + countdown
        val info = Table()
        info.top().left()

        info.add(Label(active.displayName, skin, Labels.BODY())).left().expandX().fillX()
        info.row()

        val bar = ProgressFillBar(
            fillDrawable  = skin.getDrawable(Drawables.PROGRESS_FILL_GREEN()),
            trackDrawable = skin.getDrawable(Drawables.PROGRESS_TRACK()),
            progress      = progressFraction(active)
        )
        progressBar = bar
        info.add(bar).expandX().fillX().height(PROGRESS_HEIGHT).padTop(3f)

        row.add(info).expandX().fillX().top()

        val timeLbl = Label("%.1fs".format(active.remainingTime.coerceAtLeast(0f)), skin, Labels.SMALL())
        timeLbl.color.set(0.47f, 0.50f, 0.56f, 1f)
        row.add(timeLbl).width(40f).right().top()

        add(row).expandX().fillX().padTop(6f)
    }

    private fun queuedRow(entry: CraftQueueEntry, index: Int) {
        val row = Table()

        val icon = Image(skin.getDrawable(entry.iconKey))
        row.add(icon).size(ICON_SIZE_QUEUED).padRight(6f)

        val nameLbl = Label(entry.displayName, skin, Labels.SMALL())
        row.add(nameLbl).expandX().fillX().left()

        val cancelBtn = TextButton("×", skin, Buttons.DEFAULT())
        cancelBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                model.cancelQueued(index)
            }
        })
        row.add(cancelBtn).size(24f).right()

        add(row).expandX().fillX().padTop(2f)
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    private fun updateProgress() {
        val active = model.activeEntry() ?: return
        progressBar?.progress = progressFraction(active)
    }

    private fun progressFraction(entry: CraftQueueEntry): Float =
        if (entry.totalTime > 0f) 1f - (entry.remainingTime / entry.totalTime).coerceIn(0f, 1f) else 1f

    // ── Progress bar widget ───────────────────────────────────────────────────

    inner class ProgressFillBar(
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
