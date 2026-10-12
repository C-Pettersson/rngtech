package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.RecipeCache;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Automated input never jams a machine, including on the crafting remainders its ingredients leave behind. No default
 * recipe takes such an ingredient, so the batch adds pack-maker recipes that do and restores the recipes afterwards.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AutomationInputGameTests {
    private static final String PACK_RECIPES = "automation_remainders";
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final int CRAFT_TIMEOUT = 2400;
    private static List<RecipeHolder<?>> defaultRecipes = List.of();

    private AutomationInputGameTests() {
    }

    @BeforeBatch(batch = PACK_RECIPES)
    public static void addPackRecipes(ServerLevel level) {
        RecipeManager recipes = level.getRecipeManager();
        defaultRecipes = List.copyOf(recipes.getRecipes());
        List<RecipeHolder<?>> withPackRecipes = new ArrayList<>(defaultRecipes);
        withPackRecipes.add(packRecipe(level, "honey_alloy", alloyRecipe("minecraft:honey_bottle", "minecraft:redstone")));
        withPackRecipes.add(packRecipe(level, "water_alloy", alloyRecipe("minecraft:water_bucket", "minecraft:redstone")));
        withPackRecipes.add(packRecipe(level, "bucket_alloy", alloyRecipe("minecraft:bucket", "minecraft:nether_star")));
        withPackRecipes.add(packRecipe(level, "honey_assembly", assemblyRecipe("minecraft:honey_bottle", "minecraft:redstone")));
        recipes.replaceRecipes(withPackRecipes);
        RecipeCache.invalidateAll();
    }

    @AfterBatch(batch = PACK_RECIPES)
    public static void restoreRecipes(ServerLevel level) {
        level.getRecipeManager().replaceRecipes(defaultRecipes);
        RecipeCache.invalidateAll();
    }

    /** Glass Bottles left by Honey Bottles go into the hopper below, and no input slot keeps one. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = CRAFT_TIMEOUT, batch = PACK_RECIPES)
    public static void alloyFurnacePassesRemaindersToTheInventoryBelow(GameTestHelper helper) {
        helper.setBlock(MACHINE.below(), Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getBlockEntity(MACHINE.below());
        AlloyFurnaceBlockEntity furnace = alloyFurnace(helper, new ItemStack(Items.HONEY_BOTTLE, 2), new ItemStack(Items.REDSTONE, 2));
        helper.startSequence()
                .thenWaitUntil(() -> check(count(hopper, Items.GLASS_BOTTLE) == 2,
                        "Expected 2 Glass Bottles in the hopper, found " + count(hopper, Items.GLASS_BOTTLE) + "; "
                                + MachineTestSupport.describe(furnace)))
                .thenExecute(() -> {
                    assertNoInputHolds(furnace.getProcessInventory(), AlloyFurnaceBlockEntity.MAX_INPUT_SLOTS, Items.GLASS_BOTTLE);
                    assertAccepts(furnace.getItemHandler(Direction.UP), new ItemStack(Items.HONEY_BOTTLE));
                })
                .thenSucceed();
    }

    /** With no inventory below, the Glass Bottle drops instead of sitting in the input slot. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = CRAFT_TIMEOUT, batch = PACK_RECIPES)
    public static void alloyFurnaceDropsRemaindersWithNoInventoryBelow(GameTestHelper helper) {
        AlloyFurnaceBlockEntity furnace = alloyFurnace(helper, new ItemStack(Items.HONEY_BOTTLE), new ItemStack(Items.REDSTONE));
        helper.startSequence()
                .thenWaitUntil(() -> check(furnace.getProcessInventory().getStackInSlot(AlloyFurnaceBlockEntity.SLOT_OUTPUT).is(Items.SUGAR),
                        "The Honey Bottle never alloyed; " + MachineTestSupport.describe(furnace)))
                .thenExecute(() -> {
                    helper.assertItemEntityPresent(Items.GLASS_BOTTLE, MACHINE, 3.0D);
                    assertNoInputHolds(furnace.getProcessInventory(), AlloyFurnaceBlockEntity.MAX_INPUT_SLOTS, Items.GLASS_BOTTLE);
                })
                .thenSucceed();
    }

    /** A Bucket left by a Water Bucket stays in its input slot, since another alloy recipe uses it. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = CRAFT_TIMEOUT, batch = PACK_RECIPES)
    public static void alloyFurnaceKeepsARemainderARecipeUses(GameTestHelper helper) {
        AlloyFurnaceBlockEntity furnace = alloyFurnace(helper, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.REDSTONE));
        helper.startSequence()
                .thenWaitUntil(() -> check(furnace.getProcessInventory().getStackInSlot(AlloyFurnaceBlockEntity.SLOT_OUTPUT).is(Items.SUGAR),
                        "The Water Bucket never alloyed; " + MachineTestSupport.describe(furnace)))
                .thenExecute(() -> {
                    ItemStack kept = furnace.getProcessInventory().getStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START);
                    check(kept.is(Items.BUCKET), "Expected the Bucket back in its input slot, found " + kept);
                    helper.assertItemEntityNotPresent(Items.BUCKET, MACHINE, 3.0D);
                })
                .thenSucceed();
    }

    /** Glass Bottles left by Honey Bottles go into the hopper below, and no input slot keeps one. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = CRAFT_TIMEOUT, batch = PACK_RECIPES)
    public static void batteryAssemblerPassesRemaindersToTheInventoryBelow(GameTestHelper helper) {
        helper.setBlock(MACHINE.below(), Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getBlockEntity(MACHINE.below());
        BatteryAssemblerBlockEntity assembler = MachineTestSupport.place(helper, MACHINE, ModBlocks.BATTERY_ASSEMBLER.get(),
                BatteryAssemblerBlockEntity.class);
        IItemHandler top = assembler.getItemHandler(Direction.UP);
        insertAll(top, new ItemStack(Items.HONEY_BOTTLE, 2));
        insertAll(top, new ItemStack(Items.REDSTONE, 2));
        MachineTestSupport.powerEachTick(helper, assembler.getEnergyStorage(null));
        helper.startSequence()
                .thenWaitUntil(() -> check(count(hopper, Items.GLASS_BOTTLE) == 2,
                        "Expected 2 Glass Bottles in the hopper, found " + count(hopper, Items.GLASS_BOTTLE) + "; "
                                + MachineTestSupport.describe(assembler)))
                .thenExecute(() -> {
                    assertNoInputHolds(assembler.getProcessInventory(), BatteryAssemblerBlockEntity.ITEM_INPUT_SLOT_COUNT, Items.GLASS_BOTTLE);
                    assertAccepts(top, new ItemStack(Items.HONEY_BOTTLE));
                })
                .thenSucceed();
    }

    private static AlloyFurnaceBlockEntity alloyFurnace(GameTestHelper helper, ItemStack first, ItemStack second) {
        AlloyFurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.BRONZE).get(), AlloyFurnaceBlockEntity.class);
        furnace.getGearInventory().setStackInSlot(AlloyFurnaceBlockEntity.SLOT_HEAT_CORE,
                new ItemStack(ModItems.heatCore(HeatCoreMaterial.BRONZE).get()));
        furnace.getGearInventory().setStackInSlot(AlloyFurnaceBlockEntity.SLOT_CRUCIBLE,
                new ItemStack(ModItems.alloyCrucible(AlloyCrucibleMaterial.BRONZE).get()));
        IItemHandler top = furnace.getItemHandler(Direction.UP);
        insertAll(top, first);
        insertAll(top, second);
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));
        return furnace;
    }

    private static String alloyRecipe(String first, String second) {
        return """
                {"type": "rngtech:alloy_furnace", "mode": "blend",
                 "ingredients": [{"ingredient": {"item": "%s"}}, {"ingredient": {"item": "%s"}}],
                 "result": {"id": "minecraft:sugar"}, "processing_ticks": 20, "energy": 200,
                 "minimum_component_stage": 1, "minimum_temperature": 100, "bonus_output": false}
                """.formatted(first, second);
    }

    private static String assemblyRecipe(String first, String second) {
        return """
                {"type": "rngtech:battery_assembly", "ingredients": [{"item": "%s"}, {"item": "%s"}],
                 "result": {"id": "minecraft:sugar"}, "processing_ticks": 20, "energy": 200}
                """.formatted(first, second);
    }

    private static RecipeHolder<?> packRecipe(ServerLevel level, String name, String json) {
        Recipe<?> recipe = Recipe.CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                .getOrThrow();
        return new RecipeHolder<>(RNGTech.id("gametest/" + name), recipe);
    }

    private static void insertAll(IItemHandler handler, ItemStack stack) {
        ItemStack rest = stack;
        for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++) {
            rest = handler.insertItem(slot, rest, false);
        }
        check(rest.isEmpty(), "The top face rejected " + stack);
    }

    private static void assertAccepts(IItemHandler handler, ItemStack stack) {
        ItemStack rest = stack;
        for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++) {
            rest = handler.insertItem(slot, rest, true);
        }
        check(rest.isEmpty(), "The top face rejected another " + stack);
    }

    private static void assertNoInputHolds(IItemHandler inventory, int inputSlots, Item item) {
        for (int slot = 0; slot < inputSlots; slot++) {
            check(!inventory.getStackInSlot(slot).is(item), "Input slot " + slot + " kept " + inventory.getStackInSlot(slot));
        }
    }

    private static int count(HopperBlockEntity hopper, Item item) {
        int count = 0;
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack stack = hopper.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
