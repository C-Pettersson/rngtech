# Work Orders Implementation State

Source: [draft design](work-orders.md).

Status: **Planned**. The PRD is a draft with open questions and no release target. On 2026-10-25, after a first playtest, it was redesigned around a Dispatch Board vendor, readable order items, and an Orders tab on every processing machine. After a second playtest the same day, orders became Job Chips from a Dispatch Terminal, loaded through drive bays. The branch `feature/workplan-machine-065464` has phases 2 to 4 of the rework done: chips are readable item data, are stored on the machine, are loaded through three drive bays on an Orders tab on the Crusher and Furnace, and come from a Dispatch Terminal. See [Code against the redesign](#code-against-the-redesign).

## Scope checklist

- [x] Draft the PRD and this state page.
- [x] Redesign after the first playtest (2026-10-25).
- [ ] Resolve the PRD's open questions and accept it.
- [x] Create a feature branch from clean `main`.
- [x] Order data and storage: readable order item data, the `rngtech:work_orders` machine component with three slots and an active index, drop carry, and round-trip checks.
- [x] Core loop for the Crusher and Furnace: drive bays to load and unload, activate, switch, and abandon on an Orders tab; progress hooks; the Completed Job Chip; the reward table.
- [x] Dispatch Terminal and Requisitions: the block, job rolling with count ranges and item jobs, one-kind rewards, Download and Refresh, the Requisition recipe, and the give commands.
- [x] Order templates: loader, terminal pools, per-template reward tables, the template give command, and the config key that turns random orders off. Rarity and conditions on templates wait for phase 6.
- [x] Conditions: the `work_order` profiles and pool for the Crusher and Furnace, conditions as an effect source on the machine, reset-on-event conditions, and Rush. The Bonus Summary drawer section moved to phase 9.
- [ ] More jobs and machines: stat goals for the Crusher and Furnace (done), the Component Assembler (done), the Alloy Furnace (done), the Metal Press (done), the Melter (done), the Resonance Calibrator (done), the Forestry Companion (done), then the other Mastery families and the machines without Mastery, starting with the Component Assembler.
- [ ] Refinement and corruption: order corruption pools and Phase Array rejection.
- [ ] UI polish: drive-bay panels on machine models, the Bonus Summary section, Jade, JEI, and model cues.
- [ ] Documentation: wiki page, Machine Mastery and Rarity updates, the implementation matrix, the GuideME guide, Machine Design Notes, and an FTB Quests chapter.
- [ ] In-game playtest of every eligible machine and tier.

## Decisions

Recorded 2026-10-18 while drafting. The first three were replaced on 2026-10-25:

- An order is accepted from the machine's Mastery tab and consumed from the player's inventory, the same way a Seal is used from the Ascendancy panel. No new Gear slot is added.
- Orders are crafted or dropped only. There is no Dispatch Desk for now.
- An active order is part of the Mastery state, so it travels through Soul Transfer.
- Rush is an opt-in deadline added at acceptance. It never rolls on its own.

Recorded 2026-10-25 after the first playtest, from the user's redesign:

- A Dispatch Board block is the quest vendor. Players take the orders they want and reroll the offer with a crafted Requisition.
- Orders are random, but rolled when offered, so the item shows its family and job before the player takes it. Pack makers add specific orders by datapack, and a config key turns the random orders off.
- Work Orders get their own Orders tab on the machine. The W button under Mastery was too hidden.
- Work Orders work on every processing machine, including machines without Mastery such as the Component Assembler.
- Orders need no Mastery level by default. Templates and conditions can add one.
- A machine holds several loaded orders with one active, and switching keeps progress. The PRD drafts three slots.
- Loaded orders are stored on the machine, not in Mastery, and do not move through Soul Transfer.
- Pack makers can define custom orders as datapack templates.

Recorded 2026-10-24 for phase 2 of the earlier design:

- Phase 2 started before the PRD was accepted. None of the open questions change the storage shape; the tier table is kept as draft numbers behind `ActiveWorkOrder.requiredStage` and `requiredLevel`.
- The active order stores the order's `MachineTraits` instead of separate rarity and condition fields. That keeps rarity, conditions, and corruption together, and unknown condition names survive a save the same way they do on Gear.
- The objective is stored resolved: its catalog id plus the `target` count. A datapack that changes or removes the entry later does not change a running order.

Recorded 2026-10-25 for phase 3 of the earlier design:

- Orders are Normal with no conditions until phase 4, so every accepted order starts at 100% Bounty and Prestige 0. Crafted and plain stacks read as seed 0 until phase 6 adds first-hold rolling; `/rngtech work_order give` sets a random seed.
- The first objectives are `crusher_hard_ore` (`crush`, counted by the recipe's Processing Level) and `furnace_hot_smelts` (`hot_smelt`, counted by the overheat slider). Their counts and minimums are draft numbers, not yet tuned against the processing balance simulation.
- The reward table needs two things the PRD did not name. The loot condition `rngtech:order_roll` makes the next-order pool, the base Glyph, and the Unique roll once per order. The loot function `rngtech:set_order_tier`, with an optional `offset`, sets a Glyph's or a Work Order's tier from the order's tier.
- The Uniques pool rolls the whole catalog through the existing `rngtech:unique_loot_enabled` gates. Weighting it toward the order's family stays an open question.
- Accept takes the inventory slot of the order as its action value. The phase 7 panel will send it; until then only GameTests and code call it.
- `check-volatile-catalyst.mjs` and `check-uniques.mjs` allow the Work Order reward table as their one exception. The new `check-work-orders.mjs` checks that table, the recipe condition, and recycling instead.

Recorded 2026-10-25 for the minimal panel:

- The panel opens from a **W** button on the Mastery toolbar instead of a clipboard beside the Ascendancy crest. The search box moves right to make room. The full phase 7 panel can still move it beside the crest.
- With no active order, the panel lists every Work Order in the player's inventory with its tier's level and stage needs, and an Accept button when both are met. It does not preview the job yet: the objective catalog lives on the server, so a preview needs the catalog synced to the client first.
- With an active order, it shows the job, progress, Bounty, and a paused note when the chassis is below the tier's stage. Abandon needs a Shift-click, as Clear does.
- The active order syncs as five menu data fields after the granted stats: tier, work type, minimum, target, and progress.

Recorded 2026-10-25 after playtesting the Orders tab, from the user:

- Every Tier I order of a family was identical and stacked, because each family has one objective with a fixed count per tier. That is the current build, not the design.
- Jobs come in three kinds: work objectives (the shipped kind), item objectives that name one item or recipe, such as smelting Naquadah Dust into ingots, and stat goals, such as a Furnace reaching a Max Temperature or a Crusher reaching a Bonus Output Chance. Every tier's count or target rolls within a range, rounded to a step.
- A stat goal is met at the first completed job with the stat at or above its target, so the build has to run. It asks for a stat and never grants one.
- Rarity sets only how many conditions an order rolls; it does not decide the kind of job.
- Each count or target range splits into three bands, low, middle, and high, with their own Bounty bonus (draft +0%, +10%, +20%). Within a band, every value pays the same.

Recorded 2026-10-25 for phase 4, from the user:

- One Dispatch Board block offers mixed tiers, each equally likely for now. Requisitions have no tier.
- A taken order's slot stays empty until a Requisition rerolls the board.
- The board's recipe uses the Stage 4 calibration gate, like Requisitions.

Recorded 2026-10-25 after playtesting the Dispatch Board, from the user:

- A Bounty percentage meant nothing to the player. Orders now list the exact items they pay, rolled from the reward table when the board rolls the order; opening a Fulfilled Work Order pays that list. An order without a list still rolls the table when opened.
- The first board was a placeholder cube. It is now a freestanding notice board: a slate-cased board on steel legs with a teal band, a cork face, and a circuit display strip on the crown, generated by `tools/blockmodels/dispatch_board.py`. A `notes` blockstate pins one paper note per offer left.
- The screen is a bounty board: six pinned notes on cork in a slate frame, each with the order's item, machine, tier stamp, job, reward icons, and a Take button. The player inventory is gone; Take moves the order into it.

Recorded 2026-10-25 for phase 4, from the build:

- The board is a six-slot, take-only inventory, so each offer is a real Work Order stack: the client syncs it with no custom data, its tooltip shows the whole order, and taking works like a chest, Shift-click included.
- The offer, empty slots included, rides on the dropped board as `minecraft:container`. A board without that component is new and fills on first open; one with it never refills on its own, so breaking and placing a board is not a free reroll.
- Count ranges ship for both objectives: about 20% either side of the old fixed counts, on steps of 8. The band bonus is stored on the objective as `bonus`, added to the order's Bounty, and shown on the tooltip.
- The give command is now `/rngtech work_order give <players> random [tier]`; without a tier it rolls one as the board does.

Recorded 2026-10-25 for phase 2 of the rework:

- An order names its family by id. A Mastery machine's id is its Mastery family's name, and `WorkOrderFamilies` maps ids to their top stage. Machines without Mastery join that map in phase 7.
- An order stores its stage requirement, capped at the family's top stage when it is rolled, so loading needs no tier table and a template can set any stage.
- Orders no longer pause. A loaded order stays on its chassis, and a chassis's stage cannot change.
- A Work Order stack with no order data is a Blank Work Order and never loads. The creative tab no longer lists orders; `/rngtech work_order give <players> <tier>` rolls a random one.
- The Tier I Work Order recipe and the next-order reward pool are removed. Phase 4 adds the Requisition recipe and the Requisition pool. `workOrders.recipesEnabled` and its recipe condition stay for phase 4.
- All 32 block loot tables that copy `rngtech:machine_progression` also copy `rngtech:work_orders`, so every Mastery machine keeps its orders when it is broken. Pick-block does not copy them.
- Until the phase 3 Orders tab, the W panel on the Mastery tab still loads and abandons orders. Loading fills the next slot, and the first order becomes active; Abandon removes the active order.

Recorded 2026-10-25 for phase 3 of the rework:

- The Orders tab follows the inventory screen layout redesign (layout #15: named tabs on the left that collapse to an icon rail, through a shared `MachineTabs` widget on `feature/inventory-screen-layout-0763af`). The tab's content is the self-contained `WorkOrdersTab`, which draws only inside the 240 px panel and never in the tab area. Until `MachineTabs` reaches this branch, the Crusher and Furnace add Orders as a sixth top tab, which overhangs the panel; after it lands, that entry becomes one `orders(TAB_ORDERS)` builder call with the Work Order icon, after Mastery.
- The tab shows the three slots as cards with tier, job, progress, and Activate or Pause, Unload, and Abandon, and lists this machine's orders in the player's inventory with their requirements and Load. Abandon needs a Shift-click. Unloading the active order is refused; pause it first.
- An unloaded order keeps its progress, best streak, and Rush deadline in a `rngtech:work_order_progress` component, shown on its tooltip, and resumes when it is loaded again.
- The tab's actions travel in their own `WorkOrderActionPayload` to `WorkOrderOperations`, through a `WorkOrderMenuView` menu interface, so machines without Mastery can use them in phase 7. The W button and panel on the Mastery tab are gone, and the Mastery screen and operations match `main` again.
- The loaded orders sync as 22 menu data fields after the Mastery fields: the active slot, then tier, work type, minimum, target, progress, Bounty, and Prestige for each slot. Machines without Mastery will need their own block of these fields.

Recorded 2026-10-25 after playtesting the Dispatch Board, from the user:

- Orders are Job Chips, which suit a tech mod better than paper. A Dispatch Terminal replaces the Dispatch Board, and a finished chip becomes a Completed Job Chip.
- Machines take chips in three real drive bays on the Orders tab. Inserting a chip loads it; pulling it out unloads it with its progress, and pulling the active chip leaves none active. Nothing automates the bays.
- Each job pays one reward kind, scaled by tier and Bounty. A Requisition is one possible reward, not a guaranteed one.
- The job mix is balanced: some jobs name one to three items, some ask for any work of a stage, some ask for a large count with no filter, and some filter on a machine setting. These are examples, not hard rules.

Recorded 2026-10-25 for the chips slice, from the build:

- A drive bay is a real slot over the machine's stored chips (`DriveBays`), so taking, placing, swapping, and Shift-click work as in any container. The bays are packed in load order: a chip placed in any free bay goes after the loaded ones, and taking one out moves the rest up. Shift-click from the inventory fills a free bay only while the Orders tab is open.
- `DriveBaySlot` holds one chip and reports that limit directly. `SlotItemHandler`'s default probes the limit by emptying and refilling the slot, which on the bays would unload and reorder chips.
- The Load and Unload buttons and the load list are gone; `WorkOrderOperations` keeps only activate, pause, and abandon. A bay refuses a chip for another family, a higher stage, a Mastery level the machine lacks, or a blank chip, with no message; the chip's tooltip shows its family and stage.
- The terminal lists six jobs, each a real chip in a take-only slot, with a Download button; Refresh spends one Requisition. Six lamps on the model show how many jobs are still listed.
- The Calibrated Logic Component calibrates only to Stage 2, so a Stage 4 component requirement could never be met. The Stage 4 gate is a stability floor instead: the terminal needs 80+ stability, which only the Stage 4 Calibrator's resonant recipe reaches (65 to 95; the basic recipe tops out at 75), and the Requisition needs 65+.
- Item jobs pick one to three distinct items, each counted toward its own part, from options whose stage is within the tier's cap and whose recipe is loaded. Their names read in the job line, such as "Crush 96 Raw Tin, 64 Raw Lead and 32 Raw Zinc".
- The menu sync grew to 67 fields: the active bay, then for each bay its tier, Bounty, Prestige, and part count, and for each of up to three parts its type, filter, minimum, item, target, and progress.

Recorded 2026-10-25 while playtesting the Dispatch Terminal and drive bays, from the user:

- A stage job's line ("Processing Level 4+", "Stage 4+") did not say which items count. Chip tooltips and the Orders tab now list them while Shift or Alt is held, read from the loaded recipes the same way the machine reports its work, and the terminal heading reads "Stage 4+ chassis" so the chassis requirement is not mistaken for the job. A Furnace stage job lists materials instead of items: a smelt's stage is its material's, and items made the list too long (up to about 50 at Stage 3+, where materials are a handful). Heat and any-work jobs have no list, because every recipe of the machine counts and the list would run to about 130 items.
- A finished chip stays in its bay as a Completed Job Chip for the player to take out, instead of dropping on top of the machine. It is stored as a loaded job with every part at its target, so it rides on the dropped machine like any chip. It is never active, cannot be activated, and keeps its bay until it is taken out.

Recorded 2026-10-25 for phase 5, from the build:

- A template's `weight` shares one scale with objective weights, so a listing is one weighted draw over the random objectives and the pooled templates that fit its tier. With `workOrders.randomOrdersEnabled` off, the objectives leave the draw but stay loaded, because templates name them.
- A listing of a random tier picks evenly among the tiers that have something to draw. With random jobs off and templates of only some tiers, every listing is one of those tiers instead of an empty slot.
- Every key but `objective` is optional, and unset fields roll the way a random job's do. Inline parts take one count each, fixed or ranged, and the highest band among ranged parts sets the Bounty bonus.
- Fixed rewards are listed as written, without Bounty scaling. A template's `reward_table` replaces `rngtech:challenges/work_order` for its job, and both the chip and the Completed Job Chip keep the table id, so a chip without a list still redeems from the right table. A table that is not loaded pays nothing; the loader cannot check tables, which load beside templates.
- The give command is `give <players> template <id> [tier]`, not `give <players> <id>`: a bare id would collide with the `random` literal. A template's own tier wins over the command's.
- `name` is a translation key or plain text (`translatableWithFallback`). The chip reads "Name (Job Chip III)", and the terminal row heading shows the name in place of the machine.
- The loader rejects a template with no objective, an unknown family or job type, a tier outside I to V, an item part with no item, negative numbers, an empty reward, or a reward item that is not registered. A template naming an objective that is not loaded parses but never rolls.
- No template ships in the mod's own data. `/rngtech work_order give` suggests the loaded templates.
- Work Orders have no wiki page yet (phase 10), so the template format and `workOrders.randomOrdersEnabled` are documented here and in the PRD until then.

Recorded 2026-10-25 for phase 6, from the user:

- Ship the conditions the Crusher and Furnace can read with existing stats and events. Thin Margins, Veteran Crew, Unsteady Hands, and of Fine Tolerance wait.
- Listed jobs roll Normal 50%, Magic 35%, Rare 15% (draft). This settles the open question: Normal chips exist.
- The Bonus Summary drawer's Work Order section waits for the phase 9 UI polish; this phase shows conditions on the chip tooltip and the Orders tab row.

Recorded 2026-10-25 for phase 6, from the build:

- Conditions are real affixes: `ModifierDefinition`s in `WorkOrderConditions`, stored as the chip's `MachineTraits` and carried into the bay. `WorkOrderConditions.profile(family)` builds a `work_order_<family>` eligibility profile from them for phase 8 refinement. They are not in `ModifierEligibilityProfiles`, whose capability validation is for machine stats.
- Counts: Normal 0, Magic 1–2, Rare 3–4, within the usual slot limits (Magic one prefix and one suffix, Rare three of each), each from a different group. A family with fewer conditions to offer rolls fewer. Tiers roll evenly among a condition's tiers.
- Each condition adds its tier's Bounty, and the rule conditions add one Prestige, when the job rolls; the reward then rolls at that Bounty. Rush works on top: it multiplies the listed reward, or the Bounty of a job without a list.
- Draft values: Starved Feed 15–30% less Processing Speed (+15/22/30% Bounty), Power Surcharge 25–50% increased Energy Usage (+10/17/25%), Short Crew −1 Batch Size (+30%), Cold Start 10–20% reduced Temperature Stability (+15/20/25%), Hand Feeding no Instant Process (+20%), Brittle Lining and Jammed Line reset progress on a heat failure or jam (+35% and +30%, +1 Prestige), Deep Ore one level above a stage job's minimum (+25%, +1 Prestige).
- Pressure conditions apply in the Crusher's and Furnace's `buildStats` right after the Mastery attribute stats, as implicit modifiers labelled "Work Order: <condition>" in a stat breakdown. The stats cache gets a third key object, the active chip's traits, so activating, pausing, or finishing a chip rebuilds the stats once.
- Hand Feeding is a 0× less Instant Process Chance. Short Crew is −1 Batch Size, which the machines already floor at 1.
- Rule conditions are zero-value affixes read by id, like the existing behavior-only affixes. Deep Ore applies in counting (`WorkOrderConditions.counts`), not by changing the stored job, so removing it by refinement later restores the job; the Shift list shows the raised minimum.
- Reset conditions hook the Crusher's jam and the Furnace's heat failure through `WorkOrderHost.workOrderSetback`, which resets only the active chip that carries that condition.
- Templates gained `rarity`; conditions on a template always roll.
- After `MachineTabs` reached `main` (#209), the branch merged `main`, and Orders is now a `MachineTabs` entry after Mastery with the Job Chip as its icon. The temporary sixth top tab is gone.
- Shift-click from the inventory into the drive bays did nothing in playtest: tab selection was client-only, so the server's menu never saw the Orders tab. The Crusher and Furnace screens now send the selected tab as a menu button (`WorkOrderMenuSupport.BUTTON_SELECT_TAB` plus the tab), and the GameTests open the Orders tab the same way.
- Rush: the Orders tab's Rush button activates the selected chip with a deadline of half the tier's target time (7:30 at Tier I up to 15:00; the first build used 1.5 times, which playtest found far too generous, since a Tier IV job of 440 items was nearly done in six minutes) and 50% more listed reward, rounded up, or 50% more Bounty for a job without a list. Its tooltip shows the deadline and the loss before the player commits. Machines drop expired chips about once a second, a chip in a player's inventory disappears when its deadline passes, and a bay refuses an expired chip. A finished chip never expires. The rows show "Rush m:ss" and the chip tooltip the time left; the menu sync grew a Rush field per slot.

Recorded 2026-10-25 for phase 7, from the user and the build:

- `MachineTabs` reached `main`, so the branch merged `main` before phase 7 and Orders moved onto it.
- The processing balance simulation fails on `main`: its stored Mastery tree allocations in `tools/balance/processing-setups.json` no longer validate against the passive tree ("Invalid CRUSHER tree crusher_bonus_output at level 10"). This branch did not cause it. The user chose to draft stat-goal targets from the simulation at Mastery level 1 (no tree), which still runs, and to fix the allocations separately.
- Stat goals shipped for the Crusher (Processing Speed, Super Output Chance, Output Amount) and the Furnace (Heat Transfer, Temperature Stability, Processing Speed, Super Output Chance), each at weight 1. Each tier's target range runs 40% to 65% of the way from the unrolled build to the best archetype build of the tier's stage at level 1, rounded to a clean step. Max Temperature is allowed as a Furnace goal stat but ships no objective: Gear barely moves it at level 1.
- `WorkOrderFamilies` lists the stats each family's goals may name; the objective and template loaders reject any other.
- A stat goal's progress is the machine's value at each completed job, any job, in hundredths and capped at the target. It falls if the stat falls, and the goal is met at the first completed job at or above the target. The value includes the chip's own conditions, so Starved Feed makes a speed goal harder.
- The menu sync carries a goal's stat as its ordinal in the part's item field.

Recorded 2026-10-25 for the Component Assembler, from the user and the build:

- The Component Assembler (`rngtech:battery_assembler`, `BatteryAssembler*` in code) is the first machine without Mastery. Its family id is `component_assembler`.
- It gets three job kinds (user's pick): Battery Cells of Stage N+ (the stage is the cell's material stage; other outputs report Stage 0), one or two named outputs from its 18 staged recipe outputs, and broad counts of any assembly. Counts are small, since assembly is slow and costly: broad 24–84, cells 6–24, named 4–12 across the tiers. No stat goals yet.
- It has no chassis stage, so its chips need none (user's pick). `WorkOrderFamilies` marks it stageless: the required stage is 0, and the tier's own stage still caps which outputs a job can name. Chips and terminal rows with no requirement show no stage.
- Its menu has no Mastery fields, so it syncs chips through its own data block, `WorkOrderMenuSupport.data(host)`. Its Orders tab, drive bays, Shift-click, and tab sync follow the Crusher's.
- Starved Feed and Power Surcharge apply to it; Short Crew is now Crusher and Furnace only, since the Assembler has no batches. Deep Ore applies to its cell-stage jobs.
- Its drop keeps its chips (`rngtech:work_orders` added to its loot table), and Rush expires on it like on the Crusher.

Recorded 2026-10-26 for the Alloy Furnace, from the build:

- The Alloy Furnace reports each finished recipe set as `alloy` work: the recipe's minimum component stage as its stage (3, 4, or 6), the heat slider as its setting, the output item, and the base output count times the batch. Super Output and ledger bonuses do not count.
- Four objectives, sized at about half the Furnace's counts since alloying eats several ingots per set: any (192–576), recipes of Stage N+ (minimums 3, 4, 4, 6, 6; its top stage is 6), heat slider (50–80%), and one or two named alloys from its eight ingot outputs. No stat goals yet: the processing balance simulation does not model the Alloy Furnace.
- Short Crew, Cold Start, and Brittle Lining now read on the Alloy Furnace too; its heat failure resets a Brittle Lining job.
- It syncs chips through its Mastery data block, like the Crusher and Furnace.

Recorded 2026-10-26 for the Metal Press, from the build:

- Streak jobs shipped as a `streak` filter: a part counts any work of its verb, and `WorkOrderHost.workOrderStreakBroken` sets an unfinished streak back to 0 on a failure. The chip keeps its best streak, which its tooltip and so the Orders tab row show. `WorkOrderFamilies.streaks` lists the families whose machines report failures; only the Metal Press for now, and the loaders reject a streak elsewhere.
- The Metal Press reports each finished press as `press` work: its jobs in the batch, the failure material's stage as its stage, the heat slider, and the output. A heat failure breaks streaks and resets a Brittle Lining job.
- Five objectives: any (160–432 presses), Stage N+ metal (minimums 2, 3, 3, 4, 4; its top stage is 4, so every tier needs a Stage 4 press), heat slider (50–80%), one or two named plates, gears, or casings of Stage 4 or lower metals (64–192), and streaks without a failure (64–296, the PRD's 200 at Tier V).
- Short Crew, Cold Start, and Brittle Lining read on it. The crude press is Stage 3 and cannot fail, so it loads no tiered chip and never runs a streak.
- The Shift list for a press stage job names metals, like the Furnace's.

Recorded 2026-10-26 for the Melter, from the build:

- The Melter has six recipes, all at Processing Level 6, and one body at Stage 6, so the PRD's "Stage 6+ metal" example does not fit: it makes fluids (Lava, Lubricant, Electrolyte Solution, Ender Slurry, Methane), not metal. Its jobs count fluid in mB.
- It reports each finished cycle as `melt` work: the mB its parallel melts filled, the recipe's Processing Level as its stage, the heat slider, and the output fluid's id.
- Three objectives: any fluid (24,000–84,000 mB), heat slider (two thirds of that, 50–80%), and one or two named fluids (16,000–48,000 mB). No stage jobs, since every recipe is Stage 6, and no streaks or stat goals.
- A job part can name a fluid: `JobChipItem.itemName` falls back to the fluid registry, item jobs read "Make 20000 mB of Lubricant", and an option is available when a loaded Melter recipe makes that fluid.
- Short Crew reads on it (its parallel melts); Cold Start and Brittle Lining do not, since it reports no heat failures.

Recorded 2026-10-26 for the Resonance Calibrator, from the build:

- It reports each calibration as `calibrate` work: the recipe's minimum stage, the rolled stability as its setting, the output, and the components in the batch.
- Five objectives: any (48–168 components), Stage N+ recipes (minimums 2, 2, 4, 4, 5; its top stage is 7), stability (60–80+), one or two named calibrated components, and streaks at a stability (6–18 in a row at 60–80+, after the PRD's "12 in a row at 80+").
- Streaks gained a minimum on the work's setting. Work of the streak's verb below it breaks the streak, so a low-stability calibration breaks a Calibrator streak; Metal Press streaks keep a minimum of 0 and break only on failures.
- The two Calibrator conditions the phase 6 decision deferred now ship, since the Calibrator reads them: Unsteady Hands (10–20% reduced Stability, also on the Metal Press, whose family reads Stability) and of Fine Tolerance (a calibration below a stability job's minimum resets it). A condition can now require a filter on the job: Deep Ore needs a stage job, Fine Tolerance a stability job. Short Crew reads its batches.

Recorded 2026-10-26 after playtesting the new machines, from the user:

- The Metal Press and the Resonance Calibrator should take jobs by kind of output, not only by exact item: "Press plates" of any metal, and "Calibrate Logic" of any recipe in that family. Item jobs named one output, which missed most of both machines' range, including the Calibrated Diamond Crystal and the Cavitation Rotor Hub.
- Group jobs shipped as a `group` filter: a report carries a group, the Metal Press its mold (plate, gear, casing, connector) and the Calibrator its calibration family, and a group part counts its group across every recipe. `WorkOrderFamilies` lists each family's groups, the loaders reject others, and the menu syncs a group by its index. Two objectives: Metal Press kinds (one or two, 80–216 presses) and Calibrator families (one or two, 20–54 components), both at weight 3.

Recorded 2026-10-26 for the Forestry Companion, from the build:

- The Forestry Companion is a cart entity, not a block, so its chips live in its own field, saved with the cart under `WorkOrders` and copied onto the cart item (`rngtech:work_orders`) when it is picked up and back when it is placed. The cart ticks Rush, and its stats add the active chip's conditions on top of the item's.
- It reports `harvest` work: each felling pass reports its broken logs with the cutting head's stage and the group `logs`, and each crop harvest reports one crop with the group `crops` and stage 0.
- Three objectives: logs (320–960), crops (96–336), and logs with a Stage N+ head (minimums 1, 2, 3, 4, 4). The Forestry top stage is 4, as is the cart's, so every chip loads on it. No named-item jobs.
- Its Orders tab shows the player inventory, which its menu otherwise shows only on the Cart tab.
- Power Surcharge now lists its families and leaves out the Forestry Companion, as the PRD's condition table says. Starved Feed reads on the cart.
- The GameTest caught a save bug while building this: the chips' save sat inside the Mastery block, so a cart without Mastery progress dropped its chips on save.

## Code against the redesign

The branch's loop was built for the earlier design. Phase 2 of the rework replaced the Mastery storage, the Mastery level gate, the tier and seed components, and the Tier I recipe; the lists below show what is left for later phases.

Keeps, with small changes:

- `WorkObjective`, `WorkObjectiveType`, `WorkObjectiveEntry`, the objective catalog under `work_orders/objectives`, and the two objective files. The board rolls from the same catalog.
- `WorkOrderRules.progress` and `rewardRolls`, which count work against one order and roll Bounty.
- The Crusher and Furnace completion hooks. Their target moves from the Mastery host to the machine's loaded orders.
- The Fulfilled Work Order item, the reward table, `order_tier`, `order_prestige`, `order_roll`, `set_order_tier`, and `check-work-orders.mjs`. The next-order pool becomes a Requisition pool.

Still to change:


Done in phase 2: the `work_order` field on `MachineProgressionState` and its Soul Transfer carry are gone, and orders live in a `rngtech:work_orders` component on the machine; the Mastery level gate is now an optional per-order requirement; the tier and seed components became `rngtech:work_order` item data; `ActiveWorkOrder` became `LoadedWorkOrder`; and the Tier I recipe is gone.

## Findings

Recorded 2026-10-18 from the code on `main` at `fad45920`:

- Seals are not used by right-clicking a machine. The Ascendancy panel sends an `ascend` action, and `MasteryOperations.ascendancy` takes the Seal from the player's inventory through `AscendancyPayment`. Creative players need the Seal but do not use it up. Accept can follow the same payment pattern.
- `MachineProgressionState` already has optional fields with defaults (`ascendancy`, `ascendancy_nodes`, `seal_tiers`), so a `work_order` field can follow `optionalFieldOf` with no migration.
- `SoulTransfer.Charge` stores the whole `MachineProgressionState`, so an order stored there travels with no Soul Transfer change. A charge's `releaseStage` already handles stage limits on release; the paused-order rule is separate.
- `MasteryEffectSource` is the shared interface for tree and ascendancy nodes. It already carries effects, tagged effects, behaviors, fixed values, and ceilings, which covers both pressure and rule conditions.
- `ChallengeContext` holds family, machine stage, recipe stage, stability, instability, Mastery level, and ascendancy. Adding tier, Bounty, and Prestige means new fields there and two new loot conditions.
- `MachineMasteryHost.grantMasteryXp` runs for completed work, so it marks most places that need a progress hook. Streak objectives also need the failure events: the Metal Press failure output, the heat-failure challenge roll, the Crusher jam, and the calibration result.
- The parallel-work stat is `BATCH_SIZE`, shown as Batch Size. `parallel_jobs` is a legacy id alias.
- `MachineMasteryFamily.topStage()` gives each family's highest stage, which the tier stage rule needs.

Recorded 2026-10-24 from phase 2:

- Machine drops, the Forestry Companion cart item, and Soul Transfer charges all copy the whole `MachineProgressionState` through its codecs, so the order travels with no change at those sites. Only `SoulTransfer.afterFailure` rebuilt the state field by field and needed the order passed through.
- `MasteryMenuSupport.snapshot` rebuilds a client-side view from `ContainerData` and does not carry the order. The phase 7 panel needs its own sync for it.
- Machines call `forFamily(masteryFamily())` on every read, which returns the same state for the same family, so the order is only dropped on a real change of family.
- Dormant Machine souls are built with the constructor that has no order, so they never carry one.

Recorded 2026-10-25 from phase 3:

- `grantRecipeXp` returns early when a recipe pays no XP at the machine's level, which is common at the levels orders need. The order hook sits beside it at each completion site instead of inside it: two in the Crusher (normal and Instant Process) and one in the Furnace's `craft`.
- Every Furnace chassis from Stage 1 up is electric, so the heat slider applies on every chassis an order can run on. A fuel furnace reports a slider of 0.
- An active order writes the Mastery state on every counted completion. At level 100, where XP no longer changes the state, that adds one stats rebuild per counted completion, the same rate a machine below level 100 already has from XP.

Recorded 2026-10-25 from the chips slice:

- ModDex reports the Dispatch Terminal as blocked by its same-stage rule: by its Steel inputs it is a Stage 4 item, and it needs an output of the Stage 4 Calibrator. That is what the Stage 4 calibration gate means, and the check does not fail on it. A progression stage flag could mark the terminal as late Stage 4 if the report should go quiet.

## Progress log

- 2026-10-18: drafted the PRD and this state page.
- 2026-10-21: the reward table now pays Glyphs for the [Phase Array](phase-array.md), and the Array replaces the Exotic Affix Forge as the station that rejects orders.
- 2026-10-24: phase 2 storage. `ActiveWorkOrder` record with JSON/NBT and network codecs, the optional `work_order` field on `MachineProgressionState`, every `with...` method and Soul Transfer keep it, `forFamily` drops it on a change of family, and the paused rule for an under-stage machine. Domain checks in `WorkOrderStorageChecks`.
- 2026-10-25: phase 3 core loop. The objective now stores its work type and minimum with the target, and an order that no longer decodes is dropped instead of failing the Mastery load. Added the Work Order and Fulfilled Work Order items, the objective catalog under `work_orders/objectives`, Accept and Abandon in `MasteryOperations`, Crusher and Furnace progress hooks, the reward table with `order_tier`, `order_prestige`, and `order_roll`, the `set_order_tier` function, the Tier I recipe behind `workOrders.recipesEnabled`, and `/rngtech work_order give`. Checks: `WorkOrderRuleChecks`, `WorkOrderGameTests`, and `check-work-orders.mjs`.
- 2026-10-25: minimal Work Order panel on the Mastery tab (W toolbar button): Accept from inventory, Abandon, and progress, with the active order synced through the menu data. `WorkOrderGameTests` checks the sync.
- 2026-10-25: playtested the loop on a Stage 8 Furnace; accepting, counting, and the panel worked. The PRD was then redesigned around a Dispatch Board, random orders readable before they are taken, an Orders tab, and orders stored on the machine, and its phases were renumbered.

- 2026-10-25: rework phase 2. `WorkOrder` item data with family, tier, objective, requirements, Bounty, and Prestige; `LoadedWorkOrder`; `MachineWorkOrders` with three slots and an active index, stored as `rngtech:work_orders` on the machine and copied by the machine loot tables; `WorkOrderHost` on the Crusher and Furnace; rolled orders from the give command; and the Mastery storage removed. Checks: `WorkOrderStorageChecks`, `WorkOrderRuleChecks`, and `WorkOrderGameTests`, which now cover load rules and drop carry.

- 2026-10-25: rework phase 3. The Orders tab on the Crusher and Furnace, `WorkOrderOperations` with load, activate, deactivate, unload, and abandon, `WorkOrderActionPayload`, the three-slot menu sync, and the kept-progress component. The W panel on the Mastery tab was removed. `WorkOrderGameTests` gained a test for switching, unloading, and loading again.

- 2026-10-25: rework phase 4. The Dispatch Board block, block entity, menu, and screen; Requisitions with their recipe and a once-per-order reward pool; the board recipe; count ranges with Bounty bands; and the random give command. Checks: range and band checks in `WorkOrderRuleChecks`, `DispatchBoardGameTests`, and the Requisition and board rules in `check-work-orders.mjs`.

- 2026-10-25: Dispatch Board playtest fixes. Listed rewards on orders and Fulfilled Work Orders, with a lazy codec so domain checks stay free of item classes; the notice-board model and its note states; and the bounty-board screen with Take buttons. GameTests for exact payouts and note counts.

- 2026-10-25: Job Chips, Dispatch Terminal, and drive bays. Work Order items became the Job Chip and Completed Job Chip; the Dispatch Terminal replaced the Dispatch Board, with a console model, six listing lamps, and a terminal screen with Download and Refresh; item jobs with one to three parts and the balanced job mix; one reward kind per job; three drive-bay slots on the Orders tab replace Load and Unload; and the terminal and Requisition recipes gate on calibration stability. GameTests cover loading through the bays, pulling the active chip, putting it back, Shift-click both ways, and a bay refusing another family's chip.
- 2026-10-25: playtest fixes. Shift lists what counts toward stage jobs, Crusher inputs or Furnace materials (`WorkOrderEligibility`, which the Furnace now also uses for a smelt's stage), the terminal heading names the chassis, and finished chips stay in their bays as Completed Job Chips. GameTests check the listed items against the recipes and that a finished chip stays in its bay, inactive, until it is taken out.
- 2026-10-25: rework phase 5, job templates. `WorkOrderTemplate` (parse and roll, with no item classes), `WorkOrderTemplateCatalog` under `work_orders/templates`, one weighted terminal draw over random objectives and pooled templates, `workOrders.randomOrdersEnabled`, fixed rewards and per-template reward tables carried to the Completed Job Chip, template names on chips and terminal rows, and `/rngtech work_order give <players> template <id> [tier]`. Checks: `WorkOrderTemplateChecks` and `WorkOrderTemplateGameTests`.
- 2026-10-25: rework phase 6, conditions and Rush. `WorkOrderConditions` (profiles, rarity roll, condition roll, rewards, counting, stat application), chip rarity colours and condition lines, the Orders tab row showing the chip's tooltip, the Rush button, deadlines, and expiry, stats cache keyed on the active chip, and jam and heat-failure setbacks. Checks: `WorkOrderConditionChecks`, Rush checks in `WorkOrderRuleChecks`, and `WorkOrderGameTests` for Rush and conditions.
- 2026-10-25: rework phase 7, stat goals for the Crusher and Furnace: the `stat` filter, seven objectives sized from the level-1 processing simulation, `WorkOrderRules.reachStats`, machine stat reads, and stat text on chips and the Orders tab. Checks: stat-goal checks in `WorkOrderRuleChecks` and `WorkOrderGameTests.statGoalsReadTheMachine`.
- 2026-10-25: the Component Assembler joins Work Orders: stageless families, the `assemble` verb, the assembly report hook, three objectives, its Orders tab and own menu sync, conditions in its stats, and Rush ticking. Checks: Assembler reachability in `WorkOrderRuleChecks` and `AssemblerWorkOrderGameTests`.
- 2026-10-26: the Alloy Furnace joins Work Orders: the `alloy` verb and report, four objectives, its Orders tab and bays, conditions, the Brittle Lining setback, and Rush. Checks: Alloy reachability in `WorkOrderRuleChecks` and `AlloyFurnaceWorkOrderGameTests`, which finishes a chip with a real Bronze craft.
- 2026-10-26: the Metal Press joins Work Orders, with streak jobs: the `press` verb and report, the `streak` filter and its failure hook, five objectives, its Orders tab and bays, conditions, and Rush. Checks: streak rules in `WorkOrderRuleChecks` and `MetalPressWorkOrderGameTests`, where a crude press's real Iron Plates finish a chip.
- 2026-10-26: the Melter joins Work Orders: the `melt` verb and its mB report, fluid parts and names, three objectives, its Orders tab and bays, conditions, and Rush. Checks: Melter reachability in `WorkOrderRuleChecks` and `MelterWorkOrderGameTests`, where real Lubricant melts finish a chip.
- 2026-10-26: the Resonance Calibrator joins Work Orders: the `calibrate` verb and report, streaks with a stability minimum, Unsteady Hands and Fine Tolerance, five objectives, its Orders tab and bays, and Rush. Checks: streak and Fine Tolerance rules in `WorkOrderRuleChecks` and `CalibratorWorkOrderGameTests`, where real calibrations finish a chip.

## Verification

- 2026-10-18: documentation only. `npm run repo:check` and `mkdocs build --strict` passed. No Gradle or ModDex checks were run, because no code or data changed.
- 2026-10-24: phase 2. `./gradlew quickCheck` (including `masteryCheck`), `./gradlew runGameTestServer`, and `npm run repo:check` passed. `mkdocs build --strict` was not run locally.
- 2026-10-25: phase 3. `./gradlew quickCheck`, `./gradlew runGameTestServer` (269 tests, including `WorkOrderGameTests`), `npm run moddex:check`, and `npm run repo:check` passed. `mkdocs build --strict` was not run locally. The draft objective sizes were not checked against the processing balance simulation.
- 2026-10-25: minimal panel. `./gradlew quickCheck`, `./gradlew runGameTestServer` (269 tests), and the screen string check passed. The panel has not been checked in a running client.
- 2026-10-25: rework phase 2. `./gradlew quickCheck`, `./gradlew runGameTestServer` (270 tests), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client; `mkdocs build --strict` was not run locally.
- 2026-10-25: rework phase 3. `./gradlew quickCheck`, `./gradlew runGameTestServer` (271 tests), `npm run moddex:check` (including the screen string check), and `npm run repo:check` passed. The Orders tab has not been checked in a running client.
- 2026-10-25: rework phase 4. `./gradlew quickCheck`, `./gradlew runGameTestServer` (273 tests), `npm run moddex:check`, and `npm run repo:check` passed. `tools/asset_audit.py` reports 263 findings, none for Work Order assets; they predate this branch and the audit is not part of CI. The Dispatch Board has not been checked in a running client.
- 2026-10-25: Dispatch Board playtest fixes. `./gradlew quickCheck`, `./gradlew runGameTestServer` (275 tests), `npm run moddex:check`, and `npm run repo:check` passed. The new model and screen have not been checked in a running client.

- 2026-10-25: Job Chips, Dispatch Terminal, and drive bays. `./gradlew quickCheck`, `./gradlew runGameTestServer` (276 tests), `npm run moddex:check`, and `npm run repo:check` passed. `mkdocs build --strict` was not run locally. The terminal, its screen, and the drive bays have not been checked in a running client.
- 2026-10-25: playtest fixes. `./gradlew quickCheck`, `./gradlew runGameTestServer` (277 tests), `npm run moddex:check`, and `npm run repo:check` passed. Playtested in the client: the Shift lists (Crusher items, Furnace materials, none on heat and any-work jobs) and finished chips staying in their bays behave as intended.
- 2026-10-25: rework phase 5. `./gradlew quickCheck` (including `masteryCheck` with `WorkOrderTemplateChecks`), `./gradlew runGameTestServer` (279 tests), `npm run moddex:check`, and `npm run repo:check` passed. `mkdocs build --strict` was not run locally. Templates have not been tried from a datapack in a running client.
- 2026-10-25: rework phase 6. `./gradlew quickCheck` (`masteryCheck` 604,167 checks), `./gradlew runGameTestServer` (281 tests), `npm run moddex:check`, and `npm run repo:check` passed. `mkdocs build --strict` was not run locally. Conditions and Rush have not been checked in a running client.
- 2026-10-25: phase 7 stat goals, after merging `main`. `./gradlew quickCheck`, `./gradlew runGameTestServer` (282 tests), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client.
- 2026-10-25: Component Assembler. `./gradlew quickCheck`, `./gradlew runGameTestServer` (284 tests), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client.
- 2026-10-26: Alloy Furnace. `./gradlew quickCheck`, `./gradlew runGameTestServer` (285 tests), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client.
- 2026-10-26: Metal Press. `./gradlew quickCheck`, `./gradlew runGameTestServer` (287 tests), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client.
- 2026-10-26: Melter. `./gradlew quickCheck`, `./gradlew runGameTestServer` (288 tests), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client.
- 2026-10-26: Resonance Calibrator. `./gradlew quickCheck`, `./gradlew runGameTestServer` (289 tests), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client.
- 2026-10-26: group jobs for the Metal Press and Resonance Calibrator. `./gradlew quickCheck`, `./gradlew runGameTestServer` (289 tests; the press and Calibrator GameTests now finish group jobs from real presses and calibrations), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client.
- 2026-10-26: Forestry Companion. `./gradlew quickCheck`, `./gradlew runGameTestServer` (290 tests), `npm run moddex:check`, and `npm run repo:check` passed. Not checked in a running client; no GameTest fells a real tree for a chip.

## Handoff and limits

- Tier stages, target times, condition values, rewards, the board size, the slot count, and the Requisition recipe are draft numbers. Tune them with the processing balance simulation.
- The objective filters assume each family can report the listed values at the moment work completes. Check each hook before committing to an objective type.
- Shaped models exist for some families only, so the order sheet cue is per model, with a status light as the fallback.
