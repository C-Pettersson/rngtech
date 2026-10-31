---
navigation:
  title: "Configurator"
  parent: universal-cable.md
  position: 6
  icon: rngtech:cable
---

# Configurator

The <ItemLink id="rngtech:configurator" /> copies connector settings and pastes them onto other connectors. It works on standalone and cable-side Universal Connectors and on direct Energy Connectors left on a cable face from older worlds.

Right-click a connector to open the Configurator menu:

- **Copy All**, **Copy Energy**, **Copy Fluid**, or **Copy Item** stores those settings on the Configurator. A partial copy replaces only that part of the stored preset.
- Energy settings copy from and paste onto the first Energy row only.
- **Paste** applies the stored channels, modes, Distribution, Attach As, and ghost filters. It never changes installed modules unless module paste is on.

An Attach setting left on its starting side is stored as **Default**, so a preset pastes correctly onto connectors facing other directions.

To edit the stored preset, hold the Configurator and press the **Open Configurator Preset** key. It has no default binding; set it under Controls. The preset screen can:

- Pick a category (All, Energy, Fluid, or Item), then set its channel, cycle its mode, or set its Attach to Default, None, or a fixed side, for every row at once.
- **Flip** every mode in the preset: In and Out swap.
- **Reset** the selected category, or **Reset All**.
- Turn **module paste** on (Mod On). Paste then also installs the matching modules from your inventory and hands back any modules it replaces. If a module is missing or you have no room for the replaced ones, nothing changes at all.
- Turn the **Gear helper** on (Gear On). Right-click a machine to fill its empty Gear slots from your inventory, with the same rules as placing Gear by hand. It does not let pipes or hoppers reach Gear slots.

The Configurator can also copy Machine Mastery builds. See [Machine Mastery](rngtech:machine-mastery.md).
