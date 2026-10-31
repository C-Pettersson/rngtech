package com.rngtech.content.blockentity;

import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.ItemConnectorMode;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.menu.UniversalConnectorMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.wrench.LinkStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.EnumMap;
import java.util.Map;

public class UniversalConnectorBlockEntity extends BlockEntity
        implements MenuProvider, UniversalConnectorAccess, UniversalConnectorDataOwner {
    private static final Direction[] DIRECTIONS = Direction.values();
    public static final int ENERGY_MODULE_SLOT_COUNT = 3;
    public static final int SLOT_FLUID_CONNECTOR_START = 1;
    public static final int FLUID_MODULE_SLOT_COUNT = 3;
    public static final int SLOT_ITEM_CONNECTOR_START = SLOT_FLUID_CONNECTOR_START + FLUID_MODULE_SLOT_COUNT;
    public static final int ITEM_MODULE_SLOT_COUNT = 3;
    public static final int SLOT_RESERVED_CONNECTOR = SLOT_ITEM_CONNECTOR_START + ITEM_MODULE_SLOT_COUNT;
    /** The first Energy row keeps slot 0 from before rows existed; the others follow the Bridge slot. */
    private static final int SLOT_EXTRA_ENERGY_CONNECTOR_START = SLOT_RESERVED_CONNECTOR + 1;
    public static final int SLOT_COUNT = SLOT_EXTRA_ENERGY_CONNECTOR_START + ENERGY_MODULE_SLOT_COUNT - 1;
    /** Connector slots before the Energy rows, which is also where old saves kept their filters. */
    public static final int LEGACY_SLOT_COUNT = SLOT_RESERVED_CONNECTOR + 1;
    public static final int FILTER_SLOTS_PER_MODULE = 2;
    public static final int SLOT_FLUID_FILTER_START = 0;
    public static final int FLUID_FILTER_SLOT_COUNT = FLUID_MODULE_SLOT_COUNT * FILTER_SLOTS_PER_MODULE;
    public static final int SLOT_ITEM_FILTER_START = SLOT_FLUID_FILTER_START + FLUID_FILTER_SLOT_COUNT;
    public static final int ITEM_FILTER_SLOT_COUNT = ITEM_MODULE_SLOT_COUNT * FILTER_SLOTS_PER_MODULE;
    public static final int FILTER_SLOT_COUNT = FLUID_FILTER_SLOT_COUNT + ITEM_FILTER_SLOT_COUNT;
    public static final int MAX_CHANNEL = 15;


    private static final int DATA_ENERGY_MODULE_STRIDE = 8;
    private static final int DATA_ENERGY_CHANNEL_OFFSET = 0;
    private static final int DATA_ENERGY_ATTACH_AS_OFFSET = 1;
    private static final int DATA_ENERGY_MODE_OFFSET = 2;
    private static final int DATA_ENERGY_TRANSFER_RATE_OFFSET = 3;
    private static final int DATA_ENERGY_DISTRIBUTION_OFFSET = 4;
    private static final int DATA_ENERGY_LAST_INPUT_OFFSET = 5;
    private static final int DATA_ENERGY_LAST_OUTPUT_OFFSET = 6;
    private static final int DATA_ENERGY_TARGET_ACCESS_OFFSET = 7;
    private static final int DATA_ENERGY_MODULE_BASE = 0;
    private static final int DATA_BRIDGE_CHANNEL = DATA_ENERGY_MODULE_BASE + ENERGY_MODULE_SLOT_COUNT * DATA_ENERGY_MODULE_STRIDE;
    private static final int DATA_BRIDGE_TYPE = DATA_BRIDGE_CHANNEL + 1;
    private static final int DATA_BRIDGE_MOD_LOADED = DATA_BRIDGE_TYPE + 1;
    private static final int DATA_BRIDGE_ENDPOINT_COUNT = DATA_BRIDGE_MOD_LOADED + 1;
    private static final int DATA_MODULE_STRIDE = 5;
    private static final int DATA_MODULE_CHANNEL_OFFSET = 0;
    private static final int DATA_MODULE_ATTACH_AS_OFFSET = 1;
    private static final int DATA_MODULE_MODE_OFFSET = 2;
    private static final int DATA_MODULE_COOLDOWN_OFFSET = 3;
    private static final int DATA_MODULE_JAM_TICKS_OFFSET = 4;
    private static final int DATA_FLUID_MODULE_BASE = DATA_BRIDGE_ENDPOINT_COUNT + 1;
    private static final int DATA_ITEM_MODULE_BASE = DATA_FLUID_MODULE_BASE + FLUID_MODULE_SLOT_COUNT * DATA_MODULE_STRIDE;
    private static final int DATA_NETWORK_CABLE_NODES = DATA_ITEM_MODULE_BASE + ITEM_MODULE_SLOT_COUNT * DATA_MODULE_STRIDE;
    private static final int DATA_NETWORK_UNIVERSAL_CONNECTORS = DATA_NETWORK_CABLE_NODES + 1;
    private static final int DATA_NETWORK_ENERGY_ENDPOINTS = DATA_NETWORK_UNIVERSAL_CONNECTORS + 1;
    private static final int DATA_NETWORK_ENERGY_MODULES = DATA_NETWORK_ENERGY_ENDPOINTS + 1;
    private static final int DATA_NETWORK_FLUID_MODULES = DATA_NETWORK_ENERGY_MODULES + 1;
    private static final int DATA_NETWORK_ITEM_MODULES = DATA_NETWORK_FLUID_MODULES + 1;
    private static final int DATA_NETWORK_BRIDGE_MODULES = DATA_NETWORK_ITEM_MODULES + 1;
    private static final int DATA_NETWORK_AE2_BRIDGE_ENDPOINTS = DATA_NETWORK_BRIDGE_MODULES + 1;
    private static final int DATA_NETWORK_RS_BRIDGE_ENDPOINTS = DATA_NETWORK_AE2_BRIDGE_ENDPOINTS + 1;
    private static final int DATA_NETWORK_BRIDGE_CHANNELS = DATA_NETWORK_RS_BRIDGE_ENDPOINTS + 1;
    private static final int DATA_NETWORK_ACTIVE_CHANNELS = DATA_NETWORK_BRIDGE_CHANNELS + 1;
    private static final int DATA_NETWORK_ENERGY_CHANNELS = DATA_NETWORK_ACTIVE_CHANNELS + 1;
    private static final int DATA_NETWORK_FLUID_CHANNELS = DATA_NETWORK_ENERGY_CHANNELS + 1;
    private static final int DATA_NETWORK_ITEM_CHANNELS = DATA_NETWORK_FLUID_CHANNELS + 1;
    private static final int DATA_NETWORK_ENERGY_TRANSFER_CAP = DATA_NETWORK_ITEM_CHANNELS + 1;
    private static final int DATA_NETWORK_FLUID_SHIPMENT_CAP = DATA_NETWORK_ENERGY_TRANSFER_CAP + 1;
    private static final int DATA_NETWORK_ITEM_SHIPMENT_CAP = DATA_NETWORK_FLUID_SHIPMENT_CAP + 1;
    private static final int DATA_NETWORK_CACHE_AGE = DATA_NETWORK_ITEM_SHIPMENT_CAP + 1;
    private static final int DATA_NETWORK_DEBUG_END = DATA_NETWORK_CACHE_AGE + 1;
    private static final int DATA_NETWORK_ENERGY_LIVE_INPUT = DATA_NETWORK_DEBUG_END;
    private static final int DATA_NETWORK_ENERGY_LIVE_OUTPUT = DATA_NETWORK_ENERGY_LIVE_INPUT + 1;
    private static final int DATA_NETWORK_ENERGY_INPUT_1M = DATA_NETWORK_ENERGY_LIVE_OUTPUT + 1;
    private static final int DATA_NETWORK_ENERGY_OUTPUT_1M = DATA_NETWORK_ENERGY_INPUT_1M + 1;
    private static final int DATA_NETWORK_ENERGY_INPUT_5M = DATA_NETWORK_ENERGY_OUTPUT_1M + 1;
    private static final int DATA_NETWORK_ENERGY_OUTPUT_5M = DATA_NETWORK_ENERGY_INPUT_5M + 1;
    private static final int DATA_NETWORK_ENERGY_INPUT_15M = DATA_NETWORK_ENERGY_OUTPUT_5M + 1;
    private static final int DATA_NETWORK_ENERGY_OUTPUT_15M = DATA_NETWORK_ENERGY_INPUT_15M + 1;
    private static final int DATA_NETWORK_ENERGY_INPUT_CAP = DATA_NETWORK_ENERGY_OUTPUT_15M + 1;
    private static final int DATA_NETWORK_ENERGY_OUTPUT_CAP = DATA_NETWORK_ENERGY_INPUT_CAP + 1;
    private static final int DATA_NETWORK_ENERGY_CHANNEL_CAP = DATA_NETWORK_ENERGY_OUTPUT_CAP + 1;
    private static final int DATA_COUNT = DATA_NETWORK_ENERGY_CHANNEL_CAP + 1;

    private final EnumMap<Direction, CableUniversalConnectorData> connectors = new EnumMap<>(Direction.class);

    public UniversalConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.UNIVERSAL_CONNECTOR.get(), pos, blockState);
        connectors.put(targetDirection(blockState), new CableUniversalConnectorData(this, targetDirection(blockState)));
    }

    public CableUniversalConnectorData universalConnector(Direction face) {
        return connector(face);
    }

    public UniversalConnectorAccess universalConnectorAccess(Direction face) {
        return connector(face);
    }

    @Override
    public boolean hasUniversalConnector(Direction face) {
        return connectors.containsKey(face);
    }

    public boolean installUniversalConnector(Direction face) {
        if (face == null || connectors.containsKey(face)) {
            return false;
        }
        connectors.put(face, new CableUniversalConnectorData(this, face));
        universalConnectorStateChanged();
        return true;
    }

    public int connectorCount() {
        return connectors.size();
    }

    public Map<Direction, CompoundTag> exportConnectorDataByFace(HolderLookup.Provider registries) {
        EnumMap<Direction, CompoundTag> exported = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, CableUniversalConnectorData> entry : connectors.entrySet()) {
            exported.put(entry.getKey(), entry.getValue().save(registries));
        }
        connectors.clear();
        universalConnectorStateChanged();
        return exported;
    }

    private CableUniversalConnectorData primaryConnector() {
        Direction face = targetDirection();
        CableUniversalConnectorData connector = connectors.get(face);
        if (connector == null) {
            connector = new CableUniversalConnectorData(this, face);
            connectors.put(face, connector);
        }
        return connector;
    }

    private CableUniversalConnectorData connector(Direction face) {
        return face == null ? null : connectors.get(face);
    }

    public ItemStackHandler getInventory() {
        return primaryConnector().getInventory();
    }

    @Override
    public ItemStackHandler getFilterInventory() {
        return primaryConnector().getFilterInventory();
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        CableUniversalConnectorData connector = connector(side);
        return connector == null ? null : connector.getEnergyStorage(side);
    }

    private void invalidateNetworkCache() {
        if (level != null) {
            CableBlockEntity.invalidateNetworkCache(level, worldPosition);
        }
    }

    @Override
    public boolean clearNetworkCache() {
        invalidateNetworkCache();
        return true;
    }

    @Override
    public void clearConnectorNetworkCache() {
        invalidateNetworkCache();
    }

    public ContainerData menuData() {
        return primaryConnector().menuData();
    }

    @Override
    public LinkStatus energyLinkStatus() {
        return primaryConnector().energyLinkStatus();
    }

    @Override
    public LinkStatus energyLinkStatus(int moduleIndex) {
        return primaryConnector().energyLinkStatus(moduleIndex);
    }

    @Override
    public LinkStatus fluidLinkStatus(int moduleIndex) {
        return primaryConnector().fluidLinkStatus(moduleIndex);
    }

    @Override
    public LinkStatus itemLinkStatus(int moduleIndex) {
        return primaryConnector().itemLinkStatus(moduleIndex);
    }

    public void universalConnectorStateChanged() {
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            CableBlockEntity.invalidateNetworkCache(level, worldPosition);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, UniversalConnectorBlockEntity connector) {
        BlockState updatedState = UniversalConnectorBlock.refreshCableLinks(level, pos, state);
        if (updatedState != state) {
            level.setBlock(pos, updatedState, Block.UPDATE_ALL);
        }
        for (CableUniversalConnectorData data : connector.connectors.values()) {
            data.serverTick();
        }
    }

    @Override
    public boolean incrementEnergyChannel(int moduleIndex, int amount) {
        return primaryConnector().incrementEnergyChannel(moduleIndex, amount);
    }

    @Override
    public boolean setEnergyChannel(int moduleIndex, int channel) {
        return primaryConnector().setEnergyChannel(moduleIndex, channel);
    }

    @Override
    public boolean incrementBridgeChannel(int amount) {
        return primaryConnector().incrementBridgeChannel(amount);
    }

    @Override
    public boolean setBridgeChannel(int channel) {
        return primaryConnector().setBridgeChannel(channel);
    }

    @Override
    public boolean cycleEnergyAttachAs(int moduleIndex) {
        return primaryConnector().cycleEnergyAttachAs(moduleIndex);
    }

    @Override
    public boolean setEnergyAttachAs(int moduleIndex, Direction attachAs) {
        return primaryConnector().setEnergyAttachAs(moduleIndex, attachAs);
    }

    @Override
    public boolean cycleEnergyMode(int moduleIndex) {
        return primaryConnector().cycleEnergyMode(moduleIndex);
    }

    @Override
    public boolean setEnergyMode(int moduleIndex, CableConnectorMode mode) {
        return primaryConnector().setEnergyMode(moduleIndex, mode);
    }

    @Override
    public boolean cycleEnergyDistributionMode(int moduleIndex) {
        return primaryConnector().cycleEnergyDistributionMode(moduleIndex);
    }

    @Override
    public boolean setEnergyDistributionMode(int moduleIndex, EnergyDistributionMode distributionMode) {
        return primaryConnector().setEnergyDistributionMode(moduleIndex, distributionMode);
    }

    public boolean incrementFluidChannel(int moduleIndex, int amount) {
        return primaryConnector().incrementFluidChannel(moduleIndex, amount);
    }

    @Override
    public boolean setFluidChannel(int moduleIndex, int channel) {
        return primaryConnector().setFluidChannel(moduleIndex, channel);
    }

    @Override
    public boolean cycleFluidAttachAs(int moduleIndex) {
        return primaryConnector().cycleFluidAttachAs(moduleIndex);
    }

    @Override
    public boolean setFluidAttachAs(int moduleIndex, Direction attachAs) {
        return primaryConnector().setFluidAttachAs(moduleIndex, attachAs);
    }

    @Override
    public boolean cycleFluidMode(int moduleIndex) {
        return primaryConnector().cycleFluidMode(moduleIndex);
    }

    @Override
    public boolean setFluidMode(int moduleIndex, FluidConnectorMode mode) {
        return primaryConnector().setFluidMode(moduleIndex, mode);
    }

    @Override
    public boolean setFluidFilter(int moduleIndex, int filterIndex, ItemStack stack) {
        return primaryConnector().setFluidFilter(moduleIndex, filterIndex, stack);
    }

    public boolean incrementItemChannel(int moduleIndex, int amount) {
        return primaryConnector().incrementItemChannel(moduleIndex, amount);
    }

    @Override
    public boolean setItemChannel(int moduleIndex, int channel) {
        return primaryConnector().setItemChannel(moduleIndex, channel);
    }

    @Override
    public boolean cycleItemAttachAs(int moduleIndex) {
        return primaryConnector().cycleItemAttachAs(moduleIndex);
    }

    @Override
    public boolean setItemAttachAs(int moduleIndex, Direction attachAs) {
        return primaryConnector().setItemAttachAs(moduleIndex, attachAs);
    }

    @Override
    public boolean cycleItemMode(int moduleIndex) {
        return primaryConnector().cycleItemMode(moduleIndex);
    }

    @Override
    public boolean setItemMode(int moduleIndex, ItemConnectorMode mode) {
        return primaryConnector().setItemMode(moduleIndex, mode);
    }

    @Override
    public boolean setItemFilter(int moduleIndex, int filterIndex, ItemStack stack) {
        return primaryConnector().setItemFilter(moduleIndex, filterIndex, stack);
    }

    public Direction targetDirection() {
        return targetDirection(getBlockState());
    }

    @Override
    public Direction defaultAttachAs() {
        return primaryConnector().defaultAttachAs();
    }

    public void dropInventory(Level level) {
        int connectorItemsToDrop = Math.max(0, connectors.size() - 1);
        for (CableUniversalConnectorData connector : connectors.values()) {
            connector.dropInventory(level);
        }
        for (int index = 0; index < connectorItemsToDrop; index++) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    new ItemStack(com.rngtech.content.registry.ModItems.UNIVERSAL_CONNECTOR.get())
            );
        }
    }

    public CompoundTag exportConnectorData(HolderLookup.Provider registries) {
        CompoundTag tag = primaryConnector().save(registries);
        connectors.remove(targetDirection());
        universalConnectorStateChanged();
        return tag;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.universal_connector");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        CableUniversalConnectorData connector = primaryConnector();
        return new UniversalConnectorMenu(containerId, playerInventory, connector, connector.menuData());
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeByte(targetDirection().ordinal());
    }

    @Override
    public Level getConnectorLevel() {
        return level;
    }

    @Override
    public BlockPos getConnectorPos() {
        return worldPosition;
    }

    @Override
    public boolean isMenuAvailable(Player player) {
        return level != null
                && level.getBlockState(worldPosition).getBlock() instanceof UniversalConnectorBlock
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D
                ) <= 64.0D;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag connectorsTag = new CompoundTag();
        for (Map.Entry<Direction, CableUniversalConnectorData> entry : connectors.entrySet()) {
            connectorsTag.put(entry.getKey().getSerializedName(), entry.getValue().save(registries));
        }
        tag.put("Connectors", connectorsTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        connectors.clear();
        if (tag.contains("Connectors", Tag.TAG_COMPOUND)) {
            CompoundTag connectorsTag = tag.getCompound("Connectors");
            for (Direction direction : DIRECTIONS) {
                String key = direction.getSerializedName();
                if (!connectorsTag.contains(key, Tag.TAG_COMPOUND)) {
                    continue;
                }
                CableUniversalConnectorData connector = new CableUniversalConnectorData(this, direction);
                connector.load(connectorsTag.getCompound(key), registries);
                connectors.put(direction, connector);
            }
            if (connectors.isEmpty()) {
                connectors.put(targetDirection(), new CableUniversalConnectorData(this, targetDirection()));
            }
            invalidateNetworkCache();
            return;
        }

        CableUniversalConnectorData connector = new CableUniversalConnectorData(this, targetDirection());
        connector.load(tag, registries);
        connectors.put(targetDirection(), connector);
        invalidateNetworkCache();
    }

    public static int energyConnectorSlot(int moduleIndex) {
        return moduleIndex == 0 ? 0 : SLOT_EXTRA_ENERGY_CONNECTOR_START + moduleIndex - 1;
    }

    public static boolean isEnergyConnectorSlot(int slot) {
        return slot == 0
                || slot >= SLOT_EXTRA_ENERGY_CONNECTOR_START
                && slot < SLOT_EXTRA_ENERGY_CONNECTOR_START + ENERGY_MODULE_SLOT_COUNT - 1;
    }

    public static int fluidConnectorSlot(int moduleIndex) {
        return SLOT_FLUID_CONNECTOR_START + moduleIndex;
    }

    public static boolean isFluidConnectorSlot(int slot) {
        return slot >= SLOT_FLUID_CONNECTOR_START && slot < SLOT_FLUID_CONNECTOR_START + FLUID_MODULE_SLOT_COUNT;
    }

    public static int fluidFilterSlot(int moduleIndex, int filterIndex) {
        return SLOT_FLUID_FILTER_START + moduleIndex * FILTER_SLOTS_PER_MODULE + filterIndex;
    }

    public static boolean isFluidFilterSlot(int slot) {
        return slot >= SLOT_FLUID_FILTER_START && slot < SLOT_FLUID_FILTER_START + FLUID_FILTER_SLOT_COUNT;
    }

    public static int itemConnectorSlot(int moduleIndex) {
        return SLOT_ITEM_CONNECTOR_START + moduleIndex;
    }

    public static boolean isItemConnectorSlot(int slot) {
        return slot >= SLOT_ITEM_CONNECTOR_START && slot < SLOT_ITEM_CONNECTOR_START + ITEM_MODULE_SLOT_COUNT;
    }

    public static int itemFilterSlot(int moduleIndex, int filterIndex) {
        return SLOT_ITEM_FILTER_START + moduleIndex * FILTER_SLOTS_PER_MODULE + filterIndex;
    }

    public static boolean isItemFilterSlot(int slot) {
        return slot >= SLOT_ITEM_FILTER_START && slot < SLOT_ITEM_FILTER_START + ITEM_FILTER_SLOT_COUNT;
    }

    private static Direction targetDirection(BlockState state) {
        return state.hasProperty(UniversalConnectorBlock.FACING)
                ? state.getValue(UniversalConnectorBlock.FACING)
                : Direction.NORTH;
    }

    public static int dataEnergyChannelIndex(int moduleIndex) {
        return energyDataIndex(moduleIndex, DATA_ENERGY_CHANNEL_OFFSET);
    }

    public static int dataEnergyAttachAsIndex(int moduleIndex) {
        return energyDataIndex(moduleIndex, DATA_ENERGY_ATTACH_AS_OFFSET);
    }

    public static int dataEnergyModeIndex(int moduleIndex) {
        return energyDataIndex(moduleIndex, DATA_ENERGY_MODE_OFFSET);
    }

    public static int dataEnergyDistributionIndex(int moduleIndex) {
        return energyDataIndex(moduleIndex, DATA_ENERGY_DISTRIBUTION_OFFSET);
    }

    public static int dataEnergyTransferRateIndex(int moduleIndex) {
        return energyDataIndex(moduleIndex, DATA_ENERGY_TRANSFER_RATE_OFFSET);
    }

    public static int dataEnergyLastInputIndex(int moduleIndex) {
        return energyDataIndex(moduleIndex, DATA_ENERGY_LAST_INPUT_OFFSET);
    }

    public static int dataEnergyLastOutputIndex(int moduleIndex) {
        return energyDataIndex(moduleIndex, DATA_ENERGY_LAST_OUTPUT_OFFSET);
    }

    public static int dataEnergyTargetAccessIndex(int moduleIndex) {
        return energyDataIndex(moduleIndex, DATA_ENERGY_TARGET_ACCESS_OFFSET);
    }

    public static boolean isEnergyModuleDataIndex(int index) {
        return index >= DATA_ENERGY_MODULE_BASE && index < DATA_BRIDGE_CHANNEL;
    }

    /** The Energy row a module data index belongs to; see {@link #isEnergyModuleDataIndex}. */
    public static int energyDataModule(int index) {
        return (index - DATA_ENERGY_MODULE_BASE) / DATA_ENERGY_MODULE_STRIDE;
    }

    /** Which field of its row a module data index holds, matching the {@code dataEnergy*Index} order. */
    public static int energyDataField(int index) {
        return (index - DATA_ENERGY_MODULE_BASE) % DATA_ENERGY_MODULE_STRIDE;
    }

    private static int energyDataIndex(int moduleIndex, int offset) {
        return DATA_ENERGY_MODULE_BASE + moduleIndex * DATA_ENERGY_MODULE_STRIDE + offset;
    }

    public static int dataBridgeChannelIndex() {
        return DATA_BRIDGE_CHANNEL;
    }

    public static int dataBridgeTypeIndex() {
        return DATA_BRIDGE_TYPE;
    }

    public static int dataBridgeModLoadedIndex() {
        return DATA_BRIDGE_MOD_LOADED;
    }

    public static int dataBridgeEndpointCountIndex() {
        return DATA_BRIDGE_ENDPOINT_COUNT;
    }

    public static int dataCount() {
        return DATA_COUNT;
    }

    public static int dataNetworkEnergyInputCapIndex() {
        return DATA_NETWORK_ENERGY_INPUT_CAP;
    }

    public static int dataNetworkEnergyOutputCapIndex() {
        return DATA_NETWORK_ENERGY_OUTPUT_CAP;
    }

    public static int dataNetworkEnergyChannelCapIndex() {
        return DATA_NETWORK_ENERGY_CHANNEL_CAP;
    }

    public static boolean isNetworkDebugDataIndex(int index) {
        return index >= DATA_NETWORK_CABLE_NODES && index < DATA_NETWORK_DEBUG_END;
    }

    public static boolean isNetworkEnergyTelemetryDataIndex(int index) {
        return index >= DATA_NETWORK_ENERGY_LIVE_INPUT && index <= DATA_NETWORK_ENERGY_OUTPUT_15M;
    }

    public static int networkDebugData(int index, CableBlockEntity.NetworkDebugSnapshot snapshot) {
        return switch (index) {
            case DATA_NETWORK_CABLE_NODES -> snapshot.cableNodes();
            case DATA_NETWORK_UNIVERSAL_CONNECTORS -> snapshot.universalConnectors();
            case DATA_NETWORK_ENERGY_ENDPOINTS -> snapshot.energyEndpoints();
            case DATA_NETWORK_ENERGY_MODULES -> snapshot.energyModules();
            case DATA_NETWORK_FLUID_MODULES -> snapshot.fluidModules();
            case DATA_NETWORK_ITEM_MODULES -> snapshot.itemModules();
            case DATA_NETWORK_BRIDGE_MODULES -> snapshot.bridgeModules();
            case DATA_NETWORK_AE2_BRIDGE_ENDPOINTS -> snapshot.ae2BridgeEndpoints();
            case DATA_NETWORK_RS_BRIDGE_ENDPOINTS -> snapshot.refinedStorageBridgeEndpoints();
            case DATA_NETWORK_BRIDGE_CHANNELS -> snapshot.bridgeChannelsMask();
            case DATA_NETWORK_ACTIVE_CHANNELS -> snapshot.activeChannelsMask();
            case DATA_NETWORK_ENERGY_CHANNELS -> snapshot.energyChannelsMask();
            case DATA_NETWORK_FLUID_CHANNELS -> snapshot.fluidChannelsMask();
            case DATA_NETWORK_ITEM_CHANNELS -> snapshot.itemChannelsMask();
            case DATA_NETWORK_ENERGY_TRANSFER_CAP -> snapshot.energyTransferCap();
            case DATA_NETWORK_FLUID_SHIPMENT_CAP -> snapshot.fluidShipmentCap();
            case DATA_NETWORK_ITEM_SHIPMENT_CAP -> snapshot.itemShipmentCap();
            case DATA_NETWORK_CACHE_AGE -> snapshot.cacheAgeTicks();
            default -> 0;
        };
    }

    public static int networkEnergyTelemetryData(
            int index,
            CableBlockEntity.NetworkEnergyTelemetrySnapshot snapshot
    ) {
        return switch (index) {
            case DATA_NETWORK_ENERGY_LIVE_INPUT -> snapshot.liveInput();
            case DATA_NETWORK_ENERGY_LIVE_OUTPUT -> snapshot.liveOutput();
            case DATA_NETWORK_ENERGY_INPUT_1M -> snapshot.averageInput1m();
            case DATA_NETWORK_ENERGY_OUTPUT_1M -> snapshot.averageOutput1m();
            case DATA_NETWORK_ENERGY_INPUT_5M -> snapshot.averageInput5m();
            case DATA_NETWORK_ENERGY_OUTPUT_5M -> snapshot.averageOutput5m();
            case DATA_NETWORK_ENERGY_INPUT_15M -> snapshot.averageInput15m();
            case DATA_NETWORK_ENERGY_OUTPUT_15M -> snapshot.averageOutput15m();
            default -> 0;
        };
    }

    public static int dataNetworkCableNodesIndex() {
        return DATA_NETWORK_CABLE_NODES;
    }

    public static int dataNetworkUniversalConnectorsIndex() {
        return DATA_NETWORK_UNIVERSAL_CONNECTORS;
    }

    public static int dataNetworkEnergyEndpointsIndex() {
        return DATA_NETWORK_ENERGY_ENDPOINTS;
    }

    public static int dataNetworkEnergyModulesIndex() {
        return DATA_NETWORK_ENERGY_MODULES;
    }

    public static int dataNetworkFluidModulesIndex() {
        return DATA_NETWORK_FLUID_MODULES;
    }

    public static int dataNetworkItemModulesIndex() {
        return DATA_NETWORK_ITEM_MODULES;
    }

    public static int dataNetworkBridgeModulesIndex() {
        return DATA_NETWORK_BRIDGE_MODULES;
    }

    public static int dataNetworkAe2BridgeEndpointsIndex() {
        return DATA_NETWORK_AE2_BRIDGE_ENDPOINTS;
    }

    public static int dataNetworkRefinedStorageBridgeEndpointsIndex() {
        return DATA_NETWORK_RS_BRIDGE_ENDPOINTS;
    }

    public static int dataNetworkBridgeChannelsIndex() {
        return DATA_NETWORK_BRIDGE_CHANNELS;
    }

    public static int dataNetworkActiveChannelsIndex() {
        return DATA_NETWORK_ACTIVE_CHANNELS;
    }

    public static int dataNetworkEnergyChannelsIndex() {
        return DATA_NETWORK_ENERGY_CHANNELS;
    }

    public static int dataNetworkFluidChannelsIndex() {
        return DATA_NETWORK_FLUID_CHANNELS;
    }

    public static int dataNetworkItemChannelsIndex() {
        return DATA_NETWORK_ITEM_CHANNELS;
    }

    public static int dataNetworkEnergyTransferCapIndex() {
        return DATA_NETWORK_ENERGY_TRANSFER_CAP;
    }

    public static int dataNetworkFluidShipmentCapIndex() {
        return DATA_NETWORK_FLUID_SHIPMENT_CAP;
    }

    public static int dataNetworkItemShipmentCapIndex() {
        return DATA_NETWORK_ITEM_SHIPMENT_CAP;
    }

    public static int dataNetworkCacheAgeIndex() {
        return DATA_NETWORK_CACHE_AGE;
    }

    public static int dataNetworkEnergyLiveInputIndex() {
        return DATA_NETWORK_ENERGY_LIVE_INPUT;
    }

    public static int dataNetworkEnergyLiveOutputIndex() {
        return DATA_NETWORK_ENERGY_LIVE_OUTPUT;
    }

    public static int dataNetworkEnergyInput1mIndex() {
        return DATA_NETWORK_ENERGY_INPUT_1M;
    }

    public static int dataNetworkEnergyOutput1mIndex() {
        return DATA_NETWORK_ENERGY_OUTPUT_1M;
    }

    public static int dataNetworkEnergyInput5mIndex() {
        return DATA_NETWORK_ENERGY_INPUT_5M;
    }

    public static int dataNetworkEnergyOutput5mIndex() {
        return DATA_NETWORK_ENERGY_OUTPUT_5M;
    }

    public static int dataNetworkEnergyInput15mIndex() {
        return DATA_NETWORK_ENERGY_INPUT_15M;
    }

    public static int dataNetworkEnergyOutput15mIndex() {
        return DATA_NETWORK_ENERGY_OUTPUT_15M;
    }

    public static int dataFluidChannelIndex(int moduleIndex) {
        return DATA_FLUID_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_CHANNEL_OFFSET;
    }

    public static int dataFluidAttachAsIndex(int moduleIndex) {
        return DATA_FLUID_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_ATTACH_AS_OFFSET;
    }

    public static int dataFluidModeIndex(int moduleIndex) {
        return DATA_FLUID_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_MODE_OFFSET;
    }

    public static int dataFluidCooldownIndex(int moduleIndex) {
        return DATA_FLUID_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_COOLDOWN_OFFSET;
    }

    public static int dataFluidJamTicksIndex(int moduleIndex) {
        return DATA_FLUID_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_JAM_TICKS_OFFSET;
    }

    public static int dataItemChannelIndex(int moduleIndex) {
        return DATA_ITEM_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_CHANNEL_OFFSET;
    }

    public static int dataItemAttachAsIndex(int moduleIndex) {
        return DATA_ITEM_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_ATTACH_AS_OFFSET;
    }

    public static int dataItemModeIndex(int moduleIndex) {
        return DATA_ITEM_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_MODE_OFFSET;
    }

    public static int dataItemCooldownIndex(int moduleIndex) {
        return DATA_ITEM_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_COOLDOWN_OFFSET;
    }

    public static int dataItemJamTicksIndex(int moduleIndex) {
        return DATA_ITEM_MODULE_BASE + moduleIndex * DATA_MODULE_STRIDE + DATA_MODULE_JAM_TICKS_OFFSET;
    }
}
