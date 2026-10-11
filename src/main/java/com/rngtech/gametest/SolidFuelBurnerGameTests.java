package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.blockentity.SolidFuelBurnerVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The world view of a Solid Fuel Burner: installed Gear, fuel, fire, and charge reach the synced visual state. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SolidFuelBurnerGameTests {
    private SolidFuelBurnerGameTests() {
    }

    /** A burning burner shows its Heat Core, Fuel Box, the coal left in the hopper, the fire, and the cell filling up. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void burnerShowsItsGearFuelAndFire(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity burner = burner(helper, new BlockPos(2, 1, 2), SolidFuelBurnerChassis.COPPER,
                HeatCoreMaterial.COPPER, FuelBoxMaterial.COPPER, new ItemStack(Items.COAL, 32));
        helper.runAfterDelay(10, () -> {
            SolidFuelBurnerVisualState state = burner.currentVisualState();
            check(state.burning(), "The burner should be burning: " + state);
            check(state.heatCore().equals("copper") && state.fuelBox().equals("copper"), "Wrong parts shown: " + state);
            check(state.bins() == 1, "A Copper Fuel Box has one bin: " + state);
            check(state.fuel0() == 31.0F / 64.0F, "One of 32 Coal should have gone into the firebox: " + state);
            int tint = state.fuelTint0();
            check((tint >> 16 & 0xFF) < 0x40 && (tint >> 8 & 0xFF) < 0x40 && (tint & 0xFF) < 0x40, "Coal should look dark: " + state);
            check(state.burn() > 0.0F && state.burn() <= 1.0F, "The current Coal should be part burned: " + state);
            check(state.rate() > 0.0F, "A burning burner shows its rate: " + state);
            check(state.charge() > 0.0F && state.charge() < 1.0F, "The cell should be filling: " + state);
            check(SolidFuelBurnerVisualState.load(state.save()).equals(state), "The visual state should survive syncing: " + state);
            helper.succeed();
        });
    }

    /** A Steel Heat Core spends the same Coal faster than an Iron one, so its fire burns visibly hotter. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void strongerHeatCoreBurnsHotter(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity iron = burner(helper, new BlockPos(1, 1, 2), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.IRON, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        SolidFuelBurnerBlockEntity steel = burner(helper, new BlockPos(3, 1, 2), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.STEEL, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        helper.runAfterDelay(10, () -> {
            SolidFuelBurnerVisualState slow = iron.currentVisualState();
            SolidFuelBurnerVisualState fast = steel.currentVisualState();
            check(slow.burning() && fast.burning(), "Both burners should burn: " + slow + ", " + fast);
            check(fast.rate() > slow.rate() * 2.0F, "A Steel Heat Core should burn much hotter than Iron: " + fast.rate() + " vs " + slow.rate());
            check(fast.burn() < slow.burn(), "A Steel Heat Core should spend its Coal faster: " + fast.burn() + " vs " + slow.burn());
            helper.succeed();
        });
    }

    /** With a full cell, a Fuel Governor banks the fire: no flames, and the current fuel item keeps its burn. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void governedBurnerBanksItsFire(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity burner = burner(helper, new BlockPos(2, 1, 2), SolidFuelBurnerChassis.COPPER,
                HeatCoreMaterial.IRON, FuelBoxMaterial.COPPER, new ItemStack(Items.COAL, 8));
        float[] banked = new float[1];
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    check(burner.currentVisualState().burning(), "The burner should be burning before the cell fills");
                    ItemStack full = new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get());
                    int capacity = BatteryCellItem.energyCapacity(full);
                    BatteryCellItem.setEnergy(full, capacity, capacity);
                    burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_BATTERY_CELL, full);
                })
                .thenExecuteAfter(5, () -> {
                    SolidFuelBurnerVisualState state = burner.currentVisualState();
                    check(!state.burning() && state.burn() > 0.0F, "A governed burner should bank its fire: " + state);
                    check(state.charge() == 1.0F && state.chargeSegments() == SolidFuelBurnerVisualState.CHARGE_SEGMENTS, "The meter should read full: " + state);
                    banked[0] = state.burn();
                })
                .thenExecuteAfter(20, () -> {
                    SolidFuelBurnerVisualState state = burner.currentVisualState();
                    check(state.burn() == banked[0], "A banked fire should keep its burn: " + state.burn() + " vs " + banked[0]);
                    check(state.fuel0() == 7.0F / 64.0F, "A banked fire should take no more Coal: " + state);
                })
                .thenSucceed();
    }

    /** A two-slot Fuel Box shows two bins, each with its own fuel; without a cell the meter is dark and nothing burns. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void twoSlotFuelBoxShowsBothBins(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity burner = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.solidFuelBurner(SolidFuelBurnerChassis.ALLOY).get(), SolidFuelBurnerBlockEntity.class);
        burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.BRONZE).get()));
        burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_FUEL_BOX, new ItemStack(ModItems.fuelBox(FuelBoxMaterial.BRONZE).get()));
        burner.getFuelInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_FUEL_0, new ItemStack(Items.COAL, 16));
        burner.getFuelInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_FUEL_1, new ItemStack(Items.OAK_PLANKS, 64));
        helper.runAfterDelay(2, () -> {
            SolidFuelBurnerVisualState state = burner.currentVisualState();
            check(state.bins() == 2 && state.fuelBox().equals("bronze") && state.heatCore().equals("bronze"), "Wrong parts shown: " + state);
            check(state.fuel0() == 0.25F && state.fuel1() == 1.0F, "Each bin should show its own fuel: " + state);
            check(state.fuelTint1() == Blocks.OAK_PLANKS.defaultMapColor().col, "Planks should look like planks: " + state);
            check(state.charge() < 0.0F && state.chargeSegments() == 0, "Without a cell the meter should be dark: " + state);
            check(!state.burning() && state.burn() == 0.0F, "Without a cell nothing burns: " + state);
            helper.succeed();
        });
    }

    private static SolidFuelBurnerBlockEntity burner(
            GameTestHelper helper, BlockPos pos, SolidFuelBurnerChassis chassis, HeatCoreMaterial core, FuelBoxMaterial box, ItemStack fuel
    ) {
        SolidFuelBurnerBlockEntity burner = MachineTestSupport.place(helper, pos, ModBlocks.solidFuelBurner(chassis).get(), SolidFuelBurnerBlockEntity.class);
        burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(core).get()));
        burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_FUEL_BOX, new ItemStack(ModItems.fuelBox(box).get()));
        burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()));
        burner.getFuelInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_FUEL_0, fuel);
        return burner;
    }
}
