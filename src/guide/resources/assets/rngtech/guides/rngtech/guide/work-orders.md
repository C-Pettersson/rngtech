---
navigation:
  title: "Work Orders"
  parent: guides.md
  icon: rngtech:job_chip
categories:
  - "Guides"
item_ids:
  - rngtech:dispatch_terminal
  - rngtech:job_chip
  - rngtech:completed_job_chip
  - rngtech:requisition
---

# Work Orders

<BlockImage id="rngtech:dispatch_terminal" scale="6" />

- **Type:** Job vendor
- **Power:** None
- **Listings:** 6 jobs
- **Refresh:** <ItemLink id="rngtech:requisition" />
- **Tool:** <ItemLink id="minecraft:wooden_pickaxe" />

**Work Orders** are repeatable jobs for your processing machines. A **Dispatch Terminal** lists six jobs, each on a **Job Chip**. Put a chip in the right kind of machine and run the machine until the job is done. Then redeem the chip for the reward it shows.

The terminal rolls each job when it lists it, so a chip shows everything before you take it:

- The machine it is for
- The job and its tier
- The chassis stage it needs
- Its conditions
- Its reward

Work Orders give a machine goals after its Mastery is maxed. They give Stage 7 and 8 chassis jobs that lower stages cannot take. They also give goals to machines without [Mastery](rngtech:machine-mastery.md), such as the [Component Assembler](rngtech:component-assembler.md).

## Usage

### The Dispatch Terminal

A new terminal fills its six lines the first time you open it. Each line shows the machine, chassis stage, job, and reward. Hover over a line to see the chip's full tooltip.

- **Download** moves the job into your inventory as a Job Chip. It is free, and the line stays empty.
- **Refresh** uses one <ItemLink id="rngtech:requisition" /> from your inventory. It replaces all six lines with new jobs, including jobs you did not download.

One terminal lists jobs of every tier for every machine. The six lamps on its face show how many jobs are left.

- [Tiers](rngtech:work-orders/tiers.md)
- [Jobs](rngtech:work-orders/jobs.md)
- [Drive bays and the Orders tab](rngtech:work-orders/drive-bays-and-the-orders-tab.md)
- [Conditions](rngtech:work-orders/conditions.md)
- [Refining and corrupting chips](rngtech:work-orders/refining-and-corrupting-chips.md)
- [Rush](rngtech:work-orders/rush.md)
- [Rewards](rngtech:work-orders/rewards.md)

## Screen

- **Dispatch Terminal:** Six job lines, each with its machine, chassis stage, job, reward, and a Download button. The Refresh button and your Requisition count sit below them.
- **Orders tab:** Three drive bays, with your inventory below. Each bay's row shows the chip's tier, job, progress per part, best streak, and Rush time. Activate, Pause, Rush, and Abandon act on the selected row.

## Obtaining

- [Crafting](rngtech:work-orders/crafting.md)

### Breaking

Mine a Dispatch Terminal with a pickaxe. It keeps its job list on the dropped item, empty lines included. Placing it again does not refill the empty lines.
