package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.BatteryChassisBlockEntity;
import com.rngtech.content.blockentity.BatteryChassisVisualState;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The world view of a Battery Chassis: its cells and their charge, which way FE flows, and the plate on each side. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatteryChassisVisualGameTests {
    /** Long enough for the smoothed flow to settle. */
    private static final int FLOW_TICKS = 30;

    private BatteryChassisVisualGameTests() {
    }

    /** Each active slot shows its cell and that cell's own charge; a lone chassis is idle and unwired. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void chassisShowsItsCellsAndCharge(GameTestHelper helper) {
        BatteryChassisBlockEntity chassis = chassis(helper, new BlockPos(2, 1, 2), BatteryChassisMaterial.STEEL);
        chassis.getInventory().setStackInSlot(0, cell(BatteryCellMaterial.IRON, 0.5));
        chassis.getInventory().setStackInSlot(2, cell(BatteryCellMaterial.COPPER, 1.0));
        helper.runAfterDelay(5, () -> {
            BatteryChassisVisualState state = chassis.currentVisualState();
            check(state.slots() == BatteryChassisMaterial.STEEL.slots(), "A Steel chassis shows its four slots: " + state);
            check(state.cells().get(0).kind().equals("iron") && state.cells().get(2).kind().equals("copper"), "Wrong cells shown: " + state);
            check(state.cells().get(1).empty() && state.cells().get(3).empty(), "Empty slots should show as empty: " + state);
            check(Math.abs(state.cells().get(0).charge() - 0.5F) < 0.02F, "The Iron cell should read half full: " + state);
            check(state.cells().get(2).charge() > 0.98F, "The Copper cell should read full: " + state);
            check(state.charge() > state.cells().get(0).charge() && state.charge() < 1.0F, "The meter should read the total: " + state);
            check(state.flow() == BatteryChassisVisualState.Flow.IDLE, "A lone chassis is idle: " + state);
            check(state.connectors() == 0 && state.contacts() == 0, "A lone chassis has no plates: " + state);
            check(BatteryChassisVisualState.load(state.save()).equals(state), "The visual state should survive syncing: " + state);
            helper.succeed();
        });
    }

    /** Two chassis side by side even out: the full one discharges, the empty one charges, and both show a contact plate. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = FLOW_TICKS + 40)
    public static void adjacentChassisShowFlowAndPorts(GameTestHelper helper) {
        BatteryChassisBlockEntity full = chassis(helper, new BlockPos(1, 1, 2), BatteryChassisMaterial.IRON);
        BatteryChassisBlockEntity empty = chassis(helper, new BlockPos(2, 1, 2), BatteryChassisMaterial.IRON);
        full.getInventory().setStackInSlot(0, cell(BatteryCellMaterial.IRON, 1.0));
        empty.getInventory().setStackInSlot(0, cell(BatteryCellMaterial.IRON, 0.0));
        helper.runAfterDelay(FLOW_TICKS, () -> {
            BatteryChassisVisualState source = full.currentVisualState();
            BatteryChassisVisualState sink = empty.currentVisualState();
            check(source.flow() == BatteryChassisVisualState.Flow.DISCHARGING, "The full chassis should discharge: " + source);
            check(sink.flow() == BatteryChassisVisualState.Flow.CHARGING, "The empty chassis should charge: " + sink);
            check(source.plate(Direction.EAST) == BatteryChassisVisualState.Plate.CONTACT
                    && sink.plate(Direction.WEST) == BatteryChassisVisualState.Plate.CONTACT,
                    "Both should show a contact plate: " + source + ", " + sink);
            check(source.plate(Direction.NORTH) == BatteryChassisVisualState.Plate.NONE
                    && source.plate(Direction.UP) == BatteryChassisVisualState.Plate.NONE, "Only the shared side has a plate: " + source);
            helper.succeed();
        });
    }

    /** A chassis feeding a running Crusher shows a contact plate toward it and discharges. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = FLOW_TICKS + 40)
    public static void chassisFeedingAMachineDischarges(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        BatteryChassisBlockEntity chassis = chassis(helper, pos, BatteryChassisMaterial.COPPER);
        chassis.getInventory().setStackInSlot(0, cell(BatteryCellMaterial.IRON, 1.0));
        CrusherBlockEntity crusher = MachineTestSupport.place(
                helper, pos.south(), ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.RAW_IRON, 64));
        helper.runAfterDelay(FLOW_TICKS, () -> {
            BatteryChassisVisualState state = chassis.currentVisualState();
            check(state.flow() == BatteryChassisVisualState.Flow.DISCHARGING, "A chassis feeding a Crusher should discharge: " + state);
            check(state.plate(Direction.SOUTH) == BatteryChassisVisualState.Plate.CONTACT,
                    "The chassis should show a contact plate toward the Crusher: " + state);
            check(state.meterSegments() > 0 && state.meterSegments() <= BatteryChassisVisualState.METER_SEGMENTS, "The meter should read charge: " + state);
            helper.succeed();
        });
    }

    /** A full Wooden chassis leaks, but leakage is not a flow, so its lamp stays dark instead of flickering. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = FLOW_TICKS + 40)
    public static void leakingChassisStaysIdle(GameTestHelper helper) {
        BatteryChassisBlockEntity chassis = chassis(helper, new BlockPos(2, 1, 2), BatteryChassisMaterial.WOODEN);
        chassis.getInventory().setStackInSlot(0, cell(BatteryCellMaterial.POTATO, 1.0));
        float[] start = new float[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> start[0] = chassis.currentVisualState().cells().get(0).charge())
                .thenExecuteFor(FLOW_TICKS, () -> {
                    BatteryChassisVisualState state = chassis.currentVisualState();
                    check(state.flow() == BatteryChassisVisualState.Flow.IDLE, "Leakage alone should leave the chassis idle: " + state);
                })
                .thenExecute(() -> check(chassis.currentVisualState().cells().get(0).charge() < start[0], "The Potato cell should leak"))
                .thenSucceed();
    }

    /** Universal Connectors facing the chassis from above and from the side give connector ports, whatever they hold. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void connectorsGetConnectorPorts(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        BatteryChassisBlockEntity chassis = chassis(helper, pos, BatteryChassisMaterial.IRON);
        helper.setBlock(pos.above(), ModBlocks.UNIVERSAL_CONNECTOR.get().defaultBlockState().setValue(UniversalConnectorBlock.FACING, Direction.DOWN));
        helper.setBlock(pos.north(), ModBlocks.UNIVERSAL_CONNECTOR.get().defaultBlockState().setValue(UniversalConnectorBlock.FACING, Direction.SOUTH));
        helper.setBlock(pos.west(), ModBlocks.UNIVERSAL_CONNECTOR.get().defaultBlockState().setValue(UniversalConnectorBlock.FACING, Direction.WEST));
        helper.runAfterDelay(2, () -> {
            BatteryChassisVisualState state = chassis.currentVisualState();
            check(state.plate(Direction.UP) == BatteryChassisVisualState.Plate.CONNECTOR, "A connector above should give a top port: " + state);
            check(state.plate(Direction.NORTH) == BatteryChassisVisualState.Plate.CONNECTOR, "A connector to the north should give a port: " + state);
            check(state.plate(Direction.WEST) == BatteryChassisVisualState.Plate.NONE, "A connector facing away gives no plate: " + state);
            check(state.plate(Direction.DOWN) == BatteryChassisVisualState.Plate.NONE, "Nothing below gives no plate: " + state);
            helper.succeed();
        });
    }

    private static BatteryChassisBlockEntity chassis(GameTestHelper helper, BlockPos pos, BatteryChassisMaterial material) {
        return MachineTestSupport.place(helper, pos, ModBlocks.batteryChassis(material).get(), BatteryChassisBlockEntity.class);
    }

    private static ItemStack cell(BatteryCellMaterial material, double fill) {
        ItemStack cell = new ItemStack(ModItems.batteryCell(material).get());
        int capacity = BatteryCellItem.energyCapacity(cell);
        BatteryCellItem.setEnergy(cell, (int) Math.round(capacity * fill), capacity);
        return cell;
    }
}
