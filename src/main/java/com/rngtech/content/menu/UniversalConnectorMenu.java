package com.rngtech.content.menu;

import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.CableUniversalConnectorData;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.ItemConnectorMode;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.item.NetworkConnectorItem;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class UniversalConnectorMenu extends AbstractContainerMenu {
    public static final int TAB_ENERGY = 0;
    public static final int TAB_FLUID = 1;
    public static final int TAB_ITEM = 2;
    public static final int TAB_BRIDGE = 3;
    public static final int TAB_NETWORK = 4;
    public static final int BUTTON_CHANNEL_DOWN = 0;
    public static final int BUTTON_CHANNEL_UP = 1;
    public static final int BUTTON_MODE = 2;
    public static final int BUTTON_DISTRIBUTION = 3;
    public static final int BUTTON_CLEAR_NETWORK_CACHE = 4;
    public static final int BUTTON_BRIDGE_CHANNEL_DOWN = 5;
    public static final int BUTTON_BRIDGE_CHANNEL_UP = 6;
    public static final int BUTTON_ATTACH_AS_BASE = 10;
    public static final int BUTTON_TAB_BASE = 100;
    public static final int BUTTON_FLUID_CHANNEL_DOWN_BASE = 300;
    public static final int BUTTON_FLUID_CHANNEL_UP_BASE = 310;
    public static final int BUTTON_FLUID_ATTACH_BASE = 320;
    public static final int BUTTON_FLUID_MODE_BASE = 330;
    public static final int BUTTON_ITEM_CHANNEL_DOWN_BASE = 200;
    public static final int BUTTON_ITEM_CHANNEL_UP_BASE = 210;
    public static final int BUTTON_ITEM_ATTACH_BASE = 220;
    public static final int BUTTON_ITEM_MODE_BASE = 230;
    public static final int FLUID_MODULE_COUNT = UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT;
    public static final int ITEM_MODULE_COUNT = UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT;
    private static final int DATA_COUNT = UniversalConnectorBlockEntity.dataCount();
    private static final int ENERGY_CONNECTOR_SLOT = UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR;
    private static final int FLUID_CONNECTOR_SLOT_START = UniversalConnectorBlockEntity.SLOT_FLUID_CONNECTOR_START;
    private static final int FLUID_CONNECTOR_SLOT_END = FLUID_CONNECTOR_SLOT_START + FLUID_MODULE_COUNT;
    private static final int ITEM_CONNECTOR_SLOT_START = UniversalConnectorBlockEntity.SLOT_ITEM_CONNECTOR_START;
    private static final int ITEM_CONNECTOR_SLOT_END = ITEM_CONNECTOR_SLOT_START + ITEM_MODULE_COUNT;
    private static final int BRIDGE_CONNECTOR_SLOT = UniversalConnectorBlockEntity.SLOT_RESERVED_CONNECTOR;
    private static final int FILTER_MENU_SLOT_START = UniversalConnectorBlockEntity.SLOT_COUNT;
    private static final int FLUID_FILTER_MENU_SLOT_START = FILTER_MENU_SLOT_START;
    private static final int FLUID_FILTER_MENU_SLOT_END = FLUID_FILTER_MENU_SLOT_START
            + UniversalConnectorBlockEntity.FLUID_FILTER_SLOT_COUNT;
    private static final int ITEM_FILTER_MENU_SLOT_START = FLUID_FILTER_MENU_SLOT_END;
    private static final int ITEM_FILTER_MENU_SLOT_END = ITEM_FILTER_MENU_SLOT_START
            + UniversalConnectorBlockEntity.ITEM_FILTER_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = ITEM_FILTER_MENU_SLOT_END;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final UniversalConnectorAccess connector;
    private int selectedTab = TAB_ENERGY;

    public UniversalConnectorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                connectorAccess(playerInventory, extraData.readBlockPos(), extraData.readByte()),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public UniversalConnectorMenu(
            int containerId,
            Inventory playerInventory,
            UniversalConnectorAccess connector,
            ContainerData data
    ) {
        super(ModMenus.UNIVERSAL_CONNECTOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(connector.getConnectorLevel(), connector.getConnectorPos());
        this.data = data;
        this.connector = connector;

        addSlot(new TabbedSlot(
                connector.getInventory(),
                UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR,
                30,
                60,
                () -> selectedTab == TAB_ENERGY
        ));
        for (int moduleIndex = 0; moduleIndex < FLUID_MODULE_COUNT; moduleIndex++) {
            addSlot(new TabbedSlot(
                    connector.getInventory(),
                    UniversalConnectorBlockEntity.fluidConnectorSlot(moduleIndex),
                    30,
                    60 + moduleIndex * 32,
                    () -> selectedTab == TAB_FLUID
            ));
        }
        for (int moduleIndex = 0; moduleIndex < ITEM_MODULE_COUNT; moduleIndex++) {
            addSlot(new TabbedSlot(
                    connector.getInventory(),
                    UniversalConnectorBlockEntity.itemConnectorSlot(moduleIndex),
                    30,
                    60 + moduleIndex * 32,
                    () -> selectedTab == TAB_ITEM
            ));
        }
        addSlot(new TabbedSlot(
                connector.getInventory(),
                UniversalConnectorBlockEntity.SLOT_RESERVED_CONNECTOR,
                30,
                60,
                () -> selectedTab == TAB_BRIDGE
        ));
        for (int moduleIndex = 0; moduleIndex < FLUID_MODULE_COUNT; moduleIndex++) {
            for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
                addSlot(new GhostFilterSlot(
                        connector,
                        connector.getFilterInventory(),
                        UniversalConnectorBlockEntity.fluidFilterSlot(moduleIndex, filterIndex),
                        true,
                        moduleIndex,
                        filterIndex,
                        252 + filterIndex * 18,
                        60 + moduleIndex * 32,
                        () -> selectedTab == TAB_FLUID
                ));
            }
        }
        for (int moduleIndex = 0; moduleIndex < ITEM_MODULE_COUNT; moduleIndex++) {
            for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
                addSlot(new GhostFilterSlot(
                        connector,
                        connector.getFilterInventory(),
                        UniversalConnectorBlockEntity.itemFilterSlot(moduleIndex, filterIndex),
                        false,
                        moduleIndex,
                        filterIndex,
                        252 + filterIndex * 18,
                        60 + moduleIndex * 32,
                        () -> selectedTab == TAB_ITEM
                ));
            }
        }
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_FLUID -> TAB_FLUID;
            case TAB_ITEM -> TAB_ITEM;
            case TAB_BRIDGE -> TAB_BRIDGE;
            case TAB_NETWORK -> TAB_NETWORK;
            default -> TAB_ENERGY;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public int channel() {
        return data.get(UniversalConnectorBlockEntity.dataChannelIndex());
    }

    public Direction attachAs() {
        int ordinal = data.get(UniversalConnectorBlockEntity.dataAttachAsIndex());
        Direction[] values = Direction.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Direction.NORTH;
    }

    public int modeOrdinal() {
        return data.get(UniversalConnectorBlockEntity.dataModeIndex());
    }

    public int distributionOrdinal() {
        return data.get(UniversalConnectorBlockEntity.dataDistributionIndex());
    }

    public int transferRate() {
        return data.get(UniversalConnectorBlockEntity.dataTransferRateIndex());
    }

    public boolean hasConnector() {
        return data.get(UniversalConnectorBlockEntity.dataHasConnectorIndex()) != 0;
    }

    public boolean targetHasEnergyAccess() {
        return data.get(UniversalConnectorBlockEntity.dataEnergyTargetAccessIndex()) != 0;
    }

    public int lastEnergyInput() {
        return data.get(UniversalConnectorBlockEntity.dataLastEnergyInputIndex());
    }

    public int lastEnergyOutput() {
        return data.get(UniversalConnectorBlockEntity.dataLastEnergyOutputIndex());
    }

    public int bridgeChannel() {
        return data.get(UniversalConnectorBlockEntity.dataBridgeChannelIndex());
    }

    public NetworkBridgeType bridgeType() {
        return NetworkBridgeType.byDataId(data.get(UniversalConnectorBlockEntity.dataBridgeTypeIndex()));
    }

    public boolean bridgeModLoaded() {
        return data.get(UniversalConnectorBlockEntity.dataBridgeModLoadedIndex()) != 0;
    }

    public int bridgeEndpointCount() {
        return data.get(UniversalConnectorBlockEntity.dataBridgeEndpointCountIndex());
    }

    public ItemStack bridgeModuleStack() {
        return getSlot(BRIDGE_CONNECTOR_SLOT).getItem();
    }

    public int fluidChannel(int moduleIndex) {
        return data.get(UniversalConnectorBlockEntity.dataFluidChannelIndex(moduleIndex));
    }

    public Direction fluidAttachAs(int moduleIndex) {
        int ordinal = data.get(UniversalConnectorBlockEntity.dataFluidAttachAsIndex(moduleIndex));
        Direction[] values = Direction.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    public int fluidModeOrdinal(int moduleIndex) {
        return data.get(UniversalConnectorBlockEntity.dataFluidModeIndex(moduleIndex));
    }

    public int fluidCooldown(int moduleIndex) {
        return data.get(UniversalConnectorBlockEntity.dataFluidCooldownIndex(moduleIndex));
    }

    public int fluidJamTicks(int moduleIndex) {
        return data.get(UniversalConnectorBlockEntity.dataFluidJamTicksIndex(moduleIndex));
    }

    public ItemStack fluidModuleStack(int moduleIndex) {
        return getSlot(UniversalConnectorBlockEntity.fluidConnectorSlot(moduleIndex)).getItem();
    }

    public int itemChannel(int moduleIndex) {
        return data.get(UniversalConnectorBlockEntity.dataItemChannelIndex(moduleIndex));
    }

    public Direction itemAttachAs(int moduleIndex) {
        int ordinal = data.get(UniversalConnectorBlockEntity.dataItemAttachAsIndex(moduleIndex));
        Direction[] values = Direction.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    public int itemModeOrdinal(int moduleIndex) {
        return data.get(UniversalConnectorBlockEntity.dataItemModeIndex(moduleIndex));
    }

    public int itemCooldown(int moduleIndex) {
        return data.get(UniversalConnectorBlockEntity.dataItemCooldownIndex(moduleIndex));
    }

    public int itemJamTicks(int moduleIndex) {
        return data.get(UniversalConnectorBlockEntity.dataItemJamTicksIndex(moduleIndex));
    }

    public ItemStack itemModuleStack(int moduleIndex) {
        return getSlot(UniversalConnectorBlockEntity.itemConnectorSlot(moduleIndex)).getItem();
    }

    public int networkCableNodes() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkCableNodesIndex());
    }

    public int networkUniversalConnectors() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkUniversalConnectorsIndex());
    }

    public int networkEnergyEndpoints() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyEndpointsIndex());
    }

    public int networkEnergyModules() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyModulesIndex());
    }

    public int networkFluidModules() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkFluidModulesIndex());
    }

    public int networkItemModules() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkItemModulesIndex());
    }

    public int networkBridgeModules() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkBridgeModulesIndex());
    }

    public int networkAe2BridgeEndpoints() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkAe2BridgeEndpointsIndex());
    }

    public int networkRefinedStorageBridgeEndpoints() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkRefinedStorageBridgeEndpointsIndex());
    }

    public int networkBridgeChannelsMask() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkBridgeChannelsIndex()) & 0xFFFF;
    }

    public int networkActiveChannelsMask() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkActiveChannelsIndex()) & 0xFFFF;
    }

    public int networkEnergyChannelsMask() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyChannelsIndex()) & 0xFFFF;
    }

    public int networkFluidChannelsMask() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkFluidChannelsIndex()) & 0xFFFF;
    }

    public int networkItemChannelsMask() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkItemChannelsIndex()) & 0xFFFF;
    }

    public int networkEnergyTransferCap() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyTransferCapIndex());
    }

    public int networkEnergyInputCap() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyInputCapIndex());
    }

    public int networkEnergyOutputCap() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyOutputCapIndex());
    }

    public int networkEnergyChannelCap() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyChannelCapIndex());
    }

    public int networkFluidShipmentCap() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkFluidShipmentCapIndex());
    }

    public int networkItemShipmentCap() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkItemShipmentCapIndex());
    }

    public int networkCacheAgeTicks() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkCacheAgeIndex());
    }

    public int networkEnergyLiveInput() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyLiveInputIndex());
    }

    public int networkEnergyLiveOutput() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyLiveOutputIndex());
    }

    public int networkEnergyInput1m() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyInput1mIndex());
    }

    public int networkEnergyOutput1m() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyOutput1mIndex());
    }

    public int networkEnergyInput5m() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyInput5mIndex());
    }

    public int networkEnergyOutput5m() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyOutput5mIndex());
    }

    public int networkEnergyInput15m() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyInput15mIndex());
    }

    public int networkEnergyOutput15m() {
        return data.get(UniversalConnectorBlockEntity.dataNetworkEnergyOutput15mIndex());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int tab = id - BUTTON_TAB_BASE;
        if (tab >= TAB_ENERGY && tab <= TAB_NETWORK) {
            selectTab(tab);
            return true;
        }
        if (player.level().isClientSide) {
            return isKnownButton(id);
        }
        int fluidChannelDown = id - BUTTON_FLUID_CHANNEL_DOWN_BASE;
        if (isFluidModuleIndex(fluidChannelDown)) {
            return connector.incrementFluidChannel(fluidChannelDown, -1);
        }
        int fluidChannelUp = id - BUTTON_FLUID_CHANNEL_UP_BASE;
        if (isFluidModuleIndex(fluidChannelUp)) {
            return connector.incrementFluidChannel(fluidChannelUp, 1);
        }
        int fluidAttach = id - BUTTON_FLUID_ATTACH_BASE;
        if (isFluidModuleIndex(fluidAttach)) {
            return connector.cycleFluidAttachAs(fluidAttach);
        }
        int fluidMode = id - BUTTON_FLUID_MODE_BASE;
        if (isFluidModuleIndex(fluidMode)) {
            return connector.cycleFluidMode(fluidMode);
        }
        int itemChannelDown = id - BUTTON_ITEM_CHANNEL_DOWN_BASE;
        if (isItemModuleIndex(itemChannelDown)) {
            return connector.incrementItemChannel(itemChannelDown, -1);
        }
        int itemChannelUp = id - BUTTON_ITEM_CHANNEL_UP_BASE;
        if (isItemModuleIndex(itemChannelUp)) {
            return connector.incrementItemChannel(itemChannelUp, 1);
        }
        int itemAttach = id - BUTTON_ITEM_ATTACH_BASE;
        if (isItemModuleIndex(itemAttach)) {
            return connector.cycleItemAttachAs(itemAttach);
        }
        int itemMode = id - BUTTON_ITEM_MODE_BASE;
        if (isItemModuleIndex(itemMode)) {
            return connector.cycleItemMode(itemMode);
        }
        if (id == BUTTON_CHANNEL_DOWN) {
            return connector.incrementChannel(-1);
        }
        if (id == BUTTON_CHANNEL_UP) {
            return connector.incrementChannel(1);
        }
        if (id == BUTTON_BRIDGE_CHANNEL_DOWN) {
            return connector.incrementBridgeChannel(-1);
        }
        if (id == BUTTON_BRIDGE_CHANNEL_UP) {
            return connector.incrementBridgeChannel(1);
        }
        if (id == BUTTON_MODE) {
            return connector.cycleMode();
        }
        if (id == BUTTON_DISTRIBUTION) {
            return connector.cycleEnergyDistributionMode();
        }
        if (id == BUTTON_CLEAR_NETWORK_CACHE) {
            return connector.clearNetworkCache();
        }
        int attachAs = id - BUTTON_ATTACH_AS_BASE;
        Direction[] values = Direction.values();
        if (attachAs >= 0 && attachAs < values.length) {
            return connector.setAttachAs(values[attachAs]);
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof GhostFilterSlot filterSlot) {
            handleGhostFilterClick(filterSlot, button, clickType);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void handleGhostFilterClick(GhostFilterSlot slot, int button, ClickType clickType) {
        if (clickType != ClickType.PICKUP) {
            return;
        }
        ItemStack carried = getCarried();
        if (button == 1 || carried.isEmpty()) {
            slot.clearFilter();
            return;
        }
        slot.setFilter(carried);
    }

    @Override
    public boolean stillValid(Player player) {
        return connector.isMenuAvailable(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (slot instanceof GhostFilterSlot) {
            return ItemStack.EMPTY;
        }

        if (index < FILTER_MENU_SLOT_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_START) {
            return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof EnergyConnectorItem) {
            if (!moveItemStackTo(stack, ENERGY_CONNECTOR_SLOT, ENERGY_CONNECTOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidConnectorItem) {
            if (!moveItemStackTo(stack, FLUID_CONNECTOR_SLOT_START, FLUID_CONNECTOR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ItemConnectorItem) {
            if (!moveItemStackTo(stack, ITEM_CONNECTOR_SLOT_START, ITEM_CONNECTOR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof NetworkConnectorItem) {
            if (!moveItemStackTo(stack, BRIDGE_CONNECTOR_SLOT, BRIDGE_CONNECTOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return moved;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(
                        playerInventory,
                        column + row * 9 + 9,
                        35 + column * 18,
                        156 + row * 18,
                        () -> selectedTab != TAB_NETWORK
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    35 + column * 18,
                    214,
                    () -> selectedTab != TAB_NETWORK
            ));
        }
    }

    private static boolean isKnownButton(int id) {
        int tab = id - BUTTON_TAB_BASE;
        if (tab >= TAB_ENERGY && tab <= TAB_NETWORK) {
            return true;
        }
        if (id == BUTTON_CHANNEL_DOWN
                || id == BUTTON_CHANNEL_UP
                || id == BUTTON_MODE
                || id == BUTTON_DISTRIBUTION
                || id == BUTTON_CLEAR_NETWORK_CACHE
                || id == BUTTON_BRIDGE_CHANNEL_DOWN
                || id == BUTTON_BRIDGE_CHANNEL_UP) {
            return true;
        }
        if (isFluidModuleIndex(id - BUTTON_FLUID_CHANNEL_DOWN_BASE)
                || isFluidModuleIndex(id - BUTTON_FLUID_CHANNEL_UP_BASE)
                || isFluidModuleIndex(id - BUTTON_FLUID_ATTACH_BASE)
                || isFluidModuleIndex(id - BUTTON_FLUID_MODE_BASE)) {
            return true;
        }
        if (isItemModuleIndex(id - BUTTON_ITEM_CHANNEL_DOWN_BASE)
                || isItemModuleIndex(id - BUTTON_ITEM_CHANNEL_UP_BASE)
                || isItemModuleIndex(id - BUTTON_ITEM_ATTACH_BASE)
                || isItemModuleIndex(id - BUTTON_ITEM_MODE_BASE)) {
            return true;
        }
        int attachAs = id - BUTTON_ATTACH_AS_BASE;
        return attachAs >= 0 && attachAs < Direction.values().length;
    }

    private static boolean isFluidModuleIndex(int moduleIndex) {
        return moduleIndex >= 0 && moduleIndex < FLUID_MODULE_COUNT;
    }

    private static boolean isItemModuleIndex(int moduleIndex) {
        return moduleIndex >= 0 && moduleIndex < ITEM_MODULE_COUNT;
    }

    private static UniversalConnectorAccess connectorAccess(Inventory playerInventory, BlockPos pos, int sideOrdinal) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (sideOrdinal >= 0 && sideOrdinal < Direction.values().length) {
            Direction side = Direction.values()[sideOrdinal];
            if (blockEntity instanceof CableBlockEntity cable) {
                UniversalConnectorAccess connector = cable.universalConnectorAccess(side);
                if (connector != null) {
                    return connector;
                }
            }
            if (blockEntity instanceof UniversalConnectorBlockEntity connectorBlock) {
                UniversalConnectorAccess connector = connectorBlock.universalConnectorAccess(side);
                if (connector != null) {
                    return connector;
                }
            }
            return clientFallbackOrThrow(
                    playerInventory,
                    pos,
                    "Expected cable universal connector at " + pos + " side " + sideOrdinal
            );
        }
        if (blockEntity instanceof UniversalConnectorBlockEntity connector) {
            return connector;
        }
        return clientFallbackOrThrow(playerInventory, pos, "Expected universal connector block entity at " + pos);
    }

    private static UniversalConnectorAccess clientFallbackOrThrow(
            Inventory playerInventory,
            BlockPos pos,
            String errorMessage
    ) {
        if (playerInventory.player.level().isClientSide) {
            return new ClientUniversalConnectorAccess(playerInventory.player.level(), pos);
        }
        throw new IllegalStateException(errorMessage);
    }

    private static final class TabbedSlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private TabbedSlot(
                ItemStackHandler itemHandler,
                int index,
                int xPosition,
                int yPosition,
                BooleanSupplier activeSupplier
        ) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class GhostFilterSlot extends SlotItemHandler {
        private final UniversalConnectorAccess connector;
        private final boolean fluidFilter;
        private final int moduleIndex;
        private final int filterIndex;
        private final BooleanSupplier activeSupplier;

        private GhostFilterSlot(
                UniversalConnectorAccess connector,
                ItemStackHandler itemHandler,
                int index,
                boolean fluidFilter,
                int moduleIndex,
                int filterIndex,
                int xPosition,
                int yPosition,
                BooleanSupplier activeSupplier
        ) {
            super(itemHandler, index, xPosition, yPosition);
            this.connector = connector;
            this.fluidFilter = fluidFilter;
            this.moduleIndex = moduleIndex;
            this.filterIndex = filterIndex;
            this.activeSupplier = activeSupplier;
        }

        private void setFilter(ItemStack stack) {
            boolean changed = fluidFilter
                    ? connector.setFluidFilter(moduleIndex, filterIndex, stack)
                    : connector.setItemFilter(moduleIndex, filterIndex, stack);
            if (changed) {
                setChanged();
            }
        }

        private void clearFilter() {
            boolean changed = fluidFilter
                    ? connector.setFluidFilter(moduleIndex, filterIndex, ItemStack.EMPTY)
                    : connector.setItemFilter(moduleIndex, filterIndex, ItemStack.EMPTY);
            if (changed) {
                setChanged();
            }
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class TabbedInventorySlot extends Slot {
        private final BooleanSupplier activeSupplier;

        private TabbedInventorySlot(Inventory inventory, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(inventory, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class ClientUniversalConnectorAccess implements UniversalConnectorAccess {
        private final Level level;
        private final BlockPos pos;
        private final ContainerData data = new SimpleContainerData(DATA_COUNT);
        private final ItemStackHandler inventory = new ItemStackHandler(UniversalConnectorBlockEntity.SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR) {
                    return stack.getItem() instanceof EnergyConnectorItem;
                }
                if (UniversalConnectorBlockEntity.isFluidConnectorSlot(slot)) {
                    return stack.getItem() instanceof FluidConnectorItem;
                }
                if (UniversalConnectorBlockEntity.isItemConnectorSlot(slot)) {
                    return stack.getItem() instanceof ItemConnectorItem;
                }
                if (slot == UniversalConnectorBlockEntity.SLOT_RESERVED_CONNECTOR) {
                    return stack.getItem() instanceof NetworkConnectorItem;
                }
                return false;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) {
                    return stack;
                }
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        private final ItemStackHandler filterInventory = new ItemStackHandler(UniversalConnectorBlockEntity.FILTER_SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (UniversalConnectorBlockEntity.isFluidFilterSlot(slot)) {
                    return CableUniversalConnectorData.isFluidFilterStack(stack);
                }
                return UniversalConnectorBlockEntity.isItemFilterSlot(slot) && !stack.isEmpty();
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };

        private ClientUniversalConnectorAccess(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public ItemStackHandler getInventory() {
            return inventory;
        }

        @Override
        public ItemStackHandler getFilterInventory() {
            return filterInventory;
        }

        @Override
        public ContainerData menuData() {
            return data;
        }

        @Override
        public Level getConnectorLevel() {
            return level;
        }

        @Override
        public BlockPos getConnectorPos() {
            return pos;
        }

        @Override
        public Direction defaultAttachAs() {
            return Direction.NORTH;
        }

        @Override
        public boolean isMenuAvailable(Player player) {
            return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }

        @Override
        public boolean incrementChannel(int amount) {
            return false;
        }

        @Override
        public boolean setChannel(int channel) {
            return false;
        }

        @Override
        public boolean incrementBridgeChannel(int amount) {
            return false;
        }

        @Override
        public boolean setBridgeChannel(int channel) {
            return false;
        }

        @Override
        public boolean setAttachAs(Direction attachAs) {
            return false;
        }

        @Override
        public boolean cycleMode() {
            return false;
        }

        @Override
        public boolean setMode(CableConnectorMode mode) {
            return false;
        }

        @Override
        public boolean cycleEnergyDistributionMode() {
            return false;
        }

        @Override
        public boolean setEnergyDistributionMode(EnergyDistributionMode distributionMode) {
            return false;
        }

        @Override
        public boolean clearNetworkCache() {
            return false;
        }

        @Override
        public boolean incrementFluidChannel(int moduleIndex, int amount) {
            return false;
        }

        @Override
        public boolean setFluidChannel(int moduleIndex, int channel) {
            return false;
        }

        @Override
        public boolean cycleFluidAttachAs(int moduleIndex) {
            return false;
        }

        @Override
        public boolean setFluidAttachAs(int moduleIndex, Direction attachAs) {
            return false;
        }

        @Override
        public boolean cycleFluidMode(int moduleIndex) {
            return false;
        }

        @Override
        public boolean setFluidMode(int moduleIndex, FluidConnectorMode mode) {
            return false;
        }

        @Override
        public boolean setFluidFilter(int moduleIndex, int filterIndex, ItemStack stack) {
            return false;
        }

        @Override
        public boolean incrementItemChannel(int moduleIndex, int amount) {
            return false;
        }

        @Override
        public boolean setItemChannel(int moduleIndex, int channel) {
            return false;
        }

        @Override
        public boolean cycleItemAttachAs(int moduleIndex) {
            return false;
        }

        @Override
        public boolean setItemAttachAs(int moduleIndex, Direction attachAs) {
            return false;
        }

        @Override
        public boolean cycleItemMode(int moduleIndex) {
            return false;
        }

        @Override
        public boolean setItemMode(int moduleIndex, ItemConnectorMode mode) {
            return false;
        }

        @Override
        public boolean setItemFilter(int moduleIndex, int filterIndex, ItemStack stack) {
            return false;
        }
    }
}
