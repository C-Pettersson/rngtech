package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CombustorVisualState;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.CombustionMixture;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Syngas Combustor's fuel mixture trades fuel and exhaust for power, and the shaped combustor syncs what its renderer
 * draws.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SyngasCombustorGameTests {
    private static final BlockPos COMBUSTOR = new BlockPos(2, 1, 2);
    private static final int SYNGAS = 4000;
    private static final int TANK = 8000;
    private static final BlockPos RICH = new BlockPos(1, 1, 2);
    private static final BlockPos LEAN = new BlockPos(3, 1, 2);
    /** The Syngas recipe: 1,000 mB burns over 240 ticks for 57,600 FE and 500 mB of Carbon Exhaust. */
    private static final int CYCLE_TICKS = 240;
    private static final double FE_PER_TICK = 240.0;
    private static final int FUEL_PER_CYCLE = 1000;
    private static final int EXHAUST_PER_CYCLE = 500;

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

    /** A rich mixture burns a cycle sooner for more FE/t and more exhaust; a lean one is slower, cleaner, and thriftier. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 420)
    public static void mixtureTradesFuelAndExhaustForPower(GameTestHelper helper) {
        int[] mixtures = {CombustionMixture.MAX_MIXTURE, CombustionMixture.MIN_MIXTURE};
        BlockPos[] positions = {RICH, LEAN};
        GasChemistryBlockEntity[] combustors = new GasChemistryBlockEntity[2];
        long[] energy = new long[2];
        long[] finished = {-1L, -1L};
        int[] exhaust = new int[2];
        long[] cycleEnergy = new long[2];
        for (int i = 0; i < 2; i++) {
            combustors[i] = fueledCombustor(helper, positions[i]);
            combustors[i].setMixture(mixtures[i]);
        }
        helper.onEachTick(() -> {
            for (int i = 0; i < 2; i++) {
                energy[i] += drain(combustors[i]);
                if (finished[i] < 0 && combustors[i].getInputFluid().getAmount() <= SYNGAS - FUEL_PER_CYCLE) {
                    finished[i] = helper.getTick();
                    exhaust[i] = combustors[i].getOutputFluid().getAmount();
                    cycleEnergy[i] = energy[i];
                }
            }
            if (finished[0] < 0 || finished[1] < 0) {
                return;
            }
            for (int i = 0; i < 2; i++) {
                int mixture = mixtures[i];
                long expectedTicks = (long) Math.ceil(CYCLE_TICKS / CombustionMixture.fuelRate(mixture));
                check(Math.abs(finished[i] - expectedTicks) <= 2, mixture + "% burned a cycle in " + finished[i] + " ticks, expected " + expectedTicks);
                double feRate = cycleEnergy[i] / (double) finished[i];
                double expectedRate = FE_PER_TICK * CombustionMixture.power(mixture);
                check(Math.abs(feRate - expectedRate) < expectedRate * 0.01, mixture + "% made " + feRate + " FE/t, expected " + expectedRate);
                int expectedExhaust = CombustionMixture.exhaustAmount(EXHAUST_PER_CYCLE, mixture);
                check(Math.abs(exhaust[i] - expectedExhaust) <= expectedExhaust * 0.01 + 1,
                        mixture + "% left " + exhaust[i] + " mB of exhaust, expected about " + expectedExhaust);
            }
            check(cycleEnergy[1] > cycleEnergy[0], "A lean cycle should make more FE from the same fuel than a rich one");
            helper.succeed();
        });
    }

    /** Exhaust is charged as fuel burns, so switching to lean late in a rich cycle still pays for the rich part. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 360)
    public static void switchingLeanLateStillChargesTheRichExhaust(GameTestHelper helper) {
        GasChemistryBlockEntity combustor = fueledCombustor(helper, COMBUSTOR);
        combustor.setMixture(CombustionMixture.MAX_MIXTURE);
        int richTicks = 86;
        helper.onEachTick(() -> drain(combustor));
        helper.runAtTickTime(richTicks, () -> combustor.setMixture(CombustionMixture.MIN_MIXTURE));
        helper.onEachTick(() -> {
            if (combustor.getInputFluid().getAmount() > SYNGAS - FUEL_PER_CYCLE) {
                return;
            }
            double richShare = richTicks * CombustionMixture.fuelRate(CombustionMixture.MAX_MIXTURE) / CYCLE_TICKS;
            double expected = EXHAUST_PER_CYCLE * (richShare * CombustionMixture.exhaustPerFuel(CombustionMixture.MAX_MIXTURE)
                    + (1.0 - richShare) * CombustionMixture.exhaustPerFuel(CombustionMixture.MIN_MIXTURE));
            int exhaust = combustor.getOutputFluid().getAmount();
            check(Math.abs(exhaust - expected) <= 3, "A half-rich cycle left " + exhaust + " mB of exhaust, expected about " + expected);
            helper.succeed();
        });
    }

    private static GasChemistryBlockEntity fueledCombustor(GameTestHelper helper, BlockPos pos) {
        GasChemistryBlockEntity combustor = MachineTestSupport.place(helper, pos, ModBlocks.SYNGAS_COMBUSTOR.get(), GasChemistryBlockEntity.class);
        combustor.getFluidHandler(Direction.UP).fill(new FluidStack(ModFluids.SYNGAS_SOURCE.get(), SYNGAS), IFluidHandler.FluidAction.EXECUTE);
        return combustor;
    }

    /** Empties the combustor's FE each tick, as a connector would, so a full buffer never pauses it. */
    private static int drain(GasChemistryBlockEntity combustor) {
        IEnergyStorage storage = combustor.getEnergyStorage(Direction.NORTH);
        return storage.extractEnergy(Integer.MAX_VALUE, false);
    }
}
