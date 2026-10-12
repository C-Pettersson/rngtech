package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AmmoniaFuelCellBlockEntity;
import com.rngtech.content.blockentity.AmmoniaFuelCellVisualState;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Ammonia Fuel Cell model support: the rear cable port exports FE, and the renderer's synced state follows the machine. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AmmoniaFuelCellGameTests {
    private static final BlockPos CELL = new BlockPos(2, 1, 2);
    private static final int CYCLE_TICKS = 360;
    private static final int TANK = 8000;
    private static final int AMMONIA_PER_CYCLE = 1000;

    private AmmoniaFuelCellGameTests() {
    }

    /** The cable port sits on the back of the model, so the back must export FE whichever way the cell faces. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void rearPortFaceExportsForEveryFacing(GameTestHelper helper) {
        runningCell(helper);
        GameTestSequence sequence = helper.startSequence();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos rear = CELL.relative(facing.getOpposite());
            CrusherBlockEntity[] crusher = new CrusherBlockEntity[1];
            sequence.thenExecute(() -> {
                helper.setBlock(CELL, helper.getBlockState(CELL).setValue(BaseMachineBlock.FACING, facing));
                crusher[0] = MachineTestSupport.place(
                        helper, rear, ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
            }).thenExecuteAfter(5, () -> {
                check(crusher[0].machineInfo().energy() > 0, "Facing " + facing + ": the rear port face exported no FE");
                helper.setBlock(rear, Blocks.AIR);
            });
        }
        sequence.thenSucceed();
    }

    /** The renderer's state follows the membrane slot, the Ammonia tank, and the residue each cycle leaves in the trap. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = CYCLE_TICKS + 100)
    public static void visualStateFollowsMembraneTankAndResidue(GameTestHelper helper) {
        AmmoniaFuelCellBlockEntity cell = MachineTestSupport.place(helper, CELL, ModBlocks.AMMONIA_FUEL_CELL.get(), AmmoniaFuelCellBlockEntity.class);
        // A sink keeps the cell running past its own small FE buffer.
        helper.setBlock(CELL.east(), ModBlocks.DEBUG_TRASHCAN.get());
        AmmoniaFuelCellVisualState empty = synced(helper, cell);
        check(!empty.membrane() && empty.ammonia() == 0.0F && empty.residue() == 0.0F, "An empty cell showed " + empty);

        fuel(cell);
        AmmoniaFuelCellVisualState fuelled = synced(helper, cell);
        check(fuelled.membrane(), "An installed membrane did not show in the stack");
        check(fuelled.ammonia() == 0.5F, "A half-full tank showed a level of " + fuelled.ammonia());
        check(fuelled.residue() == 0.0F, "No cycle has run, but the trap showed residue " + fuelled.residue());

        helper.succeedWhen(() -> {
            AmmoniaFuelCellVisualState state = synced(helper, cell);
            check(state.residue() > 0.0F, "No residue in the trap after " + helper.getTick() + " ticks: " + MachineTestSupport.describe(cell));
            check(state.ammonia() == (TANK / 2 - AMMONIA_PER_CYCLE) / (float) TANK, "After one cycle the tank showed " + state.ammonia());
        });
    }

    private static AmmoniaFuelCellBlockEntity runningCell(GameTestHelper helper) {
        AmmoniaFuelCellBlockEntity cell = MachineTestSupport.place(helper, CELL, ModBlocks.AMMONIA_FUEL_CELL.get(), AmmoniaFuelCellBlockEntity.class);
        fuel(cell);
        return cell;
    }

    private static void fuel(AmmoniaFuelCellBlockEntity cell) {
        cell.getGearInventory().setStackInSlot(AmmoniaFuelCellBlockEntity.SLOT_MEMBRANE, new ItemStack(ModItems.FUEL_CELL_MEMBRANE.get()));
        cell.getFluidHandler(Direction.NORTH).fill(new FluidStack(ModFluids.AMMONIA_SOURCE.get(), TANK / 2), IFluidHandler.FluidAction.EXECUTE);
    }

    private static AmmoniaFuelCellVisualState synced(GameTestHelper helper, AmmoniaFuelCellBlockEntity cell) {
        return AmmoniaFuelCellVisualState.load(cell.getUpdateTag(helper.getLevel().registryAccess()));
    }
}
