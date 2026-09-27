package com.rngtech.content.item;

import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierLensTag;
import com.rngtech.rpg.ModifierLensTargets;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

public class RefinementLensItem extends Item {
    private final Set<ModifierLensTag> lensTags;

    public RefinementLensItem(Set<ModifierLensTag> lensTags, Properties properties) {
        super(properties);
        this.lensTags = Set.copyOf(lensTags);
    }

    public Set<ModifierLensTag> lensTags() {
        return lensTags;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("rngtech.tooltip.modifier_lens").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.modifier_lens.soft_bias").withStyle(ChatFormatting.DARK_GRAY));
        lensTags.stream()
                .map(ModifierLensTag::tooltipKey)
                .map(Component::translatable)
                .map(component -> component.withStyle(ChatFormatting.DARK_AQUA))
                .forEach(tooltipComponents::add);
        if (TooltipKeyState.hasShiftDown()) {
            appendTargetedAffixes(tooltipComponents);
        } else {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.modifier_lens.hint.shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private void appendTargetedAffixes(List<Component> tooltipComponents) {
        List<ModifierDefinition> targets = ModifierLensTargets.globalTargets(lensTags);
        if (targets.isEmpty()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.modifier_lens.no_targets").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("rngtech.tooltip.modifier_lens.targets").withStyle(ChatFormatting.DARK_AQUA));
        for (ModifierDefinition target : targets) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.modifier_lens.target",
                    MachineModifierText.slotLabel(target.slot()),
                    MachineModifierText.displayName(target),
                    MachineModifierText.definitionStats(target)
            ).withStyle(ChatFormatting.BLUE));
        }
    }
}
