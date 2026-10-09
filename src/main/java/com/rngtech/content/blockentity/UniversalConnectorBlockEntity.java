package com.rngtech.content.blockentity;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.FluidConnectorTier;
import com.rngtech.content.cable.ItemConnectorMode;
import com.rngtech.content.cable.ItemConnectorTier;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.item.NetworkConnectorItem;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.menu.UniversalConnectorMenu;
import com.rngtech.content.registry.ModBlockEntities;

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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class UniversalConnectorBlockEntity extends BlockEntity
        implements MenuProvider, UniversalConnectorAccess, UniversalConnectorDataOwner {
    private static final Direction[] DIRECTIONS = Direction.values();
    public static final int SLOT_ENERGY_CONNECTOR = 0;
    public static final int SLOT_FLUID_CONNECTOR_START = 1;
    public static final int FLUID_MODULE_SLOT_COUNT = 3;
    public static final int SLOT_ITEM_CONNECTOR_START = SLOT_FLUID_CONNECTOR_START + FLUID_MODULE_SLOT_COUNT;
    public static final int ITEM_MODULE_SLOT_COUNT = 3;
    public static final int SLOT_RESERVED_CONNECTOR = SLOT_ITEM_CONNECTOR_START + ITEM_MODULE_SLOT_COUNT;
    public static final int SLOT_COUNT = SLOT_RESERVED_CONNECTOR + 1;
    public static final int FILTER_SLOTS_PER_MODULE = 2;
    public static final int SLOT_FLUID_FILTER_START = 0;
    public static final int FLUID_FILTER_SLOT_COUNT = FLUID_MODULE_SLOT_COUNT * FILTER_SLOTS_PER_MODULE;
    public static final int SLOT_ITEM_FILTER_START = SLOT_FLUID_FILTER_START + FLUID_FILTER_SLOT_COUNT;
    public static final int ITEM_FILTER_SLOT_COUNT = ITEM_MODULE_SLOT_COUNT * FILTER_SLOTS_PER_MODULE;
    public static final int FILTER_SLOT_COUNT = FLUID_FILTER_SLOT_COUNT + ITEM_FILTER_SLOT_COUNT;
    public static final int MAX_CHANNEL = 15;


    private static final int DATA_CHANNEL = 0;
    private static final int DATA_ATTACH_AS = 1;
    private static final int DATA_MODE = 2;
    private static final int DATA_TRANSFER_RATE = 3;
    private static final int DATA_DISTRIBUTION = 4;
    private static final int DATA_HAS_CONNECTOR = 5;
    private static final int DATA_LAST_ENERGY_INPUT = 6;
    private static final int DATA_LAST_ENERGY_OUTPUT = 7;
    private static final int DATA_BRIDGE_CHANNEL = 8;
    private static final int DATA_BRIDGE_TYPE = 9;
    private static final int DATA_BRIDGE_MOD_LOADED = 10;
    private static final int DATA_BRIDGE_ENDPOINT_COUNT = 11;
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
    private static final int DATA_ENERGY_TARGET_ACCESS = DATA_NETWORK_ENERGY_OUTPUT_15M + 1;
    private static final int DATA_NETWORK_ENERGY_INPUT_CAP = DATA_ENERGY_TARGET_ACCESS + 1;
    private static final int DATA_NETWORK_ENERGY_OUTPUT_CAP = DATA_NETWORK_ENERGY_INPUT_CAP + 1;
    private static final int DATA_NETWORK_ENERGY_CHANNEL_CAP = DATA_NETWORK_ENERGY_OUTPUT_CAP + 1;
    private static final int DATA_COUNT = DATA_NETWORK_ENERGY_CHANNEL_CAP + 1;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_ENERGY_CONNECTOR) {
                return stack.getItem() instanceof EnergyConnectorItem connector && connector.tier().enabled();
            }
            if (isFluidConnectorSlot(slot)) {
                return stack.getItem() instanceof FluidConnectorItem connector && connector.tier().enabled();
            }
            if (isItemConnectorSlot(slot)) {
                return stack.getItem() instanceof ItemConnectorItem connector && connector.tier().enabled();
            }
            if (slot == SLOT_RESERVED_CONNECTOR) {
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

        @Override
        protected void onContentsChanged(int slot) {
            invalidateConnectorState();
        }
    };
    private final IEnergyStorage[] sidedEnergyStorages = new IEnergyStorage[DIRECTIONS.length];
    private final FluidModuleState[] fluidModules = new FluidModuleState[FLUID_MODULE_SLOT_COUNT];
    private final ItemModuleState[] itemModules = new ItemModuleState[ITEM_MODULE_SLOT_COUNT];
    private int channel;
    private Direction attachAs;
    private CableConnectorMode mode = CableConnectorMode.BOTH;
    private EnergyDistributionMode distributionMode = EnergyDistributionMode.ROUND_ROBIN;
    private int fluidModuleTickCursor;
    private int itemModuleTickCursor;
    private long energyInputBudgetTick = Long.MIN_VALUE;
    private int energyInputTransferredThisTick;
    private final EnumMap<Direction, CableUniversalConnectorData> connectors = new EnumMap<>(Direction.class);

    public UniversalConnectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.UNIVERSAL_CONNECTOR.get(), pos, blockState);
        attachAs = targetDirection(blockState).getOpposite();
        connectors.put(targetDirection(blockState), new CableUniversalConnectorData(this, targetDirection(blockState)));
        for (Direction direction : DIRECTIONS) {
            sidedEnergyStorages[direction.ordinal()] = new ConnectorEnergyStorage(direction);
        }
        for (int index = 0; index < fluidModules.length; index++) {
            fluidModules[index] = FluidModuleState.defaults(attachAs);
        }
        for (int index = 0; index < itemModules.length; index++) {
            itemModules[index] = ItemModuleState.defaults(attachAs);
        }
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

    public void universalConnectorStateChanged() {
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            CableBlockEntity.invalidateNetworkCache(level, worldPosition);
        }
    }

    private void invalidateConnectorState() {
        universalConnectorStateChanged();
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

    public boolean incrementChannel(int amount) {
        return primaryConnector().incrementChannel(amount);
    }

    @Override
    public boolean setChannel(int channel) {
        return primaryConnector().setChannel(channel);
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
    public boolean setAttachAs(Direction attachAs) {
        return primaryConnector().setAttachAs(attachAs);
    }

    @Override
    public boolean cycleMode() {
        return primaryConnector().cycleMode();
    }

    @Override
    public boolean setMode(CableConnectorMode mode) {
        return primaryConnector().setMode(mode);
    }

    @Override
    public boolean cycleEnergyDistributionMode() {
        return primaryConnector().cycleEnergyDistributionMode();
    }

    @Override
    public boolean setEnergyDistributionMode(EnergyDistributionMode distributionMode) {
        return primaryConnector().setEnergyDistributionMode(distributionMode);
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

    public int receiveFromCableNetwork(Direction cableSide, int networkChannel, int amount, boolean simulate) {
        if (amount <= 0 || !isNetworkSide(cableSide) || networkChannel != channel || !mode.sendsNetworkOutput()) {
            return 0;
        }
        Optional<EnergyConnectorTier> tier = connectorTier();
        if (tier.isEmpty()) {
            return 0;
        }
        return insertIntoTarget(Math.min(amount, tier.get().transferRate()), simulate);
    }

    public int transferRateForChannel(int networkChannel) {
        if (networkChannel != channel || !mode.sendsNetworkOutput()) {
            return 0;
        }
        return transferRate();
    }

    void addNetworkDebugStats(CableBlockEntity.NetworkDebugAccumulator accumulator) {
        if (connectorTier().isPresent()) {
            accumulator.addEnergyModule(channel);
        }
        for (int moduleIndex = 0; moduleIndex < FLUID_MODULE_SLOT_COUNT; moduleIndex++) {
            FluidModuleState module = fluidModules[moduleIndex];
            Optional<FluidConnectorTier> tier = fluidConnectorTier(moduleIndex);
            tier.ifPresent(connector -> accumulator.addFluidModule(
                    module.channel,
                    connector.fluidPerShipment(),
                    module.attachAs != null
            ));
        }
        for (int moduleIndex = 0; moduleIndex < ITEM_MODULE_SLOT_COUNT; moduleIndex++) {
            ItemModuleState module = itemModules[moduleIndex];
            Optional<ItemConnectorTier> tier = itemConnectorTier(moduleIndex);
            tier.ifPresent(connector -> accumulator.addItemModule(
                    module.channel,
                    connector.itemsPerShipment(),
                    module.attachAs != null
            ));
        }
    }

    public ItemStack receiveItemFromCableNetwork(Direction cableSide, int networkChannel, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!isNetworkSide(cableSide)) {
            return stack.copy();
        }

        ItemStack remaining = stack.copy();
        for (int moduleIndex = 0; moduleIndex < ITEM_MODULE_SLOT_COUNT && !remaining.isEmpty(); moduleIndex++) {
            ItemModuleState module = itemModules[moduleIndex];
            if (module.channel != networkChannel
                    || module.attachAs == null
                    || !module.mode.insertsIntoTarget()) {
                continue;
            }

            Optional<ItemConnectorTier> tier = itemConnectorTier(moduleIndex);
            if (tier.isEmpty()) {
                continue;
            }

            remaining = insertIntoTargetForModule(moduleIndex, tier.get(), remaining, simulate);
        }
        return remaining;
    }

    public FluidStack receiveFluidFromCableNetwork(Direction cableSide, int networkChannel, FluidStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        if (!isNetworkSide(cableSide)) {
            return stack.copy();
        }

        FluidStack remaining = stack.copy();
        for (int moduleIndex = 0; moduleIndex < FLUID_MODULE_SLOT_COUNT && !remaining.isEmpty(); moduleIndex++) {
            FluidModuleState module = fluidModules[moduleIndex];
            if (module.channel != networkChannel
                    || module.attachAs == null
                    || !module.mode.insertsIntoTarget()) {
                continue;
            }

            Optional<FluidConnectorTier> tier = fluidConnectorTier(moduleIndex);
            if (tier.isEmpty()) {
                continue;
            }

            remaining = insertFluidIntoTargetForModule(moduleIndex, tier.get(), remaining, simulate);
        }
        return remaining;
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

    private void saveConnectorData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Channel", channel);
        tag.putString("AttachAs", attachAs.getSerializedName());
        tag.putString("Mode", mode.getSerializedName());
        tag.putString("Distribution", distributionMode.getSerializedName());
        CompoundTag fluidModulesTag = new CompoundTag();
        for (int index = 0; index < fluidModules.length; index++) {
            fluidModulesTag.put(Integer.toString(index), fluidModules[index].save());
        }
        tag.put("FluidModules", fluidModulesTag);
        CompoundTag itemModulesTag = new CompoundTag();
        for (int index = 0; index < itemModules.length; index++) {
            itemModulesTag.put(Integer.toString(index), itemModules[index].save());
        }
        tag.put("ItemModules", itemModulesTag);
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

        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            CompoundTag inventoryTag = tag.getCompound("Inventory");
            if (inventoryTag.getInt("Size") < SLOT_COUNT) {
                inventoryTag = inventoryTag.copy();
                inventoryTag.putInt("Size", SLOT_COUNT);
            }
            inventory.deserializeNBT(registries, inventoryTag);
        }
        channel = Math.max(0, Math.min(MAX_CHANNEL, tag.getInt("Channel")));
        Direction loadedAttachAs = Direction.byName(tag.getString("AttachAs"));
        attachAs = loadedAttachAs == null ? targetDirection().getOpposite() : loadedAttachAs;
        mode = CableConnectorMode.bySerializedName(tag.getString("Mode")).orElse(CableConnectorMode.BOTH);
        distributionMode = EnergyDistributionMode.bySerializedName(tag.getString("Distribution"))
                .orElse(EnergyDistributionMode.ROUND_ROBIN);
        CompoundTag fluidModulesTag = tag.getCompound("FluidModules");
        for (int index = 0; index < fluidModules.length; index++) {
            String key = Integer.toString(index);
            if (fluidModulesTag.contains(key, Tag.TAG_COMPOUND)) {
                fluidModules[index].load(fluidModulesTag.getCompound(key), targetDirection().getOpposite());
            } else {
                fluidModules[index].reset(targetDirection().getOpposite());
            }
        }
        CompoundTag itemModulesTag = tag.getCompound("ItemModules");
        for (int index = 0; index < itemModules.length; index++) {
            String key = Integer.toString(index);
            if (itemModulesTag.contains(key, Tag.TAG_COMPOUND)) {
                itemModules[index].load(itemModulesTag.getCompound(key), targetDirection().getOpposite());
            } else {
                itemModules[index].reset(targetDirection().getOpposite());
            }
        }
        invalidateNetworkCache();
    }

    private void tickFluidModules() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;
        for (int offset = 0; offset < FLUID_MODULE_SLOT_COUNT; offset++) {
            int moduleIndex = (fluidModuleTickCursor + offset) % FLUID_MODULE_SLOT_COUNT;
            FluidModuleState module = fluidModules[moduleIndex];
            if (module.jamTicks > 0) {
                module.jamTicks--;
                changed = true;
                continue;
            }
            if (module.cooldownTicks > 0) {
                module.cooldownTicks--;
                changed = true;
                continue;
            }

            Optional<FluidConnectorTier> tier = fluidConnectorTier(moduleIndex);
            if (tier.isEmpty() || !module.mode.pullsFromTarget()) {
                continue;
            }

            int moved = tryTransferFluidModule(moduleIndex, tier.get());
            if (module.jamTicks > 0) {
                changed = true;
                continue;
            }
            if (moved > 0) {
                module.cooldownTicks = tier.get().waitTicks();
                changed = true;
            }
        }
        fluidModuleTickCursor = (fluidModuleTickCursor + 1) % FLUID_MODULE_SLOT_COUNT;

        if (changed) {
            setChanged();
        }
    }

    private int tryTransferFluidModule(int moduleIndex, FluidConnectorTier tier) {
        FluidModuleState module = fluidModules[moduleIndex];
        if (module.attachAs == null) {
            return 0;
        }
        IFluidHandler source = targetFluidHandler(module.attachAs);
        if (source == null) {
            return 0;
        }

        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack stored = source.getFluidInTank(tank);
            if (stored.isEmpty()) {
                continue;
            }

            FluidStack request = stored.copyWithAmount(Math.min(stored.getAmount(), tier.fluidPerShipment()));
            FluidStack candidate = source.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (candidate.isEmpty()) {
                continue;
            }

            FluidStack simulatedRemainder = distributeFluidToConnectedCables(module.channel, candidate, true);
            int accepted = candidate.getAmount() - simulatedRemainder.getAmount();
            if (accepted <= 0) {
                continue;
            }
            if (rollFluidJam(tier)) {
                module.jamTicks = tier.jamTicks();
                return 0;
            }

            FluidStack extracted = source.drain(candidate.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
            if (extracted.isEmpty()) {
                continue;
            }

            FluidStack remainder = distributeFluidToConnectedCables(module.channel, extracted, false);
            int moved = extracted.getAmount() - remainder.getAmount();
            if (!remainder.isEmpty()) {
                source.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
            }
            return moved;
        }
        return 0;
    }

    private void tickItemModules() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;
        for (int offset = 0; offset < ITEM_MODULE_SLOT_COUNT; offset++) {
            int moduleIndex = (itemModuleTickCursor + offset) % ITEM_MODULE_SLOT_COUNT;
            ItemModuleState module = itemModules[moduleIndex];
            if (module.jamTicks > 0) {
                module.jamTicks--;
                changed = true;
                continue;
            }
            if (module.cooldownTicks > 0) {
                module.cooldownTicks--;
                changed = true;
                continue;
            }

            Optional<ItemConnectorTier> tier = itemConnectorTier(moduleIndex);
            if (tier.isEmpty() || !module.mode.pullsFromTarget()) {
                continue;
            }

            int moved = tryTransferItemModule(moduleIndex, tier.get());
            if (module.jamTicks > 0) {
                changed = true;
                continue;
            }
            if (moved > 0) {
                module.cooldownTicks = tier.get().waitTicks();
                changed = true;
            }
        }
        itemModuleTickCursor = (itemModuleTickCursor + 1) % ITEM_MODULE_SLOT_COUNT;

        if (changed) {
            setChanged();
        }
    }

    private int tryTransferItemModule(int moduleIndex, ItemConnectorTier tier) {
        ItemModuleState module = itemModules[moduleIndex];
        if (module.attachAs == null) {
            return 0;
        }
        IItemHandler source = targetItemHandler(module.attachAs);
        if (source == null) {
            return 0;
        }

        int sourceSlots = source.getSlots();
        if (sourceSlots <= 0) {
            return 0;
        }

        int startSlot = Math.floorMod(module.sourceSlotCursor, sourceSlots);
        for (int offset = 0; offset < sourceSlots; offset++) {
            int slot = (startSlot + offset) % sourceSlots;
            ItemStack candidate = source.extractItem(slot, tier.itemsPerShipment(), true);
            if (candidate.isEmpty()) {
                continue;
            }

            ItemStack simulatedRemainder = distributeItemToConnectedCables(module.channel, candidate, true);
            int accepted = candidate.getCount() - simulatedRemainder.getCount();
            if (accepted <= 0) {
                continue;
            }
            module.sourceSlotCursor = (slot + 1) % sourceSlots;
            if (rollItemJam(tier)) {
                module.jamTicks = tier.jamTicks();
                return 0;
            }

            ItemStack extracted = source.extractItem(slot, accepted, false);
            if (extracted.isEmpty()) {
                continue;
            }

            ItemStack remainder = distributeItemToConnectedCables(module.channel, extracted, false);
            int moved = extracted.getCount() - remainder.getCount();
            if (!remainder.isEmpty()) {
                ItemStack leftover = returnToSource(source, slot, remainder);
                if (!leftover.isEmpty() && level != null) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), leftover);
                }
            }
            return moved;
        }
        return 0;
    }

    private int receiveFromTarget(int amount, boolean simulate) {
        if (level == null || amount <= 0 || !mode.acceptsNetworkInput()) {
            return 0;
        }
        Optional<EnergyConnectorTier> tier = connectorTier();
        if (tier.isEmpty()) {
            return 0;
        }

        int transferable = Math.min(amount, remainingEnergyInputBudget(tier.get()));
        if (transferable <= 0) {
            return 0;
        }

        int moved = 0;
        for (Direction direction : DIRECTIONS) {
            if (direction == targetDirection()) {
                continue;
            }
            BlockPos networkPos = worldPosition.relative(direction);
            BlockState networkState = level.getBlockState(networkPos);
            if (!isConnectedNetworkNode(networkState, direction.getOpposite())) {
                continue;
            }
            moved += CableBlockEntity.distributeEnergy(
                    level,
                    networkPos,
                    worldPosition,
                    direction,
                    channel,
                    transferable - moved,
                    simulate,
                    distributionMode
            );
            if (moved >= transferable) {
                break;
            }
        }
        if (!simulate) {
            recordEnergyInputTransfer(moved);
        }
        return moved;
    }

    private boolean pullEnergyFromTarget() {
        if (level == null || level.isClientSide || !mode.acceptsNetworkInput()) {
            return false;
        }

        Optional<EnergyConnectorTier> tier = connectorTier();
        if (tier.isEmpty()) {
            return false;
        }

        int request = remainingEnergyInputBudget(tier.get());
        if (request <= 0) {
            return false;
        }

        BlockPos targetPos = worldPosition.relative(targetDirection());
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof UniversalConnectorBlock || targetState.getBlock() instanceof CableBlock) {
            return false;
        }

        IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, attachAs);
        if (source == null || !source.canExtract()) {
            return false;
        }

        int extractable = source.extractEnergy(request, true);
        if (extractable <= 0) {
            return false;
        }

        int accepted = distributeEnergyToConnectedCables(extractable, true);
        if (accepted <= 0) {
            return false;
        }

        int extracted = source.extractEnergy(accepted, false);
        if (extracted <= 0) {
            return false;
        }

        int moved = distributeEnergyToConnectedCables(extracted, false);
        recordEnergyInputTransfer(moved);
        return moved > 0;
    }

    private int distributeEnergyToConnectedCables(int amount, boolean simulate) {
        if (level == null || amount <= 0) {
            return 0;
        }

        int moved = 0;
        for (Direction direction : DIRECTIONS) {
            if (direction == targetDirection()) {
                continue;
            }
            BlockPos networkPos = worldPosition.relative(direction);
            BlockState networkState = level.getBlockState(networkPos);
            if (!isConnectedNetworkNode(networkState, direction.getOpposite())) {
                continue;
            }
            moved += CableBlockEntity.distributeEnergy(
                    level,
                    networkPos,
                    worldPosition,
                    direction,
                    channel,
                    amount - moved,
                    simulate,
                    distributionMode
            );
            if (moved >= amount) {
                break;
            }
        }
        return moved;
    }

    private int remainingEnergyInputBudget(EnergyConnectorTier tier) {
        if (level == null) {
            return 0;
        }

        long gameTime = level.getGameTime();
        if (energyInputBudgetTick != gameTime) {
            energyInputBudgetTick = gameTime;
            energyInputTransferredThisTick = 0;
        }
        return Math.max(0, tier.transferRate() - energyInputTransferredThisTick);
    }

    private void recordEnergyInputTransfer(int amount) {
        if (level == null || amount <= 0) {
            return;
        }

        long gameTime = level.getGameTime();
        if (energyInputBudgetTick != gameTime) {
            energyInputBudgetTick = gameTime;
            energyInputTransferredThisTick = 0;
        }
        energyInputTransferredThisTick += amount;
    }

    private int insertIntoTarget(int amount, boolean simulate) {
        if (level == null || amount <= 0) {
            return 0;
        }
        BlockPos targetPos = worldPosition.relative(targetDirection());
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof UniversalConnectorBlock || targetState.getBlock() instanceof CableBlock) {
            return 0;
        }
        IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, attachAs);
        if (target == null || !target.canReceive()) {
            return 0;
        }
        return target.receiveEnergy(Math.min(amount, transferRate()), simulate);
    }

    private ItemStack distributeItemToConnectedCables(int itemChannel, ItemStack stack, boolean simulate) {
        if (level == null || stack.isEmpty()) {
            return stack;
        }

        ItemStack remaining = stack.copy();
        for (Direction direction : DIRECTIONS) {
            if (direction == targetDirection()) {
                continue;
            }
            BlockPos networkPos = worldPosition.relative(direction);
            BlockState networkState = level.getBlockState(networkPos);
            if (!isConnectedNetworkNode(networkState, direction.getOpposite())) {
                continue;
            }
            remaining = CableBlockEntity.distributeItems(level, networkPos, worldPosition, direction, itemChannel, remaining, simulate);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    private FluidStack distributeFluidToConnectedCables(int fluidChannel, FluidStack stack, boolean simulate) {
        if (level == null || stack.isEmpty()) {
            return stack;
        }

        FluidStack remaining = stack.copy();
        for (Direction direction : DIRECTIONS) {
            if (direction == targetDirection()) {
                continue;
            }
            BlockPos networkPos = worldPosition.relative(direction);
            BlockState networkState = level.getBlockState(networkPos);
            if (!isConnectedNetworkNode(networkState, direction.getOpposite())) {
                continue;
            }
            remaining = CableBlockEntity.distributeFluids(level, networkPos, worldPosition, direction, fluidChannel, remaining, simulate);
            if (remaining.isEmpty()) {
                return FluidStack.EMPTY;
            }
        }
        return remaining;
    }

    private IFluidHandler targetFluidHandler(Direction targetSide) {
        if (level == null || targetSide == null) {
            return null;
        }
        BlockPos targetPos = worldPosition.relative(targetDirection());
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof UniversalConnectorBlock || targetState.getBlock() instanceof CableBlock) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, targetSide);
    }

    private IItemHandler targetItemHandler(Direction targetSide) {
        if (level == null || targetSide == null) {
            return null;
        }
        BlockPos targetPos = worldPosition.relative(targetDirection());
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof UniversalConnectorBlock || targetState.getBlock() instanceof CableBlock) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, targetSide);
    }

    private FluidStack insertFluidIntoTargetForModule(
            int moduleIndex,
            FluidConnectorTier tier,
            FluidStack stack,
            boolean simulate
    ) {
        FluidStack remaining = stack.copy();
        int shipmentSize = Math.min(remaining.getAmount(), tier.fluidPerShipment());
        FluidStack shipment = remaining.copyWithAmount(shipmentSize);
        int inserted = insertFluidIntoTarget(shipment, fluidModules[moduleIndex].attachAs, simulate);
        if (inserted <= 0) {
            return remaining;
        }
        remaining.shrink(inserted);
        return remaining.isEmpty() ? FluidStack.EMPTY : remaining;
    }

    private int insertFluidIntoTarget(FluidStack stack, Direction targetSide, boolean simulate) {
        IFluidHandler target = targetFluidHandler(targetSide);
        if (target == null || stack.isEmpty()) {
            return 0;
        }
        return target.fill(stack.copy(), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

    private ItemStack insertIntoTargetForModule(
            int moduleIndex,
            ItemConnectorTier tier,
            ItemStack stack,
            boolean simulate
    ) {
        ItemStack remaining = stack.copy();
        int shipmentSize = Math.min(remaining.getCount(), tier.itemsPerShipment());
        ItemStack shipment = remaining.copyWithCount(shipmentSize);
        ItemStack shipmentRemainder = insertIntoTarget(shipment, itemModules[moduleIndex].attachAs, simulate);
        int inserted = shipmentSize - shipmentRemainder.getCount();
        if (inserted <= 0) {
            return remaining;
        }
        remaining.shrink(inserted);
        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    private ItemStack insertIntoTarget(ItemStack stack, Direction targetSide, boolean simulate) {
        IItemHandler target = targetItemHandler(targetSide);
        if (target == null) {
            return stack;
        }

        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < target.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = target.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    private ItemStack returnToSource(IItemHandler source, int originalSlot, ItemStack stack) {
        ItemStack remaining = source.insertItem(originalSlot, stack, false);
        for (int slot = 0; slot < source.getSlots() && !remaining.isEmpty(); slot++) {
            if (slot != originalSlot) {
                remaining = source.insertItem(slot, remaining, false);
            }
        }
        return remaining;
    }

    private boolean rollItemJam(ItemConnectorTier tier) {
        return level != null
                && tier.jamChancePerThousand() > 0
                && level.random.nextInt(1000) < tier.jamChancePerThousand();
    }

    private boolean rollFluidJam(FluidConnectorTier tier) {
        return level != null
                && tier.jamChancePerThousand() > 0
                && level.random.nextInt(1000) < tier.jamChancePerThousand();
    }

    private Optional<EnergyConnectorTier> connectorTier() {
        ItemStack stack = inventory.getStackInSlot(SLOT_ENERGY_CONNECTOR);
        if (stack.getItem() instanceof EnergyConnectorItem connector && connector.tier().enabled()) {
            return Optional.of(connector.tier());
        }
        return Optional.empty();
    }

    private int transferRate() {
        return connectorTier().map(EnergyConnectorTier::transferRate).orElse(0);
    }

    private Optional<FluidConnectorTier> fluidConnectorTier(int moduleIndex) {
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null) {
            return Optional.empty();
        }
        ItemStack stack = inventory.getStackInSlot(fluidConnectorSlot(moduleIndex));
        if (stack.getItem() instanceof FluidConnectorItem connector && connector.tier().enabled()) {
            return Optional.of(connector.tier());
        }
        return Optional.empty();
    }

    private Optional<ItemConnectorTier> itemConnectorTier(int moduleIndex) {
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null) {
            return Optional.empty();
        }
        ItemStack stack = inventory.getStackInSlot(itemConnectorSlot(moduleIndex));
        if (stack.getItem() instanceof ItemConnectorItem connector && connector.tier().enabled()) {
            return Optional.of(connector.tier());
        }
        return Optional.empty();
    }

    private int connectorData(int index) {
        if (isNetworkDebugDataIndex(index)) {
            return networkDebugData(index, networkDebugSnapshot());
        }
        if (isNetworkEnergyTelemetryDataIndex(index)) {
            return networkEnergyTelemetryData(index, networkEnergyTelemetrySnapshot());
        }
        return moduleData(index);
    }

    private CableBlockEntity.NetworkDebugSnapshot networkDebugSnapshot() {
        return level == null ? CableBlockEntity.NetworkDebugSnapshot.EMPTY : CableBlockEntity.networkDebugSnapshot(level, worldPosition);
    }

    private CableBlockEntity.NetworkEnergyTelemetrySnapshot networkEnergyTelemetrySnapshot() {
        return level == null
                ? CableBlockEntity.NetworkEnergyTelemetrySnapshot.EMPTY
                : CableBlockEntity.networkEnergyTelemetrySnapshot(level, worldPosition, channel);
    }

    private int moduleData(int index) {
        if (index >= DATA_FLUID_MODULE_BASE && index < DATA_ITEM_MODULE_BASE) {
            return fluidModuleData(index);
        }
        return itemModuleData(index);
    }

    private int fluidModuleData(int index) {
        int relative = index - DATA_FLUID_MODULE_BASE;
        if (relative < 0) {
            return 0;
        }
        int moduleIndex = relative / DATA_MODULE_STRIDE;
        int offset = relative % DATA_MODULE_STRIDE;
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null) {
            return 0;
        }
        return switch (offset) {
            case DATA_MODULE_CHANNEL_OFFSET -> module.channel;
            case DATA_MODULE_ATTACH_AS_OFFSET -> module.attachAs == null ? -1 : module.attachAs.ordinal();
            case DATA_MODULE_MODE_OFFSET -> module.mode.ordinal();
            case DATA_MODULE_COOLDOWN_OFFSET -> module.cooldownTicks;
            case DATA_MODULE_JAM_TICKS_OFFSET -> module.jamTicks;
            default -> 0;
        };
    }

    private int itemModuleData(int index) {
        int relative = index - DATA_ITEM_MODULE_BASE;
        if (relative < 0) {
            return 0;
        }
        int moduleIndex = relative / DATA_MODULE_STRIDE;
        int offset = relative % DATA_MODULE_STRIDE;
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null) {
            return 0;
        }
        return switch (offset) {
            case DATA_MODULE_CHANNEL_OFFSET -> module.channel;
            case DATA_MODULE_ATTACH_AS_OFFSET -> module.attachAs == null ? -1 : module.attachAs.ordinal();
            case DATA_MODULE_MODE_OFFSET -> module.mode.ordinal();
            case DATA_MODULE_COOLDOWN_OFFSET -> module.cooldownTicks;
            case DATA_MODULE_JAM_TICKS_OFFSET -> module.jamTicks;
            default -> 0;
        };
    }

    private FluidModuleState fluidModule(int moduleIndex) {
        return moduleIndex >= 0 && moduleIndex < fluidModules.length ? fluidModules[moduleIndex] : null;
    }

    private ItemModuleState itemModule(int moduleIndex) {
        return moduleIndex >= 0 && moduleIndex < itemModules.length ? itemModules[moduleIndex] : null;
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

    public static int dataChannelIndex() {
        return DATA_CHANNEL;
    }

    public static int dataAttachAsIndex() {
        return DATA_ATTACH_AS;
    }

    public static int dataModeIndex() {
        return DATA_MODE;
    }

    public static int dataDistributionIndex() {
        return DATA_DISTRIBUTION;
    }

    public static int dataTransferRateIndex() {
        return DATA_TRANSFER_RATE;
    }

    public static int dataHasConnectorIndex() {
        return DATA_HAS_CONNECTOR;
    }

    public static int dataLastEnergyInputIndex() {
        return DATA_LAST_ENERGY_INPUT;
    }

    public static int dataLastEnergyOutputIndex() {
        return DATA_LAST_ENERGY_OUTPUT;
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

    public static int dataEnergyTargetAccessIndex() {
        return DATA_ENERGY_TARGET_ACCESS;
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

    private static final class FluidModuleState {
        private int channel;
        private Direction attachAs;
        private FluidConnectorMode mode;
        private int cooldownTicks;
        private int jamTicks;

        private FluidModuleState(
                int channel,
                Direction attachAs,
                FluidConnectorMode mode,
                int cooldownTicks,
                int jamTicks
        ) {
            this.channel = Math.max(0, Math.min(MAX_CHANNEL, channel));
            this.attachAs = attachAs;
            this.mode = mode;
            this.cooldownTicks = Math.max(0, cooldownTicks);
            this.jamTicks = Math.max(0, jamTicks);
        }

        private static FluidModuleState defaults(Direction attachAs) {
            return new FluidModuleState(0, attachAs, FluidConnectorMode.INPUT, 0, 0);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Channel", channel);
            tag.putString("AttachAs", attachAs == null ? "none" : attachAs.getSerializedName());
            tag.putString("Mode", mode.getSerializedName());
            tag.putInt("Cooldown", cooldownTicks);
            tag.putInt("JamTicks", jamTicks);
            return tag;
        }

        private void load(CompoundTag tag, Direction defaultAttachAs) {
            channel = Math.max(0, Math.min(MAX_CHANNEL, tag.getInt("Channel")));
            String attachAsName = tag.getString("AttachAs");
            Direction loadedAttachAs = Direction.byName(attachAsName);
            attachAs = "none".equals(attachAsName) ? null : loadedAttachAs == null ? defaultAttachAs : loadedAttachAs;
            mode = FluidConnectorMode.bySerializedName(tag.getString("Mode")).orElse(FluidConnectorMode.INPUT);
            cooldownTicks = Math.max(0, tag.getInt("Cooldown"));
            jamTicks = Math.max(0, tag.getInt("JamTicks"));
        }

        private void reset(Direction defaultAttachAs) {
            channel = 0;
            attachAs = defaultAttachAs;
            mode = FluidConnectorMode.INPUT;
            cooldownTicks = 0;
            jamTicks = 0;
        }
    }

    private static final class ItemModuleState {
        private int channel;
        private Direction attachAs;
        private ItemConnectorMode mode;
        private int cooldownTicks;
        private int jamTicks;
        private int sourceSlotCursor;

        private ItemModuleState(
                int channel,
                Direction attachAs,
                ItemConnectorMode mode,
                int cooldownTicks,
                int jamTicks
        ) {
            this.channel = Math.max(0, Math.min(MAX_CHANNEL, channel));
            this.attachAs = attachAs;
            this.mode = mode;
            this.cooldownTicks = Math.max(0, cooldownTicks);
            this.jamTicks = Math.max(0, jamTicks);
        }

        private static ItemModuleState defaults(Direction attachAs) {
            return new ItemModuleState(0, attachAs, ItemConnectorMode.INPUT, 0, 0);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Channel", channel);
            tag.putString("AttachAs", attachAs == null ? "none" : attachAs.getSerializedName());
            tag.putString("Mode", mode.getSerializedName());
            tag.putInt("Cooldown", cooldownTicks);
            tag.putInt("JamTicks", jamTicks);
            return tag;
        }

        private void load(CompoundTag tag, Direction defaultAttachAs) {
            channel = Math.max(0, Math.min(MAX_CHANNEL, tag.getInt("Channel")));
            String attachAsName = tag.getString("AttachAs");
            Direction loadedAttachAs = Direction.byName(attachAsName);
            attachAs = "none".equals(attachAsName) ? null : loadedAttachAs == null ? defaultAttachAs : loadedAttachAs;
            mode = ItemConnectorMode.bySerializedName(tag.getString("Mode")).orElse(ItemConnectorMode.INPUT);
            cooldownTicks = Math.max(0, tag.getInt("Cooldown"));
            jamTicks = Math.max(0, tag.getInt("JamTicks"));
            sourceSlotCursor = 0;
        }

        private void reset(Direction defaultAttachAs) {
            channel = 0;
            attachAs = defaultAttachAs;
            mode = ItemConnectorMode.INPUT;
            cooldownTicks = 0;
            jamTicks = 0;
            sourceSlotCursor = 0;
        }
    }

    private final class ConnectorEnergyStorage implements IEnergyStorage {
        private final Direction side;

        private ConnectorEnergyStorage(Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return side == targetDirection()
                    ? receiveFromTarget(toReceive, simulate)
                    : receiveFromCableNetwork(side, channel, toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return transferRate();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            if (connectorTier().isEmpty()) {
                return false;
            }
            return side == targetDirection() ? mode.acceptsNetworkInput() : mode.sendsNetworkOutput();
        }
    }

    private boolean isNetworkSide(Direction side) {
        return side != null && side != targetDirection();
    }

    private static boolean isConnectedNetworkNode(BlockState state, Direction sideToConnector) {
        return false;
    }

    private static Direction nextModuleAttachAs(Direction current) {
        if (current == null) {
            return Direction.DOWN;
        }
        int next = current.ordinal() + 1;
        return next >= DIRECTIONS.length ? null : DIRECTIONS[next];
    }
}
