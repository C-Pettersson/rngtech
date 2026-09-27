package com.rngtech.content.menu;

import com.rngtech.content.configurator.ConfiguratorOperationResult;
import com.rngtech.content.configurator.ConfiguratorOperations;
import com.rngtech.content.configurator.ConfiguratorPreset;
import com.rngtech.content.configurator.ConfiguratorPresetCategory;
import com.rngtech.content.configurator.ConfiguratorTarget;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ConfiguratorActionMenu extends AbstractContainerMenu {
    public static final int BUTTON_COPY_ALL = 0;
    public static final int BUTTON_COPY_FLUID = 1;
    public static final int BUTTON_COPY_ITEM = 2;
    public static final int BUTTON_COPY_ENERGY = 3;
    public static final int BUTTON_PASTE = 4;

    private final InteractionHand hand;
    private final ConfiguratorTarget target;

    public ConfiguratorActionMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                ConfiguratorTarget.read(extraData));
    }

    public ConfiguratorActionMenu(
            int containerId,
            Inventory playerInventory,
            InteractionHand hand,
            ConfiguratorTarget target
    ) {
        super(ModMenus.CONFIGURATOR_ACTION.get(), containerId);
        this.hand = hand;
        this.target = target;
    }

    public InteractionHand hand() {
        return hand;
    }

    public ConfiguratorTarget target() {
        return target;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) {
            return isKnownButton(id);
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !holdsConfigurator(player)) {
            return false;
        }
        if (!ConfiguratorOperations.isValid(serverPlayer, target)) {
            send(serverPlayer, ConfiguratorOperationResult.fail("rngtech.configurator.message.invalid_target"));
            return false;
        }

        ItemStack stack = player.getItemInHand(hand);
        ConfiguratorPreset preset = stack.getOrDefault(ModDataComponents.CONFIGURATOR_PRESET.get(), ConfiguratorPreset.EMPTY);
        if (id == BUTTON_PASTE) {
            ConfiguratorOperationResult result = ConfiguratorOperations.paste(serverPlayer, target, preset);
            send(serverPlayer, result);
            if (result.success()) {
                player.closeContainer();
            }
            return result.success();
        }

        ConfiguratorPresetCategory category = switch (id) {
            case BUTTON_COPY_ALL -> ConfiguratorPresetCategory.ALL;
            case BUTTON_COPY_FLUID -> ConfiguratorPresetCategory.FLUID;
            case BUTTON_COPY_ITEM -> ConfiguratorPresetCategory.ITEM;
            case BUTTON_COPY_ENERGY -> ConfiguratorPresetCategory.ENERGY;
            default -> null;
        };
        if (category == null) {
            return false;
        }
        ConfiguratorPreset copied = ConfiguratorOperations.copy(serverPlayer, target, preset, category);
        stack.set(ModDataComponents.CONFIGURATOR_PRESET.get(), copied);
        syncHeldStack(serverPlayer);
        send(serverPlayer, ConfiguratorOperationResult.success("rngtech.configurator.message.copied"));
        player.closeContainer();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return holdsConfigurator(player)
                && (!(player instanceof ServerPlayer serverPlayer) || ConfiguratorOperations.isValid(serverPlayer, target));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private boolean holdsConfigurator(Player player) {
        return player.getItemInHand(hand).is(ModItems.CONFIGURATOR.get());
    }

    private static boolean isKnownButton(int id) {
        return id >= BUTTON_COPY_ALL && id <= BUTTON_PASTE;
    }

    private static void send(ServerPlayer player, ConfiguratorOperationResult result) {
        player.displayClientMessage(result.message(), true);
    }

    private void syncHeldStack(ServerPlayer player) {
        int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().selected;
        player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, slot, player.getItemInHand(hand).copy()));
    }
}
