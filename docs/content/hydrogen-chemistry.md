# Hydrogen Chemistry

Status: Planned

Use this page for the proposal only. When it ships, player behavior moves to the wiki ([Power Chains](https://c-pettersson.github.io/rngtech/power-chains/), the machine pages, and [Rarity and Affixes](https://c-pettersson.github.io/rngtech/rarity-and-affixes/)), design rules move to [Machine Design Notes](../reference/machine-guidelines.md#machine-design-notes), and this page is deleted.

Planned resource ids: `rngtech:urea`, the `rngtech:annealed` data component, and new recipes on existing machines. No new machine blocks.

## Summary

Hydrogen has one sink today: the Ammonia Synthesizer. This package gives the Stage 6 gases several competing uses, so scaling the chain means choosing where Hydrogen, Ammonia, and Carbon Exhaust go instead of building one fixed line. Every piece is opt-in, and none of them makes the current chain harder.

| Piece | Makes | Costs | Choice it creates |
|---|---|---|---|
| [Water-gas shift](#water-gas-shift) | More Hydrogen | CO stops feeding algae at the fast rate | Closed carbon loop or more Hydrogen |
| [Urea](#urea) | Fertilizer | Ammonia and Carbon Exhaust | Power now or biomass later |
| [Urea fertilizer](#urea-fertilizer) | Faster algae and sapling growth | Urea | Build space instead of power |
| [Hydrogen annealing](#hydrogen-annealing) | Lucky RP on Gear, lucky calibration stability | Hydrogen per pressed item | Power, refinement budget, or calibration odds |
| [Hydrogen in the Fuel Cell](#hydrogen-in-the-fuel-cell) | FE without Nitrogen | Less than half the FE of the Ammonia route | A short chain or the full chain |

It extends the [Nitrogen atmosphere](https://c-pettersson.github.io/rngtech/heat/#nitrogen-atmosphere), which already gives Nitrogen a second sink.

## Design Role

The yardstick for every Hydrogen use is its value as Ammonia. One reform turns 1,000 mB Methane into 1,500 mB Hydrogen and 500 mB Carbon Monoxide. 1,500 mB Hydrogen and 500 mB Nitrogen make 1,000 mB Ammonia, worth 216,000 FE in the Fuel Cell. After the Synthesizer's 12,000 FE, Hydrogen is worth about **136 FE per mB** through Ammonia.

Balance rules:

- No loop may print power. Any path that turns Ammonia back into Hydrogen (Urea into biomass into Methane) must return less Hydrogen than the Ammonia cost.
- Each use pays off in something other than FE: Hydrogen output, build space, roll quality, or a shorter chain.
- Costs stay visible: on the recipe, in a tank gauge, or on the item. Nothing drains silently.
- Cross-stage use is welcome. A Stage 6 gas can improve Stage 1–8 Gear or a sapling farm, as long as it costs Stage 6 output.

## Behavior

### Water-gas shift

A second Steam Methane Reformer recipe: **500 mB Carbon Monoxide + 500 mB Water → 500 mB Hydrogen + 125 mB Carbon Exhaust**, same catalyst bed, about the FE of a reform. That's +33% Hydrogen per reform when all the CO is shifted.

- The cost is Photobioreactor time, not crops. Algae grows one biomass per 600 ticks on 500 mB CO but needs 1,200 ticks on Carbon Exhaust. Shifting every reform needs about twice the Photobioreactors for the same biomass.
- The exhaust return must stay near the 100 mB a biomass needs. A 1:1 return (500 mB) would hand the loop five biomass worth of carbon for every one it gave up, so the shift would cost nothing.
- The Reformer runs whichever recipe its tanks can feed. A player who wants to shift only part of the CO splits it with connector channels before it returns to the Reformer.

### Urea

A second Ammonia Synthesizer recipe: **250 mB Ammonia + 250 mB Carbon Exhaust → 1 Urea**, same catalyst bed. It is the first use for spare Carbon Exhaust besides the purge button.

- 250 mB Ammonia is worth 54,000 FE in the Fuel Cell. One Urea must save at least that much in build space, never return it as power.
- The Synthesizer's typed tanks (Nitrogen, Hydrogen, Ammonia output) do not fit a Urea recipe as they are. The first slice adds a Carbon Exhaust input tank and an item output slot and lets Ammonia go into its output tank as an input when the Urea recipe runs.
- Urea stays on the Synthesizer or not at all. If it would need a new machine, it is dropped from the package, and the Photobioreactor and Companion keep using Carbon Exhaust and bone meal as they do today.

### Urea fertilizer

Urea is an item that works wherever bone meal does, and more.

| Where | Effect |
|---|---|
| Forestry Companion, Grove Warden | Loads anywhere bone meal does: the cart's sapling slots, the station's bone meal and sapling slots, and connector input. One Urea adds **4** to the 256 bone meal store. Each growth pulse still spends 1 from the store and 30 FE. |
| By hand | Works like bone meal on any plant. |
| Algae Photobioreactor | An optional Urea slot. Each Urea doubles the growth speed of the next 4 biomass. |

On the Companion, Urea is a denser bone meal: one item in the cart lasts four pulses. The Grove Warden's store cap stays 256, so Urea extends time between restocks rather than raising peak growth.

In the Photobioreactor, the target is that one Urea saves about one Photobioreactor-cycle per biomass, never more Hydrogen than its Ammonia cost (see [Design Role](#design-role)).

### Hydrogen annealing

The Metal Press atmosphere tank takes **one gas at a time**:

- **Nitrogen**: the shipped Nitrogen atmosphere, 15% increased Overheat Tolerance with Blast Draft.
- **Hydrogen**: whatever the press presses comes out **Annealed**. It costs **250 mB Hydrogen per item**, spent when the item finishes, and needs no keystone.

An Annealed item carries the `rngtech:annealed` data component, shows "Annealed" in its tooltip, and does not stack with plain copies. Every recipe that takes the plain item also takes the Annealed one. What annealing does depends on what was pressed:

| Annealed item | Payoff |
|---|---|
| Plate | Gear crafted with all-Annealed plates rolls lucky Refinement Potential. |
| Basic Electric Circuit | Its calibration in the Resonance Calibrator rolls lucky stability. |

The choice is what to anneal. Hydrogen spent on plates buys bigger refinement budgets on Gear; Hydrogen spent on circuits buys reliable high-stability Calibrated Logic Components, which gate Advanced and Elite blanks, Ascendancy Seals, and the Soul Transfer's success chance.

Only items with a payoff spend Hydrogen. Molds that make gears, casings, connectors, or Advanced and higher circuits press normally and leave the tank alone, so nothing drains for no effect. JEI and the press's hover say which outputs can be Annealed.

#### Annealed plates: lucky RP

A trait-rolling item crafted with **all** of its plate ingredients Annealed is Annealed itself. It shows "Annealed" while Unidentified, so the player knows before spending an identification. When its traits roll, its **Refinement Potential is lucky**: RP rolls twice in its stage's range and keeps the higher roll. A Stage 5–6 item still rolls 14–22, but averages about 19.5 instead of 18, and lands at 20 or more over half the time instead of a third. A higher budget means more refinement before the item is final, so annealing pays off over the item's life, not at identification.

Annealing does not change rarity odds (Normal at Stage 5+ stays the Affix Forge's job), affix tiers, corruption, or what refinement costs.

At 250 mB per plate, a 4-plate Battery Cell costs 1,000 mB Hydrogen, about 136,000 FE of Ammonia. A player anneals the pieces they care about, not everything.

**Coverage.** Annealing passes only through direct plate ingredients. Frames, circuits, and other intermediates do not carry it into Gear, so staged machine bodies (built from Machine Frames) and plate-free Gear such as Heat Cores cannot be Annealed. Plate-based rolled items include Crush Heads, Battery Cells, Tool Heads and Rods, Cavitation Rotors, Collapse Nozzles, Void Chambers, Containment Linings, Dimensional Stabilizers, Recovery Filters, Fluid Pumps, and Solar Panels.

#### Annealed circuits: lucky stability

A Calibration recipe whose input is an Annealed Basic Electric Circuit rolls its stability twice in the recipe's range and keeps the higher roll, before the Calibrator's Control Board, Stabilizer Matrix, and chassis improve it as usual. The plain recipe's 35–75 range then reaches the Advanced blank's 55 about three times in four instead of about half the time. The resonant recipe benefits the same way.

Annealed and plain circuits never share a stack, so every copy in one multi-lane cycle is either Annealed or not, and the shared roll stays well defined.

Risks to watch in playtest:

- **Soul Transfer.** Its success chance is the recorded stability, so annealing makes transfers safer. That is acceptable because it is paid in Stage 6 output, but it is the main lever to watch.
- **Calibrator keystones.** Annealing overlaps with **Second Pass** (a calibration under 40 is re-run once) and **Master Harmonic** (every 8th calibration at the full streak comes out at 100). Both still stack with it, since annealing works per circuit and the keystones per machine, but each becomes a little less special.
- **Volatile Catalyst.** It needs *low* stability (20–35), but it comes from a Kinetic Sparksteel Gear calibration, not a circuit, so annealing never touches it.
- **Diamond crystals.** The Stage 6 Calibrated Diamond Crystal is not pressed, so the Ultimate blank's 85 gate stays as hard as it is today.

### Hydrogen in the Fuel Cell

The Ammonia Fuel Cell is renamed **Fuel Cell** in its display name and gains a Hydrogen recipe at about **60 FE per mB**, less than half the Ammonia route. It needs no Nitrogen, Cavitation Generator, or Synthesizer.

- It suits a small Stage 6 build, or a place to dump surplus Hydrogen.
- A full Ammonia build must always beat it per mB of Hydrogen.
- The block keeps the `rngtech:ammonia_fuel_cell` id so placed Fuel Cells survive the rename.

## Implementation Contract

Planned runtime surface:

- Item: `rngtech:urea`, no traits, tagged with bone meal behavior through a dispenser and use-on-block handler.
- Data component: `rngtech:annealed` (unit) on plates, Basic Electric Circuits, and Unidentified Gear.
- Recipes: `gas_reforming` CO shift, `ammonia_synthesis` Urea (or a new recipe type, see below), `ammonia_power_cycle` Hydrogen.
- Metal Press: the shipped `InertAtmosphere` tank accepts Nitrogen or Hydrogen; Hydrogen marks pressed plates and Basic Electric Circuits and is spent only on those outputs.
- Crafting: a hook that marks a trait-rolling result Annealed when every plate ingredient is Annealed, and an RP roll hook that rolls twice and keeps the higher for Annealed items.
- Resonance Calibrator: a stability roll hook that rolls twice and keeps the higher when the input is Annealed.
- Forestry Companion and station: Urea in the bone meal accept lists, worth 4 store units.
- Algae Photobioreactor: an optional Urea slot and a growth multiplier.
- Language: the Fuel Cell display name.

Implementation checks:

- `masteryCheck`: Annealed RP and Annealed calibration stability stay inside their ranges and their means match the best-of-two expectation; affix tiers and rarity odds are unchanged.
- Energy balance simulation: no route from Urea back to Hydrogen returns more than its Ammonia cost; the Fuel Cell's Hydrogen recipe loses to the Ammonia route per mB.
- GameTests: a Hydrogen-fed Metal Press makes Annealed plates and circuits and drains 250 mB each; a gear-mold press leaves the tank alone; a fully Annealed recipe marks its result; a mixed recipe does not; an Annealed circuit calibrates with the best of two rolls; Urea loads into the Companion store as 4.

## Modifier Eligibility

Available modifiers: none. Urea and Annealed plates and circuits do not roll traits. Annealing changes only how items that already roll pick their Refinement Potential, and how a calibration picks its stability; [Affix Generation](../systems/affix-generation.md) tiers and weights are unchanged.

## Base Stats

Status: Planned

| Value | Planned | Notes |
|---|---:|---|
| Shift recipe | 500 mB CO + 500 mB Water → 500 mB H₂ + 125 mB Carbon Exhaust | Exhaust near one biomass's need. |
| Urea recipe | 250 mB Ammonia + 250 mB Carbon Exhaust → 1 Urea | 54,000 FE of Ammonia per Urea. |
| Urea in the Companion store | 4 bone meal | Store cap stays 256. |
| Urea in the Photobioreactor | 2× growth for 4 biomass | Must not return more H₂ than it cost. |
| Annealing cost | 250 mB H₂ per pressed plate or Basic circuit | About 34,000 FE of Ammonia each. Other outputs spend nothing. |
| Annealed plate bonus | Gear RP rolled twice, higher kept | Same stage range; tiers and rarity unchanged. |
| Annealed circuit bonus | Calibration stability rolled twice, higher kept | 55+ from the plain recipe goes from about 51% to 76%. |
| Fuel Cell on Hydrogen | About 60 FE per mB | Ammonia route is about 136 FE per mB. |

## Open Questions

- **Urea tank layout.** Whether the Ammonia Synthesizer can take a Carbon Exhaust tank and an item output cleanly. If not, Urea is dropped rather than moved to another machine.
- **Partial annealing.** The first slice is all-or-nothing for Gear. A bonus scaled by the share of Annealed plates would reward partial investment but is harder to read in a tooltip.
- **Higher circuits.** Advanced and higher circuits press without annealing because nothing reads it. If they later feed calibration or trait-rolling recipes, they can join the list.
- **Stronger annealing.** If best-of-two RP proves too small to matter, best-of-three, or a natural tier one higher, are the next steps up. Circuit stability may need the opposite: if Soul Transfers or Elite blanks become routine, a smaller bonus such as a stability floor.
- **Intermediates.** Whether Annealed should carry through Machine Frames into staged machine bodies, which would make annealing reach machines as well as Gear.
- **Pack config.** Whether pack makers can turn annealing off or change its bonus, as they can for Unique sources.

## Related Pages

- [Machine Design Notes](../reference/machine-guidelines.md#machine-design-notes)
- [Affix Generation](../systems/affix-generation.md)
- [Tree Farm Automation requirements](../prds/tree-farm-automation.md)
- [Current Implementation Matrix](../reference/current-implementation.md)
