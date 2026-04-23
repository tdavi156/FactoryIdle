package com.github.jacks.factoryIdle.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.factoryIdle.FactoryIdle
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import ktx.app.KtxScreen
import ktx.log.logger
import ktx.scene2d.actors
import ktx.scene2d.stack
import ktx.scene2d.table

class GameScreen(game: FactoryIdle) : KtxScreen {

    private val stage = Stage(ScreenViewport())

    private val entityWorld: World = configureWorld {
        injectables {
        }
        systems {
        }
    }

    init {
        stage.actors {
            table {
                stack { stackCell ->
                }
            }
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        entityWorld.dispose()
        stage.dispose()
    }

    companion object {
        val log = logger<FactoryIdle>()
    }
}
