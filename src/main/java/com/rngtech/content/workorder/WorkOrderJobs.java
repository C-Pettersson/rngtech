package com.rngtech.content.workorder;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderConditions;
import com.rngtech.rpg.workorder.WorkOrderRules;
import com.rngtech.rpg.workorder.WorkOrderTemplate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/** Rolls Work Order jobs in a world, where item jobs can see which recipes are loaded and rewards can roll their loot. */
public final class WorkOrderJobs {
    private WorkOrderJobs() {
    }

    /** A rolled job and its chip's traits: its rarity and conditions. */
    public record Job(WorkOrder order, MachineTraits traits) {
        public ItemStack stack() {
            return JobChipItem.of(ModItems.JOB_CHIP.get(), order, traits);
        }
    }

    /**
     * A listing the way the Dispatch Terminal rolls one, of {@code tier} or of a random tier when it is 0: a random job, or
     * a pooled template by weight; with {@code workOrders.randomOrdersEnabled} off, only pooled templates. Its conditions
     * roll by rarity, then its reward is rolled at the Bounty they add, or fixed. Null when nothing can roll.
     */
    public static Job roll(Level level, BlockPos pos, int tier, RandomGenerator random) {
        WorkOrder order = WorkOrderRules.roll(WorkObjectiveCatalog.all(), RNGTechConfig.WORK_ORDER_RANDOM_ENABLED.get(), WorkOrderTemplateCatalog.all(),
                tier, random, itemAvailable(level));
        return order == null ? null : finish(level, pos, order, WorkOrderTemplateCatalog.get(ResourceLocation.tryParse(order.template())), random);
    }

    /** The template's job, of {@code tier} or of a random tier when it is 0, unless the template fixes one; null when it cannot roll. */
    public static Job roll(Level level, BlockPos pos, WorkOrderTemplate template, int tier, RandomGenerator random) {
        int rolledTier = tier > 0 ? tier : WorkOrder.MIN_TIER + random.nextInt(WorkOrder.MAX_TIER - WorkOrder.MIN_TIER + 1);
        WorkOrder order = template.roll(rolledTier, WorkObjectiveCatalog.byId(), random, itemAvailable(level));
        return order == null ? null : finish(level, pos, order, template, random);
    }

    /** Rolls the job's conditions, at the template's rarity when it fixes one, then its reward. */
    private static Job finish(Level level, BlockPos pos, WorkOrder order, WorkOrderTemplate template, RandomGenerator random) {
        WorkOrderConditions.Rolled rolled = WorkOrderConditions.roll(order, template == null ? null : template.rarity(), RandomSource.create(random.nextLong()));
        return new Job(withRewards(level, pos, rolled.order(), template, random), rolled.traits());
    }

    /** {@code order} with its template's fixed rewards, or with its reward table rolled. */
    private static WorkOrder withRewards(Level level, BlockPos pos, WorkOrder order, WorkOrderTemplate template, RandomGenerator random) {
        if (template != null && !template.rewards().isEmpty()) {
            List<ItemStack> stacks = template.rewards().stream()
                    .map(reward -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(reward.item())), reward.count()))
                    .toList();
            return order.withRewards(ItemContainerContents.fromItems(stacks));
        }
        return WorkOrderRewards.withRewards(level, pos, order, random);
    }

    /**
     * Whether an item job may name {@code id}: a registered item with a loaded Crusher or Furnace recipe that takes it, or a
     * loaded Component Assembler, Alloy Furnace, or Metal Press recipe that makes it.
     */
    public static Predicate<String> itemAvailable(Level level) {
        return id -> {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
                return false;
            }
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(BuiltInRegistries.ITEM.get(key)));
            return level.getRecipeManager().getRecipeFor(ModRecipes.CRUSHER_TYPE.get(), input, level).isPresent()
                    || level.getRecipeManager().getRecipeFor(ModRecipes.FURNACE_TYPE.get(), input, level).isPresent()
                    || level.getRecipeManager().getAllRecipesFor(ModRecipes.BATTERY_ASSEMBLY_TYPE.get()).stream()
                            .anyMatch(holder -> holder.value().outputStack().is(BuiltInRegistries.ITEM.get(key)))
                    || level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOY_FURNACE_TYPE.get()).stream()
                            .anyMatch(holder -> holder.value().outputStack().is(BuiltInRegistries.ITEM.get(key)))
                    || level.getRecipeManager().getAllRecipesFor(ModRecipes.METAL_PRESS_TYPE.get()).stream()
                            .anyMatch(holder -> holder.value().outputStack().is(BuiltInRegistries.ITEM.get(key)));
        };
    }
}
