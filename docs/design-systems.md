# FactoryIdle — Game Systems Design

Covers: resource taxonomy, hand mining, milestones, production mechanics (satisfaction model), fractional accumulation, fuel system, coal usage rules, group states, buildings, science packs, research, construction, save/load.

---

## Resource Taxonomy

Three categories of items in the global pool, organized by production complexity.

**Raw** — gathered directly from the world via a building or hand mining. No crafting step.
**Processed** — derived from raw resources in a single production step.
**Component** — made from processed materials, other components, or a mix (including raws).

### Raw Resources

| Tier | Resources |
|---|---|
| Early | Iron Ore, Copper Ore, Coal, Stone, Water (Water Pump only — not mineable) |
| Mid | Crude Oil, Sulfur, Tin Ore, Lead Ore |
| Late | Bauxite, Silicon Ore, Quartz, Titanium Ore |
| Tier 4 | Tungsten Ore, Uranium Ore, Rare Earth Minerals |

### Processed Materials

| Tier | Item | Inputs | Building |
|---|---|---|---|
| Early | Iron Plate | Iron Ore | Furnace |
| Early | Copper Plate | Copper Ore | Furnace |
| Early | Stone Brick | Stone | Furnace |
| Early | Steam | Water | Boiler |
| Early | Steel Plate | Iron Ore + Coal *(coal = recipe input)* | Furnace |
| Mid | Tin Plate | Tin Ore | Furnace |
| Mid | Lead Plate | Lead Ore | Furnace |
| Mid | Light Oil | Crude Oil | Refinery |
| Mid | Heavy Oil | Crude Oil | Refinery |
| Mid | Petroleum Gas | Crude Oil | Refinery |
| Mid | Rubber | Heavy Oil | Chemical Plant |
| Mid | Plastic | Coal + Crude Oil *(coal = recipe input)* | Chemical Plant |
| Mid | Sulfuric Acid | Sulfur + Water | Chemical Plant |
| Late | Aluminium Plate | Bauxite | Furnace |
| Late | Titanium Plate | Titanium Ore | Furnace |
| Late | Silicon Wafer | Silicon Ore | Furnace |
| Late | Quartz Crystal | Quartz | Furnace |
| Late | Ice | Water | Cryogenic Cooler |
| Tier 4 | Tungsten Plate | Tungsten Ore | Furnace |
| Tier 4 | Uranium Fuel Rod | Uranium Ore | Centrifuge |
| Tier 4 | Enriched Uranium | Uranium Ore (higher cost, alt recipe) | Centrifuge |
| Tier 4 | Rare Earth Oxide | Rare Earth Minerals | Furnace |

### Components

| Tier | Item | Key Inputs |
|---|---|---|
| Early | Iron Gear | Iron Plate |
| Early | Copper Wire | Copper Plate |
| Early | Iron Pipe | Iron Plate |
| Early | Iron Rod | Iron Plate |
| Early | Basic Circuit Board | Copper Wire + Iron Plate |
| Early | Concrete | Stone Brick + Water + Iron Plate |
| Mid | Tin Wire | Tin Plate |
| Mid | Solder | Tin Plate + Lead Plate |
| Mid | Lead Acid Cell | Lead Plate + Sulfuric Acid |
| Mid | Signal Shield | Lead Plate + Copper Wire |
| Mid | Steel Gear | Steel Plate |
| Mid | Steel Pipe | Steel Plate |
| Mid | Steel Rod | Steel Plate |
| Mid | Rubber Seal | Rubber |
| Mid | Bearing | Steel Plate + Tin Plate |
| Mid | Electric Motor | Copper Wire + Steel Plate + Iron Gear |
| Mid | Battery | Lead Acid Cell + Rubber Seal + Tin Wire |
| Mid | Advanced Circuit Board | Tin Wire + Solder + Basic Circuit Board |
| Mid | Logic Board | Advanced Circuit Board + Signal Shield + Plastic |
| Mid | Pneumatic Tube | Rubber Seal + Steel Pipe |
| Mid | Reinforced Concrete | Concrete + Steel Rod |
| Late | Aluminium Frame | Aluminium Plate |
| Late | Titanium Frame | Titanium Plate |
| Late | Tempered Glass | Quartz Crystal + Titanium Plate |
| Late | Silicon Chip | Silicon Wafer + Solder |
| Late | Quartz Oscillator | Quartz Crystal + Tin Wire |
| Late | Processing Unit | Logic Board + Silicon Chip + Sulfuric Acid |
| Late | Servo Motor | Electric Motor + Bearing + Advanced Circuit Board |
| Late | Carbon Fibre | Heavy Oil + Titanium Plate |
| Late | Carbon Composite | Carbon Fibre + Tempered Glass |
| Late | Advanced Battery | Battery + Silicon Chip + Aluminium Plate |
| Late | Heat Sink | Aluminium Plate + Steel Plate |
| Late | Insulated Wire | Copper Wire + Rubber |
| Late | Precision Gear | Titanium Plate + Steel Gear |
| Tier 4 | Tungsten Carbide | Tungsten Plate + Carbon Fibre |
| Tier 4 | Rare Earth Magnet | Rare Earth Oxide + Steel Plate |
| Tier 4 | Superconductor | Rare Earth Oxide + Tin Wire + Rubber Seal |
| Tier 4 | Quantum Circuit | Processing Unit + Quartz Oscillator + Rare Earth Magnet |
| Tier 4 | Fusion Cell | Enriched Uranium + Titanium Frame + Rare Earth Magnet |
| Tier 4 | Reactor Core | Uranium Fuel Rod + Tungsten Carbide + Heat Sink |
| Tier 4 | Cryogenic Coil | Superconductor + Aluminium Frame + Ice |
| Tier 4 | Electromagnetic Coil | Rare Earth Magnet + Tin Wire + Steel Rod |

**Display rules for the resource bar:**
- RAW: show always once unlocked, even at zero quantity
- PROCESSED / COMPONENT / SCIENCE: show only if unlocked AND (quantity > 0 OR actively produced/consumed)
- Resources the player has never encountered stay hidden — the bar grows progressively as the factory scales
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

**Available resources:** Iron Ore from start. Coal, Stone, Copper Ore unlocked via milestones and early research. Any RAW resource should be hand-mineable once unlocked.

**UI placement:** Persistent widget in the resource bar (not a nav view). One button per unlocked RAW resource with a progress bar showing the current 2s cycle.

---

## Milestone System

Milestones are one-time triggers that fire rewards when conditions are met. They drive the early-game unlock progression and serve as the tutorial scaffold.

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

Research (not milestones) becomes the primary unlock driver from Phase 2 onward.

---

## Production Mechanics — Satisfaction Rate Model

The global pool allocates resources to buildings continuously based on declared consumption rates. There is no per-building inventory buffer. Production output scales with satisfaction — cycle timers never change.

### Declared Rates

Each building or group registers its consumption rate per resource with the pool:
```
declaredRate[resource] = (inputs_per_cycle / cycle_time_seconds) × group_count
```

This registration is immediate whenever configuration changes (recipe assigned, count changed, tier upgraded, paused/unpaused). The pool's demand picture is always current.

### Pool Tick Algorithm

```
for each resource R:
    supply = R.inboundRate    // sum of all producer output rates for R
    remaining = supply

    for tier in [HIGHEST, HIGH, NORMAL, LOW, LOWEST]:
        groups = activeGroupsConsuming(R, tier)
        tierDemand = sum(g.declaredRates[R] for g in groups)
        if tierDemand == 0: continue

        if remaining >= tierDemand:
            // full satisfaction — entire tier met
            for g in groups: g.currentSatisfaction[R] = 1.0
            remaining -= tierDemand
        else:
            // proportional split within tier — all groups share the shortfall equally
            ratio = remaining / tierDemand
            for g in groups: g.currentSatisfaction[R] = ratio
            remaining = 0
            break   // nothing flows to lower tiers

// overall group satisfaction = worst input resource
g.currentSatisfaction = min(g.currentSatisfaction[R] for R in g.recipe.inputs)
```

**Key rules:**
- Priority is strict — a higher tier is fully satisfied before anything flows to the next
- Within a tier, distribution is proportional to declared consumption rate. Two groups at the same priority level, one consuming 60/s and one consuming 40/s, both run at the same satisfaction percentage when supply is short
- Group satisfaction = minimum satisfaction ratio across all input resources. The binding constraint resource determines overall output even if other inputs are fully satisfied
- Both resources are still debited at their full declared rate regardless of which is the binding constraint — a group that is iron-constrained but copper-sufficient still drains copper normally
- `declaredRates` and `currentSatisfaction` are derived values — **not serialized to save state**. They are recomputed from saved building configurations on the first pool tick after loading

### Output on Cycle Complete

Cycle timers never change. Only output scales with satisfaction:

```
fractionalAccumulator += baseOutput × mkMultiplier × currentSatisfaction
if fractionalAccumulator >= 1.0:
    wholeItems = floor(fractionalAccumulator)
    pool[outputResource].stock += wholeItems
    fractionalAccumulator -= wholeItems
```

`fractionalAccumulator` is persisted per entity (serialized to save state). It ensures smooth, correct output for individual Phase 1 buildings and large Phase 2 groups alike.

### Status Indicators

Satisfaction drives the visual indicators on group cards and tooltips:

| Satisfaction | Indicator | Tooltip |
|---|---|---|
| 100% | None | — |
| 60–99% | Amber (starved) | Running at X%. Starved for: [resource] — Available: Y/s, Needed: Z/s |
| 1–59% | Orange (severely starved) | Running at X%. Starved for: [resource] — Available: Y/s, Needed: Z/s |
| 0% | Red (stalled) | No input available. Starved for: [resource] — Available: 0/s, Needed: Z/s |

If multiple resources are simultaneously below 100%, all are listed in the tooltip ordered by severity (lowest satisfaction first). The binding constraint is always named first.

These indicators are permanent game mechanics — they are present throughout the entire game, not tutorial artifacts.

---

## Fractional Accumulation

A single mechanism handles two distinct cases. Both use the same `fractionalAccumulator` float per entity.

**1. Partial satisfaction (all building tiers):**
A building at 80% satisfaction accumulates 0.8 per cycle. After 5 cycles it has produced 4 items (4 × 0.8 = accumulated 4.0, awarded as 4 whole items). Output averages correctly without requiring fractional item quantities in the pool.

**2. Mk output multipliers:**
- Mk2 multiplier = 1.25× — every 4 cycles awards a bonus item (4 × 0.25 = 1.0)
- Mk3 multiplier = 1.5× — every 2 cycles awards a bonus item (2 × 0.5 = 1.0)

Both effects combine in one accumulation step:
```
fractionalAccumulator += baseOutput × mkMultiplier × currentSatisfaction
```

Fully deterministic — players can plan ratios around guaranteed average rates. No randomness.

---

## Coal Usage Rules

Coal has two distinct roles. Systems must never conflate them.

**Coal as fuel (invisible):** Consumed by the building as power. Not listed as a recipe ingredient. Handled exclusively by `FuelConsumerComponent` and `FuelSystem`. Applies to: Stone Furnace, Basic Miner, Boiler.

**Coal as recipe ingredient (visible):** Coal is a chemical input to the recipe. Shows in the recipe picker, tracked in `declaredRates` as a recipe input. Applies to: Steel Plate recipe, Plastic recipe.

A Stone Furnace smelting Steel Plate consumes coal in **both** roles simultaneously — as invisible fuel AND as a visible recipe ingredient. `FuelSystem` handles the fuel draw; the satisfaction model handles the recipe input draw. These are tracked entirely separately.

---

## Fuel System

Fuel (coal in Phase 1, electricity in later phases) is a composable concern separate from recipe inputs. The `FuelConsumerComponent` is shared across all fuel-burning buildings.

- Each building with a `FuelConsumerComponent` declares its fuel consumption rate to the pool separately from its recipe input rates
- `FuelSystem` runs each tick, computes fuel satisfaction using the same rate-based model as recipe inputs
- If fuel satisfaction = 0 the building is marked `FUEL_STARVED` and produces nothing that tick
- A building can be `FUEL_STARVED` even when recipe inputs are fully satisfied — these are distinct failure states
- `FuelSystem` skips paused buildings entirely

---

## Group State Machine

Systems set all states automatically except PAUSED, which only the player sets.

```
RUNNING      → satisfaction > 0 for fuel and all recipe inputs; production occurring (may be partial rate)
STALLED      → at least one non-fuel recipe input has satisfaction = 0; no output
FUEL_STARVED → fuel satisfaction = 0; no output regardless of recipe input state
PAUSED       → player-set hard stop; systems skip entity entirely; declaredRates = 0
NO_RECIPE    → no recipe assigned; inert, not an error state
```

**Key rules:**
- RUNNING includes partial satisfaction — a building producing at 60% is RUNNING. The satisfaction indicator communicates the partial rate; the state communicates that production is occurring
- A PAUSED building cannot become STALLED or FUEL_STARVED — systems skip it, and its declared rates are zero so it does not compete for resources
- Production resumes mid-cycle on unpause; `fractionalAccumulator` is preserved

---

## Production Facilities

### Miners

| Building | Fuel | Multiplier | Cycle | Avg Rate |
|---|---|---|---|---|
| Basic Miner | Coal | 1.0× | 4s | 15/min |
| Miner Mk1 | 60W | 1.0× | 2s | 30/min |
| Miner Mk2 | 150W | 1.25× | 1.5s | 50/min |
| Miner Mk3 | 375W | 1.5× | 1s | 90/min |

### Furnaces

| Building | Fuel | Multiplier | Cycle | Avg Rate |
|---|---|---|---|---|
| Stone Furnace | Coal | 1.0× | 4s | 15/min |
| Furnace Mk1 | 60W | 1.0× | 2s | 30/min |
| Furnace Mk2 | 150W | 1.25× | 1.5s | 50/min |
| Furnace Mk3 | 375W | 1.5× | 1s | 90/min |

### Assemblers

| Building | Power | Multiplier | Cycle | Avg Rate |
|---|---|---|---|---|
| Assembler Mk1 | 75W | 1.0× | 4s | 15/min |
| Assembler Mk2 | 190W | 1.25× | 3s | 25/min |
| Assembler Mk3 | 475W | 1.5× | 2s | 45/min |

### Refinery & Chemical Plant

| Building | Power | Multiplier | Cycle | Avg Rate |
|---|---|---|---|---|
| Refinery | 100W | 1.0× | 3s | 20/min |
| Refinery Mk2 | 250W | 1.5× | 1.5s | 60/min |
| Chemical Plant | 100W | 1.0× | 3s | 20/min |
| Chemical Plant Mk2 | 250W | 1.5× | 1.5s | 60/min |

### Other Buildings

| Building | Notes |
|---|---|
| Water Pump | Passive water extraction. No fuel, no power. Slow rate; multiple needed at scale. |
| Boiler | Coal (recipe input) + Water → Steam. Coal is a recipe ingredient here, not a fuel-consumer. |
| Steam Engine | Steam → Electricity. Unlocks the power grid. Gates all electric buildings. |
| Oil Pump | Crude Oil extraction. Electric. |
| Research Facility | Consumes science packs → research progress. Electric. Auto-assigns pack requirements from active research goal — no manual recipe set. |
| Industrial Water Pump | High-rate water extraction. Electric. Replaces multiple Water Pumps. |
| Solar Panel | Passive electricity generation. No fuel, no recipe. Supplements the grid. |
| Battery Bank | Stores electricity; smooths supply/demand spikes. Passive. |
| Cryogenic Cooler | Water → Ice. Very high power draw. Required active support building for Nuclear Reactor. |
| Nuclear Reactor | Uranium Fuel Rod or Enriched Uranium → massive electricity output. Requires active Cryogenic Cooler. |
| Centrifuge | Uranium Ore → Uranium Fuel Rod, or Enriched Uranium (alt recipe). Electric. |

### Building Upgrade Chains

Mk upgrades consume the previous tier as a construction input — they are not built from raw materials:

- Basic Miner → Miner Mk1 (consumes 1 Basic Miner)
- Miner Mk1 → Miner Mk2 (consumes 1 Miner Mk1)
- Miner Mk2 → Miner Mk3 (consumes 1 Miner Mk2)
- Same chain pattern for Furnace, Assembler, Refinery, Chemical Plant

Upgrading deducts 1 from the lower-tier unassigned pool and adds 1 to the upgraded-tier unassigned pool. The player must have built the lower tier before upgrading.

---

## Science Packs

Produced by Assemblers; consumed by Research Facilities. Each pack uses materials available at or before its own research tier — a pack cannot require inputs that it itself unlocks.

| Pack | Inputs |
|---|---|
| Red Science | 1 Iron Plate + 1 Copper Wire |
| Orange Science | 1 Iron Gear + 1 Copper Plate + 1 Stone Brick |
| Yellow Science | 1 Steel Plate + 1 Tin Plate + 1 Quartz Crystal |
| Green Science | 1 Rubber + 1 Basic Circuit Board + 1 Electric Motor |
| Blue Science | 1 Advanced Circuit Board + 1 Aluminium Plate + 1 Bearing |
| Purple Science | 1 Processing Unit + 1 Titanium Plate + 1 Battery |

Research time: 60 seconds per science unit per Research Facility. 10 facilities processing 100 science = 10 units each = 6 minutes total.

---

## Research System

Research is the primary unlock driver from Phase 2 onward.

**How it works:**
- Player selects one active research goal from a tiered list
- Research Facilities consume science packs from the global pool and write progress to `ResearchManager` (outside ECS) — never to the global pool
- Multiple Research Facilities stack their contribution rate
- When `progress >= goal.cost`, research completes, fires its unlock reward, player selects next goal

**Research progress is never a Resource enum value.** It lives entirely in `ResearchManager`.

### Research Tiers Overview

| Tier | Pack | Theme | Key Unlocks |
|---|---|---|---|
| 1 | Red | Getting automation started | Basic Miner, smelting recipes, Assembler Mk1, Research Facility, Red Science |
| 2 | Orange | Scaling up, first power grid | Boiler, Steam Engine, Water Pump, Steel Plate, Tin, group management I (split/merge) |
| 3 | Yellow | Fluids, chemicals, mid materials | Crude Oil, Refinery, Chemical Plant, Rubber, Plastic, Battery, Furnace Mk1, priority system |
| 4 | Green | Advanced components, power maturity | Advanced circuits, Al/Si/Ti, Miner/Furnace/Assembler Mk2, Solar, Battery Bank, group templates |
| 5 | Blue | High tech, endgame prep | Processing Units, Quartz, Tungsten, Rare Earth, Superconductors, Mk3 buildings |
| 6 | Purple | Endgame mastery, max automation | Uranium, Nuclear, Quantum Circuits, mass production multipliers, Overclock, conditional group automation |

Notable Tier 6 research: Mass Production I/II/III (+10% throughput each), Efficiency I/II (-15% fuel each), Overclock I (125% speed at 150% power cost), Overclock II (150% speed at 200% power cost), Infinite Scaling (repeatable, +5% output per level).

---

## Phase 1 Buildings

Phase 1 uses individual building entities. Groups are a Phase 2 research unlock.

### Stone Furnace
- **Cost:** 5 stone
- **Fuel:** Coal via `FuelConsumerComponent` — invisible, not a recipe input
- **Recipes:** Iron Ore → Iron Plate (4s cycle, 15/min); Stone → Stone Brick; Iron Ore + Coal → Steel Plate (coal IS a recipe input for this recipe)
- **Construction:** timed, sequential queue

### Basic Miner
- **Cost:** 5 stone + 5 iron plates
- **Component:** `ProducerComponent` — same as all other buildings. Recipe picker is restricted to RAW resource recipes by `RecipeRegistry` (no inputs, 1 resource output, 4s cycle). The resource to mine is chosen by assigning one of these recipes.
- **Rate:** 1 resource per 4s → 15/min
- **Fuel:** Coal via `FuelConsumerComponent`
- **Construction:** timed, sequential queue

### Phase 1 Production Ratios (reference)

| Building | Consumes | Produces |
|---|---|---|
| Stone Furnace | 30 iron ore/min + coal (fuel) | 15 iron plates/min |
| Basic Miner | coal (fuel) | 15 resources/min |

**Stable first factory (1 furnace, fully fed):**
- 2× iron miners → 30 iron ore/min (matches furnace consumption)
- Fuel demand: 3 buildings × ~2 coal/min = ~6 coal/min total
- 1 coal miner (15/min) covers all fuel with comfortable headroom

As more furnaces are added, coal fuel demand climbs until a second coal miner is needed — this ratio-rebalancing is the core Phase 1 loop.

---

## Construction System

Buildings are **never items in the resource pool.** They are not crafted into inventory and then placed.

1. Player clicks Build in the Factory view
2. If cost is not met: button is greyed out, no action
3. If cost is met: resources deducted immediately from `GlobalResourcePool`, timed construction begins
4. While constructing: building appears in a queue with a progress indicator
5. On completion: ECS entity is created, ready for recipe assignment

**Phase 1:** sequential queue (one at a time).
**Phase 2+ research unlocks:** parallel construction slots (2 slots, then 4 slots).

Construction times are tuned during testing. The timer replaces the routing constraint that Factorio uses — without spatial routing, timed construction throttles exponential scaling.

---

## Save / Load

Serialize to JSON via kotlinx.serialization. Minimum save state:

```
GlobalResourcePool          Map<Resource, Float>
LifetimeMiningStats         Map<Resource, Float>
UnlockRegistry              Set<BuildingType>, Set<Resource>
UnassignedPool              Map<BuildingType, Int>
PlacedBuildings (Phase 1)   List of: type, assignedRecipe, cycleProgress, fractionalAccumulator
BuildingGroups (Phase 2+)   List of: id, name, type, count, priority, paused, recipe, cycleProgress, fractionalAccumulator
ActiveResearch              goal id + progress float
CompletedMilestones         Set<String> (milestone ids already fired)
ConstructionQueue           List of: type, remainingTime
```

**Not serialized (derived on load):** `declaredRates`, `currentSatisfaction` — recomputed from saved configurations on the first pool tick after loading.

Include a `version` field in the save schema now. Future schema migrations depend on it.
