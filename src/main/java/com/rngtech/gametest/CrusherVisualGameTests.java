package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.CrusherVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The world view of a Crusher: its Crush Head rolls, its Battery Cell, its feed and product, and whether it runs or jams. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrusherVisualGameTests {
    private CrusherVisualGameTests() {
    }

    /** A bare Crusher shows only its shafts and an empty cradle. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void bareCrusherShowsNoGear(GameTestHelper helper) {
        CrusherBlockEntity crusher = crusher(helper, CrusherChassisMaterial.WOODEN);
        helper.runAfterDelay(2, () -> {
            CrusherVisualState state = crusher.currentVisualState();
            check(state.head().isEmpty() && state.cell().isEmpty() && state.charge() < 0.0F, "A bare Crusher shows no Gear: " + state);
            check(state.inputPile() == 0 && state.outputPile() == 0 && !state.running(), "A bare Crusher shows no work: " + state);
            helper.succeed();
        });
    }

    /** A Crusher running on its cell shows its head, its cell's charge, a full hopper, and its product, and turns its rolls. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void runningCrusherShowsGearAndWork(GameTestHelper helper) {
        CrusherBlockEntity crusher = crusher(helper, CrusherChassisMaterial.STEEL);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.STEEL).get()));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_FUEL, chargedCell(BatteryCellMaterial.IRON));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.RAW_IRON, 64));
        helper.succeedWhen(() -> {
            CrusherVisualState state = crusher.currentVisualState();
            check(state.outputPile() > 0, "Waiting for the first craft: " + state);
            check(state.head().equals("steel"), "The Steel head should show as rolls: " + state);
            check(state.cell().equals("iron") && state.charge() > 0.0F, "The charged Iron cell should show: " + state);
            check(state.input().is(Items.RAW_IRON) && state.inputPile() == CrusherVisualState.PILE, "A full hopper should show: " + state);
            check(state.running() && state.craftTicks() > 0, "The rolls should turn: " + state);
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            check(CrusherVisualState.load(state.save(registries), registries).key().equals(state.key()),
                    "The visual state should survive syncing: " + state);
        });
    }

    /** A jammed Crusher stops its rolls and shows the jam. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void jammedCrusherStopsItsRolls(GameTestHelper helper) {
        CrusherBlockEntity crusher = crusher(helper, CrusherChassisMaterial.IRON);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.IRON_ORE, 4));
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag tag = crusher.saveWithoutMetadata(registries);
        tag.putInt("JawJamTicks", 100);
        crusher.loadWithComponents(tag, registries);
        MachineTestSupport.powerEachTick(helper, crusher.getEnergyStorage(null));
        helper.runAfterDelay(5, () -> {
            CrusherVisualState state = crusher.currentVisualState();
            check(state.jammed() && !state.running() && state.craftTicks() == 0, "A jam should stop the rolls: " + state);
            check(state.head().equals("iron") && state.input().is(Items.IRON_ORE), "A jam keeps the head and the feed in view: " + state);
            helper.succeed();
        });
    }

    private static CrusherBlockEntity crusher(GameTestHelper helper, CrusherChassisMaterial material) {
        return MachineTestSupport.place(helper, new BlockPos(2, 1, 2), ModBlocks.crusherChassis(material).get(), CrusherBlockEntity.class);
    }

    private static ItemStack chargedCell(BatteryCellMaterial material) {
        ItemStack cell = new ItemStack(ModItems.batteryCell(material).get());
        int capacity = BatteryCellItem.energyCapacity(cell);
        BatteryCellItem.setEnergy(cell, capacity, capacity);
        return cell;
    }
}
