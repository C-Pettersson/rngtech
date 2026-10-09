# Design Proposals

These pages describe proposed systems or requirements beyond the implemented feature set. They are not release promises. Use [Current Implementation](reference/current-implementation.md) and the [player wiki](https://c-pettersson.github.io/rngtech/) to check what the mod does today.

- [Crafting and Upgrades](systems/crafting.md): assembly and refinement context, plus planned breakthrough and strain systems.
- [Affix-Driven Generator](content/affix-driven-generator.md), [Entropy Cell](content/entropy-cell.md), and [Ore Slurry Turbine](content/ore-slurry-turbine.md): unregistered generator concepts.
- [Auto Balance](modifiers/auto-balance.md) and [Packer](modifiers/packer.md): planned modifiers. Implemented [Balance Mode](https://c-pettersson.github.io/rngtech/rarity-and-affixes/#balance-mode) is a separate Battery Chassis affix.
- [Unique Item Ideas](reference/unique-item-ideas.md): candidate items, not registered content.
- [Unique Items requirements](prds/uniques.md): accepted 2.0 design for a shared Unique catalog, ranged rolls, and eight launch Uniques.
- [Corruption requirements](prds/corruption.md): accepted 2.0 design for the Volatile Catalyst and the Corrupted state.
- [Passive Tree Framework requirements](prds/passive-tree-framework.md): historical framework design; see the [shared Machine Mastery implementation](systems/machine-mastery.md).
- [Tree Farm Automation requirements](prds/tree-farm-automation.md): design record; consult [Tree Farm Automation](https://c-pettersson.github.io/rngtech/forestry-cart-station/) for current cart and station behavior.

## Planned Extensions to Shipped Machines

Status: Planned

Future work on machines that already ship. Design intent for their current behavior lives in [Machine Design Notes](reference/machine-guidelines.md#machine-design-notes).

### Battery Chassis

- Burst risk pass: give burst a budget (`ENERGY_TRANSFER x BURST_DURATION`), cooldown, and stability risk. Low-stability chassis such as Gold may overload, waste FE, or briefly lose burst; high-stability chassis such as Steel or Exotic burst more safely at a lower multiplier. Burst stays output-only, never creates energy or capacity, and never exceeds installed-cell output rates.
- A Gear tab for non-cell chassis components, kept player-managed rather than exposed to hoppers or item pipes.
- More global modifier areas: reservation (hold back a slot or a share of stored FE), matching bonuses for cells that share material, rarity, or modifier themes, throttled or charge-first output, redstone control, and side configuration.
- Material directions not built yet: Lead shielding banks with low idle loss, Redstone and Quartz control and precise-balancing chassis, and Titanium or Tungstensteel high-transfer banks.

### Battery Cells

- Decide what cell Efficiency does. It shows in the cell tooltip, but no transfer path reads it.

### Corrosion Cell

- Stronger electrolyte families.
- A reinforced Corrosion Cell body.
- Corrosion sludge as a fluid in a later fluid-handling slice.

### Universal Cable

- Heat controls on the same endpoint model.
- Wireless connector variants that share connector configuration and pair through connector codes instead of cable adjacency.

### Potential Reactor

- Rename or migrate the block to staged Energy Recycler chassis once a save-safe migration plan exists. Until then `rngtech:potential_reactor` stays registered as the Energy Recycler path.
- An enchant-style machine slot for special salvage behavior, such as preserving part materials or extracting affix residue.

### Solid Fuel Burner and Fuel Governor

- Rolled Fuel Governor modifiers: a fuel-control suffix and an automation-focused enchant-style effect. Today Fuel Governor, Quick Feed, Fuel Reserve, and Block Feed are fixed Fuel Box behaviors only.
- Generator identity modifiers for burner chassis.

### Gas Chemistry and Ammonia

- The Coal Gasifier, Steam Methane Reformer, and Ammonia Synthesizer roll Bulk Speed but do not apply it. Wire it in or drop it from their profiles.
- Stability and Fluid Transfer rolls on the Ammonia Catalyst Bed and Fuel Cell Membrane have no effect, and the Synthesizer ignores Efficiency and Instant Process. Wire them in or stop rolling them.

### Furnace and Machine Chassis

- Furnace-specific Gear for the Stage 0 Furnace, which already keeps a saved component slot.
- A Washer machine. The `rngtech:washer_machine_chassis` ingredient is craftable, but no Washer block or recipe type exists.
- Enchant-style chassis behavior.

### Fluid and Compressor Tanks

- Portable tank contents. Breaking a tank drops the block, Gear, and Refinement inventory, but not stored loose or compressed fluid.

### Exotic Affix Forge

- Placed-machine targets. It refines item stacks only.

### Modular Field Tools

- Calibrated tool-part recipes with Resonance Calibrator rod stabilization gates.
- A mode UI for Hammer and Digger area size and target filtering.
- A Tool Bench focus-slot pass for lenses, removals, rerolls, Expansion Crystal, and modifier crystals, which today belong to the Affix Forge.

### Forestry Companion

- Big Saw Gantry, Forestry Drones, and a station refinement profile are covered by the [Tree Farm Automation requirements](prds/tree-farm-automation.md).
- Not covered there: access control for cart owners (owner data is attribution-only), an external item capability for cart cargo, canopy shaping, and support for arbitrary modded tree shapes.

### Miner's Companion

- Default Miner's Companion recovery recipes. The recovery system ships, but only datapacks add recipes.

### Machine Parts

- Part upgrades, upgrade outcomes (fracture, strain, breakthrough), and Refinement Potential carry-over are covered by [Crafting and Upgrades](systems/crafting.md).
