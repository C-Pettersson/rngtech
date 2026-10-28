package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.RNGTechConfig;
import com.rngtech.content.blockentity.MachineInfoSnapshot;
import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.blockentity.SolidFuelBurnerVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.energy.SolidFuelBurnerStoke;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.menu.SolidFuelBurnerMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The world view of a Solid Fuel Burner (installed Gear, fuel, fire, and charge reach the synced visual state) and its
 * stoke, which trades FE/t for FE per fuel item.
 */
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

    /** The minimum stoke runs at a quarter of the Heat Core's FE/t and gets 20% more FE from each Coal. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void lowStokeTradesRateForFuel(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity rated = burner(helper, new BlockPos(1, 1, 2), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.STEEL, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        SolidFuelBurnerBlockEntity banked = burner(helper, new BlockPos(3, 1, 2), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.STEEL, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        banked.setStoke(SolidFuelBurnerStoke.MIN_STOKE);
        helper.runAfterDelay(10, () -> {
            MachineInfoSnapshot full = rated.machineInfo();
            MachineInfoSnapshot low = banked.machineInfo();
            check(low.energyRate() == SolidFuelBurnerStoke.generation((int) full.energyRate(), SolidFuelBurnerStoke.MIN_STOKE),
                    "Minimum stoke should run at a quarter of " + full.energyRate() + " FE/t, ran at " + low.energyRate());
            double ratio = (low.fuelMax() * (double) low.energyRate()) / (full.fuelMax() * (double) full.energyRate());
            check(Math.abs(ratio - 1.2) < 0.02, "Minimum stoke should get 20% more FE per Coal, got x" + ratio);
            check(banked.currentVisualState().rate() < rated.currentVisualState().rate(), "A low stoke should burn with a smaller fire");
            helper.succeed();
        });
    }

    /**
     * The pack's generation multiplier scales FE/t and FE per Coal, and the Coal burns for the same time. The config is
     * set and restored within this tick, so other tests never see it.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void generationMultiplierScalesFePerCoal(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity plain = burner(helper, new BlockPos(1, 1, 2), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.STEEL, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        SolidFuelBurnerBlockEntity scaled = burner(helper, new BlockPos(3, 1, 2), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.STEEL, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        double previous = RNGTechConfig.ENERGY_GENERATION_MULTIPLIER.get();
        MachineInfoSnapshot base;
        MachineInfoSnapshot doubled;
        try {
            RNGTechConfig.ENERGY_GENERATION_MULTIPLIER.set(1.0D);
            tick(helper, plain);
            base = plain.machineInfo();
            RNGTechConfig.ENERGY_GENERATION_MULTIPLIER.set(2.0D);
            tick(helper, scaled);
            doubled = scaled.machineInfo();
        } finally {
            RNGTechConfig.ENERGY_GENERATION_MULTIPLIER.set(previous);
        }
        check(base.energyRate() > 0, "The plain burner should be burning");
        check(doubled.energyRate() == 2 * base.energyRate(), "A 2x multiplier should double " + base.energyRate() + " FE/t, ran at " + doubled.energyRate());
        check(doubled.fuelMax() == base.fuelMax(), "Coal should burn for " + base.fuelMax() + " ticks at any multiplier, burned for " + doubled.fuelMax());
        helper.succeed();
    }

    /**
     * A stage multiplier scales only burners whose Heat Core has that stage, on top of the global multiplier, and the
     * menu reports the combined multiplier to the Stats tab. The config is set and restored within this tick, so other
     * tests never see it.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void stageMultiplierFollowsTheHeatCore(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity plainSteel = burner(helper, new BlockPos(1, 1, 1), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.STEEL, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        SolidFuelBurnerBlockEntity scaledSteel = burner(helper, new BlockPos(3, 1, 1), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.STEEL, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        SolidFuelBurnerBlockEntity plainCopper = burner(helper, new BlockPos(1, 1, 3), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.COPPER, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        SolidFuelBurnerBlockEntity scaledCopper = burner(helper, new BlockPos(3, 1, 3), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.COPPER, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        double previousGlobal = RNGTechConfig.ENERGY_GENERATION_MULTIPLIER.get();
        ModConfigSpec.DoubleValue stageFour = RNGTechConfig.ENERGY_STAGE_GENERATION_MULTIPLIERS.get(3);
        double previousStageFour = stageFour.get();
        MachineInfoSnapshot steelBase;
        MachineInfoSnapshot steelScaled;
        MachineInfoSnapshot copperBase;
        MachineInfoSnapshot copperScaled;
        double steelShown;
        double copperShown;
        try {
            RNGTechConfig.ENERGY_GENERATION_MULTIPLIER.set(1.0D);
            stageFour.set(1.0D);
            tick(helper, plainSteel);
            tick(helper, plainCopper);
            steelBase = plainSteel.machineInfo();
            copperBase = plainCopper.machineInfo();
            RNGTechConfig.ENERGY_GENERATION_MULTIPLIER.set(2.0D);
            stageFour.set(3.0D);
            tick(helper, scaledSteel);
            tick(helper, scaledCopper);
            steelScaled = scaledSteel.machineInfo();
            copperScaled = scaledCopper.machineInfo();
            Inventory inventory = helper.makeMockPlayer(GameType.SURVIVAL).getInventory();
            steelShown = new SolidFuelBurnerMenu(0, inventory, scaledSteel, scaledSteel.getMenuData()).packGenerationMultiplier();
            copperShown = new SolidFuelBurnerMenu(0, inventory, scaledCopper, scaledCopper.getMenuData()).packGenerationMultiplier();
        } finally {
            RNGTechConfig.ENERGY_GENERATION_MULTIPLIER.set(previousGlobal);
            stageFour.set(previousStageFour);
        }
        check(steelBase.energyRate() > 0 && copperBase.energyRate() > 0, "Both plain burners should be burning");
        check(steelScaled.energyRate() == 6 * steelBase.energyRate(),
                "2x global and 3x Stage 4 should make " + steelBase.energyRate() + " FE/t six times as much, ran at " + steelScaled.energyRate());
        check(copperScaled.energyRate() == 2 * copperBase.energyRate(),
                "A Copper Heat Core should get only the 2x global, " + copperBase.energyRate() + " FE/t ran at " + copperScaled.energyRate());
        check(Math.abs(steelShown - 6.0) < 0.001, "The Stats tab should name a 6x pack setting for the Steel Heat Core, got " + steelShown);
        check(Math.abs(copperShown - 2.0) < 0.001, "The Stats tab should name a 2x pack setting for the Copper Heat Core, got " + copperShown);
        helper.succeed();
    }

    /** Turning the stoke down mid-item keeps the item's burned share and revalues what is left at the new economy. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void stokeChangeKeepsTheBurningItemsShare(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity burner = burner(helper, new BlockPos(2, 1, 2), SolidFuelBurnerChassis.STEEL,
                HeatCoreMaterial.STEEL, FuelBoxMaterial.IRON, new ItemStack(Items.COAL, 8));
        helper.runAfterDelay(40, () -> {
            MachineInfoSnapshot before = burner.machineInfo();
            burner.setStoke(50);
            MachineInfoSnapshot after = burner.machineInfo();
            double shareBefore = before.fuel() / (double) before.fuelMax();
            double shareAfter = after.fuel() / (double) after.fuelMax();
            check(Math.abs(shareBefore - shareAfter) < 0.01, "The burned share should stay put: " + shareBefore + " vs " + shareAfter);
            double left = before.fuel() * (double) before.energyRate();
            double revalued = after.fuel() * (double) after.energyRate();
            double expected = left * SolidFuelBurnerStoke.fuelEconomy(50);
            check(Math.abs(revalued - expected) <= 2.0 * before.energyRate(), "The rest of the Coal should be worth " + expected + " FE, was " + revalued);
            helper.succeed();
        });
    }

    /** Stoke clamps to its range, syncs to the world view, and survives a save. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void stokeClampsAndSaves(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity burner = MachineTestSupport.place(helper, new BlockPos(1, 1, 2),
                ModBlocks.solidFuelBurner(SolidFuelBurnerChassis.CRUDE).get(), SolidFuelBurnerBlockEntity.class);
        check(burner.stoke() == SolidFuelBurnerStoke.RATED_STOKE, "A new burner should start at the rated stoke");
        burner.setStoke(5);
        check(burner.stoke() == SolidFuelBurnerStoke.MIN_STOKE, "Stoke should clamp to its minimum, was " + burner.stoke());
        burner.setStoke(500);
        check(burner.stoke() == SolidFuelBurnerStoke.MAX_STOKE, "Stoke should clamp to its maximum, was " + burner.stoke());
        burner.setStoke(60);
        check(burner.currentVisualState().stoke() == 60, "The world view should show the stoke");
        CompoundTag saved = burner.saveWithoutMetadata(helper.getLevel().registryAccess());
        SolidFuelBurnerBlockEntity copy = MachineTestSupport.place(helper, new BlockPos(3, 1, 2),
                ModBlocks.solidFuelBurner(SolidFuelBurnerChassis.CRUDE).get(), SolidFuelBurnerBlockEntity.class);
        copy.loadWithComponents(saved, helper.getLevel().registryAccess());
        check(copy.stoke() == 60, "Stoke should survive a save, was " + copy.stoke());
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, SolidFuelBurnerBlockEntity burner) {
        SolidFuelBurnerBlockEntity.serverTick(helper.getLevel(), burner.getBlockPos(), burner.getBlockState(), burner);
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
