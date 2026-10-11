package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.MachineStatsCache;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Cached machine stats: built once per change, never stale, and still explained by stat breakdowns. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StatsCacheGameTests {
    private static final int RUN_TICKS = 400;

    private StatsCacheGameTests() {
    }

    /** A running Crusher, read every tick like an open menu, only rebuilds its stats on the ticks a craft completes. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = RUN_TICKS + 100)
    public static void runningCrusherRebuildsStatsOnlyWhenACraftCompletes(GameTestHelper helper) {
        CrusherBlockEntity crusher = crusher(helper, new BlockPos(2, 1, 2));
        crusher.setMachineTraits(MachineTestSupport.BULK_SPEED);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.RAW_IRON, 64));
        MachineTestSupport.powerEachTick(helper, crusher.getEnergyStorage(null));
        int[] last = {crusher.statsBuilds(), outputCount(crusher)};
        int[] craftTicks = new int[1];
        helper.onEachTick(() -> {
            readLikeAnOpenMenu(crusher);
            int builds = crusher.statsBuilds();
            int output = outputCount(crusher);
            if (output != last[1]) {
                craftTicks[0]++;
            } else if (helper.getTick() > 1) {
                check(builds == last[0], "Rebuilt stats " + (builds - last[0]) + " times at tick " + helper.getTick() + " with nothing changed");
            }
            last[0] = builds;
            last[1] = output;
        });
        helper.runAtTickTime(RUN_TICKS, () -> {
            check(craftTicks[0] >= 2, "Expected at least two crafts, saw " + craftTicks[0]);
            check(crusher.statsBuilds() <= 2 + 2 * craftTicks[0], "Built stats " + crusher.statsBuilds() + " times for " + craftTicks[0] + " crafts");
            assertMatchesFreshBuild(crusher, "after crafting");
            helper.succeed();
        });
    }

    /**
     * Gear, battery, traits, Mastery, and a reload each update the stats at once, and breakdowns still list sources.
     * A reload rebuilds every machine's stats, so this test runs in its own batch.
     */
    @GameTest(template = MachineTestSupport.EMPTY, batch = "stats_cache_reload")
    public static void cachedStatsFollowEveryInput(GameTestHelper helper) {
        CrusherBlockEntity crusher = crusher(helper, new BlockPos(2, 1, 2));
        MachineStatAccumulator first = crusher.effectiveStats();
        check(crusher.effectiveStats() == first, "Unchanged stats were rebuilt");
        assertMatchesFreshBuild(crusher, "after placing");

        double ironLevel = first.value(MachineStat.PROCESSING_LEVEL);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.FLINT).get()));
        check(crusher.effectiveStats().value(MachineStat.PROCESSING_LEVEL) < ironLevel, "Crush Head swap kept the old Processing Level");
        assertMatchesFreshBuild(crusher, "after a Crush Head swap");

        double withoutBattery = crusher.effectiveStats().value(MachineStat.OUTPUT_AMOUNT);
        ItemStack cell = new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get());
        check(crusher.canInstallBatteryCell(cell), "Crusher rejects the Battery Cell");
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_FUEL, cell);
        check(crusher.effectiveStats().value(MachineStat.OUTPUT_AMOUNT) > withoutBattery, "Battery Cell kept the no-battery penalty");
        assertMatchesFreshBuild(crusher, "after installing a Battery Cell");

        double speed = crusher.effectiveStats().value(MachineStat.PROCESSING_SPEED);
        crusher.setMachineTraits(new MachineTraits(Rarity.MAGIC, 0, List.of(new MachineModifier(
                ModifierSlot.SUFFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 50.0)), List.of(MachineBehavior.BULK_SPEED)));
        check(crusher.effectiveStats().value(MachineStat.PROCESSING_SPEED) > speed, "New traits kept the old Processing Speed");
        assertMatchesFreshBuild(crusher, "after new traits");

        int builds = crusher.statsBuilds();
        crusher.setMachineProgression(crusher.machineProgression().withAddedXp(1));
        crusher.effectiveStats();
        check(crusher.statsBuilds() == builds + 1, "New Mastery progression did not rebuild the stats");

        MachineStatsCache.invalidateAll();
        crusher.effectiveStats();
        check(crusher.statsBuilds() == builds + 2, "A config or tag reload did not rebuild the stats");

        MachineStatAccumulator recorded = MachineStatAccumulator.recording(crusher::effectiveStats);
        check(recorded != crusher.effectiveStats(), "A breakdown reused the cached stats");
        check(!recorded.breakdowns().isEmpty(), "A breakdown lost its sources");
        check(crusher.effectiveStats().mutable() != crusher.effectiveStats(), "Cached stats were handed out for changes");
        helper.succeed();
    }

    private static CrusherBlockEntity crusher(GameTestHelper helper, BlockPos pos) {
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, pos, ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        return crusher;
    }

    private static void readLikeAnOpenMenu(CrusherBlockEntity crusher) {
        for (int index = 0; index < crusher.getMenuData().getCount(); index++) {
            crusher.getMenuData().get(index);
        }
        crusher.machineInfo();
    }

    /** Every stat of the cached build equals a fresh one. */
    private static void assertMatchesFreshBuild(CrusherBlockEntity crusher, String when) {
        MachineStatAccumulator cached = crusher.effectiveStats();
        MachineStatAccumulator fresh = MachineStatAccumulator.recording(crusher::effectiveStats);
        for (MachineStat stat : MachineStat.values()) {
            check(Double.compare(cached.value(stat), fresh.value(stat)) == 0,
                    "Cached " + stat + " was " + cached.value(stat) + " but a fresh build gives " + fresh.value(stat) + " " + when);
        }
    }

    private static int outputCount(CrusherBlockEntity crusher) {
        return crusher.getInventory().getStackInSlot(CrusherBlockEntity.SLOT_OUTPUT).getCount();
    }
}
