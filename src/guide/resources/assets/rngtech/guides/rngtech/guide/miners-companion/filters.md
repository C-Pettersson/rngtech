---
navigation:
  title: "Filters"
  parent: miners-companion.md
  position: 3
  icon: rngtech:miners_companion
---

# Filters

The Process tab has ten **ghost filter slots** in two rows of five. A ghost slot holds a copy and never takes your item:

- Click a slot while holding a block item to set it as a filter. The item stays on your cursor.
- Click a slot with an empty cursor, or right-click it, to clear it.

Only block items and <ItemLink id="rngtech:advanced_item_filter" /> are accepted.

A Miner's Companion starts with **3 active filter slots**. The others are greyed out and show "Locked by current roll". The **Indexing** prefix ("Indexed", "Cataloged", and so on) adds more, up to all ten. The active count is the Block Filter Slots stat in [Machine Stats](rngtech:machine-stats.md).

A block-item filter matches in two ways: when a drop is that item, or when the block you broke is that block. A Stone filter therefore removes the cobblestone that stone drops. Take care with ore blocks: a Diamond Ore filter removes the diamonds that the ore drops.

An Advanced Item Filter can be set to **allow** or **deny**, and match by sample items, item tags, mod, component stage, Stability, identified state, or rarity; see [Advanced Item Filter](rngtech:universal-cable.md#advanced-item-filter) for how to configure one. It is checked against both the drop and the broken block. Deny filters are checked first, so a drop matching any deny filter is kept. Then the drop is chewed if it matches any allow filter, or plain block-item filter. If every active filter is a deny filter, everything that is not denied gets chewed.
