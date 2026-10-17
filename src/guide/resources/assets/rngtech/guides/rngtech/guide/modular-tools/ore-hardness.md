---
navigation:
  title: "Ore hardness"
  parent: modular-tools.md
  position: 1
  icon: rngtech:modular_pick
---

# Ore hardness

RNGTech ores have a **hardness** from 1 to 8, separate from vanilla mining tiers. A Modular Pick or Hammer mines ores up to its head's hardness, and a head reaches every hardness below its own. Ore that is too hard drops nothing. A Hammer skips any block in its area that is too hard for it, without spending durability or FE on it.

| Head material | Stage | Ore hardness | Opens up |
|---|---:|---:|---|
| Flint | 0 | 1 | Iron, Copper |
| Iron | 1 | 1 | |
| Copper | 2 | 2 | Tin, Zinc, Gold |
| Bronze | 3 | 2 | |
| Steel | 4 | 3 | Nickel, Lead, Silver |
| Steel with a <ItemLink id="rngtech:diamond_tip" /> | 4 | 4 | Aluminum, Osmium |
| Aluminum | 5 | 5 | Titanium |
| Titanium | 6 | 6 | Tungsten, Platinum |
| Tungstensteel | 7 | 7 | None by default |
| Nullite | 7 | 7 | None by default |
| Exotic | 8 | 8 | Naquadah |

Other mods' ores of the same metal share its hardness, so another mod's Tin Ore is hardness 2 like RNGTech's.

By default, a vanilla stone pickaxe reaches hardness 1 and an iron or better pickaxe reaches hardness 2. Other mods' pickaxes and drills follow the same rule by their tier. Beyond that you need Modular tools. Modpacks can change these rules and can weaken or disable vanilla tools; see Ore progression.
