package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.blockentity.ResonanceCalibratorVisualState;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The world view of the Resonance Calibrator: lanes, pattern, cycle, stability, and installed Gear reach the synced visual state. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResonanceCalibratorVisualGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private ResonanceCalibratorVisualGameTests() {
    }

    /** A working Iron Calibrator shows its Gear, the input on its pin, the active pattern, and the cycle on its scope. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void calibratorShowsItsGearPatternAndCycle(GameTestHelper helper) {
        ResonanceCalibratorBlockEntity calibrator = equipped(helper, ResonanceCalibratorChassis.IRON, 16);
        calibrator.getGearInventory().setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_STABILIZER_MATRIX,
                new ItemStack(ModItems.stabilizerMatrix(CalibrationGearMaterial.COPPER).get()));
        calibrator.getGearInventory().setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_BATTERY_CELL,
                new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()));
        MachineTestSupport.powerEachTick(helper, calibrator.getEnergyStorage(null));

        helper.succeedWhen(() -> {
            ResonanceCalibratorVisualState state = calibrator.currentVisualState();
            check(state.coil().equals("iron") && state.board().equals("iron") && state.matrix().equals("copper") && state.cell().equals("iron"),
                    "Wrong parts shown: " + state.key());
            check(state.lanes() == 1 && state.input() == item("iron_plate"), "The pin should hold an Iron Plate: " + state.key());
            check(state.pattern() == ModItems.calibrationPattern(CalibrationFamily.STRUCTURAL).get(), "The monitor should show the pattern: " + state.key());
            check(state.working() && state.cycleTicks() > 0 && state.progress() > 0.0F, "The scope should show a running cycle: " + state.key());
            check(state.stabilityMin() > 0 && state.stabilityMax() >= state.stabilityMin(), "The scope should show the stability range: " + state.key());
            check(state.charge() >= 0.0F && state.streakCap() == 0, "A cell lights the meter, and there is no streak without the Harmonist: " + state.key());
            check(ResonanceCalibratorVisualState.load(state.save()).key().equals(state.key()), "The visual state should survive syncing: " + state.key());
        });
    }

    /** A finished calibration lands in the tray, marks its roll on the stability bar, and steps the flash counter. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void finishedCalibrationShowsItsRoll(GameTestHelper helper) {
        ResonanceCalibratorBlockEntity calibrator = equipped(helper, ResonanceCalibratorChassis.IRON, 16);
        MachineTestSupport.powerEachTick(helper, calibrator.getEnergyStorage(null));

        helper.succeedWhen(() -> {
            ResonanceCalibratorVisualState state = calibrator.currentVisualState();
            check(state.output() == ModItems.calibratedComponent(CalibrationFamily.STRUCTURAL).get(), "The tray should hold the component: " + state.key());
            check(state.lastStability() >= 0 && state.lastStability() <= 100, "The bar should mark the roll: " + state.key());
            check(state.completions() > 0, "A finished calibration should step the flash counter: " + state.key());
        });
    }

    /** A Tungstensteel Calibrator fills as many of its three pins as its stacks allow. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void bulkCalibratorShowsEachLane(GameTestHelper helper) {
        ResonanceCalibratorBlockEntity calibrator = equipped(helper, ResonanceCalibratorChassis.TUNGSTENSTEEL, 2);
        helper.runAfterDelay(1, () -> {
            check(calibrator.currentVisualState().lanes() == 2, "Two plates fill two pins: " + calibrator.currentVisualState().key());
            calibrator.getProcessInventory().setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_INPUT, new ItemStack(item("iron_plate"), 16));
            check(calibrator.currentVisualState().lanes() == 3, "A full stack fills all three pins: " + calibrator.currentVisualState().key());
            helper.succeed();
        });
    }

    /** A Calibrator without Gear or a pattern shows bare saddles, a dark scope, and the input still on its pin. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void bareCalibratorShowsEmptySaddles(GameTestHelper helper) {
        ResonanceCalibratorBlockEntity calibrator = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.resonanceCalibrator(ResonanceCalibratorChassis.STEEL).get(), ResonanceCalibratorBlockEntity.class);
        calibrator.getProcessInventory().setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_INPUT, new ItemStack(item("iron_plate"), 4));
        helper.runAfterDelay(10, () -> {
            ResonanceCalibratorVisualState state = calibrator.currentVisualState();
            check(state.coil().isEmpty() && state.board().isEmpty() && state.matrix().isEmpty() && state.cell().isEmpty(),
                    "A bare Calibrator shows no Gear: " + state.key());
            check(state.pattern() == Items.AIR && state.cycleTicks() == 0 && !state.working(), "Without a pattern the scope stays dark: " + state.key());
            check(state.charge() < 0.0F && state.lastStability() < 0, "No cell lights the meter, and nothing has been rolled: " + state.key());
            check(state.lanes() == 1 && state.input() == item("iron_plate"), "The pin should still hold the plate: " + state.key());
            helper.succeed();
        });
    }

    /** A Calibrator with an Iron coil and board, the Structural pattern selected, Iron Plates, and Lapis Lazuli. */
    private static ResonanceCalibratorBlockEntity equipped(GameTestHelper helper, ResonanceCalibratorChassis chassis, int plates) {
        ResonanceCalibratorBlockEntity calibrator = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.resonanceCalibrator(chassis).get(), ResonanceCalibratorBlockEntity.class);
        ItemStackHandler gear = calibrator.getGearInventory();
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_RESONANCE_COIL, new ItemStack(ModItems.resonanceCoil(CalibrationGearMaterial.IRON).get()));
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_CONTROL_BOARD, new ItemStack(ModItems.controlBoard(CalibrationGearMaterial.IRON).get()));
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE,
                new ItemStack(ModItems.calibrationPattern(CalibrationFamily.STRUCTURAL).get()));
        ItemStackHandler process = calibrator.getProcessInventory();
        process.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_INPUT, new ItemStack(item("iron_plate"), plates));
        process.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_CATALYST, new ItemStack(Items.LAPIS_LAZULI, 16));
        return calibrator;
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, path));
    }
}
