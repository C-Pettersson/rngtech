---
navigation:
  title: "Machine and part affixes"
  parent: rarity-and-affixes.md
  position: 6
  icon: rngtech:affix_injector
---

# Machine and part affixes

Where an affix sits changes what it affects:

- **Affixes on the machine** apply to the whole machine.
- **Affixes on an installed part are local.** They improve that part's own contribution first, and then the part's total is added to the machine. For example, a percentage Max Temperature affix on a Heat Core raises that core's heat, not the chassis's.
- **Crush Head yield is the exception.** The head's own Output Amount and its yield affixes add straight to the Crusher's increased Output Amount total.

Hold Left Shift on a part to see which of its affixes are marked **(Local)**. On generators, a part's generation affix scales the machine's base or recipe output once; it does not scale flat FE/t from other parts.

Machine affixes, together with [Machine Mastery](rngtech:machine-mastery.md) passives, are what scale the whole machine.
