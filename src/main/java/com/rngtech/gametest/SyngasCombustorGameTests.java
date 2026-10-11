package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CombustorVisualState;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The shaped Syngas Combustor syncs what its renderer draws; the Gasifier and Reformer that share its block entity stay cubes. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SyngasCombustorGameTests {
    private static final BlockPos COMBUSTOR = new BlockPos(2, 1, 2);
    private static final BlockPos GASIFIER = new BlockPos(1, 1, 1);
    private static final BlockPos REFORMER = new BlockPos(3, 1, 3);
    private static final int SYNGAS = 4000;
    private static final int TANK = 8000;

    private SyngasCombustorGameTests() {
    }

    /** A firing combustor sends its flame, fuel level, shaft speed, and installed Gear to clients. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 120)
    public static void firingCombustorSyncsFuelSpeedAndGear(GameTestHelper helper) {
        GasChemistryBlockEntity combustor = MachineTestSupport.place(helper, COMBUSTOR, ModBlocks.SYNGAS_COMBUSTOR.get(), GasChemistryBlockEntity.class);
        combustor.getFluidHandler(Direction.UP).fill(new FluidStack(ModFluids.SYNGAS_SOURCE.get(), SYNGAS), IFluidHandler.FluidAction.EXECUTE);
        combustor.getGearInventory().insertItem(GasChemistryBlockEntity.SLOT_SERVO, new ItemStack(ModItems.STEEL_SERVO.get()), false);
        combustor.getGearInventory().insertItem(
                GasChemistryBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.SPARKSTEEL).get()), false);
        check(!combustor.getBlockState().canOcclude(), "The shaped combustor should not occlude its neighbours");
        check(combustor.getUpdatePacket() != null, "The combustor should send its visual state to clients");
        helper.runAtTickTime(80, () -> {
            CombustorVisualState state = CombustorVisualState.load(combustor.getUpdateTag(helper.getLevel().registryAccess()));
            check(state.firing(), "The combustor should be firing: " + MachineTestSupport.describe(combustor));
            check(state.fuelFluid() == ModFluids.SYNGAS_SOURCE.get(), "The fuel sight glass should show Syngas, was " + state.fuelFluid());
            check(state.fuel() > 0.0F && state.fuel() <= SYNGAS / (float) TANK, "The fuel level should show the Syngas left, was " + state.fuel());
            check(state.speed() > 1.05F, "A Steel Servo should speed the shaft up, was " + state.speed());
            check("steel".equals(state.servo()), "The Servo should sync as steel, was '" + state.servo() + "'");
            check("sparksteel".equals(state.cell()), "The Battery Cell should sync as sparksteel, was '" + state.cell() + "'");
            check(state.charge() > 0.0F, "Generated FE should overflow into the Battery Cell, was " + state.charge());
            helper.succeed();
        });
    }

    /** The Gasifier and Reformer share the combustor's block entity but keep their cube models and default sync. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void gasifierAndReformerStayCubes(GameTestHelper helper) {
        for (BlockPos pos : new BlockPos[] {GASIFIER, REFORMER}) {
            Block block = pos.equals(GASIFIER) ? ModBlocks.COAL_GASIFIER.get() : ModBlocks.STEAM_METHANE_REFORMER.get();
            GasChemistryBlockEntity machine = MachineTestSupport.place(helper, pos, block, GasChemistryBlockEntity.class);
            check(machine.getBlockState().canOcclude(), machine.machine() + " should stay a full occluding cube");
            check(machine.getUpdatePacket() == null, machine.machine() + " should not send combustor visual updates");
            check(!machine.getUpdateTag(helper.getLevel().registryAccess()).contains("Firing"), machine.machine() + " should not sync a combustor visual state");
        }
        helper.succeed();
    }
}
