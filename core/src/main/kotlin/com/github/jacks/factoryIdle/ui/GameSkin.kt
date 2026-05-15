package com.github.jacks.factoryIdle.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.scene2d.Scene2DSkin

object GameSkin {

    fun initialize() {
        val skin = Skin()

        val atlasFile = Gdx.files.internal("ui/ui_atlas.atlas")
        if (atlasFile.exists()) {
            val atlas = TextureAtlas(atlasFile)
            atlas.textures.forEach {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
            skin.addRegions(atlas)
        }

        loadFonts(skin)
        addPlaceholders(skin)   // fills any key not already covered by the atlas
        addButtonStyles(skin)
        addLabelStyles(skin)
        skin.add("default", ScrollPane.ScrollPaneStyle())
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

    // Only registers the placeholder when the atlas didn't already supply this key,
    // checking both the Drawable slot and the TextureRegion slot that addRegions() populates.
    private fun Skin.addDrawableIfMissing(key: String, drawable: Drawable) {
        if (!has(key, Drawable::class.java) && !has(key, TextureRegion::class.java)) {
            add(key, drawable, Drawable::class.java)
        }
    }

    private fun addPlaceholders(skin: Skin) {
        // Default buttons
        skin.addDrawableIfMissing(Drawables.BUTTON_DEFAULT_UP(),       placeholder("2d303a"))
        skin.addDrawableIfMissing(Drawables.BUTTON_DEFAULT_OVER(),     placeholder("363a47"))
        skin.addDrawableIfMissing(Drawables.BUTTON_DEFAULT_DOWN(),     placeholder("1e2028"))
        skin.addDrawableIfMissing(Drawables.BUTTON_DEFAULT_DISABLED(), placeholder("22242a"))
        // Accent buttons
        skin.addDrawableIfMissing(Drawables.BUTTON_ACCENT_UP(),        placeholder("8a5a10"))
        skin.addDrawableIfMissing(Drawables.BUTTON_ACCENT_OVER(),      placeholder("a06a14"))
        skin.addDrawableIfMissing(Drawables.BUTTON_ACCENT_DOWN(),      placeholder("6a4408"))
        skin.addDrawableIfMissing(Drawables.BUTTON_ACCENT_DISABLED(),  placeholder("2e2a22"))
        // Danger buttons
        skin.addDrawableIfMissing(Drawables.BUTTON_DANGER_UP(),        placeholder("4a1a1a"))
        skin.addDrawableIfMissing(Drawables.BUTTON_DANGER_OVER(),      placeholder("5a2020"))
        skin.addDrawableIfMissing(Drawables.BUTTON_DANGER_DOWN(),      placeholder("341212"))
        skin.addDrawableIfMissing(Drawables.BUTTON_DANGER_DISABLED(),  placeholder("28201e"))
        // Navigation buttons
        skin.addDrawableIfMissing(Drawables.BUTTON_NAVIGATION_UP(),       placeholder("22242a"))
        skin.addDrawableIfMissing(Drawables.BUTTON_NAVIGATION_OVER(),     placeholder("2d303a"))
        skin.addDrawableIfMissing(Drawables.BUTTON_NAVIGATION_DOWN(),     placeholder("1a1c22"))
        skin.addDrawableIfMissing(Drawables.BUTTON_NAVIGATION_SELECTED(), placeholder("1e2028"))
        // Panels
        skin.addDrawableIfMissing(Drawables.PANEL_BG(),             placeholder("22242a"))
        skin.addDrawableIfMissing(Drawables.PANEL_DARK(),           placeholder("1a1c22"))
        skin.addDrawableIfMissing(Drawables.PANEL_INSET(),          placeholder("18191e"))
        skin.addDrawableIfMissing(Drawables.TOOLTIP_BG(),           placeholder("12131a"))
        skin.addDrawableIfMissing(Drawables.RESOURCE_BAR_BG(),      placeholder("1a1a1f"))
        // Cards
        skin.addDrawableIfMissing(Drawables.CARD_BG_RUNNING(),      placeholder("22242a"))
        skin.addDrawableIfMissing(Drawables.CARD_BG_STALLED(),      placeholder("22242a"))
        skin.addDrawableIfMissing(Drawables.CARD_BG_FUEL_STARVED(), placeholder("22242a"))
        skin.addDrawableIfMissing(Drawables.CARD_BG_PAUSED(),       placeholder("22242a"))
        skin.addDrawableIfMissing(Drawables.CARD_BG_IDLE(),         placeholder("22242a"))
        // Progress bars
        skin.addDrawableIfMissing(Drawables.PROGRESS_TRACK(),       placeholder("18191e"))
        skin.addDrawableIfMissing(Drawables.PROGRESS_FILL_GREEN(),  placeholder("27ae60"))
        skin.addDrawableIfMissing(Drawables.PROGRESS_FILL_AMBER(),  placeholder("e8a020"))
        skin.addDrawableIfMissing(Drawables.PROGRESS_FILL_BLUE(),   placeholder("2980b9"))
        skin.addDrawableIfMissing(Drawables.PROGRESS_FILL_RED(),    placeholder("c0392b"))
        // Pixels (1×1)
        skin.addDrawableIfMissing(Drawables.PX_DIVIDER(),           placeholder("3a3d47", 1, 1))
        skin.addDrawableIfMissing(Drawables.PX_WHITE(),             placeholder("ffffff", 1, 1))
        skin.addDrawableIfMissing(Drawables.PX_BLACK(),             placeholder("000000", 1, 1))
        // Status dots (12×12)
        skin.addDrawableIfMissing(Drawables.STATUS_RUNNING(),       placeholder("27ae60", 12, 12))
        skin.addDrawableIfMissing(Drawables.STATUS_STALLED(),       placeholder("f39c12", 12, 12))
        skin.addDrawableIfMissing(Drawables.STATUS_FUEL_STARVED(),  placeholder("e67e22", 12, 12))
        skin.addDrawableIfMissing(Drawables.STATUS_PAUSED(),        placeholder("c0392b", 12, 12))
        skin.addDrawableIfMissing(Drawables.STATUS_IDLE(),          placeholder("7a8090", 12, 12))
        // Navigation icons (32×32)
        skin.addDrawableIfMissing(Drawables.ICON_NAVIGATION_FACTORY(),  placeholder("7a8090", 32, 32))
        skin.addDrawableIfMissing(Drawables.ICON_NAVIGATION_POWER(),    placeholder("e8a020", 32, 32))
        skin.addDrawableIfMissing(Drawables.ICON_NAVIGATION_RESEARCH(), placeholder("2980b9", 32, 32))
        skin.addDrawableIfMissing(Drawables.ICON_NAVIGATION_PROGRESS(), placeholder("27ae60", 32, 32))
        skin.addDrawableIfMissing(Drawables.ICON_NAVIGATION_SETTINGS(), placeholder("7a8090", 32, 32))
        skin.addDrawableIfMissing(Drawables.ICON_NAVIGATION_MINING(),   placeholder("8b5e3c", 32, 32))
        skin.addDrawableIfMissing(Drawables.ICON_NAVIGATION_CRAFT(),    placeholder("27ae60", 32, 32))
        // Raw resource icons (small = 32px, medium = 64px, large = 128px)
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_IRON_ORE_SMALL(),      placeholder("8b5e3c", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_IRON_ORE_MEDIUM(),     placeholder("8b5e3c", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_IRON_ORE_LARGE(),      placeholder("8b5e3c", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_COAL_SMALL(),          placeholder("2a2a2a", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_COAL_MEDIUM(),         placeholder("2a2a2a", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_COAL_LARGE(),          placeholder("2a2a2a", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_STONE_SMALL(),         placeholder("888888", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_STONE_MEDIUM(),        placeholder("888888", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_STONE_LARGE(),         placeholder("888888", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_COPPER_ORE_SMALL(),    placeholder("b87333", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_COPPER_ORE_MEDIUM(),   placeholder("b87333", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_RESOURCE_COPPER_ORE_LARGE(),    placeholder("b87333", 128, 128))
        // Processed material icons (small = 32px, medium = 64px, large = 128px)
        skin.addDrawableIfMissing(Drawables.ICON_PROCESSED_IRON_PLATE_SMALL(),     placeholder("aaaaaa", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_PROCESSED_IRON_PLATE_MEDIUM(),    placeholder("aaaaaa", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_PROCESSED_IRON_PLATE_LARGE(),     placeholder("aaaaaa", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_PROCESSED_COPPER_PLATE_SMALL(),   placeholder("cd7f32", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_PROCESSED_COPPER_PLATE_MEDIUM(),  placeholder("cd7f32", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_PROCESSED_COPPER_PLATE_LARGE(),   placeholder("cd7f32", 128, 128))
        // Component icons
        skin.addDrawableIfMissing(Drawables.ICON_COMPONENT_COPPER_WIRE_SMALL(),    placeholder("d4701a", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_COMPONENT_COPPER_WIRE_MEDIUM(),   placeholder("d4701a", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_COMPONENT_COPPER_WIRE_LARGE(),    placeholder("d4701a", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_COMPONENT_IRON_GEAR_SMALL(),      placeholder("b0b8c8", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_COMPONENT_IRON_GEAR_MEDIUM(),     placeholder("b0b8c8", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_COMPONENT_IRON_GEAR_LARGE(),      placeholder("b0b8c8", 128, 128))
        // Science pack icons
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_RED_SMALL(),    placeholder("cc2020", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_RED_MEDIUM(),   placeholder("cc2020", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_RED_LARGE(),    placeholder("cc2020", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_ORANGE_SMALL(), placeholder("cc6600", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_ORANGE_MEDIUM(),placeholder("cc6600", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_ORANGE_LARGE(), placeholder("cc6600", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_YELLOW_SMALL(), placeholder("cccc00", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_YELLOW_MEDIUM(),placeholder("cccc00", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_YELLOW_LARGE(), placeholder("cccc00", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_GREEN_SMALL(),  placeholder("20cc20", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_GREEN_MEDIUM(), placeholder("20cc20", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_GREEN_LARGE(),  placeholder("20cc20", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_BLUE_SMALL(),   placeholder("2060cc", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_BLUE_MEDIUM(),  placeholder("2060cc", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_BLUE_LARGE(),   placeholder("2060cc", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_PURPLE_SMALL(), placeholder("8020cc", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_PURPLE_MEDIUM(),placeholder("8020cc", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_SCIENCE_PURPLE_LARGE(), placeholder("8020cc", 128, 128))
        // Building icons (small = 32px, medium = 64px, large = 128px)
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_STONE_FURNACE_SMALL(),        placeholder("555555", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_STONE_FURNACE_MEDIUM(),       placeholder("555555", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_STONE_FURNACE_LARGE(),        placeholder("555555", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_BASIC_MINER_SMALL(),          placeholder("666666", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_BASIC_MINER_MEDIUM(),         placeholder("666666", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_BASIC_MINER_LARGE(),          placeholder("666666", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_ASSEMBLER_MK1_SMALL(),        placeholder("4488aa", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_ASSEMBLER_MK1_MEDIUM(),       placeholder("4488aa", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_ASSEMBLER_MK1_LARGE(),        placeholder("4488aa", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_RESEARCH_FACILITY_SMALL(),    placeholder("8844cc", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_RESEARCH_FACILITY_MEDIUM(),   placeholder("8844cc", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_RESEARCH_FACILITY_LARGE(),    placeholder("8844cc", 128, 128))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_MINER_MK1_SMALL(),            placeholder("559955", 32,  32))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_MINER_MK1_MEDIUM(),           placeholder("559955", 64,  64))
        skin.addDrawableIfMissing(Drawables.ICON_BUILDING_MINER_MK1_LARGE(),            placeholder("559955", 128, 128))
    }

    private fun addButtonStyles(skin: Skin) {
        val bodyFont  = skin.getFont(Fonts.BODY.skinKey)
        val textColor = Color.valueOf("dde0e8")
        val dimColor  = Color.valueOf("404450")

        skin.add(Buttons.DEFAULT(), TextButtonStyle().apply {
            up                = skin.getDrawable(Drawables.BUTTON_DEFAULT_UP())
            over              = skin.getDrawable(Drawables.BUTTON_DEFAULT_OVER())
            down              = skin.getDrawable(Drawables.BUTTON_DEFAULT_DOWN())
            checked           = skin.getDrawable(Drawables.BUTTON_DEFAULT_DOWN())
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
