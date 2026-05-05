# FactoryIdle — Claude Development Guide

## Stack
- **Language:** Kotlin
- **JVM target:** 11 (core and lwjgl3 modules — required by Fleks 2.11)
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
- **No per-building inventory.** Each entity declares continuous consumption rates (`declaredRates`) to the pool. The pool tick computes `currentSatisfaction` (0.0–1.0) per entity based on available supply and priority tier. Output = fractional accumulation of `baseOutput × mkMultiplier × currentSatisfaction`. Cycle timers never change — only output scales.
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

Enums live in `data/Enums.kt`. `Recipe` is a plain data class in `data/RecipeRegistry.kt` — it is **not** a component.

Each ECS component is **its own file** in the `components/` package. Never group multiple components in one file. Every component implements `Component<T>` with a companion object as its `ComponentType<T>`:

```kotlin
data class Producer(...) : Component<Producer> {
    override fun type() = Producer
    companion object : ComponentType<Producer>()
}
```

---

## ECS System Execution Order (do not reorder)
1. `PoolTickSystem` — iterates all active entities; computes `currentSatisfaction` per entity per resource using priority-tier allocation; proportional within tier; writes satisfaction back to each entity; skips paused entities
2. `ProductionSystem` — advances cycle timers; on cycle complete: `fractionalAccumulator += baseOutput × mkMultiplier × currentSatisfaction`; awards whole items to pool; skips paused or no-recipe entities
3. `MinerSystem` — same pattern as ProductionSystem; deposits directly to global pool
4. `FuelSystem` — computes fuel satisfaction per entity using the same priority model; marks FUEL_STARVED if fuel satisfaction = 0; skips paused
5. `MilestoneSystem` — checks pending conditions each tick; fires reward callbacks; removes completed milestones

---

## Fleks 2.x API Notes
- World builder: `configureWorld { }` — import `com.github.quillraven.fleks.configureWorld`. Not `world {}` or `World {}`
- Components must implement `Component<T>` with a companion object `ComponentType<T>` — the companion is the key used in families and `entity[...]` access
- No `components { }` registration block — components are picked up automatically via their `ComponentType`
- Systems added as instances: `systems { add(SomeSystem()) }`
- `injectables { add(dependency) }` registers by type name
- Inject inside a system: `private val pool = world.inject<GlobalResourcePool>()`
- Component access: `entity[Producer]` (throws if absent), `entity.getOrNull(Producer)` (nullable), `entity has Producer` (boolean)
- Family defined in system constructor: `IteratingSystem(family { all(Producer) })`
- `World.Companion.family { }` is available during `configureWorld` — import `com.github.quillraven.fleks.World.Companion.family`

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
- **No per-building inventory** — satisfaction rate model only; `declaredRates` + `currentSatisfaction` per entity; no local buffer
- **Do not reorder ECS systems** — execution order is load-bearing; PoolTickSystem must run before ProductionSystem
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
