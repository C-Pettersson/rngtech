# Unique Item Ideas

Status: Planned

This page collects candidate Unique machine parts, cells, and special components. These entries are not implemented resource ids and are not current gameplay until promoted by a PRD or implementation task.

Unique items should be authored, narrow, and find-only. A strong Unique item should feel like it came from one exact place in the world, solves one weird build problem, and carries one real drawback. It should not be a generic higher-stage upgrade.

## Design Rules

- Unique items use fixed identity traits and `UNIQUE` rarity.
- Unique items are not craftable, refinable, ascended, affix-injected, or debug-rerolled unless a future Unique-specific system says otherwise.
- Unique items should stack to one when they are machines, parts, cells, or other RPG-bearing targets.
- Unique items should have exact source hooks such as a structure chest, rare archaeology brush result, vault reward, boss drop, or explicit recipe-only salvage output.
- Unique items should usually target one host slot, one machine family, one recipe group, one fuel type, or one environmental niche.
- Unique items need a downside that is visible in stats or behavior.
- Unique items should not replace the material stage ladder. They should create a sidegrade or build-around choice.

## Candidate Items

Candidate ids are planning names only.

| Item | Candidate id | Type | Source hook | Niche identity | Tradeoff |
|---|---|---|---|---|---|
| Fortress Heater Element | `rngtech:fortress_heater_element` | Unique Heat Core | Rare extra roll in Nether fortress chests | Runs electric Furnace and Melter recipes at unusually high effective `MAX_TEMPERATURE` for its stage. Good for blaze, nether brick, and alloy heat gates. | High `ENERGY_USAGE`, reduced `TEMPERATURE_STABILITY`, and poor fuel efficiency in solid fuel-burning generators. |
| Blaze Cage Igniter | `rngtech:blaze_cage_igniter` | Unique Fuel Box | Nether fortress chests, weighted higher in fortress corridors than generic Nether loot | Converts blaze rods and blaze powder into bursty generator output and starts burn cycles quickly. | Rejects ordinary coal-like fuel and has low `STABILITY` when the installed Battery Cell is almost full. |
| Igloo Basement Thermostat | `rngtech:igloo_basement_thermostat` | Unique Heat Core | Igloo basement chest | Gives very high `TEMPERATURE_STABILITY` for low and mid temperature recipes. Good for precision Furnace or Metal Press work that punishes overheating. | Low `MAX_TEMPERATURE` ceiling and slow `HEAT_TRANSFER`. Bad for hot alloy work. |
| Shipwreck Salt-Zinc Anode | `rngtech:shipwreck_salt_zinc_anode` | Unique Battery Cell | Shipwreck supply chests | Early cell with Lead-like capacity before the player can build Lead cells. Useful for field machines and weak generators. | Heavy `IDLE_LOSS`, poor efficiency, and low output rate. |
| Bastion Coin-Stack Capacitor | `rngtech:bastion_coin_stack_capacitor` | Unique Battery Cell | Bastion treasure chest | Massive short burst output when installed in burst-capable Battery Chassis. | Small steady output, gold-heavy repair or recycling theme, and increased idle leakage. |
| End City Phase-Latched Cell | `rngtech:end_city_phase_latched_cell` | Unique Battery Cell | End city treasure | High capacity and strong input rate for late storage banks that need fast charging. | Output is intentionally modest unless the host chassis already has Burst Release behavior. |
| Ancient Echo Control Board | `rngtech:ancient_echo_control_board` | Unique Control Board | Ancient city chests | Exceptional `CALIBRATION_PRECISION` for Resonance Calibrator work on high-value machine components. | Reduces `PROCESSING_SPEED` and has no catalyst preservation. |
| Stronghold Marginalia Board | `rngtech:stronghold_marginalia_board` | Unique Control Board | Stronghold library chest | Adds `REFINEMENT_POTENTIAL_BONUS` to calibrated outputs that are already stage 6 or higher. | Bad early-game board: low stability and no speed value below stage 6. |
| Trail-Sherd Calibration Shim | `rngtech:trail_sherd_calibration_shim` | Unique Stabilizer Matrix | Rare archaeology brush result from trail ruins suspicious gravel | Improves `CALIBRATION_QUALITY` for low-stage components and failed upgrade recovery. | Penalty when used on stage 5+ outputs, so it stays an archaeology sidegrade instead of a universal matrix. |
| Mansion Escapement Servo | `rngtech:mansion_escapement_servo` | Unique Servo | Woodland mansion chest | Very fast first cycle after recipe change or machine idle, suited to small batch Metal Press work. | Worse sustained `PROCESSING_SPEED` and lower `STABILITY` during long runs. |
| Jungle Tripwire Governor | `rngtech:jungle_tripwire_governor` | Unique Servo | Jungle temple dispenser or chest loot | Strong power-drop grace and overheat tolerance for trap-like stop-start automation. | Lower baseline speed and cannot benefit from Bulk Speed-style sustained ramps. |
| Mineshaft Worn Commutator | `rngtech:mineshaft_worn_commutator` | Unique Servo | Abandoned mineshaft minecart chests | Cheap exploration servo that lets early presses tolerate uneven power. | Low precision and low durability theme: reduced `TEMPERATURE_STABILITY` and no high-stage recipe reach. |
| Prismarine Quench Lining | `rngtech:prismarine_quench_lining` | Unique Containment Lining | Elder guardian drop or ocean monument vault-style reward if added by a pack | Strong Potential Reactor `STABILITY` and residue safety for wet or mineral salvage recipes. | Reduces `ENERGY_GENERATION`, making it a safety part rather than a power part. |
| Dungeon Ash Filter | `rngtech:dungeon_ash_filter` | Unique Recovery Filter | Dungeon chest | Improves residue output from low-stage salvage and stripped RPG-bearing items. | Weak on high-stage targets and slows reactor processing. |
| Trial Vault Tempered Pawl | `rngtech:trial_vault_tempered_pawl` | Unique Disassembly Head | Trial vault reward | Component Recycler part tuned for failed upgrade outputs, fractured parts, and low-durability salvage. | Worse normal material recovery and low `OUTPUT_AMOUNT` on pristine parts. |
| Ruined Portal Crying Regulator | `rngtech:crying_regulator` | Unique Stabilizer Matrix | Ruined portal chest | Stabilizes Nether-adjacent calibration recipes and reduces overheat failure risk in heat-heavy component chains. | Adds energy cost and does nothing for ordinary metal-only calibration. |
| Buried Anchor Coil | `rngtech:buried_anchor_coil` | Unique Resonance Coil | Buried treasure chest | Strong calibration quality for dense, heavy materials such as Lead, Steel, and Tungstensteel. | Poor precision and weak results on light conductive materials such as Copper or Sparksteel. |
| Witch-Bottle Reflux Pump | `rngtech:witch_bottle_reflux_pump` | Unique Fluid Pump | Rare witch drop or swamp hut injected reward chest if a pack adds one | Excellent lubricant handling for Melter and Metal Press support recipes. | Bad general fluid transfer and no benefit for non-lubricant fluids. |

## Source Guidelines

Use sources that make the item's identity obvious:

| Source type | Good fit | Avoid |
|---|---|---|
| Structure chest | Nether fortress heat parts, stronghold control boards, shipwreck corroded cells | Generic "any dungeon chest" for a top-tier item |
| Archaeology | Ceramic shims, old calibration gauges, damaged prototype parts | Broad material-stage replacements |
| Boss or miniboss drop | Monument containment, Nether heat control, endgame phase storage | Mandatory progression drops for ordinary machines |
| Trial or vault reward | Stop-start servos, burst parts, risky high-value sidegrades | Required crafting ingredients for baseline chassis |
| Explicit salvage output | Broken Unique fragments, one-off prototype components | Automatic recycling of all Unique items |

## Promotion Checklist

Before moving an idea from this page into implementation, define:

- Final item name and resource id.
- Exact source table or drop hook.
- Compatible host machine and Gear slot.
- Fixed numeric modifiers and behavior flags.
- Tooltip identity text.
- Recycling or "do not recycle automatically" rule.
- Whether the item appears in JEI and how its source is explained.
- Manual tests for installation, rejection from incompatible slots, stat display, and Unique refinement rejection.

## Related Pages

- [Rarity](rarity.md)
- [Modifier Eligibility](modifier-eligibility.md)
- [Machine Parts](../content/machine-parts.md)
- [Battery Cells](../content/battery-cells.md)
- [Crafting and Upgrades](../systems/crafting.md#unique-components)
