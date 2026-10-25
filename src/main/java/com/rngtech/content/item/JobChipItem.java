package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.workorder.WorkOrderEligibility;
import com.rngtech.content.workorder.WorkOrderRewards;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderProgress;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

/**
 * A Work Order: its family, tier, job, requirements, Bounty, and Prestige are readable on the item, and its rarity and
 * conditions are its traits. A machine of the order's family loads it on its Orders tab. A stack without order data is
 * blank and never loads.
 */
public class JobChipItem extends Item {
    private static final int ELIGIBLE_SHOWN = 24;
    private static final int ELIGIBLE_PER_LINE = 3;

    public JobChipItem(Properties properties) {
        super(properties);
    }

    /** The order, or null for a blank stack. */
    public static WorkOrder data(ItemStack stack) {
        return stack.get(ModDataComponents.WORK_ORDER.get());
    }

    public static MachineTraits traits(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }

    public static boolean isWorkOrder(ItemStack stack) {
        return stack.getItem() instanceof JobChipItem;
    }

    public static ItemStack of(Item item, WorkOrder order, MachineTraits traits) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.WORK_ORDER.get(), order);
        if (traits != null && !traits.isEmpty()) {
            stack.set(ModDataComponents.MACHINE_TRAITS.get(), traits);
        }
        return stack;
    }

    /** The job as a sentence, such as "Crush 256 items from recipes that need Processing Level 6 or more." */
    public static Component objectiveText(WorkObjective objective) {
        List<WorkPart> parts = objective.parts();
        boolean items = parts.stream().allMatch(part -> WorkFilter.ITEM.id().equals(part.filter()));
        WorkObjectiveType type = WorkObjectiveType.byId(parts.getFirst().type());
        if (type == null) {
            return Component.translatable("rngtech.work_order.objective.unknown");
        }
        if (!items) {
            return partText(parts.getFirst());
        }
        MutableComponent list = Component.empty();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) {
                list.append(Component.translatable(index == parts.size() - 1 ? "rngtech.work_order.list.last" : "rngtech.work_order.list.next"));
            }
            list.append(Component.translatable("rngtech.work_order.item_count", parts.get(index).target(), itemName(parts.get(index).item())));
        }
        return Component.translatable("rngtech.work_order.verb." + type.id(), list);
    }

    /** One part as a sentence, such as "Crush 256 items needing Processing Level 6+" or "Smelt 96 Crushed Tin". */
    public static Component partText(WorkPart part) {
        WorkObjectiveType type = WorkObjectiveType.byId(part.type());
        WorkFilter filter = WorkFilter.byId(part.filter());
        if (type == null || filter == null) {
            return Component.translatable("rngtech.work_order.objective.unknown");
        }
        return Component.translatable("rngtech.work_order.part." + type.id() + "." + filter.id(), part.target(), part.minimum(), itemName(part.item()));
    }

    /**
     * For a stage job, what counts toward it while Shift or Alt is held: Crusher inputs, or Furnace materials. Otherwise a
     * hint to hold it. A job line such as "Processing Level 6+" or "Stage 5+" alone does not say what reaches it.
     */
    public static void appendEligibleItems(List<Component> lines, WorkObjective objective) {
        WorkPart part = objective.parts().getFirst();
        if (!WorkOrderEligibility.lists(part)) {
            return;
        }
        String kind = WorkObjectiveType.byId(part.type()) == WorkObjectiveType.SMELT ? "materials" : "items";
        if (!TooltipKeyState.hasDetailKeyDown()) {
            lines.add(Component.translatable("rngtech.tooltip.work_order.eligible_hint." + kind).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        RecipeManager recipes = ClientRecipes.manager();
        List<Component> names = recipes == null ? List.of() : WorkOrderEligibility.names(recipes, part);
        if (names.isEmpty()) {
            lines.add(Component.translatable("rngtech.tooltip.work_order.eligible_none").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.add(Component.translatable("rngtech.tooltip.work_order.eligible." + kind).withStyle(ChatFormatting.GRAY));
        int shown = Math.min(names.size(), ELIGIBLE_SHOWN);
        for (int start = 0; start < shown; start += ELIGIBLE_PER_LINE) {
            MutableComponent line = Component.literal(" ");
            for (int index = start; index < Math.min(shown, start + ELIGIBLE_PER_LINE); index++) {
                if (index > start) {
                    line.append(", ");
                }
                line.append(names.get(index));
            }
            lines.add(line.withStyle(ChatFormatting.DARK_AQUA));
        }
        if (names.size() > shown) {
            lines.add(Component.translatable("rngtech.tooltip.work_order.eligible_more", names.size() - shown).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** An item's display name from its id, or the id itself when no such item exists. */
    public static Component itemName(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.ITEM.containsKey(key)
                ? BuiltInRegistries.ITEM.get(key).getDescription() : Component.literal(id);
    }

    /** The job in a few words for a narrow card, such as "Crush 256, Processing Level 6+". */
    public static Component shortObjectiveText(WorkObjective objective) {
        return objectiveText(objective);
    }

    /** A template job's display name: a translation key, or plain text when no such key exists. */
    public static MutableComponent displayName(WorkOrder order) {
        return Component.translatableWithFallback(order.name(), order.name());
    }

    public static MutableComponent familyName(String family) {
        return Component.translatable("rngtech.work_order.family." + family);
    }

    @Override
    public Component getName(ItemStack stack) {
        WorkOrder order = data(stack);
        if (order == null) {
            return Component.translatable("item.rngtech.job_chip.blank");
        }
        return order.name().isEmpty() ? Component.translatable("item.rngtech.job_chip.tiered", GlyphItem.numeral(order.tier()))
                : Component.translatable("item.rngtech.job_chip.named", displayName(order), GlyphItem.numeral(order.tier()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        WorkOrder order = data(stack);
        if (order == null) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.work_order.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltipComponents.add(familyName(order.family()).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(objectiveText(order.objective()).copy().withStyle(ChatFormatting.GRAY));
        appendEligibleItems(tooltipComponents, order.objective());
        tooltipComponents.add(order.minMasteryLevel() > 0
                ? Component.translatable("rngtech.tooltip.work_order.requires_level", order.minStage(), order.minMasteryLevel()).withStyle(ChatFormatting.GRAY)
                : Component.translatable("rngtech.tooltip.work_order.requires", order.minStage()).withStyle(ChatFormatting.GRAY));
        if (!WorkOrderRewards.items(order.rewards()).isEmpty()) {
            WorkOrderRewards.appendLines(tooltipComponents, order.rewards());
        } else {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.work_order.bounty", order.bounty()).withStyle(ChatFormatting.GRAY));
        }
        WorkOrderProgress progress = stack.get(ModDataComponents.WORK_ORDER_PROGRESS.get());
        if (progress != null && !progress.isNone()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.work_order.progress", progress.progress().stream().mapToLong(Long::longValue).sum(), order.objective().totalTarget())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (order.prestige() > 0) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.fulfilled_work_order.prestige", order.prestige()).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.work_order.load").withStyle(ChatFormatting.DARK_GRAY));
    }
}
