package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AlgaeCultureDynamics;
import com.rngtech.content.blockentity.AlgaePhotobioreactorBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Algae Photobioreactor cultures: a fresh culture ramps up, a full one harvests at the recipe rate, aeration trades carbon for speed. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlgaeGameTests {
    private static final BlockPos REACTOR = new BlockPos(2, 1, 2);
    /** Carbon Monoxide: 500 mB and 600 ticks per Algae Biomass. */
    private static final int CYCLE_TICKS = 600;
    private static final int CARBON_PER_HARVEST = 500;
    private static final int TANK = 4000;

    private AlgaeGameTests() {
    }

    /** The first harvest waits for the culture to grow in; once it is full, harvests come exactly once per cycle. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = CYCLE_TICKS * 4)
    public static void freshCultureRampsUpThenHarvestsAtTheRecipeRate(GameTestHelper helper) {
        AlgaePhotobioreactorBlockEntity reactor = litReactor(helper);
        List<Long> harvests = recordHarvests(helper, reactor);
        helper.onEachTick(() -> {
            if (harvests.size() < 3) {
                return;
            }
            long first = harvests.get(0);
            long steady = harvests.get(2) - harvests.get(1);
            check(first > CYCLE_TICKS && first < CYCLE_TICKS * 2, "First harvest at tick " + first + " should wait for the culture");
            check(Math.abs(steady - CYCLE_TICKS) <= 1, "A full culture harvested after " + steady + " ticks, expected " + CYCLE_TICKS);
            int used = TANK - reactor.getCarbonFluid().getAmount();
            check(Math.abs(used - 3 * CARBON_PER_HARVEST) <= 1, "Three rated harvests used " + used + " mB of carbon");
            check(reactor.cultureDensity() == 1.0, "The culture should be full, was " + reactor.cultureDensity());
            helper.succeed();
        });
    }

    /** Full aeration grows faster but feeds more gas into each harvest. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = CYCLE_TICKS * 4)
    public static void richAerationTradesCarbonForSpeed(GameTestHelper helper) {
        AlgaePhotobioreactorBlockEntity reactor = litReactor(helper);
        reactor.setAeration(AlgaeCultureDynamics.MAX_AERATION);
        List<Long> harvests = recordHarvests(helper, reactor);
        helper.onEachTick(() -> {
            if (harvests.size() < 3) {
                return;
            }
            long steady = harvests.get(2) - harvests.get(1);
            int expectedTicks = (int) Math.ceil(CYCLE_TICKS / AlgaeCultureDynamics.growth(AlgaeCultureDynamics.MAX_AERATION));
            check(Math.abs(steady - expectedTicks) <= 1, "Rich aeration harvested after " + steady + " ticks, expected " + expectedTicks);
            int used = TANK - reactor.getCarbonFluid().getAmount();
            int expected = (int) Math.round(3 * CARBON_PER_HARVEST * AlgaeCultureDynamics.carbonPerBiomass(AlgaeCultureDynamics.MAX_AERATION));
            check(Math.abs(used - expected) <= 2, "Three rich harvests used " + used + " mB of carbon, expected about " + expected);
            helper.succeed();
        });
    }

    /** Without light the culture dies back, but it keeps its harvest progress for when the light returns. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 800)
    public static void darkCultureDiesBackWithoutLosingProgress(GameTestHelper helper) {
        AlgaePhotobioreactorBlockEntity reactor = litReactor(helper);
        double[] grown = new double[2];
        helper.runAtTickTime(400, () -> {
            grown[0] = reactor.cultureDensity();
            grown[1] = reactor.machineInfo().progress();
            helper.setBlock(REACTOR.above(), Blocks.STONE);
        });
        helper.runAtTickTime(700, () -> {
            check(grown[0] > AlgaeCultureDynamics.SEED_DENSITY, "The culture should have grown in the light, was " + grown[0]);
            check(reactor.cultureDensity() < grown[0], "The culture should die back in the dark: " + reactor.cultureDensity() + " vs " + grown[0]);
            check(reactor.machineInfo().progress() >= grown[1] - 1, "Harvest progress should wait in the dark, not reset");
            helper.succeed();
        });
    }

    private static AlgaePhotobioreactorBlockEntity litReactor(GameTestHelper helper) {
        AlgaePhotobioreactorBlockEntity reactor = MachineTestSupport.place(
                helper, REACTOR, ModBlocks.ALGAE_PHOTOBIOREACTOR.get(), AlgaePhotobioreactorBlockEntity.class);
        helper.setBlock(REACTOR.above(), Blocks.GLOWSTONE);
        IFluidHandler fluids = reactor.getFluidHandler(Direction.UP);
        fluids.fill(new FluidStack(Fluids.WATER, TANK), IFluidHandler.FluidAction.EXECUTE);
        fluids.fill(new FluidStack(ModFluids.CARBON_MONOXIDE_SOURCE.get(), TANK), IFluidHandler.FluidAction.EXECUTE);
        return reactor;
    }

    private static List<Long> recordHarvests(GameTestHelper helper, AlgaePhotobioreactorBlockEntity reactor) {
        List<Long> harvests = new ArrayList<>();
        int[] last = {0};
        helper.onEachTick(() -> {
            int count = reactor.getInventory().getStackInSlot(AlgaePhotobioreactorBlockEntity.SLOT_OUTPUT).getCount();
            if (count > last[0]) {
                harvests.add(helper.getTick());
            }
            last[0] = count;
        });
        return harvests;
    }
}
