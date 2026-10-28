package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.SilicaGelColumnCasingBlock;
import com.rngtech.content.blockentity.SilicaGelDehumidifierBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Arrays;

/** Silica Gel Dehumidifier automation: every face and both casings feed the lanes evenly, and a working column shows it. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SilicaGelDehumidifierGameTests {
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);
    private static final int TIMEOUT = 100;

    private SilicaGelDehumidifierGameTests() {
    }

    /** One bead at a time through the upper casing, the way a hopper or cable inserts, fills all three lanes. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void casingTrickleSpreadsAcrossLanes(GameTestHelper helper) {
        SilicaGelDehumidifierBlockEntity dehumidifier = column(helper);
        helper.runAfterDelay(2, () -> {
            IItemHandler casing = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(CONTROLLER.above(2)), Direction.UP);
            check(casing != null, "The formed column's upper casing should take items");
            for (int i = 0; i < 20; i++) {
                check(feed(casing, beads(1)).isEmpty(), "The casing refused bead " + (i + 1) + "; lanes " + Arrays.toString(lanes(dehumidifier)));
            }
            int[] counts = lanes(dehumidifier);
            check(Arrays.stream(counts).sum() == 20 && Arrays.stream(counts).max().orElseThrow() - Arrays.stream(counts).min().orElseThrow() <= 1,
                    "A trickle left uneven lanes " + Arrays.toString(counts));
            helper.succeed();
        });
    }

    /** A stack offered to a side face spreads, a second bead type takes its own lane, and only saturated beads come out. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void sideFaceSpreadsAStackAndKeepsLanesApart(GameTestHelper helper) {
        SilicaGelDehumidifierBlockEntity dehumidifier = column(helper);
        IItemHandler side = dehumidifier.getItemHandler(Direction.NORTH);
        check(side.insertItem(0, beads(64), true).isEmpty() && Arrays.stream(lanes(dehumidifier)).sum() == 0,
                "Simulating changed the lanes or refused beads");
        check(side.insertItem(0, new ItemStack(Items.STICK), false).getCount() == 1, "The dehumidifier took an item that is not a dry bead");
        check(side.insertItem(0, beads(60), false).isEmpty(), "The side refused part of a stack");
        check(Arrays.equals(lanes(dehumidifier), new int[] {20, 20, 20}), "A stack landed as " + Arrays.toString(lanes(dehumidifier)));
        check(side.extractItem(0, 1, false).isEmpty(), "Automation pulled dry beads out of a lane");

        dehumidifier.getInventory().setStackInSlot(SilicaGelDehumidifierBlockEntity.SLOT_DRY_START + 2, ItemStack.EMPTY);
        check(feed(side, new ItemStack(ModItems.SUPERCHARGED_SILICA_GEL_BEADS.get(), 4)).isEmpty(), "The side refused supercharged beads");
        ItemStack third = dehumidifier.getInventory().getStackInSlot(SilicaGelDehumidifierBlockEntity.SLOT_DRY_START + 2);
        check(third.is(ModItems.SUPERCHARGED_SILICA_GEL_BEADS.get()) && third.getCount() == 4,
                "Supercharged beads should fill the empty lane alone, found " + third);

        dehumidifier.getInventory().setStackInSlot(SilicaGelDehumidifierBlockEntity.SLOT_SATURATED_START,
                new ItemStack(ModItems.SATURATED_SILICA_GEL_BEADS.get(), 3));
        ItemStack pulled = side.extractItem(SilicaGelDehumidifierBlockEntity.SLOT_SATURATED_START, 64, false);
        check(pulled.is(ModItems.SATURATED_SILICA_GEL_BEADS.get()) && pulled.getCount() == 3, "Automation should pull saturated beads, got " + pulled);
        helper.succeed();
    }

    /** The casings show the working state, and drop it again when the lanes run dry. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void workingColumnLightsItsCasings(GameTestHelper helper) {
        SilicaGelDehumidifierBlockEntity dehumidifier = column(helper);
        dehumidifier.insertDryInput(beads(3), false);
        helper.runAfterDelay(5, () -> {
            check(casingsActive(helper), "A working column should show it on both casings");
            for (int lane = 0; lane < SilicaGelDehumidifierBlockEntity.LANE_COUNT; lane++) {
                dehumidifier.getInventory().setStackInSlot(SilicaGelDehumidifierBlockEntity.SLOT_DRY_START + lane, ItemStack.EMPTY);
            }
            helper.runAfterDelay(5, () -> {
                check(!casingsActive(helper), "An idle column should go dark");
                helper.succeed();
            });
        });
    }

    private static SilicaGelDehumidifierBlockEntity column(GameTestHelper helper) {
        SilicaGelDehumidifierBlockEntity dehumidifier = MachineTestSupport.place(helper, CONTROLLER,
                ModBlocks.SILICA_GEL_DEHUMIDIFIER.get(), SilicaGelDehumidifierBlockEntity.class);
        helper.setBlock(CONTROLLER.above(1), ModBlocks.SILICA_GEL_COLUMN_CASING.get());
        helper.setBlock(CONTROLLER.above(2), ModBlocks.SILICA_GEL_COLUMN_CASING.get());
        return dehumidifier;
    }

    private static boolean casingsActive(GameTestHelper helper) {
        return helper.getBlockState(CONTROLLER.above(1)).getValue(SilicaGelColumnCasingBlock.ACTIVE)
                && helper.getBlockState(CONTROLLER.above(2)).getValue(SilicaGelColumnCasingBlock.ACTIVE);
    }

    private static ItemStack beads(int count) {
        return new ItemStack(ModItems.SILICA_GEL_BEADS.get(), count);
    }

    /** Offers the stack to each slot in order until it is gone, the way cables and hoppers insert. */
    private static ItemStack feed(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
        }
        return remaining;
    }

    private static int[] lanes(SilicaGelDehumidifierBlockEntity dehumidifier) {
        int[] counts = new int[SilicaGelDehumidifierBlockEntity.LANE_COUNT];
        for (int lane = 0; lane < counts.length; lane++) {
            counts[lane] = dehumidifier.getInventory().getStackInSlot(SilicaGelDehumidifierBlockEntity.SLOT_DRY_START + lane).getCount();
        }
        return counts;
    }
}
