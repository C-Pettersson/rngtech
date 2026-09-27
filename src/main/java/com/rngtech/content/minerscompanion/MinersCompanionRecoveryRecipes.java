package com.rngtech.content.minerscompanion;

import com.rngtech.content.recipe.MinersCompanionRecoveryRecipe;
import com.rngtech.content.registry.ModRecipes;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public final class MinersCompanionRecoveryRecipes {
    public static final double BASE_RECOVERY_CHANCE = 1.0D / 5000.0D;

    public static ItemStack rollRecovery(
            Level level,
            RandomSource random,
            ItemStack chewedStack,
            int filterStage,
            int processingLevel,
            double filterEfficiency
    ) {
        List<RecipeHolder<MinersCompanionRecoveryRecipe>> eligible = eligibleRecipes(
                level,
                chewedStack,
                filterStage,
                processingLevel
        );
        if (eligible.isEmpty()) {
            return ItemStack.EMPTY;
        }
        double chance = Math.min(1.0D, BASE_RECOVERY_CHANCE * Math.max(0.0D, filterEfficiency));
        if (random.nextDouble() >= chance) {
            return ItemStack.EMPTY;
        }
        return chooseWeighted(eligible, random).copy();
    }

    public static List<RecipeHolder<MinersCompanionRecoveryRecipe>> eligibleRecipes(
            Level level,
            ItemStack chewedStack,
            int filterStage,
            int processingLevel
    ) {
        if (chewedStack.isEmpty()) {
            return List.of();
        }
        SingleRecipeInput input = new SingleRecipeInput(chewedStack);
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.MINERS_COMPANION_RECOVERY_TYPE.get()).stream()
                .filter(holder -> holder.value().matches(input, level))
                .filter(holder -> !holder.value().result().isEmpty())
                .filter(holder -> holder.value().weight() > 0)
                .filter(holder -> filterStage >= holder.value().minFilterStage())
                .filter(holder -> processingLevel >= holder.value().minProcessingLevel())
                .toList();
    }

    private static ItemStack chooseWeighted(List<RecipeHolder<MinersCompanionRecoveryRecipe>> recipes, RandomSource random) {
        int totalWeight = recipes.stream().mapToInt(holder -> Math.max(1, holder.value().weight())).sum();
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (RecipeHolder<MinersCompanionRecoveryRecipe> holder : recipes) {
            cursor += Math.max(1, holder.value().weight());
            if (roll < cursor) {
                return holder.value().result();
            }
        }
        return recipes.getLast().value().result();
    }

    private MinersCompanionRecoveryRecipes() {
    }
}
