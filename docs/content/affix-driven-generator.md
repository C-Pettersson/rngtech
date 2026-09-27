# Affix-Driven Generator

Status: Planned


Planned resource id: `rngtech:affix_driven_generator`

## Summary

The Affix-Driven Generator is a generator whose operating behavior comes from its rolled modifiers.

Most machines use affixes to improve stats. This generator should make the affix set itself the fuel plan, control logic, and risk profile. A normal generator works at low output. A Magic or Rare generator becomes stronger when the player builds around its conditions.

## Design Role

The Affix-Driven Generator should be the most RNGTech-specific power source in the early-to-mid power lineup.

It turns machine rarity into gameplay instead of a pure stat bonus. Players should look at a rolled name and decide how to place, fuel, throttle, or pair the generator.

Primary design goals:

- Make generator rolls change machine operation, not just output numbers.
- Reward odd affix combinations with build-around power.
- Give the Affix Forge a direct energy-generation target.
- Keep bad rolls usable at baseline output.

## Affix Behavior Contract

The generator should read its own implicit, prefix, and suffix modifiers at runtime.

Affixes can affect:

| Effect Area | Examples |
|---|---|
| Output shape | Steady output, burst output, pulsed output, delayed output. |
| Trigger condition | Runs near active machines, while charging a battery, during redstone pulses, or while heat is nearby. |
| Risk profile | Backlash, cooldown, output variance, or instability under high generation. |
| Routing behavior | Prefer adjacent battery blocks, wires, or active machines. |
| Recovery behavior | Store overflow, vent excess, or throttle when the buffer is full. |

Concrete named behavior should live on future modifier pages once those modifiers are finalized. Until then, examples on this page are design directions.

## Power Model

The Affix-Driven Generator should have a weak base output and stronger conditional output.

Suggested calculation shape:

```text
base FE/t = generator tier value
stat output = base FE/t * ENERGY_GENERATION * EFFICIENCY
condition multiplier = product of satisfied behavioral conditions
effective FE/t = stat output * condition multiplier
```

`ENERGY_GENERATION` should scale output. `EFFICIENCY` should reduce fuel, charge, or condition cost if the machine uses one. `STABILITY` should reduce variance and backlash from conflicting conditions.

## Rarity Interaction

Rarity should change how many behavior hooks the generator can carry.

| Rarity | Expected Behavior |
|---|---|
| Normal | Low output, no rolled build-around behavior. |
| Magic | One prefix and one suffix can create a readable condition pair. |
| Rare | Multiple affixes can stack conditions, create conflicts, or unlock stronger payoffs. |
| Unique | Reserved for fixed authored generators if Unique machine behavior is added later. |

Rare generators should not become automatically better. A Rare with conflicting conditions should ask the player to solve a layout, redstone, heat, or storage problem.

## Example Behavior Directions

These are examples, not finalized modifier definitions.

| Direction | Gameplay Hook |
|---|---|
| Battery-linked | Generates more while charging a nearby Battery Chassis or installed Battery Cell. |
| Heat-linked | Converts nearby furnace or fuel-generator heat into extra output. |
| Pulse-linked | Produces better output from changing redstone input than from a constant signal. |
| Refinement-linked | Gains a bonus from remaining Refinement Potential, but loses it if fully spent. |
| Instability-linked | Higher output at low stability, with waste or cooldown risk. |

Any direction promoted into a real named modifier should get its own modifier page and eligibility row.

## Machine UI

The generator screen should show why it is producing its current output.

Required UI information:

- Current FE/t.
- Base FE/t before conditions.
- Active and inactive affix conditions.
- Buffer level.
- Risk, cooldown, or instability state.
- Refinement tab for the placed generator.

The screen should avoid hiding key behavior in tooltips only. If an affix changes generation logic, the current condition state should be visible while the machine runs.

## Gear

The Affix-Driven Generator should use Gear slots that interpret, amplify, and stabilize its rolled modifiers. The first implementation should expose the Affix Lens, Resonance Coil, and Control Board slots. Routing Bus can wait until adjacent-machine targeting needs more control.

| Gear Slot | Required? | Role | Stat and Behavior Hooks |
|---|---|---|---|
| Affix Lens | Yes | Reads the generator's rolled affixes and determines which conditions can become active. | Condition detection, trigger strength, `PROCESSING_LEVEL` |
| Resonance Coil | Yes | Converts satisfied conditions into FE and sets the conditional-output ceiling. | `ENERGY_GENERATION`, `EFFICIENCY`, condition multiplier |
| Control Board | No | Handles redstone modes, condition priority, and conflicts between affixes. | `STABILITY`, pulse behavior, cooldown behavior |
| Routing Bus | No | Targets nearby batteries, wires, or machines when an affix cares about neighbors. | `ENERGY_TRANSFER`, routing preference, overflow behavior |

The Affix Lens should make the machine readable before it makes the machine strong. A low-stage lens can support simple conditions such as redstone or nearby storage. Higher-stage lenses can support compound conditions, conflicting affixes, and future enchant-style behavior.

## Automation

The Affix-Driven Generator should use standard capabilities:

- Energy capability for FE output.
- Item capability only if a specific affix or recipe path needs input or residue.

Redstone should be part of the machine contract because several planned behavior directions depend on pulses, thresholds, or external control.

## Modifier Eligibility

| Modifier Source | Notes |
|---|---|
| Machine implicit | Identity can define the generator as affix-sensitive. |
| Machine prefix | Can affect capacity, trigger sensitivity, stability, or input behavior. |
| Machine suffix | Can affect `ENERGY_GENERATION`, `EFFICIENCY`, pulse behavior, routing, or cooldown. |
| Machine enchant | Reserved for high-impact conditional generation behavior. |
| Machine part modifiers | Control boards, resonance parts, and stabilizers can tune conditions or reduce conflicts. |

## Implementation Notes

No Affix-Driven Generator block, block entity, recipe type, menu, screen, loot table, or machine chassis subtype is currently registered.

A future implementation should start with two or three condition families and expose them clearly in the UI. The first pass should avoid a large library of named behavioral modifiers until the stat and refinement loops feel good in play.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Affix Forge](affix-forge.md)
- [Battery Chassis](battery-chassis.md)
- [Machine Chassis](machine-chassis.md)
- [Machine Stats](../reference/machine-stats.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
- [Affix Generation](../systems/affix-generation.md)
- [Modifiers Overview](../modifiers/index.md)
