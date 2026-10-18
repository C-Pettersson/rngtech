---
navigation:
  title: "Batching"
  parent: furnace.md
  position: 2
  icon: rngtech:furnace
---

# Batching

With a Batch Size above 1, each lane smelts several items from its input stack in one cycle. A batch takes as many items as the stack and the lane's free output space allow, up to the Batch Size, and keeps that count until the cycle ends. Each extra item makes the cycle longer by the Batch Overhead, 25% of one item's time to start with. Every item still absorbs its own heat, paid in FE or burn time, and earns its own XP. A bigger batch takes a bigger heat dip from the lane when it goes in. If a batch fails, every item in it becomes a **Malformed Ingot**.
