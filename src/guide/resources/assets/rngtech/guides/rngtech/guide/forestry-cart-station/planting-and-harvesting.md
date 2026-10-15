---
navigation:
  title: "Planting and harvesting"
  parent: forestry-cart-station.md
  position: 1
  icon: rngtech:forestry_cart
---

# Planting and harvesting

As the cart moves, it scans the row of blocks beside the rail on both sides. It plants saplings in empty spots and remembers each spot as a managed cell. A cart manages up to 96 cells to start with; Mastery can raise the cell cap and the number of rows, from 1 up to 7 on each side. It plants the nearest rows first. Only the first row reports **Invalid Soil** or **Planting Blocked**; outer-row spots over water, paths, or fences are skipped silently. Running out of saplings only stops planting: the cart still drives and harvests.

The cap fills sooner on wider farms. With one row, each rail block has 2 cells, so 96 cells cover 48 rail blocks; with three rows, each rail block has 6 cells and the cap fills after 16. When the cart scans a rail block, it forgets cells there that lie outside its current rows and no longer hold a sapling, crop, or log, so shortening the range or moving the track frees up cells.

To start over, **shift-click Reset** on the cart's Cart tab. The cart forgets every managed cell, returns any Seed Library reserves to its cargo, and reboots, standing still with a blue screen for 3 seconds. Plants stay in the world, and the cart takes back the ones it can tend as it passes them.

When it finds a grown tree beside the track, it stops, clears the leaves first, then cuts the logs from the top down. An Axe cuts one log at a time. A Treefeller cuts a batch of logs at once, which is faster but costs FE for its full batch size; if the battery cannot cover a whole batch, the cart goes to recharge first. Trees planted close together, or with merged canopies, are cut one tree at a time, and leaves touching another tree's logs wait for that tree. Saplings that drop go back into the cart's sapling cargo first, and everything else goes into its output cargo.

A tree can be too big to take. The cart works a tree of up to 512 logs, cutting it down over as many actions as it needs. A larger tree, or a connected mass of more than 2,048 logs, is skipped: the cart shows **Tree Too Large**, marks that cell blocked, and drives on to work other cells. It checks the tree again on later passes.

Without shears, leaves are removed but drop nothing, so the cart cannot replant from its own harvest. The Serrated Leaf Protocol Mastery node lets the cart collect normal leaf drops without shears, but each of those leaves costs 12 times the normal cut FE. With shears installed, leaves drop normally. Vanilla shears lose 1 durability per 8 leaves. <ItemLink id="rngtech:iron_pruning_shears" /> and the other Pruning Shears last longer, and higher-stage shears last longer still. Broken shears are used up.

Cutting takes time. Logs crack like when you mine them, taking hardness × 60 ÷ (tool speed × Processing Speed) ticks, at least 6. An iron-head Axe cuts an ordinary log in about 24 ticks, and Efficiency on the tool speeds it up. After each cut the cart waits 4 ticks. A Treefeller cracks its whole batch at once, in the slowest log's time × (1 + 0.35 × log₂ of the batch size): a 16-log batch takes 2.4 times as long as one log. Planting takes 30 ticks divided by Processing Speed, whatever the tool. A cut in progress starts over if the cart is reloaded.

The cart pays FE from its Battery Cell for moving, scanning, planting, and cutting:

| Action | Base FE |
|---|---:|
| Moving | 1 per tick (4 with Manual Throttle on) |
| Scanning a rail block | 2 per row worked |
| Planting | 40 |
| Cutting a log or leaf | 80 (Treefeller: 80 × Tree Fell Limit per batch) |

Energy Use affixes and Mastery change these costs, and Processing Speed affixes shorten cutting and planting. The cart only charges its own Battery Cell; it never charges the cutting tool's cell. When its cargo is full or its battery is low, it stops working and heads along the track looking for a station. If it runs out of FE, it keeps rolling slowly so it can still reach one.
