package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.WorkOrderMachineBlock;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.item.CompletedJobChipItem;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.menu.CrusherMenu;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.recipe.CrusherRecipe;
import com.rngtech.content.recipe.FurnaceRecipe;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.content.workorder.WorkOrderEligibility;
import com.rngtech.content.workorder.WorkOrderOperations;
import com.rngtech.content.workorder.WorkOrderRewards;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.workorder.FulfilledWorkOrder;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderConditions;
import com.rngtech.rpg.workorder.WorkOrderProgress;
import com.rngtech.rpg.workorder.WorkOrderRules;
import com.rngtech.rpg.workorder.WorkPart;
import com.rngtech.rpg.workorder.WorkReport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Work Order loop on a real Crusher: load Job Chips through the drive bays, count matching crushes, drop and redeem the
 * Completed Job Chip, and keep loaded chips on the dropped machine.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkOrderGameTests {
    private static final int LEFT = 2;

    private WorkOrderGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 2400)
    public static void crusherFulfilsAWorkOrder(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CrusherBlockEntity crusher = steelCrusher(helper, pos);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.STEEL).get()));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.PAPER));
        player.getInventory().setItem(1, order("crusher", 2, 5, 0));
        player.getInventory().setItem(2, order("furnace", 1, 4, 0));
        player.getInventory().setItem(3, order("crusher", 1, 4, 60));
        player.getInventory().setItem(4, new ItemStack(ModItems.JOB_CHIP.get()));
        player.getInventory().setItem(5, order("crusher", 1, 4, 0));
        CrusherMenu menu = ordersMenu(crusher, player);
        check(!shiftIntoBay(menu, player, 0), "Loaded paper as a Job Chip");
        check(!shiftIntoBay(menu, player, 1), "A Stage 4 Crusher loaded a Stage 5 chip");
        check(!shiftIntoBay(menu, player, 2), "A Crusher loaded a Furnace chip");
        check(!shiftIntoBay(menu, player, 3), "A level 1 Crusher loaded a chip that needs level 60");
        check(!shiftIntoBay(menu, player, 4), "A Crusher loaded a blank chip");
        check(crusher.workOrders().isEmpty(), "A refused chip loaded: " + crusher.workOrders());
        check(shiftIntoBay(menu, player, 5), "A level 1 Crusher refused a chip with no Mastery requirement");
        LoadedWorkOrder loaded = crusher.workOrders().activeOrder();
        check(loaded != null && loaded.order().tier() == 1 && loaded.order().objective().parts().getFirst().minimum() == 3, "The Crusher did not activate the order: " + loaded);

        // Skip most of the job, then let the Crusher finish it: raw iron is below the minimum, raw nickel counts.
        crusher.setWorkOrders(crusher.workOrders().withReplaced(0, loaded.withPartProgress(0, loaded.order().objective().totalTarget() - LEFT)));
        ItemStack rawNickel = new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("raw_nickel")), LEFT);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.RAW_IRON, 2));
        MachineTestSupport.powerEachTick(helper, crusher.getEnergyStorage(null));
        AABB above = new AABB(helper.absolutePos(pos)).inflate(2);

        helper.startSequence()
                .thenWaitUntil(() -> check(crusher.getInventory().getStackInSlot(CrusherBlockEntity.SLOT_INPUT_A).isEmpty(), "Crushing raw iron"))
                .thenExecute(() -> {
                    LoadedWorkOrder running = crusher.workOrders().activeOrder();
                    check(running != null && running.totalProgress() == running.order().objective().totalTarget() - LEFT, "Raw iron counted toward the order");
                    crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
                    crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, rawNickel.copy());
                })
                .thenWaitUntil(() -> check(!crusher.workOrders().isEmpty() && crusher.workOrders().orders().getFirst().fulfilled(),
                        "Waiting for the job to finish: " + crusher.workOrders() + "; " + MachineTestSupport.describe(crusher)))
                .thenExecute(() -> {
                    check(crusher.workOrders().active() == MachineWorkOrders.NONE, "The finished chip is still active");
                    check(!WorkOrderOperations.perform(player, crusher, "activate", "0"), "The finished chip became active again");
                    List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, above,
                            entity -> entity.getItem().is(ModItems.COMPLETED_JOB_CHIP.get()));
                    check(drops.isEmpty(), "The Crusher dropped the Completed Job Chip instead of keeping it in its bay");
                    menu.clicked(bay(menu, 0), 0, ClickType.PICKUP, player);
                    ItemStack fulfilled = menu.getCarried().copy();
                    menu.setCarried(ItemStack.EMPTY);
                    check(fulfilled.is(ModItems.COMPLETED_JOB_CHIP.get()) && crusher.workOrders().isEmpty(),
                            "Taking the finished chip out did not give a Completed Job Chip: " + fulfilled + "; " + crusher.workOrders());
                    var data = CompletedJobChipItem.data(fulfilled);
                    check(data.tier() == 1 && data.family().equals("crusher") && data.bounty() == WorkOrder.BASE_BOUNTY,
                            "The Completed Job Chip did not keep its job: " + data);
                    player.getInventory().clearContent();
                    player.setItemInHand(InteractionHand.MAIN_HAND, fulfilled);
                    fulfilled.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                    check(player.getMainHandItem().isEmpty(), "Opening the order did not use it up");
                    Set<Item> kinds = new HashSet<>();
                    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        ItemStack reward = player.getInventory().getItem(slot);
                        if (!reward.isEmpty()) {
                            kinds.add(reward.getItem());
                        }
                    }
                    check(kinds.size() == 1, "Redeeming the chip paid " + kinds.size() + " reward kinds; expected exactly one: " + kinds);
                })
                .thenSucceed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void abandoningAnOrderReturnsNothing(GameTestHelper helper) {
        CrusherBlockEntity crusher = steelCrusher(helper, new BlockPos(2, 1, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, order("crusher", 1, 4, 0));
        check(!WorkOrderOperations.perform(player, crusher, "abandon", "0"), "Abandoned an order that was not there");
        check(shiftIntoBay(ordersMenu(crusher, player), player, 0), "The Crusher refused a Tier I chip");
        WorkOrderMenuSupport.Slot synced = synced(crusher).activeSlot();
        check(synced != null && synced.tier() == 1 && synced.objective().parts().getFirst().type().equals(WorkObjectiveType.CRUSH.id())
                && synced.objective().parts().getFirst().minimum() == 3 && synced.totalTarget() == 256 && synced.totalProgress() == 0
                && synced.bounty() == WorkOrder.BASE_BOUNTY,
                "The Orders tab did not sync the active order: " + synced);
        check(WorkOrderOperations.perform(player, crusher, "abandon", "0"), "The Crusher kept its order");
        check(crusher.workOrders().isEmpty() && synced(crusher).slots().isEmpty(), "The order is still loaded or synced");
        boolean empty = true;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            empty &= player.getInventory().getItem(slot).isEmpty();
        }
        check(empty, "Abandoning returned an item");
        helper.succeed();
    }

    /** Loaded orders, their progress, and which one is active stay on the dropped machine and come back when it is placed. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void loadedOrdersDropWithTheMachine(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 2);
        CrusherBlockEntity crusher = steelCrusher(helper, pos);
        MachineWorkOrders orders = MachineWorkOrders.EMPTY
                .withLoaded(new LoadedWorkOrder(JobChipItem.data(order("crusher", 1, 4, 0)), MachineTraits.EMPTY).withPartProgress(0, 40))
                .withLoaded(new LoadedWorkOrder(JobChipItem.data(order("crusher", 2, 4, 0)), MachineTraits.EMPTY).withPartProgress(0, 7))
                .withActive(1);
        crusher.setWorkOrders(orders);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), crusher);
        ItemStack dropped = drops.stream().filter(stack -> stack.has(ModDataComponents.WORK_ORDERS.get())).findFirst().orElse(ItemStack.EMPTY);
        check(orders.equals(dropped.get(ModDataComponents.WORK_ORDERS.get())), "The dropped Crusher lost its orders: " + drops);

        CrusherBlockEntity placed = steelCrusher(helper, new BlockPos(3, 1, 2));
        placed.applyComponentsFromItemStack(dropped);
        check(orders.equals(placed.workOrders()), "The placed Crusher did not get its orders back: " + placed.workOrders());
        helper.succeed();
    }

    /**
     * Switching keeps each chip's progress. A chip taken out of its bay keeps its progress and loads again; taking out the
     * active chip leaves none active.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void switchingAndPullingChipsKeepProgress(GameTestHelper helper) {
        CrusherBlockEntity crusher = steelCrusher(helper, new BlockPos(2, 1, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, order("crusher", 1, 4, 0));
        player.getInventory().setItem(1, order("crusher", 2, 4, 0));
        CrusherMenu menu = ordersMenu(crusher, player);
        check(shiftIntoBay(menu, player, 0) && shiftIntoBay(menu, player, 1), "The Crusher refused two chips");
        check(crusher.workOrders().active() == 0, "The first chip loaded did not become active");
        crusher.workOrderWork(new WorkReport(WorkObjectiveType.CRUSH, 6, 0, "", 30));
        check(WorkOrderOperations.perform(player, crusher, "activate", "1"), "The Crusher refused to switch chips");
        crusher.workOrderWork(new WorkReport(WorkObjectiveType.CRUSH, 6, 0, "", 4));
        check(crusher.workOrders().orders().get(0).totalProgress() == 30 && crusher.workOrders().orders().get(1).totalProgress() == 4,
                "Switching lost progress: " + crusher.workOrders());

        menu.clicked(bay(menu, 1), 0, ClickType.PICKUP, player);
        WorkOrderProgress pulled = menu.getCarried().get(ModDataComponents.WORK_ORDER_PROGRESS.get());
        check(JobChipItem.isWorkOrder(menu.getCarried()) && pulled != null && pulled.progress().getFirst() == 4, "The pulled chip lost its progress: " + pulled);
        check(crusher.workOrders().orders().size() == 1 && crusher.workOrders().active() == MachineWorkOrders.NONE,
                "Pulling the active chip left one active: " + crusher.workOrders());
        menu.clicked(bay(menu, 2), 0, ClickType.PICKUP, player);
        check(menu.getCarried().isEmpty() && crusher.workOrders().orders().size() == 2 && crusher.workOrders().orders().get(1).totalProgress() == 4,
                "Putting the chip back did not load it with its progress: " + crusher.workOrders());

        menu.quickMoveStack(player, bay(menu, 0));
        int landed = -1;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && landed < 0; slot++) {
            landed = JobChipItem.isWorkOrder(player.getInventory().getItem(slot)) ? slot : -1;
        }
        check(landed >= 0, "Shift-clicking the bay did not move its chip into the inventory");
        ItemStack unloaded = player.getInventory().getItem(landed);
        WorkOrderProgress kept = unloaded.get(ModDataComponents.WORK_ORDER_PROGRESS.get());
        check(JobChipItem.isWorkOrder(unloaded) && kept != null && kept.progress().getFirst() == 30, "The shift-clicked chip lost its progress: " + kept);
        check(crusher.workOrders().orders().size() == 1 && crusher.workOrders().orders().getFirst().totalProgress() == 4,
                "Taking out a chip did not move the next one up: " + crusher.workOrders());
        check(shiftIntoBay(menu, player, landed), "The Crusher refused the chip it gave back");
        check(crusher.workOrders().orders().get(1).totalProgress() == 30, "Loading the chip again did not resume it: " + crusher.workOrders());
        check(WorkOrderOperations.perform(player, crusher, "deactivate", "") && crusher.workOrders().activeOrder() == null, "Pause left a chip active");
        helper.succeed();
    }

    /**
     * The Job Chip lamp is lit exactly while a chip is active, a tick's stale running-state write does not relight it, and a
     * machine placed with an active chip lights it within a second.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void jobChipLampFollowsTheActiveChip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CrusherBlockEntity crusher = steelCrusher(helper, pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, order("crusher", 1, 4, 0));
        CrusherMenu menu = ordersMenu(crusher, player);
        check(!lampLit(helper, pos), "The lamp was lit with no chip");
        check(shiftIntoBay(menu, player, 0) && lampLit(helper, pos), "Loading a chip did not light the lamp");
        BlockState tickStart = helper.getBlockState(pos);
        check(WorkOrderOperations.perform(player, crusher, "deactivate", "") && !lampLit(helper, pos), "Pausing the chip left the lamp lit");
        BaseMachineBlock.setActive(helper.getLevel(), helper.absolutePos(pos), tickStart, !tickStart.getValue(BaseMachineBlock.ACTIVE));
        check(!lampLit(helper, pos), "A running-state change from the tick's start relit the lamp");
        check(WorkOrderOperations.perform(player, crusher, "activate", "0") && lampLit(helper, pos), "Activating the chip did not light the lamp");

        BlockPos placedPos = new BlockPos(4, 1, 2);
        CrusherBlockEntity placed = steelCrusher(helper, placedPos);
        ItemStack machine = new ItemStack(ModBlocks.crusherChassis(CrusherChassisMaterial.STEEL).get());
        machine.set(ModDataComponents.WORK_ORDERS.get(), crusher.workOrders());
        placed.applyComponentsFromItemStack(machine);
        check(placed.workOrders().activeOrder() != null && !lampLit(helper, placedPos), "The placed Crusher did not start with an unlit active chip");
        helper.succeedWhen(() -> check(lampLit(helper, placedPos), "A Crusher placed with an active chip never lit its lamp"));
    }

    private static boolean lampLit(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(WorkOrderMachineBlock.WORK_ORDER);
    }

    /** A bay refuses a chip for another machine family, so the chip stays on the cursor. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void driveBaysRefuseAnotherFamily(GameTestHelper helper) {
        CrusherBlockEntity crusher = steelCrusher(helper, new BlockPos(2, 1, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        CrusherMenu menu = ordersMenu(crusher, player);
        menu.setCarried(order("furnace", 1, 4, 0));
        menu.clicked(bay(menu, 0), 0, ClickType.PICKUP, player);
        check(crusher.workOrders().isEmpty() && JobChipItem.isWorkOrder(menu.getCarried()), "A Crusher bay took a Furnace chip: " + crusher.workOrders());
        helper.succeed();
    }

    /** A Completed Job Chip with a listed reward pays exactly that list. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void listedRewardsPayExactly(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack fulfilled = new ItemStack(ModItems.COMPLETED_JOB_CHIP.get());
        ItemStack chaos = new ItemStack(ModItems.CHAOS_CRYSTAL.get(), 3);
        fulfilled.set(ModDataComponents.FULFILLED_WORK_ORDER.get(), new FulfilledWorkOrder(2, "crusher", "", 150, 0,
                ItemContainerContents.fromItems(List.of(chaos, new ItemStack(ModItems.REQUISITION.get())))));
        player.setItemInHand(InteractionHand.MAIN_HAND, fulfilled);
        fulfilled.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        int crystals = 0;
        int requisitions = 0;
        int other = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.CHAOS_CRYSTAL.get())) {
                crystals += stack.getCount();
            } else if (stack.is(ModItems.REQUISITION.get())) {
                requisitions += stack.getCount();
            } else if (!stack.isEmpty()) {
                other++;
            }
        }
        check(crystals == 3 && requisitions == 1 && other == 0,
                "The order paid " + crystals + " Chaos Crystals, " + requisitions + " Requisitions, and " + other + " other stacks");
        helper.succeed();
    }

    private static CrusherBlockEntity steelCrusher(GameTestHelper helper, BlockPos pos) {
        return MachineTestSupport.place(helper, pos, ModBlocks.crusherChassis(CrusherChassisMaterial.STEEL).get(), CrusherBlockEntity.class);
    }

    private static ItemStack order(String family, int tier, int minStage, int minMasteryLevel) {
        WorkObjective objective = new WorkObjective(RNGTech.id(family + "_test"),
                new WorkPart(family.equals("crusher") ? WorkObjectiveType.CRUSH : WorkObjectiveType.SMELT, WorkFilter.STAGE, 3, "", 256));
        return JobChipItem.of(ModItems.JOB_CHIP.get(),
                new WorkOrder(family, tier, objective, minStage, minMasteryLevel, WorkOrder.BASE_BOUNTY, 0), MachineTraits.EMPTY);
    }

    /**
     * A stage job's Shift list names exactly what the machine would count: the Crusher inputs at the Processing Level, or
     * the materials of the Furnace recipes at the Stage. Heat and any-work jobs have no list.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void stageJobsListWhatCounts(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        WorkPart crush = new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.STAGE, 6, "", 100);
        List<Item> crushed = WorkOrderEligibility.items(recipes, crush);
        check(!crushed.isEmpty(), "No Crusher input counts toward Processing Level 6+");
        for (RecipeHolder<CrusherRecipe> holder : recipes.getAllRecipesFor(ModRecipes.CRUSHER_TYPE.get())) {
            for (ItemStack input : holder.value().ingredient().getItems()) {
                boolean counts = holder.value().requiredProcessingLevel() >= 6;
                check(!counts || crushed.contains(input.getItem()), "The list misses " + input + " from " + holder.id());
            }
        }
        check(!crushed.contains(Items.RAW_IRON), "Raw iron is listed for Processing Level 6+");

        WorkPart smelt = new WorkPart(WorkObjectiveType.SMELT, WorkFilter.STAGE, 5, "", 100);
        List<String> materials = WorkOrderEligibility.materials(recipes, smelt);
        check(!materials.isEmpty() && materials.size() == WorkOrderEligibility.names(recipes, smelt).size(), "No material counts toward Stage 5+");
        for (RecipeHolder<FurnaceRecipe> holder : recipes.getAllRecipesFor(ModRecipes.FURNACE_TYPE.get())) {
            boolean counts = WorkOrderEligibility.smeltStage(holder.value()) >= 5;
            check(counts == materials.contains(WorkOrderEligibility.material(holder.value())),
                    "The material list disagrees with " + holder.id() + " at stage " + WorkOrderEligibility.smeltStage(holder.value()));
        }
        check(!WorkOrderEligibility.lists(new WorkPart(WorkObjectiveType.SMELT, WorkFilter.SETTING, 60, "", 100))
                && !WorkOrderEligibility.lists(new WorkPart(WorkObjectiveType.SMELT, WorkFilter.ANY, 0, "", 100)), "A heat or any-work job has a Shift list");
        check(!WorkOrderEligibility.lists(new WorkPart(WorkObjectiveType.HARVEST, WorkFilter.STAGE, 2, "", 100))
                && !WorkOrderEligibility.lists(new WorkPart(WorkObjectiveType.RECYCLE, WorkFilter.STAGE, 2, "", 100)),
                "A Forestry or Recycler stage job lists items it does not take");
        WorkPart calibrated = new WorkPart(WorkObjectiveType.CALIBRATE, WorkFilter.STAGE, 5, "", 100);
        List<Item> calibrations = WorkOrderEligibility.calibrations(recipes, calibrated);
        check(WorkOrderEligibility.lists(calibrated) && !calibrations.isEmpty() && calibrations.size() == WorkOrderEligibility.names(recipes, calibrated).size()
                && calibrations.stream().noneMatch(item -> crushed.contains(item)), "A Calibrator stage job lists the Crusher's inputs: " + calibrations);
        helper.succeed();
    }

    /**
     * Rush activates a chip with a deadline of the job's Rush clock and half again its listed reward. Past the deadline the
     * chip is lost from the bay, from an inventory, and a bay refuses it.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void rushRaisesTheRewardAndExpires(GameTestHelper helper) {
        CrusherBlockEntity crusher = steelCrusher(helper, new BlockPos(2, 1, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        WorkOrder listed = JobChipItem.data(order("crusher", 1, 4, 0)).withRewards(ItemContainerContents.fromItems(List.of(new ItemStack(ModItems.CHAOS_CRYSTAL.get(), 4))));
        player.getInventory().setItem(0, JobChipItem.of(ModItems.JOB_CHIP.get(), listed, MachineTraits.EMPTY));
        player.getInventory().setItem(1, order("crusher", 2, 4, 0));
        CrusherMenu menu = ordersMenu(crusher, player);
        check(shiftIntoBay(menu, player, 0) && shiftIntoBay(menu, player, 1), "The Crusher refused the chips");
        long now = helper.getLevel().getGameTime();
        check(WorkOrderOperations.perform(player, crusher, "rush", "1"), "The Crusher refused Rush");
        LoadedWorkOrder rushed = crusher.workOrders().orders().get(1);
        long clock = WorkOrderRules.rushTicks(rushed.order(), rushed.traits());
        check(crusher.workOrders().active() == 1 && rushed.rushed() && rushed.rushDeadline() >= now + clock
                && rushed.rushDeadline() <= now + clock + 5, "Rush did not activate the chip with its deadline: " + crusher.workOrders());
        check(!WorkOrderOperations.perform(player, crusher, "rush", "1"), "Rush ran twice on one chip");
        check(WorkOrderOperations.perform(player, crusher, "rush", "0"), "The Crusher refused Rush on the listed chip");
        List<ItemStack> boosted = WorkOrderRewards.items(crusher.workOrders().orders().get(0).order().rewards());
        check(boosted.size() == 1 && boosted.getFirst().getCount() == 6, "Rush did not raise the listed reward by half: " + boosted);
        check(crusher.workOrders().orders().get(1).order().bounty() == WorkOrder.rushAmount(WorkOrder.BASE_BOUNTY),
                "Rush did not raise an unlisted chip's Bounty by half");

        crusher.setWorkOrders(crusher.workOrders().withReplaced(1, rushed.withRush(now - 1)));
        crusher.workOrderTick();
        check(crusher.workOrders().orders().size() == 1 && crusher.workOrders().orders().getFirst().rushed(), "The expired chip stayed in its bay: " + crusher.workOrders());

        ItemStack late = order("crusher", 1, 4, 0);
        late.set(ModDataComponents.WORK_ORDER_PROGRESS.get(), new WorkOrderProgress(List.of(5L), 5, now - 1));
        player.getInventory().setItem(2, late);
        check(!shiftIntoBay(menu, player, 2), "A bay took an expired Rush chip");
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ItemStack held = player.getInventory().getItem(2);
                    held.inventoryTick(helper.getLevel(), player, 2, false);
                    check(held.isEmpty(), "Waiting for the expired chip to leave the inventory");
                })
                .thenSucceed();
    }

    /**
     * A chip's pressure conditions cost the machine only while the chip is active, and a reset condition sends the active
     * job back to 0 on its event.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void conditionsCostTheMachineWhileActive(GameTestHelper helper) {
        CrusherBlockEntity crusher = steelCrusher(helper, new BlockPos(2, 1, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        RandomSource random = RandomSource.create(2L);
        MachineTraits traits = new MachineTraits(Rarity.MAGIC, 0, List.of(
                WorkOrderConditions.get(WorkOrderConditions.STARVED_FEED).definition().roll(3, random),
                WorkOrderConditions.get(WorkOrderConditions.JAMMED_LINE).definition().roll(1, random)), List.of());
        double speed = crusher.effectiveStats().value(MachineStat.PROCESSING_SPEED);
        player.getInventory().setItem(0, JobChipItem.of(ModItems.JOB_CHIP.get(), JobChipItem.data(order("crusher", 1, 4, 0)), traits));
        player.getInventory().setItem(1, order("crusher", 1, 4, 0));
        CrusherMenu menu = ordersMenu(crusher, player);
        check(shiftIntoBay(menu, player, 0) && shiftIntoBay(menu, player, 1), "The Crusher refused the chips");
        check(crusher.workOrders().orders().getFirst().traits().equals(traits), "The bay lost the chip's conditions");
        double starved = crusher.effectiveStats().value(MachineStat.PROCESSING_SPEED);
        check(starved < speed * 0.76 && starved > speed * 0.69, "Starved Feed did not slow the Crusher: " + speed + " -> " + starved);

        crusher.workOrderWork(new WorkReport(WorkObjectiveType.CRUSH, 6, 0, "", 40));
        crusher.workOrderSetback(WorkOrderConditions.BRITTLE_LINING);
        check(crusher.workOrders().activeOrder().totalProgress() == 40, "A setback the chip does not carry reset it");
        crusher.workOrderSetback(WorkOrderConditions.JAMMED_LINE);
        check(crusher.workOrders().activeOrder().totalProgress() == 0, "Jammed Line did not reset the job on a jam");

        check(WorkOrderOperations.perform(player, crusher, "activate", "1"), "The Crusher refused to switch chips");
        check(Math.abs(crusher.effectiveStats().value(MachineStat.PROCESSING_SPEED) - speed) < 1.0E-6,
                "The inactive chip's condition still slowed the Crusher");
        helper.succeed();
    }

    /** A stat goal reads the Crusher's effective stat at each completed job, and is met once the stat reaches its target. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void statGoalsReadTheMachine(GameTestHelper helper) {
        CrusherBlockEntity crusher = steelCrusher(helper, new BlockPos(2, 1, 2));
        double speed = crusher.workOrderStat("processing_speed");
        long current = Math.round(speed * WorkPart.STAT_SCALE);
        check(speed > 0 && speed == crusher.effectiveStats().value(MachineStat.PROCESSING_SPEED), "The Crusher did not report its Processing Speed");
        WorkOrder high = new WorkOrder("crusher", 1, new WorkObjective(RNGTech.id("goal_high"),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.STAT, 0, "processing_speed", current + 50)), 4, 0, 100, 0);
        WorkOrder low = new WorkOrder("crusher", 1, new WorkObjective(RNGTech.id("goal_low"),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.STAT, 0, "processing_speed", Math.max(1, current - 10))), 4, 0, 100, 0);
        crusher.setWorkOrders(MachineWorkOrders.EMPTY.withLoaded(new LoadedWorkOrder(high, MachineTraits.EMPTY))
                .withLoaded(new LoadedWorkOrder(low, MachineTraits.EMPTY)));
        crusher.workOrderWork(new WorkReport(WorkObjectiveType.CRUSH, 1, 0, "minecraft:raw_iron", 1));
        LoadedWorkOrder reading = crusher.workOrders().orders().get(0);
        check(reading.totalProgress() == current && !reading.fulfilled(), "The goal did not show the Crusher's speed below its target: " + reading.progress());
        check(WorkOrderOperations.perform(helper.makeMockPlayer(GameType.SURVIVAL), crusher, "activate", "1"), "The Crusher refused to switch goals");
        crusher.workOrderWork(new WorkReport(WorkObjectiveType.CRUSH, 1, 0, "minecraft:raw_iron", 1));
        check(crusher.workOrders().orders().get(1).fulfilled() && crusher.workOrders().active() == MachineWorkOrders.NONE,
                "A goal under the Crusher's speed was not met at the next job: " + crusher.workOrders());
        helper.succeed();
    }

    /** The Crusher's menu as the server sees it after the screen opens the Orders tab, so Shift-click fills the bays. */
    private static CrusherMenu ordersMenu(CrusherBlockEntity crusher, Player player) {
        CrusherMenu menu = (CrusherMenu) crusher.createMenu(1, player.getInventory(), player);
        check(menu.clickMenuButton(player, WorkOrderMenuSupport.BUTTON_SELECT_TAB + CrusherMenu.TAB_ORDERS) && menu.selectedTab() == CrusherMenu.TAB_ORDERS,
                "The server menu did not follow the screen to the Orders tab");
        return menu;
    }

    /** The menu index of drive bay {@code bay}; the bays are the menu's last slots. */
    private static int bay(CrusherMenu menu, int bay) {
        return menu.slots.size() - MachineWorkOrders.SLOTS + bay;
    }

    /** Shift-clicks the player's inventory slot {@code slot} and returns whether it went into a bay. */
    private static boolean shiftIntoBay(CrusherMenu menu, Player player, int slot) {
        long before = loadedBays(menu);
        for (int index = 0; index < bay(menu, 0); index++) {
            Slot candidate = menu.getSlot(index);
            if (candidate.container == player.getInventory() && candidate.getContainerSlot() == slot) {
                menu.quickMoveStack(player, index);
            }
        }
        return loadedBays(menu) > before && player.getInventory().getItem(slot).isEmpty();
    }

    private static long loadedBays(CrusherMenu menu) {
        return menu.slots.subList(bay(menu, 0), menu.slots.size()).stream().filter(Slot::hasItem).count();
    }

    /** The loaded orders as the Orders tab reads them from the menu's synced data. */
    private static WorkOrderMenuSupport.View synced(CrusherBlockEntity crusher) {
        SimpleContainerData data = new SimpleContainerData(MasteryMenuSupport.FIELD_COUNT);
        for (int field = 0; field < MasteryMenuSupport.FIELD_COUNT; field++) {
            data.set(field, MasteryMenuSupport.get(crusher, field, crusher::effectiveStats));
        }
        return MasteryMenuSupport.workOrders(data, 0);
    }
}
