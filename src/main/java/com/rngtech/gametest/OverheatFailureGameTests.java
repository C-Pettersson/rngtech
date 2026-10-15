package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * A metal smelt pushed far past its overheat temperature fails, even when overheating makes each cycle short: strain
 * carries from cycle to cycle. Without Blast Draft the slider stops at the safe maximum, so the same lane never fails.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OverheatFailureGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final int DUST = 32;

    private OverheatFailureGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 4000)
    public static void overheatedTinDustBurnsToMalformedIngots(GameTestHelper helper) {
        FurnaceBlockEntity furnace = overheatedTinFurnace(helper, true);
        int[] drained = drainOutput(helper, furnace);
        helper.succeedWhen(() -> check(drained[1] > 0,
                "No malformed ingot after " + drained[0] + " tin ingots; " + MachineTestSupport.describe(furnace)));
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 4000)
    public static void withoutBlastDraftTheSliderCannotOverheatTinDust(GameTestHelper helper) {
        FurnaceBlockEntity furnace = overheatedTinFurnace(helper, false);
        int[] drained = drainOutput(helper, furnace);
        helper.succeedWhen(() -> {
            check(drained[1] == 0, "The safe maximum ruined " + drained[1] + " ingots");
            check(drained[0] >= 8, "Only " + drained[0] + " tin ingots so far; " + MachineTestSupport.describe(furnace));
        });
    }

    /** Empties the output every tick, as automation would, counting good ingots [0] and malformed ingots [1]. */
    private static int[] drainOutput(GameTestHelper helper, FurnaceBlockEntity furnace) {
        int[] drained = new int[2];
        helper.onEachTick(() -> {
            ItemStack stack = output(furnace);
            if (!stack.isEmpty()) {
                drained[stack.is(ModItems.MALFORMED_INGOT.get()) ? 1 : 0] += stack.getCount();
                furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START, ItemStack.EMPTY);
            }
        });
        return drained;
    }

    private static FurnaceBlockEntity overheatedTinFurnace(GameTestHelper helper, boolean blastDraft) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.TUNGSTENSTEEL).get(), FurnaceBlockEntity.class);
        furnace.getGearInventory().setStackInSlot(0, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        if (blastDraft) {
            String start = MachineMasteryFamily.FURNACE.startNodeId();
            List<String> path = MegaPassiveTree.allocationPath(node -> node.id().equals(start), MegaPassiveTree.node("blast_draft"))
                    .stream().map(MegaPassiveNode::id).toList();
            check(!path.isEmpty(), "Blast Draft is unreachable from the Furnace start");
            furnace.setMachineProgression(new MachineProgressionState(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL), 0,
                    MachineProgressionState.MAX_LEVEL, path, start, List.of(), false));
        }
        furnace.setHeatSlider(100);
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START,
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "tin_dust")), DUST));
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));
        return furnace;
    }

    private static ItemStack output(FurnaceBlockEntity furnace) {
        return furnace.getInventory().getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START);
    }
}
