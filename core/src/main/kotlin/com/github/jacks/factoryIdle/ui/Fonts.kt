package com.github.jacks.factoryIdle.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin

enum class Fonts(
    val skinKey: String,
    val fontPath: String,
    val atlasRegionKey: String,
    val scaling: Float = 1f
) {
    HEADING   ("font_heading",   "fonts/heading.fnt",   "font_heading"),
    BODY      ("font_body",      "fonts/body.fnt",      "font_body"),
    BODY_BOLD ("font_body_bold", "fonts/body_bold.fnt", "font_body_bold"),
    SMALL     ("font_small",     "fonts/small.fnt",     "font_small"),
    MONO      ("font_mono",      "fonts/mono.fnt",      "font_mono"),
}

fun loadFonts(skin: Skin) {
    Fonts.entries.forEach { font ->
        val bitmapFont = if (Gdx.files.internal(font.fontPath).exists()) {
            val region = skin.optional(font.atlasRegionKey, TextureRegion::class.java)
            if (region != null) {
                BitmapFont(Gdx.files.internal(font.fontPath), region)
            } else {
                BitmapFont(Gdx.files.internal(font.fontPath))
            }
        } else {
            BitmapFont()
        }
        bitmapFont.data.setScale(font.scaling)
        bitmapFont.data.markupEnabled = true
        skin.add(font.skinKey, bitmapFont)
    }
}

operator fun Skin.get(font: Fonts): BitmapFont = this.getFont(font.skinKey)
