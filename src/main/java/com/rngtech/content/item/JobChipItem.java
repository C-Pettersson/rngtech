package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.workorder.WorkOrderEligibility;
import com.rngtech.content.workorder.WorkOrderRewards;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatDisplay;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderConditions;
import com.rngtech.rpg.workorder.WorkOrderProgress;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

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
        if (filter == WorkFilter.STAT) {
            MachineStat stat = MachineStat.bySerializedName(part.item());
            return stat == null ? Component.translatable("rngtech.work_order.objective.unknown")
                    : Component.translatable("rngtech.work_order.part.stat", Component.translatable(stat.translationKey()), statText(stat, part.target()));
        }
        return Component.translatable("rngtech.work_order.part." + type.id() + "." + filter.id(), part.target(), part.minimum(), itemName(part.item()));
    }

    /** A stat goal's value in hundredths, written the way the Stats tab writes that stat. */
    public static String statText(MachineStat stat, long hundredths) {
        return MachineStatDisplay.statValue(stat, hundredths / (double) WorkPart.STAT_SCALE);
    }

    /**
     * A job's progress against its target: items for counted work, and for a one-part stat goal the machine's value at
     * its last completed job against the target, both written as the stat.
     */
    public static Component progressText(WorkObjective objective, long progress) {
        WorkPart first = objective.parts().getFirst();
        MachineStat stat = first.statGoal() ? MachineStat.bySerializedName(first.item()) : null;
        if (stat != null && objective.parts().size() == 1) {
            return Component.translatable("rngtech.work_order.tab.progress", statText(stat, progress), statText(stat, first.target()));
        }
        return Component.translatable("rngtech.work_order.tab.progress", progress, objective.totalTarget());
    }

    /**
     * For a stage job, what counts toward it while Shift or Alt is held: Crusher inputs, or Furnace materials. Otherwise a
     * hint to hold it. A job line such as "Processing Level 6+" or "Stage 5+" alone does not say what reaches it.
     */
    public static void appendEligibleItems(List<Component> lines, WorkObjective objective, MachineTraits traits) {
        WorkPart listed = objective.parts().getFirst();
        WorkPart part = new WorkPart(listed.type(), listed.filter(), WorkOrderConditions.minimum(listed, traits), listed.item(), listed.target());
        if (!WorkOrderEligibility.lists(part)) {
            return;
        }
        String kind = WorkObjectiveType.byId(part.type()) == WorkObjectiveType.SMELT ? "materials" : "items";
        if (!TooltipKeyState.hasDetailKeyDown()) {
            lines.add(Component.translatable("rngtech.tooltip.work_order.eligible_hint." + kind).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        RecipeManager recipes = ClientWorld.recipes();
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

    /** Whole seconds as m:ss, or h:mm:ss from an hour up. */
    public static String clock(long seconds) {
        long hours = seconds / 3600;
        long minutes = seconds / 60 % 60;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds % 60)
                : String.format(Locale.ROOT, "%d:%02d", minutes, seconds % 60);
    }

    /** A Rush chip in a player's inventory is lost when its deadline passes, the same as one in a drive bay. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) {
            return;
        }
        WorkOrderProgress progress = stack.get(ModDataComponents.WORK_ORDER_PROGRESS.get());
        if (progress != null && progress.rushDeadline() > 0 && level.getGameTime() >= progress.rushDeadline()) {
            stack.setCount(0);
            if (entity instanceof Player player) {
                player.displayClientMessage(Component.translatable("rngtech.work_order.message.expired"), true);
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        WorkOrder order = data(stack);
        if (order == null) {
            return Component.translatable("item.rngtech.job_chip.blank");
        }
        MutableComponent name = order.name().isEmpty() ? Component.translatable("item.rngtech.job_chip.tiered", GlyphItem.numeral(order.tier()))
                : Component.translatable("item.rngtech.job_chip.named", displayName(order), GlyphItem.numeral(order.tier()));
        return switch (traits(stack).rarity()) {
            case MAGIC -> name.withStyle(ChatFormatting.AQUA);
            case RARE -> name.withStyle(ChatFormatting.GOLD);
            default -> name;
        };
    }

    /** One line per condition: its name, its cost or rule, and the Bounty and Prestige it adds. */
    public static void appendConditions(List<Component> lines, MachineTraits traits) {
        for (MachineModifier modifier : traits.modifiers()) {
            WorkOrderConditions.Condition condition = WorkOrderConditions.get(modifier.affixId());
            if (condition == null) {
                continue;
            }
            String id = condition.id();
            Component effect = switch (id) {
                case WorkOrderConditions.STARVED_FEED -> Component.translatable("rngtech.work_order.condition." + id + ".effect",
                        Math.round((1 - modifier.value()) * 100));
                case WorkOrderConditions.POWER_SURCHARGE, WorkOrderConditions.COLD_START ->
                        Component.translatable("rngtech.work_order.condition." + id + ".effect", Math.round(modifier.value()));
                default -> Component.translatable("rngtech.work_order.condition." + id + ".effect");
            };
            Component reward = condition.prestige() > 0
                    ? Component.translatable("rngtech.tooltip.work_order.condition_reward_prestige", condition.bounty(modifier.tier()), condition.prestige())
                    : Component.translatable("rngtech.tooltip.work_order.condition_reward", condition.bounty(modifier.tier()));
            lines.add(Component.translatable("rngtech.tooltip.work_order.condition",
                    Component.translatable("rngtech.work_order.condition." + id).withStyle(ChatFormatting.RED),
                    effect, reward.copy().withStyle(ChatFormatting.DARK_GREEN)).withStyle(ChatFormatting.GRAY));
        }
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
        appendEligibleItems(tooltipComponents, order.objective(), traits(stack));
        appendConditions(tooltipComponents, traits(stack));
        if (order.minStage() > 0 || order.minMasteryLevel() > 0) {
            tooltipComponents.add(order.minMasteryLevel() > 0
                    ? Component.translatable("rngtech.tooltip.work_order.requires_level", order.minStage(), order.minMasteryLevel()).withStyle(ChatFormatting.GRAY)
                    : Component.translatable("rngtech.tooltip.work_order.requires", order.minStage()).withStyle(ChatFormatting.GRAY));
        }
        if (!WorkOrderRewards.items(order.rewards()).isEmpty()) {
            WorkOrderRewards.appendLines(tooltipComponents, order.rewards());
        } else {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.work_order.bounty", order.bounty()).withStyle(ChatFormatting.GRAY));
        }
        WorkOrderProgress progress = stack.get(ModDataComponents.WORK_ORDER_PROGRESS.get());
        if (progress != null && !progress.isNone()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.work_order.progress_line",
                    progressText(order.objective(), progress.progress().stream().mapToLong(Long::longValue).sum())).withStyle(ChatFormatting.GRAY));
        }
        if (progress != null && progress.rushDeadline() > 0) {
            long now = ClientWorld.gameTime();
            tooltipComponents.add(Component.translatable("rngtech.tooltip.work_order.rush",
                    clock(now < 0 ? 0 : Math.max(0, progress.rushDeadline() - now) / 20)).withStyle(ChatFormatting.GOLD));
        }
        if (order.prestige() > 0) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.fulfilled_work_order.prestige", order.prestige()).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("rngtech.tooltip.work_order.load").withStyle(ChatFormatting.DARK_GRAY));
    }
}
