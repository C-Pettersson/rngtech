package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.tool.FieldToolEnchantments;
import com.rngtech.content.tool.ToolBaseStatCatalog;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.content.tool.ToolHeadMaterial;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierEligibilityProfiles;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

import java.util.List;

public class ToolHeadItem extends Item {
    private final ToolHeadFamily family;
    private final ToolHeadMaterial material;

    public ToolHeadItem(ToolHeadFamily family, ToolHeadMaterial material, Properties properties) {
        super(properties);
        this.family = family;
        this.material = material;
    }

    public ToolHeadFamily family() {
        return family;
    }

    public ToolHeadMaterial material() {
        return material;
    }

    public static boolean canAcceptDiamondTip(ItemStack stack) {
        return stack.getItem() instanceof ToolHeadItem head
                && head.material() == ToolHeadMaterial.STEEL
                && head.family().supportsOreBurst()
                && !isDiamondTipped(stack);
    }

    public static boolean isDiamondTipped(ItemStack stack) {
        if (!(stack.getItem() instanceof ToolHeadItem head)) {
            return false;
        }
        return head.material() == ToolHeadMaterial.STEEL
                && head.family().supportsOreBurst()
                && stack.getOrDefault(ModDataComponents.DIAMOND_TIPPED.get(), false);
    }

    public static void applyDiamondTip(ItemStack stack) {
        if (!canAcceptDiamondTip(stack)) {
            throw new IllegalArgumentException("Diamond Tip requires an untipped Steel Pick or Hammer Head");
        }
        stack.set(ModDataComponents.DIAMOND_TIPPED.get(), true);
    }

    public MachineTraits traits(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getMaxStackSize() == 1;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return FieldToolEnchantments.enchantmentValue(material);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return FieldToolEnchantments.proxyForFamily(family).isPrimaryItemFor(enchantment);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return FieldToolEnchantments.proxyForFamily(family).supportsEnchantment(enchantment);
    }

    @Override
    public ItemStack applyEnchantments(ItemStack stack, List<EnchantmentInstance> enchantments) {
        return FieldToolEnchantments.applyEnchantmentsToHead(stack, enchantments);
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
        CraftedTraitOutputs.handleCrafted(stack, level.random);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (CraftedTraitOutputs.isUnidentified(stack)) {
            return CraftedTraitOutputs.unidentifiedName(super.getName(stack));
        }
        MachineTraits traits = traits(stack);
        return traits.isEmpty() ? super.getName(stack) : MachineNameGenerator.generatedName(traits, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        MachineTraitTooltip.appendUnidentified(stack, tooltipComponents);
        MachineTraits traits = traits(stack);
        if (stack.has(ModDataComponents.MACHINE_TRAITS.get())) {
            MachineTraitTooltip.appendTraitHeader(traits, tooltipComponents, true);
        } else {
            MachineTraitTooltip.appendUnrolledTraitHeader(
                    MachineTraitRoller.refinementPotentialRange(
                            ModifierEligibilityProfiles.forToolHead(family == ToolHeadFamily.PICK),
                            material.stage()
                    ),
                    tooltipComponents
            );
        }
        if (MachineTraitTooltip.shouldShowMaterialData()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.tool_head.family",
                    Component.translatable(family.translationKey())
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.tool_part.stage",
                    material.stage()
            ).withStyle(ChatFormatting.GRAY));
            if (isDiamondTipped(stack)) {
                tooltipComponents.add(Component.translatable("rngtech.tooltip.tool_head.diamond_tipped").withStyle(ChatFormatting.AQUA));
            }
        }
        if (ModularToolItem.hasStoredWear(stack)) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.tool_part.stored_wear",
                    ModularToolItem.storedWearPercent(stack)
            ).withStyle(ChatFormatting.RED));
        }
        FieldToolTooltip.appendBaseStats(stack, ToolBaseStatCatalog.baseStats(stack), tooltipComponents);
        MachineTraitTooltip.appendTraitDetails(traits, tooltipComponents);
        MachineTraitTooltip.appendTraitKeyHints(tooltipComponents);
    }
}
