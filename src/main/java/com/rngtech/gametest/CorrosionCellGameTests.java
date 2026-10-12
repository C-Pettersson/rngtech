package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CorrosionCellBlockEntity;
import com.rngtech.content.blockentity.CorrosionVisualState;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.energy.CorrosionCellLoad;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Corrosion Cell load: rated load keeps the recipe, a light load trades FE/t for FE per fuel, and the bath shows it. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CorrosionCellGameTests {
    private static final int LIGHT_LOAD = 50;
    private static final String IRON_PLATE = "rngtech:iron_plate";

    private CorrosionCellGameTests() {
    }

    /**
     * One Iron Plate each at rated and at half load. The rated cell makes the recipe's FE/t for the recipe's ticks; the
     * light one takes twice as long and gets {@link CorrosionCellLoad#fePerFuel} more FE out of the same plate.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 700)
    public static void lightLoadTradesFePerTickForFePerFuel(GameTestHelper helper) {
        CorrosionCellBlockEntity rated = loadedCell(helper, new BlockPos(1, 1, 2), CorrosionCellLoad.RATED_LOAD);
        CorrosionCellBlockEntity light = loadedCell(helper, new BlockPos(3, 1, 2), LIGHT_LOAD);
        Run ratedRun = new Run(rated);
        Run lightRun = new Run(light);
        boolean[] sampledMidCycle = new boolean[1];
        helper.onEachTick(() -> {
            ratedRun.tick();
            lightRun.tick();
            if (!sampledMidCycle[0] && lightRun.activeTicks == 100) {
                sampledMidCycle[0] = true;
                CorrosionVisualState state = light.currentVisualState();
                check(IRON_PLATE.equals(state.fuel()), "The bath should show the dissolving Iron Plate, got '" + state.fuel() + "'");
                check(Math.abs(state.current() - LIGHT_LOAD / 100.0F) < 0.01F, "The light cell should draw half current, got " + state.current());
                check(state.fuelLeft() > 0.5F && state.fuelLeft() < 0.8F, "A third of the way in, about two thirds of the plate should remain, got " + state.fuelLeft());
                check(state.bath(), "A cell with a fuel in it shows a filled bath");
            }
        });
        helper.succeedWhen(() -> {
            check(ratedRun.finished() && lightRun.finished(), "Waiting for both cycles: rated " + MachineTestSupport.describe(rated)
                    + "; light " + MachineTestSupport.describe(light));
            check(sampledMidCycle[0], "The light cell was never sampled mid-cycle");
            check(Math.abs(ratedRun.activeTicks - ratedRun.expectedTicks) <= 1,
                    "Rated load took " + ratedRun.activeTicks + " ticks, the recipe takes " + ratedRun.expectedTicks);
            check(Math.abs(ratedRun.energy - (long) ratedRun.expectedPerTick * ratedRun.expectedTicks) <= ratedRun.expectedTicks,
                    "Rated load made " + ratedRun.energy + " FE, expected " + ratedRun.expectedPerTick + " FE/t for " + ratedRun.expectedTicks + " ticks");
            double tickRatio = lightRun.activeTicks / (double) ratedRun.activeTicks;
            double energyRatio = lightRun.energy / (double) ratedRun.energy;
            check(Math.abs(tickRatio - CorrosionCellLoad.cycleLength(LIGHT_LOAD)) < 0.02,
                    "Half load should take twice as long, took " + tickRatio + "x");
            check(Math.abs(energyRatio - CorrosionCellLoad.fePerFuel(LIGHT_LOAD)) < 0.005,
                    "Half load should get " + CorrosionCellLoad.fePerFuel(LIGHT_LOAD) + "x the FE per plate, got " + energyRatio + "x");
            check(Math.abs(lightRun.expectedTicks - lightRun.activeTicks) <= 1,
                    "The screen promised " + lightRun.expectedTicks + " ticks at half load, the cycle took " + lightRun.activeTicks);
        });
    }

    /** A redstone-paused cell keeps its fuel and draws no current, then resumes the same fuel. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void pausedCellKeepsItsFuel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CorrosionCellBlockEntity cell = loadedCell(helper, pos, CorrosionCellLoad.RATED_LOAD);
        IEnergyStorage energy = cell.getEnergyStorage(Direction.NORTH);
        helper.onEachTick(() -> energy.extractEnergy(Integer.MAX_VALUE, false));
        float[] pausedAt = new float[1];
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    pausedAt[0] = cell.currentVisualState().fuelLeft();
                    check(pausedAt[0] > 0.0F && pausedAt[0] < 1.0F, "The plate should be part-dissolved, " + pausedAt[0] + " left");
                    helper.setBlock(pos.above(), Blocks.REDSTONE_BLOCK);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    CorrosionVisualState state = cell.currentVisualState();
                    check(state.current() == 0.0F, "A paused cell draws no current, got " + state.current());
                    check(Math.abs(state.fuelLeft() - pausedAt[0]) < 0.02F, "The paused plate kept dissolving: " + pausedAt[0] + " -> " + state.fuelLeft());
                    helper.setBlock(pos.above(), Blocks.AIR);
                })
                .thenWaitUntil(() -> check(cell.currentVisualState().fuelLeft() < pausedAt[0] - 0.05F,
                        "The cell should resume the same plate; " + MachineTestSupport.describe(cell)))
                .thenSucceed();
    }

    /**
     * Automation works on every face, whatever the model shows: FE leaves through all six faces and reaches a machine
     * placed on top, and with a Fluid Pump installed Electrolyte Solution fills through the top and bottom too.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void everyFaceTakesAutomation(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        CorrosionCellBlockEntity cell = loadedCell(helper, pos, CorrosionCellLoad.RATED_LOAD);
        cell.getGearInventory().setStackInSlot(CorrosionCellBlockEntity.SLOT_FLUID_PUMP, new ItemStack(ModItems.fluidPump(FluidPumpMaterial.OSMIUM).get()));
        CrusherBlockEntity above = MachineTestSupport.place(helper, pos.above(), ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
        BlockPos absolute = helper.absolutePos(pos);
        for (Direction side : Direction.values()) {
            IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, absolute, side);
            check(energy != null, "No FE access on the " + side + " face");
            IFluidHandler fluid = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, absolute, side);
            check(fluid != null, "No fluid access on the " + side + " face with a Fluid Pump installed");
            check(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, absolute, side) != null, "No item access on the " + side + " face");
        }
        for (Direction side : new Direction[] {Direction.UP, Direction.DOWN}) {
            IFluidHandler fluid = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, absolute, side);
            int filled = fluid.fill(new FluidStack(ModFluids.ELECTROLYTE_SOLUTION_SOURCE.get(), 100), IFluidHandler.FluidAction.EXECUTE);
            check(filled > 0, "Electrolyte Solution did not fill through the " + side + " face");
        }
        helper.succeedWhen(() -> check(above.machineInfo().energy() > 0,
                "The machine on top got no FE from the cell; " + MachineTestSupport.describe(cell)));
    }

    private static CorrosionCellBlockEntity loadedCell(GameTestHelper helper, BlockPos pos, int load) {
        CorrosionCellBlockEntity cell = MachineTestSupport.place(helper, pos, ModBlocks.CORROSION_CELL.get(), CorrosionCellBlockEntity.class);
        cell.setLoad(load);
        ItemStack plate = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(IRON_PLATE)));
        cell.getProcessInventory().setStackInSlot(CorrosionCellBlockEntity.SLOT_PLATE, plate);
        cell.getProcessInventory().setStackInSlot(CorrosionCellBlockEntity.SLOT_ELECTROLYTE, new ItemStack(ModItems.materialItem("electrolyte").get()));
        return cell;
    }

    /** Drains one cell every tick and times its single cycle from the first dissolving tick to the residue. */
    private static final class Run {
        private final CorrosionCellBlockEntity cell;
        private final IEnergyStorage storage;
        private long energy;
        private int activeTicks;
        private int expectedTicks;
        private int expectedPerTick;
        private boolean done;

        private Run(CorrosionCellBlockEntity cell) {
            this.cell = cell;
            storage = cell.getEnergyStorage(Direction.NORTH);
        }

        private void tick() {
            energy += storage.extractEnergy(Integer.MAX_VALUE, false);
            if (done) {
                return;
            }
            if (!cell.getProcessInventory().getStackInSlot(CorrosionCellBlockEntity.SLOT_RESIDUE).isEmpty()) {
                done = true;
                return;
            }
            if (!cell.currentVisualState().fuel().isEmpty()) {
                if (activeTicks == 0) {
                    expectedTicks = cell.machineInfo().progressMax();
                    expectedPerTick = (int) cell.machineInfo().energyRate();
                }
                activeTicks++;
            }
        }

        private boolean finished() {
            return done;
        }
    }
}
