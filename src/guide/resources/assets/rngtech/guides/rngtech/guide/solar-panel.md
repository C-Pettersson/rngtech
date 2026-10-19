---
navigation:
  title: "Solar Panel"
  parent: power-generation.md
  icon: rngtech:crude_solar_panel
categories:
  - "Power generation"
item_ids:
  - rngtech:crude_solar_panel
  - rngtech:copper_solar_panel
  - rngtech:gold_solar_panel
  - rngtech:sparksteel_solar_panel
  - rngtech:solar_array_controller
  - rngtech:sparksteel_solar_array_extender
  - rngtech:aethergold_solar_array_extender
---

# Solar Panel

<BlockImage id="rngtech:crude_solar_panel" scale="6" />

<Row>
  <ItemImage id="rngtech:crude_solar_panel" scale="2" />
  <ItemImage id="rngtech:copper_solar_panel" scale="2" />
  <ItemImage id="rngtech:gold_solar_panel" scale="2" />
  <ItemImage id="rngtech:sparksteel_solar_panel" scale="2" />
  <ItemImage id="rngtech:solar_array_controller" scale="2" />
</Row>

- **Type:** Passive generator
- **Stages:** 1–3, 5 (panels); 4 (controller)
- **Power:** Sunlight, generates FE
- **Gear:** Controller only: <ItemLink id="rngtech:iron_battery_cell" />, <ItemLink id="rngtech:sparksteel_solar_array_extender" />
- **Mastery:** No
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

**Solar Panels** generate FE from daylight with no fuel at all. Place one under open sky and it charges whatever is next to it. Once you have a few, a **Solar Array Controller** gathers a connected field of panels into one output with a shared buffer and its own bonuses. See [Power Chains](rngtech:power-chains.md#solar-panels) for how panels fit beside the main generators.

Each panel is a low tracker on a short post: a deck of cells on a torque tube. While it generates, the deck tilts toward the sun and follows it across the sky, east in the morning and west in the afternoon. When it can't generate it lies flat, so you can tell at a glance which panels are working. Panels are a little over a third of a block tall, so you can walk across a field of them.

Each panel and controller you craft rolls its own [rarity and affixes](rngtech:rarity-and-affixes.md). Solar blocks do not have Machine Mastery.

## Usage

- [Generating power](rngtech:solar-panel/generating-power.md)
- [Stages](rngtech:solar-panel/stages.md)
- [Solar Array Controller](rngtech:solar-panel/solar-array-controller.md)

### Gear

Panels have no Gear and no screen. The controller's Gear tab has two optional slots:

- **Battery Cell**: extra storage and smoother output.
- **Solar Array Extender**: adds +1 range.

| Extender | Built-in bonus |
|---|---|
| <ItemLink id="rngtech:sparksteel_solar_array_extender" /> | +20% Clear Sky Amplification |
| <ItemLink id="rngtech:aethergold_solar_array_extender" /> | +20% Moonlight Conversion, so the array generates at night |

Extenders also roll their own affixes, which apply while installed.

### Automation

| Block | Behavior |
|---|---|
| Solar Panel | Exports FE on every side. The cable port is under its foot. Does not accept FE or items. |
| Solar Array Controller | Exports FE on every side. The cable port is under its base. Does not accept FE. Gear is installed by hand. |

If a very large field slows your game down, turn off animated solar panels in RNGTech's client config. Panels then tilt in steps instead of turning smoothly.

## Obtaining

- [Crafting](rngtech:solar-panel/crafting.md)

### Breaking

Mine panels and the controller with a pickaxe. They drop themselves and keep their rolled traits. A broken controller also drops its installed Gear.

## More

- [Screen](rngtech:solar-panel/screen.md)
