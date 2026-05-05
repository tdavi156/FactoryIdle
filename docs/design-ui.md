# FactoryIdle — UI Design

Covers: navigation structure, all views, resource bar, factory view, group cards, MVC pattern, GameScreen structure, Scene2D layout rules.

---

## Overall Layout — 1440×900 Desktop

```
┌──────────────────────────────────────────────────────┐
│  Resource Bar (fixed top, always visible, ~52px tall) │
├──────┬───────────────────────────────────────────────┤
│      │                                               │
│ Nav  │   Content Area                                │
│ Side │   (Stack — one view visible at a time)        │
│ bar  │                                               │
│ ~64px│                                               │
└──────┴───────────────────────────────────────────────┘
```

Root table uses `setFillParent(true)`. Resource bar spans full width (colspan 2 or in its own row before the horizontal split). Nav sidebar and content stack sit side by side in the second row, both `expand().fill()`.

---

## Navigation Sidebar

- Vertical icon column, ~64px wide, full height below the resource bar
- One icon button per top-level view
- `button_nav_*` nine-patch styles for button states
- Active view uses `button_nav_selected` style (accent-colored border)
- Clicking a nav button hides the current content view and shows the selected one via `NavigationModel`

### Navigation Views (in order)
| Icon | View | Purpose |
|---|---|---|
| Factory icon | Factory | Build menu + group card grid — most used |
| Power icon | Power | Power network monitoring |
| Research icon | Research | Active goal, progress, tech tree |
| Progress icon | Progress | Milestones + Achievements (tabbed) |
| Settings icon | Settings | Game settings |

Hand mining is **not** a nav view — it lives as a persistent widget in the resource bar.

---

## Resource Bar

Fixed top strip, always visible regardless of which nav view is active.

### Left side — Hand Mining Widget
- One button per unlocked RAW resource
- Each button shows resource icon + resource name
- Progress bar below (or within) each button showing current 2s mining cycle
- Buttons are always pressable (never greyed out — hand mining is always the escape hatch)

### Right side — Resource Display
- Resources grouped by category: RAW / PROCESSED / INTERMEDIATE / SCIENCE
- Each category is collapsible (player preference)
- Each resource entry shows: small icon + name + amount or rate (see toggle below)
- Categories only appear if they have at least one visible resource

### Display Mode Toggle
- A small toggle button on the resource bar switches between two display modes:
  - **Count mode:** `Iron Ore  247`
  - **Rate mode:** `Iron Ore  +12.4/min`
- Rate is calculated using a rolling window (~10 seconds of samples × 6) — not a strict per-minute snapshot. This keeps the number stable and readable.
- Toggle lives on the bar itself — not buried in Settings

### Resource Visibility Rules
- RAW: always show once unlocked, even at zero
- PROCESSED / INTERMEDIATE / SCIENCE: show only if unlocked AND (quantity > 0 OR actively produced or consumed by any building)
- Resources the player has never encountered stay hidden — the bar grows progressively

---

## Factory View

The most-used view. Split into two panels side by side.

### Left Panel — Build Menu (~280px wide)
- Lists all unlocked building types
- Each entry shows: building icon, name, cost breakdown, construction time
- If player cannot afford: entry is visually greyed (cost text in red, button disabled)
- If construction queue is active: shows queue position and remaining time
- Clicking an affordable building opens a confirmation or starts construction directly (TBD during implementation)
- Unassigned pool count shown per building type: "Assemblers: 47 unassigned"

### Right Panel — Group Card Grid (fills remaining width)
- Scrollable grid of BuildingGroup cards
- Cards laid out in rows, consistent card size
- "New Group" button per building type (appears above that type's cards or as a dedicated card slot)

---

## Group Card Design

Each card is a fixed-size widget (e.g. 160×180px — tune during implementation).

```
┌─────────────────────┐  ← card_bg_[state].9 border reflects GroupState color
│                     │
│   [building art]    │  ← 64×64px building type image, centered
│      [icon]         │  ← 36×36px recipe/resource icon overlay
│                     │
│ ● "Iron Plates #1"  │  ← status dot (12×12) + group name label
│ [■■■■□□] pri: High  │  ← optional: buffer fill + priority indicator
│  ●  70 buildings    │  ← status dot bottom-left, count bottom-right
└─────────────────────┘
```

**Card elements (bottom to top, front to back in Scene2D Stack):**
1. `card_bg_[state].9` — background with state-colored border (fills card)
2. Building type art image (centered, ~64×64)
3. Recipe/resource icon overlay (centered, ~36×36)
4. Group name label (left-aligned, body font)
5. Status dot (bottom-left, 12×12px, same color as border)
6. Building count label (bottom-right, bold font, white with drop shadow)
7. Optional: small priority label or buffer fill bar

**Card states → border + dot color:**
- RUNNING → `#27ae60` green
- STALLED → `#f39c12` yellow
- FUEL_STARVED → `#e67e22` orange
- PAUSED → `#c0392b` red
- NO_RECIPE → `#7a8090` grey

Clicking a card opens the Group Detail view (could be a panel that slides in from the right, or an overlay dialog — TBD during implementation).

---

## Group Detail View

Opens when a group card is tapped. Shows all management controls for one group.

**Header:**
- Group name (editable inline — tap to rename)
- Building type label
- Current GroupState indicator (colored dot + text)

**Stats section:**
- Building count in group
- Current recipe/resource assigned
- Effective production rate (count × base rate)
- Buffer fill level (progress bar per input resource)
- Priority stepper: `[◀]  Normal  [▶]`

**Action buttons:**
- `+ Add` / `− Remove` (building count adjusters)
- `Quick Fill` (move all unassigned of this type into group)
- `Change Recipe` (opens recipe picker, with flush warning)
- `Pause` / `Unpause` toggle
- `Split` (opens count input → recipe for new group or return to pool)
- `Merge` (available if another group of same type is selected)
- `Disband` (with confirmation)

---

## Other Views (brief — not yet implemented)

### Power View
Monitoring panel only. No building construction here — power buildings are built in the Factory view like everything else. Shows: total power production vs. consumption, net balance, list of power-stalled buildings by group.

### Research View
- Active research goal shown prominently with progress bar
- Science pack consumption rate displayed
- Tech tree or tiered list of available/locked research goals
- Player clicks a goal to set it as active

### Progress View
Two tabs:
- **Milestones** — one-time triggers from the milestone chain; shows completed (greyed) and pending (active) with condition descriptions
- **Achievements** — optional side-goals; same visual treatment

### Settings View
Standard game settings (resolution, vsync, audio, etc.)

---

## MVC Pattern

All UI follows a lightweight MVC-inspired pattern.

### Models (`ui/models/`)
- Extend `PropertyChangeSource`
- Listen to ECS events (or poll global state) to detect changes
- Notify bound views via `onPropertyChange` callbacks
- Hold no Scene2D references — pure data and logic

### Views (`ui/views/`)
- Extend Scene2D `Table`
- Bind to model properties in `init` block
- Never query ECS or global state directly — always go through the model
- Responsible only for layout and display

### Skin (`ui/Skin.kt`)
- Enums for type-safe asset references: `Buttons`, `Labels`, `Drawables`, `Fonts`
- Built programmatically using KTX style DSL (no JSON skin files)
- `Scene2DSkin.defaultSkin` set once in `create()` before any UI is built
- All views use the default skin implicitly via KTX DSL

---

## GameScreen Structure

```kotlin
// Class-level properties — constructed before stage.actors { }
private val navigationModel  = NavigationModel()
private val resourceBarModel = ResourceBarModel(entityWorld)
private val factoryModel     = FactoryModel(entityWorld)
// ... one model per view

private val resourceBarView  = ResourceBarView(resourceBarModel)
private val factoryView      = FactoryView(factoryModel)
private val powerView        = PowerView(...)
private val researchView     = ResearchView(...)
private val progressView     = ProgressView(...)
private val settingsView     = SettingsView(...)

init {
    navigationModel.register(factoryView, powerView, researchView, progressView, settingsView)

    stage.actors {
        table {
            setFillParent(true)

            // Resource bar — full width
            add(resourceBarView).expandX().fillX().height(RESOURCE_BAR_HEIGHT)
            row()

            // Horizontal split: nav sidebar + content stack
            table { cell ->
                cell.expand().fill()

                add(navView(navigationModel)).fillY().width(NAV_WIDTH)

                stack { stackCell ->
                    add(factoryView)
                    add(powerView)
                    add(researchView)
                    add(progressView)
                    add(settingsView)
                    stackCell.expand().fill().prefWidth(0f).minWidth(0f)
                }
            }
        }
    }

    navigationModel.show(factoryView) // default tab
}
```

Key differences from the old pattern:
- All views are class-level properties — no nullable var declarations
- All models are class-level properties — not constructed inside the DSL block
- Navigation goes through `NavigationModel` — nav sidebar doesn't hold direct view references
- Stack cell has `prefWidth(0f).minWidth(0f)` to prevent layout being forced to largest child

---

## Scene2D Layout Rules (Expanded)

### The expand + fill mental model
- `expandX()` — "give this column all leftover horizontal space" (affects the cell, not the widget)
- `fillX()` — "stretch my widget to fill my cell width" (affects the widget inside the cell)
- They almost always go together. `fillX()` without `expandX()` does nothing useful — the cell has no extra space to fill. `expandX()` without `fillX()` makes a large cell but the widget floats at preferred size inside it.

### Sizing hierarchy (weakest → strongest)
```
prefWidth → minWidth → maxWidth → width() (sets all three)
```
- `prefWidth(x)` — "I'd like to be x wide" — overridable by layout
- `minWidth(x)` — hard floor, table never goes below this
- `maxWidth(x)` — hard ceiling
- `width(x)` — sets all three, effectively fixed size

### Stack cells
Always set `.prefWidth(0f).minWidth(0f)` on the stack cell. A Stack's preferred size equals its largest child. Without this, the stack forces the layout to accommodate its biggest view, which is rarely what you want.

### Only one expand per row/column needed
`expandX()` on one cell expands the entire column. Multiple cells with `expandX()` in the same column share the extra space proportionally. You only need it on one cell to make that column fill remaining space.

### Padding vs spacing
- Padding adds together between adjacent cells
- Spacing takes the larger of two adjacent values — use spacing for consistent gutters

### Colspan, no rowspan
`colspan(n)` works. Rowspan does not exist in Table — use a nested table to achieve rowspan-like layouts.

### Do not size widgets directly
`widget.setSize()` or `widget.setWidth()` will be overridden by the layout system. Always set sizes on the cell.

### `setFillParent(true)` on root table only
Using it on nested tables causes undefined behavior.

### Debug during layout work
```kotlin
stage.isDebugAll = true
```
Draws colored outlines around every cell and widget. Essential for understanding why something is the wrong size or in the wrong place. Remove before committing.

### Use ChangeListener, not ClickListener on buttons
ClickListener misses programmatic state changes and doesn't handle disabled correctly. ChangeListener fires for both input and programmatic changes.
