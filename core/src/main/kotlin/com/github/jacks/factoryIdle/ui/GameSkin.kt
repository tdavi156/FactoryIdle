package com.github.jacks.factoryIdle.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.scene2d.Scene2DSkin

object GameSkin {

    fun initialize() {
        val skin = Skin()

        val atlasFile = Gdx.files.internal("ui.atlas")
        if (atlasFile.exists()) {
            val atlas = TextureAtlas(atlasFile)
            atlas.textures.forEach {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
            skin.addRegions(atlas)
        } else {
            addPlaceholders(skin)
        }

        loadFonts(skin)
        addButtonStyles(skin)
        addLabelStyles(skin)
        Scene2DSkin.defaultSkin = skin
    }

    private fun placeholder(hex: String, width: Int = 16, height: Int = 16): TextureRegionDrawable {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.valueOf(hex))
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(texture)
    }

    private fun Skin.addDrawable(key: String, drawable: Drawable) =
        add(key, drawable, Drawable::class.java)

    private fun addPlaceholders(skin: Skin) {
        // Default buttons
        skin.addDrawable(Drawables.BUTTON_DEFAULT_UP(),       placeholder("2d303a"))
        skin.addDrawable(Drawables.BUTTON_DEFAULT_OVER(),     placeholder("363a47"))
        skin.addDrawable(Drawables.BUTTON_DEFAULT_DOWN(),     placeholder("1e2028"))
        skin.addDrawable(Drawables.BUTTON_DEFAULT_DISABLED(), placeholder("22242a"))
        // Accent buttons
        skin.addDrawable(Drawables.BUTTON_ACCENT_UP(),        placeholder("8a5a10"))
        skin.addDrawable(Drawables.BUTTON_ACCENT_OVER(),      placeholder("a06a14"))
        skin.addDrawable(Drawables.BUTTON_ACCENT_DOWN(),      placeholder("6a4408"))
        skin.addDrawable(Drawables.BUTTON_ACCENT_DISABLED(),  placeholder("2e2a22"))
        // Danger buttons
        skin.addDrawable(Drawables.BUTTON_DANGER_UP(),        placeholder("4a1a1a"))
        skin.addDrawable(Drawables.BUTTON_DANGER_OVER(),      placeholder("5a2020"))
        skin.addDrawable(Drawables.BUTTON_DANGER_DOWN(),      placeholder("341212"))
        skin.addDrawable(Drawables.BUTTON_DANGER_DISABLED(),  placeholder("28201e"))
        // Navigation buttons
        skin.addDrawable(Drawables.BUTTON_NAVIGATION_UP(),       placeholder("22242a"))
        skin.addDrawable(Drawables.BUTTON_NAVIGATION_OVER(),     placeholder("2d303a"))
        skin.addDrawable(Drawables.BUTTON_NAVIGATION_DOWN(),     placeholder("1a1c22"))
        skin.addDrawable(Drawables.BUTTON_NAVIGATION_SELECTED(), placeholder("1e2028"))
        // Panels
        skin.addDrawable(Drawables.PANEL_BG(),             placeholder("22242a"))
        skin.addDrawable(Drawables.PANEL_DARK(),           placeholder("1a1c22"))
        skin.addDrawable(Drawables.PANEL_INSET(),          placeholder("18191e"))
        skin.addDrawable(Drawables.TOOLTIP_BG(),           placeholder("12131a"))
        skin.addDrawable(Drawables.RESOURCE_BAR_BG(),      placeholder("1a1a1f"))
        // Cards
        skin.addDrawable(Drawables.CARD_BG_RUNNING(),      placeholder("22242a"))
        skin.addDrawable(Drawables.CARD_BG_STALLED(),      placeholder("22242a"))
        skin.addDrawable(Drawables.CARD_BG_FUEL_STARVED(), placeholder("22242a"))
        skin.addDrawable(Drawables.CARD_BG_PAUSED(),       placeholder("22242a"))
        skin.addDrawable(Drawables.CARD_BG_IDLE(),         placeholder("22242a"))
        // Progress bars
        skin.addDrawable(Drawables.PROGRESS_TRACK(),       placeholder("18191e"))
        skin.addDrawable(Drawables.PROGRESS_FILL_GREEN(),  placeholder("27ae60"))
        skin.addDrawable(Drawables.PROGRESS_FILL_AMBER(),  placeholder("e8a020"))
        skin.addDrawable(Drawables.PROGRESS_FILL_BLUE(),   placeholder("2980b9"))
        skin.addDrawable(Drawables.PROGRESS_FILL_RED(),    placeholder("c0392b"))
        // Pixels (1×1)
        skin.addDrawable(Drawables.PX_DIVIDER(),           placeholder("3a3d47", 1, 1))
        skin.addDrawable(Drawables.PX_WHITE(),             placeholder("ffffff", 1, 1))
        skin.addDrawable(Drawables.PX_BLACK(),             placeholder("000000", 1, 1))
        // Status dots (12×12)
        skin.addDrawable(Drawables.STATUS_RUNNING(),       placeholder("27ae60", 12, 12))
        skin.addDrawable(Drawables.STATUS_STALLED(),       placeholder("f39c12", 12, 12))
        skin.addDrawable(Drawables.STATUS_FUEL_STARVED(),  placeholder("e67e22", 12, 12))
        skin.addDrawable(Drawables.STATUS_PAUSED(),        placeholder("c0392b", 12, 12))
        skin.addDrawable(Drawables.STATUS_IDLE(),          placeholder("7a8090", 12, 12))
        // Navigation icons (32×32)
        skin.addDrawable(Drawables.ICON_NAVIGATION_FACTORY(),  placeholder("7a8090", 32, 32))
        skin.addDrawable(Drawables.ICON_NAVIGATION_POWER(),    placeholder("e8a020", 32, 32))
        skin.addDrawable(Drawables.ICON_NAVIGATION_RESEARCH(), placeholder("2980b9", 32, 32))
        skin.addDrawable(Drawables.ICON_NAVIGATION_PROGRESS(), placeholder("27ae60", 32, 32))
        skin.addDrawable(Drawables.ICON_NAVIGATION_SETTINGS(), placeholder("7a8090", 32, 32))
        // Resource icons sm (20×20)
        skin.addDrawable(Drawables.ICON_RSC_IRON_ORE_SM(),      placeholder("8b5e3c", 20, 20))
        skin.addDrawable(Drawables.ICON_RSC_COAL_SM(),          placeholder("2a2a2a", 20, 20))
        skin.addDrawable(Drawables.ICON_RSC_STONE_SM(),         placeholder("888888", 20, 20))
        skin.addDrawable(Drawables.ICON_RSC_COPPER_ORE_SM(),    placeholder("b87333", 20, 20))
        skin.addDrawable(Drawables.ICON_RSC_IRON_PLATE_SM(),    placeholder("aaaaaa", 20, 20))
        skin.addDrawable(Drawables.ICON_RSC_COPPER_PLATE_SM(),  placeholder("d4843e", 20, 20))
        // Resource icons md (36×36)
        skin.addDrawable(Drawables.ICON_RSC_IRON_ORE_MD(),      placeholder("8b5e3c", 36, 36))
        skin.addDrawable(Drawables.ICON_RSC_COAL_MD(),          placeholder("2a2a2a", 36, 36))
        skin.addDrawable(Drawables.ICON_RSC_STONE_MD(),         placeholder("888888", 36, 36))
        skin.addDrawable(Drawables.ICON_RSC_COPPER_ORE_MD(),    placeholder("b87333", 36, 36))
        skin.addDrawable(Drawables.ICON_RSC_IRON_PLATE_MD(),    placeholder("aaaaaa", 36, 36))
        skin.addDrawable(Drawables.ICON_RSC_COPPER_PLATE_MD(),  placeholder("d4843e", 36, 36))
        // Building icons (32×32) and art (64×64)
        skin.addDrawable(Drawables.ICON_BLD_STONE_FURNACE(),  placeholder("555555", 32, 32))
        skin.addDrawable(Drawables.ICON_BLD_BASIC_MINER(),    placeholder("666666", 32, 32))
        skin.addDrawable(Drawables.BLD_ART_STONE_FURNACE(),   placeholder("555555", 64, 64))
        skin.addDrawable(Drawables.BLD_ART_BASIC_MINER(),     placeholder("666666", 64, 64))
    }

    private fun addButtonStyles(skin: Skin) {
        val bodyFont  = skin.getFont(Fonts.BODY.skinKey)
        val textColor = Color.valueOf("dde0e8")
        val dimColor  = Color.valueOf("404450")

        skin.add(Buttons.DEFAULT(), TextButtonStyle().apply {
            up                = skin.getDrawable(Drawables.BUTTON_DEFAULT_UP())
            over              = skin.getDrawable(Drawables.BUTTON_DEFAULT_OVER())
            down              = skin.getDrawable(Drawables.BUTTON_DEFAULT_DOWN())
            disabled          = skin.getDrawable(Drawables.BUTTON_DEFAULT_DISABLED())
            font              = bodyFont
            fontColor         = textColor
            disabledFontColor = dimColor
            pressedOffsetX    = 1f
            pressedOffsetY    = -1f
        })

        skin.add(Buttons.ACCENT(), TextButtonStyle().apply {
            up                = skin.getDrawable(Drawables.BUTTON_ACCENT_UP())
            over              = skin.getDrawable(Drawables.BUTTON_ACCENT_OVER())
            down              = skin.getDrawable(Drawables.BUTTON_ACCENT_DOWN())
            disabled          = skin.getDrawable(Drawables.BUTTON_ACCENT_DISABLED())
            font              = bodyFont
            fontColor         = textColor
            disabledFontColor = dimColor
            pressedOffsetX    = 1f
            pressedOffsetY    = -1f
        })

        skin.add(Buttons.DANGER(), TextButtonStyle().apply {
            up                = skin.getDrawable(Drawables.BUTTON_DANGER_UP())
            over              = skin.getDrawable(Drawables.BUTTON_DANGER_OVER())
            down              = skin.getDrawable(Drawables.BUTTON_DANGER_DOWN())
            disabled          = skin.getDrawable(Drawables.BUTTON_DANGER_DISABLED())
            font              = bodyFont
            fontColor         = textColor
            disabledFontColor = dimColor
            pressedOffsetX    = 1f
            pressedOffsetY    = -1f
        })

        // NAVIGATION uses checked state for the selected tab; no disabled state needed
        skin.add(Buttons.NAVIGATION(), TextButtonStyle().apply {
            up             = skin.getDrawable(Drawables.BUTTON_NAVIGATION_UP())
            over           = skin.getDrawable(Drawables.BUTTON_NAVIGATION_OVER())
            down           = skin.getDrawable(Drawables.BUTTON_NAVIGATION_DOWN())
            checked        = skin.getDrawable(Drawables.BUTTON_NAVIGATION_SELECTED())
            font           = bodyFont
            fontColor      = textColor
            pressedOffsetX = 1f
            pressedOffsetY = -1f
        })
    }

    private fun addLabelStyles(skin: Skin) {
        val textPrimary = Color.valueOf("dde0e8")
        val textDim     = Color.valueOf("7a8090")

        skin.add(Labels.HEADING(),   LabelStyle(skin.getFont(Fonts.HEADING.skinKey),   textPrimary))
        skin.add(Labels.BODY(),      LabelStyle(skin.getFont(Fonts.BODY.skinKey),      textPrimary))
        skin.add(Labels.BODY_BOLD(), LabelStyle(skin.getFont(Fonts.BODY_BOLD.skinKey), textPrimary))
        skin.add(Labels.SMALL(),     LabelStyle(skin.getFont(Fonts.SMALL.skinKey),     textPrimary))
        skin.add(Labels.DIM(),       LabelStyle(skin.getFont(Fonts.SMALL.skinKey),     textDim))
    }
}
