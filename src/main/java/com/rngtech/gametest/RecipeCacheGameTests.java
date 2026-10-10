package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.blockentity.RecipeCache;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Remembered recipe lookups: no search while the input stays the same, found or not, and a new search on any change. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeCacheGameTests {
    private static final int RUN_TICKS = 300;

    private RecipeCacheGameTests() {
    }

    /** A running Crusher read like an open menu only searches when a craft uses up input. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = RUN_TICKS + 100)
    public static void runningCrusherSearchesOnlyWhenItsInputChanges(GameTestHelper helper) {
        CrusherBlockEntity crusher = crusher(helper);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.RAW_IRON, 64));
        MachineTestSupport.powerEachTick(helper, crusher.getEnergyStorage(null));
        int[] last = {crusher.recipeSearches(), inputCount(crusher)};
        int[] crafts = new int[1];
        helper.onEachTick(() -> {
            readLikeAnOpenMenu(crusher);
            int searches = crusher.recipeSearches();
            int input = inputCount(crusher);
            if (input != last[1]) {
                crafts[0]++;
            } else if (helper.getTick() > 1) {
                check(searches == last[0], "Searched recipes at tick " + helper.getTick() + " with an unchanged input");
            }
            last[0] = searches;
            last[1] = input;
        });
        helper.runAtTickTime(RUN_TICKS, () -> {
            check(crafts[0] >= 2, "Expected at least two crafts, saw " + crafts[0]);
            helper.succeed();
        });
    }

    /** A Crusher holding an item with no recipe stops searching, and finds the recipe as soon as the input changes. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 300)
    public static void crusherRemembersAFailedLookup(GameTestHelper helper) {
        CrusherBlockEntity crusher = crusher(helper);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.BEDROCK));
        MachineTestSupport.powerEachTick(helper, crusher.getEnergyStorage(null));
        int[] settled = new int[1];
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> settled[0] = crusher.recipeSearches())
                .thenExecuteFor(100, () -> {
                    readLikeAnOpenMenu(crusher);
                    check(crusher.recipeSearches() == settled[0], "Searched again for an input with no recipe");
                })
                .thenExecute(() -> crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.RAW_IRON, 4)))
                .thenWaitUntil(() -> check(crusher.machineInfo().progress() > 0, "The new input never started crushing"))
                .thenSucceed();
    }

    /** Switching Molds picks up the other Mold's recipe, and a recipe reload searches again. */
    @GameTest(template = MachineTestSupport.EMPTY, batch = "recipe_cache_reload")
    public static void metalPressFollowsTheMoldAndReloads(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, new BlockPos(2, 1, 2), ModBlocks.CRUDE_METAL_PRESS.get(), MetalPressBlockEntity.class);
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD, new ItemStack(ModItems.PLATE_MOLD.get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD + 1, new ItemStack(ModItems.GEAR_MOLD.get()));
        press.getProcessInventory().setStackInSlot(MetalPressBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 3));

        int plateTicks = press.machineInfo().progressMax();
        check(plateTicks > 0, "No plate recipe for an iron ingot");
        int searches = press.recipeSearches();
        press.machineInfo();
        check(press.recipeSearches() == searches, "Searched again with nothing changed");

        press.selectMold(1);
        int gearTicks = press.machineInfo().progressMax();
        check(gearTicks > plateTicks, "Selecting the Gear Mold kept the plate recipe (" + gearTicks + " ticks)");

        searches = press.recipeSearches();
        RecipeCache.invalidateAll();
        check(press.machineInfo().progressMax() == gearTicks, "A reload changed the recipe");
        check(press.recipeSearches() == searches + 1, "A reload did not search again");
        helper.succeed();
    }

    private static CrusherBlockEntity crusher(GameTestHelper helper) {
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        return crusher;
    }

    private static void readLikeAnOpenMenu(CrusherBlockEntity crusher) {
        for (int index = 0; index < crusher.getMenuData().getCount(); index++) {
            crusher.getMenuData().get(index);
        }
        crusher.machineInfo();
    }

    private static int inputCount(CrusherBlockEntity crusher) {
        return crusher.getInventory().getStackInSlot(CrusherBlockEntity.SLOT_INPUT_A).getCount();
    }
}
