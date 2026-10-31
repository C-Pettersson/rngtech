package com.rngtech.content.blockentity;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.CableStats;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.FluidConnectorTier;
import com.rngtech.content.cable.ItemConnectorMode;
import com.rngtech.content.cable.ItemConnectorTier;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.cable.StallBackoff;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.item.NetworkConnectorItem;
import com.rngtech.content.itemfilter.ItemFilterMatcher;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.wrench.LinkStatus;
import com.rngtech.util.TickTransferCounter;

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
            if (UniversalConnectorBlockEntity.isEnergyConnectorSlot(slot)) {
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
    private final List<ConnectorTargetCache<IEnergyStorage>> energyTargets = new ArrayList<>(DIRECTIONS.length);
    private final List<ConnectorTargetCache<IItemHandler>> itemTargets = new ArrayList<>(DIRECTIONS.length);
    private final List<ConnectorTargetCache<IFluidHandler>> fluidTargets = new ArrayList<>(DIRECTIONS.length);
    private final IItemHandler itemHandler = new NetworkItemHandler();
    private final IFluidHandler fluidHandler = new NetworkFluidHandler();
    private final EnergyModuleState[] energyModules =
            new EnergyModuleState[UniversalConnectorBlockEntity.ENERGY_MODULE_SLOT_COUNT];
    private final FluidModuleState[] fluidModules =
            new FluidModuleState[UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT];
    private final ItemModuleState[] itemModules =
            new ItemModuleState[UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT];
    private int bridgeChannel;
    private int fluidModuleTickCursor;
    private int itemModuleTickCursor;

    public CableUniversalConnectorData(UniversalConnectorDataOwner owner, Direction face) {
        this.owner = owner;
        this.face = face;
        for (int index = 0; index < energyModules.length; index++) {
            energyModules[index] = new EnergyModuleState(defaultAttachAs());
        }
        for (int index = 0; index < fluidModules.length; index++) {
            fluidModules[index] = FluidModuleState.defaults(defaultAttachAs());
        }
        for (int index = 0; index < itemModules.length; index++) {
            itemModules[index] = ItemModuleState.defaults(defaultAttachAs());
        }
        for (int side = 0; side < DIRECTIONS.length; side++) {
            energyTargets.add(new ConnectorTargetCache<>(Capabilities.EnergyStorage.BLOCK));
            itemTargets.add(new ConnectorTargetCache<>(Capabilities.ItemHandler.BLOCK));
            fluidTargets.add(new ConnectorTargetCache<>(Capabilities.FluidHandler.BLOCK));
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
        return side == face && hasEnergyModule() ? energyStorage : null;
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
        for (int moduleIndex = 0; moduleIndex < energyModules.length; moduleIndex++) {
            energyModules[moduleIndex].beginTelemetryTick(owner.getLevel());
            requestNetworkEnergyPass(moduleIndex);
        }
        tickFluidModules();
        tickItemModules();
    }

    /** True when the attached block exposes FE on the side this Energy row attaches as. */
    public boolean targetHasEnergyAccess(int moduleIndex) {
        return targetEnergyStorage(moduleIndex) != null;
    }

    IEnergyStorage targetEnergyStorage(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        return module == null ? null : energyTargets.get(module.attachAs.ordinal())
                .get(owner.getLevel(), owner.getBlockPos().relative(face), module.attachAs);
    }

    private CableBlockEntity.TransferOrigin energySourceOrigin(EnergyModuleState module, IEnergyStorage source) {
        BlockPos ownerPos = owner.getBlockPos();
        return CableBlockEntity.TransferOrigin.endpoint(ownerPos, face, ownerPos.relative(face))
                .withEnergyBuffer(module.mode == CableConnectorMode.BOTH ? source : null);
    }

    /** Bit mask of the item channels this connector inserts on: rows set to OUT with a module and an Attach side. */
    int itemReceiveChannelMask() {
        int mask = 0;
        for (int moduleIndex = 0; moduleIndex < itemModules.length; moduleIndex++) {
            ItemModuleState module = itemModules[moduleIndex];
            if (module.attachAs != null && module.mode.insertsIntoTarget() && itemConnectorTier(moduleIndex).isPresent()) {
                mask |= 1 << module.channel;
            }
        }
        return mask;
    }

    /** Bit mask of the fluid channels this connector inserts on: rows set to OUT with a module and an Attach side. */
    int fluidReceiveChannelMask() {
        int mask = 0;
        for (int moduleIndex = 0; moduleIndex < fluidModules.length; moduleIndex++) {
            FluidModuleState module = fluidModules[moduleIndex];
            if (module.attachAs != null && module.mode.insertsIntoTarget() && fluidConnectorTier(moduleIndex).isPresent()) {
                mask |= 1 << module.channel;
            }
        }
        return mask;
    }

    /** The channel this Energy row works on, or -1 without an Energy Connector in it. */
    int networkEnergyChannel(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        return module != null && energyTransferRate(moduleIndex) > 0 ? module.channel : -1;
    }

    CableConnectorMode energyMode(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        return module == null ? CableConnectorMode.BOTH : module.mode;
    }

    EnergyDistributionMode energyDistributionMode(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        return module == null ? EnergyDistributionMode.ROUND_ROBIN : module.distributionMode;
    }

    /** FE this Energy row may still take from its block into the network this tick. */
    int networkEnergyInputBudget(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        return module == null ? 0 : module.remainingInputBudget(owner.getLevel(), energyTransferRate(moduleIndex));
    }

    /** FE this Energy row may still deliver from the network into its block this tick. */
    int networkEnergyOutputBudget(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        Level level = owner.getLevel();
        return module == null || level == null
                ? 0
                : module.outputBudget.remaining(level.getGameTime(), energyTransferRate(moduleIndex));
    }

    void recordNetworkEnergyInput(int moduleIndex, int amount) {
        EnergyModuleState module = energyModule(moduleIndex);
        if (module != null) {
            module.recordInput(owner.getLevel(), amount);
        }
    }

    void recordNetworkEnergyOutput(int moduleIndex, int amount) {
        EnergyModuleState module = energyModule(moduleIndex);
        if (module != null) {
            module.recordOutput(owner.getLevel(), amount);
        }
    }

    void addNetworkDebugStats(CableBlockEntity.NetworkDebugAccumulator accumulator) {
        for (int moduleIndex = 0; moduleIndex < energyModules.length; moduleIndex++) {
            EnergyModuleState module = energyModules[moduleIndex];
            if (energyConnectorTier(moduleIndex).isEmpty()) {
                continue;
            }
            accumulator.addEnergyModule(module.channel);
            accumulator.addEnergyConnector(
                    module.channel,
                    module.mode,
                    energyTransferRate(moduleIndex),
                    targetEnergyStorage(moduleIndex),
                    module.lastInput(owner.getLevel()),
                    module.lastOutput(owner.getLevel())
            );
        }
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT; moduleIndex++) {
            FluidModuleState module = fluidModules[moduleIndex];
            Optional<FluidConnectorTier> tier = fluidConnectorTier(moduleIndex);
            tier.ifPresent(connector -> accumulator.addFluidModule(
                    module.channel,
                    module.mode,
                    connector.fluidPerShipment(),
                    module.attachAs != null
            ));
        }
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT; moduleIndex++) {
            ItemModuleState module = itemModules[moduleIndex];
            Optional<ItemConnectorTier> tier = itemConnectorTier(moduleIndex);
            tier.ifPresent(connector -> accumulator.addItemModule(
                    module.channel,
                    module.mode,
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
        tag.putInt("BridgeChannel", bridgeChannel);
        CompoundTag energyModulesTag = new CompoundTag();
        for (int index = 0; index < energyModules.length; index++) {
            energyModulesTag.put(Integer.toString(index), energyModules[index].save());
        }
        tag.put("EnergyModules", energyModulesTag);
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
        bridgeChannel = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, tag.getInt("BridgeChannel")));
        CompoundTag energyModulesTag = tag.getCompound("EnergyModules");
        for (int index = 0; index < energyModules.length; index++) {
            String key = Integer.toString(index);
            if (energyModulesTag.contains(key, Tag.TAG_COMPOUND)) {
                energyModules[index].load(energyModulesTag.getCompound(key), defaultAttachAs());
            } else if (index == 0 && !tag.contains("EnergyModules", Tag.TAG_COMPOUND)) {
                // Saves from before Energy rows keep the single connector's settings at the top level.
                energyModules[index].load(tag, defaultAttachAs());
            } else {
                energyModules[index].reset(defaultAttachAs());
            }
        }
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
        if (savedSize <= UniversalConnectorBlockEntity.LEGACY_SLOT_COUNT) {
            return;
        }
        ItemStackHandler legacyInventory = new ItemStackHandler(savedSize);
        legacyInventory.deserializeNBT(registries, inventoryTag);
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT; moduleIndex++) {
            for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
                int legacySlot = UniversalConnectorBlockEntity.LEGACY_SLOT_COUNT
                        + moduleIndex * UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE
                        + filterIndex;
                if (legacySlot >= legacyInventory.getSlots()) {
                    continue;
                }
                setFluidFilter(moduleIndex, filterIndex, legacyInventory.getStackInSlot(legacySlot));
            }
        }
        int legacyItemStart = UniversalConnectorBlockEntity.LEGACY_SLOT_COUNT
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
                if (UniversalConnectorBlockEntity.isEnergyModuleDataIndex(index)) {
                    return energyModuleData(index);
                }
                if (index == UniversalConnectorBlockEntity.dataBridgeChannelIndex()) {
                    return bridgeChannel;
                }
                if (index == UniversalConnectorBlockEntity.dataBridgeTypeIndex()) {
                    return bridgeType().dataId();
                }
                if (index == UniversalConnectorBlockEntity.dataBridgeModLoadedIndex()) {
                    return bridgeType() != NetworkBridgeType.NONE && bridgeType().isLoaded() ? 1 : 0;
                }
                if (index == UniversalConnectorBlockEntity.dataBridgeEndpointCountIndex()) {
                    return sameChannelBridgeEndpointCount();
                }
                return connectorData(index);
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
    public boolean incrementEnergyChannel(int moduleIndex, int amount) {
        EnergyModuleState module = energyModule(moduleIndex);
        if (module == null) {
            return false;
        }
        return setEnergyChannel(moduleIndex, Math.floorMod(module.channel + amount, UniversalConnectorBlockEntity.MAX_CHANNEL + 1));
    }

    @Override
    public boolean setEnergyChannel(int moduleIndex, int channel) {
        EnergyModuleState module = energyModule(moduleIndex);
        int next = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, channel));
        if (module == null || module.channel == next) {
            return false;
        }
        module.channel = next;
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
    public boolean cycleEnergyAttachAs(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        return module != null && setEnergyAttachAs(moduleIndex, DIRECTIONS[(module.attachAs.ordinal() + 1) % DIRECTIONS.length]);
    }

    @Override
    public boolean setEnergyAttachAs(int moduleIndex, Direction attachAs) {
        EnergyModuleState module = energyModule(moduleIndex);
        if (module == null || attachAs == null || module.attachAs == attachAs) {
            return false;
        }
        module.attachAs = attachAs;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean cycleEnergyMode(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        return module != null && setEnergyMode(moduleIndex, module.mode.next());
    }

    @Override
    public boolean setEnergyMode(int moduleIndex, CableConnectorMode mode) {
        EnergyModuleState module = energyModule(moduleIndex);
        if (module == null || mode == null || module.mode == mode) {
            return false;
        }
        module.mode = mode;
        invalidateConnectorState();
        return true;
    }

    @Override
    public boolean cycleEnergyDistributionMode(int moduleIndex) {
        EnergyModuleState module = energyModule(moduleIndex);
        return module != null && setEnergyDistributionMode(moduleIndex, module.distributionMode.next());
    }

    @Override
    public boolean setEnergyDistributionMode(int moduleIndex, EnergyDistributionMode distributionMode) {
        EnergyModuleState module = energyModule(moduleIndex);
        if (module == null || distributionMode == null || module.distributionMode == distributionMode) {
            return false;
        }
        module.distributionMode = distributionMode;
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
        for (FluidModuleState module : fluidModules) {
            module.stall.clear();
        }
        for (ItemModuleState module : itemModules) {
            module.stall.clear();
        }
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
            if (module.stall.active() && module.stall.waiting(CableBlockEntity.networkVersion(level, owner.getBlockPos()))) {
                continue;
            }

            Optional<FluidConnectorTier> tier = fluidConnectorTier(moduleIndex);
            if (tier.isEmpty() || !module.mode.pullsFromTarget()) {
                continue;
            }

            CableStats.increment(CableStats.Counter.MODULE_ATTEMPTS);
            int moved = tryTransferFluidModule(moduleIndex, tier.get());
            if (module.jamTicks > 0) {
                changed = true;
                continue;
            }
            if (moved > 0) {
                module.cooldownTicks = tier.get().waitTicks();
                changed = true;
            } else {
                module.stall.start(CableBlockEntity.networkVersion(level, owner.getBlockPos()), tier.get().waitTicks());
                CableStats.increment(CableStats.Counter.MODULE_BACKOFFS);
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
            if (module.stall.active() && module.stall.waiting(CableBlockEntity.networkVersion(level, owner.getBlockPos()))) {
                continue;
            }

            Optional<ItemConnectorTier> tier = itemConnectorTier(moduleIndex);
            if (tier.isEmpty() || !module.mode.pullsFromTarget()) {
                continue;
            }

            CableStats.increment(CableStats.Counter.MODULE_ATTEMPTS);
            int moved = tryTransferItemModule(moduleIndex, tier.get());
            if (module.jamTicks > 0) {
                changed = true;
                continue;
            }
            if (moved > 0) {
                module.cooldownTicks = tier.get().waitTicks();
                changed = true;
            } else {
                module.stall.start(CableBlockEntity.networkVersion(level, owner.getBlockPos()), tier.get().waitTicks());
                CableStats.increment(CableStats.Counter.MODULE_BACKOFFS);
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

    /** FE pushed into the connector by its block goes to the Input or Both rows in order, each up to its own cap. */
    private int receiveFromTarget(int amount, boolean simulate) {
        Level level = owner.getLevel();
        if (level == null || amount <= 0) {
            return 0;
        }
        int moved = 0;
        for (int moduleIndex = 0; moduleIndex < energyModules.length && moved < amount; moduleIndex++) {
            moved += receiveFromTarget(level, moduleIndex, amount - moved, simulate);
        }
        return moved;
    }

    private int receiveFromTarget(Level level, int moduleIndex, int amount, boolean simulate) {
        EnergyModuleState module = energyModules[moduleIndex];
        if (!module.mode.acceptsNetworkInput()) {
            return 0;
        }
        int transferable = Math.min(amount, networkEnergyInputBudget(moduleIndex));
        if (transferable <= 0) {
            return 0;
        }

        int moved = CableBlockEntity.distributeEnergy(
                level,
                owner.getBlockPos(),
                energySourceOrigin(module, targetEnergyStorage(moduleIndex)),
                module.channel,
                transferable,
                simulate,
                module.distributionMode
        );
        if (!simulate) {
            module.recordInput(level, moved);
        }
        return moved;
    }

    /** An Input or Both row asks for its network's energy pass, which pulls FE from every source at once. */
    private void requestNetworkEnergyPass(int moduleIndex) {
        Level level = owner.getLevel();
        EnergyModuleState module = energyModules[moduleIndex];
        // A standalone plate has no cable, so it has no network to run a pass on.
        if (level == null
                || level.isClientSide
                || !(owner instanceof CableBlockEntity)
                || !module.mode.acceptsNetworkInput()
                || energyTransferRate(moduleIndex) <= 0) {
            return;
        }
        CableBlockEntity.requestEnergyPass(level, owner.getBlockPos(), module.channel);
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
        if (targetSide == null) {
            return null;
        }
        return fluidTargets.get(targetSide.ordinal()).get(owner.getLevel(), owner.getBlockPos().relative(face), targetSide);
    }

    private IItemHandler targetItemHandler(Direction targetSide) {
        if (targetSide == null) {
            return null;
        }
        return itemTargets.get(targetSide.ordinal()).get(owner.getLevel(), owner.getBlockPos().relative(face), targetSide);
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

    private Optional<EnergyConnectorTier> energyConnectorTier(int moduleIndex) {
        if (energyModule(moduleIndex) == null) {
            return Optional.empty();
        }
        ItemStack stack = inventory.getStackInSlot(UniversalConnectorBlockEntity.energyConnectorSlot(moduleIndex));
        if (stack.getItem() instanceof EnergyConnectorItem connector && connector.tier().enabled()) {
            return Optional.of(connector.tier());
        }
        return Optional.empty();
    }

    private int energyTransferRate(int moduleIndex) {
        return energyConnectorTier(moduleIndex).map(EnergyConnectorTier::transferRate).orElse(0);
    }

    private boolean hasEnergyModule() {
        for (int moduleIndex = 0; moduleIndex < energyModules.length; moduleIndex++) {
            if (energyConnectorTier(moduleIndex).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /** The channel the Network tab's energy row reads: the first Energy row with a connector, else the first row. */
    private int primaryEnergyChannel() {
        for (int moduleIndex = 0; moduleIndex < energyModules.length; moduleIndex++) {
            if (energyConnectorTier(moduleIndex).isPresent()) {
                return energyModules[moduleIndex].channel;
            }
        }
        return energyModules[0].channel;
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
        BlockPos targetPos = owner.getBlockPos().relative(face);
        return level != null && level.isLoaded(targetPos) && CableBlock.isValidConnectorTarget(level.getBlockState(targetPos));
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

    private int energyModuleData(int index) {
        int moduleIndex = UniversalConnectorBlockEntity.energyDataModule(index);
        EnergyModuleState module = energyModules[moduleIndex];
        if (index == UniversalConnectorBlockEntity.dataEnergyChannelIndex(moduleIndex)) {
            return module.channel;
        }
        if (index == UniversalConnectorBlockEntity.dataEnergyAttachAsIndex(moduleIndex)) {
            return module.attachAs.ordinal();
        }
        if (index == UniversalConnectorBlockEntity.dataEnergyModeIndex(moduleIndex)) {
            return module.mode.ordinal();
        }
        if (index == UniversalConnectorBlockEntity.dataEnergyTransferRateIndex(moduleIndex)) {
            return energyTransferRate(moduleIndex);
        }
        if (index == UniversalConnectorBlockEntity.dataEnergyDistributionIndex(moduleIndex)) {
            return module.distributionMode.ordinal();
        }
        if (index == UniversalConnectorBlockEntity.dataEnergyLastInputIndex(moduleIndex)) {
            return module.lastInput(owner.getLevel());
        }
        if (index == UniversalConnectorBlockEntity.dataEnergyLastOutputIndex(moduleIndex)) {
            return module.lastOutput(owner.getLevel());
        }
        return targetHasEnergyAccess(moduleIndex) ? 1 : 0;
    }

    private int connectorData(int index) {
        if (index == UniversalConnectorBlockEntity.dataNetworkEnergyInputCapIndex()) {
            return networkDebugSnapshot().energyInputCap();
        }
        if (index == UniversalConnectorBlockEntity.dataNetworkEnergyOutputCapIndex()) {
            return networkDebugSnapshot().energyOutputCap();
        }
        if (index == UniversalConnectorBlockEntity.dataNetworkEnergyChannelCapIndex()) {
            return networkDebugSnapshot().energyChannelCap(primaryEnergyChannel());
        }
        if (UniversalConnectorBlockEntity.isNetworkDebugDataIndex(index)) {
            return UniversalConnectorBlockEntity.networkDebugData(index, networkDebugSnapshot());
        }
        if (UniversalConnectorBlockEntity.isNetworkEnergyTelemetryDataIndex(index)) {
            return UniversalConnectorBlockEntity.networkEnergyTelemetryData(index, networkEnergyTelemetrySnapshot());
        }
        return moduleData(index);
    }

    @Override
    public LinkStatus energyLinkStatus() {
        for (int moduleIndex = 0; moduleIndex < energyModules.length; moduleIndex++) {
            LinkStatus status = energyLinkStatus(moduleIndex);
            if (status.isProblem()) {
                return status;
            }
        }
        return LinkStatus.OK;
    }

    @Override
    public LinkStatus energyLinkStatus(int moduleIndex) {
        if (energyConnectorTier(moduleIndex).isEmpty()) {
            return LinkStatus.OK;
        }
        IEnergyStorage target = targetEnergyStorage(moduleIndex);
        if (target == null) {
            return LinkStatus.TARGET_BLOCKED;
        }
        EnergyModuleState module = energyModules[moduleIndex];
        int rate = energyTransferRate(moduleIndex);
        Level level = owner.getLevel();
        return networkDebugSnapshot().links().status(
                CableBlockEntity.LinkCounts.Kind.ENERGY,
                module.channel,
                module.mode.acceptsNetworkInput(),
                module.mode.sendsNetworkOutput(),
                CableBlockEntity.countsAsEnergySource(module.mode, rate, target, module.lastInput(level)),
                CableBlockEntity.countsAsEnergySink(module.mode, rate, target, module.lastOutput(level))
        );
    }

    @Override
    public LinkStatus fluidLinkStatus(int moduleIndex) {
        FluidModuleState module = fluidModule(moduleIndex);
        if (module == null || fluidConnectorTier(moduleIndex).isEmpty()) {
            return LinkStatus.OK;
        }
        if (module.attachAs == null) {
            return LinkStatus.NOT_ATTACHED;
        }
        return networkDebugSnapshot().links().status(
                CableBlockEntity.LinkCounts.Kind.FLUID,
                module.channel,
                module.mode.pullsFromTarget(),
                module.mode.insertsIntoTarget(),
                module.mode.pullsFromTarget(),
                module.mode.insertsIntoTarget()
        );
    }

    @Override
    public LinkStatus itemLinkStatus(int moduleIndex) {
        ItemModuleState module = itemModule(moduleIndex);
        if (module == null || itemConnectorTier(moduleIndex).isEmpty()) {
            return LinkStatus.OK;
        }
        if (module.attachAs == null) {
            return LinkStatus.NOT_ATTACHED;
        }
        return networkDebugSnapshot().links().status(
                CableBlockEntity.LinkCounts.Kind.ITEM,
                module.channel,
                module.mode.pullsFromTarget(),
                module.mode.insertsIntoTarget(),
                module.mode.pullsFromTarget(),
                module.mode.insertsIntoTarget()
        );
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
                : CableBlockEntity.networkEnergyTelemetrySnapshot(level, owner.getBlockPos(), primaryEnergyChannel());
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

    private EnergyModuleState energyModule(int moduleIndex) {
        return moduleIndex >= 0 && moduleIndex < energyModules.length ? energyModules[moduleIndex] : null;
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

    /** One Energy row: its settings, its per-tick input and output budgets, and the FE it moved last tick. */
    private static final class EnergyModuleState {
        private final TickTransferCounter outputBudget = new TickTransferCounter();
        private int channel;
        private Direction attachAs;
        private CableConnectorMode mode = CableConnectorMode.BOTH;
        private EnergyDistributionMode distributionMode = EnergyDistributionMode.ROUND_ROBIN;
        private long inputBudgetTick = Long.MIN_VALUE;
        private int inputTransferredThisTick;
        private long telemetryTick = Long.MIN_VALUE;
        private int inputMovedThisTick;
        private int outputMovedThisTick;
        private int lastInputMoved;
        private int lastOutputMoved;

        private EnergyModuleState(Direction attachAs) {
            this.attachAs = attachAs;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Channel", channel);
            tag.putString("AttachAs", attachAs.getSerializedName());
            tag.putString("Mode", mode.getSerializedName());
            tag.putString("Distribution", distributionMode.getSerializedName());
            return tag;
        }

        private void load(CompoundTag tag, Direction defaultAttachAs) {
            channel = Math.max(0, Math.min(UniversalConnectorBlockEntity.MAX_CHANNEL, tag.getInt("Channel")));
            Direction loadedAttachAs = Direction.byName(tag.getString("AttachAs"));
            attachAs = loadedAttachAs == null ? defaultAttachAs : loadedAttachAs;
            mode = CableConnectorMode.bySerializedName(tag.getString("Mode")).orElse(CableConnectorMode.BOTH);
            distributionMode = EnergyDistributionMode.bySerializedName(tag.getString("Distribution"))
                    .orElse(EnergyDistributionMode.ROUND_ROBIN);
        }

        private void reset(Direction defaultAttachAs) {
            channel = 0;
            attachAs = defaultAttachAs;
            mode = CableConnectorMode.BOTH;
            distributionMode = EnergyDistributionMode.ROUND_ROBIN;
        }

        private int remainingInputBudget(Level level, int transferRate) {
            if (level == null) {
                return 0;
            }
            resetInputBudget(level.getGameTime());
            return Math.max(0, transferRate - inputTransferredThisTick);
        }

        private void recordInput(Level level, int amount) {
            if (level == null || amount <= 0) {
                return;
            }
            beginTelemetryTick(level);
            resetInputBudget(level.getGameTime());
            inputTransferredThisTick += amount;
            inputMovedThisTick += amount;
        }

        private void recordOutput(Level level, int amount) {
            if (level == null || amount <= 0) {
                return;
            }
            beginTelemetryTick(level);
            outputBudget.add(level.getGameTime(), amount);
            outputMovedThisTick += amount;
        }

        private int lastInput(Level level) {
            beginTelemetryTick(level);
            return lastInputMoved;
        }

        private int lastOutput(Level level) {
            beginTelemetryTick(level);
            return lastOutputMoved;
        }

        private void resetInputBudget(long gameTime) {
            if (inputBudgetTick != gameTime) {
                inputBudgetTick = gameTime;
                inputTransferredThisTick = 0;
            }
        }

        private void beginTelemetryTick(Level level) {
            if (level == null || telemetryTick == level.getGameTime()) {
                return;
            }
            telemetryTick = level.getGameTime();
            lastInputMoved = inputMovedThisTick;
            lastOutputMoved = outputMovedThisTick;
            inputMovedThisTick = 0;
            outputMovedThisTick = 0;
        }
    }

    private static final class FluidModuleState {
        private final StallBackoff stall = new StallBackoff();
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
        private final StallBackoff stall = new StallBackoff();
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
            int capacity = 0;
            for (int moduleIndex = 0; moduleIndex < energyModules.length; moduleIndex++) {
                capacity += energyTransferRate(moduleIndex);
            }
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            for (int moduleIndex = 0; moduleIndex < energyModules.length; moduleIndex++) {
                if (energyTransferRate(moduleIndex) > 0 && energyModules[moduleIndex].mode.acceptsNetworkInput()) {
                    return true;
                }
            }
            return false;
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
