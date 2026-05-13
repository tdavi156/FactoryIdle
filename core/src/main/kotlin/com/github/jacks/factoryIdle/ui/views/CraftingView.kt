package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.factoryIdle.ui.Buttons
import com.github.jacks.factoryIdle.ui.Labels
import com.github.jacks.factoryIdle.ui.models.CraftingModel
import com.github.jacks.factoryIdle.ui.models.RecipeDisplayItem
import ktx.scene2d.Scene2DSkin

class CraftingView(private val model: CraftingModel) : Table() {

    private val skin = Scene2DSkin.defaultSkin

    private val buildingRows  = Table()
    private val intermediateRows = Table()

    init {
        top().left()

        val heading = Label("Craft", skin, Labels.HEADING())
        add(heading).left().pad(12f, 12f, 8f, 12f)
        row()

        // Production Facilities section
        val facilitiesHeader = Label("Production Facilities", skin, Labels.BODY_BOLD())
        add(facilitiesHeader).left().padLeft(12f).padBottom(4f)
        row()

        buildingRows.top().left()
        val buildingScroll = ScrollPane(buildingRows, skin).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
        }
        add(buildingScroll).expandX().fillX().prefHeight(240f)
        row()

        // Intermediates section — only shown when non-empty
        val intermediatesHeader = Label("Intermediates", skin, Labels.BODY_BOLD())
        add(intermediatesHeader).left().padLeft(12f).padTop(8f).padBottom(4f)
        row()

        intermediateRows.top().left()
        val intermediateScroll = ScrollPane(intermediateRows, skin).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
        }
        add(intermediateScroll).expandX().fillX().prefHeight(200f)
        row()

        add(Actor()).expand()

        model.onQueueChanged { rebuildRecipeRows() }
        rebuildRecipeRows()
    }

    private fun rebuildRecipeRows() {
        buildingRows.clearChildren()
        buildingRows.top().left()

        val buildingRecipes = model.getBuildingRecipes()
        if (buildingRecipes.isEmpty()) {
            val lbl = Label("No buildings unlocked yet.", skin, Labels.SMALL())
            lbl.color.set(0.47f, 0.50f, 0.56f, 1f)
            buildingRows.add(lbl).left().pad(4f, 12f, 4f, 12f)
        } else {
            for (item in buildingRecipes) {
                buildingRows.add(recipeRow(item)).expandX().fillX().pad(3f, 8f, 3f, 8f)
                buildingRows.row()
            }
        }

        intermediateRows.clearChildren()
        intermediateRows.top().left()

        val intermediateRecipes = model.getIntermediateRecipes()
        if (intermediateRecipes.isEmpty()) {
            val lbl = Label("No recipes available.", skin, Labels.SMALL())
            lbl.color.set(0.47f, 0.50f, 0.56f, 1f)
            intermediateRows.add(lbl).left().pad(4f, 12f, 4f, 12f)
        } else {
            for (item in intermediateRecipes) {
                intermediateRows.add(recipeRow(item)).expandX().fillX().pad(3f, 8f, 3f, 8f)
                intermediateRows.row()
            }
        }
    }

    private fun recipeRow(item: RecipeDisplayItem): Table {
        val row = Table()

        // Icon (32×32)
        val icon = Image(skin.getDrawable(item.iconKey))
        row.add(icon).size(32f).padRight(8f).top()

        // Info column: name + inputs + output + duration
        val info = Table()
        info.top().left()

        info.add(Label(item.displayName, skin, Labels.BODY())).left().expandX().fillX()
        info.row()

        // Input row
        if (item.inputSummary.isNotEmpty()) {
            val inputRow = Table()
            for ((iconKey, amount) in item.inputSummary) {
                val rIcon = Image(skin.getDrawable(iconKey))
                inputRow.add(rIcon).size(20f).padRight(2f)
                val amtLbl = Label(formatAmount(amount), skin, Labels.SMALL())
                amtLbl.color.set(0.87f, 0.87f, 0.87f, 1f)
                inputRow.add(amtLbl).padRight(6f)
            }
            info.add(inputRow).left().padTop(2f)
            info.row()
        }

        // Output + duration row
        val outRow = Table()
        for ((iconKey, amount) in item.outputSummary) {
            val oIcon = Image(skin.getDrawable(iconKey))
            outRow.add(oIcon).size(20f).padRight(2f)
            val amtLbl = Label("→ ${formatAmount(amount)}", skin, Labels.SMALL())
            amtLbl.color.set(0.47f, 0.50f, 0.56f, 1f)
            outRow.add(amtLbl).padRight(8f)
        }
        val durLbl = Label("%.1fs".format(item.durationSeconds), skin, Labels.SMALL())
        durLbl.color.set(0.47f, 0.50f, 0.56f, 1f)
        outRow.add(durLbl)
        info.add(outRow).left().padTop(2f)

        row.add(info).expandX().fillX().top().padRight(8f)

        // Craft button
        val btn = TextButton("Craft", skin, Buttons.ACCENT())
        btn.isDisabled = !item.canAfford
        btn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (!btn.isDisabled) {
                    model.enqueue(item)
                }
            }
        })
        row.add(btn).width(72f).height(32f).top()

        return row
    }

    private fun formatAmount(amount: Float): String {
        val whole = amount.toLong()
        return if (amount == whole.toFloat()) whole.toString() else "%.1f".format(amount)
    }
}
