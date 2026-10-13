package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Arrays;
import java.util.List;

/** The Even Feed keystone spreads automation across a Furnace's input lanes; without it, lane 1 fills first. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EvenFeedGameTests {
    private static final int TRICKLE = 30;

    private EvenFeedGameTests() {
    }

    /** One item at a time through a slot-order loop, like a cable or hopper: all four lanes end within one item. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void evenFeedSpreadsATrickleAcrossLeadFurnaceLanes(GameTestHelper helper) {
        FurnaceBlockEntity furnace = leadFurnace(helper, true);
        IItemHandler top = furnace.getItemHandler(Direction.UP);
        for (int i = 0; i < TRICKLE; i++) {
            check(feed(top, new ItemStack(Items.POTATO)).isEmpty(), "The furnace refused item " + (i + 1) + "; lanes " + lanes(furnace));
        }
        int[] counts = lanes(furnace);
        check(Arrays.stream(counts).sum() == TRICKLE, "Lanes hold " + Arrays.toString(counts) + ", expected " + TRICKLE + " items");
        check(Arrays.stream(counts).max().orElseThrow() - Arrays.stream(counts).min().orElseThrow() <= 1,
                "Even Feed left uneven lanes " + Arrays.toString(counts));
        helper.succeed();
    }

    /** A full stack offered to the first slot spreads too, and items the furnace cannot smelt are still refused. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void evenFeedSpreadsABulkStackAndKeepsTheFilter(GameTestHelper helper) {
        FurnaceBlockEntity furnace = leadFurnace(helper, true);
        IItemHandler top = furnace.getItemHandler(Direction.UP);
        ItemStack simulated = top.insertItem(0, new ItemStack(Items.POTATO, 64), true);
        check(simulated.isEmpty() && Arrays.stream(lanes(furnace)).sum() == 0, "Simulating changed the lanes or refused items: " + simulated);
        check(top.insertItem(0, new ItemStack(Items.POTATO, 64), false).isEmpty(), "The furnace refused part of a stack");
        check(Arrays.equals(lanes(furnace), new int[] {16, 16, 16, 16}), "A stack landed as " + Arrays.toString(lanes(furnace)));
        ItemStack stick = new ItemStack(Items.STICK);
        check(!top.isItemValid(0, stick) && feed(top, stick).getCount() == 1, "Even Feed accepted an item with no smelting recipe");
        helper.succeed();
    }

    /** A second item takes the emptiest lane that can hold it, never a lane holding a different item. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void evenFeedNeverMixesItemsInALane(GameTestHelper helper) {
        FurnaceBlockEntity furnace = leadFurnace(helper, true);
        IItemHandler top = furnace.getItemHandler(Direction.UP);
        for (int i = 0; i < 6; i++) {
            feed(top, new ItemStack(i % 2 == 0 ? Items.POTATO : Items.BEEF));
        }
        int potatoes = 0, beef = 0;
        for (int lane = 0; lane < furnace.activeProcessingSlots(); lane++) {
            ItemStack stack = furnace.getItemHandler(null).getStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START + lane);
            potatoes += stack.is(Items.POTATO) ? stack.getCount() : 0;
            beef += stack.is(Items.BEEF) ? stack.getCount() : 0;
        }
        check(potatoes == 3 && beef == 3, "Lanes hold " + potatoes + " potatoes and " + beef + " beef, expected 3 of each");
        check(Arrays.stream(lanes(furnace)).allMatch(count -> count > 0), "Two items left a lane empty: " + Arrays.toString(lanes(furnace)));
        helper.succeed();
    }

    /** Without the keystone, automation keeps filling lane 1 first. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void withoutEvenFeedLaneOneFillsFirst(GameTestHelper helper) {
        FurnaceBlockEntity furnace = leadFurnace(helper, false);
        IItemHandler top = furnace.getItemHandler(Direction.UP);
        for (int i = 0; i < TRICKLE; i++) {
            feed(top, new ItemStack(Items.POTATO));
        }
        check(Arrays.equals(lanes(furnace), new int[] {TRICKLE, 0, 0, 0}), "Ordered fill changed: " + Arrays.toString(lanes(furnace)));
        helper.succeed();
    }

    private static FurnaceBlockEntity leadFurnace(GameTestHelper helper, boolean evenFeed) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.LEAD).get(), FurnaceBlockEntity.class);
        if (evenFeed) {
            String start = MachineMasteryFamily.FURNACE.startNodeId();
            var path = MegaPassiveTree.allocationPath(node -> node.id().equals(start), MegaPassiveTree.node("even_feed"))
                    .stream().map(MegaPassiveNode::id).toList();
            check(!path.isEmpty(), "Even Feed is unreachable from the Furnace start");
            furnace.setMachineProgression(new MachineProgressionState(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL), 0,
                    MachineProgressionState.MAX_LEVEL, path, start, List.of(), false));
        }
        check(furnace.activeProcessingSlots() == 4, "A Lead Furnace should have 4 lanes, found " + furnace.activeProcessingSlots());
        return furnace;
    }

    /** Offers the stack to each slot in order until it is gone, the way cables and hoppers insert. */
    private static ItemStack feed(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
        }
        return remaining;
    }

    private static int[] lanes(FurnaceBlockEntity furnace) {
        int[] counts = new int[furnace.activeProcessingSlots()];
        for (int lane = 0; lane < counts.length; lane++) {
            counts[lane] = furnace.getItemHandler(null).getStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START + lane).getCount();
        }
        return counts;
    }
}
