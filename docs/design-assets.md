# FactoryIdle — Asset Design & Pipeline

Covers: color palette, nine-patch guide, full asset list with sizes and colors, font pipeline, Fonts enum, atlas organization, texture filtering.

---

## Color Palette

| Name | Hex | Use |
|---|---|---|
| **Background** | `#1a1a1f` | Stage background, darkest layer |
| **Panel** | `#22242a` | Standard panel backgrounds |
| **Panel Dark** | `#1a1c22` | Recessed / inset panels |
| **Panel Inset** | `#18191e` | Deepest inset, input fields |
| **Border** | `#3a3d47` | Standard borders |
| **Border Subtle** | `#2e3038` | Low-contrast borders |
| **Border Highlight** | `#4a4f5e` | Active / focused borders |
| **Accent** | `#e8a020` | Primary accent (amber/orange — Factorio-ish) |
| **Accent Dark** | `#8a5a10` | Accent button background |
| **Text Primary** | `#dde0e8` | Main readable text |
| **Text Dim** | `#7a8090` | Secondary / muted text |
| **Text Disabled** | `#404450` | Greyed-out labels |
| **State Running** | `#27ae60` | RUNNING group state |
| **State Stalled** | `#f39c12` | STALLED group state |
| **State Fuel** | `#e67e22` | FUEL_STARVED group state |
| **State Paused** | `#c0392b` | PAUSED group state |
| **State Idle** | `#7a8090` | NO_RECIPE group state |
| **Danger** | `#c0392b` | Destructive actions |

---

## Nine-Patch Guide

### Why Small is Correct

LibGDX sets a NinePatchDrawable's **preferred size** to its native texture dimensions. A 200×80px nine-patch wants to render at 200×80 by default. If you don't constrain the cell, it will. Large native sizes are the root cause of nine-patches "shooting off screen."

**Keep nine-patches as small as possible.** The center stretch region can be 1–2px and stretch to any size. The corner size should match the actual corner artwork (typically the border radius in pixels).

```
Minimum correct size = left_corner + 1px_center + right_corner
                     = top_corner  + 1px_center + bottom_corner
```

A 4px rounded-corner button: `4 + 1 + 4 = 9×9px minimum.` This nine-patch has prefWidth=9, prefHeight=9 and scales cleanly to any button size.

### Nine-patches cannot reliably shrink below minWidth/minHeight
`minWidth = left_border + right_border`. `minHeight = top_border + bottom_border`. Never attempt to render a nine-patch smaller than its corner sizes.

### File naming
Name files with `.9` before the extension: `btn_default_up.9.png`. LibGDX TexturePacker auto-detects the split lines from this convention.

---

## Full Asset List

All assets go into a single texture atlas (`ui.atlas`). One atlas = one texture bind for all UI rendering.

---

### Button Nine-Patches

All button nine-patches: **48×48px, 12px corner split all sides.**
*(Inner fill rectangle: 46×46px at position 1,1 — Inkscape rx outer: 12px, inner: 11px)*

#### Default Buttons (general use: build, confirm, actions)
| Asset | Background | Border |
|---|---|---|
| `btn_default_up` | `#2d303a` | `#4a4f5e` |
| `btn_default_over` | `#363a47` | `#5a6070` |
| `btn_default_down` | `#1e2028` | `#4a4f5e` |
| `btn_default_disabled` | `#22242a` | `#2e3038` |

#### Accent Buttons (primary CTA: first build, research start)
| Asset | Background | Border |
|---|---|---|
| `btn_accent_up` | `#8a5a10` | `#e8a020` |
| `btn_accent_over` | `#a06a14` | `#f0b030` |
| `btn_accent_down` | `#6a4408` | `#c88a18` |
| `btn_accent_disabled` | `#2e2a22` | `#4a4030` |

#### Danger Buttons (demolish, disband, cancel)
| Asset | Background | Border |
|---|---|---|
| `btn_danger_up` | `#4a1a1a` | `#c0392b` |
| `btn_danger_over` | `#5a2020` | `#e04030` |
| `btn_danger_down` | `#341212` | `#a02020` |
| `btn_danger_disabled` | `#28201e` | `#402828` |

#### Nav Buttons (sidebar icon buttons) — **48×48px, 12px corner split**
*(Same dimensions as regular buttons — nav buttons display at similar screen size)*
| Asset | Background | Border |
|---|---|---|
| `btn_nav_up` | `#22242a` | `#2e3038` (very subtle, nearly invisible) |
| `btn_nav_over` | `#2d303a` | `#4a4f5e` |
| `btn_nav_down` | `#1a1c22` | `#3a3d47` |
| `btn_nav_selected` | `#1e2028` | `#e8a020` (accent border — active tab) |

---

### Panel Nine-Patches — **32×32px, 8px corner split**
*(Inner fill rectangle: 30×30px at position 1,1 — Inkscape rx outer: 8px, inner: 7px)*

| Asset | Background | Border |
|---|---|---|
| `panel_bg` | `#22242a` | `#3a3d47` |
| `panel_dark` | `#1a1c22` | `#2e3038` |
| `panel_inset` | `#18191e` | `#2a2c34` |
| `tooltip_bg` | `#12131a` | `#5a6070` |

#### Resource Bar Background — **24×24px, 6px corner split**
*(Inner fill: 22×22px at 1,1 — Inkscape rx outer: 6px, inner: 5px)*
| Asset | Background | Border |
|---|---|---|
| `resource_bar_bg` | `#1a1a1f` | `#3a3d47` |

---

### Group Card Backgrounds — **48×48px, 12px corner split, 2px border baked in**

Dark fill is identical across all five — only the border color changes.
*(Outer rect: 48×48px rx 12px — Inner fill rect: 44×44px at position 2,2 — rx: 10px)*

| Asset | Fill | Border Color | State |
|---|---|---|---|
| `card_bg_running` | `#22242a` | `#27ae60` | RUNNING |
| `card_bg_stalled` | `#22242a` | `#f39c12` | STALLED |
| `card_bg_fuel_starved` | `#22242a` | `#e67e22` | FUEL_STARVED |
| `card_bg_paused` | `#22242a` | `#c0392b` | PAUSED |
| `card_bg_idle` | `#22242a` | `#7a8090` | NO_RECIPE |

---

### Progress Bars

Fill nine-patches are slightly smaller than the track to create a visible inset.

#### Track — **24×24px, 6px corner split**
*(Inner fill: 22×22px at 1,1 — Inkscape rx outer: 6px, inner: 5px)*
| Asset | Fill | Border |
|---|---|---|
| `progress_track` | `#18191e` | `#2a2c34` |

#### Fills — **20×20px, 5px corner split**
*(Solid fill only — no border rectangle needed, Inkscape rx: 5px)*
| Asset | Color | Use |
|---|---|---|
| `progress_fill_green` | `#27ae60` | General production progress |
| `progress_fill_amber` | `#e8a020` | Mining cycle, construction timer |
| `progress_fill_blue` | `#2980b9` | Research progress |
| `progress_fill_red` | `#c0392b` | Fuel level (low warning) |

---

### Nine-Patch Quick Reference (Inkscape)

| Asset Group | Texture Size | Corner Split | rx outer | rx inner | Inner rect position |
|---|---|---|---|---|---|
| All buttons (incl. nav) | 48×48px | 12px | 12px | 11px | 46×46 at (1,1) |
| Panels | 32×32px | 8px | 8px | 7px | 30×30 at (1,1) |
| Resource bar bg | 24×24px | 6px | 6px | 5px | 22×22 at (1,1) |
| Card backgrounds | 48×48px | 12px | 12px | 10px | 44×44 at (2,2) — 2px border |
| Progress track | 24×24px | 6px | 6px | 5px | 22×22 at (1,1) |
| Progress fills | 20×20px | 5px | 5px | n/a | solid fill only |

---

### Flat Solid Regions (1×1px — no nine-patch)

Single pixels packed into the atlas. Used as `Image` actors with `fillX`/`fillY` for dividers and overlays.

| Asset | Color | Use |
|---|---|---|
| `px_divider` | `#3a3d47` | Horizontal / vertical separators |
| `px_white` | `#ffffff` | Tinted at runtime to any color |
| `px_black` | `#000000` | Dim overlays, drop shadows |

---

### Status Indicator Dots (12×12px, circular or rounded square)

| Asset | Color | State |
|---|---|---|
| `status_running` | `#27ae60` | RUNNING |
| `status_stalled` | `#f39c12` | STALLED |
| `status_fuel_starved` | `#e67e22` | FUEL_STARVED |
| `status_paused` | `#c0392b` | PAUSED |
| `status_idle` | `#7a8090` | NO_RECIPE |

---

### Navigation Icons (32×32px each)

Clean, readable icons — not pixel art. Simple shapes with the game's color palette.

| Asset | Color Hint |
|---|---|
| `icon_nav_factory` | Grey wrench or building silhouette |
| `icon_nav_power` | Amber lightning bolt |
| `icon_nav_research` | Blue flask or circuit board |
| `icon_nav_progress` | Green checkmark or star |
| `icon_nav_settings` | Grey gear |

---

### Resource Icons (two sizes — pre-scale in GIMP, do not use originals in atlas)

#### Small (20×20px) — resource bar, dense lists
| Asset | Color Hint |
|---|---|
| `icon_rsc_iron_ore_sm` | Brown/rust chunky rock |
| `icon_rsc_coal_sm` | Near-black lump |
| `icon_rsc_stone_sm` | Medium grey block |
| `icon_rsc_iron_plate_sm` | Silver flat bar |

#### Medium (36×36px) — recipe panels, assignment selectors
| Asset | Color Hint |
|---|---|
| `icon_rsc_iron_ore_md` | Same as small, larger |
| `icon_rsc_coal_md` | |
| `icon_rsc_stone_md` | |
| `icon_rsc_iron_plate_md` | |

**Scaling note:** 36px → 48px is a 1.33× scale-up and looks fine with Linear filtering. Do not use original large source files in the atlas — pre-scale to these target sizes in GIMP using Lanczos downsampling.

---

### Building Type Icons (32×32px — nav menus, build menu)
| Asset | Color Hint |
|---|---|
| `icon_bld_stone_furnace` | Dark stone body, orange glow |
| `icon_bld_basic_miner` | Grey machine, drill tip |

### Building Type Art (64×64px — group card background image)
| Asset | Notes |
|---|---|
| `bld_art_stone_furnace` | Larger detailed version for card display |
| `bld_art_basic_miner` | Larger detailed version for card display |

---

## Texture Filtering

LibGDX defaults to `Nearest` filtering (sharp, pixelated scaling). Since the game targets a non-pixel art look, set Linear filtering on the atlas texture after loading:

```kotlin
atlas.textures.forEach {
    it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
}
```

Do this wherever the atlas is loaded (asset manager callback or directly after `TextureAtlas(...)` call). Linear filtering makes icon scaling smooth and readable at all sizes.

---

## Font Pipeline (Hiero → Atlas → BitmapFont)

### How It Works
1. **Hiero** (bundled with LibGDX tools) generates two files per font: a `.fnt` descriptor and a `.png` glyph texture
2. **TexturePacker** packs the font `.png` into the atlas alongside all other assets
3. The region name in the atlas must match the `atlasRegionKey` in the Fonts enum — name font PNGs clearly (e.g. `font_body.png` → region `font_body`)
4. At runtime, `BitmapFont(fntFile, atlasRegion)` loads character data from `.fnt` and uses the atlas region instead of a separate `.png` — one atlas bind covers all text and all UI

### Hiero Settings
- **Render mode:** Java2D (cleaner edges at small sizes than LWJGL)
- **Font:** a clean sans-serif — Roboto, Inter, or Exo 2 (slight industrial feel) are good free choices
- **Padding:** set equal to your largest effect radius. No effects = 2px all sides is safe
- **Effects:** subtle drop shadow at 1px offset for heading fonts; nothing for body/small
- **Bold:** export as a separate font file using Hiero's Bold checkbox, not synthetic bold

### Font Sizes and Roles
| Enum Name | Role | Hiero Size | Notes |
|---|---|---|---|
| `HEADING` | Section titles, panel headers | 20px | Subtle shadow optional |
| `BODY` | Button text, labels, resource names | 15px | The workhorse font |
| `BODY_BOLD` | Resource amounts, emphasis | 15px | Bold variant of BODY |
| `SMALL` | Secondary info, rates, tooltips | 12px | Must be legible at small size |
| `MONO` | Numbers that must not shift width | 15px | Use a monospaced typeface |

### Loading Code Pattern
```kotlin
enum class Fonts(
    val skinKey: String,
    val fontPath: String,
    val atlasRegionKey: String,
    val scaling: Float = 1f
) {
    HEADING   ("font_heading",   "fonts/heading.fnt",   "font_heading",   1f),
    BODY      ("font_body",      "fonts/body.fnt",      "font_body",      1f),
    BODY_BOLD ("font_body_bold", "fonts/body_bold.fnt", "font_body_bold", 1f),
    SMALL     ("font_small",     "fonts/small.fnt",     "font_small",     1f),
    MONO      ("font_mono",      "fonts/mono.fnt",      "font_mono",      1f),
}

private fun loadFonts(skin: Skin) {
    Fonts.entries.forEach { font ->
        skin[font.skinKey] = BitmapFont(
            Gdx.files.internal(font.fontPath),
            skin.getRegion(font.atlasRegionKey)
        ).apply {
            data.setScale(font.scaling)
            data.markupEnabled = true    // enables [#e8a020]colored text[]
        }
    }
}

operator fun Skin.get(font: Fonts): BitmapFont = this.getFont(font.skinKey)
```

### markupEnabled
Setting `data.markupEnabled = true` allows inline color markup in any string:
- `[#e8a020]42[]` renders "42" in accent amber
- `[RED]warning[]` uses named colors
- Useful for coloring resource amounts, stall warnings, rate values without separate label widgets

---

## Atlas Organization

All assets live in a single `ui.atlas` file in `assets/`. Suggested subfolder grouping in your source art directory (TexturePacker will flatten these into one atlas):

```
assets-src/
  buttons/        btn_default_up.9.png, btn_accent_over.9.png, etc.
  panels/         panel_bg.9.png, card_bg_running.9.png, etc.
  progress/       progress_track.9.png, progress_fill_green.9.png, etc.
  icons/nav/      icon_nav_factory.png, etc.
  icons/resource/ icon_rsc_iron_ore_sm.png, icon_rsc_iron_ore_md.png, etc.
  icons/building/ icon_bld_stone_furnace.png, etc.
  art/            bld_art_stone_furnace.png, bld_art_basic_miner.png
  status/         status_running.png, status_stalled.png, etc.
  pixels/         px_divider.png, px_white.png, px_black.png
  fonts/          font_body.png, font_heading.png, etc. (from Hiero output)
```

TexturePacker page size should be at least 2048×2048 to fit everything on one page. Padding of 2px between regions prevents bleeding artifacts.
