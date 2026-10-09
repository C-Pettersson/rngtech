# Ammonia Power Chain

Status: Prototype

Player guides: [Ammonia Synthesizer](https://c-pettersson.github.io/rngtech/ammonia-synthesizer/), [Ammonia Fuel Cell](https://c-pettersson.github.io/rngtech/ammonia-fuel-cell/). The upstream gas machines are covered in [Coal Gasifier](https://c-pettersson.github.io/rngtech/coal-gasifier/), [Syngas Combustor](https://c-pettersson.github.io/rngtech/syngas-combustor/), and [Steam Methane Reformer](https://c-pettersson.github.io/rngtech/steam-methane-reformer/).

The ammonia chain has two registered machines: `rngtech:ammonia_synthesizer`, a powered processor that converts Nitrogen and Hydrogen into Ammonia, and `rngtech:ammonia_fuel_cell`, the Stage 6 generator that burns Ammonia for FE.

## Runtime Surface

| Content | Resource id | Block entity | Menu / screen | Capabilities | Refinement |
|---|---|---|---|---|---|
| Ammonia Synthesizer | `rngtech:ammonia_synthesizer` | `AmmoniaSynthesizerBlockEntity` | `AmmoniaSynthesizerMenu` / `AmmoniaSynthesizerScreen` | Non-bottom FE input, fluid input, ammonia fluid output, top Catalyst Bed item handler | Item stack and placed machine |
| Ammonia Fuel Cell | `rngtech:ammonia_fuel_cell` | `AmmoniaFuelCellBlockEntity` | `AmmoniaFuelCellMenu` / `AmmoniaFuelCellScreen` | Ammonia fluid input, top/side FE extraction, bottom residue extraction | Item stack and placed machine |

Ammonia synthesis uses the `rngtech:ammonia_synthesis` recipe type. Ammonia generation uses the `rngtech:ammonia_power_cycle` recipe type. JEI exposes both categories.

The synthesizer owns Nitrogen, Hydrogen, and Ammonia tanks and has no process item slots; its only item handler is the top Catalyst Bed handler. The fuel cell owns one Ammonia input tank and one residue output slot; its Gear slots are not exposed to automation. Purging Synthesizer Nitrogen/Hydrogen or Fuel Cell Ammonia resets that machine's active work state before draining.

The default Nitrogen source is the Cavitation Generator's Stage 6 water separation recipe with `rngtech:nitrogen_extraction_rotor` and `rngtech:nitrogen_separation_nozzle`; the default Hydrogen source is Steam Methane Reforming.

## Gear

| Machine | Slot | Required | Accepted item | Role |
|---|---|---:|---|---|
| Ammonia Synthesizer | Catalyst Bed | Yes | `rngtech:ammonia_catalyst_bed` | Gates synthesis recipes by stage. Its rolled Processing Speed and Energy Usage modifiers apply to the Synthesizer |
| Ammonia Fuel Cell | Fuel Cell Membrane | Yes | `rngtech:fuel_cell_membrane` | Gates ammonia power recipes by stage. Its rolled Energy Generation, Processing Speed, and Efficiency modifiers apply to the Fuel Cell |
| Ammonia Fuel Cell | Battery Cell | No | Stage 6+ Battery Cells | Adds portable FE capacity and output storage |

Both parts have neutral base stats, so an unrolled part only gates recipes. Their rolled modifiers are local to the part, as on other machine parts. The machines do not currently read Stability or Fluid Transfer, and the Synthesizer does not read Efficiency or Instant Process chance, so those rolls have no effect yet.

## Balance

| Recipe | Inputs | Output | Ticks |
|---|---|---|---:|
| Default synthesis | `500 mB` Nitrogen, `1,500 mB` Hydrogen, `12,000 FE` | `1,000 mB` Ammonia | `400` |
| Default power cycle | `1,000 mB` Ammonia | `216,000 FE` (`600 FE/t`) | `360` |

Values are before part and machine rolls. After the synthesis cost the chain nets about `510 FE/t`.

Energy contract: the Synthesizer, Coal Gasifier, and Steam Methane Reformer have no machine-side FE intake cap; intake is limited by free buffer or installed-cell space plus the source or attached Universal Connector. Coal Gasifier and Steam Methane Reformer fill their internal FE buffer first, then charge an installed Battery Cell. The Fuel Cell and Syngas Combustor have no machine-side export cap; they export whatever is stored, and the attached receiver or [Universal Connector tier](basic-wire.md#energy-transfer-limits) decides the rate.

## Related Pages

- [Current Implementation Matrix](../reference/current-implementation.md)
- [Machine Parts](machine-parts.md)
