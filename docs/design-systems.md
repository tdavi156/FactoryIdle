# FactoryIdle — Game Systems Design

Covers: resource categories, hand mining, milestones, production mechanics, research, Phase 1 buildings, construction, save/load.

---

## Resource Categories

Resources are categorized for display and organization purposes.

```kotlin
enum class ResourceCategory { RAW, PROCESSED, INTERMEDIATE, SCIENCE }

enum class Resource(val category: ResourceCategory) {
    IRON_ORE  (RAW),
    COAL      (RAW),
    STONE     (RAW),
    IRON_PLATE(PROCESSED),
    // Phase 2+: COPPER_ORE(RAW), COPPER_PLATE(PROCESSED),
    //           IRON_GEAR(INTERMEDIATE), COPPER_WIRE(INTERMEDIATE),
    //           BASIC_CIRCUIT(INTERMEDIATE), RED_SCIENCE(SCIENCE), etc.
}
```

**Display rules for the resource bar:**
- RAW: show always once unlocked, even at zero quantity
- PROCESSED / INTERMEDIATE / SCIENCE: show only if unlocked AND (quantity > 0 OR actively produced/consumed)
- Resources the player has never seen stay hidden — the bar grows progressively as the factory scales
- Categories are collapsible by the player

---

## Hand Mining

Always available, never removed. The player's permanent escape hatch against soft-locks.

**Mechanics:**
- Player clicks/taps a resource button to begin a 2-second mining cycle
- Yields 1 unit of that resource per cycle
- Auto-idles after one cycle begins — no repeated clicks required
- Direct write to both `GlobalResourcePool` and `LifetimeMiningStats`
- Not tracked via ECS at all — purely a UI action that mutates global state

**Available resources:** Iron Ore from start. Coal, Stone unlocked via milestones. Any future RAW category resource added to the game should be hand-mineable by default.

**UI placement:** Persistent widget in the resource bar area (not a full navigation view). One button per unlocked RAW resource with a progress bar showing current 2s cycle.

---

## Milestone System

Milestones are one-time triggers that fire rewards when conditions are met. They drive the entire early-game unlock progression.

**How they work:**
- Each milestone has a condition lambda and a reward lambda
- Conditions read from `LifetimeMiningStats` — not from current pool amounts. A player who mined 20 iron ore and spent it all still satisfies a "10 iron ore mined" condition.
- `MilestoneSystem` checks pending conditions each tick
- On condition met: reward fires, milestone removed from pending list, never fires again

```kotlin
data class Milestone(
    val id: String,
    val description: String,
    val condition: () -> Boolean,   // reads LifetimeMiningStats or other global state
    val reward: () -> Unit          // mutates UnlockRegistry
)
```

### Phase 1 Unlock Chain

| Condition | Unlock |
|---|---|
| Game start | Hand mine Iron Ore |
| 10 iron ore mined (lifetime) | Coal (hand minable) |
| 10 coal + 20 iron ore mined (lifetime) | Stone (hand minable) |
| 30 iron ore + 20 coal + 10 stone mined (lifetime) | Stone Furnace (buildable) |
| 10 iron plates produced (lifetime) | Basic Miner (buildable) |

Future phases will add milestone chains for new building types and resource tiers, but research (not milestones) becomes the primary unlock driver from Phase 2 onward. Milestones are the tutorial scaffold and late-game prestige goals.

---

## Production Mechanics

### The Buffer Anti-Jitter System

Every building (or BuildingGroup in Phase 2) has a local `ResourceBuffer` holding 3 cycles of inputs. This exists solely to prevent stalls caused by tick timing — a building that theoretically has enough resources globally should never stall because a tick boundary fell at the wrong moment.

**Execution flow each tick:**
1. `BufferFillSystem` pulls from `GlobalResourcePool` into local buffers (up to capacity)
2. `ProductionSystem` reads only from the local buffer — never touches the global pool as input
3. On cycle complete: inputs consumed from buffer, outputs written to global pool

**Do not shortcut this.** Never have ProductionSystem read directly from the global pool.

### Fuel System

Fuel (coal in Phase 1, electricity in later phases) is a composable concern separate from recipe inputs. Both Miners and Furnaces use the same `FuelConsumer` component — the system is shared.

- Fuel buffer tops up from the global pool each tick (handled by FuelSystem, not BufferFillSystem)
- If the fuel buffer empties, the building is marked `FUEL_STARVED` and halts
- A building can be `FUEL_STARVED` even if recipe inputs are available — these are distinct stall reasons
- `FuelSystem` skips paused buildings entirely

### Group State Machine (Phase 2+)

Systems set state automatically based on conditions. Player can only set PAUSED.

```
RUNNING      → all inputs and fuel available, producing normally
STALLED      → fuel available, recipe inputs missing from global pool
FUEL_STARVED → recipe inputs available, fuel/power missing
PAUSED       → player-set hard stop; systems skip this entity entirely
NO_RECIPE    → no recipe or resource assigned; inert, not an error state
```

A PAUSED building cannot become STALLED or FUEL_STARVED — systems skip it.

---

## Phase 1 Buildings

Phase 1 uses individual building entities. Groups are a Phase 2 research unlock.

### Stone Furnace
- **Cost:** 5 stone
- **Fuel:** Coal at 1 unit / 30s (FuelConsumer component)
- **Recipe:** 2 iron ore → 1 iron plate per 5s cycle
- **Buffer:** 6 iron ore, 6 coal (3 cycles × 2 inputs each)
- **Construction:** timed, sequential queue

### Basic Miner
- **Cost:** 5 stone + 5 iron plates
- **Assignment:** one RAW resource type (not a recipe — uses Miner component, not Producer)
- **Rate:** 1 resource / 4s
- **Fuel:** Coal at 1 unit / 30s (FuelConsumer component)
- **Buffer:** fuel buffer only (miners have no recipe input buffer)
- **Construction:** timed, sequential queue

### Phase 1 Production Ratios (reference)
| Building | Consumes | Produces |
|---|---|---|
| Stone Furnace | 24 iron ore/min + 2 coal/min (fuel) | 12 iron plates/min |
| Basic Miner | 2 coal/min (fuel) | 15 resources/min |

**Stable first factory (1 furnace, fully fed):**
- 2× iron miners → 30 iron ore/min (furnace needs 24, 6 surplus)
- 1× coal miner → 15 coal/min
- Coal demand: 3 miners × 2/min + 1 furnace × 2/min = 8/min total
- 1 coal miner (15/min) handles all fuel with headroom

As more furnaces are added, coal demand climbs until a second coal miner is needed. This ratio-rebalancing is the core Phase 1 gameplay loop.

---

## Construction System

Buildings are **never items in the resource pool.** They are not crafted into inventory and then placed. The flow is:

1. Player clicks Build [Building Type] in the Factory view
2. If cost is not met: button is greyed out, no action
3. If cost is met: resources deducted immediately from `GlobalResourcePool`, timed construction begins
4. While constructing: building appears in a construction queue with a progress indicator
5. On completion: ECS entity is created and appears in the building list ready for recipe/resource assignment

**Phase 1 construction:** sequential queue (one at a time, hand-crafting)

**Phase 2+ research unlocks:** parallel construction slots (e.g. 2 slots, then 4 slots). This is a research reward, not a baseline feature.

Construction times are tuned during testing for pacing. The timer exists to replace the routing constraint that Factorio uses — without spatial routing, timed construction is the throttle on exponential scaling.

---

## Research System (Phase 2+)

Research is the primary unlock driver from Phase 2 onward. Milestones remain the tutorial scaffold; research is the ongoing progression engine.

**How it works:**
- Player selects one active research goal from a tech tree or tiered list
- Science labs are specialized producer entities: they consume science packs (from the global pool) over time and output progress to `ResearchManager` — not to the global pool
- `ResearchManager` (global state, outside ECS) tracks `activeGoal` and `progress: Float`
- Multiple science labs stack their contribution rate — 10 labs complete research 10× faster than 1
- When `progress >= goal.cost`, research completes, fires its unlock reward, player selects next goal

**Science lab behavior vs. assemblers:**
- Assemblers require a recipe to be manually set
- Science labs automatically consume whatever science packs are required for the currently selected research — the player does not manually set a recipe on a science lab
- If no research is selected, science labs idle

**Research progress is never a Resource enum value.** It lives entirely in `ResearchManager`. Do not add it to `GlobalResourcePool`.

---

## Save / Load

Serialize to JSON via kotlinx.serialization. Minimum save state:

```
GlobalResourcePool          Map<Resource, Float>
LifetimeMiningStats         Map<Resource, Float>
UnlockRegistry              Set<BuildingType>, Set<Resource>
UnassignedPool              Map<BuildingType, Int>
PlacedBuildings (Phase 1)   List of: type, assignedRecipe/Resource, progress, fuelBuffer
BuildingGroups (Phase 2+)   List of: id, name, type, count, priority, paused, recipe/resource, progress, buffer contents
ActiveResearch              goal id + progress float
CompletedMilestones         Set<String> (milestone ids already fired)
```

Construction queue state (buildings mid-construction) should also be serialized with remaining time.
