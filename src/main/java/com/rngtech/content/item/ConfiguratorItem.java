package com.rngtech.content.item;

import com.rngtech.content.configurator.ConfiguratorOperationResult;
import com.rngtech.content.configurator.ConfiguratorOperations;
import com.rngtech.content.configurator.ConfiguratorPreset;
import com.rngtech.content.configurator.ConfiguratorTarget;
import com.rngtech.content.menu.ConfiguratorActionMenu;
import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class ConfiguratorItem extends Item {
    public ConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockHitResult hitResult = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()
        );
        ConfiguratorTarget target = ConfiguratorTarget.resolve(context.getLevel(), hitResult);
        ItemStack stack = context.getItemInHand();
        ConfiguratorPreset preset = stack.getOrDefault(ModDataComponents.CONFIGURATOR_PRESET.get(), ConfiguratorPreset.EMPTY);
        if (context.getLevel().isClientSide) {
            return target != null || preset.gearHelper() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (target != null) {
            openActionMenu(serverPlayer, context, target);
            return InteractionResult.SUCCESS;
        }
        if (preset.gearHelper()) {
            ConfiguratorOperationResult result = ConfiguratorOperations.installGearHelper(serverPlayer, context.getClickedPos());
            serverPlayer.displayClientMessage(result.message(), true);
            return result.success() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ConfiguratorPreset preset = stack.getOrDefault(ModDataComponents.CONFIGURATOR_PRESET.get(), ConfiguratorPreset.EMPTY);
        tooltipComponents.add(Component.translatable("rngtech.tooltip.configurator.role").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.configurator.advanced").withStyle(ChatFormatting.DARK_GRAY));
        if (preset.hasAnySettings()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.configurator.has_preset").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (preset.pasteModules()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.configurator.modules").withStyle(ChatFormatting.GOLD));
        }
        if (preset.gearHelper()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.configurator.gear_helper").withStyle(ChatFormatting.GREEN));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private static void openActionMenu(ServerPlayer player, UseOnContext context, ConfiguratorTarget target) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.rngtech.configurator");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new ConfiguratorActionMenu(containerId, inventory, context.getHand(), target);
            }
        };
        player.openMenu(provider, buffer -> {
            buffer.writeBoolean(context.getHand() == net.minecraft.world.InteractionHand.OFF_HAND);
            target.write(buffer);
        });
    }
}
