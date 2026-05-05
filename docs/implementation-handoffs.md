# FactoryIdle — Implementation Handoff Prompts

Each section is a self-contained prompt for a new Claude chat. The implementing chat has no memory of prior sessions — every prompt includes full context. Steps must be completed in order unless marked as independent.

Steps 1, 3 are complete. Step 2 needs a full redo. Steps 4–15 are new.

---

## Step 2 Redo — ECS: Satisfaction Rate Model

You are implementing the core production simulation for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules, Fleks API notes, and the workflow rule you must follow.

**The game in one paragraph:** No spatial world, no belts. All resources flow through a single `GlobalResourcePool: Map<Resource, Float>`. Buildings are ECS entities that consume resources from the pool and produce other resources into it. The core player loop is hand mine → build → automate → balance ratios.

**What already exists (do not recreate):**
- `data/Enums.kt` — `Resource`, `BuildingType`, `GroupState`, `GroupPriority`, `ResourceCategory`
- `data/GlobalState.kt` — `GlobalResourcePool`, `LifetimeMiningStats`, `UnlockRegistry`
- `data/RecipeRegistry.kt` — `Recipe` data class, `RecipeRegistry`
- `data/MilestoneDefinitions.kt` — Phase 1 milestone chain
- `components/Producer.kt`, `Miner.kt`, `FuelConsumer.kt`, `Building.kt`, `BuildingGroup.kt`
- `systems/MilestoneSystem.kt` — leave this untouched
- `systems/MinerSystem.kt` — update for fractional accumulator (see below)
- `ui/` — skin layer, fully complete, do not touch
- `screens/GameScreen.kt` — has the ECS world and system registrations; update system list

**What is WRONG and must be replaced:**
- `components/ResourceBuffer.kt` — **delete this file**
- `systems/BufferFillSystem.kt` — **delete this file**
- `systems/ProductionSystem.kt` — **rewrite entirely** (currently reads from ResourceBuffer; wrong model)
- `systems/FuelSystem.kt` — **rewrite** (currently does buffer drain/top-up; wrong model)

**The correct model — read `docs/design-systems.md` for full spec. Summary:**

Each building entity declares its consumption rate per resource to the pool (`declaredRates`). A pool tick runs every frame that computes how much of each resource is available, allocates it across priority tiers (HIGHEST first, proportional within tier), and writes `currentSatisfaction` (0.0–1.0) back to each entity. Production output uses fractional accumulation: `fractionalAccumulator += baseOutput × mkMultiplier × currentSatisfaction`. When the accumulator reaches 1.0 or more, whole items are awarded to the pool and the remainder is kept. Cycle timers never change — only output scales with satisfaction.

**Specifically build:**

`components/ProductionSatisfaction.kt` — new component:
```kotlin
data class ProductionSatisfaction(
    val declaredRates: MutableMap<Resource, Float> = mutableMapOf(),
    var currentSatisfaction: Float = 1f,
    var fractionalAccumulator: Float = 0f
) : Component<ProductionSatisfaction> {
    override fun type() = ProductionSatisfaction
    companion object : ComponentType<ProductionSatisfaction>()
}
```

`systems/PoolTickSystem.kt` — replaces `BufferFillSystem`. This system must access all entities globally (not one at a time), so implement it as an `IntervalSystem` (or override `onTick()`) rather than an `IteratingSystem`. Each tick:
1. For each resource R, gather all active (non-paused) entities that consume R (check their `ProductionSatisfaction.declaredRates`)
2. Compute `inboundRate[R]` = sum of output rates of all producers of R (from global pool tracking or by iterating Producer entities)
3. For each priority tier HIGHEST → LOWEST: compute tier demand, compare to remaining supply, set satisfaction proportionally, subtract from remaining. If remaining = 0, all lower tiers get satisfaction = 0
4. Set each entity's `currentSatisfaction = min(satisfaction[R] for R in recipe.inputs)`
5. Skip paused entities (check `BuildingGroup.paused` in Phase 2)

`systems/ProductionSystem.kt` — rewrite:
- Family: `all(Producer, ProductionSatisfaction)`
- Skip if paused (Phase 2 BuildingGroup check) or `recipe == null` (set `NO_RECIPE`)
- Advance `producer.progress += deltaTime`
- On cycle complete (`progress >= recipe.duration`): compute `fractionalAccumulator += recipe.baseOutput × currentSatisfaction`; award `floor(fractionalAccumulator)` items to pool; subtract awarded amount from accumulator; reset `producer.progress = 0f`; set `RUNNING` if satisfaction > 0, `STALLED` if satisfaction = 0
- Do NOT reset progress if inputs are missing — the cycle timer always runs

`systems/FuelSystem.kt` — rewrite:
- Each `FuelConsumer` entity declares its fuel consumption rate in `ProductionSatisfaction.declaredRates` for the fuel resource (Coal in Phase 1)
- FuelSystem runs after PoolTickSystem. It reads `currentSatisfaction` for the fuel resource specifically
- If fuel satisfaction = 0: set `FUEL_STARVED` on any `Producer` or `Miner` on this entity
- FuelSystem skips paused entities

`systems/MinerSystem.kt` — update:
- Add `ProductionSatisfaction` to family
- Miners have no recipe input (they produce raw resources), so their `currentSatisfaction` = fuel satisfaction only
- Apply fractional accumulation: `fractionalAccumulator += baseOutput × currentSatisfaction`; award whole items to pool

**Fix in `data/Enums.kt`:**
- Rename `ResourceCategory.INTERMEDIATE` to `ResourceCategory.COMPONENT`
- Add `COPPER_ORE(RAW)` and `COPPER_PLATE(PROCESSED)` to `Resource` enum (needed by research system later; safe to add now)

**Update `screens/GameScreen.kt`:**
- Remove `BufferFillSystem()` from systems block
- Add `PoolTickSystem()` as the first system
- Every entity that was getting a `ResourceBuffer` component should now get a `ProductionSatisfaction` component instead
- Recompute `declaredRates` in `ProductionSatisfaction` from the entity's recipe: `rate[resource] = inputs_per_cycle[resource] / recipe.duration`

**Compile check:** Run `./gradlew core:compileKotlin` when done. Fix all errors before finishing.

---

## Step 4 — UI Shell

You are implementing the navigation shell for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules, Scene2D pitfalls, and the workflow rule you must follow.

**What already exists (do not recreate):**
- Full skin layer: `ui/GameSkin.kt`, `ui/Drawables.kt`, `ui/Buttons.kt`, `ui/Labels.kt`, `ui/Fonts.kt`
- `screens/GameScreen.kt` — has a stage, ECS world, and a nearly empty `stage.actors { }` block. You will restructure the actors block.
- All ECS systems and components from Step 2 — do not touch

**What to build:**

Read `docs/design-ui.md` for the full layout spec. The layout is:
```
┌─────────────────────────────────────────────┐
│  Resource Bar (full width, ~52px tall)       │
├──────┬──────────────────────────────────────┤
│ Nav  │  Content Stack (one view visible)     │
│ ~64px│                                       │
└──────┴──────────────────────────────────────┘
```

**`ui/models/NavigationModel.kt`**
Manages which content view is currently visible. Holds references to all registered content views as `Actor`. Exposes `show(view: Table)` which sets all views invisible then makes the target visible. Views are registered via `register(vararg views: Table)`.

**`ui/views/NavSidebarView.kt`**
A `Table` subclass. Vertical column of icon buttons, one per nav destination, full height below the resource bar. Uses `Buttons.NAVIGATION()` skin style for each button. The active view's button uses the checked state (which maps to `BUTTON_NAVIGATION_SELECTED` drawable in the skin). Clicking a button calls `navigationModel.show(targetView)`. Nav buttons in order: Factory, Power, Research, Progress, Settings. Uses `ICON_NAVIGATION_*` drawables for button icons. Icons are 32×32; buttons fill the ~64px sidebar width.

**Stub content views** — each a `Table` subclass with a centered `Label` only. Real content comes in later steps.
- `ui/views/FactoryView.kt` — label: "Factory"
- `ui/views/PowerView.kt` — label: "Power — Coming Soon"
- `ui/views/ResearchView.kt` — label: "Research — Coming Soon"
- `ui/views/ProgressView.kt` — label: "Progress — Coming Soon"
- `ui/views/SettingsView.kt` — label: "Settings — Coming Soon"

**Stub resource bar** — `ui/views/ResourceBarView.kt` — a `Table` subclass with a placeholder label "Resource Bar" for now. Real content in Step 5.

**`screens/GameScreen.kt` restructure** — follow the pattern in `docs/design-ui.md` exactly:
- All models and views are class-level properties, not created inside the DSL block
- Use `setFillParent(true)` on the root table only
- Stack cell gets `.prefWidth(0f).minWidth(0f)`
- `navigationModel.show(factoryView)` sets the default tab after the stage is built
- `Gdx.input.inputProcessor = stage` in `show()`

**Constants** to define in GameScreen companion object:
```kotlin
const val RESOURCE_BAR_HEIGHT = 52f
const val NAV_WIDTH = 64f
```

**Compile check:** Run `./gradlew core:compileKotlin` when done and fix all errors.

---

## Step 5 — Resource Bar & Hand Mining

You are implementing the resource bar UI for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules, Scene2D pitfalls, and the workflow rule you must follow.

**What already exists:**
- Full navigation shell from Step 4: `GameScreen`, `NavigationModel`, `NavSidebarView`, all view stubs
- `ResourceBarView.kt` — stub with placeholder label; replace this entirely
- Global state: `GlobalResourcePool`, `LifetimeMiningStats`, `UnlockRegistry` (all injected into GameScreen)
- Skin with all drawables, fonts, button styles

**Read `docs/design-ui.md` (Resource Bar section) and `docs/design-systems.md` (Hand Mining section) before designing anything.**

**`ui/models/ResourceBarModel.kt`**

Constructed in `GameScreen` with references to `GlobalResourcePool`, `LifetimeMiningStats`, `UnlockRegistry`. Called from `GameScreen.render()` via `update(delta)`.

Responsibilities:
- Track which RAW resources are unlocked (from `UnlockRegistry`) — expose for hand mining button list
- Track all resources that are visible (unlocked AND quantity > 0 OR actively produced/consumed)
- Maintain a rolling rate window: ring buffer of 60 samples taken 1 second apart. Each sample is a snapshot of the pool amounts. Rate = `(currentSnapshot[R] - sampleFrom60sAgo[R]) / 60.0`. Expose as `getRate(resource): Float`
- Expose `getAmount(resource): Float` directly from pool
- Expose `displayMode: DisplayMode` (COUNT or RATE) toggled by the player
- Expose `isHandMining(resource): Boolean` and `handMiningProgress(resource): Float` (0.0–1.0 of the 2s cycle)

Hand mining state lives in the model: a `Map<Resource, Float>` tracking cycle progress per resource. When player starts mining: set `cycleProgress[R] = 0f`. Each `update(delta)`: advance all in-progress cycles. When a cycle reaches 2.0s: add 1 to `GlobalResourcePool`, add 1 to `LifetimeMiningStats`, reset progress (auto-idles — does not restart). Hand mining never uses ECS.

**`ui/views/ResourceBarView.kt`** (replace stub)

Left side — Hand Mining Widget:
- One `TextButton` per unlocked RAW resource (from model)
- Button shows resource name; below or within it, a thin progress bar (use `PROGRESS_FILL_GREEN` drawable) showing current 2s mining cycle
- Clicking a button that is idle starts a new cycle; clicking mid-cycle has no effect (already running)
- Buttons never disabled — hand mining is always the escape hatch

Right side — Resource Display:
- Resources grouped by category: RAW, PROCESSED, COMPONENT, SCIENCE
- Each category is collapsible (toggle arrow, player preference stored in model)
- Only show categories that have at least one visible resource
- Each entry: small icon (20×20, `ICON_RSC_*` drawable) + name label + amount or rate label
- Count mode: `Iron Ore  247` — use `BODY` font for name, `BODY_BOLD` for number
- Rate mode: `Iron Ore  +12.4/min` — positive in `#27ae60` green, negative in `#c0392b` red, zero in dim text color
- Toggle button on the bar switches between COUNT and RATE modes

Display mode toggle: a small `TextButton` using `Buttons.DEFAULT()` style; label changes to show current mode ("Count" / "Rate").

The model calls `onPropertyChange` callbacks (or equivalent) when data changes. The view subscribes and updates labels. Do not poll the model from `act()` — use change callbacks.

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 6 — Factory View (Phase 1)

You are implementing the factory view UI for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules, Scene2D pitfalls, and the workflow rule you must follow.

**What already exists:**
- Navigation shell (Step 4), Resource Bar (Step 5) — do not touch
- `FactoryView.kt` — stub table with "Factory" label; replace this
- ECS world with `Producer`, `Miner`, `FuelConsumer`, `ProductionSatisfaction` components on building entities
- `GroupState` enum: `RUNNING`, `STALLED`, `FUEL_STARVED`, `PAUSED`, `NO_RECIPE`
- `Drawables` enum: `STATUS_RUNNING`, `STATUS_STALLED`, `STATUS_FUEL_STARVED`, `STATUS_PAUSED`, `STATUS_IDLE`, `CARD_BG_*`, `ICON_BLD_*`, `ICON_RSC_*` — all registered in skin
- Skin: `Buttons.DEFAULT()`, `Buttons.ACCENT()`, `Buttons.DANGER()`, `Labels.BODY()`, `Labels.BODY_BOLD()`, `Labels.SMALL()`, `Labels.HEADING()`

**Read `docs/design-ui.md` (Factory View section), `docs/design-buildings.md`, `docs/design-systems.md`, and `docs/design-assets.md` before designing anything.**

**`ui/models/FactoryModel.kt`**

Constructed in GameScreen with access to the ECS world, `GlobalResourcePool`, and `UnlockRegistry`. Called each render tick to sync ECS state to UI data.

Exposes:
- List of unlocked building types with their cost and whether the player can currently afford them
- List of placed building entities with their current state: `groupState`, `currentSatisfaction`, assigned recipe/resource, fuel state
- Callbacks when any building's state changes (for view refresh)

**`ui/views/FactoryView.kt`** (replace stub)

Two-panel horizontal split:

**Left Panel (~280px wide) — Build Menu:**
- `panel_bg` nine-patch background
- Scrollable list of unlocked building types
- Each entry: building icon (`ICON_BLD_*`, 32×32) + building name + cost breakdown (resource icon + amount per cost item)
- If unaffordable: cost labels in `#c0392b` red, build button disabled (uses `disabled` style)
- Build button: `Buttons.ACCENT()` style, labeled "Build". For now (Step 7 not done yet), clicking shows a placeholder "Construction coming in Step 7" log message — do NOT wire actual construction yet
- Below the button list: show current unassigned count per building type if > 0: `"Basic Miners: 3 unassigned"` in `Labels.SMALL()`/dim color

**Right Panel — Building List:**
- `panel_dark` nine-patch background
- Scrollable list of individual building cards (Phase 1 — one card per ECS entity)
- Empty state: `"No buildings yet. Build your first one →"` centered label pointing left toward build menu

**Building Card Widget** (each card is a Table):
- Background: `CARD_BG_RUNNING/STALLED/FUEL_STARVED/PAUSED/IDLE` nine-patch — chosen by `groupState`
- Building type icon: `ICON_BLD_*`, 32×32, left side
- Right side stacked: building type name in `Labels.BODY()`, assigned recipe/resource in `Labels.SMALL()` (or "No recipe" in dim)
- Status dot: `STATUS_*` drawable, 12×12, bottom-left of card
- Satisfaction bar: thin horizontal bar below the card content area. Width scales with `currentSatisfaction`. Color: green at 100%, amber at 60–99%, orange at 1–59%, hidden/absent at 0% or STALLED
- Click on card: opens an inline detail panel replacing the card list (or a side panel — your choice, keep it simple for Phase 1)

**Inline Detail Panel** (shown when a building card is clicked):
- Building name label + back button to return to list
- Recipe/resource picker: shows available recipes for this building type from `RecipeRegistry`; player taps one to assign; updates the entity's `Producer.recipe` and triggers `declaredRates` recomputation in `ProductionSatisfaction`
- Current state label: e.g. "RUNNING at 87%" or "STALLED — waiting for Iron Ore"
- Per-input satisfaction breakdown: one row per input: icon + name + "12.4/s available, 24.0/s needed (50%)"
- Fuel state (if `FuelConsumer`): "Fuel: Coal — OK" or "FUEL STARVED"
- Pause toggle button: `Buttons.DEFAULT()` style. Pausing sets `BuildingGroup.paused = true` and zeroes `declaredRates`

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 7 — Construction System

You are implementing the construction system for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**What already exists:**
- Factory view from Step 6 — build buttons exist but show placeholder messages; wire them in this step
- Full ECS world with `PoolTickSystem`, `ProductionSystem`, `MinerSystem`, `FuelSystem`, `MilestoneSystem`
- `GlobalResourcePool`, `UnlockRegistry`, `RecipeRegistry` available in GameScreen
- `data/Enums.kt` — `BuildingType` enum

**Read `docs/design-systems.md` (Construction System and Phase 1 Buildings sections) and `docs/design-buildings.md` before designing.**

**`data/ConstructionQueue.kt`**

```kotlin
data class ConstructionEntry(
    val type: BuildingType,
    var remainingTime: Float
)

class ConstructionQueue {
    val entries: MutableList<ConstructionEntry> = mutableListOf()
    fun enqueue(type: BuildingType, duration: Float) { entries.add(ConstructionEntry(type, duration)) }
    val active: ConstructionEntry? get() = entries.firstOrNull()
    fun advance(delta: Float): ConstructionEntry? {
        val entry = active ?: return null
        entry.remainingTime -= delta
        return if (entry.remainingTime <= 0f) { entries.removeFirst(); entry } else null
    }
}
```

Instantiate `ConstructionQueue` in `GameScreen` as a class-level property. Add it to `injectables` so `FactoryModel` can expose queue state to the UI.

**Construction times (Phase 1, tune during testing):**
- Stone Furnace: 5 seconds
- Basic Miner: 8 seconds

**Build button flow (wire into `FactoryModel` / `FactoryView`):**
1. Check `GlobalResourcePool` has enough of each cost resource
2. If not: button stays disabled (already handled visually in Step 6)
3. If yes: deduct cost from pool immediately; call `constructionQueue.enqueue(type, duration)`
4. Show construction entry in build menu left panel: building name + progress bar + remaining time countdown

**`GameScreen.render()` — advance queue:**
```kotlin
constructionQueue.advance(delta)?.let { completed ->
    createBuildingEntity(completed.type)
}
```

**`createBuildingEntity(type: BuildingType)` in GameScreen:**
- Creates the ECS entity with the correct components for the building type
- Stone Furnace: `Building`, `Producer` (recipe = null initially), `FuelConsumer` (coal, consume rate from design doc), `ProductionSatisfaction` (declaredRates empty until recipe assigned)
- Basic Miner: `Building`, `Miner` (resource = null initially), `FuelConsumer` (coal), `ProductionSatisfaction`
- After creation: does NOT assign recipe — player assigns from the detail panel in FactoryView
- `FactoryModel` detects new entities on next update (Fleks world provides entity query)

**Recipe assignment (wire into the detail panel from Step 6):**
When player assigns a recipe in the detail panel:
1. Set `entity[Producer].recipe = selectedRecipe`
2. Recompute `entity[ProductionSatisfaction].declaredRates`: for each input resource, `rate = inputs_per_cycle / recipe.duration`
3. `PoolTickSystem` picks up new rates on the next tick automatically

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 8 — Save / Load & Offline Progress

You are implementing the save/load system for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**What already exists:**
- Full game loop: ECS, factory view, construction queue
- `kotlinx.serialization` is already in `core/build.gradle`
- Global state: `GlobalResourcePool`, `LifetimeMiningStats`, `UnlockRegistry`, `ConstructionQueue`
- ECS world with building entities

**Read `docs/design-systems.md` (Save/Load section) before designing.**

**`data/SaveData.kt`** — the serializable save schema:
```kotlin
@Serializable
data class SaveData(
    val version: Int = 1,
    val savedAt: Long = 0L,
    val globalPool: Map<String, Float>,
    val lifetimeStats: Map<String, Float>,
    val unlockedBuildings: List<String>,
    val unlockedResources: List<String>,
    val placedBuildings: List<PlacedBuildingData>,
    val constructionQueue: List<ConstructionEntryData>,
    val completedMilestones: Set<String>
)

@Serializable
data class PlacedBuildingData(
    val type: String,
    val assignedRecipe: String?,       // recipe id or null
    val assignedResource: String?,     // resource name or null (for miners)
    val cycleProgress: Float,
    val fractionalAccumulator: Float,
    val paused: Boolean
)

@Serializable
data class ConstructionEntryData(
    val type: String,
    val remainingTime: Float
)
```

Use `String` keys for enum values (`.name`) rather than enum references — this makes future schema migrations simpler.

**`data/SaveManager.kt`**

- `save(gameScreen: GameScreen): Unit` — serializes all global state + ECS entity state to `SaveData`, writes JSON to `Gdx.files.local("save.json")`
- `load(): SaveData?` — reads and deserializes from `save.json`; returns null if file missing or parse fails
- `applyLoad(data: SaveData, gameScreen: GameScreen): Unit` — restores all state: pool, lifetime stats, unlocks, then reconstructs ECS entities from `placedBuildings`, restores construction queue. After entity reconstruction, `ProductionSatisfaction.declaredRates` are recomputed from each entity's recipe (derived — not saved). `currentSatisfaction` starts at 1.0 and is computed on first pool tick.

**`GameScreen` integration:**
- On `create()` / first run: attempt load; if save exists, call `applyLoad`; otherwise start fresh
- Autosave: track `timeSinceLastSave += delta` in `render()`; when > 60f, call `SaveManager.save()` and reset counter; show "Saved" label that fades after 2 seconds
- On `pause()` (fires on minimize/close on desktop): call `SaveManager.save()`

**Offline progress:**
- `savedAt` timestamp is `System.currentTimeMillis()` at save time
- On load: `elapsedSeconds = (System.currentTimeMillis() - data.savedAt) / 1000.0`
- Cap at 8 hours (28800 seconds) to prevent absurd catch-up
- For each resource: `gained = pool.netRate[R] * elapsedSeconds` (net rate = inbound − outbound from last session; approximate from saved rates or skip if complex, simplify as needed)
- Apply gained amounts to pool
- Show a one-time modal after load listing resources gained: "While you were away (2h 14m): +1,840 Iron Ore, +920 Iron Plates..." — use a simple `Dialog` from Scene2D. Player dismisses it.

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 9 — Research System & Science Packs

You are implementing the research system for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**What already exists:**
- Full game loop including save/load (Steps 1–8)
- `UnlockRegistry` — manages which `BuildingType` and `Resource` values are unlocked
- Milestone system — fires reward lambdas; research unlocks use the same pattern
- `RecipeRegistry` — data-driven; new recipes = new config entries

**Read `docs/design-systems.md` (Research System, Science Packs, Research Tiers Overview) and `docs/design-ui.md` (Research View section) before designing.**

**`data/ResearchGoal.kt`**
```kotlin
data class ResearchGoal(
    val id: String,
    val name: String,
    val tier: Int,                         // 1=Red, 2=Orange, etc.
    val cost: Map<Resource, Int>,          // science packs required
    val reward: () -> Unit                 // mutates UnlockRegistry
)
```

**`data/ResearchManager.kt`** (global state, outside ECS)
```kotlin
class ResearchManager(private val unlockRegistry: UnlockRegistry) {
    var activeGoal: ResearchGoal? = null
    var progress: Float = 0f
    val completedIds: MutableSet<String> = mutableSetOf()
    val allGoals: List<ResearchGoal> = buildAllResearch(unlockRegistry)

    fun setActive(goal: ResearchGoal) { activeGoal = goal; progress = 0f }
    fun addProgress(amount: Float) { ... }  // advance, fire reward on completion
    fun isCompleted(id: String) = id in completedIds
    fun isAvailable(goal: ResearchGoal): Boolean  // tier prereqs met
}
```

Add `ResearchManager` to `GameScreen` as a class-level injectable.

**`data/ResearchDefinitions.kt`** — Tier 1 (Red Science) research goals. Implement only Tier 1 now; stub Tiers 2–6 as empty lists. Key Tier 1 goals from `docs/design-systems.md`:
- Basic Mining (10 red) → unlocks Basic Miner
- Basic Smelting (10 red) → unlocks Iron Plate recipe
- Copper Smelting (15 red) → unlocks Copper Plate recipe
- Basic Assembly (25 red) → unlocks Assembler Mk1
- Iron Gear Casting (30 red) → unlocks Iron Gear recipe
- Copper Wiring (30 red) → unlocks Copper Wire recipe
- Red Science Prod (50 red) → unlocks Red Science Pack recipe, unlocks Research Facility

**Research Facility ECS entity:**
- Uses `Producer` component but its output goes to `ResearchManager`, not the pool
- Override behavior in `ProductionSystem` or use a marker component `ResearchProducer` to distinguish: on cycle complete, call `researchManager.addProgress(1f)` instead of writing to pool
- Auto-assigns required science packs from `researchManager.activeGoal?.cost` as its `declaredRates` — player does not set a recipe manually
- If no active research: entity idles (`NO_RECIPE` state)

**Science Pack recipes** — add to `RecipeRegistry`:
- Red Science: Assembler recipe, inputs: 1 Iron Plate + 1 Copper Wire, output: 1 Red Science Pack, duration: 5s

**Add to `Enums.kt`:**
- `RED_SCIENCE(SCIENCE)`, `ORANGE_SCIENCE(SCIENCE)` etc. to `Resource`
- `RESEARCH_FACILITY`, `ASSEMBLER_MK1` to `BuildingType`
- `COPPER_ORE(RAW)`, `COPPER_PLATE(PROCESSED)`, `COPPER_WIRE(COMPONENT)`, `IRON_GEAR(COMPONENT)` to `Resource` if not already present

**`ui/models/ResearchModel.kt`** and **`ui/views/ResearchView.kt`** (replace stub):
- Active research section: goal name, progress bar, science pack consumption rate
- Tiered list: Tier 1 visible; goals show as unlocked (clickable), locked (greyed), or completed (checked/greyed)
- Click an available goal → `researchManager.setActive(goal)`
- Lock indicator for unavailable goals: "Requires: [prerequisite name]"

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 10a — Phase 2: Building Groups (ECS & Data)

You are implementing the building group system (data/ECS layer) for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**Context:** Phase 1 uses one ECS entity per individual building. Phase 2 (unlocked by early Orange Science research) transitions to one ECS entity per BuildingGroup of N buildings. The player never interacts with individual buildings again after this point. Read `docs/design-buildings.md` fully and `docs/design-systems.md` (Pool Tick Algorithm, Fractional Accumulation sections) before designing.

**What already exists:**
- Individual building entities with `Producer`/`Miner`/`FuelConsumer`/`ProductionSatisfaction`
- `BuildingGroup` component exists but is Phase 2 aware only
- `UnlockRegistry` manages unlocked building types
- Research system (Step 9) — the unlock trigger for groups is a research reward

**Group unlock transition:**
When the "Group Management I" research completes (Orange Science tier), fire a one-time transition:
1. For each recipe/resource currently assigned among existing individual entities: collect all entities sharing that recipe, create ONE new group entity with `count = N` (where N = number of individual entities), assign that recipe/resource
2. Individual entities whose recipe matches are deleted; the new group entity replaces them
3. Entities with no recipe go to `UnassignedPool`
4. Player sees the UI simplify from N individual cards to a few group cards

**`data/UnassignedPool.kt`**
```kotlin
class UnassignedPool {
    private val counts: MutableMap<BuildingType, Int> = mutableMapOf()
    fun add(type: BuildingType, count: Int = 1) { counts[type] = (counts[type] ?: 0) + count }
    fun remove(type: BuildingType, count: Int = 1): Boolean { ... }  // returns false if insufficient
    fun get(type: BuildingType) = counts[type] ?: 0
}
```

Add to `GameScreen` and `injectables`.

**Updated `components/BuildingGroup.kt`:**
```kotlin
data class BuildingGroup(
    val id: String,
    val type: BuildingType,
    var name: String,
    var count: Int = 0,
    var priority: GroupPriority = GroupPriority.NORMAL,
    var paused: Boolean = false
) : Component<BuildingGroup> { ... }
```

**`declaredRates` scaling for groups:**
When count changes, immediately recompute `ProductionSatisfaction.declaredRates`:
```kotlin
rate[resource] = (singleBuildingInputsPerCycle[resource] / recipe.duration) * count
```
`PoolTickSystem` picks this up automatically on the next tick.

**`fractionalAccumulator` in groups:**
A group of N buildings produces N items per cycle at full satisfaction. The accumulator handles Mk multipliers normally. Output per cycle: `floor(baseOutput × N × mkMultiplier × currentSatisfaction)`. The accumulator still smooths partial satisfaction across cycles.

**Group creation (for new buildings in Phase 2):**
- New buildings from construction go to `UnassignedPool` (not directly into a group)
- Player creates a group via the factory view UI (Step 10b); the group entity is created then
- A group with `count = 0` is valid — it's a named placeholder ready for buildings

**`PoolTickSystem` updates:**
No structural change required. Groups already have `BuildingGroup` component for paused check. Their `declaredRates` are already N× the single-building rate. The satisfaction math is identical.

**Save/load additions for groups** — update `SaveData.kt`:
- `unassignedPool: Map<String, Int>` — serialized counts
- Group entities serialize as `PlacedBuildingData` with `count` field added

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 10b — Phase 2: Group UI

You are implementing the building group UI for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules, Scene2D pitfalls, and the workflow rule you must follow.

**What already exists:**
- Phase 2 ECS layer from Step 10a: `BuildingGroup`, `UnassignedPool`, group transition logic
- Factory view from Step 6 (individual building cards) — replace with group card grid in Phase 2
- Full skin with `CARD_BG_*`, `ICON_BLD_*`, `ICON_RSC_*`, `STATUS_*` drawables, `BUTTON_NAVIGATION_SELECTED`, etc.

**Read `docs/design-buildings.md` (all sections), `docs/design-ui.md` (Group Card Design, Group Detail View, Factory View sections), and `docs/design-assets.md` before designing.**

**`FactoryView.kt` — Phase 2 mode:**
After the group unlock transition, the right panel switches from an individual building list to a group card grid. The left panel build menu remains but now also shows unassigned pool counts and a "New Group" button per building type.

**Group card widget** — a fixed-size `Table` or `Stack` (approximately 160×180px, tune during implementation):
- Background: `CARD_BG_RUNNING/STALLED/FUEL_STARVED/PAUSED/IDLE` nine-patch, fills card
- Building type art image: `BLD_ART_*` drawable, 64×64, centered
- Recipe/resource icon overlay: `ICON_RSC_*` drawable, 36×36, centered over art
- Group name label: `Labels.BODY()`, left-aligned, below art
- Status dot: `STATUS_*` drawable, 12×12, bottom-left
- Building count label: `Labels.BODY_BOLD()`, bottom-right, e.g. "70"
- Satisfaction bar: thin bar below name, width = `currentSatisfaction × cardWidth`. Colors: green at 100%, amber 60–99%, orange 1–59%, hidden at 0% or STALLED

All card elements update every render tick from `FactoryModel`.

**Group detail view** — opens when a card is clicked (slide in from right or replace right panel):

Header:
- Group name field: tap to make editable inline; confirm on Enter or focus-lost
- Building type label
- State indicator: colored dot + state name + satisfaction percentage

Stats:
- Building count, recipe/resource assigned
- Effective production rate: `count × baseRate × mkMultiplier × currentSatisfaction /min`
- Per-input satisfaction breakdown (same as Phase 1 detail panel)

Controls:
- `+ Add` / `− Remove` count adjusters (from `UnassignedPool`; disable if pool empty)
- `Quick Fill` — move all unassigned of this type into group
- `Change Recipe` — opens recipe picker; shows confirmation warning "Current progress resets"
- `Pause` / `Unpause` toggle
- `Split` — count input, then assign recipe to split group or return to pool
- `Merge` — available if another group of same building type is selected
- `Disband` — confirmation dialog; returns all buildings to `UnassignedPool`

Priority stepper:
```
[◀]  Normal  [▶]
```
Cycles through `LOWEST → LOW → NORMAL → HIGH → HIGHEST`. Never shows numbers.

**"New Group" flow:**
1. Player clicks "New Group" for a building type in the build menu
2. Recipe picker opens — shows all unlocked recipes for this building type
3. Count input appears (capped at `UnassignedPool.get(type)`)
4. Confirm → creates new group entity with `BuildingGroup`, assigns recipe, sets `declaredRates`, moves buildings from pool to group

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 11 — Bottleneck Inspector & Net Rate Display

You are implementing factory health diagnostics for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**What already exists:**
- Full game loop with groups, research, save/load
- Resource bar shows amounts; Step 5 added rolling rate window to `ResourceBarModel`
- `PoolTickSystem` tracks per-entity `currentSatisfaction` and per-resource inbound/outbound rates

**Read `docs/design-ui.md` and `docs/design-systems.md` (Status Indicators section) before designing.**

**`util/NumberFormatter.kt`** — single utility used everywhere in the codebase:
```kotlin
enum class NumberStyle { FULL, COMPACT, SCIENTIFIC }

fun formatNumber(n: Float, style: NumberStyle): String {
    return when (style) {
        FULL -> "%,.0f".format(n)              // "1,250,000"
        COMPACT -> compactFormat(n)            // "1.25M", "42.3K"
        SCIENTIFIC -> scientificFormat(n)      // "1.25e6"
    }
}
```

Audit every number render point in the codebase and route through this function. Store the player's style preference in settings (persisted in save state or separately in `Gdx.files.local("settings.json")`). Add a style selector to `SettingsView`.

**Net rate display — update `ResourceBarModel`:**
- Expose `getNetRate(resource): Float` = `inboundRate[R] - outboundRate[R]` from pool tick data
- `PoolTickSystem` must track and expose these per-resource rates (add `inboundRates` and `outboundRates` maps)
- Resource bar right panel already shows amounts; add net rate display: `+12.4/s` (green) or `−3.1/s` (red) next to each resource. Use `formatNumber` for amounts.

**Bottleneck Inspector panel:**
Add as a dedicated sub-view within `FactoryView` (e.g. a collapsible panel at the bottom, or an icon button in the nav bar for a full view — your choice). Re-evaluates every 3 seconds.

Four checks:
1. **Zero-output groups:** any group with `currentSatisfaction == 0f` and state = `STALLED`
2. **Negative net rate:** any resource where `netRate < 0` (pool draining)
3. **Fuel-starved groups:** any group with state = `FUEL_STARVED`
4. **Zero-stock resources:** any resource where pool amount ≤ 0 that has active demand

Results rendered as a scrollable flagged list. Each entry: warning icon + description + a link/button that scrolls the group card grid to the offending group and briefly highlights it. If no issues: show "Factory healthy" in green.

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 12 — Nudge System & Tutorial

You are implementing the in-game nudge and tutorial system for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**What already exists:**
- Full game loop, all views, milestone system (fires rewards invisibly)
- `MilestoneSystem` fires milestones — tutorial callouts attach to specific milestone events

**Read `docs/design-ui.md` (brief) and `docs/design-systems.md` (Milestone System section). The full tutorial specification is written below — this spec takes precedence over anything else.**

**Core philosophy:** The tutorial is not a separate mode. It is the first 3 minutes of the game. The milestone system is invisible — players never see thresholds or progress bars. Callouts are non-blocking, brief, observational, and disappear the moment the player acts. Never prescribe quantities, never block UI, never use the word "tutorial."

**`ui/NudgeSystem.kt`** — evaluates a priority-ordered condition list each update tick, surfaces the first true condition as a one-line hint in a small persistent panel (bottom of screen or corner). One nudge at a time. Dismissible. Re-evaluates on dismiss. Conditions (in priority order):
1. Any group has no recipe assigned
2. Any resource has been at zero for more than 30 seconds
3. A new science tier is unlockable at current pack production rate
4. A Mk upgrade is affordable given current stock

Nudge display: small `Label` in a `panel_bg` table, with an `×` dismiss button. Re-evaluates every 5 seconds or on player action.

**`ui/TutorialController.kt`** — fires callouts keyed to game events. A callout is a non-blocking `Table` positioned near a UI element with a brief text string. Disappears after 10 seconds or when the player takes the relevant action.

**Tutorial sequence** (milestone thresholds are invisible to the player — these are implementation targets only):

| Event | Callout | Target element |
|---|---|---|
| Game opens | "Click to start mining. Resources accumulate automatically." | Iron ore card in resource bar |
| 10 iron ore mined | Stone card appears — no callout, just appear | Mining widget |
| 10 stone mined | Badge appears on Production nav button | Nav button |
| Production panel opened (first time) | "Something new in production." | Production nav badge |
| Unassigned building exists (first time) | "This facility needs a recipe to know what to produce." | Building card |
| Recipe assigned, building has no fuel | — (permanent orange fuel indicator handles this) | — |
| Orange fuel indicator hovered (first time) | Permanent tooltip: "No fuel. This facility requires: Coal" | Indicator |
| 10 coal mined | Badge on Production nav button | Nav button |
| 10 iron plates produced | Badge on Production nav button | Nav button |
| 5 iron gear wheels produced | Copper chain unlocks — no callout, world just expands | — |

**Callout rules:**
- Each callout fires at most once per play session per key
- Callout dismissed by: player performs the relevant action, 10s timeout, or explicit × click
- Do not show a callout for an event the player has already handled before the callout fires

**Stuck-player escalation** — fires only if player is past the expected time without acting:

| Stuck state | L1 (visual pulse) | L2 (one-line) | L3 (dismissible tooltip) |
|---|---|---|---|
| Stone unlocked, not mined after 90s | 90s | +90s | +90s — "Stone ore is now available in the mining panel" |
| Production panel not opened after furnace available for 90s | 90s | +90s | +90s — "Visit the production panel to build your first furnace" |
| Furnace built, no recipe after 60s | 60s | +60s | +60s — "This furnace has no recipe assigned — click to assign one" |
| Miner unlocked, not built after 120s | 120s | +120s | +120s — "A new facility is available to build" |
| Assembler unlocked, not built after 120s | 120s | +120s | +120s — "A new facility is available to build" |

L1 = visual pulse animation on the relevant UI element. L2 = one-line status text on the relevant card. L3 = dismissible tooltip. Each level cancels immediately when the player acts.

**Skip intro:**
- Small unobtrusive "Skip intro" text link in bottom corner from game start until tutorial is flagged complete
- No confirmation required
- On skip: dismiss all active callouts, grant 20 iron ore + 10 stone + 15 coal + 5 iron plates to pool, unlock Stone Furnace and Basic Miner in `UnlockRegistry`, activate nudge system

**Tutorial state persisted in save:** which callouts have fired, whether tutorial is complete.

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 13 — Statistics Panel & UI Polish

You are implementing statistics tracking and UI polish for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**What already exists:**
- Full game loop, all views, nudge system
- `LifetimeMiningStats` tracks lifetime mined amounts — extend the concept to all production

**Read `docs/design-ui.md` before designing.**

**`data/StatisticsTracker.kt`** (global state, outside ECS):
```kotlin
class StatisticsTracker {
    val lifetimeProduced: MutableMap<Resource, Float> = mutableMapOf()
    val totalFacilitiesBuilt: MutableMap<BuildingType, Int> = mutableMapOf()
    var sciencePacksConsumed: Float = 0f
    val sessionStartTime: Long = System.currentTimeMillis()
}
```

Wire into `ProductionSystem` and `MinerSystem` — increment `lifetimeProduced` on every cycle completion. Wire into construction system — increment `totalFacilitiesBuilt` on entity creation. Wire into research system — increment `sciencePacksConsumed`.

**Statistics tab** — update `ProgressView.kt` to show two tabs: Milestones and Statistics.

Statistics tab content:
- Session time: `"Playing for: 1h 24m"`
- Per-resource lifetime produced: scrollable table of resource icon + name + lifetime amount (formatted)
- Facilities built: per building type count
- Science packs consumed
- All numbers through `formatNumber(n, COMPACT)`

Per-group lifetime output — add a line to the group detail panel (Step 10b): `"Lifetime output: 48,291 Iron Plates"`. Track per-group in `StatisticsTracker` keyed by group id.

**Empty state pass** — audit every panel. Add one-line centered messages for each:
- Factory view with no buildings: "No buildings yet. Build your first one →"
- Research view with no active research: "No active research. Select a goal above."
- Progress view milestones (all completed): "All milestones complete."
- Bottleneck Inspector (no issues): "Factory healthy."
- Group card grid (no groups in Phase 2): "No groups yet. Create one from the build menu."

**Number transition smoothing** — resource amounts and rates should tick up/down smoothly rather than jumping. Implement a simple lerp on display values: `displayValue = lerp(displayValue, targetValue, delta × 8f)`. Apply to all number labels in the resource bar.

**UI consistency audit** — scan all views for:
- Any label not using the established font styles (`Labels.HEADING()`, `Labels.BODY()`, `Labels.SMALL()`, `Labels.DIM()`)
- Any button not using `Buttons.DEFAULT()`, `Buttons.ACCENT()`, or `Buttons.DANGER()`
- Inconsistent padding (standardize to 8px between components, 4px for tight groups)
- Fix any found without changing layout structure

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 14 — Audio

You are implementing the audio system for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**What already exists:** Full game loop, all views. No audio currently.

**Read `docs/design-ui.md` (Settings View section) before designing.**

**Audio files needed** (source these before starting — LibGDX supports OGG/MP3/WAV):
- Ambience loops (seamlessly loopable OGG files): `sfx/ambience_light.ogg`, `sfx/ambience_medium.ogg`, `sfx/ambience_heavy.ogg`
- UI sounds (short OGG): `sfx/ui_recipe_assign.ogg`, `sfx/ui_research_complete.ogg`, `sfx/ui_resource_depleted.ogg`, `sfx/ui_save_confirm.ogg`

Place all in `assets/sfx/`.

**`audio/AudioManager.kt`**:
- Loads all audio files on init (dispose in GameScreen)
- Exposes `masterVolume: Float` (0.0–1.0), `muted: Boolean`
- `updateAmbience(totalActiveFacilities: Int)` — mix three ambience layers:
  - Light hum: always plays at low volume when any facilities exist
  - Medium industrial: volume scales from 0 at <20 facilities to full at 100+
  - Heavy factory roar: volume scales from 0 at <100 to full at 500+
  - All clamped: `effectiveVolume = if (muted) 0f else layer.targetVolume × masterVolume`
- `playUI(sound: UISoundType)` — plays the corresponding short SFX at `masterVolume`, skips if muted

**Wire UI sounds to existing event hooks:**
- Recipe assigned: in the recipe picker confirmation handler
- Research complete: in `ResearchManager.addProgress()` when goal completes
- Resource depleted: in `PoolTickSystem` when any resource crosses from > 0 to exactly 0
- Autosave confirm: in the autosave trigger in `GameScreen.render()`

**Settings view audio controls** — update `SettingsView.kt`:
- Master volume slider (0–100), label showing current value
- Mute toggle checkbox
- Both stored in settings and applied to `AudioManager` immediately

**Save/load audio settings:** Store `masterVolume` and `muted` in a separate `settings.json` (not the main save state), loaded on startup before any audio plays.

**Compile check:** `./gradlew core:compileKotlin` when done.

---

## Step 15 — Import / Export Save

You are implementing the save import/export feature for an idle factory game called FactoryIdle. The game is built with LibGDX + LibKTX + Fleks 2.x ECS in Kotlin. Read `CLAUDE.md` first — it contains architecture rules and the workflow rule you must follow.

**Prerequisite:** Step 8 (save system) must be stable with a version field in the schema before starting this step.

**What already exists:**
- `SaveManager.kt` with `save()` and `load()` operating on `save.json`
- `SaveData.kt` with `version: Int` field
- `SettingsView.kt` — add export/import UI here

**Read `docs/design-systems.md` (Save/Load section) before designing.**

**`data/SaveManager.kt` additions:**
```kotlin
fun exportToBase64(): String {
    val json = Json.encodeToString(buildSaveData())
    return Base64.getEncoder().encodeToString(json.toByteArray())
}

fun importFromBase64(encoded: String): Boolean {
    return try {
        val json = String(Base64.getDecoder().decode(encoded))
        val data = Json.decodeFromString<SaveData>(json)
        if (data.version != CURRENT_VERSION) return false  // version mismatch — reject
        pendingImport = data  // store; apply after user confirms
        true
    } catch (e: Exception) { false }
}
```

**Export UI** (in `SettingsView`):
- "Export Save" `TextButton` using `Buttons.DEFAULT()` style
- On click: call `exportToBase64()`, copy result to clipboard (`Gdx.app.clipboard.contents = encoded`), show brief "Copied to clipboard!" confirmation label that fades after 3 seconds

**Import UI** (in `SettingsView`):
- `TextField` for pasting a base64 string (labeled "Paste save code")
- "Import" `TextButton` using `Buttons.ACCENT()` style
- On click: call `importFromBase64(textField.text)`
  - If false (invalid/version mismatch): show error label "Invalid save code."
  - If true: show confirmation dialog "This will replace your current save. Continue?" with Confirm/Cancel buttons using `Buttons.DANGER()` and `Buttons.DEFAULT()` styles
  - On confirm: call `applyLoad(pendingImport)`, restart/reload the game state

**Version mismatch handling:** If imported save has a different `version` than `CURRENT_VERSION`, reject with "Save code is from an incompatible version." Do not attempt migration in this step — that is future work.

**Compile check:** `./gradlew core:compileKotlin` when done.
