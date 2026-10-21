---
navigation:
  title: "Operations"
  parent: phase-array.md
  position: 4
  icon: rngtech:exotic_affix_forge
---

# Operations

| Operation | Coils | Glyph | Base FE | RP | Partial | Overhold |
|---|---|---|---|---|---|---|
| Attune | 4 | None | 1 MFE | 1 | Reroll one value | Best of two values |
| Exalt | 8 | IV | 4 MFE | 2 | Reroll selected value | None |
| Excise | 5 | None | 1 MFE | 1 | Remove random | Remove strain |
| Graft | 9 | V | 8 MFE | 2 | Donor tier down | Keep donor |
| Imprint | 6 | II | 2 MFE | 2 | Add random of family | Best of two tiers |
| Reforge | 3 | None | 2 MFE | 0 | Reroll affixes | Best of two rarity |
| Resonate | 5 | None | 2 MFE | 1 | Reroll selected value | Two tiers |
| Temper | 7 | III | 4 MFE | 0 | Halve strain | None |
| Transmute | 7 | III | 4 MFE | 1 | Swap random same slot | Keep value quality |

| Operation | On Success | You choose |
|---|---|---|
| **Reforge** | Rerolls rarity, affixes, and RP as if the item were newly crafted. Strain stays. Works at 0 RP. | Nothing |
| **Attune** | Rerolls every affix's value within its tier. | Nothing |
| **Resonate** | Raises the selected affix one tier, up to Tier 6. | One affix |
| **Excise** | Removes the selected affix. | One affix |
| **Imprint** | Adds the affix you choose to the empty slot you choose. The tier rolls with the item's normal weights. | An empty slot, then an affix |
| **Transmute** | Swaps the selected affix for another legal affix of the same slot, keeping its tier. | One affix |
| **Temper** | Sets Strain to 0. | Nothing |
| **Exalt** | Raises the selected Tier 6 affix to Tier 7. Stage 8 Exotic items only. | One Tier 6 affix |
| **Graft** | Moves the selected donor affix onto the empty slot you choose, keeping its tier and value. The donor is destroyed. | A donor affix and an empty slot |

Imprint and Graft need an empty slot, and rarity sets how many an item has: a Magic item holds one prefix and one suffix, a Rare item three of each, and nothing but Reforge changes rarity. Fixed identity affixes and implicits are never selectable. An affix the item could not roll on its own cannot be Imprinted or Grafted onto it, and neither can one from a modifier group the item already carries; the donor list dims such affixes and says why. A pack can retune, remove, or add operations: they are `rngtech:phase_array` recipes.
