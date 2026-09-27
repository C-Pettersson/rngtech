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
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.item.NetworkConnectorItem;
import com.rngtech.content.itemfilter.ItemFilterMatcher;
import com.rngtech.content.menu.UniversalConnectorAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CableUniversalConnectorData implements UniversalConnectorAccess {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final UniversalConnectorDataOwner owner;
    private final Direction face;
    private final ItemStackHandler inventory = new ItemStackHandler(UniversalConnectorBlockEntity.SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR) {
                return stack.getItem() instanceof EnergyConnectorItem connector && connector.tier().enabled();
            }
            if (UniversalConnectorBlockEntity.isFluidConnectorSlot(slot)) {
                return stack.getItem() instanceof FluidConnectorItem connector && connector.tier().enabled();
            }
            if (UniversalConnectorBlockEntity.isItemConnectorSlot(slot)) {
                return stack.getItem() instanceof ItemConnectorItem connector && connector.tier().enabled();
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

        @Override
        protected void onContentsChanged(int slot) {
            invalidateConnectorState();
        }
    };
    private final ItemStackHandler filterInventory = new ItemStackHandler(UniversalConnectorBlockEntity.FILTER_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (UniversalConnectorBlockEntity.isFluidFilterSlot(slot)) {
                return isFluidFilterStack(stack);
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

        @Override
        protected void onContentsChanged(int slot) {
            invalidateConnectorState();
        }
    };
    private final IEnergyStorage energyStorage = new ConnectorEnergyStorage();
    private final IItemHandler itemHandler = new NetworkItemHandler();
    private final IFluidHandler fluidHandler = new NetworkFluidHandler();
    private final FluidModuleState[] fluidModules =
            new FluidModuleState[UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT];
    private final ItemModuleState[] itemModules =
            new ItemModuleState[UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT];
    private int channel;
    private int bridgeChannel;
    private Direction attachAs;
    private CableConnectorMode mode = CableConnectorMode.BOTH;
    private EnergyDistributionMode distributionMode = EnergyDistributionMode.ROUND_ROBIN;
    private int fluidModuleTickCursor;
    private int itemModuleTickCursor;
    private long energyInputBudgetTick = Long.MIN_VALUE;
    private int energyInputTransferredThisTick;
    private long energyTelemetryTick = Long.MIN_VALUE;
    private int energyInputMovedThisTick;
    private int energyOutputMovedThisTick;
    private int lastEnergyInputMoved;
    private int lastEnergyOutputMoved;

    public CableUniversalConnectorData(UniversalConnectorDataOwner owner, Direction face) {
        this.owner = owner;
        this.face = face;
        attachAs = defaultAttachAs();
        for (int index = 0; index < fluidModules.length; index++) {
            fluidModules[index] = FluidModuleState.defaults(defaultAttachAs());
        }
        for (int index = 0; index < itemModules.length; index++) {
            itemModules[index] = ItemModuleState.defaults(defaultAttachAs());
        }
    }

    @Override
    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public ItemStackHandler getFilterInventory() {
        return filterInventory;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == face && connectorTier().isPresent() ? energyStorage : null;
    }

    public IItemHandler getItemHandler(Direction side) {
        return side == face && hasInputItemModule() ? itemHandler : null;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return side == face && hasInputFluidModule() ? fluidHandler : null;
    }

    @Override
    public Level getConnectorLevel() {
        return owner.getLevel();
    }

    @Override
    public BlockPos getConnectorPos() {
        return owner.getBlockPos();
    }

    @Override
    public boolean isMenuAvailable(Player player) {
        BlockPos pos = owner.getBlockPos();
        return owner.hasUniversalConnector(face)
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    public Direction face() {
        return face;
    }

    @Override
    public Direction defaultAttachAs() {
        return defaultAttachAsInternal();
    }

    public void serverTick() {
        beginEnergyTelemetryTick();
        pullEnergyFromTarget();
        tickFluidModules();
        tickItemModules();
    }

    public int receiveFromCableNetwork(Direction cableSide, int networkChannel, int amount, boolean simulate) {
        if (amount <= 0 || cableSide != face || networkChannel != channel || !mode.sendsNetworkOutput()) {
            return 0;
        }
        Optional<EnergyConnectorTier> tier = connectorTier();
        if (tier.isEmpty()) {
            return 0;
        }
        int inserted = insertIntoTarget(Math.min(amount, tier.get().transferRate()), simulate);
        if (!simulate) {
            recordEnergyOutputTransfer(inserted);
        }
        return inserted;
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
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT; moduleIndex++) {
            FluidModuleState module = fluidModules[moduleIndex];
            Optional<FluidConnectorTier> tier = fluidConnectorTier(moduleIndex);
            tier.ifPresent(connector -> accumulator.addFluidModule(
                    module.channel,
                    connector.fluidPerShipment(),
                    module.attachAs != null
            ));
        }
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT; moduleIndex++) {
            ItemModuleState module = itemModules[moduleIndex];
            Optional<ItemConnectorTier> tier = itemConnectorTier(moduleIndex);
            tier.ifPresent(connector -> accumulator.addItemModule(
                    module.channel,
                    connector.itemsPerShipment(),
                    module.attachAs != null
            ));
        }
        NetworkBridgeType bridgeType = bridgeType();
        if (bridgeType != NetworkBridgeType.NONE) {
            accumulator.addBridgeModule(bridgeType, bridgeChannel, hasActiveBridgeModule());
        }
    }

    public ItemStack receiveItemFromCableNetwork(Direction cableSide, int networkChannel, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (cableSide != face) {
            return stack.copy();
        }

        ItemStack remaining = stack.copy();
        for (int moduleIndex = 0; moduleIndex < itemModules.length && !remaining.isEmpty(); moduleIndex++) {
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

            if (!passesItemFilter(moduleIndex, remaining)) {
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
        if (cableSide != face) {
            return stack.copy();
        }

        FluidStack remaining = stack.copy();
        for (int moduleIndex = 0; moduleIndex < fluidModules.length && !remaining.isEmpty(); moduleIndex++) {
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

            if (!passesFluidFilter(moduleIndex, remaining)) {
                continue;
            }
            remaining = insertFluidIntoTargetForModule(moduleIndex, tier.get(), remaining, simulate);
        }
        return remaining;
    }

    public void dropInventory(Level level) {
        BlockPos pos = owner.getBlockPos();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("FilterInventory", filterInventory.serializeNBT(registries));
        tag.putInt("Channel", channel);
        tag.putInt("BridgeChannel", bridgeChannel);
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
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            CompoundTag inventoryTag = tag.getCompound("Inventory");
            if (!tag.contains("FilterInventory", Tag.TAG_COMPOUND)) {
                migrateLegacyFilterInventory(inventoryTag, registries);
            }
            inventory.deserializeNBT(registries, inventoryTagWithSize(
                    inventoryTag,
                    UniversalConnectorBlockEntity.SLOT_COUNT
            ));
        }
        if (tag.contains("FilterInventory", Tag.TAG_COMPOUND)) {
            filterInventory.deserializeNBT(registries, inventoryTagWithSize(
                    tag.getCompound("FilterInventory"),
                    UniversalConnectorBlockEntity.FILTER_SLOT_COUNT
            ));
        }
        channel = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, tag.getInt("Channel")));
        bridgeChannel = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, tag.getInt("BridgeChannel")));
        Direction loadedAttachAs = Direction.byName(tag.getString("AttachAs"));
        attachAs = loadedAttachAs == null ? defaultAttachAs() : loadedAttachAs;
        mode = CableConnectorMode.bySerializedName(tag.getString("Mode")).orElse(CableConnectorMode.BOTH);
        distributionMode = EnergyDistributionMode.bySerializedName(tag.getString("Distribution"))
                .orElse(EnergyDistributionMode.ROUND_ROBIN);
        CompoundTag fluidModulesTag = tag.getCompound("FluidModules");
        for (int index = 0; index < fluidModules.length; index++) {
            String key = Integer.toString(index);
            if (fluidModulesTag.contains(key, Tag.TAG_COMPOUND)) {
                fluidModules[index].load(fluidModulesTag.getCompound(key), defaultAttachAs());
            } else {
                fluidModules[index].reset(defaultAttachAs());
            }
        }
        CompoundTag itemModulesTag = tag.getCompound("ItemModules");
        for (int index = 0; index < itemModules.length; index++) {
            String key = Integer.toString(index);
            if (itemModulesTag.contains(key, Tag.TAG_COMPOUND)) {
                itemModules[index].load(itemModulesTag.getCompound(key), defaultAttachAs());
            } else {
                itemModules[index].reset(defaultAttachAs());
            }
        }
    }

    private void migrateLegacyFilterInventory(CompoundTag inventoryTag, HolderLookup.Provider registries) {
        int savedSize = inventoryTag.getInt("Size");
        if (savedSize <= UniversalConnectorBlockEntity.SLOT_COUNT) {
            return;
        }
        ItemStackHandler legacyInventory = new ItemStackHandler(savedSize);
        legacyInventory.deserializeNBT(registries, inventoryTag);
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT; moduleIndex++) {
            for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
                int legacySlot = UniversalConnectorBlockEntity.SLOT_COUNT
                        + moduleIndex * UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE
                        + filterIndex;
                if (legacySlot >= legacyInventory.getSlots()) {
                    continue;
                }
                setFluidFilter(moduleIndex, filterIndex, legacyInventory.getStackInSlot(legacySlot));
            }
        }
        int legacyItemStart = UniversalConnectorBlockEntity.SLOT_COUNT
                + UniversalConnectorBlockEntity.FLUID_FILTER_SLOT_COUNT;
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT; moduleIndex++) {
            for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
                int legacySlot = legacyItemStart
                        + moduleIndex * UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE
                        + filterIndex;
                if (legacySlot >= legacyInventory.getSlots()) {
                    continue;
                }
                setItemFilter(moduleIndex, filterIndex, legacyInventory.getStackInSlot(legacySlot));
            }
        }
    }

    private static CompoundTag inventoryTagWithSize(CompoundTag source, int size) {
        CompoundTag resized = source.copy();
        resized.putInt("Size", size);
        ListTag sourceItems = source.getList("Items", Tag.TAG_COMPOUND);
        ListTag resizedItems = new ListTag();
        for (int index = 0; index < sourceItems.size(); index++) {
            CompoundTag itemTag = sourceItems.getCompound(index);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < size) {
                resizedItems.add(itemTag.copy());
            }
        }
        resized.put("Items", resizedItems);
        return resized;
    }

    @Override
    public ContainerData menuData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> channel;
                    case 1 -> attachAs.ordinal();
                    case 2 -> mode.ordinal();
                    case 3 -> transferRate();
                    case 4 -> distributionMode.ordinal();
                    case 5 -> connectorTier().isPresent() ? 1 : 0;
                    case 6 -> lastEnergyInput();
                    case 7 -> lastEnergyOutput();
                    case 8 -> bridgeChannel;
                    case 9 -> bridgeType().dataId();
                    case 10 -> bridgeType() != NetworkBridgeType.NONE && bridgeType().isLoaded() ? 1 : 0;
                    case 11 -> sameChannelBridgeEndpointCount();
                    default -> connectorData(index);
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return UniversalConnectorBlockEntity.dataCount();
            }
        };
    }

    @Override
    public boolean incrementChannel(int amount) {
        int next = Math.floorMod(channel + amount, UniversalConnectorBlockEntity.MAX_CHANNEL + 1);
        if (channel == next) {
            return false;
        }
        channel = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setChannel(int channel) {
        int next = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, channel));
        if (this.channel == next) {
            return false;
        }
        this.channel = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean incrementBridgeChannel(int amount) {
        int next = Math.floorMod(bridgeChannel + amount, UniversalConnectorBlockEntity.MAX_CHANNEL + 1);
        if (bridgeChannel == next) {
            return false;
        }
        bridgeChannel = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setBridgeChannel(int channel) {
        int next = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, channel));
        if (bridgeChannel == next) {
            return false;
        }
        bridgeChannel = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setAttachAs(Direction attachAs) {
        if (attachAs == null || this.attachAs == attachAs) {
            return false;
        }
        this.attachAs = attachAs;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean cycleMode() {
        CableConnectorMode next = mode.next();
        if (mode == next) {
            return false;
        }
        mode = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setMode(CableConnectorMode mode) {
        if (mode == null || this.mode == mode) {
            return false;
        }
        this.mode = mode;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean cycleEnergyDistributionMode() {
        EnergyDistributionMode next = distributionMode.next();
        if (distributionMode == next) {
            return false;
        }
        distributionMode = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setEnergyDistributionMode(EnergyDistributionMode distributionMode) {
        if (distributionMode == null || this.distributionMode == distributionMode) {
            return false;
        }
        this.distributionMode = distributionMode;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean clearNetworkCache() {
        owner.clearConnectorNetworkCache();
        return true;
    }

    @Override
    public boolean incrementFluidChannel(int moduleIndex, int amount) {
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null) {
            return false;
        }
        int next = Math.floorMod(module.channel + amount, UniversalConnectorBlockEntity.MAX_CHANNEL + 1);
        if (module.channel == next) {
            return false;
        }
        module.channel = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setFluidChannel(int moduleIndex, int channel) {
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null) {
            return false;
        }
        int next = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, channel));
        if (module.channel == next) {
            return false;
        }
        module.channel = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean cycleFluidAttachAs(int moduleIndex) {
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null) {
            return false;
        }
        module.attachAs = nextModuleAttachAs(module.attachAs);
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setFluidAttachAs(int moduleIndex, Direction attachAs) {
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null || module.attachAs == attachAs) {
            return false;
        }
        module.attachAs = attachAs;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean cycleFluidMode(int moduleIndex) {
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null) {
            return false;
        }
        module.mode = module.mode.next();
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setFluidMode(int moduleIndex, FluidConnectorMode mode) {
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null || mode == null || module.mode == mode) {
            return false;
        }
        module.mode = mode;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setFluidFilter(int moduleIndex, int filterIndex, ItemStack stack) {
        if (fluidModule(moduleIndex) == null || !isValidFilterIndex(filterIndex)) {
            return false;
        }
        return setFilterStack(
                UniversalConnectorBlockEntity.fluidFilterSlot(moduleIndex, filterIndex),
                normalizeFluidFilterStack(stack)
        );
    }

    @Override
    public boolean incrementItemChannel(int moduleIndex, int amount) {
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null) {
            return false;
        }
        int next = Math.floorMod(module.channel + amount, UniversalConnectorBlockEntity.MAX_CHANNEL + 1);
        if (module.channel == next) {
            return false;
        }
        module.channel = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setItemChannel(int moduleIndex, int channel) {
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null) {
            return false;
        }
        int next = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, channel));
        if (module.channel == next) {
            return false;
        }
        module.channel = next;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean cycleItemAttachAs(int moduleIndex) {
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null) {
            return false;
        }
        module.attachAs = nextModuleAttachAs(module.attachAs);
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setItemAttachAs(int moduleIndex, Direction attachAs) {
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null || module.attachAs == attachAs) {
            return false;
        }
        module.attachAs = attachAs;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean cycleItemMode(int moduleIndex) {
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null) {
            return false;
        }
        module.mode = module.mode.next();
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setItemMode(int moduleIndex, ItemConnectorMode mode) {
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null || mode == null || module.mode == mode) {
            return false;
        }
        module.mode = mode;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean setItemFilter(int moduleIndex, int filterIndex, ItemStack stack) {
        if (itemModule(moduleIndex) == null || !isValidFilterIndex(filterIndex)) {
            return false;
        }
        return setFilterStack(
                UniversalConnectorBlockEntity.itemFilterSlot(moduleIndex, filterIndex),
                normalizeItemFilterStack(stack)
        );
    }

    private void invalidateConnectorState() {
        owner.universalConnectorStateChanged();
    }

    private Direction defaultAttachAsInternal() {
        return face.getOpposite();
    }

    private void tickFluidModules() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;
        for (int offset = 0; offset < fluidModules.length; offset++) {
            int moduleIndex = (fluidModuleTickCursor + offset) % fluidModules.length;
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
        fluidModuleTickCursor = (fluidModuleTickCursor + 1) % fluidModules.length;

        if (changed) {
            owner.setChanged();
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
            if (!passesFluidFilter(moduleIndex, candidate)) {
                continue;
            }

            FluidStack simulatedRemainder = distributeFluidToNetwork(module.channel, candidate, true);
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

            FluidStack remainder = distributeFluidToNetwork(module.channel, extracted, false);
            int moved = extracted.getAmount() - remainder.getAmount();
            if (!remainder.isEmpty()) {
                source.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
            }
            return moved;
        }
        return 0;
    }

    private void tickItemModules() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean changed = false;
        for (int offset = 0; offset < itemModules.length; offset++) {
            int moduleIndex = (itemModuleTickCursor + offset) % itemModules.length;
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
        itemModuleTickCursor = (itemModuleTickCursor + 1) % itemModules.length;

        if (changed) {
            owner.setChanged();
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
            if (!passesItemFilter(moduleIndex, candidate)) {
                continue;
            }

            ItemStack simulatedRemainder = distributeItemToNetwork(module.channel, candidate, true);
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

            ItemStack remainder = distributeItemToNetwork(module.channel, extracted, false);
            int moved = extracted.getCount() - remainder.getCount();
            if (!remainder.isEmpty()) {
                ItemStack leftover = returnToSource(source, slot, remainder);
                Level level = owner.getLevel();
                if (!leftover.isEmpty() && level != null) {
                    BlockPos pos = owner.getBlockPos();
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), leftover);
                }
            }
            return moved;
        }
        return 0;
    }

    private int receiveFromTarget(int amount, boolean simulate) {
        Level level = owner.getLevel();
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

        BlockPos ownerPos = owner.getBlockPos();
        int moved = CableBlockEntity.distributeEnergy(
                level,
                ownerPos,
                CableBlockEntity.TransferOrigin.endpoint(ownerPos, face, ownerPos.relative(face)),
                channel,
                transferable,
                simulate,
                distributionMode
        );
        if (!simulate) {
            recordEnergyInputTransfer(moved);
        }
        return moved;
    }

    private boolean pullEnergyFromTarget() {
        Level level = owner.getLevel();
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

        BlockPos targetPos = owner.getBlockPos().relative(face);
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

        BlockPos ownerPos = owner.getBlockPos();
        CableBlockEntity.TransferOrigin sourceOrigin =
                CableBlockEntity.TransferOrigin.endpoint(ownerPos, face, ownerPos.relative(face));
        int accepted = CableBlockEntity.distributeEnergy(
                level,
                ownerPos,
                sourceOrigin,
                channel,
                extractable,
                true,
                distributionMode
        );
        if (accepted <= 0) {
            return false;
        }

        int extracted = source.extractEnergy(accepted, false);
        if (extracted <= 0) {
            return false;
        }

        int moved = CableBlockEntity.distributeEnergy(
                level,
                ownerPos,
                sourceOrigin,
                channel,
                extracted,
                false,
                distributionMode
        );
        recordEnergyInputTransfer(moved);
        return moved > 0;
    }

    private int remainingEnergyInputBudget(EnergyConnectorTier tier) {
        Level level = owner.getLevel();
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
        Level level = owner.getLevel();
        if (level == null || amount <= 0) {
            return;
        }

        beginEnergyTelemetryTick();
        long gameTime = level.getGameTime();
        if (energyInputBudgetTick != gameTime) {
            energyInputBudgetTick = gameTime;
            energyInputTransferredThisTick = 0;
        }
        energyInputTransferredThisTick += amount;
        energyInputMovedThisTick += amount;
    }

    private void recordEnergyOutputTransfer(int amount) {
        Level level = owner.getLevel();
        if (level == null || amount <= 0) {
            return;
        }

        beginEnergyTelemetryTick();
        energyOutputMovedThisTick += amount;
    }

    private int lastEnergyInput() {
        beginEnergyTelemetryTick();
        return lastEnergyInputMoved;
    }

    private int lastEnergyOutput() {
        beginEnergyTelemetryTick();
        return lastEnergyOutputMoved;
    }

    private void beginEnergyTelemetryTick() {
        Level level = owner.getLevel();
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        if (energyTelemetryTick == gameTime) {
            return;
        }

        energyTelemetryTick = gameTime;
        lastEnergyInputMoved = energyInputMovedThisTick;
        lastEnergyOutputMoved = energyOutputMovedThisTick;
        energyInputMovedThisTick = 0;
        energyOutputMovedThisTick = 0;
    }

    private int insertIntoTarget(int amount, boolean simulate) {
        Level level = owner.getLevel();
        if (level == null || amount <= 0) {
            return 0;
        }
        BlockPos targetPos = owner.getBlockPos().relative(face);
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

    private ItemStack distributeItemToNetwork(int itemChannel, ItemStack stack, boolean simulate) {
        Level level = owner.getLevel();
        if (level == null || stack.isEmpty()) {
            return stack;
        }

        return CableBlockEntity.distributeItems(
                level,
                owner.getBlockPos(),
                CableBlockEntity.TransferOrigin.endpoint(owner.getBlockPos(), face),
                itemChannel,
                stack,
                simulate
        );
    }

    private FluidStack distributeFluidToNetwork(int fluidChannel, FluidStack stack, boolean simulate) {
        Level level = owner.getLevel();
        if (level == null || stack.isEmpty()) {
            return stack;
        }

        return CableBlockEntity.distributeFluids(
                level,
                owner.getBlockPos(),
                CableBlockEntity.TransferOrigin.endpoint(owner.getBlockPos(), face),
                fluidChannel,
                stack,
                simulate
        );
    }

    private IFluidHandler targetFluidHandler(Direction targetSide) {
        Level level = owner.getLevel();
        if (level == null || targetSide == null) {
            return null;
        }
        BlockPos targetPos = owner.getBlockPos().relative(face);
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof UniversalConnectorBlock || targetState.getBlock() instanceof CableBlock) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, targetSide);
    }

    private IItemHandler targetItemHandler(Direction targetSide) {
        Level level = owner.getLevel();
        if (level == null || targetSide == null) {
            return null;
        }
        BlockPos targetPos = owner.getBlockPos().relative(face);
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
        Level level = owner.getLevel();
        return level != null
                && tier.jamChancePerThousand() > 0
                && level.random.nextInt(1000) < tier.jamChancePerThousand();
    }

    private boolean rollFluidJam(FluidConnectorTier tier) {
        Level level = owner.getLevel();
        return level != null
                && tier.jamChancePerThousand() > 0
                && level.random.nextInt(1000) < tier.jamChancePerThousand();
    }

    private Optional<EnergyConnectorTier> connectorTier() {
        ItemStack stack = inventory.getStackInSlot(UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR);
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
        ItemStack stack = inventory.getStackInSlot(UniversalConnectorBlockEntity.fluidConnectorSlot(moduleIndex));
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
        ItemStack stack = inventory.getStackInSlot(UniversalConnectorBlockEntity.itemConnectorSlot(moduleIndex));
        if (stack.getItem() instanceof ItemConnectorItem connector && connector.tier().enabled()) {
            return Optional.of(connector.tier());
        }
        return Optional.empty();
    }

    public NetworkBridgeType bridgeType() {
        ItemStack stack = inventory.getStackInSlot(UniversalConnectorBlockEntity.SLOT_RESERVED_CONNECTOR);
        if (stack.getItem() instanceof NetworkConnectorItem connector) {
            return connector.bridgeType();
        }
        return NetworkBridgeType.NONE;
    }

    public int bridgeChannel() {
        return bridgeChannel;
    }

    public boolean hasLoadedBridgeModule() {
        NetworkBridgeType bridgeType = bridgeType();
        return bridgeType != NetworkBridgeType.NONE && bridgeType.isLoaded();
    }

    public boolean hasActiveBridgeModule() {
        return hasLoadedBridgeModule() && hasValidAttachmentTarget();
    }

    public boolean hasValidAttachmentTarget() {
        Level level = owner.getLevel();
        return level != null && CableBlock.isValidConnectorTarget(level.getBlockState(owner.getBlockPos().relative(face)));
    }

    private int sameChannelBridgeEndpointCount() {
        NetworkBridgeType bridgeType = bridgeType();
        Level level = owner.getLevel();
        if (bridgeType == NetworkBridgeType.NONE || level == null) {
            return 0;
        }
        return CableBlockEntity.bridgeEndpointCount(level, owner.getBlockPos(), bridgeType, bridgeChannel);
    }

    private boolean passesFluidFilter(int moduleIndex, FluidStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        boolean hasFilter = false;
        for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
            FluidStack filter = fluidFilter(moduleIndex, filterIndex);
            if (filter.isEmpty()) {
                continue;
            }
            hasFilter = true;
            if (FluidStack.isSameFluidSameComponents(filter, stack)) {
                return true;
            }
        }
        return !hasFilter;
    }

    private FluidStack fluidFilter(int moduleIndex, int filterIndex) {
        if (fluidModule(moduleIndex) == null || !isValidFilterIndex(filterIndex)) {
            return FluidStack.EMPTY;
        }
        ItemStack filterStack = filterInventory.getStackInSlot(UniversalConnectorBlockEntity.fluidFilterSlot(
                moduleIndex,
                filterIndex
        ));
        return FluidUtil.getFluidContained(filterStack).orElse(FluidStack.EMPTY);
    }

    private boolean passesItemFilter(int moduleIndex, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        List<ItemStack> filters = new ArrayList<>(UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE);
        for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
            filters.add(itemFilter(moduleIndex, filterIndex));
        }
        return ItemFilterMatcher.passesConnectorFilters(filters, stack);
    }

    private ItemStack itemFilter(int moduleIndex, int filterIndex) {
        if (itemModule(moduleIndex) == null || !isValidFilterIndex(filterIndex)) {
            return ItemStack.EMPTY;
        }
        return filterInventory.getStackInSlot(UniversalConnectorBlockEntity.itemFilterSlot(moduleIndex, filterIndex));
    }

    private static boolean isValidFilterIndex(int filterIndex) {
        return filterIndex >= 0 && filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE;
    }

    private boolean setFilterStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= filterInventory.getSlots()) {
            return false;
        }
        ItemStack next = stack == null ? ItemStack.EMPTY : stack;
        ItemStack current = filterInventory.getStackInSlot(slot);
        if (ItemStack.isSameItemSameComponents(current, next) && current.getCount() == next.getCount()) {
            return false;
        }
        filterInventory.setStackInSlot(slot, next);
        return true;
    }

    public static boolean isFluidFilterStack(ItemStack stack) {
        return stack != null && !stack.isEmpty() && FluidUtil.getFluidContained(stack).isPresent();
    }

    public static ItemStack normalizeFluidFilterStack(ItemStack stack) {
        return isFluidFilterStack(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }

    public static ItemStack normalizeItemFilterStack(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private boolean hasInputItemModule() {
        for (int moduleIndex = 0; moduleIndex < itemModules.length; moduleIndex++) {
            ItemModuleState module = itemModules[moduleIndex];
            if (module.attachAs != null && module.mode.pullsFromTarget() && itemConnectorTier(moduleIndex).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInputFluidModule() {
        for (int moduleIndex = 0; moduleIndex < fluidModules.length; moduleIndex++) {
            FluidModuleState module = fluidModules[moduleIndex];
            if (module.attachAs != null && module.mode.pullsFromTarget() && fluidConnectorTier(moduleIndex).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private int connectorData(int index) {
        if (UniversalConnectorBlockEntity.isNetworkDebugDataIndex(index)) {
            return UniversalConnectorBlockEntity.networkDebugData(index, networkDebugSnapshot());
        }
        if (UniversalConnectorBlockEntity.isNetworkEnergyTelemetryDataIndex(index)) {
            return UniversalConnectorBlockEntity.networkEnergyTelemetryData(index, networkEnergyTelemetrySnapshot());
        }
        return moduleData(index);
    }

    private CableBlockEntity.NetworkDebugSnapshot networkDebugSnapshot() {
        Level level = owner.getLevel();
        return level == null
                ? CableBlockEntity.NetworkDebugSnapshot.EMPTY
                : CableBlockEntity.networkDebugSnapshot(level, owner.getBlockPos());
    }

    private CableBlockEntity.NetworkEnergyTelemetrySnapshot networkEnergyTelemetrySnapshot() {
        Level level = owner.getLevel();
        return level == null
                ? CableBlockEntity.NetworkEnergyTelemetrySnapshot.EMPTY
                : CableBlockEntity.networkEnergyTelemetrySnapshot(level, owner.getBlockPos(), channel);
    }

    private int moduleData(int index) {
        int itemBase = UniversalConnectorBlockEntity.dataItemChannelIndex(0);
        if (index >= UniversalConnectorBlockEntity.dataFluidChannelIndex(0) && index < itemBase) {
            return fluidModuleData(index);
        }
        return itemModuleData(index);
    }

    private int fluidModuleData(int index) {
        int relative = index - UniversalConnectorBlockEntity.dataFluidChannelIndex(0);
        if (relative < 0) {
            return 0;
        }
        int moduleIndex = relative / 5;
        int offset = relative % 5;
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null) {
            return 0;
        }
        return switch (offset) {
            case 0 -> module.channel;
            case 1 -> module.attachAs == null ? -1 : module.attachAs.ordinal();
            case 2 -> module.mode.ordinal();
            case 3 -> module.cooldownTicks;
            case 4 -> module.jamTicks;
            default -> 0;
        };
    }

    private int itemModuleData(int index) {
        int relative = index - UniversalConnectorBlockEntity.dataItemChannelIndex(0);
        if (relative < 0) {
            return 0;
        }
        int moduleIndex = relative / 5;
        int offset = relative % 5;
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null) {
            return 0;
        }
        return switch (offset) {
            case 0 -> module.channel;
            case 1 -> module.attachAs == null ? -1 : module.attachAs.ordinal();
            case 2 -> module.mode.ordinal();
            case 3 -> module.cooldownTicks;
            case 4 -> module.jamTicks;
            default -> 0;
        };
    }

    private FluidModuleState fluidModule(int moduleIndex) {
        return moduleIndex >= 0 && moduleIndex < fluidModules.length ? fluidModules[moduleIndex] : null;
    }

    private ItemModuleState itemModule(int moduleIndex) {
        return moduleIndex >= 0 && moduleIndex < itemModules.length ? itemModules[moduleIndex] : null;
    }

    private static Direction nextModuleAttachAs(Direction current) {
        if (current == null) {
            return Direction.DOWN;
        }
        int next = current.ordinal() + 1;
        return next >= DIRECTIONS.length ? null : DIRECTIONS[next];
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
            this.channel = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, channel));
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
            channel = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, tag.getInt("Channel")));
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
            this.channel = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, channel));
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
            channel = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, tag.getInt("Channel")));
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
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return receiveFromTarget(toReceive, simulate);
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
            return connectorTier().isPresent() && mode.acceptsNetworkInput();
        }
    }

    private final class NetworkItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return itemModules.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemModuleState module = itemModule(slot);
            Optional<ItemConnectorTier> tier = itemConnectorTier(slot);
            if (module == null
                    || tier.isEmpty()
                    || module.attachAs == null
                    || !module.mode.pullsFromTarget()
                    || module.cooldownTicks > 0
                    || module.jamTicks > 0
                    || !passesItemFilter(slot, stack)) {
                return stack;
            }

            int shipmentSize = Math.min(stack.getCount(), tier.get().itemsPerShipment());
            ItemStack shipment = stack.copyWithCount(shipmentSize);
            ItemStack simulatedRemainder = distributeItemToNetwork(module.channel, shipment, true);
            int accepted = shipmentSize - simulatedRemainder.getCount();
            if (accepted <= 0) {
                return stack;
            }
            if (simulate) {
                ItemStack remaining = stack.copy();
                remaining.shrink(accepted);
                return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
            }

            if (rollItemJam(tier.get())) {
                module.jamTicks = tier.get().jamTicks();
                owner.setChanged();
                return stack;
            }

            ItemStack acceptedStack = stack.copyWithCount(accepted);
            ItemStack remainder = distributeItemToNetwork(module.channel, acceptedStack, false);
            int moved = accepted - remainder.getCount();
            if (moved <= 0) {
                return stack;
            }

            module.cooldownTicks = tier.get().waitTicks();
            owner.setChanged();
            ItemStack remaining = stack.copy();
            remaining.shrink(moved);
            return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemConnectorTier(slot).map(ItemConnectorTier::itemsPerShipment).orElse(64);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            ItemModuleState module = itemModule(slot);
            return module != null
                    && module.attachAs != null
                    && module.mode.pullsFromTarget()
                    && itemConnectorTier(slot).isPresent()
                    && passesItemFilter(slot, stack);
        }
    }

    private final class NetworkFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return fluidModules.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidConnectorTier(tank).map(FluidConnectorTier::fluidPerShipment).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            FluidModuleState module = fluidModule(tank);
            return module != null
                    && module.attachAs != null
                    && module.mode.pullsFromTarget()
                    && fluidConnectorTier(tank).isPresent()
                    && passesFluidFilter(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }

            for (int moduleIndex = 0; moduleIndex < fluidModules.length; moduleIndex++) {
                FluidModuleState module = fluidModules[moduleIndex];
                Optional<FluidConnectorTier> tier = fluidConnectorTier(moduleIndex);
                if (tier.isEmpty()
                        || module.attachAs == null
                        || !module.mode.pullsFromTarget()
                        || module.cooldownTicks > 0
                        || module.jamTicks > 0
                        || !passesFluidFilter(moduleIndex, resource)) {
                    continue;
                }

                int shipmentSize = Math.min(resource.getAmount(), tier.get().fluidPerShipment());
                FluidStack shipment = resource.copyWithAmount(shipmentSize);
                FluidStack simulatedRemainder = distributeFluidToNetwork(module.channel, shipment, true);
                int accepted = shipmentSize - simulatedRemainder.getAmount();
                if (accepted <= 0) {
                    continue;
                }
                if (action.simulate()) {
                    return accepted;
                }

                if (rollFluidJam(tier.get())) {
                    module.jamTicks = tier.get().jamTicks();
                    owner.setChanged();
                    return 0;
                }

                FluidStack remainder = distributeFluidToNetwork(
                        module.channel,
                        resource.copyWithAmount(accepted),
                        false
                );
                int moved = accepted - remainder.getAmount();
                if (moved <= 0) {
                    continue;
                }

                module.cooldownTicks = tier.get().waitTicks();
                owner.setChanged();
                return moved;
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
