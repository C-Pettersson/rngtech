package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.ResonanceCalibratorMenu;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderOperations;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Work Orders on the Resonance Calibrator: a chip loaded through its Orders tab, and real calibrations counting toward it. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CalibratorWorkOrderGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private CalibratorWorkOrderGameTests() {
    }

    /** Two Structural calibrations, reported by their family, finish a "Calibrate 2 Structural components" job in its bay. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void calibrationsCountTowardTheirJob(GameTestHelper helper) {
        ResonanceCalibratorBlockEntity calibrator = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.resonanceCalibrator(ResonanceCalibratorChassis.IRON).get(), ResonanceCalibratorBlockEntity.class);
        ItemStackHandler gear = calibrator.getGearInventory();
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_RESONANCE_COIL, new ItemStack(ModItems.resonanceCoil(CalibrationGearMaterial.IRON).get()));
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_CONTROL_BOARD, new ItemStack(ModItems.controlBoard(CalibrationGearMaterial.IRON).get()));
        gear.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE, new ItemStack(ModItems.calibrationPattern(CalibrationFamily.STRUCTURAL).get()));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        WorkOrder calibrate = new WorkOrder("resonance_calibrator", 1, new WorkObjective(RNGTech.id("calibrate"),
                new WorkPart(WorkObjectiveType.CALIBRATE, WorkFilter.GROUP, 0, "structural", 2)), 0, 0, WorkOrder.BASE_BOUNTY, 0);
        player.getInventory().setItem(0, JobChipItem.of(ModItems.JOB_CHIP.get(), calibrate, MachineTraits.EMPTY));
        ResonanceCalibratorMenu menu = (ResonanceCalibratorMenu) calibrator.createMenu(1, player.getInventory(), player);
        check(menu.clickMenuButton(player, WorkOrderMenuSupport.BUTTON_SELECT_TAB + ResonanceCalibratorMenu.TAB_ORDERS)
                && menu.selectedTab() == ResonanceCalibratorMenu.TAB_ORDERS, "The Calibrator menu did not open the Orders tab");
        for (int index = 0; index < menu.slots.size() - MachineWorkOrders.SLOTS; index++) {
            Slot slot = menu.getSlot(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == 0) {
                menu.quickMoveStack(player, index);
            }
        }
        check(calibrator.workOrders().orders().size() == 1 && calibrator.workOrders().active() == MachineWorkOrders.NONE
                && WorkOrderOperations.perform(player, calibrator, "activate", "0") && calibrator.workOrders().active() == 0,
                "The Calibrator did not load its chip: " + calibrator.workOrders());

        ItemStackHandler process = calibrator.getProcessInventory();
        process.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_INPUT, new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("iron_plate")), 4));
        process.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_CATALYST, new ItemStack(Items.LAPIS_LAZULI, 16));
        MachineTestSupport.powerEachTick(helper, calibrator.getEnergyStorage(null));
        // Calibrated components keep their own stability, so they do not stack; take each one out.
        helper.onEachTick(() -> process.setStackInSlot(ResonanceCalibratorBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY));
        helper.succeedWhen(() -> {
            LoadedWorkOrder job = calibrator.workOrders().orders().isEmpty() ? null : calibrator.workOrders().orders().getFirst();
            check(job != null && job.fulfilled(), "Waiting for two calibrations: " + calibrator.workOrders() + "; " + MachineTestSupport.describe(calibrator));
        });
    }
}
