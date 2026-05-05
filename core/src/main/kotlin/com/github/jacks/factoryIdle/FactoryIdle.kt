package com.github.jacks.factoryIdle

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.factoryIdle.screens.GameScreen
import com.github.jacks.factoryIdle.ui.GameSkin
import ktx.app.KtxGame
import ktx.app.KtxScreen

class FactoryIdle : KtxGame<KtxScreen>(), EventListener {

    override fun create() {
        GameSkin.initialize()
        addScreen(GameScreen(this))
        setScreen<GameScreen>()
    }

    override fun render() {
        super.render()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }

    override fun dispose() {
        super.dispose()
    }

    override fun handle(event: Event): Boolean {
        return false
    }
}
