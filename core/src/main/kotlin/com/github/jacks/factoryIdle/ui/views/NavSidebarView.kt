package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.factoryIdle.ui.Drawables
import com.github.jacks.factoryIdle.ui.models.NavigationModel
import ktx.scene2d.Scene2DSkin

class NavSidebarView(
    private val navigationModel: NavigationModel,
    miningView: Table,
    craftingView: Table,
    factoryView: Table,
    powerView: Table,
    researchView: Table,
    progressView: Table,
    settingsView: Table
) : Table() {

    private var programmaticUpdate = false

    init {
        val skin = Scene2DSkin.defaultSkin

        val navItems = listOf(
            Drawables.ICON_NAVIGATION_MINING   to miningView,
            Drawables.ICON_NAVIGATION_CRAFT    to craftingView,
            Drawables.ICON_NAVIGATION_FACTORY  to factoryView,
            Drawables.ICON_NAVIGATION_POWER    to powerView,
            Drawables.ICON_NAVIGATION_RESEARCH to researchView,
            Drawables.ICON_NAVIGATION_PROGRESS to progressView,
            Drawables.ICON_NAVIGATION_SETTINGS to settingsView
        )

        val buttonGroup = ButtonGroup<Button>()
        buttonGroup.setMinCheckCount(1)
        buttonGroup.setMaxCheckCount(1)

        val buttonViewPairs = mutableListOf<Pair<Button, Table>>()

        navItems.forEach { (iconDrawable, view) ->
            val style = ImageButtonStyle().apply {
                up      = skin.getDrawable(Drawables.BUTTON_NAVIGATION_UP())
                over    = skin.getDrawable(Drawables.BUTTON_NAVIGATION_OVER())
                down    = skin.getDrawable(Drawables.BUTTON_NAVIGATION_DOWN())
                checked = skin.getDrawable(Drawables.BUTTON_NAVIGATION_SELECTED())
                imageUp = skin.getDrawable(iconDrawable())
            }
            val button = ImageButton(style)
            button.addListener(object : ChangeListener() {
                override fun changed(event: ChangeListener.ChangeEvent, actor: Actor) {
                    if (!programmaticUpdate && button.isChecked) {
                        navigationModel.show(view)
                    }
                }
            })
            buttonGroup.add(button)
            buttonViewPairs.add(button to view)
            add(button).expandX().fillX().height(BUTTON_SIZE).row()
        }

        navigationModel.onActiveViewChange { activeView ->
            programmaticUpdate = true
            buttonViewPairs.forEach { (btn, view) ->
                btn.isChecked = (view === activeView)
            }
            programmaticUpdate = false
        }
    }

    companion object {
        private const val BUTTON_SIZE = 64f
    }
}
