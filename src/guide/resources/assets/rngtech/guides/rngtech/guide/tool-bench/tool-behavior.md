---
navigation:
  title: "Tool behavior"
  parent: tool-bench.md
  position: 2
  icon: rngtech:tool_bench
---

# Tool behavior

- **Durability and FE** are spent per block actually broken, so a Hammer or Digger that hits fewer blocks costs less.
- **Stability** above 1.0 gives each broken block a chance to cost no durability and no FE. 1.10 Stability is a 10% chance.
- **Control** above 1.0 moves part of each durability point onto the installed Battery Cell as FE. 1.60 Control pays 60% of the wear in FE, up to 100% at 2.0. The full FE price per durability point is 16 FE for a Pick or Axe, 12 FE for a Shovel, 32 FE for a Hammer, and 28 FE for a Digger or Treefeller, scaled down by the share Control covers. If the cell cannot pay, the tool takes full durability wear for that block. Stability is checked first.
- **Self Repair** heads and rods restore 3 to 8 durability, depending on the affix tier, every 7.5 seconds while the tool sits idle in your hotbar or offhand. A head and rod that both have it add together. Mining pauses the repair.
- **Battery Cells** in your inventory recharge the tool's installed cell, first inventory slot first, up to the tool's FE transfer rate. Only cells within the rod's Battery Support count, the same limit that decides which cells you can install.
- **Vein Miner**, a rare Pick Head affix, breaks connected ore of the same type, up to the Vein Mine Limit. Each block after the first costs extra FE; when the cell cannot pay it, the rest of the vein stays. Sneak while mining to break only the block you aim at.
- **Ore Burst** speeds up mining on ore for Picks and Hammers. It needs the Ore Burst suffix on a part and an Ore Burst material: a Copper, Nullite, or Exotic head, or a **Copper Conduit Tool Rod**, **Sparksteel Routed Tool Rod**, **Nullite Phase Tool Rod**, or **Exotic Harmonic Tool Rod**. Each ore it breaks costs extra FE, and without enough FE the ore mines at normal speed.
- **Luck** can add extra drops from ores and logs broken by the tool. Each point, up to 5, gives every drop a 15% chance of one extra copy. It does nothing with Silk Touch, and nothing for machines, chests, or mobs.
