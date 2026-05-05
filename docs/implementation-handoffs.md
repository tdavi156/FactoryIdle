# FactoryIdle — Phase 1 Implementation Handoffs

Each section is a self-contained prompt for a new chat session. The project CLAUDE.md is auto-loaded as context in every session. Each prompt specifies which additional design docs to read. Always follow the workflow rule: present a plan and wait for explicit approval before writing any code.

Package root: `com.github.jacks.factoryIdle`
Source root: `core/src/main/kotlin/com/github/jacks/factoryIdle/`

---

---

## Step 1 — Data Layer

**Paste this into a new chat:**

---

I'm working on FactoryIdle, a LibGDX + Fleks ECS idle factory game. CLAUDE.md is loaded as project context — read it fully before starting. Also read `docs/design-systems.md` in full.

**What already exists:**
- `FactoryIdle.kt` — KtxGame<KtxScreen> entry point
- `screens/GameScreen.kt` — KtxScreen with an empty `configureWorld { }` stub and Scene2D stage

**Your task:** Create the entire data layer. No system logic, no UI, no LibGDX rendering. Pure Kotlin data definitions. Everything else in Phase 1 depends on this step being correct.

Create the following files under `core/src/main/kotlin/com/github/jacks/factoryIdle/`:

---

**`data/Enums.kt`**
- `ResourceCategory` enum: RAW, PROCESSED, INTERMEDIATE, SCIENCE
- `Resource` enum with `val category: ResourceCategory` constructor param. Phase 1 entries: IRON_ORE(RAW), COAL(RAW), STONE(RAW), IRON_PLATE(PROCESSED). Structured so adding a new resource is a single new enum entry — no other code changes.
- `BuildingType` enum. Phase 1 entries: STONE_FURNACE, BASIC_MINER. Same extensibility principle.
- `GroupState` enum: RUNNING, STALLED, FUEL_STARVED, PAUSED, NO_RECIPE
- `GroupPriority` enum: LOWEST, LOW, NORMAL, HIGH, HIGHEST

---

**`data/Components.kt`**
All ECS components exactly as defined in CLAUDE.md. Import Fleks annotations as needed. `Recipe` is a plain data class — not a component, not registered with ECS.

---

**`data/GlobalState.kt`**
Simple mutable wrappers that live outside ECS. Each wraps a map or set and exposes clean operations — do not expose the raw map directly.

- `GlobalResourcePool` — wraps `MutableMap<Resource, Float>`. Operations: `get(resource)`, `add(resource, amount)`, `subtract(resource, amount)` (floor at 0), `set(resource, amount)`, `has(resource, amount)` (returns Boolean).
- `LifetimeMiningStats` — wraps `MutableMap<Resource, Float>`. Operations: `add(resource, amount)`, `get(resource)`. Add-only, never subtract.
- `UnlockRegistry` — wraps `MutableSet<BuildingType>` and `MutableSet<Resource>` separately. Operations: `unlock(resource)`, `unlock(buildingType)`, `isUnlocked(resource)`, `isUnlocked(buildingType)`, `unlockedResources()`, `unlockedBuildingTypes()`.
- `UnassignedPool` — wraps `MutableMap<BuildingType, Int>`. Operations: `add(type, count)`, `remove(type, count)` (throws if insufficient), `count(type)`, `canRemove(type, count)`.

---

**`data/RecipeRegistry.kt`**
- `RecipeRegistry` — a `Map<BuildingType, List<Recipe>>` initialized with Phase 1 data:
  - STONE_FURNACE: `[Recipe(inputs = mapOf(IRON_ORE to 2f), outputs = mapOf(IRON_PLATE to 1f), duration = 5f)]`
  - BASIC_MINER: `emptyList()` — miners use `Miner.assignedResource`, not recipes
- Provide a `recipesFor(type: BuildingType): List<Recipe>` function
- This is data-driven: adding a new building type = new map entry, no new code paths

---

**`data/MilestoneDefinitions.kt`**
- `Milestone` data class: `id: String`, `description: String`, `condition: () -> Boolean`, `reward: () -> Unit`
- Top-level function: `buildPhase1Milestones(pool: GlobalResourcePool, stats: LifetimeMiningStats, unlocks: UnlockRegistry): List<Milestone>`

Returning these 5 milestones (conditions read from `stats`, rewards mutate `unlocks`):
1. id="start" — condition: always true — reward: unlock IRON_ORE resource
2. id="coal_unlock" — condition: stats.get(IRON_ORE) >= 10 — reward: unlock COAL resource
3. id="stone_unlock" — condition: stats.get(COAL) >= 10 && stats.get(IRON_ORE) >= 20 — reward: unlock STONE resource
4. id="furnace_unlock" — condition: stats.get(IRON_ORE) >= 30 && stats.get(COAL) >= 20 && stats.get(STONE) >= 10 — reward: unlock STONE_FURNACE building type
5. id="miner_unlock" — condition: stats.get(IRON_PLATE) >= 10 — reward: unlock BASIC_MINER building type

Note on milestone 5: IRON_PLATE should also be tracked in LifetimeMiningStats when produced by buildings (not just hand-mined). ProductionSystem will call `stats.add(IRON_PLATE, amount)` on each production cycle — LifetimeMiningStats tracks all resource gains, not just hand mining despite the name.

---

**Acceptance criteria:**
- `./gradlew core:compileKotlin` passes with zero errors
- All types match CLAUDE.md component definitions exactly
- No logic in this step — only data definitions and pure helper functions

---

---

## Step 2 — ECS Systems

**Paste this into a new chat:**

---

I'm working on FactoryIdle, a LibGDX + Fleks ECS idle factory game. CLAUDE.md is loaded as project context — read it fully. Also read `docs/design-systems.md` in full.

**What already exists:**
- `data/` package — all enums, components, global state classes, RecipeRegistry, MilestoneDefinitions (Step 1 complete)
- `screens/GameScreen.kt` — empty `configureWorld { }` stub, stage, show/render/dispose overrides

**Your task:** Implement all 5 ECS systems in the correct order and wire them into GameScreen. After this step, the game simulation runs invisibly — production happens but nothing displays it yet.

The system execution order is load-bearing. Do not reorder. See CLAUDE.md for the full order and rationale.

Create under `core/src/main/kotlin/com/github/jacks/factoryIdle/systems/`:

---

**`BufferFillSystem.kt`**

Iterates all entities with `ResourceBuffer`. For each resource in `buffer.capacity`, calculate shortage = capacity - current contents. Take `min(shortage, globalPool.get(resource))` and transfer that amount from global pool to buffer. Do not overfill.

Design note: In Phase 2 this system will sort entities by GroupPriority before iterating. Structure the system so that sort can be dropped in without a rewrite — even if Phase 1 has no groups, the iteration should be over a sortable collection.

Inject: `GlobalResourcePool`

---

**`ProductionSystem.kt`**

Iterates entities with `Producer`. Each tick:
1. If `producer.recipe == null`: set `producer.groupState = NO_RECIPE`, skip
2. Check if `ResourceBuffer.contents` has enough of each recipe input for one cycle. If not: set `STALLED`, skip
3. Advance `producer.progress += delta`
4. If `producer.progress >= recipe.duration`:
   - Consume inputs from `ResourceBuffer.contents`
   - Write each output to `GlobalResourcePool` (and `LifetimeMiningStats` — iron plates produced count toward the miner unlock milestone)
   - Reset `producer.progress = 0f`
   - Set `producer.groupState = RUNNING`

Inject: `GlobalResourcePool`, `LifetimeMiningStats`

---

**`MinerSystem.kt`**

Iterates entities with `Miner`. Each tick:
1. If `miner.assignedResource == null`: set `miner.groupState = NO_RECIPE`, skip
2. Check FuelConsumer if present — if `fuelBuffer <= 0`: set `FUEL_STARVED`, skip
3. Advance `miner.progress += delta`
4. If `miner.progress >= 4f` (4 seconds per unit):
   - Add 1f to `GlobalResourcePool` for `miner.assignedResource`
   - Add 1f to `LifetimeMiningStats` for `miner.assignedResource`
   - Reset `miner.progress = 0f`
   - Set `miner.groupState = RUNNING`

Inject: `GlobalResourcePool`, `LifetimeMiningStats`

---

**`FuelSystem.kt`**

Iterates entities with `FuelConsumer`. Each tick:
1. Drain: `fuelConsumer.fuelBuffer -= fuelConsumer.consumeRate * delta`
2. Clamp: `fuelConsumer.fuelBuffer = max(0f, fuelConsumer.fuelBuffer)`
3. If `fuelBuffer <= 0`: set FUEL_STARVED on any Producer or Miner component on this entity
4. Top up: if `fuelBuffer < maxFuelBuffer` (e.g. 6f) and global pool has the fuel resource: take `min(needed, available)` from global pool and add to `fuelBuffer`

Note: FuelSystem runs after ProductionSystem and MinerSystem. Its FUEL_STARVED setting may override a STALLED state set by production — this is correct. FUEL_STARVED is the more specific diagnosis.

Inject: `GlobalResourcePool`

---

**`MilestoneSystem.kt`**

Holds `private val pending: MutableList<Milestone>` initialized at construction time.

Each tick: iterate a copy of `pending`. For each milestone, call `milestone.condition()`. If true: call `milestone.reward()`, remove from `pending`. Milestones are never re-added once fired.

Constructor takes the pending list: `class MilestoneSystem(milestones: List<Milestone>)`

Inject: nothing (conditions and rewards are closures that already capture their dependencies)

---

**Wire into `GameScreen.kt`:**

Add class-level global state properties:
```kotlin
private val globalResourcePool  = GlobalResourcePool()
private val lifetimeMiningStats = LifetimeMiningStats()
private val unlockRegistry      = UnlockRegistry()
private val recipeRegistry      = RecipeRegistry()
```

Update `configureWorld { }`:
```kotlin
private val entityWorld: World = configureWorld {
    injectables {
        add(globalResourcePool)
        add(lifetimeMiningStats)
        add(unlockRegistry)
        add(recipeRegistry)
    }
    systems {
        add(BufferFillSystem())
        add(ProductionSystem())
        add(MinerSystem())
        add(FuelSystem())
        add(MilestoneSystem(buildPhase1Milestones(globalResourcePool, lifetimeMiningStats, unlockRegistry)))
    }
}
```

Update `GameScreen.render(delta: Float)` to call `entityWorld.update(delta)` before `stage.act(delta)`.

---

**Verification (temporary test code, remove before committing):**

In `GameScreen.show()`, after wiring, add a temporary test that:
1. Adds 100f IRON_ORE and 100f COAL to globalResourcePool
2. Creates a Stone Furnace entity with Producer (recipe set to iron plate recipe), FuelConsumer (coal, 1/30f rate), ResourceBuffer (capacity: IRON_ORE→6f, COAL→6f)
3. Logs globalResourcePool every 5 seconds

Confirm iron plates accumulate and coal depletes. Then delete the test code.

**Acceptance criteria:**
- `./gradlew core:compileKotlin` passes
- Test confirms production and fuel drain work correctly
- All test code removed before final commit

---

---

## Step 3 — Skin & Asset Foundation

**Paste this into a new chat:**

---

I'm working on FactoryIdle, a LibGDX + Fleks ECS idle factory game. CLAUDE.md is loaded as project context — read it fully. Also read `docs/design-assets.md` in full — it contains every asset name, size, color, and the complete font pipeline.

**What already exists:**
- `data/` package — all enums and global state (Step 1 complete)
- `FactoryIdle.kt` — KtxGame entry point
- `screens/GameScreen.kt` — KtxScreen

**Your task:** Set up the Skin system, define all style enums, and wire font loading. The skin must be fully functional before any UI can be built. If final art assets are not yet available, use solid-color placeholder drawables (LibGDX `Pixmap`-generated textures) — do not block on art. The enum names and style names must match `docs/design-assets.md` exactly so the UI steps can reference them without changes.

Create under `core/src/main/kotlin/com/github/jacks/factoryIdle/ui/`:

---

**`ui/Drawables.kt`**
Enum of all drawable asset names. Every nine-patch and icon in `docs/design-assets.md` gets an entry. These are the keys used to look up drawables in the skin.

```kotlin
enum class Drawables {
    // Buttons
    BUTTON_DEFAULT_UP, BUTTON_DEFAULT_OVER, BUTTON_DEFAULT_DOWN, BUTTON_DEFAULT_DISABLED,
    BUTTON_ACCENT_UP, BUTTON_ACCENT_OVER, BUTTON_ACCENT_DOWN, BUTTON_ACCENT_DISABLED,
    BUTTON_DANGER_UP, BUTTON_DANGER_OVER, BUTTON_DANGER_DOWN, BUTTON_DANGER_DISABLED,
    BUTTON_NAVIGATION_UP, BUTTON_NAVIGATION_OVER, BUTTON_NAVIGATION_DOWN, BUTTON_NAVIGATION_SELECTED,
    // Panels
    PANEL_BG, PANEL_DARK, PANEL_INSET, TOOLTIP_BG, RESOURCE_BAR_BG,
    // Cards
    CARD_BG_RUNNING, CARD_BG_STALLED, CARD_BG_FUEL_STARVED, CARD_BG_PAUSED, CARD_BG_IDLE,
    // Progress
    PROGRESS_TRACK, PROGRESS_FILL_GREEN, PROGRESS_FILL_AMBER, PROGRESS_FILL_BLUE, PROGRESS_FILL_RED,
    // Pixels
    PX_DIVIDER, PX_WHITE, PX_BLACK,
    // Status dots
    STATUS_RUNNING, STATUS_STALLED, STATUS_FUEL_STARVED, STATUS_PAUSED, STATUS_IDLE,
    // Navigation icons
    ICON_NAVIGATION_FACTORY, ICON_NAVIGATION_POWER, ICON_NAVIGATION_RESEARCH, ICON_NAVIGATION_PROGRESS, ICON_NAVIGATION_SETTINGS,
    // Resource icons (sm = 20px, md = 36px)
    ICON_RSC_IRON_ORE_SM, ICON_RSC_IRON_ORE_MD,
    ICON_RSC_COAL_SM, ICON_RSC_COAL_MD,
    ICON_RSC_STONE_SM, ICON_RSC_STONE_MD,
    ICON_RSC_IRON_PLATE_SM, ICON_RSC_IRON_PLATE_MD,
    // Building icons and art
    ICON_BLD_STONE_FURNACE, ICON_BLD_BASIC_MINER,
    BLD_ART_STONE_FURNACE, BLD_ART_BASIC_MINER,
}
```

---

**`ui/Buttons.kt`**
Enum of button style names used in the skin.

```kotlin
enum class Buttons {
    DEFAULT, ACCENT, DANGER, NAVIGATION;
    operator fun invoke() = name.lowercase()
}
```

---

**`ui/Labels.kt`**
Enum of label style names.

```kotlin
enum class Labels {
    HEADING, BODY, BODY_BOLD, SMALL, DIM;
    operator fun invoke() = name.lowercase()
}
```

---

**`ui/Fonts.kt`**
Font definitions and loading. See `docs/design-assets.md` for the full loading pattern.

```kotlin
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
```

Include the `loadFonts(skin: Skin)` function and `operator fun Skin.get(font: Fonts)` extension as shown in `docs/design-assets.md`. If font files are not yet available, create a fallback that uses `BitmapFont()` (the default LibGDX font) for all entries so the skin does not crash.

---

**`ui/GameSkin.kt`**
The skin builder. Responsible for:
1. Loading the texture atlas (if it exists at `assets/ui.atlas`) OR generating placeholder drawables via `Pixmap`
2. Setting Linear texture filtering on the atlas
3. Defining all button styles using KTX style DSL — each style references drawables by `Drawables` enum
4. Defining label styles (font + color per Labels enum)
5. Loading fonts via `loadFonts(skin)`
6. Setting `Scene2DSkin.defaultSkin = skin`

**Placeholder strategy (when atlas is not yet available):**
Generate solid-color `Pixmap` textures and add them to the skin manually. Use the colors from `docs/design-assets.md`. For example:
```kotlin
fun createPlaceholder(color: Color, width: Int = 16, height: Int = 16): TextureRegionDrawable {
    val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
    pixmap.setColor(color)
    pixmap.fill()
    val texture = Texture(pixmap)
    pixmap.dispose()
    return TextureRegionDrawable(texture)
}
```

Button styles must have pressedOffsetX = 1f, pressedOffsetY = -1f for the physical press feel.

---

**Wire into `FactoryIdle.kt`:**

In `create()`, before `addScreen(GameScreen(this))`:
```kotlin
GameSkin.initialize()   // builds skin and sets Scene2DSkin.defaultSkin
```

`GameSkin.initialize()` must be called before any screen or view is constructed.

---

**Acceptance criteria:**
- `./gradlew lwjgl3:run` launches without skin-related errors
- A test button (added temporarily to GameScreen.init) renders with visible up/over/down states
- All enum names match `docs/design-assets.md` exactly
- Test code removed before committing

---

---

## Step 4 — UI Shell

**Paste this into a new chat:**

---

I'm working on FactoryIdle, a LibGDX + Fleks ECS idle factory game. CLAUDE.md is loaded as project context — read it fully. Also read `docs/design-ui.md` in full — it contains the layout diagram, navigation structure, GameScreen init pattern, and Scene2D layout rules.

**What already exists:**
- `data/` package — all enums, components, global state (Step 1)
- `systems/` package — all 5 ECS systems (Step 2)
- `ui/` package — Skin, Drawables, Buttons, Labels, Fonts enums (Step 3)
- `screens/GameScreen.kt` — currently has a basic init; this step rewrites it

**Your task:** Build the UI shell — the root layout, navigation model, nav sidebar, and stub content views. After this step the game launches showing a working nav sidebar that switches between placeholder views. No real content yet.

Create under `core/src/main/kotlin/com/github/jacks/factoryIdle/`:

---

**`ui/PropertyChangeSource.kt`**
Base class for all UI models. Provides a simple observer pattern:
```kotlin
abstract class PropertyChangeSource {
    private val listeners = mutableListOf<() -> Unit>()
    fun addListener(listener: () -> Unit) { listeners.add(listener) }
    protected fun notifyListeners() { listeners.forEach { it() } }
}
```

---

**`ui/models/NavigationModel.kt`**
Manages which content view is currently visible. Holds references to all registered content views (as `Actor` or `Table`). Exposes `show(view: Table)` which sets all views invisible then sets the target visible. Views register themselves on construction or are passed in at GameScreen init time.

---

**`ui/views/NavSidebarView.kt`**
A `Table` subclass. Vertical column of icon buttons, one per nav destination. Uses `button_navigation_*` skin styles. The active view's button uses the `BUTTON_NAVIGATION_SELECTED` drawable. Clicking a button calls `navigationModel.show(targetView)` and updates the selected state. Icons use the `ICON_NAVIGATION_*` drawables.

---

**Stub content views (empty Tables with a centered label — real content in later steps):**
- `ui/views/FactoryView.kt` — label: "Factory" (Step 6 will replace the content)
- `ui/views/PowerView.kt` — label: "Power — Coming Soon"
- `ui/views/ResearchView.kt` — label: "Research — Coming Soon"
- `ui/views/ProgressView.kt` — label: "Progress — Coming Soon"
- `ui/views/SettingsView.kt` — label: "Settings — Coming Soon"

---

**Rewrite `screens/GameScreen.kt`** to the clean pattern from `docs/design-ui.md`:

```kotlin
class GameScreen(private val game: FactoryIdle) : KtxScreen {

    // Global state (used by models)
    private val globalResourcePool  = GlobalResourcePool()
    private val lifetimeMiningStats = LifetimeMiningStats()
    private val unlockRegistry      = UnlockRegistry()
    private val recipeRegistry      = RecipeRegistry()

    private val stage = Stage(ScreenViewport())

    private val entityWorld: World = configureWorld { /* ... as wired in Step 2 ... */ }

    // Navigation
    private val navigationModel = NavigationModel()

    // Content views (class-level — no nullable vars)
    private val factoryView  = FactoryView()
    private val powerView    = PowerView()
    private val researchView = ResearchView()
    private val progressView = ProgressView()
    private val settingsView = SettingsView()

    init {
        navigationModel.register(factoryView, powerView, researchView, progressView, settingsView)

        stage.actors {
            table {
                setFillParent(true)

                // Resource bar row — placeholder for Step 5
                row()

                // Main content row
                table { cell ->
                    cell.expand().fill()

                    add(NavSidebarView(navigationModel, factoryView, powerView, researchView, progressView, settingsView))
                        .fillY().width(64f)

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

        navigationModel.show(factoryView)
    }

    override fun show() { Gdx.input.inputProcessor = stage }
    override fun render(delta: Float) {
        entityWorld.update(delta)
        stage.act(delta)
        stage.draw()
    }
    override fun resize(width: Int, height: Int) { stage.viewport.update(width, height, true) }
    override fun dispose() { entityWorld.dispose(); stage.dispose() }

    companion object { val log = logger<FactoryIdle>() }
}
```

---

**Acceptance criteria:**
- `./gradlew lwjgl3:run` launches showing nav sidebar with 5 icon buttons
- Clicking each nav button switches the content area to the correct placeholder view
- `stage.isDebugAll = true` confirms layout fills 1440×900 correctly, stack cell is not forcing an unexpected size
- Debug rendering removed before committing

---

---

## Step 5 — Resource Bar & Hand Mining

**Paste this into a new chat:**

---

I'm working on FactoryIdle, a LibGDX + Fleks ECS idle factory game. CLAUDE.md is loaded as project context — read it fully. Also read `docs/design-ui.md` and `docs/design-systems.md` in full.

**What already exists:**
- `data/` package — all enums, global state, milestones (Step 1)
- `systems/` package — all 5 ECS systems running (Step 2)
- `ui/` package — Skin, enums, PropertyChangeSource (Step 3)
- `screens/GameScreen.kt` — full shell with nav sidebar and content stack (Step 4)
- Stub `FactoryView`, `PowerView`, etc. in place

**Your task:** Implement the resource bar (always-visible top strip) and hand mining widget. After this step, the player can hand-mine resources and watch them appear and accumulate in the resource bar.

---

**`ui/models/ResourceBarModel.kt`**

Extends `PropertyChangeSource`. Holds references to `GlobalResourcePool`, `LifetimeMiningStats`, `UnlockRegistry`.

Responsibilities:
- Exposes the list of currently visible resources per `ResourceCategory` (filtered by unlock and display rules from `docs/design-ui.md`)
- Calculates per-resource rates using a rolling window: maintain a `Map<Resource, ArrayDeque<Pair<Float, Float>>>` (timestamp, delta-amount). Each tick, add the latest observation, prune entries older than 10 seconds, multiply sum by 6 to get per-minute rate.
- Exposes `displayMode: DisplayMode` (COUNT or RATE) with a toggle function
- Calls `notifyListeners()` on meaningful changes (new resource visible, amount changes, rate changes)

```kotlin
enum class DisplayMode { COUNT, RATE }
```

---

**`ui/views/ResourceBarView.kt`**

A `Table` subclass. Displays the resource bar and hand mining widget side by side:
- Left side: `HandMiningWidget` (built below)
- Right side: resource display area — resources grouped by `ResourceCategory`, each shown as icon + name + amount/rate. Category headers are collapsible.
- Top-right: small toggle button switching `DisplayMode` on the model

Binds to `ResourceBarModel` in `init`. Adds a listener that calls `refresh()` when the model notifies changes. `refresh()` rebuilds the resource display from current model data without recreating the hand mining widget.

Height: ~52px. Spans full width.

---

**`ui/views/HandMiningWidget.kt`**

A `Table` subclass. One button per unlocked RAW resource. Each button:
- Shows the resource icon (small, 20px) and name
- Has a `ProgressBar` below it showing progress through the current 2s mining cycle
- On click: if not already mining this resource, starts the cycle
- On cycle complete (2s elapsed): adds 1f to `GlobalResourcePool` and `LifetimeMiningStats` for that resource, notifies `ResourceBarModel`, then idles (player must click again or it auto-idles)
- Multiple resources can have simultaneous active cycles (clicking coal while iron is mid-cycle is valid)

The widget observes `UnlockRegistry` (or is refreshed by `ResourceBarModel`) so it adds new buttons when Coal and Stone are unlocked via milestones.

---

**Wire into `GameScreen.kt`:**

Add class-level:
```kotlin
private val resourceBarModel = ResourceBarModel(globalResourcePool, lifetimeMiningStats, unlockRegistry)
private val resourceBarView  = ResourceBarView(resourceBarModel)
```

In `init`, add the resource bar to the root table before the main content row:
```kotlin
add(resourceBarView).expandX().fillX().height(52f)
row()
```

In `render()`, after `entityWorld.update(delta)`, tick the `ResourceBarModel` to update rates:
```kotlin
resourceBarModel.tick(delta)
stage.act(delta)
stage.draw()
```

---

**Acceptance criteria:**
- Resource bar is visible at the top on all nav views
- Iron Ore button appears on launch; clicking it mines 1 ore every 2 seconds (progress bar shows cycle)
- After 10 lifetime iron ore, Coal button appears without restart
- After 10 coal + 20 iron ore lifetime, Stone button appears
- Toggle button switches between count display (e.g. "47") and rate display (e.g. "+12.4/min")
- Rate values are stable (rolling window), not jumpy per-tick numbers

---

---

## Step 6 — Factory View

**Paste this into a new chat:**

---

I'm working on FactoryIdle, a LibGDX + Fleks ECS idle factory game. CLAUDE.md is loaded as project context — read it fully. Also read `docs/design-ui.md` and `docs/design-buildings.md` in full.

**What already exists:**
- All data, systems, skin, UI shell, resource bar, and hand mining (Steps 1–5 complete)
- `FactoryView.kt` is currently a stub with a placeholder label — this step replaces its content
- Building entities are created by `ConstructionManager` (Step 7) — for this step, wire the UI so it's ready to display whatever entities exist, and expose a `requestBuild(type)` callback that Step 7 will connect

**Your task:** Build the full Phase 1 Factory view — build menu on the left, building list on the right, and a detail panel for assigning recipes/resources.

---

**`ui/models/FactoryModel.kt`**

Extends `PropertyChangeSource`. Holds references to `GlobalResourcePool`, `UnlockRegistry`, `RecipeRegistry`, `UnassignedPool`, and the Fleks `World`.

Responsibilities:
- Exposes the list of unlocked building types with their costs and whether the player can currently afford them
- Exposes the list of all placed building entities with their current state (type, recipe/resource assignment, `GroupState`)
- Exposes construction queue state (current item and remaining time) — populated by `ConstructionManager` in Step 7; for now just expose an observable slot
- `requestBuild(type: BuildingType)` — callback slot that Step 7 wires up; call it from the build button
- `assignRecipe(entity: Entity, recipe: Recipe)` — sets the recipe on a Producer entity, notifies listeners
- `assignResource(entity: Entity, resource: Resource)` — sets the resource on a Miner entity, notifies listeners
- Calls `notifyListeners()` when any observable data changes

---

**`ui/views/FactoryView.kt`** (replace the stub)

A `Table` subclass. Two-panel horizontal layout:
- Left panel (~280px fixed width): `BuildMenuView`
- Right panel (fills remaining): `BuildingListView`

Both panels are created at class level and added to the table. Binds to `FactoryModel`.

---

**`ui/views/BuildMenuView.kt`**

A `Table` subclass inside a `ScrollPane`. For each unlocked `BuildingType`:
- Building icon (32px) + name label
- Cost breakdown (e.g. "5 Stone") in dim text
- Build button: active (default style) if affordable, disabled style if not
- Construction queue indicator below the button: if a building is currently being constructed, show a `ProgressBar` with remaining time and "Building... X of Y" text

Clicking an active Build button calls `factoryModel.requestBuild(type)`. The button greys out immediately after clicking (construction is now queued). Affordability re-checks each time the model notifies.

Building types appear only when `UnlockRegistry.isUnlocked(type)` is true. The view refreshes when the model notifies.

---

**`ui/views/BuildingListView.kt`**

A `Table` inside a `ScrollPane`. Displays all placed building entities from `FactoryModel`. Each row shows:
- Building type icon (32px)
- Building type name
- Assignment label: recipe name (for furnaces) or resource name (for miners), or "— Unassigned —" in dim text if nothing assigned
- `GroupState` status dot (12px, colored per state using the `STATUS_*` drawables)

Clicking a row opens `BuildingDetailPanel` for that entity. The list refreshes when the model notifies.

---

**`ui/views/BuildingDetailPanel.kt`**

An overlay `Table` (added to the stage at a high z-index, not to the content stack) that appears when a building row is clicked.

Contains:
- Building type name header
- If `Producer`: a scrollable list of available recipes from `RecipeRegistry.recipesFor(type)`. Each recipe shown as: output icon → output name, input icons + amounts, duration. Clicking a recipe calls `factoryModel.assignRecipe(entity, recipe)` and closes the panel.
- If `Miner`: a list of currently unlocked RAW resources from `UnlockRegistry`. Each shown as icon + name. Clicking calls `factoryModel.assignResource(entity, resource)` and closes the panel.
- A "Close" button (danger or default style)
- Dark semi-transparent background covering the content area

---

**Wire into `GameScreen.kt`:**
```kotlin
private val factoryModel = FactoryModel(globalResourcePool, unlockRegistry, recipeRegistry, unassignedPool, entityWorld)
private val factoryView  = FactoryView(factoryModel)
```

Replace the stub `FactoryView()` with `factoryView`. Add the detail panel actor to the stage directly (not the content stack) so it overlays everything.

---

**Acceptance criteria:**
- Factory view shows build menu on the left and an empty building list on the right
- Stone Furnace entry appears in build menu after furnace milestone fires; is greyed until player has 5 stone
- Building list shows any entities created manually in code (for testing — actual construction wired in Step 7)
- Clicking a building row opens the detail panel; selecting a recipe/resource assigns it and closes the panel
- Status dots update in real time as entities change state

---

---

## Step 7 — Construction System

**Paste this into a new chat:**

---

I'm working on FactoryIdle, a LibGDX + Fleks ECS idle factory game. CLAUDE.md is loaded as project context — read it fully. Also read `docs/design-systems.md` (Construction section) and `docs/design-buildings.md` in full.

**What already exists:**
- All data, systems, skin, and UI (Steps 1–6 complete)
- `FactoryModel.requestBuild(type)` is a callback slot waiting to be wired
- `FactoryModel` observes a construction queue state that is currently empty

**Your task:** Implement the construction queue and manager that connect the Build button to actual ECS entity creation.

---

**`construction/ConstructionEntry.kt`**
```kotlin
data class ConstructionEntry(
    val type: BuildingType,
    val totalTime: Float,
    var remainingTime: Float
)
```

Construction times (tune during testing — these are starting values):
```kotlin
val BuildingType.constructionTime: Float get() = when (this) {
    BuildingType.STONE_FURNACE -> 10f
    BuildingType.BASIC_MINER   -> 15f
}
```

---

**`construction/ConstructionQueue.kt`**

A simple sequential queue wrapping `ArrayDeque<ConstructionEntry>`.
- `enqueue(type: BuildingType)` — adds entry with full time
- `peek(): ConstructionEntry?` — returns head without removing
- `tick(delta: Float): ConstructionEntry?` — advances head's `remainingTime`; if <= 0, removes and returns the completed entry; otherwise returns null
- `isEmpty(): Boolean`
- Observable: accepts a `onChange: () -> Unit` callback called on any state change

---

**`construction/ConstructionManager.kt`**

Owns a `ConstructionQueue`. Holds references to `GlobalResourcePool`, `UnassignedPool`, and the Fleks `World`.

**`requestBuild(type: BuildingType): Boolean`**
- Look up cost for `type` (hardcoded Phase 1 costs: STONE_FURNACE = 5 stone, BASIC_MINER = 5 stone + 5 iron plates)
- Check affordability via `GlobalResourcePool.has(resource, amount)` for each cost item
- If affordable: deduct all costs immediately, enqueue the build, return true
- If not affordable: return false (UI already prevents this, but defensive check)

**`tick(delta: Float)`**
- Call `constructionQueue.tick(delta)`
- If a completed entry is returned: create the ECS entity in the Fleks world

**Entity creation for completed builds:**

```kotlin
private fun createEntity(type: BuildingType) {
    entityWorld.entity {
        it += Building(type)
        when (type) {
            STONE_FURNACE -> {
                it += Producer()
                it += FuelConsumer(fuelType = COAL, consumeRate = 1f / 30f, fuelBuffer = 0f)
                it += ResourceBuffer(capacity = mapOf(IRON_ORE to 6f, COAL to 6f))
            }
            BASIC_MINER -> {
                it += Miner()
                it += FuelConsumer(fuelType = COAL, consumeRate = 1f / 30f, fuelBuffer = 0f)
                it += ResourceBuffer(capacity = mapOf(COAL to 6f))
            }
        }
    }
    unassignedPool.add(type, 1)
}
```

Entity configuration is data-driven per BuildingType — adding a new building type = new `when` branch with its components, not a new system.

---

**Wire into `GameScreen.kt`:**

Add class-level:
```kotlin
private val constructionManager = ConstructionManager(globalResourcePool, unassignedPool, entityWorld)
```

Wire `FactoryModel.requestBuild`:
```kotlin
factoryModel.onRequestBuild = { type -> constructionManager.requestBuild(type) }
```

In `render()`, tick the manager:
```kotlin
constructionManager.tick(delta)
```

Wire the queue's `onChange` callback to notify `FactoryModel` so the build menu progress bar updates.

---

**Acceptance criteria:**
- Clicking Build deducts resources immediately if affordable
- Progress bar in build menu shows remaining construction time ticking down
- On completion, building appears in the building list
- Building list row is clickable and assignment panel opens
- Building produces correctly once recipe is assigned (verify via resource bar rates)
- Queue correctly prevents starting a second build while one is in progress (Phase 1 sequential)

---

---

## Step 8 — Save / Load

**Paste this into a new chat:**

---

I'm working on FactoryIdle, a LibGDX + Fleks ECS idle factory game. CLAUDE.md is loaded as project context — read it fully. Also read `docs/design-systems.md` (Save/Load section) in full.

**What already exists:**
- All systems, UI, construction — the full Phase 1 game is playable (Steps 1–7 complete)

**Your task:** Implement save and load so game state persists across sessions. Use kotlinx.serialization for JSON. Save on game exit and on a 60-second autosave timer. Load on startup and restore all state.

---

**`save/GameState.kt`**

A fully serializable snapshot of everything needed to restore a session:

```kotlin
@Serializable
data class GameState(
    val resourcePool: Map<String, Float>,              // Resource.name() -> amount
    val lifetimeStats: Map<String, Float>,             // Resource.name() -> lifetime total
    val unlockedResources: List<String>,               // Resource.name() list
    val unlockedBuildings: List<String>,               // BuildingType.name() list
    val unassignedPool: Map<String, Int>,              // BuildingType.name() -> count
    val placedBuildings: List<PlacedBuildingState>,
    val constructionQueue: List<ConstructionEntryState>,
    val firedMilestoneIds: List<String>,
    val version: Int = 1                               // for future migration
)

@Serializable
data class PlacedBuildingState(
    val type: String,                   // BuildingType.name()
    val assignedRecipe: RecipeState?,   // null if unassigned
    val assignedResource: String?,      // Resource.name(), null if unassigned
    val progress: Float,
    val fuelBuffer: Float,
    val bufferContents: Map<String, Float>
)

@Serializable
data class RecipeState(
    val inputs: Map<String, Float>,
    val outputs: Map<String, Float>,
    val duration: Float
)

@Serializable
data class ConstructionEntryState(
    val type: String,
    val totalTime: Float,
    val remainingTime: Float
)
```

---

**`save/SaveManager.kt`**

Handles serialization and file I/O.

```kotlin
object SaveManager {
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }
    private val saveFile = Gdx.files.local("save/game.json")

    fun save(state: GameState) { saveFile.writeString(json.encodeToString(state), false) }
    fun load(): GameState? = if (saveFile.exists()) json.decodeFromString(saveFile.readString()) else null
}
```

---

**`save/StateSerialization.kt`**

Two top-level functions to convert between live game state and `GameState`:

**`captureState(...): GameState`** — reads from all global state objects and iterates Fleks world entities to build the serializable snapshot.

**`restoreState(state: GameState, ...)`** — writes all values back to global state objects and recreates ECS entities from `PlacedBuildingState` entries. Restores construction queue. Marks fired milestones as already-completed in `MilestoneSystem` (pass the fired ID set in so the system skips those conditions).

`MilestoneSystem` should accept a `Set<String>` of already-fired milestone IDs at construction time and skip those conditions immediately.

---

**Wire into `FactoryIdle.kt`:**

```kotlin
override fun create() {
    GameSkin.initialize()
    val screen = GameScreen(this)
    // Attempt load
    SaveManager.load()?.let { state ->
        restoreState(state, screen.globalResourcePool, screen.lifetimeStats, ...)
    }
    addScreen(screen)
    setScreen<GameScreen>()
}

override fun dispose() {
    val state = captureState(...)
    SaveManager.save(state)
    super.dispose()
}
```

Note: `GameScreen`'s global state properties will need to be `internal` or exposed via a getter so `FactoryIdle` can pass them to `restoreState`.

**Autosave in `GameScreen.render()`:**
```kotlin
private var autosaveTimer = 0f
// In render:
autosaveTimer += delta
if (autosaveTimer >= 60f) {
    SaveManager.save(captureState(...))
    autosaveTimer = 0f
}
```

---

**Acceptance criteria:**
- Play session (mine resources, build a furnace, assign recipe, watch production) → close game → reopen → all state restored exactly
- Mid-construction building resumes with correct remaining time
- Milestone state preserved (coal does not re-unlock if already unlocked)
- Autosave triggers at 60s intervals (verify via log)
- Save file is valid JSON and human-readable enough to debug

---
