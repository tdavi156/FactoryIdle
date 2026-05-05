# FactoryIdle — Building Management Design

Covers: individual buildings (Phase 1), building groups (Phase 2+), unassigned pool, group states, priority, construction, all player actions, assignment flow.

---

## Phase 1: Individual Buildings

In Phase 1 the player has few enough buildings (typically 5–15 total) that individual management is tractable and teaches the mechanics. Each ECS entity is one physical building. No grouping UI exists yet.

The player interacts with individual buildings directly:
- Click a building in the building list to open its detail panel
- Assign a recipe (for furnaces) or a resource (for miners)
- See its current state (running, stalled, fuel-starved)

This is intentionally simple. The group system unlocks in early Phase 2 when assemblers arrive and the player would otherwise manage 20+ identical buildings one by one.

---

## Phase 2+: Building Groups

### The Core Principle
Players never interact with individual buildings directly once groups unlock. A building type (e.g. "Assembler") is a count you own. Groups are how you direct that count. The simulation unit is the group, not the individual building.

One ECS entity = one BuildingGroup. All production rates, buffer sizes, and fuel consumption scale linearly with `group.count`.

```kotlin
data class BuildingGroup(
    val id: String,
    val type: BuildingType,
    var name: String,                          // player-renameable, e.g. "Iron Gears #1"
    var count: Int = 0,                        // buildings assigned to this group
    var priority: GroupPriority = NORMAL,
    var paused: Boolean = false
)
```

A group with `count = 0` is valid — it acts as a named placeholder with a recipe set, ready to receive buildings. Useful for pre-planning before buildings are built.

---

## The Unassigned Pool

`UnassignedPool: Map<BuildingType, Int>` holds buildings not yet assigned to any group.

- Newly constructed buildings always go to the unassigned pool first
- Groups pull from and return buildings to the unassigned pool
- Displayed in the Factory view as: **"Assemblers: 47 unassigned"**

---

## Group Unlock Transition

When the groups research completes (early Phase 2):
- All existing individual buildings are **automatically grouped by their current recipe/resource** — one group created per active recipe, named automatically (e.g. "Iron Plates #1")
- Buildings with no assignment go to the unassigned pool
- No manual action required from the player
- The player immediately feels the UI simplify — 20 individual furnace rows collapse into 2 group cards

From this point, all building simulation is at the group level.

---

## Group States

Five mutually exclusive states. Systems set all states automatically except PAUSED, which only the player sets.

| State | Color | Hex | Meaning |
|---|---|---|---|
| RUNNING | Green | `#27ae60` | Producing — satisfaction > 0 for all inputs (may be partial rate) |
| STALLED | Yellow | `#f39c12` | At least one recipe input has satisfaction = 0; no output |
| FUEL_STARVED | Orange | `#e67e22` | Fuel satisfaction = 0; no output regardless of recipe inputs |
| PAUSED | Red | `#c0392b` | Player-set hard stop |
| NO_RECIPE | Grey | `#7a8090` | No recipe assigned; inert, not an error |

**Key rules:**
- RUNNING includes partial satisfaction — a group producing at 60% is RUNNING. The satisfaction indicator on the card communicates the partial rate; the state communicates that production is occurring
- STALLED and FUEL_STARVED are distinct — they give the player different diagnostic information
- A PAUSED group cannot become STALLED or FUEL_STARVED; systems skip it entirely and its declared rates are zero so it does not compete for resources
- `fractionalAccumulator` is preserved on pause; production resumes mid-cycle on unpause

State colors appear in two places on each group card: a small dot (bottom-left) and a thin border around the entire card. Both carry the same color. The border enables quick scanning across many cards; the dot confirms on closer inspection.

---

## Priority System

```kotlin
enum class GroupPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST }
```

Priority controls how resources are allocated when supply cannot meet total demand. The pool tick evaluates tiers in strict order (HIGHEST first). Whatever supply remains after a tier is fully satisfied flows to the next tier. If a tier cannot be fully satisfied, lower tiers receive nothing.

Within a tier, supply is distributed proportionally to each group's declared consumption rate — a group consuming 60/s and one consuming 40/s both run at the same satisfaction percentage. Neither is arbitrarily favoured.

**Rules:**
- Priority is always displayed by name, never as a number
- Default for new groups: NORMAL
- Priority affects recipe input allocation only — fuel allocation follows the same priority model independently via `FuelSystem`
- Priority is a soft, ongoing preference. For a hard stop, use pause.
- Setting a group to HIGH when supply is tight has real visible consequences: lower-tier groups may receive zero supply until the High tier is fully met

**Use case:** Iron plates are scarce. Player sets circuit board group to HIGH, gear group to LOWEST. Circuits run at full satisfaction; gears run at whatever is left — without the player having to manually intervene each tick. If supply is so tight that HIGH groups alone drain it all, LOWEST groups stall entirely.

**UI control:** A stepper widget on the group detail view: `[◀]  Normal  [▶]` cycling through the five named levels. Never show numbers.

---

## Priority vs Pause — When to Use Each

| Situation | Tool |
|---|---|
| Ongoing preference when resources are tight | Priority |
| Temporarily freeing up inputs for another group to catch up | Pause |
| Definitive "stop this forever until I say so" | Pause |
| Burst capacity that self-throttles under low supply | Low/Lowest priority (no manual action needed) |

---

## Multiple Same-Recipe Groups (Baseline + Burst Pattern)

Multiple groups of the same building type and recipe are fully supported and intentional. Example:

- **Group A:** 1000 assemblers — Iron Gears — NORMAL priority — never paused → baseline production
- **Group B:** 500 assemblers — Iron Gears — LOW priority — toggled on/off → burst capacity

When iron plates run short, the pool tick satisfies NORMAL tier first. Group B (LOW) receives only what remains and its satisfaction ratio drops first — it self-throttles automatically without manual intervention. The player only pauses Group B when they want a definitive hard stop, not just a soft throttle.

This pattern is a core late-game tool. **Never auto-merge groups** — same recipe groups exist together intentionally.

---

## Assignment Flow — Never Auto-Merge

When a player assigns unassigned buildings to a recipe:
1. A **new group is always created** by default
2. The UI surfaces any existing groups with the same recipe as a shortcut: "Add to [Group Name]?" with a list
3. The player makes the explicit choice — new group or add to existing
4. If they choose an existing group, buildings are added and count increases

Auto-merge never happens silently. The player is always in control of group boundaries.

---

## All Player Group Actions

### Create Group
1. Click "New Group" in the Factory view for a given building type
2. Choose recipe (or resource for miners) from picker
3. Set count via +/− stepper or slider (capped at current unassigned pool size; 0 is valid)
4. Confirm → group created, buildings moved from unassigned pool to group

### Add Buildings to Group
- Open group detail view → click "+ Add" → enter count → confirm
- Or: "Quick Fill" button — moves all unassigned buildings of that type into this group in one action (useful late-game when you just want to dump new buildings somewhere)

### Remove Buildings from Group
- Open group detail view → click "− Remove" → enter count → confirm
- Removed buildings return to unassigned pool
- Remaining group production scales down proportionally
- No recipe change, no disruption, no buffer flush

### Change Recipe
- Open group detail view → tap recipe field → recipe picker opens
- Selecting a new recipe shows confirmation: "Change recipe to [X]? Current progress will reset."
- On confirm: buffer flushed to global pool (raw inputs returned, not crafted), progress resets, new recipe assigned, production restarts next tick

### Pause / Unpause
- Toggle on the group card or in the group detail view
- State cycles: RUNNING ↔ PAUSED
- No count or recipe change
- Buffer frozen while paused, resumes mid-cycle on unpause

### Split Group
- In group detail view → "Split" button
- Player enters count to split off
- Prompt: assign recipe to new group, or skip (split buildings return to unassigned pool)

### Merge Groups
- In group list: select two or more groups of the same building type
- "Merge" option appears
- If same recipe: confirm immediately
- If different recipes: recipe picker appears; player chooses which recipe the merged group uses; other recipe's buffer is flushed and progress resets

### Rename Group
- Tap the group name anywhere it appears (card or detail view)
- Inline text field opens
- Default names are auto-generated (e.g. "Iron Gears #1") but should be renamed by the player for clarity

### Disband Group
- In group detail view → "Disband" button → confirmation prompt
- All buildings returned to unassigned pool
- Buffer flushed entirely to global pool
- Group entity deleted

---

## Recipe Change Rules

There is no local buffer to flush. When a recipe changes, the satisfaction model transitions cleanly:

| Trigger | Behaviour |
|---|---|
| Recipe changed | Old declared rates deregistered immediately; `fractionalAccumulator` resets to 0; new rates registered; production of new recipe begins on next tick |
| Group disbanded | Declared rates drop to zero; entity removed from pool tick; `fractionalAccumulator` discarded |
| Count reduced | Declared rates recomputed for new count immediately; `fractionalAccumulator` scales proportionally |

Because resources are allocated continuously (not batched into a buffer), there are no partially-consumed inputs to return on recipe change. The pool simply stops debiting the old inputs and begins debiting the new ones. Any in-progress accumulation is discarded on recipe change — the cycle restarts cleanly.

---

## Auto-Allocate (Future / Late Game)

Groups can optionally have an allocation target — a desired output rate. The system calculates required building count and pulls from the unassigned pool automatically.

```kotlin
data class AllocationTarget(
    val targetOutputPerMinute: Float,
    val resource: Resource
)
```

If target cannot be met (insufficient unassigned buildings), the group runs at max available capacity and flags as undersupplied. It does not pull from other groups.

This is a late-game QoL feature. Do not implement until explicitly scoped.

---

## Example Player Workflow

```
Player has 100 unassigned assemblers.

→ New Group → Iron Plates recipe → count 70 → "Iron Plates #1"
→ New Group → Gear recipe       → count 30 → "Gears #1"
Unassigned: 0
[Iron Plates #1]  70 — RUNNING (green)
[Gears #1]        30 — RUNNING (green)

Iron supply gets tight, plates falling behind:
→ Open [Gears #1] → Pause
[Gears #1]        30 — PAUSED (red)
[Iron Plates #1]  70 — RUNNING (green, full resources now)

Plates catch up:
→ Open [Gears #1] → Unpause
[Gears #1]        30 — RUNNING (resumes mid-cycle)

Player builds 50 more assemblers → Unassigned: 50
→ Open [Iron Plates #1] → Quick Fill
[Iron Plates #1] 120 — RUNNING
Unassigned: 0

Player wants burst capacity for gears:
→ New Group → Gear recipe → count 0 → "Gears Burst"  (placeholder)
→ Later: when 40 more assemblers built, Quick Fill [Gears Burst]
[Gears #1]        30 — RUNNING  (NORMAL priority, always on)
[Gears Burst]     40 — RUNNING  (LOW priority, self-throttles when iron short)
```
