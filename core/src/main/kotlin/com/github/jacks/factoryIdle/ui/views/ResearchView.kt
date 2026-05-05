package com.github.jacks.factoryIdle.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.factoryIdle.ui.Labels
import ktx.scene2d.Scene2DSkin

class ResearchView : Table() {
    init {
        add(Label("Research — Coming Soon", Scene2DSkin.defaultSkin, Labels.BODY())).expand()
    }
}
