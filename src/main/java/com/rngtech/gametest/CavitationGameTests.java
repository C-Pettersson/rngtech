package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
import com.rngtech.content.energy.CavitationRotorDynamics;
import com.rngtech.content.energy.CavitationRotorMaterial;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.item.CavitationPartItem;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Cavitation rotor speed: spool-up before the cycle advances, supercavitation output, and coasting when paused. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CavitationGameTests {
    private static final int SAMPLE_TICKS = 40;

    private CavitationGameTests() {
    }

    private static final int SUPERCAVITATION_THROTTLE = 90;
    private static final int RESONANCE_RUN_TICKS = 900;

    /** A Titanium Heat Core sheds 90% throttle heat, so the FE gain is measured without a strain penalty. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void rotorSpoolsUpThenTradesWearForOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CavitationGeneratorBlockEntity generator = steelGenerator(helper, pos);
        generator.getGearInventory().setStackInSlot(CavitationGeneratorBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        IEnergyStorage energy = generator.getEnergyStorage(Direction.NORTH);

        long[] generated = new long[1];
        int[] ticks = new int[1];
        int[] lastProgress = new int[1];
        helper.onEachTick(() -> {
            int progress = generator.machineInfo().progress();
            if (progress > lastProgress[0]) {
                check(CavitationRotorDynamics.cavitating(generator.rotorSpeed()), "The cycle advanced at " + generator.rotorSpeed() + " before cavitation onset");
            }
            lastProgress[0] = progress;
            generated[0] += energy.extractEnergy(Integer.MAX_VALUE, false);
            ticks[0]++;
        });

        long[] mark = new long[2];
        double[] rates = new double[2];
        double[] pausedFrom = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> check(generator.rotorSpeed() >= CavitationRotorDynamics.RATED_SPEED,
                        "Spooling to rated speed: " + generator.rotorSpeed() + "; " + MachineTestSupport.describe(generator)))
                .thenExecute(() -> mark(mark, generated, ticks))
                .thenIdle(SAMPLE_TICKS)
                .thenExecute(() -> {
                    rates[0] = rate(mark, generated, ticks);
                    check(rates[0] > 0.0, "No FE at rated speed; " + MachineTestSupport.describe(generator));
                    generator.setThrottle(SUPERCAVITATION_THROTTLE);
                })
                .thenWaitUntil(() -> check(generator.rotorSpeed() >= CavitationRotorDynamics.targetSpeed(SUPERCAVITATION_THROTTLE),
                        "Spooling to 90% throttle: " + generator.rotorSpeed()))
                .thenExecute(() -> mark(mark, generated, ticks))
                .thenIdle(SAMPLE_TICKS)
                .thenExecute(() -> {
                    rates[1] = rate(mark, generated, ticks);
                    double ratio = rates[1] / rates[0];
                    double expected = CavitationRotorDynamics.generation(CavitationRotorDynamics.targetSpeed(SUPERCAVITATION_THROTTLE));
                    check(generator.heatStrain() < 5.0, "A Titanium Heat Core sheds 90% throttle heat, strain " + generator.heatStrain());
                    check(Math.abs(ratio - expected) < 0.05, "90% throttle made " + ratio + "x the rated FE/t, expected " + expected);
                    pausedFrom[0] = generator.rotorSpeed();
                    helper.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(10)
                .thenExecute(() -> check(generator.rotorSpeed() < pausedFrom[0] && generator.rotorSpeed() > 0.0,
                        "A paused rotor coasts down: " + pausedFrom[0] + " -> " + generator.rotorSpeed()))
                .thenSucceed();
    }

    /** Rated speed cools faster than it heats; full throttle without a Heat Core builds strain. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void fullThrottleOverheatsWithoutAHeatCore(GameTestHelper helper) {
        CavitationGeneratorBlockEntity rated = steelGenerator(helper, new BlockPos(1, 1, 2));
        CavitationGeneratorBlockEntity full = steelGenerator(helper, new BlockPos(3, 1, 2));
        full.setThrottle(CavitationRotorDynamics.MAX_THROTTLE);
        helper.runAfterDelay(250, () -> {
            check(rated.heatStrain() < 1.0, "Rated speed built heat strain " + rated.heatStrain());
            check(full.heatStrain() > 200.0, "Full throttle without a Heat Core only reached heat strain " + full.heatStrain());
            helper.succeed();
        });
    }

    /** Same rotor, same throttle: the one tuned exactly to its resonance wears about a third as fast and makes the same FE. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void tunedRotorWearsSlowerAtTheSameOutput(GameTestHelper helper) {
        CavitationGeneratorBlockEntity tuned = steelGenerator(helper, new BlockPos(1, 1, 2), CavitationRotorDynamics.RATED_THROTTLE);
        CavitationGeneratorBlockEntity detuned = steelGenerator(helper, new BlockPos(3, 1, 2), CavitationRotorDynamics.RATED_THROTTLE + 10);
        long[] energy = new long[2];
        helper.onEachTick(() -> {
            energy[0] += tuned.getEnergyStorage(Direction.NORTH).extractEnergy(Integer.MAX_VALUE, false);
            energy[1] += detuned.getEnergyStorage(Direction.NORTH).extractEnergy(Integer.MAX_VALUE, false);
        });
        helper.runAfterDelay(RESONANCE_RUN_TICKS, () -> {
            int tunedWear = CavitationPartItem.rotorWear(tuned.getGearInventory().getStackInSlot(CavitationGeneratorBlockEntity.SLOT_ROTOR));
            int detunedWear = CavitationPartItem.rotorWear(detuned.getGearInventory().getStackInSlot(CavitationGeneratorBlockEntity.SLOT_ROTOR));
            check(detunedWear >= 9, "Three detuned cycles should carry fractional wear to about 10, got " + detunedWear);
            check(tunedWear * 2 < detunedWear, "Tuned wear " + tunedWear + " is not well below detuned wear " + detunedWear);
            check(Math.abs(energy[0] - energy[1]) <= energy[1] / 100, "Resonance changed FE: " + energy[0] + " vs " + energy[1]);
            helper.succeed();
        });
    }

    private static CavitationGeneratorBlockEntity steelGenerator(GameTestHelper helper, BlockPos pos) {
        return steelGenerator(helper, pos, 0);
    }

    /** A Steel rotor and nozzle with full Water; a resonance above 0 is set on the rotor before install. */
    private static CavitationGeneratorBlockEntity steelGenerator(GameTestHelper helper, BlockPos pos, int resonance) {
        CavitationGeneratorBlockEntity generator = MachineTestSupport.place(helper, pos, ModBlocks.CAVITATION_GENERATOR.get(), CavitationGeneratorBlockEntity.class);
        ItemStack rotor = new ItemStack(ModItems.CAVITATION_ROTORS.get(CavitationRotorMaterial.STEEL).get());
        if (resonance > 0) {
            rotor.set(ModDataComponents.CAVITATION_ROTOR_RESONANCE.get(), resonance);
        }
        generator.getGearInventory().setStackInSlot(CavitationGeneratorBlockEntity.SLOT_ROTOR, rotor);
        generator.getGearInventory().setStackInSlot(CavitationGeneratorBlockEntity.SLOT_NOZZLE, new ItemStack(ModItems.COLLAPSE_NOZZLES.get(CollapseNozzleMaterial.STEEL).get()));
        generator.getInputTank().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        return generator;
    }

    private static void mark(long[] mark, long[] generated, int[] ticks) {
        mark[0] = generated[0];
        mark[1] = ticks[0];
    }

    private static double rate(long[] mark, long[] generated, int[] ticks) {
        return (generated[0] - mark[0]) / (double) Math.max(1, ticks[0] - mark[1]);
    }
}
