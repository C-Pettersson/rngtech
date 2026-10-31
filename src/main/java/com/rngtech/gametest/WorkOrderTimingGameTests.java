package com.rngtech.gametest;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.blockentity.MachineInfoProvider;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderFamilies;
import com.rngtech.rpg.workorder.WorkPart;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The good-build speed table's measured rows: a bare build of each family's best chassis at the tier's stage, with
 * matching plain Gear and a Battery Cell, runs against an Any-work chip that never finishes. Its counted speed times the
 * good-to-bare ratio must stay within {@link #TOLERANCE} of {@code WorkOrderFamilies.JOB_SPEED}, so a stat change that
 * makes jobs too short or too long fails here. Each run also logs a {@code WO-TIMING} line for retuning.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkOrderTimingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "wo_timing";
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final int WARMUP = 1200;
    private static final int END = 7200;
    private static final int TIMEOUT = 8000;
    private static final double HEATED = 1.5;
    private static final double UNHEATED = 4.0;
    private static final double TOLERANCE = 0.15;

    private WorkOrderTimingGameTests() {
    }

    // Alloy Furnace: Steel is the best chassis at Stage 4 and 5 (there is no Stage 5 chassis), Titanium at Stage 6.

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void alloySteel(GameTestHelper helper) {
        alloy(helper, AlloyFurnaceChassisMaterial.STEEL, HeatCoreMaterial.STEEL, AlloyCrucibleMaterial.STEEL, BatteryCellMaterial.INVAR, "invar",
                new int[] {1, 2}, feed(0, Items.IRON_INGOT), feed(1, item("nickel_ingot")));
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void alloyTitanium(GameTestHelper helper) {
        alloy(helper, AlloyFurnaceChassisMaterial.TITANIUM, HeatCoreMaterial.TITANIUM, AlloyCrucibleMaterial.TITANIUM, BatteryCellMaterial.ARCLITE,
                "aethergold", new int[] {3, 4, 5}, feed(0, item("tin_ingot")), feed(1, item("silver_ingot")), feed(2, Items.GLOWSTONE_DUST));
    }

    // Metal Press: one Stage 4 chassis for every tier, with the Stage 4 Heat Core.

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void pressSteel(GameTestHelper helper) {
        press(helper, HeatCoreMaterial.STEEL, BatteryCellMaterial.INVAR, "iron_plate", Items.IRON_INGOT);
    }

    // Resonance Calibrator: Lead (2 lanes) is the best at Stage 4 and 5, Titanium at 6, and Nullite, with its Coil, at 7.

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void calibratorLead(GameTestHelper helper) {
        calibrator(helper, ResonanceCalibratorChassis.LEAD, CalibrationGearMaterial.STEEL, BatteryCellMaterial.INVAR, 4, new int[] {1, 2});
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void calibratorTitanium(GameTestHelper helper) {
        calibrator(helper, ResonanceCalibratorChassis.TITANIUM, CalibrationGearMaterial.TITANIUM, BatteryCellMaterial.ARCLITE, 6, new int[] {3});
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void calibratorNullite(GameTestHelper helper) {
        calibrator(helper, ResonanceCalibratorChassis.NULLITE, CalibrationGearMaterial.NULLITE, BatteryCellMaterial.NULLITE, 7, new int[] {4, 5});
    }

    // Component Assembler: stageless, one setup.

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void assembler(GameTestHelper helper) {
        assembler(helper, BatteryCellMaterial.INVAR);
    }

    // Component Recycler: Steel at 4, Aluminum at 5, Titanium at 6 and 7 (it beats Tungstensteel), Exotic at 8. It has no
    // Battery Cell slot that changes its speed.

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void recyclerSteel(GameTestHelper helper) {
        recycler(helper, ComponentRecyclerChassis.STEEL, DisassemblyHeadMaterial.STEEL, "metal_press", 4, new int[] {1});
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void recyclerAluminum(GameTestHelper helper) {
        recycler(helper, ComponentRecyclerChassis.ALUMINUM, DisassemblyHeadMaterial.ALUMINUM, "aluminum_crusher_chassis", 5, new int[] {2});
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void recyclerTitanium(GameTestHelper helper) {
        recycler(helper, ComponentRecyclerChassis.TITANIUM, DisassemblyHeadMaterial.TITANIUM, "melter", 6, new int[] {3, 4});
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT, batch = BATCH)
    public static void recyclerExotic(GameTestHelper helper) {
        recycler(helper, ComponentRecyclerChassis.EXOTIC, DisassemblyHeadMaterial.EXOTIC, "exotic_crusher_chassis", 8, new int[] {5});
    }

    private static void alloy(GameTestHelper helper, AlloyFurnaceChassisMaterial chassis, HeatCoreMaterial core, AlloyCrucibleMaterial crucible,
            BatteryCellMaterial cell, String recipe, int[] tiers, Feed... feeds) {
        AlloyFurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE, ModBlocks.alloyFurnaceChassis(chassis).get(), AlloyFurnaceBlockEntity.class);
        ItemStackHandler gear = furnace.getGearInventory();
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(core).get()));
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_CRUCIBLE, new ItemStack(ModItems.alloyCrucible(crucible).get()));
        if (cell != null) { gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(cell).get())); }
        String gearText = "core:" + name(core) + ",crucible:" + name(crucible) + cellText(cell);
        meter(helper, new Setup("alloy_furnace", WorkObjectiveType.ALLOY, tiers, chassis.stage(), name(chassis), gearText, recipe, HEATED),
                furnace, furnace::effectiveStats, furnace.getEnergyStorage(null), furnace.getProcessInventory(), feeds,
                AlloyFurnaceBlockEntity.SLOT_OUTPUT);
    }

    private static void press(GameTestHelper helper, HeatCoreMaterial core, BatteryCellMaterial cell, String recipe, Item input) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.METAL_PRESS.get(), MetalPressBlockEntity.class);
        ItemStackHandler gear = press.getGearInventory();
        gear.setStackInSlot(MetalPressBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(core).get()));
        gear.setStackInSlot(MetalPressBlockEntity.SLOT_SERVO, new ItemStack(ModItems.servo(ServoMaterial.STEEL).get()));
        gear.setStackInSlot(MetalPressBlockEntity.SLOT_MOLD, new ItemStack(ModItems.PLATE_MOLD.get()));
        if (cell != null) { gear.setStackInSlot(MetalPressBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(cell).get())); }
        String gearText = "core:" + name(core) + ",servo:steel,mold:plate" + cellText(cell);
        meter(helper, new Setup("metal_press", WorkObjectiveType.PRESS, new int[] {1, 2, 3, 4, 5}, 4, "metal_press", gearText, recipe, HEATED),
                press, press::effectiveStats, press.getEnergyStorage(null), press.getProcessInventory(),
                new Feed[] {feed(MetalPressBlockEntity.SLOT_INPUT, input)}, MetalPressBlockEntity.SLOT_OUTPUT);
    }

    private static void calibrator(GameTestHelper helper, ResonanceCalibratorChassis chassis, CalibrationGearMaterial gearMaterial,
            BatteryCellMaterial cell, int stage, int[] tiers) {
        ResonanceCalibratorBlockEntity calibrator = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.resonanceCalibrator(chassis).get(), ResonanceCalibratorBlockEntity.class);
        ItemStackHandler gear = calibrator.getGearInventory();
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_RESONANCE_COIL, new ItemStack(ModItems.resonanceCoil(gearMaterial).get()));
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_CONTROL_BOARD, new ItemStack(ModItems.controlBoard(gearMaterial).get()));
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE, new ItemStack(ModItems.calibrationPattern(CalibrationFamily.STRUCTURAL).get()));
        if (cell != null) { gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(cell).get())); }
        String gearText = "coil:" + name(gearMaterial) + ",board:" + name(gearMaterial) + ",pattern:structural" + cellText(cell);
        meter(helper, new Setup("resonance_calibrator", WorkObjectiveType.CALIBRATE, tiers, stage, name(chassis), gearText,
                        "resonant_structural_iron_plate", UNHEATED),
                calibrator, calibrator::effectiveStats, calibrator.getEnergyStorage(null), calibrator.getProcessInventory(),
                new Feed[] {feed(ResonanceCalibratorBlockEntity.SLOT_INPUT, item("iron_plate")), feed(ResonanceCalibratorBlockEntity.SLOT_CATALYST, item("matrix")),
                        feed(ResonanceCalibratorBlockEntity.SLOT_STABILIZER, item("stabilization_catalyst"))},
                ResonanceCalibratorBlockEntity.SLOT_OUTPUT);
    }

    private static void assembler(GameTestHelper helper, BatteryCellMaterial cell) {
        BatteryAssemblerBlockEntity assembler = MachineTestSupport.place(helper, MACHINE, ModBlocks.BATTERY_ASSEMBLER.get(), BatteryAssemblerBlockEntity.class);
        if (cell != null) {
            assembler.getGearInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(cell).get()));
        }
        meter(helper, new Setup(WorkOrderFamilies.COMPONENT_ASSEMBLER, WorkObjectiveType.ASSEMBLE, new int[] {1, 2, 3, 4, 5}, 0, "assembler",
                        "none" + cellText(cell), "copper_battery_cell", UNHEATED),
                assembler, assembler::effectiveStats, assembler.getEnergyStorage(null), assembler.getProcessInventory(),
                new Feed[] {feed(BatteryAssemblerBlockEntity.SLOT_INPUT_0, item("battery_cell")), feed(BatteryAssemblerBlockEntity.SLOT_INPUT_1, item("copper_plate")),
                        feed(BatteryAssemblerBlockEntity.SLOT_INPUT_2, Items.COPPER_INGOT), feed(BatteryAssemblerBlockEntity.SLOT_INPUT_3, Items.REDSTONE)},
                BatteryAssemblerBlockEntity.SLOT_OUTPUT_0, BatteryAssemblerBlockEntity.SLOT_OUTPUT_1, BatteryAssemblerBlockEntity.SLOT_OUTPUT_2,
                BatteryAssemblerBlockEntity.SLOT_OUTPUT_3);
    }

    private static void recycler(GameTestHelper helper, ComponentRecyclerChassis chassis, DisassemblyHeadMaterial head, String input, int stage,
            int[] tiers) {
        ComponentRecyclerBlockEntity recycler = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.componentRecycler(chassis).get(), ComponentRecyclerBlockEntity.class);
        recycler.getGearInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD, new ItemStack(ModItems.disassemblyHead(head).get()));
        meter(helper, new Setup(WorkOrderFamilies.COMPONENT_RECYCLER, WorkObjectiveType.RECYCLE, tiers, stage, name(chassis), "head:" + name(head),
                        input, UNHEATED),
                recycler, recycler::effectiveStats, recycler.getEnergyStorage(null), recycler.getProcessInventory(),
                new Feed[] {feed(ComponentRecyclerBlockEntity.SLOT_INPUT, item(input))},
                ComponentRecyclerBlockEntity.SLOT_OUTPUT_PRIMARY, ComponentRecyclerBlockEntity.SLOT_OUTPUT_SECONDARY,
                ComponentRecyclerBlockEntity.SLOT_OUTPUT_TERTIARY);
    }

    /**
     * Loads an Any-work chip that never finishes, keeps the inputs full and the outputs empty, and logs the speed the chip
     * counted: after the warm-up, between the first and last finished work after the warm-up, and over the whole run.
     */
    private static <T extends BaseMachineBlockEntity & MachineInfoProvider> void meter(GameTestHelper helper, Setup setup, T machine,
            Supplier<MachineStatAccumulator> stats, IEnergyStorage energy, ItemStackHandler process, Feed[] feeds, int... outputs) {
        WorkOrder order = new WorkOrder(setup.family(), setup.tiers()[0], new WorkObjective(RNGTech.id("meter"),
                new WorkPart(setup.type(), WorkFilter.ANY, 0, "", 1_000_000)), 0, 0, WorkOrder.BASE_BOUNTY, 0);
        machine.setWorkOrders(MachineWorkOrders.EMPTY.withLoaded(new LoadedWorkOrder(order, MachineTraits.EMPTY)).withActive(0));
        for (Feed feed : feeds) { topUp(process, feed); }
        MachineTestSupport.powerEachTick(helper, energy);
        Meter meter = new Meter();
        helper.onEachTick(() -> {
            for (Feed feed : feeds) { topUp(process, feed); }
            for (int slot : outputs) {
                if (!process.getStackInSlot(slot).isEmpty()) { process.setStackInSlot(slot, ItemStack.EMPTY); }
            }
            meter.sample(helper.getTick(), work(machine, setup.family()));
        });
        helper.runAtTickTime(END, () -> {
            long value = work(machine, setup.family());
            long tick = helper.getTick();
            MachineStatAccumulator current = stats.get();
            double windowed = meter.warmValue < 0 ? -1 : (value - meter.warmValue) * 100.0 / (tick - meter.warmTick);
            double span = meter.spanLast > meter.spanFirst ? (meter.spanLastValue - meter.spanFirstValue) * 100.0 / (meter.spanLast - meter.spanFirst) : -1;
            double average = value * 100.0 / tick;
            for (int tier : setup.tiers()) {
                int table = WorkOrderFamilies.jobSpeed(setup.family(), tier);
                LOGGER.info(String.format(Locale.ROOT,
                        "WO-TIMING family=%s tier=%d stage=%d chassis=%s gear=%s recipe=%s steady=%.0f span=%.0f warmupTicks=%d average=%.0f"
                                + " table=%d tableBare=%.0f completions=%d ticks=%d ps=%.3f tc=%.3f batch=%.0f overhead=%.0f maxTemp=%.0f transfer=%.0f state=[%s]",
                        setup.family(), tier, stageOf(setup, tier),
                        setup.chassis(), setup.gear(), setup.recipe(), windowed, span, meter.firstTick, average, table, table / setup.ratio(),
                        meter.completions, tick, current.value(MachineStat.PROCESSING_SPEED), current.value(MachineStat.THERMAL_CONDUCTIVITY),
                        current.value(MachineStat.BATCH_SIZE), current.value(MachineStat.BATCH_OVERHEAD), current.value(MachineStat.MAX_TEMPERATURE),
                        current.value(MachineStat.ENERGY_TRANSFER), MachineTestSupport.describe(machine)));
            }
            if (value <= 0 || span <= 0) {
                throw new GameTestAssertException("No work counted for " + setup + ": " + MachineTestSupport.describe(machine));
            }
            for (int tier : setup.tiers()) {
                int table = WorkOrderFamilies.jobSpeed(setup.family(), tier);
                double good = span * setup.ratio();
                if (Math.abs(good - table) > table * TOLERANCE) {
                    throw new GameTestAssertException(String.format(Locale.ROOT, "%s Tier %d: a good build runs at about %.0f, but JOB_SPEED says %d",
                            setup, tier, good, table));
                }
            }
            helper.succeed();
        });
    }

    /** The tier's stage capped at the family's top stage, or 0 for a stageless family. */
    private static int stageOf(Setup setup, int tier) {
        return setup.stage() == 0 ? 0 : Math.min(tier + 3, WorkOrderFamilies.topStage(setup.family()));
    }

    private static long work(BaseMachineBlockEntity machine, String family) {
        List<LoadedWorkOrder> orders = machine.workOrders().orders();
        if (orders.isEmpty()) { return 0; }
        LoadedWorkOrder job = orders.getFirst();
        return job.totalProgress() * WorkOrderFamilies.workTicks(family) + job.workCarry();
    }

    private static void topUp(ItemStackHandler process, Feed feed) {
        ItemStack held = process.getStackInSlot(feed.slot());
        int max = feed.item().getDefaultMaxStackSize();
        if (held.isEmpty() || held.getCount() < max / 2) {
            process.setStackInSlot(feed.slot(), new ItemStack(feed.item(), max));
        }
    }

    private static Feed feed(int slot, Item item) {
        return new Feed(slot, item);
    }

    private static Item item(String path) {
        Item item = BuiltInRegistries.ITEM.get(RNGTech.id(path));
        if (item == Items.AIR) { throw new GameTestAssertException("Unknown item rngtech:" + path); }
        return item;
    }

    private static String cellText(BatteryCellMaterial cell) {
        return cell == null ? ",cell:none" : ",cell:" + name(cell);
    }

    private static String name(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private record Feed(int slot, Item item) {
    }

    private record Setup(String family, WorkObjectiveType type, int[] tiers, int stage, String chassis, String gear, String recipe, double ratio) {
        @Override public String toString() {
            return family + " " + chassis + " " + gear + " " + recipe + " tiers " + Arrays.toString(tiers);
        }
    }

    /** Work counted on the chip over time, sampled once a tick. */
    private static final class Meter {
        private long previous;
        private int completions;
        private long firstTick = -1;
        private long warmTick = -1;
        private long warmValue = -1;
        private long spanFirst = -1;
        private long spanFirstValue;
        private long spanLast = -1;
        private long spanLastValue;

        void sample(long tick, long value) {
            if (warmValue < 0 && tick >= WARMUP) {
                warmTick = tick;
                warmValue = value;
            }
            if (value <= previous) { return; }
            previous = value;
            completions++;
            if (firstTick < 0) { firstTick = tick; }
            if (tick >= WARMUP) {
                if (spanFirst < 0) {
                    spanFirst = tick;
                    spanFirstValue = value;
                }
                spanLast = tick;
                spanLastValue = value;
            }
        }
    }
}
