package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.blockentity.RecipeCache;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Remembered recipe lookups: no search while the input stays the same, found or not, and a new search on any change. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeCacheGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int RUN_TICKS = 300;
    private static final int EMPTY_TICKS = 5;

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

    /** Every machine that caches lookups does no recipe search while it stands empty. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 4000)
    public static void emptyMachinesNeverSearch(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        List<String> checked = new ArrayList<>();
        GameTestSequence sequence = helper.startSequence();
        for (Block block : machineBlocks()) {
            sequence.thenExecute(() -> {
                        helper.setBlock(pos, Blocks.AIR);
                        helper.setBlock(pos, block);
                    })
                    .thenIdle(EMPTY_TICKS)
                    .thenExecute(() -> {
                        BlockEntity machine = helper.getBlockEntity(pos);
                        int searches = recipeSearches(machine);
                        if (searches >= 0) {
                            check(searches == 0, BuiltInRegistries.BLOCK.getKey(block) + " searched recipes " + searches + " times while empty");
                            checked.add(BuiltInRegistries.BLOCK.getKey(block).getPath());
                        }
                    });
        }
        sequence.thenExecute(() -> {
            LOGGER.info("Empty machines: {} cache recipe lookups and did not search: {}", checked.size(), checked);
            check(checked.size() >= 15, "Only checked " + checked);
        }).thenSucceed();
    }

    /** A Component Recycler stops searching for an input with no recipe, and finds the recipe when the input changes. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void recyclerRemembersAFailedLookup(GameTestHelper helper) {
        ComponentRecyclerBlockEntity recycler = recycler(helper);
        recycler.getProcessInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_INPUT, new ItemStack(Items.BEDROCK));
        MachineTestSupport.powerEachTick(helper, recycler.getEnergyStorage(null));
        int[] settled = new int[1];
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> settled[0] = recycler.recipeSearches())
                .thenExecuteFor(100, () -> {
                    readLikeAnOpenMenu(recycler);
                    check(recycler.recipeSearches() == settled[0], "Searched again for an input with no recipe");
                })
                .thenExecute(() -> recycler.getProcessInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_INPUT, new ItemStack(brokenCircuit(), 4)))
                .thenWaitUntil(() -> check(recycler.machineInfo().progress() > 0, "The new input never started recycling"))
                .thenSucceed();
    }

    /** A recipe reload makes an idle machine with a cached lookup search again, once. */
    @GameTest(template = MachineTestSupport.EMPTY, batch = "recipe_cache_reload")
    public static void recyclerSearchesAgainAfterAReload(GameTestHelper helper) {
        ComponentRecyclerBlockEntity recycler = recycler(helper);
        recycler.getProcessInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_INPUT, new ItemStack(brokenCircuit(), 4));
        int ticks = recycler.machineInfo().progressMax();
        check(ticks > 0, "No recycling recipe for a Broken Circuit");
        int searches = recycler.recipeSearches();
        recycler.machineInfo();
        check(recycler.recipeSearches() == searches, "Searched again with nothing changed");
        RecipeCache.invalidateAll();
        check(recycler.machineInfo().progressMax() == ticks, "A reload changed the recipe");
        check(recycler.recipeSearches() == searches + 1, "A reload did not search again");
        helper.succeed();
    }

    private static ComponentRecyclerBlockEntity recycler(GameTestHelper helper) {
        ComponentRecyclerBlockEntity recycler = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.componentRecycler(ComponentRecyclerChassis.IRON).get(), ComponentRecyclerBlockEntity.class);
        recycler.getGearInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD,
                new ItemStack(ModItems.disassemblyHead(DisassemblyHeadMaterial.IRON).get()));
        return recycler;
    }

    private static void readLikeAnOpenMenu(ComponentRecyclerBlockEntity recycler) {
        for (int index = 0; index < recycler.getMenuData().getCount(); index++) {
            recycler.getMenuData().get(index);
        }
        recycler.machineInfo();
    }

    private static Item brokenCircuit() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "broken_circuit"));
    }

    private static List<Block> machineBlocks() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(RNGTech.MOD_ID))
                .filter(block -> block instanceof EntityBlock)
                .sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
                .toList();
    }

    /** The machine's {@code recipeSearches()}, or -1 when it does not cache recipe lookups. */
    private static int recipeSearches(BlockEntity machine) {
        if (machine == null) {
            return -1;
        }
        try {
            Method method = machine.getClass().getMethod("recipeSearches");
            return (int) method.invoke(machine);
        } catch (NoSuchMethodException exception) {
            return -1;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
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
