package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.minerscompanion.MinersCompanionRecoveryRecipes;
import com.rngtech.content.recipe.MinersCompanionRecoveryRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** The default trace recovery recipes: each raw metal needs a Recovery Filter and Crush Head of its own stage. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MinersCompanionRecoveryGameTests {
    private MinersCompanionRecoveryGameTests() {
    }

    /** An Iron Filter with a Flint Head recovers only Raw Iron from cobblestone; a Bronze setup adds copper, tin, and zinc. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void stoneRecoveryFollowsMetalStage(GameTestHelper helper) {
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        Set<Item> early = results(helper, cobblestone, 1, 1);
        check(early.equals(Set.of(Items.RAW_IRON)), "An Iron Filter and Flint Head should recover only Raw Iron, got " + early);
        Set<Item> bronze = results(helper, cobblestone, 3, 3);
        check(bronze.equals(Set.of(Items.RAW_IRON, Items.RAW_COPPER, item("raw_tin"), item("raw_zinc"))),
                "A Bronze Filter and Head should add copper, tin, and zinc, got " + bronze);
        check(!results(helper, cobblestone, 4, 3).contains(item("raw_lead")), "A Bronze Head should not reach Stage 4 Raw Lead");
        helper.succeed();
    }

    /** The Steel Filter caps the filter gate, so the deepest metals wait on the Crush Head alone. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void deepslateNaquadahNeedsExoticHead(GameTestHelper helper) {
        ItemStack deepslate = new ItemStack(Items.COBBLED_DEEPSLATE);
        check(!results(helper, deepslate, 4, 7).contains(item("raw_naquadah")), "A Stage 7 Head should not recover Raw Naquadah");
        Set<Item> exotic = results(helper, deepslate, 4, 8);
        check(exotic.contains(item("raw_naquadah")) && exotic.size() == 12,
                "A Steel Filter and Exotic Head should reach all 12 deepslate metals, got " + exotic);
        helper.succeed();
    }

    /** Nether rock and sediment have their own metals; dirt is not a recovery rock, so chewing it never rolls. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void otherRocksAndDirt(GameTestHelper helper) {
        check(results(helper, new ItemStack(Items.NETHERRACK), 1, 1).equals(Set.of(Items.QUARTZ)),
                "Netherrack should recover Nether Quartz with an Iron Filter");
        check(results(helper, new ItemStack(Items.RED_SAND), 3, 3).equals(Set.of(item("raw_tin"), Items.RAW_GOLD)),
                "Sand should recover tin and gold placers with a Bronze setup");
        check(results(helper, new ItemStack(Items.DIRT), 4, 8).isEmpty(), "Dirt should have no recovery recipe");
        helper.succeed();
    }

    private static Set<Item> results(GameTestHelper helper, ItemStack chewed, int filterStage, int processingLevel) {
        List<RecipeHolder<MinersCompanionRecoveryRecipe>> recipes =
                MinersCompanionRecoveryRecipes.eligibleRecipes(helper.getLevel(), chewed, filterStage, processingLevel);
        return recipes.stream().map(holder -> holder.value().result().getItem()).collect(Collectors.toSet());
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(RNGTech.id(id));
    }
}
