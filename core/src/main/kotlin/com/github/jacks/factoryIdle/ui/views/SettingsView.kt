package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.factoryIdle.ui.Labels
import ktx.scene2d.Scene2DSkin

class SettingsView : Table() {
    init {
        add(Label("Settings — Coming Soon", Scene2DSkin.defaultSkin, Labels.BODY())).expand()
    }
}
