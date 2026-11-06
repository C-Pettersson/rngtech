package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.WoodenComposterBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The Wooden Composter's catalyst slot: Rich Biomass at the crafting grid's ratios, with one kind of item in the output. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WoodenComposterGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);
    private static final int UNITS_PER_BATCH = WoodenComposterBlockEntity.COMPOST_UNITS_PER_BATCH;

    private WoodenComposterGameTests() {
    }

    /** A catalyst makes the batch Rich Biomass for the same compost, and each one pays for one batch. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void boneMealMakesRichBiomass(GameTestHelper helper) {
        WoodenComposterBlockEntity composter = composter(helper, 2 * UNITS_PER_BATCH + 1);
        composter.getInventory().setStackInSlot(WoodenComposterBlockEntity.SLOT_CATALYST, new ItemStack(Items.BONE_MEAL, 2));

        runBatches(helper, composter, 1);

        check(output(composter).is(ModItems.RICH_BIOMASS.get()) && output(composter).getCount() == 1, "Expected 1 Rich Biomass: " + output(composter));
        check(catalyst(composter).getCount() == 1, "One Bone Meal should pay for one batch: " + catalyst(composter));
        check(composter.getInventory().getStackInSlot(0).getCount() == UNITS_PER_BATCH + 1, "The batch should use the usual compost");
        helper.succeed();
    }

    /** One Urea pays for four batches, as in the crafting grid; then Rich Biomass in the output stops the Composter. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void ureaPaysForFourBatchesThenWaits(GameTestHelper helper) {
        WoodenComposterBlockEntity composter = composter(helper, 6 * UNITS_PER_BATCH);
        composter.getInventory().setStackInSlot(WoodenComposterBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.UREA.get()));

        runBatches(helper, composter, 1);
        check(catalyst(composter).isEmpty(), "The first batch should take the Urea: " + catalyst(composter));
        runBatches(helper, composter, 3);
        check(output(composter).is(ModItems.RICH_BIOMASS.get()) && output(composter).getCount() == 4, "Expected 4 Rich Biomass: " + output(composter));

        int compostLeft = composter.getInventory().getStackInSlot(0).getCount();
        runBatches(helper, composter, 1);
        check(output(composter).getCount() == 4, "A fifth batch has no catalyst and cannot stack on Rich Biomass: " + output(composter));
        check(composter.getInventory().getStackInSlot(0).getCount() == compostLeft, "A stopped Composter should keep its compost");
        check(composter.getMenuData().get(5) == WoodenComposterBlockEntity.STATUS_OUTPUT_MIXED, "The status should say why it stopped");

        composter.getInventory().setStackInSlot(WoodenComposterBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        runBatches(helper, composter, 1);
        check(output(composter).is(ModItems.COMPOSTED_BIOMASS.get()), "An empty output with no catalyst should get Composted Biomass: " + output(composter));
        helper.succeed();
    }

    /** Composted Biomass already in the output keeps the batch Composted, and the catalyst waits for an empty slot. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void compostedOutputKeepsTheCatalyst(GameTestHelper helper) {
        WoodenComposterBlockEntity composter = composter(helper, 2 * UNITS_PER_BATCH);
        composter.getInventory().setStackInSlot(WoodenComposterBlockEntity.SLOT_OUTPUT, new ItemStack(ModItems.COMPOSTED_BIOMASS.get()));
        composter.getInventory().setStackInSlot(WoodenComposterBlockEntity.SLOT_CATALYST, new ItemStack(Items.BONE_MEAL));

        runBatches(helper, composter, 1);
        check(output(composter).is(ModItems.COMPOSTED_BIOMASS.get()) && output(composter).getCount() == 2, "Expected 2 Composted Biomass: " + output(composter));
        check(catalyst(composter).getCount() == 1, "The catalyst should wait: " + catalyst(composter));

        composter.getInventory().setStackInSlot(WoodenComposterBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        runBatches(helper, composter, 1);
        check(output(composter).is(ModItems.RICH_BIOMASS.get()), "An empty output should take Rich Biomass: " + output(composter));
        check(catalyst(composter).isEmpty(), "The catalyst should now be spent: " + catalyst(composter));
        helper.succeed();
    }

    /** Catalysts go only to their own slot, so a Bone Meal line cannot fill the input slots, and they cannot be pulled out. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void automationFeedsTheCatalystSlot(GameTestHelper helper) {
        WoodenComposterBlockEntity composter = composter(helper, 0);
        IItemHandler top = composter.getItemHandler(Direction.UP);

        ItemStack left = ItemHandlerHelper.insertItem(top, new ItemStack(Items.BONE_MEAL, 96), false);
        check(catalyst(composter).getCount() == 64 && left.getCount() == 32, "Bone Meal should stop at one full stack, left " + left);
        check(ItemHandlerHelper.insertItem(top, new ItemStack(Items.WHEAT_SEEDS), false).isEmpty(), "Compost should still fit");

        IItemHandler bottom = composter.getItemHandler(Direction.DOWN);
        for (int slot = 0; slot < bottom.getSlots(); slot++) {
            check(bottom.extractItem(slot, 1, true).isEmpty(), "The bottom should not extract the catalyst, slot " + slot);
        }
        helper.succeed();
    }

    /** A save from before the catalyst slot, with 27 input slots: everything lands on today's slots. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void saveWithoutCatalystSlotMoves(GameTestHelper helper) {
        WoodenComposterBlockEntity composter = composter(helper, 0);
        ItemStackHandler legacy = new ItemStackHandler(30);
        legacy.setStackInSlot(0, new ItemStack(Items.WHEAT_SEEDS, 10));
        legacy.setStackInSlot(25, new ItemStack(Items.WHEAT_SEEDS, 5));
        legacy.setStackInSlot(27, new ItemStack(ModItems.COMPOSTED_BIOMASS.get(), 3));
        legacy.setStackInSlot(28, new ItemStack(Items.WATER_BUCKET));
        legacy.setStackInSlot(29, new ItemStack(Items.BUCKET, 2));

        load(helper, composter, legacy);

        check(composter.getInventory().getSlots() == WoodenComposterBlockEntity.SLOT_COUNT, "Expected today's slot count: " + composter.getInventory().getSlots());
        check(composter.getInventory().getStackInSlot(0).getCount() == 15, "The stack past the new inputs should merge: " + composter.getInventory().getStackInSlot(0));
        check(output(composter).is(ModItems.COMPOSTED_BIOMASS.get()) && output(composter).getCount() == 3, "The output should move: " + output(composter));
        check(composter.getInventory().getStackInSlot(WoodenComposterBlockEntity.SLOT_WATER_INPUT_CONTAINER).is(Items.WATER_BUCKET), "The water bucket should move");
        check(composter.getInventory().getStackInSlot(WoodenComposterBlockEntity.SLOT_WATER_OUTPUT_CONTAINER).getCount() == 2, "The empty buckets should move");
        check(composter.getInventory().insertItem(WoodenComposterBlockEntity.SLOT_CATALYST, new ItemStack(Items.BONE_MEAL), false).isEmpty(), "The catalyst slot should work");
        helper.succeed();
    }

    /** A save with 27 inputs and a catalyst: the catalyst moves, and stacks with no room drop instead of vanishing. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void saveWithFullInputsDropsWhatNoLongerFits(GameTestHelper helper) {
        WoodenComposterBlockEntity composter = composter(helper, 0);
        ItemStackHandler legacy = new ItemStackHandler(31);
        for (int slot = 0; slot < 27; slot++) {
            legacy.setStackInSlot(slot, new ItemStack(Items.WHEAT_SEEDS, 64));
        }
        legacy.setStackInSlot(30, new ItemStack(Items.BONE_MEAL, 7));

        load(helper, composter, legacy);

        check(catalyst(composter).is(Items.BONE_MEAL) && catalyst(composter).getCount() == 7, "The catalyst should move: " + catalyst(composter));
        WoodenComposterBlockEntity.serverTick(helper.getLevel(), composter.getBlockPos(), composter.getBlockState(), composter);
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(composter.getBlockPos()).inflate(3.0D), entity -> entity.getItem().is(Items.WHEAT_SEEDS))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        check(dropped == 3 * 64, "Three stacks should drop, got " + dropped);
        helper.succeed();
    }

    private static void load(GameTestHelper helper, WoodenComposterBlockEntity composter, ItemStackHandler legacy) {
        CompoundTag saved = composter.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.put("Inventory", legacy.serializeNBT(helper.getLevel().registryAccess()));
        composter.loadWithComponents(saved, helper.getLevel().registryAccess());
    }

    private static WoodenComposterBlockEntity composter(GameTestHelper helper, int seeds) {
        WoodenComposterBlockEntity composter = MachineTestSupport.place(helper, POS, ModBlocks.WOODEN_COMPOSTER.get(), WoodenComposterBlockEntity.class);
        if (seeds > 0) {
            composter.getInventory().setStackInSlot(0, new ItemStack(Items.WHEAT_SEEDS, seeds));
        }
        return composter;
    }

    /** Ticks directly, so a test does not wait out a dry batch per batch. One dry batch at a time is the slowest case. */
    private static void runBatches(GameTestHelper helper, WoodenComposterBlockEntity composter, int batches) {
        for (int tick = 0; tick < batches * WoodenComposterBlockEntity.DRY_COMPOST_TICKS; tick++) {
            WoodenComposterBlockEntity.serverTick(helper.getLevel(), composter.getBlockPos(), composter.getBlockState(), composter);
        }
    }

    private static ItemStack output(WoodenComposterBlockEntity composter) {
        return composter.getInventory().getStackInSlot(WoodenComposterBlockEntity.SLOT_OUTPUT);
    }

    private static ItemStack catalyst(WoodenComposterBlockEntity composter) {
        return composter.getInventory().getStackInSlot(WoodenComposterBlockEntity.SLOT_CATALYST);
    }
}
