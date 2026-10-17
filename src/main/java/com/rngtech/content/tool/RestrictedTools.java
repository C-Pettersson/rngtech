package com.rngtech.content.tool;

import com.rngtech.RNGTech;
import com.rngtech.RNGTechConfig;
import com.rngtech.RNGTechConfig.RestrictedToolMode;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Pack-configurable penalties for the tools in {@code rngtech:restricted_tools}, stone through netherite pickaxes, axes, and shovels by default. */
public final class RestrictedTools {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, RNGTech.id("restricted_tools"));

    public static RestrictedToolMode mode(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(TAG)) {
            return RestrictedToolMode.NORMAL;
        }
        return RNGTechConfig.RESTRICTED_TOOLS_MODE.get();
    }

    public static boolean useless(ItemStack stack) {
        return mode(stack) == RestrictedToolMode.USELESS;
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        switch (mode(stack)) {
            case NORMAL -> {
            }
            case WEAKENED -> event.setNewSpeed(event.getNewSpeed() * RNGTechConfig.RESTRICTED_TOOLS_WEAKENED_SPEED_MULTIPLIER.get().floatValue());
            case USELESS -> {
                float toolSpeed = Math.max(1.0F, stack.getDestroySpeed(event.getState()));
                event.setNewSpeed(event.getNewSpeed() / toolSpeed);
            }
        }
    }

    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (useless(event.getEntity().getMainHandItem()) && event.getTargetBlock().requiresCorrectToolForDrops()) {
            event.setCanHarvest(false);
        }
    }

    public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (useless(event.getHeldItemStack())) {
            event.setCanceled(true);
        }
    }

    public static void onTooltip(ItemTooltipEvent event) {
        switch (mode(event.getItemStack())) {
            case NORMAL -> {
            }
            case WEAKENED -> event.getToolTip().add(Component.translatable(
                    "rngtech.tooltip.restricted_tool.weakened",
                    Math.round(RNGTechConfig.RESTRICTED_TOOLS_WEAKENED_SPEED_MULTIPLIER.get() * 100.0D)
            ).withStyle(ChatFormatting.RED));
            case USELESS -> event.getToolTip().add(Component.translatable("rngtech.tooltip.restricted_tool.useless")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private RestrictedTools() {
    }
}
