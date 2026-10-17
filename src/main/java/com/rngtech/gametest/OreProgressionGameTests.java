package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.RNGTechConfig;
import com.rngtech.RNGTechConfig.RestrictedToolMode;
import com.rngtech.content.recipe.CrusherRecipe;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.content.tool.OreHardness;
import com.rngtech.content.tool.RestrictedTools;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Ore hardness follows common ore tags, Crusher ore recipes match any mod's ore, and restricted tools lose their reach. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OreProgressionGameTests {
    private OreProgressionGameTests() {
    }

    /** Nether Gold Ore is listed by no RNGTech file; it inherits Gold's hardness through {@code c:ores/gold}. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void commonOreTagsCarryHardness(GameTestHelper helper) {
        check(OreHardness.level(Blocks.NETHER_GOLD_ORE.defaultBlockState()) == 2, "Nether Gold Ore should share Gold's hardness 2");
        check(OreHardness.level(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState()) == 1, "Deepslate Iron Ore should keep hardness 1");
        check(OreHardness.level(Blocks.STONE.defaultBlockState()) == 0, "Stone should have no ore hardness");
        helper.succeed();
    }

    /** Stone and deepslate ore recipes match by common tag and never both match one ore. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void crusherOreRecipesMatchByTag(GameTestHelper helper) {
        List<RecipeHolder<CrusherRecipe>> recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHER_TYPE.get());
        checkSingleRecipe(recipes, new ItemStack(Items.IRON_ORE), "crusher/iron_ore");
        checkSingleRecipe(recipes, new ItemStack(Items.DEEPSLATE_IRON_ORE), "crusher/deepslate_iron_ore");
        checkSingleRecipe(recipes, new ItemStack(Items.NETHER_GOLD_ORE), "crusher/gold_ore");
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void pickaxeTierIsReadFromHarvestableBlocks(GameTestHelper helper) {
        check(OreHardness.vanillaPickTier(new ItemStack(Items.WOODEN_PICKAXE)) == 0, "A Wooden Pickaxe should read as no tier");
        check(OreHardness.vanillaPickTier(new ItemStack(Items.GOLDEN_PICKAXE)) == 0, "A Golden Pickaxe should read as no tier");
        check(OreHardness.vanillaPickTier(new ItemStack(Items.STONE_PICKAXE)) == 1, "A Stone Pickaxe should read as stone tier");
        check(OreHardness.vanillaPickTier(new ItemStack(Items.IRON_PICKAXE)) == 2, "An Iron Pickaxe should read as iron tier");
        check(OreHardness.vanillaPickTier(new ItemStack(Items.DIAMOND_PICKAXE)) == 3, "A Diamond Pickaxe should read as diamond tier");
        check(OreHardness.vanillaPickTier(new ItemStack(Items.NETHERITE_PICKAXE)) == 4, "A Netherite Pickaxe should read as netherite tier");
        check(OreHardness.vanillaPickTier(new ItemStack(Items.DIAMOND_SHOVEL)) == 0, "A shovel should read as no tier");
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void uselessRestrictedToolsReachNoOre(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        RestrictedToolMode previous = RNGTechConfig.RESTRICTED_TOOLS_MODE.get();
        try {
            RNGTechConfig.RESTRICTED_TOOLS_MODE.set(RestrictedToolMode.USELESS);
            check(RestrictedTools.useless(pickaxe), "An Iron Pickaxe should be restricted by default");
            check(OreHardness.oreReach(pickaxe) == 0, "A useless restricted pickaxe should reach no ore");
            check(!RestrictedTools.useless(new ItemStack(Items.IRON_HOE)), "Hoes should not be restricted by default");
            check(!RestrictedTools.useless(new ItemStack(Items.WOODEN_PICKAXE)), "Wooden tools should not be restricted by default");
        } finally {
            RNGTechConfig.RESTRICTED_TOOLS_MODE.set(previous);
        }
        helper.succeed();
    }

    private static void checkSingleRecipe(List<RecipeHolder<CrusherRecipe>> recipes, ItemStack ore, String expectedPath) {
        List<ResourceLocation> matches = recipes.stream()
                .filter(holder -> holder.value().ingredient().test(ore))
                .map(RecipeHolder::id)
                .toList();
        check(matches.equals(List.of(RNGTech.id(expectedPath))), ore.getItem() + " should match only " + expectedPath + ", matched " + matches);
    }
}
