---
navigation:
  title: "Block Chew"
  parent: miners-companion.md
  position: 2
  icon: rngtech:miners_companion
---

# Block Chew

When Block Chew is on and you break a block, the Miner's Companion checks each drop against its filters. Each matching item costs **16 FE** to remove; the Economy affix lowers that cost. If the cell cannot pay for a whole stack, the items it can pay for are removed and the rest drops normally. With more than one Miner's Companion on you, the next one with a matching filter takes what the first could not pay for.

Block Chew only acts on drops from blocks you break yourself. It never cleans out items already in your inventory, and it ignores mob drops, chest contents, and machine output.

The Process tab has a status square for Block Chew:

| Status | Meaning |
|---|---|
| Ready | Chewing works. |
| Disabled | Block Chew is switched off. |
| Needs Head | No Crush Head is installed. |
| Needs Cell | No Battery Cell is installed. |
| No Filters | None of the active filter slots has a filter in it. |
| No Power | The cell holds less FE than one chew costs. |
