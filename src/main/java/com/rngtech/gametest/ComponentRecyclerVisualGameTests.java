package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.blockentity.ComponentRecyclerVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.RecoveryFilterMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The world view of a Component Recycler: its claw, filter, and cell, the item it takes apart, and its returns. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ComponentRecyclerVisualGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private ComponentRecyclerVisualGameTests() {
    }

    /** A bare Recycler shows only its spindle, an empty cage and cradle, and empty trays. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void bareRecyclerShowsNoGear(GameTestHelper helper) {
        ComponentRecyclerBlockEntity recycler = recycler(helper, ComponentRecyclerChassis.IRON);
        helper.runAfterDelay(2, () -> {
            ComponentRecyclerVisualState state = recycler.currentVisualState();
            check(state.head().isEmpty() && state.filter().isEmpty() && state.cell().isEmpty() && state.charge() < 0.0F,
                    "A bare Recycler shows no Gear: " + state);
            check(state.input().isEmpty() && state.outputs().stream().allMatch(ItemStack::isEmpty) && !state.running(),
                    "A bare Recycler shows no work: " + state);
            helper.succeed();
        });
    }

    /** A Recycler running on its cell shows its head, filter, and cell, the item on the turntable, and its returns. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void runningRecyclerShowsGearAndReturns(GameTestHelper helper) {
        ComponentRecyclerBlockEntity recycler = recycler(helper, ComponentRecyclerChassis.STEEL);
        recycler.getGearInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD,
                new ItemStack(ModItems.disassemblyHead(DisassemblyHeadMaterial.STEEL).get()));
        recycler.getGearInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_RECOVERY_FILTER,
                new ItemStack(ModItems.recoveryFilter(RecoveryFilterMaterial.BRONZE).get()));
        recycler.getGearInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_BATTERY_CELL, chargedCell(BatteryCellMaterial.IRON));
        recycler.getProcessInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_INPUT, new ItemStack(brokenCircuit(), 16));
        helper.succeedWhen(() -> {
            ComponentRecyclerVisualState state = recycler.currentVisualState();
            check(state.outputs().get(0).is(Items.REDSTONE) && !state.outputs().get(1).isEmpty(),
                    "Waiting for the first returns in two trays: " + state);
            check(state.outputs().get(2).isEmpty(), "Only two trays should fill: " + state);
            check(state.head().equals("steel") && state.filter().equals("bronze"), "The head and filter should show: " + state);
            check(state.cell().equals("iron") && state.charge() > 0.0F, "The charged Iron cell should show: " + state);
            check(state.input().is(brokenCircuit()) && state.input().getCount() == 1, "One input should lie on the turntable: " + state);
            check(state.running() && state.craftTicks() > 0, "The claw should turn: " + state);
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            check(ComponentRecyclerVisualState.load(state.save(registries), registries).key().equals(state.key()),
                    "The visual state should survive syncing: " + state);
        });
    }

    /** The Crude Recycler shows its input on the runner stone and its returns, and runs while its Hand Crank turns. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void crudeRecyclerTurnsWithItsCrank(GameTestHelper helper) {
        ComponentRecyclerBlockEntity recycler = recycler(helper, ComponentRecyclerChassis.CRUDE);
        helper.setBlock(POS.above(), ModBlocks.HAND_CRANK.get().defaultBlockState());
        Item flintHead = BuiltInRegistries.ITEM.get(RNGTech.id("flint_crush_head"));
        recycler.getProcessInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_INPUT, new ItemStack(flintHead, 2));
        boolean[] seenRunning = new boolean[1];
        helper.onEachTick(() -> {
            recycler.turnManualCrank(helper.absolutePos(POS.above()));
            ComponentRecyclerVisualState state = recycler.currentVisualState();
            seenRunning[0] |= state.running() && state.input().is(flintHead);
        });
        helper.succeedWhen(() -> {
            ComponentRecyclerVisualState state = recycler.currentVisualState();
            check(state.outputs().get(0).is(Items.FLINT), "Waiting for the first returns: " + state);
            check(seenRunning[0], "The runner should turn with the input on it while the crank turns");
            check(state.cranks() > 0, "Each crank should turn the runner a step: " + state);
            check(state.head().isEmpty() && state.filter().isEmpty() && state.cell().isEmpty(), "The Crude Recycler has no Gear: " + state);
        });
    }

    /** A loaded Crude Recycler stands still until its Hand Crank turns. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void crudeRecyclerWaitsForItsCrank(GameTestHelper helper) {
        ComponentRecyclerBlockEntity recycler = recycler(helper, ComponentRecyclerChassis.CRUDE);
        helper.setBlock(POS.above(), ModBlocks.HAND_CRANK.get().defaultBlockState());
        recycler.getProcessInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_INPUT,
                new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("flint_crush_head"))));
        helper.runAfterDelay(10, () -> {
            ComponentRecyclerVisualState state = recycler.currentVisualState();
            check(!state.running() && state.cranks() == 0, "An uncranked Crude Recycler should stand still: " + state);
            helper.succeed();
        });
    }

    private static ComponentRecyclerBlockEntity recycler(GameTestHelper helper, ComponentRecyclerChassis chassis) {
        return MachineTestSupport.place(helper, POS, ModBlocks.componentRecycler(chassis).get(), ComponentRecyclerBlockEntity.class);
    }

    private static Item brokenCircuit() {
        return BuiltInRegistries.ITEM.get(RNGTech.id("broken_circuit"));
    }

    private static ItemStack chargedCell(BatteryCellMaterial material) {
        ItemStack cell = new ItemStack(ModItems.batteryCell(material).get());
        int capacity = BatteryCellItem.energyCapacity(cell);
        BatteryCellItem.setEnergy(cell, capacity, capacity);
        return cell;
    }
}
