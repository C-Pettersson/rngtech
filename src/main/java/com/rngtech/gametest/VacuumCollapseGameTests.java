package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.VacuumCollapseGeneratorBlockEntity;
import com.rngtech.content.blockentity.VacuumCollapseVisualState;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Vacuum Collapse Generator's synced visual state: installed Gear materials and the running collapse. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VacuumCollapseGameTests {
    private static final BlockPos GENERATOR = new BlockPos(2, 1, 2);

    private VacuumCollapseGameTests() {
    }

    /**
     * Clients learn each installed part's material and the collapse's progress and instability; a removed part leaves
     * its mount empty. The Titanium nozzle is a stage below Void Catalyst, which adds a full point of instability.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void visualStateShowsInstalledGearAndTheCollapse(GameTestHelper helper) {
        VacuumCollapseGeneratorBlockEntity generator = MachineTestSupport.place(
                helper, GENERATOR, ModBlocks.VACUUM_COLLAPSE_GENERATOR.get(), VacuumCollapseGeneratorBlockEntity.class);
        generator.getGearInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_VOID_CHAMBER,
                new ItemStack(ModItems.voidChamber(VacuumCollapsePartMaterial.TUNGSTENSTEEL).get()));
        generator.getGearInventory().setStackInSlot(VacuumCollapseGeneratorBlockEntity.SLOT_COLLAPSE_NOZZLE,
                new ItemStack(ModItems.collapseNozzle(CollapseNozzleMaterial.TITANIUM).get()));
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

    private static VacuumCollapseVisualState visual(GameTestHelper helper, VacuumCollapseGeneratorBlockEntity generator) {
        return VacuumCollapseVisualState.load(generator.getUpdateTag(helper.getLevel().registryAccess()));
    }
}
