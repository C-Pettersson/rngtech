# Shared Machine Mega Passive Tree

Status: Prototype

## Accepted design

- One graph of about 1300 nodes for functional machines, with six starts on a start ring around a central pathing core and only three attributes: Control, Drive, and Reserve. The other starts mix pairs of those attributes.
- First integrations: Crusher (Drive), Furnace (Drive/Reserve), Forestry Companion (Control/Drive). Other processors, generators, storage machines, and companions follow; Gear and cables are excluded.
- Approximately half the nodes grant travel attributes on a connected road network. Distinct layers of travel separate the core, home regions, middle ring, and outer field. Travel steps separate optional reward constellations; notables require at least three allocations into a reward branch. Single-entry reward pockets cannot shortcut between roads. Repeated reward packages support home-region investment plus a distant specialty.
- Keep the layers varied: each home region has its own road silhouette, and hubs and constellations are placed irregularly. Avoid rings of identical clusters, mirrored sectors, and evenly repeated keystone pairs. Keystones are spread through every layer, including the core, and may sit directly on roads when they are at least 10 points from every start.
- One point per non-starter node, 99 points at level 100. Storage allows 120 points for a future non-level source. Connected paths govern access; irrelevant effects remain selectable. Gear validation still applies.
- Ordinary bonuses support chassis and Gear. Keystones offer major tradeoffs or distinct behavior, including family-specific effects.
- Attributes have totals, family-specific inherent conversions, and separately authored explicit scaling. Attribute clusters may increase individual/all attributes. “Attributes grant no inherent bonuses” preserves totals and explicit scaling.
- Absolute values and ceilings resolve after ordinary modifiers. Lower conflicting absolute maximum values win. A hard recipe ceiling is distinct from a fixed processing-level stat and must explicitly forbid recipes above its limit.
- Progression stays with the chassis. Advanced work grants faster catch-up; low-tier work loses XP effectiveness. Reversible loops and idle placement grant no XP.
- Refunds cost one consumable per removed allocation and preserve connectivity and Gear legality. Clear-tree uses the same per-node cost.
- Ordered target builds copy through Mastery and the Configurator, paste only onto cleared trees with matching starts, and automatically allocate earned points. Gear conflicts pause allocation; manual refunds pause the target.
- Migrate existing progression by preserving XP and earned levels while refunding old allocations. Preserve drops/pick-block and multiplayer validation.
- Machine-family ascendancies and additional machine integrations are deferred.

Implementation: [Machine Mastery](../systems/machine-mastery.md).

## Delivery

Implement on `feature/machine-mega-passive-tree`, based on `main`. Do not merge automatically. Maintain [implementation state](machine-mega-passive-tree-state.md), update canonical references, validate graph geometry and runtime behavior, and run repository verification.
