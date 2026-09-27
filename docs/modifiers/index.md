# Modifiers

This page is the modifier inventory. Use [Affix Generation](../systems/affix-generation.md) for roll mechanics, slots, operations, rarity interaction, and tier ranges. Use [Modifier Eligibility](../reference/modifier-eligibility.md) for capability flags and profile-level eligibility.

## Code-Backed Rollable Named Affixes

These named affix definitions have `CAN_ROLL` and can be selected by normal rolls, Affix Injector, Ascension Matrix, Expansion Crystal, transmutation, Chaos Crystal, and Debug Reroller when the target profile allows them. Each affix stores an id, slot, modifier group, tier data, and one or more effects.

| Affix family | Slot | Operation / effects | Notes |
|---|---|---|---|
| Machine energy capacity | Prefix | `ADD ENERGY_CAPACITY_FLAT` | Uses the machine flat capacity range. |
| Battery Cell energy capacity | Prefix | `ADD ENERGY_CAPACITY_FLAT` | Uses the larger Battery Cell flat capacity range. |
| Battery Chassis increased capacity | Prefix | `INCREASED_PERCENT ENERGY_CAPACITY` | Scales installed-cell capacity while cells remain in the chassis. |
| Battery Chassis Charged Storage | Prefix | `MORE ENERGY_CAPACITY` | Untiered `100% more` installed-cell capacity after the placed chassis has held energy for `10` continuous minutes. |
| [Battery Chassis Balance Mode](balance-mode.md) | Prefix | Behavioral `CHARGE_BALANCER` | Fills and drains installed Battery Cells evenly. Single-tier prefix. |
| Battery Chassis Additional Battery Slots | Prefix | `ADD BATTERY_SLOTS` | Adds active Battery Cell slots using the Battery Chassis slot tier table. |
| Durability | Prefix | `ADD DURABILITY` or `INCREASED_PERCENT DURABILITY` | Tool Heads, Tool Rods, and Cavitation Rotors can roll either flat or percent durability prefixes. |
| Tool Self Repair | Prefix | `ADD SELF_REPAIR` | Tool Heads and Tool Rods can roll passive repair. Assembled tools restore durability every `7.5` seconds while idle in the hotbar or offhand; active mining or tool-use pauses it. |
| Tool Attack Speed | Suffix | `ADD ATTACK_SPEED` | Tool Heads and Tool Rods can roll flat main-hand swing cadence. Assembled tools derive their attack speed from installed parts. |
| Vein Miner | Prefix | `ADD VEIN_MINE_LIMIT` plus `ADD VEIN_MINE_FE_USAGE` | Rare Pick Head-only compound prefix. Assembled Picks break connected same-type ore blocks up to the limit; each extra block requires the listed FE surcharge. |
| Stat affix families | Prefix or suffix | Active stat's normal operation | Each concrete stat family has one legal slot and one modifier group. Upgrading raises the tier of that same affix instead of adding another named variant from the same family. |
| Processing-specific speed | Suffix | `INCREASED_PERCENT PROCESSING_SPEED` | Work-specific names such as Crushing, Smelting, Pressing, Melting, Alloying, Calibrating, Screening, and Disassembling. These share the `processing_speed` modifier group with generic speed, Bulk Speed, and Overclocked. |
| Overclocked | Suffix | `INCREASED_PERCENT PROCESSING_SPEED` plus `INCREASED_PERCENT ENERGY_USAGE` | Powered-processing hybrid. Tier 4 caps at `+30%` speed and `+20%` energy usage. |
| Instant Process | Suffix | `ADD INSTANT_PROCESS_CHANCE` | Chance for eligible processing to complete instantly if the machine can pay the full up-front cost. |
| Super Output | Suffix | `ADD SUPER_OUTPUT_CHANCE` | Chance for item-output processors to add one extra copy of the base stackable output. |
| Fuel duration | Suffix | `INCREASED_PERCENT FUEL_DURATION` | Extends generator burn ticks for eligible fuel-burning generators and parts without increasing FE/t. |
| Crush Head Pulverizing | Prefix | `INCREASED_PERCENT OUTPUT_AMOUNT` | Tiered Crush Head prefix using Coarse, Grinding, Pulverizing, and Micronizing display names. |
| Crush Head Jagged | Prefix | `ADD CRUSHER_SALVAGE_CHANCE` | Tiered Crush Head prefix using Nicked, Jagged, Serrated, and Rending display names. |
| Crush Head Kinetic | Prefix | `INCREASED_PERCENT PROCESSING_SPEED` | Tiered Crush Head prefix using Quickened, Kinetic, Momentum-Driven, and Impulse-Forged display names. |
| Crush Head Scuffed | Prefix | `ADD NO_BATTERY_OUTPUT_RETENTION` | Untiered low-impact filler prefix that barely softens the missing-Battery-Cell output penalty. |
| Crush Head Dust Groove | Prefix | `ADD CRUSHER_INPUT_FILTER` | Untiered low-impact filler prefix that rejects non-recipe items from top automation. |
| Peak Solar | Suffix | `INCREASED_PERCENT PEAK_SOLAR_GENERATION` | Solar Panel-only zenith-window output bonus. |
| Energy transfer, generation, efficiency, stability, heat, calibration, fluid, tool, and battery stats | Prefix or suffix | Active stat's normal operation | Rollable only on profiles whose current runtime logic consumes the stat. |

## Code-Backed Rollable Behavioral Modifiers

These behavior definitions can be selected by normal machine rolls and Debug Reroller when the target profile allows them. Bulk Speed is stored directly as a behavior flag. Balance Mode is stored as a prefix affix that enables a behavior and occupies a normal prefix slot.

| Modifier | Status | Notes |
|---|---|---|
| [Balance Mode](balance-mode.md) | Implemented | Battery Chassis prefix that enables `CHARGE_BALANCER`; fills and drains installed cells evenly. |
| [Bulk Speed](bulk-speed.md) | Implemented | Ramps processing speed by `1%` per completed process, up to `100%`; higher speed raises powered machines' FE/t. It uses the `processing_speed` modifier group, so it cannot coexist with generic speed, processing-specific speed, or Overclocked. |

## Fixed Authored Implicit Modifiers

These modifier definitions do not have `CAN_ROLL`. They are authored onto specific item, machine, or part identities, still display and serialize as modifiers, and still affect stats.

| Modifier | Slot | Operation | Current use |
|---|---|---|---|
| Processing level | Implicit | `ADD PROCESSING_LEVEL` | Crush Heads and Reactor Chambers. |
| Processing speed | Implicit | `INCREASED_PERCENT` or `DECREASED_PERCENT PROCESSING_SPEED` | Authored machine-part identity behavior. |
| Energy generation | Implicit, prefix, and suffix | `ADD ENERGY_GENERATION` for component bases and flat generator prefixes; `INCREASED_PERCENT ENERGY_GENERATION` for generator suffixes | Heat Cores, Reactor Chambers, and profiles with `HAS_FE_GENERATION`. |
| Fuel efficiency | Implicit | `INCREASED_PERCENT` or `DECREASED_PERCENT FUEL_EFFICIENCY` | Heat Cores and Fuel Boxes. |
| Max temperature | Implicit and prefix | `ADD MAX_TEMPERATURE` for component bases; `INCREASED_PERCENT` and flat `ADD MAX_TEMPERATURE` for heat affixes | Heat Cores, heat-capable parts, and heat-capable machine profiles. |
| Heat isolation | Implicit | `INCREASED_PERCENT` or `DECREASED_PERCENT HEAT_ISOLATION` | Heat-aware parts. |
| Fuel slots | Implicit | `ADD INPUT_SLOTS` | Fuel Box identity behavior. |
| Stability | Implicit | `INCREASED_PERCENT` or `DECREASED_PERCENT STABILITY` | Fuel Boxes, Reactor Chambers, Containment Linings, and Servos. |
| Temperature stability | Implicit | `INCREASED_PERCENT TEMPERATURE_STABILITY` | Servo identity behavior. |
| Overheat tolerance | Implicit | `INCREASED_PERCENT OVERHEAT_TOLERANCE` | Servo identity behavior. |

## Named Behavioral Modifiers

Named behavioral modifiers are documented as pages once they become concrete design or implementation targets. Rows without pages are tracked in [Modifier Eligibility](../reference/modifier-eligibility.md#modifier-matrix) until they need a dedicated page. Instant Process, Super Output, and Overclocked are named rollable stat affixes, not behavior flags.

| Modifier | Status | Page |
|---|---|---|
| Auto Balance | Planned | [Auto Balance](auto-balance.md) |
| Balance Mode | Implemented | [Balance Mode](balance-mode.md) |
| Auto Eject | Planned | Eligibility matrix only |
| Batch Start | Planned | Eligibility matrix only |
| Bulk Speed | Implemented | [Bulk Speed](bulk-speed.md) |
| Catalyst Saver | Deferred | Eligibility matrix only |
| Energy category modifiers | Prototype | See code-backed stat modifiers above |
| Fuel Governor | Prototype fixed behavior; rolled named modifier planned | [Fuel Governor](fuel-governor.md) |
| Fuel Reserve | Planned | Eligibility matrix only |
| Graceful Progress | Planned | Eligibility matrix only |
| Input Filter | Planned | Eligibility matrix only |
| Input Pairing | Planned | Eligibility matrix only |
| Jam Bypass | Planned | Eligibility matrix only |
| Output Sorter | Planned | Eligibility matrix only |
| Overflow Guard | Planned | Eligibility matrix only |
| Packer | Planned | [Packer](packer.md) |
| Priority Feed | Planned | Eligibility matrix only |
| Recipe Lock | Planned | Eligibility matrix only |
| Remainder Keeper | Planned | Eligibility matrix only |
| Round Robin Output | Planned | Eligibility matrix only |
| Smart Fuel | Planned | Eligibility matrix only |

## Related Pages

- [Affix Generation](../systems/affix-generation.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Machine Stats](../reference/machine-stats.md)
