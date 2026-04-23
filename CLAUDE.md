# FactoryIdle — Claude Development Guide

## Stack
- **Language:** Kotlin
- **ECS:** Fleks 2.x
- **Framework:** LibGDX + LibKTX
- **Target:** Desktop (lwjgl3)
- **Serialization:** kotlinx.serialization (JSON)

## Modules
- **core** — All shared game logic and UI
- **lwjgl3** — Desktop launcher and config (1440×900, vsync)
- **android** — Android launcher (secondary target)

## Build & Run
```bash
./gradlew lwjgl3:run          # Run desktop
./gradlew core:compileKotlin  # Compile check (faster than full build)
./gradlew lwjgl3:jar          # Runnable JAR → lwjgl3/build/libs/
```

---

## What This Game Is
Idle factory game inspired by Factorio. No spatial world, no belts, no pipes. All resources flow through a single global pool. Core loop: hand mine → unlock buildings → automate → rebalance ratios.

---

## Core Architecture Rules
- `GlobalResourcePool: Map<Resource, Float>` is the single source of truth. All buildings read from and write to it.
- **No per-building inventory.** Local `ResourceBuffer` is anti-jitter only (3 cycles of inputs). `BufferFillSystem` fills it from the global pool each tick. `ProductionSystem` reads from the buffer only — never directly from the pool.
- **Buildings are never items in the resource pool.** Construction deducts resources and creates an ECS entity directly.
- **Phase 1:** one ECS entity = one individual building. **Phase 2+:** one ECS entity = one BuildingGroup.

## Global State (lives outside ECS)
```
GlobalResourcePool      Map<Resource, Float>
LifetimeMiningStats     Map<Resource, Float>
UnlockRegistry          Set<BuildingType>, Set<Resource>
RecipeRegistry          Map<BuildingType, List<Recipe>>
UnassignedPool          Map<BuildingType, Int>
ResearchManager         active research goal + progress float
```

---

## ECS Components
```kotlin
enum class ResourceCategory { RAW, PROCESSED, INTERMEDIATE, SCIENCE }

enum class Resource(val category: ResourceCategory) {
    IRON_ORE(RAW), COAL(RAW), STONE(RAW),
    IRON_PLATE(PROCESSED)
}

enum class BuildingType { STONE_FURNACE, BASIC_MINER }

enum class GroupState { RUNNING, STALLED, FUEL_STARVED, PAUSED, NO_RECIPE }

enum class GroupPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST }

data class Building(val type: BuildingType)           // Phase 1 individual buildings only

data class BuildingGroup(                              // Phase 2+ replaces Building
    val id: String,
    val type: BuildingType,
    var name: String,
    var count: Int = 0,
    var priority: GroupPriority = GroupPriority.NORMAL,
    var paused: Boolean = false
)

data class Producer(
    var recipe: Recipe? = null,
    var progress: Float = 0f,
    var groupState: GroupState = GroupState.NO_RECIPE
)

data class Miner(
    var assignedResource: Resource? = null,
    var progress: Float = 0f,
    var groupState: GroupState = GroupState.NO_RECIPE
)

data class FuelConsumer(
    val fuelType: Resource,
    val consumeRate: Float,       // per second (e.g. 1/30f)
    var fuelBuffer: Float = 0f
)

data class ResourceBuffer(
    val capacity: Map<Resource, Float>,
    val contents: MutableMap<Resource, Float> = mutableMapOf()
)

// Recipe is a data definition, NOT a component
data class Recipe(
    val inputs: Map<Resource, Float>,
    val outputs: Map<Resource, Float>,
    val duration: Float
)
```

---

## ECS System Execution Order (do not reorder)
1. `BufferFillSystem` — sorts groups by GroupPriority (HIGHEST first); fills buffers from global pool; skips paused groups
2. `ProductionSystem` — advances producers; reads buffer, writes pool on cycle complete; skips paused or count=0; multiplies all rates by group count
3. `MinerSystem` — same pattern as ProductionSystem; deposits directly to global pool
4. `FuelSystem` — drains fuel buffer over time; tops up from global pool; sets FUEL_STARVED if empty; skips paused
5. `MilestoneSystem` — checks pending conditions each tick; fires reward callbacks; removes completed milestones

---

## Fleks 2.x API Notes
- World builder: `configureWorld { }` — import `com.github.quillraven.fleks.configureWorld`. Not `world {}` or `World {}`
- No `components { }` block — components are auto-registered plain Kotlin classes
- Systems added as instances: `systems { add(SomeSystem()) }`
- `injectables { add(dependency) }` registers by type name

---

## Scene2D Key Pitfalls
- **Never size widgets directly** — always size the cell (`cell.width()`, `cell.prefWidth()`, etc.)
- `expandX()` + `fillX()` almost always needed together — expand gives the cell space, fill makes the widget use it
- Stack cell must have `.prefWidth(0f).minWidth(0f)` — prevents layout being forced to the size of its largest child
- `setFillParent(true)` on the root table only
- Use `ChangeListener` on buttons, not `ClickListener`
- `stage.isDebugAll = true` during layout work — always remove before committing

---

## Workflow
Always present a plan and wait for explicit approval before making any code changes.

---

## Rules for Claude Sessions
- **No spatial elements** (no grid, no belts, no pipes) — intentional design
- **No per-building inventory** — global pool only; local buffer is anti-jitter only
- **Do not reorder ECS systems** — execution order is load-bearing
- **Milestones check lifetime stats, not current pool**
- **Fuel is separate from recipe inputs** — FuelConsumer stays composable
- **Recipe is a data class, not an ECS component**
- **RecipeRegistry is data-driven** — new buildings = new config entries, not new code paths
- **Buildings never enter the resource pool**
- **Never auto-merge groups** — always an explicit player action
- **GroupPriority shown by name only** — never as a number
- **Phase 1 = individual building entities** — BuildingGroup is a Phase 2 research unlock
- **ResearchManager is outside ECS** — research progress is never a Resource value
- **Stay in Phase 1 scope** unless explicitly told otherwise

---

## Design Docs
Read the relevant doc for your task — do not read all of them.
- `docs/design-systems.md` — milestones, production logic, research, hand mining, Phase 1 buildings, save/load
- `docs/design-buildings.md` — building groups, priority, construction, assignment flow, group actions
- `docs/design-ui.md` — navigation, all views, resource bar, factory view, group cards, MVC pattern, GameScreen structure
- `docs/design-assets.md` — full asset list with sizes and colors, nine-patch guide, font pipeline, atlas setup
