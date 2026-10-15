---
navigation:
  title: "Recycling gear"
  parent: potential-reactor.md
  position: 1
  icon: rngtech:potential_reactor
---

# Recycling gear

Burning a rolled item pays FE for its rarity, affix tiers, and Refinement Potential. Plain base stats pay nothing on their own. The payout then scales with what the item was:

- **Stage**: Stage 0 junk pays a quarter, Stage 4 gear pays 1.25×, and Stage 8 gear pays 2.25×.
- **Mastery**: +1% for each Mastery level above 1.
- **Ascendancy**: +200% for each Seal tier used on the machine, so a fully ascended machine is worth 7×.

These scales apply to the item's base value. The reactor's own stats then multiply every payout, salvage fuel included: energy generation, efficiency, and stability all raise the FE you recover, and processing speed makes each item finish sooner.

Repeats lose value. The reactor remembers the item types of its last 15 gear burns, and each earlier burn of the same type cuts the next payout by a quarter, down to 10%. Rotate through 16 or more different items to avoid the penalty. Salvage fuel recipes are not affected.

The output is a single **stripped** copy of the item you burned. A stripped item cannot be placed, installed, refined, or charged, but the [Component Recycler](rngtech:component-recycler.md) still recognizes it. A typical chain is:

```text
Chest → Potential Reactor → Component Recycler → storage
```
