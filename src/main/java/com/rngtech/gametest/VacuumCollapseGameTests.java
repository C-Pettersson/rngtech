package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.VacuumCollapseDynamics;
import com.rngtech.content.blockentity.VacuumCollapseGeneratorBlockEntity;
import com.rngtech.content.blockentity.VacuumCollapseVisualState;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
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

import java.util.ArrayList;
import java.util.List;

/** The Vacuum Collapse Generator: the collapse curve, collapse depth, and the synced visual state. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VacuumCollapseGameTests {
    private static final BlockPos GENERATOR = new BlockPos(2, 1, 2);
    private VacuumCollapseGameTests() {
    }

    /**
     * A rated collapse, drained every tick so nothing is vented, climbs from a low start to a peak at its end and
     * delivers exactly the FE it promised when it started.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 2800)
    public static void ratedCollapseClimbsToItsPeakAndDeliversEveryFe(GameTestHelper helper) {
        VacuumCollapseGeneratorBlockEntity generator = tungstensteelGenerator(helper, CollapseNozzleMaterial.TUNGSTENSTEEL);
        IEnergyStorage storage = generator.getEnergyStorage(Direction.NORTH);
        generator.getProcessInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_INPUT, new ItemStack(ModItems.VOID_CATALYST.get()));
        long promised = generator.collapseEnergy();
        List<Long> perTick = new ArrayList<>();
        helper.onEachTick(() -> {
            perTick.add((long) storage.extractEnergy(Integer.MAX_VALUE, false));
            if (generator.getProcessInventory().getStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_RESIDUE).isEmpty()) {
                return;
            }
            perTick.add((long) storage.extractEnergy(Integer.MAX_VALUE, false));
            long delivered = perTick.stream().mapToLong(Long::longValue).sum();
            check(promised > 0L && delivered == promised, "The collapse promised " + promised + " FE and delivered " + delivered);
            int tenth = Math.max(1, perTick.size() / 10);
            double average = delivered / (double) perTick.size();
            double start = perTick.subList(0, tenth).stream().mapToLong(Long::longValue).average().orElse(0.0);
            double end = perTick.subList(perTick.size() - tenth, perTick.size()).stream().mapToLong(Long::longValue).average().orElse(0.0);
            check(start < average * 0.6, "The collapse started at " + start + " FE/t against an average of " + average);
            check(end > average * 1.6, "The collapse ended at " + end + " FE/t against an average of " + average);
            helper.succeed();
        });
    }

    /**
     * Before a catalyst starts, the generator previews the next collapse at the selected depth: deep draws about 1.25x
     * the rated FE from the catalyst over a longer cycle, and shallow about two thirds over a shorter one.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void depthTradesFePerCatalystAgainstCycleLength(GameTestHelper helper) {
        VacuumCollapseGeneratorBlockEntity generator = tungstensteelGenerator(helper, CollapseNozzleMaterial.TUNGSTENSTEEL);
        generator.getProcessInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_INPUT, new ItemStack(ModItems.VOID_CATALYST.get()));
        long[] energy = new long[3];
        int[] ticks = new int[3];
        int[] depths = {VacuumCollapseDynamics.RATED_DEPTH, VacuumCollapseDynamics.MAX_DEPTH, VacuumCollapseDynamics.MIN_DEPTH};
        for (int index = 0; index < depths.length; index++) {
            generator.setDepth(depths[index]);
            energy[index] = generator.collapseEnergy();
            ticks[index] = generator.machineInfo().progressMax();
        }
        double deep = energy[1] / (double) energy[0];
        double shallow = energy[2] / (double) energy[0];
        check(deep > 1.2 && deep < 1.3, "A 150% collapse would draw " + deep + "x the rated FE per catalyst");
        check(shallow > 0.6 && shallow < 0.7, "A 50% collapse would draw " + shallow + "x the rated FE per catalyst");
        check(ticks[1] > ticks[0] && ticks[2] < ticks[0], "Deeper collapses should run longer: " + ticks[0] + ", " + ticks[1] + ", " + ticks[2]);
        helper.succeed();
    }

    /**
     * Clients learn each installed part's material and the collapse's progress and instability; a removed part leaves
     * its mount empty. The Titanium nozzle is a stage below Void Catalyst, which adds a full point of instability.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void visualStateShowsInstalledGearAndTheCollapse(GameTestHelper helper) {
        VacuumCollapseGeneratorBlockEntity generator = tungstensteelGenerator(helper, CollapseNozzleMaterial.TITANIUM);
        generator.getGearInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_DIMENSIONAL_STABILIZER,
                new ItemStack(ModItems.dimensionalStabilizer(VacuumCollapsePartMaterial.NULLITE).get()));

        VacuumCollapseVisualState idle = visual(helper, generator);
        check(!idle.collapsing() && idle.progress() == 0.0F, "An unfed generator should be idle, was " + idle);
        check(idle.chamber().equals("tungstensteel") && idle.nozzle().equals("titanium") && idle.stabilizer().equals("nullite"),
                "Installed Gear should sync its materials, was " + idle);

        generator.getProcessInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_INPUT, new ItemStack(ModItems.VOID_CATALYST.get()));
        boolean[] stabilizerRemoved = {false};
        helper.onEachTick(() -> {
            VacuumCollapseVisualState state = visual(helper, generator);
            if (!stabilizerRemoved[0]) {
                if (state.progress() < 0.1F) {
                    return;
                }
                check(state.collapsing(), "A running collapse should show as collapsing, was " + MachineTestSupport.describe(generator));
                check(state.progress() < 1.0F, "Progress should stay below 1, was " + state.progress());
                check(state.instability() > 1.0F, "An under-stage nozzle should add instability, was " + state.instability());
                generator.getGearInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_DIMENSIONAL_STABILIZER, ItemStack.EMPTY);
                stabilizerRemoved[0] = true;
                return;
            }
            check(state.stabilizer().isEmpty(), "A removed stabilizer should leave its mount empty, was " + state);
            check(state.collapsing() && state.chamber().equals("tungstensteel"), "The collapse should keep running, was " + state);
            helper.succeed();
        });
    }

    /** The Component Assembler takes Ender Slurry into its tank and pours it over a Nullite Ingot and a Tungstensteel Plate. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void componentAssemblerPoursEnderSlurryIntoAVoidCatalyst(GameTestHelper helper) {
        BatteryAssemblerBlockEntity assembler = MachineTestSupport.place(
                helper, GENERATOR, ModBlocks.BATTERY_ASSEMBLER.get(), BatteryAssemblerBlockEntity.class);
        assembler.getProcessInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_0, new ItemStack(ModItems.materialItem("nullite_ingot").get()));
        assembler.getProcessInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_1, new ItemStack(ModItems.materialItem("tungstensteel_plate").get()));
        // The tank's own validator decides which fluids the Assembler accepts; pipes then fill it at the tank's rate.
        int filled = assembler.getInputTank().fill(new FluidStack(ModFluids.ENDER_SLURRY_SOURCE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        check(filled == 1000, "The assembly tank should take Ender Slurry, took " + filled + " mB");
        MachineTestSupport.powerEachTick(helper, assembler.getEnergyStorage(Direction.NORTH));
        helper.succeedWhen(() -> {
            ItemStack output = assembler.getProcessInventory().getStackInSlot(BatteryAssemblerBlockEntity.SLOT_OUTPUT_0);
            check(output.is(ModItems.VOID_CATALYST.get()), "Expected a Void Catalyst, " + MachineTestSupport.describe(assembler));
            check(assembler.getInputTank().isEmpty(), "The assembly should use all 1,000 mB of Ender Slurry");
        });
    }

    private static VacuumCollapseGeneratorBlockEntity tungstensteelGenerator(GameTestHelper helper, CollapseNozzleMaterial nozzle) {
        VacuumCollapseGeneratorBlockEntity generator = MachineTestSupport.place(
                helper, GENERATOR, ModBlocks.VACUUM_COLLAPSE_GENERATOR.get(), VacuumCollapseGeneratorBlockEntity.class);
        generator.getGearInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_VOID_CHAMBER,
                new ItemStack(ModItems.voidChamber(VacuumCollapsePartMaterial.TUNGSTENSTEEL).get()));
        generator.getGearInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_COLLAPSE_NOZZLE,
                new ItemStack(ModItems.collapseNozzle(nozzle).get()));
        generator.getGearInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_DIMENSIONAL_STABILIZER,
                new ItemStack(ModItems.dimensionalStabilizer(VacuumCollapsePartMaterial.TUNGSTENSTEEL).get()));
        return generator;
    }

    private static VacuumCollapseVisualState visual(GameTestHelper helper, VacuumCollapseGeneratorBlockEntity generator) {
        return VacuumCollapseVisualState.load(generator.getUpdateTag(helper.getLevel().registryAccess()));
    }
}
