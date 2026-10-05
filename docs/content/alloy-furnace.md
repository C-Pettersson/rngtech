# Alloy Furnace

Status: Prototype

Resource ids:

- `rngtech:bronze_alloy_furnace_chassis`
- `rngtech:steel_alloy_furnace_chassis`
- `rngtech:titanium_alloy_furnace_chassis`

## Summary

The Alloy Furnace is an FE-powered mixing machine for alloy blend and direct-ingot recipes. It consumes exact counted ratios from up to four input slots and outputs either material blend items or finished alloy ingots. Early access paths are deliberately slow, lossy, and additive-heavy, while stronger Alloy Furnace setups unlock cleaner direct-ingot recipes. The machine reports recipe FE/t demand; external FE intake is limited by free buffer or installed-cell space plus the source or attached Universal Connector.

Bronze is the first Alloy Furnace tier. Higher chassis and crucibles improve heat transfer, processing speed, stability, heat control, and active input slots through authored base stat catalogs.

## Implementation Contract

Current runtime surface:

- Block entity: `AlloyFurnaceBlockEntity`.
- Menu and screen: `AlloyFurnaceMenu` and `AlloyFurnaceScreen`.
- Recipe source: custom `rngtech:alloy_furnace` recipe type.
- Required Gear: one Heat Core and one Alloy Crucible.
- Optional Gear: one Battery Cell and one Servo.
- Automation: top inserts active process inputs, bottom extracts output, and sides receive FE. Gear slots are manual UI equipment.
- Refinement applies to the placed chassis through the Refinement tab and to Alloy Crucible stacks through the Affix Forge.
- JEI exposes alloy recipes, counted inputs, FE/tick cost, stage gates, heat gates, stability gates, and machine XP.
- Mastery applies to the placed chassis's `rngtech:machine_progression` component. Breaking and pick-blocking the chassis preserve Mastery state.

Machine/chassis numeric identity is authored through `MachineBaseStatCatalog`. Alloy Crucible numeric identity is authored through `ComponentBaseStatCatalog`. Fixed identity behavior remains in `MachineImplicitCatalog`; Steel and Titanium Alloy Furnace chassis carry `OUTPUT_GUARD`.

## Chassis

| Stage | Block | Notes |
|---:|---|---|
| 3 | `rngtech:bronze_alloy_furnace_chassis` | First full-yield alloy chassis. |
| 4 | `rngtech:steel_alloy_furnace_chassis` | Midgame chassis with output guard behavior. |
| 6 | `rngtech:titanium_alloy_furnace_chassis` | Advanced controlled alloy chassis with output guard behavior. |

## Alloy Crucibles

| Stage | Item | Input slots | Notes |
|---:|---|---:|---|
| 3 | `rngtech:bronze_alloy_crucible` | 3 | First valid crucible and Bronze blend gear, crafted through the Bronze Casing path. |
| 4 | `rngtech:steel_alloy_crucible` | 3 | Handles three-input layouts. |
| 6 | `rngtech:titanium_alloy_crucible` | 4 | Full four-slot mixing. |

## Recipe Use

Alloy Furnace recipes use counted, orderless ingredients:

```json
{
  "type": "rngtech:alloy_furnace",
  "group": "rngtech_direct_alloys",
  "mode": "direct_ingot",
  "ingredients": [
    {
      "ingredient": { "tag": "c:dusts/copper" },
      "count": 3
    },
    {
      "ingredient": { "tag": "c:dusts/tin" },
      "count": 1
    },
    {
      "ingredient": { "item": "minecraft:charcoal" },
      "count": 1
    }
  ],
  "result": {
    "count": 3,
    "id": "rngtech:bronze_blend"
  },
  "processing_ticks": 200,
  "energy": 2400,
  "minimum_component_stage": 3,
  "minimum_temperature": 900,
  "target_temperature": 900,
  "safe_maximum_temperature": 1035,
  "required_temperature_stability": 0.9
}
```

Alloy Furnace recipes may also declare `failure_output`, `failure_material`, `power_sensitive`, and `bonus_output`. `bonus_output` defaults to `true`; set it to `false` to turn off Super Output for a recipe that could otherwise form a loop. Processing waits for live heat to reach `target_temperature`, which defaults to `minimum_temperature`. Stage 4+ recipes with a failure output use the shared `6000` failure-strain threshold instead of hard-failing on stability after work starts. `power_sensitive: true` means empty FE after progress starts adds strain; empty FE during warmup only pauses and cools the machine.

The first Bronze path is a shapeless blend recipe outside the Alloy Furnace: `3` Copper Dust, `1` Tin Dust, and `1` Coal Dust make `2` Bronze Blend. Furnace recipes smelt Bronze Blend into Bronze Ingots. Once the Bronze Alloy Furnace setup is built, its three-slot Bronze Alloy Crucible can make `3` Bronze Blend from `3` Copper Dust, `1` Tin Dust, and `1` Charcoal at `900` heat. The later direct-ingot Bronze recipe makes `4` Bronze Ingots from `3` Copper Ingots, `1` Tin Ingot, and `1` Charcoal at `1100` heat.

Steel has a Stage 3 bootstrap recipe that runs in full Bronze Alloy Furnace gear and outputs Steel Blend from Iron Dust, Coal, and Charcoal at `900` heat over `1800` ticks for `36,000 FE`. Higher-heat Steel recipes use Coal Dust or direct Iron Ingot inputs: Iron Dust, Coal Dust, and Charcoal make Steel Blend at `1100` heat over `1200` ticks for `24,000 FE`; Iron Ingot, Coal Dust, and Charcoal make a Steel Ingot at `1100` heat over `1800` ticks for `36,000 FE`; and Iron Ingot plus Coal makes a Steel Ingot at `1300` heat over `1200` ticks for `24,000 FE`.

Invar keeps its existing direct-ingot Alloy Furnace recipes.

Sparksteel and Arclite are Stage 4 direct-ingot recipes. They use ingots plus Redstone, run slowly at `1200` ticks and `24,000 FE`, and output `1` alloy ingot so the first downstream coils, circuits, and Titanium infrastructure are reachable without adding alloy dust progression.

Higher direct-ingot recipes make Aethergold from Tin, Silver, and Glowstone Dust; Nullite from Lead, Platinum, and Ender Pearls; and Tungstensteel from Tungsten, Steel, and Coal Dust. These late recipes are intentionally slower and lossy compared with ordinary metal processing, and failure-bearing recipes output material-marked `rngtech:malformed_ingot` recovery stacks rather than ordinary progression inputs.

Bronze Blend and Steel Blend are default progression items and are tagged as `rngtech:alloy_blend_smeltables` so Bronze Furnace's `ALLOY_BLEND` behavior applies to their Furnace recipes. Invar Blend and Sparksteel Blend remain hidden compatibility ids with no default survival recipes.

## Mastery

Alloy Furnaces enter the [shared Machine Mastery tree](../systems/machine-mastery.md) at the Drive / Reserve start shared with the [Furnace](furnace.md). Base attributes are `0` Control, `10` Drive, and `10` Reserve. Besides the shared attribute conversions, each Control point grants `0.05%` increased Temperature Stability and each Reserve point grants `0.1%` increased Heat Isolation, as on the Furnace. Effects the Alloy Furnace cannot use, such as fuel duration, output amount, and parallel jobs, stay selectable but are marked inactive. As a heated machine, it gains the [tagged](../systems/machine-mastery.md#tagged-payoffs) speed bonuses of Low Heat Specialist and Flash Annealing along with their temperature caps.

Recipes may set `machine_xp` and an optional `machine_xp_band`; the band defaults from `target_temperature` on the same scale as Furnace recipes. XP is granted only after a completed craft merges its output. Failure outputs, stalled heat, blocked output, and power-starved ticks grant none.

| Default XP | Recipes |
|---:|---|
| 2 | Bronze Blend, Steel Blend bootstrap, Invar bootstrap |
| 3 | Bronze Ingot, Steel Blend, Steel Ingot from Coal Dust, Invar, Sparksteel, Arclite |
| 4 | Steel Ingot from Coal |
| 6 | Aethergold, Nullite, Tungstensteel |

Supercharged Silica Gel Beads grant no XP.

## Ascendancies

Status: Prototype

Alloy Furnace machines choose between Metallurgist and Blendwright when they use their first Ascendancy Seal. [Machine Mastery](../systems/machine-mastery.md#ascendancies) defines Seals, points, refunds, and switching, and [Machine Stats](../reference/machine-stats.md#ascendancy-stats) defines the new stats. The tables below are generated from the ascendancy catalog.

- Metallurgist banks Flux toward skipping a unit of an alloy’s largest ingredient, first on a tie. Only an ingredient that needs two or more units is saved, so no input reaches zero. Reactive Flux doubles the Flux Rate on direct-ingot recipes.
- Recipe Lock limits top automation to the current recipe’s inputs in recipe ratio. Dross Skimming refunds one unit of the recipe’s largest input on a failure.
- Blendwright favors blend recipes. Blend Reversal returns the blend whose furnace smelt makes a failed direct-ingot recipe’s ingot, never more than that recipe would make, and makes direct-ingot recipes 15% slower.
- Master Blend disables direct-ingot recipes on its chassis; the Process tab shows a recipe-disabled status for them.
- The Process tab draws the Flux or blend ledger under the progress bar.

<!-- ascendancy-trees:start -->

### Metallurgist

| Node | Type | After | Effect |
|---|---|---|---|
| **Flux Ledger** | Root | — | +10% Flux Rate. |
| Fluxed Charge | Small | Flux Ledger | +3% Flux Rate. |
| **Reactive Flux** | Notable | Fluxed Charge | Flux Rate is doubled on direct-ingot recipes. |
| Rich Flux | Small | Reactive Flux | +3% Flux Rate. |
| **Transmuter’s Rate** | Deep notable | Rich Flux | +12% Flux Rate; 25% less Processing Speed. |
| Steady Measure | Small | Flux Ledger | 8% increased Temperature Stability. |
| **Recipe Lock** | Notable | Steady Measure | Automation inserts only the current recipe’s inputs, one craft ahead at most. |
| Lucky Pour | Small | Recipe Lock | +1% Super Output. |
| **Unbroken Pour** | Deep notable | Lucky Pour | Super Output Cadence fixed at 32 cycles. |
| Skimmed Melt | Small | Flux Ledger | 8% increased Stability. |
| **Dross Skimming** | Notable | Skimmed Melt | A failure keeps one unit of the recipe’s largest input. |
| Banked Heat | Small | Flux Ledger | 5% reduced Energy Use. |
| **Heat Economy** | Notable | Banked Heat | Warmup costs 30% less FE. |

### Blendwright

| Node | Type | After | Effect |
|---|---|---|---|
| **Blend Reversal** | Root | — | +25% Blend Speed. A failed direct-ingot craft returns the blend its inputs would make, never more. Direct-ingot recipes are 15% slower. |
| Quick Blend | Small | Blend Reversal | +5% Blend Speed. |
| **Cold Mixing** | Notable | Quick Blend | +100 °C Blend Heat Reduction. |
| Warm Mix | Small | Cold Mixing | +5% Blend Speed. |
| **Continuous Pour** | Deep notable | Warm Mix | Back-to-back blend crafts skip re-warmup. |
| Steady Pace | Small | Blend Reversal | 6% increased Processing Speed. |
| **Blend Ledger** | Notable | Steady Pace | +25% Ledger Rate. |
| Measured Pace | Small | Blend Ledger | 6% increased Processing Speed. |
| **Master Blend** | Deep notable | Measured Pace | 100% more Ledger Rate. Direct-ingot recipes are disabled. |
| Even Heat | Small | Blend Reversal | 8% increased Temperature Stability. |
| **Tempered Crucible** | Notable | Even Heat | 40% more Temperature Stability on blend recipes. |
| Sure Footing | Small | Blend Reversal | 8% increased Stability. |
| **Steady Supply** | Notable | Sure Footing | Power-drop failure strain is halved. |

<!-- ascendancy-trees:end -->

## Related Pages

- [Furnace](furnace.md)
- [Machine Mastery](../systems/machine-mastery.md)
- [Machine Parts](machine-parts.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
