package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.BatteryChassisBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.SolarArrayControllerBlockEntity;
import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Installed Battery Cells keep their stats until the cell changes, and the holder sees a change on the next tick. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatteryCellCacheGameTests {
    private static final int STEADY_TICKS = 100;
    /** The chassis menu values before the connector caps and last-tick telemetry, which a fresh chassis lacks. */
    private static final int CELL_DERIVED_MENU_VALUES = 16;
    private static final MachineTraits MORE_CAPACITY = new MachineTraits(Rarity.MAGIC, 0, List.of(new MachineModifier(
            ModifierSlot.PREFIX, MachineStat.ENERGY_CAPACITY, ModifierOperation.INCREASED_PERCENT, 50.0)));

    private BatteryCellCacheGameTests() {
    }

    /** A chassis exporting to a running Crusher, read every tick like an open menu, builds its cells' stats once. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = STEADY_TICKS + 100)
    public static void steadyBatteryChassisBuildsNoCellStats(GameTestHelper helper) {
        BlockPos chassisPos = new BlockPos(2, 1, 2);
        BatteryChassisBlockEntity chassis = chassis(helper, chassisPos);
        chassis.getInventory().setStackInSlot(0, chargedCell(BatteryCellMaterial.IRON));
        chassis.getInventory().setStackInSlot(1, chargedCell(BatteryCellMaterial.IRON));
        CrusherBlockEntity sink = runningCrusher(helper, chassisPos.east());
        IEnergyStorage storage = chassis.getEnergyStorage(null);
        int[] builds = new int[1];
        int[] energy = new int[1];
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    builds[0] = chassis.cellStatsBuilds();
                    energy[0] = storage.getEnergyStored();
                    check(builds[0] == 2, "Built cell stats " + builds[0] + " times for two cells");
                })
                .thenExecuteFor(STEADY_TICKS, () -> {
                    readLikeAnOpenMenu(chassis);
                    check(chassis.cellStatsBuilds() == builds[0],
                            "Rebuilt cell stats at tick " + helper.getTick() + " with no cell changed");
                })
                .thenExecute(() -> {
                    check(storage.getEnergyStored() < energy[0], "The chassis exported nothing to the Crusher");
                    check(sink.machineInfo().energy() > 0, "The Crusher got no power from the chassis");
                })
                .thenSucceed();
    }

    /** Refining a cell in its slot or swapping it shows on the next tick, matching a chassis that builds it fresh. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void refiningOrSwappingACellUpdatesTheChassisNextTick(GameTestHelper helper) {
        BatteryChassisBlockEntity chassis = chassis(helper, new BlockPos(1, 1, 1));
        chassis.getInventory().setStackInSlot(0, chargedCell(BatteryCellMaterial.IRON));
        IEnergyStorage storage = chassis.getEnergyStorage(null);
        int[] before = new int[2];
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    before[0] = storage.getMaxEnergyStored();
                    before[1] = chassis.cellStatsBuilds();
                    chassis.getInventory().getStackInSlot(0).set(ModDataComponents.MACHINE_TRAITS.get(), MORE_CAPACITY);
                })
                .thenExecuteAfter(1, () -> {
                    check(storage.getMaxEnergyStored() > before[0], "A refined cell kept capacity " + before[0]);
                    check(chassis.cellStatsBuilds() == before[1] + 1, "A refined cell was not rebuilt exactly once");
                    assertMatchesFreshChassis(helper, chassis, new BlockPos(3, 1, 1), "after refining");
                    before[0] = storage.getMaxEnergyStored();
                    chassis.getInventory().setStackInSlot(0, chargedCell(BatteryCellMaterial.LEAD));
                })
                .thenExecuteAfter(1, () -> {
                    check(storage.getMaxEnergyStored() != before[0], "A swapped cell kept capacity " + before[0]);
                    assertMatchesFreshChassis(helper, chassis, new BlockPos(3, 1, 3), "after swapping");
                })
                .thenSucceed();
    }

    /** A Solar Array Controller exporting from its cell builds the cell's stats once and follows a refined cell. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = STEADY_TICKS + 100)
    public static void solarControllerCellStatsFollowTheCell(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(2, 1, 2);
        SolarArrayControllerBlockEntity controller = MachineTestSupport.place(
                helper, controllerPos, ModBlocks.SOLAR_ARRAY_CONTROLLER.get(), SolarArrayControllerBlockEntity.class);
        ItemStack cell = chargedCell(BatteryCellMaterial.IRON);
        int cellCapacity = BatteryCellItem.energyCapacity(cell);
        controller.getGearInventory().setStackInSlot(SolarArrayControllerBlockEntity.SLOT_BATTERY_CELL, cell);
        runningCrusher(helper, controllerPos.east());
        IEnergyStorage storage = controller.getEnergyStorage(null);
        int[] builds = new int[1];
        int[] values = new int[2];
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    builds[0] = controller.cellStatsBuilds();
                    values[0] = storage.getEnergyStored();
                    values[1] = storage.getMaxEnergyStored();
                    check(builds[0] == 1, "Built cell stats " + builds[0] + " times for one cell");
                })
                .thenExecuteFor(STEADY_TICKS, () -> {
                    for (int index = 0; index < controller.getMenuData().getCount(); index++) {
                        controller.getMenuData().get(index);
                    }
                    check(controller.cellStatsBuilds() == builds[0],
                            "Rebuilt cell stats at tick " + helper.getTick() + " with no cell changed");
                })
                .thenExecute(() -> {
                    check(storage.getEnergyStored() < values[0], "The controller exported nothing from its cell");
                    controller.getGearInventory().getStackInSlot(SolarArrayControllerBlockEntity.SLOT_BATTERY_CELL)
                            .set(ModDataComponents.MACHINE_TRAITS.get(), MORE_CAPACITY);
                })
                .thenExecuteAfter(1, () -> {
                    ItemStack installed = controller.getGearInventory().getStackInSlot(SolarArrayControllerBlockEntity.SLOT_BATTERY_CELL);
                    int refinedCapacity = BatteryCellItem.energyCapacity(installed);
                    check(refinedCapacity > cellCapacity, "The refinement did not raise the cell's capacity");
                    check(storage.getMaxEnergyStored() == values[1] - cellCapacity + refinedCapacity,
                            "Controller capacity " + storage.getMaxEnergyStored() + " missed the refined cell's " + refinedCapacity);
                    check(controller.cellStatsBuilds() == builds[0] + 1, "A refined cell was not rebuilt exactly once");
                })
                .thenSucceed();
    }

    /** A Crusher running from its cell, read every tick like an open menu, builds the cell's stats once. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = STEADY_TICKS + 100)
    public static void steadyCrusherBuildsNoCellStats(GameTestHelper helper) {
        CrusherBlockEntity crusher = runningCrusher(helper, new BlockPos(2, 1, 2));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_FUEL, chargedCell(BatteryCellMaterial.IRON));
        IEnergyStorage storage = crusher.getEnergyStorage(null);
        int[] builds = new int[1];
        int[] energy = new int[1];
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    builds[0] = crusher.cellStatsBuilds();
                    energy[0] = storage.getEnergyStored();
                    check(builds[0] == 1, "Built cell stats " + builds[0] + " times for one cell");
                })
                .thenExecuteFor(STEADY_TICKS, () -> {
                    readLikeAnOpenMenu(crusher.getMenuData());
                    crusher.machineInfo();
                    check(crusher.cellStatsBuilds() == builds[0],
                            "Rebuilt cell stats at tick " + helper.getTick() + " with no cell changed");
                })
                .thenExecute(() -> check(storage.getEnergyStored() < energy[0], "The Crusher drew nothing from its cell"))
                .thenSucceed();
    }

    /** Refining, emptying, or swapping a Crusher's cell shows in its capacity on the next tick. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void crusherFollowsItsCellNextTick(GameTestHelper helper) {
        CrusherBlockEntity crusher = runningCrusher(helper, new BlockPos(1, 1, 1));
        ItemStackHandler inventory = crusher.getInventory();
        followsTheCell(helper, crusher.getEnergyStorage(null), crusher::cellStatsBuilds,
                cell -> inventory.setStackInSlot(CrusherBlockEntity.SLOT_FUEL, cell),
                () -> inventory.getStackInSlot(CrusherBlockEntity.SLOT_FUEL));
    }

    /** A Solid Fuel Burner charging its cell and exporting to a Crusher, read like an open menu, builds the cell's stats once. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = STEADY_TICKS + 100)
    public static void steadySolidFuelBurnerBuildsNoCellStats(GameTestHelper helper) {
        BlockPos burnerPos = new BlockPos(2, 1, 2);
        SolidFuelBurnerBlockEntity burner = burningBurner(helper, burnerPos);
        burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_BATTERY_CELL, chargedCell(BatteryCellMaterial.IRON));
        CrusherBlockEntity sink = runningCrusher(helper, burnerPos.east());
        int[] builds = new int[1];
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    builds[0] = burner.cellStatsBuilds();
                    check(builds[0] == 1, "Built cell stats " + builds[0] + " times for one cell");
                })
                .thenExecuteFor(STEADY_TICKS, () -> {
                    readLikeAnOpenMenu(burner.getMenuData());
                    burner.machineInfo();
                    check(burner.cellStatsBuilds() == builds[0],
                            "Rebuilt cell stats at tick " + helper.getTick() + " with no cell changed");
                })
                .thenExecute(() -> check(sink.machineInfo().energy() > 0, "The Crusher got no power from the burner"))
                .thenSucceed();
    }

    /** Refining, emptying, or swapping a Solid Fuel Burner's cell shows in its capacity on the next tick. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void solidFuelBurnerFollowsItsCellNextTick(GameTestHelper helper) {
        SolidFuelBurnerBlockEntity burner = burningBurner(helper, new BlockPos(1, 1, 1));
        ItemStackHandler gear = burner.getGearInventory();
        followsTheCell(helper, burner.getEnergyStorage(null), burner::cellStatsBuilds,
                cell -> gear.setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_BATTERY_CELL, cell),
                () -> gear.getStackInSlot(SolidFuelBurnerBlockEntity.SLOT_BATTERY_CELL));
    }

    private static BatteryChassisBlockEntity chassis(GameTestHelper helper, BlockPos pos) {
        return MachineTestSupport.place(helper, pos, ModBlocks.batteryChassis(BatteryChassisMaterial.COPPER).get(), BatteryChassisBlockEntity.class);
    }

    private static ItemStack chargedCell(BatteryCellMaterial material) {
        ItemStack cell = new ItemStack(ModItems.batteryCell(material).get());
        int capacity = BatteryCellItem.energyCapacity(cell);
        BatteryCellItem.setEnergy(cell, capacity / 2, capacity);
        return cell;
    }

    private static CrusherBlockEntity runningCrusher(GameTestHelper helper, BlockPos pos) {
        CrusherBlockEntity crusher = MachineTestSupport.place(
                helper, pos, ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.RAW_IRON, 64));
        return crusher;
    }

    /**
     * Installs an iron cell, then refines it in place, empties the slot, and installs a copper cell, checking a tick
     * after each that the holder's capacity is its own plus the installed cell's and that only a changed cell was built.
     */
    private static void followsTheCell(
            GameTestHelper helper,
            IEnergyStorage storage,
            IntSupplier builds,
            Consumer<ItemStack> install,
            Supplier<ItemStack> installed
    ) {
        install.accept(chargedCell(BatteryCellMaterial.IRON));
        int ironCapacity = BatteryCellItem.energyCapacity(installed.get());
        int[] own = new int[1];
        int[] built = new int[1];
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    own[0] = storage.getMaxEnergyStored() - ironCapacity;
                    built[0] = builds.getAsInt();
                    installed.get().set(ModDataComponents.MACHINE_TRAITS.get(), MORE_CAPACITY);
                })
                .thenExecuteAfter(1, () -> {
                    int refinedCapacity = BatteryCellItem.energyCapacity(installed.get());
                    check(refinedCapacity > ironCapacity, "The refinement did not raise the cell's capacity");
                    check(storage.getMaxEnergyStored() == own[0] + refinedCapacity,
                            "Capacity " + storage.getMaxEnergyStored() + " missed the refined cell's " + refinedCapacity);
                    check(builds.getAsInt() == built[0] + 1, "A refined cell was not rebuilt exactly once");
                    install.accept(ItemStack.EMPTY);
                })
                .thenExecuteAfter(1, () -> {
                    check(storage.getMaxEnergyStored() == own[0], "An emptied slot kept capacity " + storage.getMaxEnergyStored());
                    install.accept(chargedCell(BatteryCellMaterial.COPPER));
                })
                .thenExecuteAfter(1, () -> {
                    int copperCapacity = BatteryCellItem.energyCapacity(installed.get());
                    check(storage.getMaxEnergyStored() == own[0] + copperCapacity,
                            "Capacity " + storage.getMaxEnergyStored() + " missed the swapped cell's " + copperCapacity);
                    check(builds.getAsInt() == built[0] + 2, "The swapped cell was not built exactly once");
                })
                .thenSucceed();
    }

    private static SolidFuelBurnerBlockEntity burningBurner(GameTestHelper helper, BlockPos pos) {
        SolidFuelBurnerBlockEntity burner = MachineTestSupport.place(
                helper, pos, ModBlocks.solidFuelBurner(SolidFuelBurnerChassis.COPPER).get(), SolidFuelBurnerBlockEntity.class);
        burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.IRON).get()));
        burner.getGearInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_FUEL_BOX, new ItemStack(ModItems.fuelBox(FuelBoxMaterial.IRON).get()));
        burner.getFuelInventory().setStackInSlot(SolidFuelBurnerBlockEntity.SLOT_FUEL_0, new ItemStack(Items.COAL, 64));
        return burner;
    }

    private static void readLikeAnOpenMenu(ContainerData data) {
        for (int index = 0; index < data.getCount(); index++) {
            data.get(index);
        }
    }

    private static void readLikeAnOpenMenu(BatteryChassisBlockEntity chassis) {
        for (int index = 0; index < chassis.getMenuData().getCount(); index++) {
            chassis.getMenuData().get(index);
        }
        chassis.machineInfo();
    }

    /** A second chassis given a copy of the same cells reports the same capacity and rates from its first build. */
    private static void assertMatchesFreshChassis(GameTestHelper helper, BatteryChassisBlockEntity chassis, BlockPos freshPos, String when) {
        BatteryChassisBlockEntity fresh = chassis(helper, freshPos);
        for (int slot = 0; slot < chassis.getInventory().getSlots(); slot++) {
            fresh.getInventory().setStackInSlot(slot, chassis.getInventory().getStackInSlot(slot).copy());
        }
        for (int index = 0; index < CELL_DERIVED_MENU_VALUES; index++) {
            int cached = chassis.getMenuData().get(index);
            int built = fresh.getMenuData().get(index);
            check(cached == built, "Menu value " + index + " was " + cached + " but a fresh chassis gives " + built + " " + when);
        }
    }
}
