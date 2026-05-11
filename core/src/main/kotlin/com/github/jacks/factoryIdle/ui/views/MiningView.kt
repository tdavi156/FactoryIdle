package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.github.jacks.factoryIdle.data.Resource
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.largeIconKey
import com.github.jacks.factoryIdle.ui.models.MiningModel
import ktx.scene2d.Scene2DSkin
import kotlin.math.max

class MiningView(private val model: MiningModel) : Table() {

    private val skin = Scene2DSkin.defaultSkin

    private val miningProgressBars = mutableMapOf<Resource, ProgressBar>()
    private val miningCardTables   = mutableMapOf<Resource, Table>()

    private val progressBarStyle = ProgressBar.ProgressBarStyle().apply {
        background = skin.getDrawable(Drawables.PROGRESS_TRACK())
        knobBefore = skin.getDrawable(Drawables.PROGRESS_FILL_GREEN())
    }

    private var needsRebuild = false
    private var lastCols = -1

    private val cardContainer = object : Table() {
        override fun layout() {
            val cols = max(1, (width / CARD_WIDTH).toInt())
            if (needsRebuild || cols != lastCols) {
                needsRebuild = false
                lastCols = cols
                rebuildCards(cols)
            }
            super.layout()
        }
    }

    init {
        pad(12f)

        add(Label("Mining", skin, Labels.HEADING())).left().padBottom(8f).row()

        val scrollPane = ScrollPane(cardContainer, ScrollPane.ScrollPaneStyle()).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
            setFadeScrollBars(true)
        }
        add(scrollPane).expand().fill()

        model.onStructureChanged {
            needsRebuild = true
            cardContainer.invalidate()
        }
        model.onUpdate { updateCards() }
    }

    private fun rebuildCards(cols: Int) {
        cardContainer.clear()
        miningProgressBars.clear()
        miningCardTables.clear()

        val resources = model.unlockedRawResources()
        resources.forEachIndexed { index, resource ->
            cardContainer.add(buildCard(resource)).size(CARD_WIDTH, CARD_HEIGHT).pad(4f)
            if ((index + 1) % cols == 0) cardContainer.row()
        }
        if (resources.isNotEmpty() && resources.size % cols != 0) {
            cardContainer.row()
        }
    }

    private fun buildCard(resource: Resource): Table {
        val card = Table()
        card.background = skin.getDrawable(Drawables.CARD_BG_IDLE())

        card.add(Image(skin.getDrawable(resource.largeIconKey())))
            .expandX().center().size(ICON_SIZE).padTop(8f).row()

        card.add(Label(resource.displayName, skin, Labels.BODY()).also { it.setAlignment(Align.center) })
            .expandX().center().padTop(4f).row()

        card.add(Table()).expandX().expandY().row()

        val bar = ProgressBar(0f, 1f, 0.001f, false, progressBarStyle).also {
            it.touchable = Touchable.disabled
        }
        miningProgressBars[resource] = bar
        card.add(bar).expandX().fillX().height(PROGRESS_BAR_H)

        miningCardTables[resource] = card

        card.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                model.toggleMining(resource)
            }
        })

        return card
    }

    private fun updateCards() {
        for ((resource, card) in miningCardTables) {
            val bg = if (model.isHandMining(resource)) Drawables.CARD_BG_RUNNING() else Drawables.CARD_BG_IDLE()
            card.background = skin.getDrawable(bg)
        }
        for ((resource, bar) in miningProgressBars) {
            bar.value = model.miningProgress(resource)
        }
    }

    companion object {
        private const val CARD_WIDTH     = 180f
        private const val CARD_HEIGHT    = 220f
        private const val ICON_SIZE      = 128f
        private const val PROGRESS_BAR_H = 8f
    }
}
