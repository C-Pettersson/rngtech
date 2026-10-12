package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.VacuumCollapseDynamics;
import com.rngtech.content.blockentity.VacuumCollapseGeneratorBlockEntity;
import com.rngtech.content.blockentity.VacuumCollapseVisualState;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** The Vacuum Collapse Generator: the collapse curve, collapse depth, and the synced visual state. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VacuumCollapseGameTests {
    private static final BlockPos GENERATOR = new BlockPos(2, 1, 2);
    private static final int[] DEPTHS = {VacuumCollapseDynamics.RATED_DEPTH, VacuumCollapseDynamics.MAX_DEPTH, VacuumCollapseDynamics.MIN_DEPTH};

    private VacuumCollapseGameTests() {
    }

    /**
     * One catalyst at each depth, drained every tick so nothing is vented. Each collapse climbs from a low start to a
     * peak at its end and delivers all of its FE; a deep collapse draws about 1.25x the rated FE from its catalyst over
     * a longer cycle, and a shallow one about two thirds over a shorter cycle.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 2400)
    public static void collapsesClimbToTheirPeakAndDepthTradesFePerCatalyst(GameTestHelper helper) {
        VacuumCollapseGeneratorBlockEntity generator = tungstensteelGenerator(helper, CollapseNozzleMaterial.TUNGSTENSTEEL);
        IEnergyStorage storage = generator.getEnergyStorage(Direction.NORTH);
        List<Long> perTick = new ArrayList<>();
        List<Long> totals = new ArrayList<>();
        List<Integer> durations = new ArrayList<>();
        int[] collapse = {0};
        boolean[] fed = {false};
        helper.onEachTick(() -> {
            if (!fed[0]) {
                generator.setDepth(DEPTHS[collapse[0]]);
                generator.getProcessInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_INPUT, new ItemStack(ModItems.VOID_CATALYST.get()));
                fed[0] = true;
                return;
            }
            perTick.add((long) storage.extractEnergy(Integer.MAX_VALUE, false));
            if (generator.getProcessInventory().getStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_RESIDUE).isEmpty()) {
                return;
            }
            perTick.add((long) storage.extractEnergy(Integer.MAX_VALUE, false));
            checkCurve(DEPTHS[collapse[0]], perTick);
            totals.add(perTick.stream().mapToLong(Long::longValue).sum());
            durations.add(perTick.size());
            perTick.clear();
            generator.getProcessInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_RESIDUE, ItemStack.EMPTY);
            fed[0] = false;
            if (++collapse[0] < DEPTHS.length) {
                return;
            }
            double deep = totals.get(1) / (double) totals.get(0);
            double shallow = totals.get(2) / (double) totals.get(0);
            check(deep > 1.2 && deep < 1.3, "A 150% collapse drew " + deep + "x the rated FE per catalyst");
            check(shallow > 0.6 && shallow < 0.7, "A 50% collapse drew " + shallow + "x the rated FE per catalyst");
            check(durations.get(1) > durations.get(0) && durations.get(2) < durations.get(0),
                    "Deeper collapses should run longer: " + durations);
            helper.succeed();
        });
    }

    /** The first tenth of a collapse runs well below its average and the last tenth well above it. */
    private static void checkCurve(int depth, List<Long> perTick) {
        int tenth = Math.max(1, perTick.size() / 10);
        double average = perTick.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double start = perTick.subList(0, tenth).stream().mapToLong(Long::longValue).average().orElse(0.0);
        double end = perTick.subList(perTick.size() - tenth, perTick.size()).stream().mapToLong(Long::longValue).average().orElse(0.0);
        check(start < average * 0.8, depth + "% collapse started at " + start + " FE/t against an average of " + average);
        check(end > average * 1.2, depth + "% collapse ended at " + end + " FE/t against an average of " + average);
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
