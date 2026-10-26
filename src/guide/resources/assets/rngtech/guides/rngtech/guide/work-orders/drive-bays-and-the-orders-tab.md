---
navigation:
  title: "Drive bays and the Orders tab"
  parent: work-orders.md
  position: 2
  icon: rngtech:job_chip
---

# Drive bays and the Orders tab

Each machine in the table has an **Orders** tab with three **drive bays**. To load a chip, put it in a bay. While the tab is open, you can also Shift-click a chip in your inventory to put it in the next empty bay.

A bay refuses:

- A chip for a different kind of machine
- A chip that needs a higher chassis stage
- A chip that needs a higher Mastery level
- A blank chip
- A Rush chip with no time left

A machine works on one chip at a time, the **active** chip. A loaded chip waits until you click **Activate** or **Rush**. A small blue lamp on the machine lights while it has an active chip. Click a chip's row to select it, then use these buttons:

- **Activate:** Makes the selected chip active. The previously active chip pauses and keeps its progress.
- **Pause:** Stops the active chip. It keeps its progress.
- **Rush:** Makes the selected chip active with a time limit. Only a chip with no progress can be Rushed. See [Rush](rngtech:work-orders/rush.md).
- **Abandon:** Shift-click to destroy the chip. You get nothing back.

A chip you take out of its bay keeps its progress, which its tooltip shows. When you load it again, here or in another machine of the same kind, it carries on from there. Taking out the active chip leaves the machine with no active chip.

When the active job is done, its chip turns into a <ItemLink id="rngtech:completed_job_chip" /> in the same bay. It is no longer active, and no other chip becomes active on its own. Take it out and use it to get its reward.

A broken machine keeps its chips and loads them again when you place it. The Forestry Companion keeps its chips on the cart item.

With Jade installed, look at a machine to see its active job, its progress, and any Rush time left. Hold Shift to see how many chips it holds and how many are done.
