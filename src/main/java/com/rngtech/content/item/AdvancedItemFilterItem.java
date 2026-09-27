package com.rngtech.content.item;

import com.rngtech.content.itemfilter.AdvancedItemFilterSettings;
import com.rngtech.content.menu.AdvancedItemFilterMenu;
import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class AdvancedItemFilterItem extends Item {
    public AdvancedItemFilterItem(Properties properties) {
        super(properties);
    }

    public static boolean isAdvancedFilter(ItemStack stack) {
        return stack.getItem() instanceof AdvancedItemFilterItem;
    }

    public static AdvancedItemFilterSettings settings(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ADVANCED_ITEM_FILTER.get(), AdvancedItemFilterSettings.EMPTY);
    }

    public static void setSettings(ItemStack stack, AdvancedItemFilterSettings settings) {
        if (isAdvancedFilter(stack)) {
            stack.set(ModDataComponents.ADVANCED_ITEM_FILTER.get(), settings);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        AdvancedItemFilterSettings settings = settings(stack);
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.advanced_item_filter.mode",
                Component.translatable("rngtech.advanced_filter.mode." + settings.mode().getSerializedName())
        ).withStyle(settings.mode() == AdvancedItemFilterSettings.Mode.DENY ? ChatFormatting.RED : ChatFormatting.GREEN));
        if (!settings.hasActiveCriteria()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.advanced_item_filter.unconfigured").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (settings.exactItemsEnabled()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.advanced_item_filter.samples",
                    sampleCount(settings),
                    AdvancedItemFilterSettings.SAMPLE_SLOT_COUNT
            ).withStyle(ChatFormatting.GRAY));
        }
        if (settings.tagEnabled()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.advanced_item_filter.tags",
                    settings.tags().size(),
                    AdvancedItemFilterSettings.MAX_TAGS
            ).withStyle(ChatFormatting.GRAY));
        }
        if (settings.namespaceEnabled()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.advanced_item_filter.namespaces",
                    settings.namespaces().size(),
                    AdvancedItemFilterSettings.MAX_NAMESPACES
            ).withStyle(ChatFormatting.GRAY));
        }
        if (settings.stageEnabled()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.advanced_item_filter.stage",
                    settings.minStage(),
                    settings.maxStage()
            ).withStyle(ChatFormatting.GRAY));
        }
        if (settings.stabilityEnabled()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.advanced_item_filter.stability",
                    settings.minStability(),
                    settings.maxStability()
            ).withStyle(ChatFormatting.GRAY));
        }
        if (settings.identityMode() != AdvancedItemFilterSettings.IdentityMode.ANY) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.advanced_item_filter.identity",
                    Component.translatable("rngtech.advanced_filter.identity." + settings.identityMode().getSerializedName())
            ).withStyle(ChatFormatting.GRAY));
        }
        if (settings.rarityMode() != AdvancedItemFilterSettings.RarityMode.ANY) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.advanced_item_filter.rarity",
                    Component.translatable("rngtech.advanced_filter.rarity." + settings.rarityMode().getSerializedName())
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    private static int sampleCount(AdvancedItemFilterSettings settings) {
        int count = 0;
        for (ItemStack sample : settings.samples()) {
            if (!sample.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void openMenu(ServerPlayer player, InteractionHand hand) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.rngtech.advanced_item_filter");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new AdvancedItemFilterMenu(containerId, inventory, hand);
            }
        };
        player.openMenu(provider, buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND));
    }
}
