package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.recipe.TaggedRecipeOutput;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Vanilla grinding and tag-result recipes: a tag result picks a vanilla item, and a compat recipe loads only when a mod adds its dust. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrusherCompatRecipeGameTests {
    private CrusherCompatRecipeGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void tagResultPrefersTheVanillaItem(GameTestHelper helper) {
        TaggedRecipeOutput output = TaggedRecipeOutput.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"tag\": \"c:dusts/redstone\", \"count\": 2}"))
                .getOrThrow();
        ItemStack stack = output.stack();
        check(stack.is(Items.REDSTONE) && stack.getCount() == 2, "Expected 2 Redstone, got " + stack);
        TaggedRecipeOutput empty = TaggedRecipeOutput.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"tag\": \"rngtech:gametest/no_such_tag\"}"))
                .getOrThrow();
        check(empty.stack().isEmpty(), "Expected an empty tag to resolve to nothing, got " + empty.stack());
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void compatRecipeLoadsOnlyWithADust(GameTestHelper helper) {
        TagKey<Item> dusts = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:dusts/quartz"));
        boolean hasDust = BuiltInRegistries.ITEM.getTag(dusts).map(set -> set.size() > 0).orElse(false);
        var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHER_TYPE.get()).stream()
                .filter(holder -> holder.id().equals(RNGTech.id("crusher/compat/quartz_dust")))
                .findFirst();
        check(recipe.isPresent() == hasDust, "Quartz dust recipe loaded: " + recipe.isPresent() + ", c:dusts/quartz has items: " + hasDust);
        recipe.ifPresent(holder -> check(holder.value().result().is(dusts), "Quartz dust recipe made " + holder.value().result()));
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void crusherGrindsCobblestoneIntoGravel(GameTestHelper helper) {
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.COBBLESTONE, 4));
        MachineTestSupport.powerEachTick(helper, crusher.getEnergyStorage(null));
        helper.succeedWhen(() -> check(crusher.getInventory().getStackInSlot(CrusherBlockEntity.SLOT_OUTPUT).is(Items.GRAVEL),
                "Expected Gravel in the output slot"));
    }
}
