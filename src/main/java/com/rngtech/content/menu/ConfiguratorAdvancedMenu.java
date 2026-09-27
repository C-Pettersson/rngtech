package com.rngtech.content.menu;

import com.rngtech.content.configurator.ConfiguratorPreset;
import com.rngtech.content.configurator.ConfiguratorPresetCategory;
import com.rngtech.content.configurator.ConnectorSetting;
import com.rngtech.content.configurator.RelativeDirection;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ConfiguratorAdvancedMenu extends AbstractContainerMenu {
    public static final int BUTTON_CATEGORY = 0;
    public static final int BUTTON_TOGGLE_MODULES = 1;
    public static final int BUTTON_TOGGLE_GEAR_HELPER = 2;
    public static final int BUTTON_FLIP_MODES = 3;
    public static final int BUTTON_RESET_SELECTED = 4;
    public static final int BUTTON_RESET_ALL = 5;
    public static final int BUTTON_CHANNEL_DOWN = 6;
    public static final int BUTTON_CHANNEL_UP = 7;
    public static final int BUTTON_CYCLE_MODE = 8;
    public static final int BUTTON_ATTACH_DEFAULT = 20;
    public static final int BUTTON_ATTACH_NONE = 21;
    public static final int BUTTON_ATTACH_BASE = 30;

    private final Inventory inventory;
    private final InteractionHand hand;
    private ConfiguratorPresetCategory selectedCategory = ConfiguratorPresetCategory.ALL;

    public ConfiguratorAdvancedMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public ConfiguratorAdvancedMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenus.CONFIGURATOR_ADVANCED.get(), containerId);
        this.inventory = playerInventory;
        this.hand = hand;
    }

    public InteractionHand hand() {
        return hand;
    }

    public ConfiguratorPresetCategory selectedCategory() {
        return selectedCategory;
    }

    public ConfiguratorPreset preset() {
        return configuratorStack().getOrDefault(ModDataComponents.CONFIGURATOR_PRESET.get(), ConfiguratorPreset.EMPTY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!isKnownButton(id)) {
            return false;
        }
        if (id == BUTTON_CATEGORY) {
            selectedCategory = selectedCategory.next();
            return true;
        }
        if (!holdsConfigurator(player)) {
            return false;
        }

        ConfiguratorPreset preset = preset();
        ConfiguratorPreset updated = switch (id) {
            case BUTTON_TOGGLE_MODULES -> preset.withPasteModules(!preset.pasteModules());
            case BUTTON_TOGGLE_GEAR_HELPER -> preset.withGearHelper(!preset.gearHelper());
            case BUTTON_FLIP_MODES -> preset.flipModes();
            case BUTTON_RESET_SELECTED -> preset.resetCategory(selectedCategory);
            case BUTTON_RESET_ALL -> preset.resetAll();
            case BUTTON_CHANNEL_DOWN -> preset.setCategoryChannel(selectedCategory, representativeChannel(preset) - 1);
            case BUTTON_CHANNEL_UP -> preset.setCategoryChannel(selectedCategory, representativeChannel(preset) + 1);
            case BUTTON_CYCLE_MODE -> preset.cycleCategoryMode(selectedCategory);
            case BUTTON_ATTACH_DEFAULT -> preset.setCategoryAttach(selectedCategory, RelativeDirection.DEFAULT);
            case BUTTON_ATTACH_NONE -> preset.setCategoryAttach(selectedCategory, RelativeDirection.NONE);
            default -> attachButton(id, preset);
        };
        if (updated == null) {
            return false;
        }
        configuratorStack().set(ModDataComponents.CONFIGURATOR_PRESET.get(), updated);
        syncHeldStack(player);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return holdsConfigurator(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int representativeChannel() {
        return representativeChannel(preset());
    }

    public int representativeMode() {
        return representativeSetting(preset()).modeOrdinal();
    }

    public ConfiguratorPresetCategory representativeModeCategory() {
        ConfiguratorPreset preset = preset();
        return switch (selectedCategory) {
            case ENERGY, FLUID, ITEM -> selectedCategory;
            case ALL -> {
                if (preset.energy().present()) {
                    yield ConfiguratorPresetCategory.ENERGY;
                }
                if (firstPresent(preset.fluidModules()).present()) {
                    yield ConfiguratorPresetCategory.FLUID;
                }
                if (firstPresent(preset.itemModules()).present()) {
                    yield ConfiguratorPresetCategory.ITEM;
                }
                yield ConfiguratorPresetCategory.ENERGY;
            }
        };
    }

    public RelativeDirection representativeAttach() {
        return representativeSetting(preset()).attachAs();
    }

    private ConfiguratorPreset attachButton(int id, ConfiguratorPreset preset) {
        int ordinal = id - BUTTON_ATTACH_BASE;
        Direction[] values = Direction.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return preset.setCategoryAttach(selectedCategory, RelativeDirection.absolute(values[ordinal]));
    }

    private int representativeChannel(ConfiguratorPreset preset) {
        return representativeSetting(preset).channel();
    }

    private ConnectorSetting representativeSetting(ConfiguratorPreset preset) {
        return switch (selectedCategory) {
            case ENERGY -> preset.energy();
            case FLUID -> firstPresent(preset.fluidModules());
            case ITEM -> firstPresent(preset.itemModules());
            case ALL -> {
                if (preset.energy().present()) {
                    yield preset.energy();
                }
                ConnectorSetting fluid = firstPresent(preset.fluidModules());
                if (fluid.present()) {
                    yield fluid;
                }
                ConnectorSetting item = firstPresent(preset.itemModules());
                yield item.present() ? item : preset.energy();
            }
        };
    }

    private static ConnectorSetting firstPresent(java.util.List<ConnectorSetting> settings) {
        for (ConnectorSetting setting : settings) {
            if (setting.present()) {
                return setting;
            }
        }
        return settings.isEmpty() ? ConnectorSetting.ABSENT : settings.get(0);
    }

    private ItemStack configuratorStack() {
        return inventory.player.getItemInHand(hand);
    }

    private boolean holdsConfigurator(Player player) {
        return player.getItemInHand(hand).is(ModItems.CONFIGURATOR.get());
    }

    private static boolean isKnownButton(int id) {
        if (id >= BUTTON_CATEGORY && id <= BUTTON_CYCLE_MODE) {
            return true;
        }
        if (id == BUTTON_ATTACH_DEFAULT || id == BUTTON_ATTACH_NONE) {
            return true;
        }
        int attach = id - BUTTON_ATTACH_BASE;
        return attach >= 0 && attach < Direction.values().length;
    }

    private void syncHeldStack(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : serverPlayer.getInventory().selected;
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                -2,
                0,
                slot,
                serverPlayer.getItemInHand(hand).copy()
        ));
    }
}
