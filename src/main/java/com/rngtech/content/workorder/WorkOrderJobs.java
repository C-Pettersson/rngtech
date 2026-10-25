package com.rngtech.content.workorder;

import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/** Rolls Work Order jobs in a world, where item jobs can see which recipes are loaded and rewards can roll their loot. */
public final class WorkOrderJobs {
    private WorkOrderJobs() {
    }

    /** A random job of {@code tier}, or of a random tier when it is 0, with its reward rolled; null when nothing can roll. */
    public static WorkOrder roll(Level level, BlockPos pos, int tier, RandomGenerator random) {
        Predicate<String> available = itemAvailable(level);
        WorkOrder order = tier > 0
                ? WorkOrderRules.roll(WorkObjectiveCatalog.all(), tier, random, available)
                : WorkOrderRules.roll(WorkObjectiveCatalog.all(), random, available);
        return order == null ? null : WorkOrderRewards.withRewards(level, pos, order, random);
    }

    /** Whether an item job may name {@code id}: a registered item with a loaded Crusher or Furnace recipe. */
    public static Predicate<String> itemAvailable(Level level) {
        return id -> {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
                return false;
            }
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(BuiltInRegistries.ITEM.get(key)));
            return level.getRecipeManager().getRecipeFor(ModRecipes.CRUSHER_TYPE.get(), input, level).isPresent()
                    || level.getRecipeManager().getRecipeFor(ModRecipes.FURNACE_TYPE.get(), input, level).isPresent();
        };
    }
}
