package com.rngtech.content.tool;

import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.item.ToolHeadItem;
import com.rngtech.content.registry.ModDataComponents;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Optional;

public final class FieldToolEnchantments {
    public static final int MIN_ENCHANTMENT_VALUE = 5;
    public static final int MAX_ENCHANTMENT_VALUE = 22;

    public record AnvilMergeResult(ItemStack output, long cost, int materialCost, boolean blocked) {
    }

    public static ItemStack proxyForFamily(ToolHeadFamily family) {
        return switch (family) {
            case PICK, HAMMER -> new ItemStack(Items.IRON_PICKAXE);
            case SHOVEL, DIGGER -> new ItemStack(Items.IRON_SHOVEL);
            case AXE, TREEFELLER -> new ItemStack(Items.IRON_AXE);
        };
    }

    public static int enchantmentValue(ToolHeadMaterial material) {
        return Mth.clamp(6 + material.stage() * 2, MIN_ENCHANTMENT_VALUE, MAX_ENCHANTMENT_VALUE);
    }

    public static ItemEnchantments headEnchantments(ItemStack stack) {
        if (stack.getItem() instanceof ToolHeadItem) {
            return EnchantmentHelper.getEnchantmentsForCrafting(stack);
        }
        ItemStack head = installedHead(stack);
        return head.isEmpty() ? ItemEnchantments.EMPTY : EnchantmentHelper.getEnchantmentsForCrafting(head);
    }

    public static ItemEnchantments allEnchantments(ItemStack stack) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(headEnchantments(stack));
        for (Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            mutable.upgrade(entry.getKey(), entry.getIntValue());
        }
        return mutable.toImmutable();
    }

    public static int enchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return Math.max(
                headEnchantments(stack).getLevel(enchantment),
                EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment)
        );
    }

    public static boolean hasAnyEnchantments(ItemStack stack) {
        return !allEnchantments(stack).isEmpty();
    }

    public static ItemStack applyEnchantmentsToHead(ItemStack stack, List<EnchantmentInstance> enchantments) {
        if (enchantments.isEmpty()) {
            return stack;
        }
        if (stack.getItem() instanceof ToolHeadItem) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack));
            for (EnchantmentInstance instance : enchantments) {
                mutable.upgrade(instance.enchantment, instance.level);
            }
            EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
            return stack;
        }
        if (!ModularToolItem.hasValidAssembly(stack)) {
            return stack;
        }
        ItemStack head = installedHead(stack).copy();
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(head));
        for (EnchantmentInstance instance : enchantments) {
            mutable.upgrade(instance.enchantment, instance.level);
        }
        EnchantmentHelper.setEnchantments(head, mutable.toImmutable());
        setInstalledHead(stack, head);
        clearOuterEnchantments(stack);
        return stack;
    }

    public static boolean migrateOuterEnchantments(ItemStack stack) {
        if (!ModularToolItem.hasValidAssembly(stack)) {
            return false;
        }
        ItemEnchantments outerEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (outerEnchantments.isEmpty()) {
            return false;
        }
        ItemStack head = installedHead(stack).copy();
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(head));
        for (Entry<Holder<Enchantment>> entry : outerEnchantments.entrySet()) {
            mutable.upgrade(entry.getKey(), entry.getIntValue());
        }
        EnchantmentHelper.setEnchantments(head, mutable.toImmutable());
        setInstalledHead(stack, head);
        clearOuterEnchantments(stack);
        return true;
    }

    public static void clearOuterEnchantments(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static void appendHeadEnchantments(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        ItemEnchantments enchantments = headEnchantments(stack);
        if (enchantments.isEmpty()) {
            return;
        }
        enchantments.addToTooltip(context, tooltipComponents::add, tooltipFlag);
    }

    public static Optional<AnvilMergeResult> mergeBookInAnvil(
            ItemStack left,
            ItemStack right,
            String name,
            Player player
    ) {
        if (!ModularToolItem.hasValidAssembly(left) || !right.has(DataComponents.STORED_ENCHANTMENTS)) {
            return Optional.empty();
        }
        ItemEnchantments bookEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (bookEnchantments.isEmpty()) {
            return Optional.empty();
        }

        ItemStack output = left.copyWithCount(1);
        migrateOuterEnchantments(output);
        ItemStack head = installedHead(output).copy();
        ItemEnchantments.Mutable mergedEnchantments =
                new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(head));
        long baseCost = (long) head.getOrDefault(DataComponents.REPAIR_COST, 0)
                + (long) right.getOrDefault(DataComponents.REPAIR_COST, 0);
        int workCost = 0;
        boolean acceptedAny = false;
        boolean rejectedAny = false;

        for (Entry<Holder<Enchantment>> entry : bookEnchantments.entrySet()) {
            Holder<Enchantment> enchantmentHolder = entry.getKey();
            int currentLevel = mergedEnchantments.getLevel(enchantmentHolder);
            int incomingLevel = entry.getIntValue();
            int resultLevel = currentLevel == incomingLevel
                    ? incomingLevel + 1
                    : Math.max(incomingLevel, currentLevel);
            Enchantment enchantment = enchantmentHolder.value();
            boolean allowed = output.supportsEnchantment(enchantmentHolder);
            if (player.getAbilities().instabuild) {
                allowed = true;
            }

            for (Holder<Enchantment> existing : mergedEnchantments.keySet()) {
                if (!existing.equals(enchantmentHolder) && !Enchantment.areCompatible(enchantmentHolder, existing)) {
                    allowed = false;
                    workCost++;
                }
            }

            if (!allowed) {
                rejectedAny = true;
                continue;
            }

            acceptedAny = true;
            int acceptedLevel = Math.min(resultLevel, enchantment.getMaxLevel());
            mergedEnchantments.set(enchantmentHolder, acceptedLevel);
            workCost += Math.max(1, enchantment.getAnvilCost() / 2) * acceptedLevel;
        }

        if (rejectedAny && !acceptedAny) {
            return Optional.of(new AnvilMergeResult(ItemStack.EMPTY, 0, 0, true));
        }

        int renameCost = applyRename(left, output, name);
        workCost += renameCost;
        if (workCost <= 0) {
            return Optional.of(new AnvilMergeResult(ItemStack.EMPTY, 0, 0, true));
        }

        long totalCost = Math.min(Integer.MAX_VALUE, Math.max(0L, baseCost + (long) workCost));
        if (renameCost == workCost && renameCost > 0 && totalCost >= 40L) {
            totalCost = 39L;
        }
        if (totalCost >= 40L && !player.getAbilities().instabuild) {
            return Optional.of(new AnvilMergeResult(ItemStack.EMPTY, 0, 0, true));
        }

        int repairCost = Math.max(
                head.getOrDefault(DataComponents.REPAIR_COST, 0),
                right.getOrDefault(DataComponents.REPAIR_COST, 0)
        );
        if (renameCost != workCost || renameCost == 0) {
            repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost);
        }
        head.set(DataComponents.REPAIR_COST, repairCost);
        EnchantmentHelper.setEnchantments(head, mergedEnchantments.toImmutable());
        setInstalledHead(output, head);
        clearOuterEnchantments(output);
        return Optional.of(new AnvilMergeResult(output, totalCost, 1, false));
    }

    public static Optional<AnvilMergeResult> stripWithGrindstone(ItemStack top, ItemStack bottom) {
        ItemStack input = ItemStack.EMPTY;
        if (ModularToolItem.hasValidAssembly(top) && bottom.isEmpty()) {
            input = top;
        } else if (ModularToolItem.hasValidAssembly(bottom) && top.isEmpty()) {
            input = bottom;
        }
        if (input.isEmpty() || !hasAnyEnchantments(input)) {
            return Optional.empty();
        }

        ItemStack output = input.copyWithCount(1);
        migrateOuterEnchantments(output);
        ItemStack head = installedHead(output).copy();
        ItemEnchantments remaining = EnchantmentHelper.updateEnchantments(
                head,
                enchantments -> enchantments.removeIf(holder -> !holder.is(EnchantmentTags.CURSE))
        );
        int repairCost = 0;
        for (int index = 0; index < remaining.size(); index++) {
            repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost);
        }
        head.set(DataComponents.REPAIR_COST, repairCost);
        setInstalledHead(output, head);
        clearOuterEnchantments(output);
        return Optional.of(new AnvilMergeResult(output, grindstoneXp(input), 0, false));
    }

    private static int applyRename(ItemStack left, ItemStack output, String name) {
        if (name == null) {
            return 0;
        }
        if (!StringUtil.isBlank(name)) {
            if (!name.equals(left.getHoverName().getString())) {
                output.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                return 1;
            }
        } else if (left.has(DataComponents.CUSTOM_NAME)) {
            output.remove(DataComponents.CUSTOM_NAME);
            return 1;
        }
        return 0;
    }

    private static int grindstoneXp(ItemStack stack) {
        int experience = 0;
        for (Entry<Holder<Enchantment>> entry : allEnchantments(stack).entrySet()) {
            if (!entry.getKey().is(EnchantmentTags.CURSE)) {
                experience += entry.getKey().value().getMinCost(entry.getIntValue());
            }
        }
        return experience <= 0 ? 0 : (int) Math.ceil(experience / 2.0D);
    }

    private static ItemStack installedHead(ItemStack stack) {
        FieldToolAssembly assembly = ModularToolItem.assembly(stack);
        return assembly.isValid() ? assembly.head() : ItemStack.EMPTY;
    }

    private static void setInstalledHead(ItemStack stack, ItemStack head) {
        FieldToolAssembly assembly = ModularToolItem.assembly(stack);
        if (!assembly.isValid()) {
            return;
        }
        stack.set(
                ModDataComponents.FIELD_TOOL_ASSEMBLY.get(),
                new FieldToolAssembly(head, assembly.rod(), assembly.batteryCell())
        );
    }

    private FieldToolEnchantments() {
    }
}
