package com.rngtech.content.blockentity;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.EvenSplit;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;
import com.rngtech.util.TickTransferCounter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class CableBlockEntity extends BlockEntity implements UniversalConnectorDataOwner {
    public static final int MAX_CHANNEL = 15;

    private static final Direction[] DIRECTIONS = Direction.values();

    private static final int DATA_CHANNEL = 0;
    private static final int DATA_ATTACH_AS = 1;
    private static final int DATA_MODE = 2;
    private static final int DATA_DISTRIBUTION = 3;
    private static final int DATA_TRANSFER_RATE = 4;
    private static final int DATA_ENERGY_TARGET_ACCESS = 5;
    private static final double BUFFER_EQUALIZATION_DEADBAND = 0.01;

    /**
     * Per-level cache of connected cable components.
     *
     * <p>The snapshot is intentionally valid for one game tick only. That keeps topology changes safe without needing
     * every block-neighbour event to reach this class, while still turning many same-tick transfers from
     * "BFS per transfer" into "BFS once per network per tick".</p>
     */
    private static final Map<Level, LevelNetworkCache> NETWORK_CACHES = new WeakHashMap<>();

    private final IEnergyStorage[] sidedEnergyStorages = new IEnergyStorage[DIRECTIONS.length];
    private final EnumSet<Direction> disabledLinks = EnumSet.noneOf(Direction.class);
    private final EnumMap<Direction, ConnectorData> connectors = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, CableUniversalConnectorData> universalConnectors = new EnumMap<>(Direction.class);
    private final TickTransferCounter[] directEnergyInput = new TickTransferCounter[DIRECTIONS.length];
    private final TickTransferCounter[] directEnergyOutput = new TickTransferCounter[DIRECTIONS.length];
    private DyeColor color;

    public CableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CABLE.get(), pos, blockState);
        for (Direction direction : DIRECTIONS) {
            sidedEnergyStorages[direction.ordinal()] = new CableEnergyStorage(direction);
            directEnergyInput[direction.ordinal()] = new TickTransferCounter();
            directEnergyOutput[direction.ordinal()] = new TickTransferCounter();
        }
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        if (side == null) {
            return null;
        }
        CableUniversalConnectorData universalConnector = universalConnectors.get(side);
        if (universalConnector != null) {
            return universalConnector.getEnergyStorage(side);
        }
        ConnectorData connector = connectors.get(side);
        return connector != null && connector.transferRate() > 0 ? sidedEnergyStorages[side.ordinal()] : null;
    }

    public IItemHandler getItemHandler(Direction side) {
        CableUniversalConnectorData universalConnector = side == null ? null : universalConnectors.get(side);
        return universalConnector == null ? null : universalConnector.getItemHandler(side);
    }

    public IFluidHandler getFluidHandler(Direction side) {
        CableUniversalConnectorData universalConnector = side == null ? null : universalConnectors.get(side);
        return universalConnector == null ? null : universalConnector.getFluidHandler(side);
    }

    public boolean isLinkDisabled(Direction direction) {
        return disabledLinks.contains(direction);
    }

    public void setLinkDisabled(Direction direction, boolean disabled) {
        boolean changed = disabled ? disabledLinks.add(direction) : disabledLinks.remove(direction);
        if (changed) {
            setChanged();
            invalidateNetworkCache();
        }
    }

    /** Dyed cables only link to uncoloured cables and cables of the same colour; null means uncoloured. */
    public DyeColor color() {
        return color;
    }

    public void setColor(DyeColor color) {
        if (this.color == color) {
            return;
        }
        this.color = color;
        setChanged();
        invalidateNetworkCache();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static boolean colorsLink(DyeColor first, DyeColor second) {
        return first == null || second == null || first == second;
    }

    public boolean hasConnector(Direction direction) {
        return connectors.containsKey(direction);
    }

    public boolean hasUniversalConnector(Direction direction) {
        return universalConnectors.containsKey(direction);
    }

    public boolean hasAnyConnector(Direction direction) {
        return hasConnector(direction) || hasUniversalConnector(direction);
    }

    public ConnectorData connector(Direction direction) {
        return connectors.get(direction);
    }

    public ItemStack removeConnector(Direction direction) {
        ConnectorData removed = connectors.remove(direction);
        if (removed == null) {
            return ItemStack.EMPTY;
        }
        setChanged();
        invalidateCableCapabilities();
        invalidateNetworkCache();
        return connectorStack(removed.tier());
    }

    public boolean installUniversalConnector(Direction direction) {
        if (hasAnyConnector(direction)) {
            return false;
        }
        universalConnectors.put(direction, new CableUniversalConnectorData(this, direction));
        setChanged();
        invalidateCableCapabilities();
        invalidateNetworkCache();
        return true;
    }

    public boolean installUniversalConnector(Direction direction, CompoundTag connectorTag, HolderLookup.Provider registries) {
        if (hasAnyConnector(direction)) {
            return false;
        }
        CableUniversalConnectorData connector = new CableUniversalConnectorData(this, direction);
        connector.load(connectorTag, registries);
        universalConnectors.put(direction, connector);
        setChanged();
        invalidateCableCapabilities();
        invalidateNetworkCache();
        return true;
    }

    public ItemStack removeUniversalConnector(Direction direction) {
        CableUniversalConnectorData removed = universalConnectors.remove(direction);
        if (removed == null) {
            return ItemStack.EMPTY;
        }
        if (level != null && !level.isClientSide) {
            removed.dropInventory(level);
        }
        setChanged();
        invalidateCableCapabilities();
        invalidateNetworkCache();
        return new ItemStack(ModItems.UNIVERSAL_CONNECTOR.get());
    }

    public UniversalConnectorAccess universalConnectorAccess(Direction direction) {
        return universalConnectors.get(direction);
    }

    public CableUniversalConnectorData universalConnector(Direction direction) {
        return universalConnectors.get(direction);
    }

    public void universalConnectorStateChanged() {
        setChanged();
        invalidateCableCapabilities();
        invalidateNetworkCache();
    }

    public void connectorTargetChanged() {
        invalidateCableCapabilities();
        invalidateNetworkCache();
    }

    public void clearNetworkCache() {
        invalidateNetworkCache();
    }

    @Override
    public void clearConnectorNetworkCache() {
        clearNetworkCache();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CableBlockEntity cable) {
        cable.pullEnergyFromDirectConnectors();
        for (CableUniversalConnectorData connector : cable.universalConnectors.values()) {
            connector.serverTick();
        }
    }

    public void dropConnectors(Level level) {
        for (Direction direction : DIRECTIONS) {
            ItemStack stack = removeConnector(direction);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
            ItemStack universalStack = removeUniversalConnector(direction);
            if (!universalStack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), universalStack);
            }
        }
    }

    public ContainerData connectorMenuData(Direction face) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                ConnectorData connector = connectors.get(face);
                if (connector == null) {
                    return 0;
                }
                return switch (index) {
                    case DATA_CHANNEL -> connector.channel();
                    case DATA_ATTACH_AS -> connector.attachAs().ordinal();
                    case DATA_MODE -> connector.mode().ordinal();
                    case DATA_DISTRIBUTION -> connector.distributionMode().ordinal();
                    case DATA_TRANSFER_RATE -> connector.transferRate();
                    case DATA_ENERGY_TARGET_ACCESS -> directTargetHasEnergyAccess(face) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return dataCount();
            }
        };
    }

    public boolean incrementChannel(Direction face, int amount) {
        ConnectorData connector = connectors.get(face);
        if (connector == null) {
            return false;
        }

        int next = Math.floorMod(connector.channel() + amount, MAX_CHANNEL + 1);
        if (connector.channel() == next) {
            return false;
        }

        connector.setChannel(next);
        setChanged();
        invalidateNetworkCache();
        return true;
    }

    public boolean setChannel(Direction face, int channel) {
        ConnectorData connector = connectors.get(face);
        if (connector == null) {
            return false;
        }

        int next = Math.max(0, Math.min(MAX_CHANNEL, channel));
        if (connector.channel() == next) {
            return false;
        }

        connector.setChannel(next);
        setChanged();
        invalidateNetworkCache();
        return true;
    }

    public boolean setAttachAs(Direction face, Direction attachAs) {
        ConnectorData connector = connectors.get(face);
        if (connector == null || attachAs == null || connector.attachAs() == attachAs) {
            return false;
        }

        connector.setAttachAs(attachAs);
        setChanged();
        invalidateCableCapabilities();
        invalidateNetworkCache();
        return true;
    }

    public boolean cycleMode(Direction face) {
        ConnectorData connector = connectors.get(face);
        if (connector == null) {
            return false;
        }

        CableConnectorMode next = connector.mode().next();
        if (connector.mode() == next) {
            return false;
        }

        connector.setMode(next);
        setChanged();
        invalidateCableCapabilities();
        invalidateNetworkCache();
        return true;
    }

    public boolean setMode(Direction face, CableConnectorMode mode) {
        ConnectorData connector = connectors.get(face);
        if (connector == null || mode == null || connector.mode() == mode) {
            return false;
        }

        connector.setMode(mode);
        setChanged();
        invalidateCableCapabilities();
        invalidateNetworkCache();
        return true;
    }

    public boolean cycleDistributionMode(Direction face) {
        ConnectorData connector = connectors.get(face);
        if (connector == null) {
            return false;
        }

        EnergyDistributionMode next = connector.distributionMode().next();
        if (connector.distributionMode() == next) {
            return false;
        }

        connector.setDistributionMode(next);
        setChanged();
        invalidateNetworkCache();
        return true;
    }

    public boolean setDistributionMode(Direction face, EnergyDistributionMode distributionMode) {
        ConnectorData connector = connectors.get(face);
        if (connector == null || distributionMode == null || connector.distributionMode() == distributionMode) {
            return false;
        }

        connector.setDistributionMode(distributionMode);
        setChanged();
        invalidateNetworkCache();
        return true;
    }

    private void invalidateCableCapabilities() {
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    private void invalidateNetworkCache() {
        if (level != null) {
            invalidateNetworkCache(level, worldPosition);
        }
    }

    public static void invalidateNetworkCache(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }

        LevelNetworkCache cache;
        synchronized (NETWORK_CACHES) {
            cache = NETWORK_CACHES.get(level);
        }

        if (cache != null) {
            cache.invalidate(pos);
        }
    }

    private int receiveFromSide(Direction entrySide, int amount, boolean simulate) {
        if (level == null || amount <= 0) {
            return 0;
        }

        ConnectorData source = connectors.get(entrySide);
        if (source == null || !source.mode().acceptsNetworkInput()) {
            return 0;
        }

        int transferable = Math.min(remainingDirectEnergyInput(entrySide, source.transferRate()), amount);
        if (transferable <= 0) {
            return 0;
        }

        BlockPos sourcePos = worldPosition.relative(entrySide);
        TransferOrigin sourceOrigin = directSourceOrigin(sourcePos, source);
        int moved = distributeEnergy(
                level,
                worldPosition,
                sourceOrigin,
                source.channel(),
                transferable,
                simulate,
                source.distributionMode()
        );
        if (!simulate) {
            directEnergyInput[entrySide.ordinal()].add(level.getGameTime(), moved);
        }
        return moved;
    }

    private boolean pullEnergyFromDirectConnectors() {
        if (level == null || level.isClientSide) {
            return false;
        }

        boolean moved = false;
        for (Direction direction : DIRECTIONS) {
            ConnectorData connector = connectors.get(direction);
            if (connector == null || !connector.mode().acceptsNetworkInput()) {
                continue;
            }
            moved |= pullEnergyFromDirectConnector(direction, connector);
        }
        return moved;
    }

    private boolean pullEnergyFromDirectConnector(Direction face, ConnectorData connector) {
        if (level == null) {
            return false;
        }

        int request = remainingDirectEnergyInput(face, connector.transferRate());
        if (request <= 0) {
            return false;
        }

        BlockPos sourcePos = worldPosition.relative(face);
        if (!level.isLoaded(sourcePos)) {
            return false;
        }
        BlockState sourceState = level.getBlockState(sourcePos);
        if (isNetworkNode(sourceState)) {
            return false;
        }

        IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, sourcePos, connector.attachAs());
        if (source == null || !source.canExtract()) {
            return false;
        }

        int extractable = source.extractEnergy(request, true);
        if (extractable <= 0) {
            return false;
        }

        TransferOrigin sourceOrigin = TransferOrigin.endpoint(sourcePos, connector.attachAs())
                .withEnergyBuffer(connector.mode() == CableConnectorMode.BOTH ? source : null);
        int accepted = distributeEnergy(
                level,
                worldPosition,
                sourceOrigin,
                connector.channel(),
                extractable,
                true,
                connector.distributionMode()
        );
        if (accepted <= 0) {
            return false;
        }

        int extracted = source.extractEnergy(accepted, false);
        if (extracted <= 0) {
            return false;
        }

        int moved = distributeEnergy(
                level,
                worldPosition,
                sourceOrigin,
                connector.channel(),
                extracted,
                false,
                connector.distributionMode()
        );
        directEnergyInput[face.ordinal()].add(level.getGameTime(), moved);
        return moved > 0;
    }

    private TransferOrigin directSourceOrigin(BlockPos sourcePos, ConnectorData connector) {
        TransferOrigin origin = TransferOrigin.endpoint(sourcePos, connector.attachAs());
        if (level == null || connector.mode() != CableConnectorMode.BOTH || !level.isLoaded(sourcePos)) {
            return origin;
        }
        IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, sourcePos, connector.attachAs());
        return origin.withEnergyBuffer(source);
    }

    private int sendToDirectTarget(Direction face, int amount, boolean simulate) {
        ConnectorData connector = connectors.get(face);
        if (level == null || amount <= 0 || connector == null || !connector.mode().sendsNetworkOutput()) {
            return 0;
        }

        int transferable = Math.min(amount, remainingDirectEnergyOutput(face, connector.transferRate()));
        if (transferable <= 0) {
            return 0;
        }

        BlockPos targetPos = worldPosition.relative(face);
        if (!level.isLoaded(targetPos)) {
            return 0;
        }
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof CableBlock || targetState.getBlock() instanceof UniversalConnectorBlock) {
            return 0;
        }

        IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, connector.attachAs());
        if (target == null || !target.canReceive()) {
            return 0;
        }

        int accepted = target.receiveEnergy(transferable, simulate);
        if (!simulate) {
            directEnergyOutput[face.ordinal()].add(level.getGameTime(), accepted);
        }
        return accepted;
    }

    private boolean directTargetHasEnergyAccess(Direction face) {
        return directTargetEnergy(face) != null;
    }

    private IEnergyStorage directTargetEnergy(Direction face) {
        ConnectorData connector = connectors.get(face);
        if (level == null || connector == null) {
            return null;
        }
        BlockPos targetPos = worldPosition.relative(face);
        if (!level.isLoaded(targetPos)) {
            return null;
        }
        BlockState targetState = level.getBlockState(targetPos);
        if (isNetworkNode(targetState) || targetState.getBlock() instanceof UniversalConnectorBlock) {
            return null;
        }
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, connector.attachAs());
    }

    private IEnergyStorage directTargetEnergyBuffer(Direction face) {
        ConnectorData connector = connectors.get(face);
        BlockPos targetPos = worldPosition.relative(face);
        if (level == null || connector == null || !level.isLoaded(targetPos)) {
            return null;
        }
        IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, connector.attachAs());
        return isEnergyBuffer(target) ? target : null;
    }

    private int remainingDirectEnergyInput(Direction direction, int transferRate) {
        return level == null ? 0 : directEnergyInput[direction.ordinal()].remaining(level.getGameTime(), transferRate);
    }

    private int remainingDirectEnergyOutput(Direction direction, int transferRate) {
        return level == null ? 0 : directEnergyOutput[direction.ordinal()].remaining(level.getGameTime(), transferRate);
    }

    public int lastDirectEnergyInput(Direction face) {
        return level == null || face == null ? 0 : directEnergyInput[face.ordinal()].lastTick(level.getGameTime());
    }

    public int lastDirectEnergyOutput(Direction face) {
        return level == null || face == null ? 0 : directEnergyOutput[face.ordinal()].lastTick(level.getGameTime());
    }

    /**
     * True for blocks that both accept and supply FE right now, such as Battery Chassis. Two such blocks behind
     * {@link CableConnectorMode#BOTH} connectors would otherwise trade FE back and forth every tick and lose it to
     * charge/discharge efficiency, so FE only moves between them from the fuller one to the emptier one.
     */
    static boolean isEnergyBuffer(IEnergyStorage storage) {
        return storage != null && storage.canReceive() && storage.canExtract();
    }

    /**
     * FE a fuller buffer may send to an emptier one so both end at the same fill fraction. Below a 1% fill gap it
     * sends nothing, so charge/discharge losses cannot make the two trade small amounts back and forth.
     */
    public static int bufferEqualizationLimit(long sourceStored, long sourceCapacity, long targetStored, long targetCapacity) {
        if (sourceCapacity <= 0 || targetCapacity <= 0 || sourceStored <= 0) {
            return 0;
        }
        double gap = (double) sourceStored / sourceCapacity - (double) targetStored / targetCapacity;
        if (gap <= BUFFER_EQUALIZATION_DEADBAND) {
            return 0;
        }
        long excess = sourceStored * targetCapacity - targetStored * sourceCapacity;
        long limit = excess / (sourceCapacity + targetCapacity);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, limit));
    }

    /**
     * Identifies the exact endpoint and external energy block that started a network transfer.
     *
     * <p>The old transfer code excluded only by {@link BlockPos}. That caused a whole adjacent connector/block to be
     * skipped when one of its faces inserted into the network. With multiple logical inputs/outputs on the same block,
     * that prevented the other faces from participating in the same tick. This origin excludes only the source endpoint:
     * position + side. Energy transfer additionally skips endpoints that target the same external source block, so a
     * battery bypass cannot route charge back into the same storage through another connector face. If {@code side} is
     * {@code null}, the origin intentionally falls back to whole-block exclusion for legacy callers where the source
     * side cannot be known.</p>
     *
     * <p>{@code energyBuffer} is the fill level of a storage block that FE is drawn from through a
     * {@link CableConnectorMode#BOTH} connector, or {@code null}. Another storage block behind a {@code BOTH}
     * connector only receives that FE while it is emptier, and only up to an even fill, so buffers on one network share
     * charge instead of draining into each other.</p>
     */
    public record TransferOrigin(BlockPos pos, Direction side, BlockPos energyTargetPos, EnergyBufferLevel energyBuffer) {
        public static final TransferOrigin NONE = new TransferOrigin(null, null, null, null);

        public static TransferOrigin wholeBlock(BlockPos pos) {
            return pos == null ? NONE : new TransferOrigin(pos, null, pos, null);
        }

        public static TransferOrigin endpoint(BlockPos pos, Direction side) {
            return endpoint(pos, side, pos);
        }

        public static TransferOrigin endpoint(BlockPos pos, Direction side, BlockPos energyTargetPos) {
            return pos == null ? NONE : new TransferOrigin(pos, side, energyTargetPos, null);
        }

        /** Marks the origin as a storage block behind a Both connector; a null or non-storage source clears it. */
        public TransferOrigin withEnergyBuffer(IEnergyStorage source) {
            if (this == NONE) {
                return NONE;
            }
            EnergyBufferLevel buffer = isEnergyBuffer(source)
                    ? new EnergyBufferLevel(source.getEnergyStored(), source.getMaxEnergyStored())
                    : null;
            return new TransferOrigin(pos, side, energyTargetPos, buffer);
        }

        public static TransferOrigin inferAdjacentEndpoint(BlockPos startPos, BlockPos sourcePos) {
            if (sourcePos == null) {
                return NONE;
            }

            if (startPos != null) {
                for (Direction direction : DIRECTIONS) {
                    if (startPos.relative(direction).equals(sourcePos)) {
                        return endpoint(sourcePos, direction.getOpposite());
                    }
                }
            }

            return wholeBlock(sourcePos);
        }

        public boolean matches(BlockPos targetPos, Direction targetSide) {
            if (pos == null || targetPos == null || !pos.equals(targetPos)) {
                return false;
            }
            return side == null || side == targetSide;
        }

        public boolean matchesEnergyTarget(BlockPos targetPos) {
            return energyTargetPos != null && targetPos != null && energyTargetPos.equals(targetPos);
        }
    }

    public static int distributeEnergy(
            Level level,
            BlockPos startPos,
            BlockPos sourcePos,
            int channel,
            int amount,
            boolean simulate
    ) {
        return distributeEnergy(
                level,
                startPos,
                TransferOrigin.inferAdjacentEndpoint(startPos, sourcePos),
                channel,
                amount,
                simulate,
                EnergyDistributionMode.ROUND_ROBIN
        );
    }

    public static int distributeEnergy(
            Level level,
            BlockPos startPos,
            BlockPos sourcePos,
            Direction sourceSide,
            int channel,
            int amount,
            boolean simulate
    ) {
        return distributeEnergy(level, startPos, sourcePos, sourceSide, channel, amount, simulate,
                EnergyDistributionMode.ROUND_ROBIN);
    }

    public static int distributeEnergy(
            Level level,
            BlockPos startPos,
            BlockPos sourcePos,
            Direction sourceSide,
            int channel,
            int amount,
            boolean simulate,
            EnergyDistributionMode distributionMode
    ) {
        return distributeEnergy(
                level,
                startPos,
                TransferOrigin.endpoint(sourcePos, sourceSide),
                channel,
                amount,
                simulate,
                distributionMode
        );
    }

    public static int distributeEnergy(
            Level level,
            BlockPos startPos,
            TransferOrigin source,
            int channel,
            int amount,
            boolean simulate
    ) {
        return distributeEnergy(level, startPos, source, channel, amount, simulate, EnergyDistributionMode.ROUND_ROBIN);
    }

    public static int distributeEnergy(
            Level level,
            BlockPos startPos,
            TransferOrigin source,
            int channel,
            int amount,
            boolean simulate,
            EnergyDistributionMode distributionMode
    ) {
        if (level == null || startPos == null || amount <= 0 || !isValidChannel(channel)) {
            return 0;
        }

        if (source == null) {
            source = TransferOrigin.NONE;
        }
        if (distributionMode == null) {
            distributionMode = EnergyDistributionMode.ROUND_ROBIN;
        }

        NetworkSnapshot snapshot = networkSnapshot(level, startPos);
        List<EnergyEndpoint> endpoints = snapshot.energyEndpoints(channel);
        if (endpoints.isEmpty()) {
            return 0;
        }

        if (distributionMode == EnergyDistributionMode.EVEN) {
            return distributeEnergyEven(level, snapshot, source, channel, amount, simulate, endpoints);
        }

        int moved = 0;
        int size = endpoints.size();
        int startIndex = distributionMode == EnergyDistributionMode.ROUND_ROBIN ? snapshot.energyCursor(channel, size) : 0;
        int lastVisitedIndex = -1;

        for (int i = 0; i < size && moved < amount; i++) {
            int index = (startIndex + i) % size;
            lastVisitedIndex = index;

            EnergyEndpoint endpoint = endpoints.get(index);
            if (endpoint.isExcludedBy(level, source)) {
                continue;
            }

            int request = Math.min(
                    Math.min(amount - moved, endpoint.transferRate()),
                    endpoint.bufferLimit(level, source, moved)
            );
            if (request <= 0) {
                continue;
            }

            int accepted = endpoint.receive(level, channel, request, simulate);
            if (!simulate) {
                snapshot.recordEnergyOutput(channel, accepted, level.getGameTime());
            }
            moved += accepted;
        }

        if (!simulate && distributionMode == EnergyDistributionMode.ROUND_ROBIN && lastVisitedIndex >= 0) {
            snapshot.setEnergyCursor(channel, (lastVisitedIndex + 1) % size);
        }
        if (!simulate) {
            snapshot.recordEnergyInput(channel, moved, level.getGameTime());
        }

        return moved;
    }

    private static int distributeEnergyEven(
            Level level,
            NetworkSnapshot snapshot,
            TransferOrigin source,
            int channel,
            int amount,
            boolean simulate,
            List<EnergyEndpoint> endpoints
    ) {
        List<EnergyEndpoint> eligible = new ArrayList<>();
        for (EnergyEndpoint endpoint : endpoints) {
            if (!endpoint.isExcludedBy(level, source) && endpoint.transferRate() > 0) {
                eligible.add(endpoint);
            }
        }
        if (eligible.isEmpty()) {
            return 0;
        }

        int moved = 0;
        int size = eligible.size();
        int[] delivered = new int[size];
        int baseShare = amount / size;
        int extra = amount % size;

        for (int index = 0; index < size && moved < amount; index++) {
            EnergyEndpoint endpoint = eligible.get(index);
            int share = baseShare + (index < extra ? 1 : 0);
            int request = Math.min(
                    Math.min(Math.min(share, endpoint.transferRate()), amount - moved),
                    endpoint.bufferLimit(level, source, moved)
            );
            if (request <= 0) {
                continue;
            }

            int accepted = endpoint.receive(level, channel, request, simulate);
            if (!simulate) {
                snapshot.recordEnergyOutput(channel, accepted, level.getGameTime());
            }
            delivered[index] = accepted;
            moved += accepted;
        }

        for (int index = 0; index < size && moved < amount; index++) {
            EnergyEndpoint endpoint = eligible.get(index);
            int remainingEndpointTransfer = endpoint.transferRate() - delivered[index];
            int request = Math.min(
                    Math.min(Math.min(amount - moved, remainingEndpointTransfer), endpoint.transferRate()),
                    endpoint.bufferLimit(level, source, moved)
            );
            if (request <= 0) {
                continue;
            }

            int accepted = endpoint.receive(level, channel, request, simulate);
            if (!simulate) {
                snapshot.recordEnergyOutput(channel, accepted, level.getGameTime());
            }
            delivered[index] += accepted;
            moved += accepted;
        }
        if (!simulate) {
            snapshot.recordEnergyInput(channel, moved, level.getGameTime());
        }

        return moved;
    }

    public static ItemStack distributeItems(
            Level level,
            BlockPos startPos,
            BlockPos sourcePos,
            int channel,
            ItemStack stack,
            boolean simulate
    ) {
        return distributeItems(
                level,
                startPos,
                TransferOrigin.inferAdjacentEndpoint(startPos, sourcePos),
                channel,
                stack,
                simulate
        );
    }

    public static ItemStack distributeItems(
            Level level,
            BlockPos startPos,
            BlockPos sourcePos,
            Direction sourceSide,
            int channel,
            ItemStack stack,
            boolean simulate
    ) {
        return distributeItems(
                level,
                startPos,
                TransferOrigin.endpoint(sourcePos, sourceSide),
                channel,
                stack,
                simulate
        );
    }

    public static ItemStack distributeItems(
            Level level,
            BlockPos startPos,
            TransferOrigin source,
            int channel,
            ItemStack stack,
            boolean simulate
    ) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (level == null || startPos == null || !isValidChannel(channel)) {
            return stack.copy();
        }

        if (source == null) {
            source = TransferOrigin.NONE;
        }

        NetworkSnapshot snapshot = networkSnapshot(level, startPos);
        List<UniversalEndpoint> endpoints = snapshot.universalEndpoints();
        if (endpoints.isEmpty()) {
            return stack.copy();
        }

        TransferOrigin origin = source;
        EvenSplit.Result result = EvenSplit.distribute(
                endpoints.size(),
                snapshot.itemCursor(channel, endpoints.size()),
                index -> origin.matches(endpoints.get(index).pos(), endpoints.get(index).side()),
                stack.getCount(),
                (index, amount, simulateShare) -> amount - endpoints.get(index)
                        .receiveItem(level, channel, stack.copyWithCount(amount), simulateShare)
                        .getCount(),
                simulate
        );

        if (!simulate) {
            snapshot.setItemCursor(channel, result.nextCursor());
        }

        int remaining = stack.getCount() - result.moved();
        return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    public static FluidStack distributeFluids(
            Level level,
            BlockPos startPos,
            BlockPos sourcePos,
            int channel,
            FluidStack stack,
            boolean simulate
    ) {
        return distributeFluids(
                level,
                startPos,
                TransferOrigin.inferAdjacentEndpoint(startPos, sourcePos),
                channel,
                stack,
                simulate
        );
    }

    public static FluidStack distributeFluids(
            Level level,
            BlockPos startPos,
            BlockPos sourcePos,
            Direction sourceSide,
            int channel,
            FluidStack stack,
            boolean simulate
    ) {
        return distributeFluids(
                level,
                startPos,
                TransferOrigin.endpoint(sourcePos, sourceSide),
                channel,
                stack,
                simulate
        );
    }

    public static FluidStack distributeFluids(
            Level level,
            BlockPos startPos,
            TransferOrigin source,
            int channel,
            FluidStack stack,
            boolean simulate
    ) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }

        if (level == null || startPos == null || !isValidChannel(channel)) {
            return stack.copy();
        }

        if (source == null) {
            source = TransferOrigin.NONE;
        }

        NetworkSnapshot snapshot = networkSnapshot(level, startPos);
        List<UniversalEndpoint> endpoints = snapshot.universalEndpoints();
        if (endpoints.isEmpty()) {
            return stack.copy();
        }

        TransferOrigin origin = source;
        EvenSplit.Result result = EvenSplit.distribute(
                endpoints.size(),
                snapshot.fluidCursor(channel, endpoints.size()),
                index -> origin.matches(endpoints.get(index).pos(), endpoints.get(index).side()),
                stack.getAmount(),
                (index, amount, simulateShare) -> amount - endpoints.get(index)
                        .receiveFluid(level, channel, stack.copyWithAmount(amount), simulateShare)
                        .getAmount(),
                simulate
        );

        if (!simulate) {
            snapshot.setFluidCursor(channel, result.nextCursor());
        }

        int remaining = stack.getAmount() - result.moved();
        return remaining <= 0 ? FluidStack.EMPTY : stack.copyWithAmount(remaining);
    }

    private static boolean isValidChannel(int channel) {
        return channel >= 0 && channel <= MAX_CHANNEL;
    }

    private static NetworkSnapshot networkSnapshot(Level level, BlockPos startPos) {
        LevelNetworkCache cache;
        synchronized (NETWORK_CACHES) {
            cache = NETWORK_CACHES.computeIfAbsent(level, ignored -> new LevelNetworkCache());
        }
        return cache.getOrBuild(level, startPos);
    }

    public static NetworkDebugSnapshot networkDebugSnapshot(Level level, BlockPos startPos) {
        if (level == null || startPos == null) {
            return NetworkDebugSnapshot.EMPTY;
        }

        NetworkSnapshot snapshot = networkSnapshot(level, startPos);
        return snapshot.debugSnapshot(level);
    }

    public static int bridgeEndpointCount(
            Level level,
            BlockPos startPos,
            NetworkBridgeType bridgeType,
            int channel
    ) {
        if (level == null
                || startPos == null
                || bridgeType == null
                || bridgeType == NetworkBridgeType.NONE
                || !isValidChannel(channel)) {
            return 0;
        }

        int count = 0;
        for (BridgeEndpoint endpoint : networkSnapshot(level, startPos).bridgeEndpoints()) {
            if (endpoint.bridgeType() == bridgeType && endpoint.channel() == channel) {
                count++;
            }
        }
        return count;
    }

    public static List<BridgeEndpoint> bridgeEndpoints(
            Level level,
            BlockPos startPos,
            NetworkBridgeType bridgeType,
            int channel
    ) {
        if (level == null
                || startPos == null
                || bridgeType == null
                || bridgeType == NetworkBridgeType.NONE
                || !isValidChannel(channel)) {
            return Collections.emptyList();
        }

        List<BridgeEndpoint> endpoints = new ArrayList<>();
        for (BridgeEndpoint endpoint : networkSnapshot(level, startPos).bridgeEndpoints()) {
            if (endpoint.bridgeType() == bridgeType && endpoint.channel() == channel) {
                endpoints.add(endpoint);
            }
        }
        return immutableList(endpoints);
    }

    public static NetworkEnergyTelemetrySnapshot networkEnergyTelemetrySnapshot(
            Level level,
            BlockPos startPos,
            int channel
    ) {
        if (level == null || startPos == null || !isValidChannel(channel)) {
            return NetworkEnergyTelemetrySnapshot.EMPTY;
        }

        NetworkSnapshot snapshot = networkSnapshot(level, startPos);
        return snapshot.energyTelemetrySnapshot(channel, level.getGameTime());
    }

    private static NetworkDebugSnapshot buildNetworkDebugSnapshot(Level level, NetworkSnapshot snapshot) {
        NetworkDebugAccumulator accumulator = new NetworkDebugAccumulator(cacheAgeTicks(level, snapshot));
        for (BlockPos pos : snapshot.cablePositions()) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
                accumulator.addCableNode();
                for (Map.Entry<Direction, ConnectorData> entry : cable.connectors.entrySet()) {
                    ConnectorData connector = entry.getValue();
                    accumulator.addEnergyModule(connector.channel());
                    accumulator.addEnergyConnector(
                            connector.channel(),
                            connector.mode(),
                            connector.transferRate(),
                            cable.directTargetEnergy(entry.getKey()),
                            cable.lastDirectEnergyInput(entry.getKey()),
                            cable.lastDirectEnergyOutput(entry.getKey())
                    );
                }
                for (CableUniversalConnectorData connector : cable.universalConnectors.values()) {
                    accumulator.addUniversalConnector();
                    connector.addNetworkDebugStats(accumulator);
                }
            }
        }

        return accumulator.snapshot();
    }

    private static int cacheAgeTicks(Level level, NetworkSnapshot snapshot) {
        long age = Math.max(0L, level.getGameTime() - snapshot.gameTime);
        return NetworkDebugAccumulator.saturatedInt(age);
    }

    private static NetworkSnapshot buildNetworkSnapshot(
            Level level,
            BlockPos startPos,
            long gameTime,
            LevelNetworkCache cache
    ) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> networkPositions = new ArrayList<>();
        List<UniversalEndpoint> universalEndpoints = new ArrayList<>();
        Set<UniversalEndpoint> seenUniversalEndpoints = new HashSet<>();
        List<BridgeEndpoint> bridgeEndpoints = new ArrayList<>();
        Set<BridgeEndpoint> seenBridgeEndpoints = new HashSet<>();
        List<EnergyEndpoint>[] energyEndpoints = createEnergyEndpointBuckets();

        if (level.isLoaded(startPos)) {
            queue.addLast(startPos);
            visited.add(startPos);
        }

        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof CableBlock) {
                networkPositions.add(pos);
                collectCableNode(
                        level,
                        pos,
                        state,
                        queue,
                        visited,
                        universalEndpoints,
                        seenUniversalEndpoints,
                        bridgeEndpoints,
                        seenBridgeEndpoints,
                        energyEndpoints
                );
            }
        }

        List<BlockPos> cablePositions = immutableList(networkPositions);
        return new NetworkSnapshot(
                gameTime,
                cablePositions,
                immutableEnergyBuckets(energyEndpoints),
                immutableList(sortedUniversalEndpoints(universalEndpoints)),
                immutableList(bridgeEndpoints),
                cache.claimState(cablePositions, gameTime)
        );
    }

    /** Orders endpoints by position so routing cursors mean the same endpoint whichever cable rebuilt the network. */
    private static List<UniversalEndpoint> sortedUniversalEndpoints(List<UniversalEndpoint> endpoints) {
        endpoints.sort(Comparator.comparing(UniversalEndpoint::pos).thenComparing(UniversalEndpoint::side));
        return endpoints;
    }

    private static void collectCableNode(
            Level level,
            BlockPos pos,
            BlockState state,
            ArrayDeque<BlockPos> queue,
            Set<BlockPos> visited,
            List<UniversalEndpoint> universalEndpoints,
            Set<UniversalEndpoint> seenUniversalEndpoints,
            List<BridgeEndpoint> bridgeEndpoints,
            Set<BridgeEndpoint> seenBridgeEndpoints,
            List<EnergyEndpoint>[] energyEndpoints
    ) {
        CableBlockEntity cable = level.getBlockEntity(pos) instanceof CableBlockEntity c ? c : null;

        for (Direction direction : DIRECTIONS) {
            BlockPos targetPos = pos.relative(direction);
            if (!level.isLoaded(targetPos)) {
                // An unloaded chunk is the edge of the network until it loads again; reading it would load it.
                continue;
            }
            BlockState targetState = null;

            if (CableBlock.hasCableConnection(state, direction)) {
                targetState = level.getBlockState(targetPos);
                if (canEnterNetworkNode(targetState, direction.getOpposite())) {
                    addNetworkNode(targetPos, targetState, queue, visited);
                }
            }

            if (cable == null) {
                continue;
            }

            CableUniversalConnectorData universalConnector = cable.universalConnector(direction);
            if (universalConnector != null) {
                if (targetState == null) {
                    targetState = level.getBlockState(targetPos);
                }
                addCableUniversalEndpoint(
                        universalEndpoints,
                        seenUniversalEndpoints,
                        bridgeEndpoints,
                        seenBridgeEndpoints,
                        energyEndpoints,
                        universalConnector,
                        pos,
                        direction,
                        targetState
                );
            }

            ConnectorData connector = cable.connector(direction);
            if (connector == null || !connector.mode().sendsNetworkOutput()) {
                continue;
            }

            if (targetState == null) {
                targetState = level.getBlockState(targetPos);
            }

            if (!CableBlock.isValidConnectorTarget(targetState) || isNetworkNode(targetState)) {
                continue;
            }

            energyEndpoints[connector.channel()].add(EnergyEndpoint.direct(
                    pos,
                    direction,
                    connector.attachAs(),
                    connector.transferRate(),
                    connector.mode() == CableConnectorMode.BOTH
            ));
        }
    }

    private static void addNetworkNode(BlockPos targetPos, BlockState targetState, ArrayDeque<BlockPos> queue, Set<BlockPos> visited) {
        if (isNetworkNode(targetState) && visited.add(targetPos)) {
            queue.addLast(targetPos);
        }
    }

    private static void addCableUniversalEndpoint(
            List<UniversalEndpoint> universalEndpoints,
            Set<UniversalEndpoint> seenUniversalEndpoints,
            List<BridgeEndpoint> bridgeEndpoints,
            Set<BridgeEndpoint> seenBridgeEndpoints,
            List<EnergyEndpoint>[] energyEndpoints,
            CableUniversalConnectorData connector,
            BlockPos targetPos,
            Direction targetSide,
            BlockState targetState
    ) {
        if (!CableBlock.isValidConnectorTarget(targetState)) {
            return;
        }
        UniversalEndpoint endpoint = new UniversalEndpoint(targetPos, targetSide, UniversalEndpointType.CABLE_SIDE);
        if (seenUniversalEndpoints.add(endpoint)) {
            universalEndpoints.add(endpoint);
            addCableUniversalEnergyEndpoints(energyEndpoints, connector, targetPos, targetSide);
        }
        addCableUniversalBridgeEndpoint(bridgeEndpoints, seenBridgeEndpoints, connector, targetPos, targetSide);
    }

    private static void addCableUniversalBridgeEndpoint(
            List<BridgeEndpoint> bridgeEndpoints,
            Set<BridgeEndpoint> seenBridgeEndpoints,
            CableUniversalConnectorData connector,
            BlockPos targetPos,
            Direction targetSide
    ) {
        NetworkBridgeType bridgeType = connector.bridgeType();
        if (bridgeType == NetworkBridgeType.NONE || !connector.hasActiveBridgeModule()) {
            return;
        }
        BridgeEndpoint endpoint = new BridgeEndpoint(targetPos, targetSide, bridgeType, connector.bridgeChannel());
        if (seenBridgeEndpoints.add(endpoint)) {
            bridgeEndpoints.add(endpoint);
        }
    }

    private static boolean isNetworkNode(BlockState state) {
        return state.getBlock() instanceof CableBlock;
    }

    private static boolean canEnterNetworkNode(BlockState state, Direction side) {
        return state.getBlock() instanceof CableBlock && CableBlock.hasCableConnection(state, side);
    }

    private static void addCableUniversalEnergyEndpoints(
            List<EnergyEndpoint>[] energyEndpoints,
            CableUniversalConnectorData connector,
            BlockPos targetPos,
            Direction targetSide
    ) {
        for (int channel = 0; channel <= MAX_CHANNEL; channel++) {
            int transferRate = connector.transferRateForChannel(channel);
            if (transferRate > 0) {
                energyEndpoints[channel].add(EnergyEndpoint.cableUniversal(
                        targetPos,
                        targetSide,
                        transferRate,
                        connector.mode() == CableConnectorMode.BOTH
                ));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<EnergyEndpoint>[] createEnergyEndpointBuckets() {
        List<EnergyEndpoint>[] buckets = (List<EnergyEndpoint>[]) new List<?>[MAX_CHANNEL + 1];
        for (int channel = 0; channel <= MAX_CHANNEL; channel++) {
            buckets[channel] = new ArrayList<>();
        }
        return buckets;
    }

    private static List<EnergyEndpoint>[] immutableEnergyBuckets(List<EnergyEndpoint>[] buckets) {
        for (int channel = 0; channel <= MAX_CHANNEL; channel++) {
            buckets[channel] = immutableList(buckets[channel]);
        }
        return buckets;
    }

    private static <T> List<T> immutableList(List<T> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    private static final class LevelNetworkCache {
        private static final long PRUNE_INTERVAL_TICKS = 200L;

        private static final long TELEMETRY_RETENTION_TICKS = 16L * 60L * 20L;

        private final Map<BlockPos, NetworkSnapshot> byCablePosition = new HashMap<>();
        private final Map<BlockPos, NetworkState> stateByAnchor = new HashMap<>();
        private long lastPruneGameTime = Long.MIN_VALUE;

        private NetworkSnapshot getOrBuild(Level level, BlockPos startPos) {
            long gameTime = level.getGameTime();
            pruneStaleSnapshots(gameTime);

            NetworkSnapshot snapshot = byCablePosition.get(startPos);
            if (snapshot != null && snapshot.isFresh(gameTime)) {
                return snapshot;
            }

            if (snapshot != null) {
                removeSnapshot(snapshot);
            }

            snapshot = buildNetworkSnapshot(level, startPos, gameTime, this);
            addSnapshot(snapshot);
            return snapshot;
        }

        /**
         * Finds the throughput history and routing cursors for a rebuilt network. State is keyed by the network's
         * lowest cable position, so it survives the per-tick rebuild, cache pruning and invalidation. A merged network
         * keeps the history of every part, and the part of a split network that lost the old key starts fresh.
         */
        private NetworkState claimState(List<BlockPos> cablePositions, long gameTime) {
            if (cablePositions.isEmpty()) {
                return new NetworkState(gameTime);
            }
            BlockPos anchor = Collections.min(cablePositions);
            NetworkState state = stateByAnchor.get(anchor);
            for (BlockPos pos : cablePositions) {
                if (pos.equals(anchor)) {
                    continue;
                }
                NetworkState other = stateByAnchor.remove(pos);
                if (other == null) {
                    continue;
                }
                if (state == null) {
                    state = other;
                } else {
                    state.telemetry.absorb(other.telemetry);
                }
            }
            if (state == null) {
                state = new NetworkState(gameTime);
            }
            state.telemetry.markUsed(gameTime);
            stateByAnchor.put(anchor, state);
            return state;
        }

        private void invalidate(BlockPos pos) {
            NetworkSnapshot snapshot = byCablePosition.remove(pos);
            if (snapshot != null) {
                removeSnapshot(snapshot);
            }
        }

        private void addSnapshot(NetworkSnapshot snapshot) {
            for (BlockPos cablePos : snapshot.cablePositions()) {
                byCablePosition.put(cablePos, snapshot);
            }
        }

        private void removeSnapshot(NetworkSnapshot snapshot) {
            for (BlockPos cablePos : snapshot.cablePositions()) {
                byCablePosition.remove(cablePos, snapshot);
            }
        }

        private void pruneStaleSnapshots(long gameTime) {
            if (lastPruneGameTime != Long.MIN_VALUE && gameTime - lastPruneGameTime < PRUNE_INTERVAL_TICKS) {
                return;
            }

            lastPruneGameTime = gameTime;
            byCablePosition.entrySet().removeIf(entry -> !entry.getValue().isFresh(gameTime));
            stateByAnchor.values().removeIf(state -> state.telemetry.unusedFor(gameTime) > TELEMETRY_RETENTION_TICKS);
        }
    }

    private static final class NetworkState {
        private final NetworkEnergyTelemetry telemetry;
        private final int[] energyCursors = new int[MAX_CHANNEL + 1];
        private final int[] itemCursors = new int[MAX_CHANNEL + 1];
        private final int[] fluidCursors = new int[MAX_CHANNEL + 1];

        private NetworkState(long gameTime) {
            telemetry = new NetworkEnergyTelemetry(gameTime);
        }
    }

    private static final class NetworkSnapshot {
        private final long gameTime;
        private final List<BlockPos> cablePositions;
        private final List<EnergyEndpoint>[] energyEndpoints;
        private final List<UniversalEndpoint> universalEndpoints;
        private final List<BridgeEndpoint> bridgeEndpoints;
        private final NetworkEnergyTelemetry energyTelemetry;
        private final int[] energyCursors;
        private final int[] itemCursors;
        private final int[] fluidCursors;
        private NetworkDebugSnapshot debugSnapshot;

        private NetworkSnapshot(
                long gameTime,
                List<BlockPos> cablePositions,
                List<EnergyEndpoint>[] energyEndpoints,
                List<UniversalEndpoint> universalEndpoints,
                List<BridgeEndpoint> bridgeEndpoints,
                NetworkState state
        ) {
            this.gameTime = gameTime;
            this.cablePositions = cablePositions;
            this.energyEndpoints = energyEndpoints;
            this.universalEndpoints = universalEndpoints;
            this.bridgeEndpoints = bridgeEndpoints;
            this.energyTelemetry = state.telemetry;
            this.energyCursors = state.energyCursors;
            this.itemCursors = state.itemCursors;
            this.fluidCursors = state.fluidCursors;
        }

        private boolean isFresh(long gameTime) {
            return this.gameTime == gameTime;
        }

        private List<BlockPos> cablePositions() {
            return cablePositions;
        }

        private List<EnergyEndpoint> energyEndpoints(int channel) {
            return isValidChannel(channel) ? energyEndpoints[channel] : Collections.emptyList();
        }

        private List<UniversalEndpoint> universalEndpoints() {
            return universalEndpoints;
        }

        private List<BridgeEndpoint> bridgeEndpoints() {
            return bridgeEndpoints;
        }

        private NetworkDebugSnapshot debugSnapshot(Level level) {
            if (debugSnapshot == null) {
                debugSnapshot = buildNetworkDebugSnapshot(level, this);
            }
            return debugSnapshot;
        }

        private void recordEnergyInput(int channel, int amount, long gameTime) {
            energyTelemetry.recordInput(channel, amount, gameTime);
        }

        private void recordEnergyOutput(int channel, int amount, long gameTime) {
            energyTelemetry.recordOutput(channel, amount, gameTime);
        }

        private NetworkEnergyTelemetrySnapshot energyTelemetrySnapshot(int channel, long gameTime) {
            return energyTelemetry.snapshot(channel, gameTime);
        }

        private int energyCursor(int channel, int size) {
            return cursor(energyCursors, channel, size);
        }

        private int itemCursor(int channel, int size) {
            return cursor(itemCursors, channel, size);
        }

        private int fluidCursor(int channel, int size) {
            return cursor(fluidCursors, channel, size);
        }

        private void setEnergyCursor(int channel, int value) {
            setCursor(energyCursors, channel, value);
        }

        private void setItemCursor(int channel, int value) {
            setCursor(itemCursors, channel, value);
        }

        private void setFluidCursor(int channel, int value) {
            setCursor(fluidCursors, channel, value);
        }

        private static int cursor(int[] cursors, int channel, int size) {
            if (!isValidChannel(channel) || size <= 0) {
                return 0;
            }
            return Math.floorMod(cursors[channel], size);
        }

        private static void setCursor(int[] cursors, int channel, int value) {
            if (isValidChannel(channel)) {
                cursors[channel] = value;
            }
        }
    }

    public record NetworkDebugSnapshot(
            int cableNodes,
            int universalConnectors,
            int energyEndpoints,
            int energyModules,
            int fluidModules,
            int itemModules,
            int bridgeModules,
            int ae2BridgeEndpoints,
            int refinedStorageBridgeEndpoints,
            int bridgeChannelsMask,
            int activeChannelsMask,
            int energyChannelsMask,
            int fluidChannelsMask,
            int itemChannelsMask,
            int energyTransferCap,
            int fluidShipmentCap,
            int itemShipmentCap,
            int cacheAgeTicks,
            int energyInputCap,
            int energyOutputCap,
            int[] energyChannelCaps
    ) {
        public static final NetworkDebugSnapshot EMPTY = new NetworkDebugSnapshot(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                new int[MAX_CHANNEL + 1]
        );

        /** Most FE/t the channel can move: the smaller of its supplying and its accepting connector tiers. */
        public int energyChannelCap(int channel) {
            return isValidChannel(channel) ? energyChannelCaps[channel] : 0;
        }
    }

    public record NetworkEnergyTelemetrySnapshot(
            int liveInput,
            int liveOutput,
            int averageInput1m,
            int averageOutput1m,
            int averageInput5m,
            int averageOutput5m,
            int averageInput15m,
            int averageOutput15m
    ) {
        public static final NetworkEnergyTelemetrySnapshot EMPTY = new NetworkEnergyTelemetrySnapshot(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }

    private static final class NetworkEnergyTelemetry {
        private static final int TICKS_PER_SECOND = 20;
        private static final int ONE_MINUTE_SECONDS = 60;
        private static final int FIVE_MINUTES_SECONDS = 5 * ONE_MINUTE_SECONDS;
        private static final int FIFTEEN_MINUTES_SECONDS = 15 * ONE_MINUTE_SECONDS;

        private final ChannelTelemetry[] channels = new ChannelTelemetry[MAX_CHANNEL + 1];
        private long createdTick;
        private long lastUsedTick;

        private NetworkEnergyTelemetry(long gameTime) {
            createdTick = gameTime;
            lastUsedTick = gameTime;
            for (int channel = 0; channel < channels.length; channel++) {
                channels[channel] = new ChannelTelemetry();
            }
        }

        private void markUsed(long gameTime) {
            lastUsedTick = gameTime;
        }

        private long unusedFor(long gameTime) {
            return gameTime - lastUsedTick;
        }

        private void absorb(NetworkEnergyTelemetry other) {
            createdTick = Math.min(createdTick, other.createdTick);
            for (int channel = 0; channel < channels.length; channel++) {
                channels[channel].absorb(other.channels[channel]);
            }
        }

        private void recordInput(int channel, int amount, long gameTime) {
            if (isValidChannel(channel) && amount > 0) {
                channels[channel].recordInput(amount, gameTime);
            }
        }

        private void recordOutput(int channel, int amount, long gameTime) {
            if (isValidChannel(channel) && amount > 0) {
                channels[channel].recordOutput(amount, gameTime);
            }
        }

        private NetworkEnergyTelemetrySnapshot snapshot(int channel, long gameTime) {
            return isValidChannel(channel)
                    ? channels[channel].snapshot(gameTime, createdTick)
                    : NetworkEnergyTelemetrySnapshot.EMPTY;
        }

        private static final class ChannelTelemetry {
            private final SecondBin[] bins = new SecondBin[FIFTEEN_MINUTES_SECONDS];
            private long activeTick = Long.MIN_VALUE;
            private int inputThisTick;
            private int outputThisTick;
            private int liveInput;
            private int liveOutput;

            private ChannelTelemetry() {
                for (int index = 0; index < bins.length; index++) {
                    bins[index] = new SecondBin();
                }
            }

            private void recordInput(int amount, long gameTime) {
                beginTick(gameTime);
                inputThisTick = NetworkDebugAccumulator.saturatedInt((long) inputThisTick + amount);
                bin(gameTime).input += amount;
            }

            private void recordOutput(int amount, long gameTime) {
                beginTick(gameTime);
                outputThisTick = NetworkDebugAccumulator.saturatedInt((long) outputThisTick + amount);
                bin(gameTime).output += amount;
            }

            private NetworkEnergyTelemetrySnapshot snapshot(long gameTime, long createdTick) {
                beginTick(gameTime);
                return new NetworkEnergyTelemetrySnapshot(
                        liveInput,
                        liveOutput,
                        average(gameTime, createdTick, ONE_MINUTE_SECONDS, true),
                        average(gameTime, createdTick, ONE_MINUTE_SECONDS, false),
                        average(gameTime, createdTick, FIVE_MINUTES_SECONDS, true),
                        average(gameTime, createdTick, FIVE_MINUTES_SECONDS, false),
                        average(gameTime, createdTick, FIFTEEN_MINUTES_SECONDS, true),
                        average(gameTime, createdTick, FIFTEEN_MINUTES_SECONDS, false)
                );
            }

            private void absorb(ChannelTelemetry other) {
                for (SecondBin otherBin : other.bins) {
                    if (otherBin.second == Long.MIN_VALUE) {
                        continue;
                    }
                    SecondBin bin = bins[(int) (otherBin.second % bins.length)];
                    if (bin.second == otherBin.second) {
                        bin.input += otherBin.input;
                        bin.output += otherBin.output;
                    } else if (bin.second < otherBin.second) {
                        bin.second = otherBin.second;
                        bin.input = otherBin.input;
                        bin.output = otherBin.output;
                    }
                }
            }

            private void beginTick(long gameTime) {
                if (activeTick == gameTime) {
                    return;
                }

                if (activeTick != Long.MIN_VALUE && gameTime - activeTick == 1L) {
                    liveInput = inputThisTick;
                    liveOutput = outputThisTick;
                } else {
                    liveInput = 0;
                    liveOutput = 0;
                }
                activeTick = gameTime;
                inputThisTick = 0;
                outputThisTick = 0;
            }

            private SecondBin bin(long gameTime) {
                long second = Math.max(0L, gameTime / TICKS_PER_SECOND);
                SecondBin bin = bins[(int) (second % bins.length)];
                if (bin.second != second) {
                    bin.second = second;
                    bin.input = 0L;
                    bin.output = 0L;
                }
                return bin;
            }

            /** Averages over the ticks actually measured, so a fresh network reads its real rate instead of ramping. */
            private int average(long gameTime, long createdTick, int seconds, boolean input) {
                long currentSecond = Math.max(0L, gameTime / TICKS_PER_SECOND);
                long earliestSecond = currentSecond - seconds + 1L;
                long total = 0L;
                for (SecondBin bin : bins) {
                    if (bin.second >= earliestSecond && bin.second <= currentSecond) {
                        total += input ? bin.input : bin.output;
                    }
                }
                long windowStart = Math.max(earliestSecond * TICKS_PER_SECOND, createdTick);
                long measuredTicks = Math.max(1L, gameTime - windowStart + 1L);
                return NetworkDebugAccumulator.saturatedInt(total / measuredTicks);
            }
        }

        private static final class SecondBin {
            private long second = Long.MIN_VALUE;
            private long input;
            private long output;
        }
    }

    static final class NetworkDebugAccumulator {
        private int cableNodes;
        private int universalConnectors;
        private int energyEndpoints;
        private int energyModules;
        private int fluidModules;
        private int itemModules;
        private int bridgeModules;
        private int ae2BridgeEndpoints;
        private int refinedStorageBridgeEndpoints;
        private int activeChannelsMask;
        private int energyChannelsMask;
        private int fluidChannelsMask;
        private int itemChannelsMask;
        private int bridgeChannelsMask;
        private final long[] energyInputCaps = new long[MAX_CHANNEL + 1];
        private final long[] energyOutputCaps = new long[MAX_CHANNEL + 1];
        private long fluidShipmentCap;
        private long itemShipmentCap;
        private final int cacheAgeTicks;

        private NetworkDebugAccumulator(int cacheAgeTicks) {
            this.cacheAgeTicks = cacheAgeTicks;
        }

        void addCableNode() {
            cableNodes++;
        }

        void addUniversalConnector() {
            universalConnectors++;
        }

        void addEnergyModule(int channel) {
            energyModules++;
            markEnergyChannel(channel);
        }

        /**
         * Counts a connector's tier only in the directions its target can move FE: it can right now, or it did last
         * tick. The second check keeps a generator that empties its buffer every tick from dropping out.
         */
        void addEnergyConnector(
                int channel,
                CableConnectorMode mode,
                int transferRate,
                IEnergyStorage target,
                int lastInput,
                int lastOutput
        ) {
            if (!isValidChannel(channel) || target == null || transferRate <= 0) {
                return;
            }
            if (mode.acceptsNetworkInput() && (lastInput > 0 || target.canExtract())) {
                energyInputCaps[channel] += transferRate;
            }
            if (mode.sendsNetworkOutput() && (lastOutput > 0 || target.canReceive())) {
                energyEndpoints++;
                energyOutputCaps[channel] += transferRate;
            }
        }

        void addFluidModule(int channel, int shipmentCap, boolean active) {
            fluidModules++;
            if (active) {
                fluidShipmentCap += Math.max(0, shipmentCap);
                markFluidChannel(channel);
            }
        }

        void addItemModule(int channel, int shipmentCap, boolean active) {
            itemModules++;
            if (active) {
                itemShipmentCap += Math.max(0, shipmentCap);
                markItemChannel(channel);
            }
        }

        void addBridgeModule(NetworkBridgeType bridgeType, int channel, boolean activeEndpoint) {
            bridgeModules++;
            if (!activeEndpoint) {
                return;
            }
            if (bridgeType == NetworkBridgeType.AE2) {
                ae2BridgeEndpoints++;
            } else if (bridgeType == NetworkBridgeType.REFINED_STORAGE) {
                refinedStorageBridgeEndpoints++;
            }
            markBridgeChannel(channel);
        }

        NetworkDebugSnapshot snapshot() {
            long inputCap = 0L;
            long outputCap = 0L;
            long throughputCap = 0L;
            int[] channelCaps = new int[MAX_CHANNEL + 1];
            for (int channel = 0; channel <= MAX_CHANNEL; channel++) {
                inputCap += energyInputCaps[channel];
                outputCap += energyOutputCaps[channel];
                long channelCap = Math.min(energyInputCaps[channel], energyOutputCaps[channel]);
                channelCaps[channel] = saturatedInt(channelCap);
                throughputCap += channelCap;
            }
            return new NetworkDebugSnapshot(
                    cableNodes,
                    universalConnectors,
                    energyEndpoints,
                    energyModules,
                    fluidModules,
                    itemModules,
                    bridgeModules,
                    ae2BridgeEndpoints,
                    refinedStorageBridgeEndpoints,
                    bridgeChannelsMask,
                    activeChannelsMask,
                    energyChannelsMask,
                    fluidChannelsMask,
                    itemChannelsMask,
                    saturatedInt(throughputCap),
                    saturatedInt(fluidShipmentCap),
                    saturatedInt(itemShipmentCap),
                    cacheAgeTicks,
                    saturatedInt(inputCap),
                    saturatedInt(outputCap),
                    channelCaps
            );
        }

        private void markEnergyChannel(int channel) {
            if (isValidChannel(channel)) {
                energyChannelsMask |= 1 << channel;
                activeChannelsMask |= 1 << channel;
            }
        }

        private void markFluidChannel(int channel) {
            if (isValidChannel(channel)) {
                fluidChannelsMask |= 1 << channel;
                activeChannelsMask |= 1 << channel;
            }
        }

        private void markItemChannel(int channel) {
            if (isValidChannel(channel)) {
                itemChannelsMask |= 1 << channel;
                activeChannelsMask |= 1 << channel;
            }
        }

        private void markBridgeChannel(int channel) {
            if (isValidChannel(channel)) {
                bridgeChannelsMask |= 1 << channel;
                activeChannelsMask |= 1 << channel;
            }
        }

        private static int saturatedInt(long value) {
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return value < 0L ? 0 : (int) value;
        }
    }

    private enum UniversalEndpointType {
        CABLE_SIDE
    }

    private enum EnergyEndpointType {
        DIRECT,
        CABLE_UNIVERSAL
    }

    public record BridgeEndpoint(BlockPos pos, Direction side, NetworkBridgeType bridgeType, int channel) {
    }

    private record UniversalEndpoint(BlockPos pos, Direction side, UniversalEndpointType type) {
        private ItemStack receiveItem(Level level, int channel, ItemStack stack, boolean simulate) {
            return switch (type) {
                case CABLE_SIDE -> level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof CableBlockEntity cable
                        && cable.universalConnector(side) != null
                                ? cable.universalConnector(side).receiveItemFromCableNetwork(side, channel, stack, simulate)
                                : stack;
            };
        }

        private FluidStack receiveFluid(Level level, int channel, FluidStack stack, boolean simulate) {
            return switch (type) {
                case CABLE_SIDE -> level.isLoaded(pos)
                        && level.getBlockEntity(pos) instanceof CableBlockEntity cable
                        && cable.universalConnector(side) != null
                                ? cable.universalConnector(side).receiveFluidFromCableNetwork(side, channel, stack, simulate)
                                : stack;
            };
        }
    }

    public record EnergyBufferLevel(long stored, long capacity) {
    }

    private record EnergyEndpoint(
            BlockPos pos,
            Direction side,
            int transferRate,
            EnergyEndpointType type,
            BlockPos connectorPos,
            Direction connectorFace,
            boolean bothMode
    ) {
        private static EnergyEndpoint direct(
                BlockPos cablePos,
                Direction face,
                Direction attachAs,
                int transferRate,
                boolean bothMode
        ) {
            return new EnergyEndpoint(
                    cablePos.relative(face),
                    attachAs,
                    transferRate,
                    EnergyEndpointType.DIRECT,
                    cablePos,
                    face,
                    bothMode
            );
        }

        private static EnergyEndpoint cableUniversal(BlockPos pos, Direction side, int transferRate, boolean bothMode) {
            return new EnergyEndpoint(pos, side, transferRate, EnergyEndpointType.CABLE_UNIVERSAL, pos, side, bothMode);
        }

        private boolean isExcludedBy(Level level, TransferOrigin origin) {
            return origin.matches(pos, side)
                    || origin.matchesEnergyTarget(energyTargetPos())
                    || isAdjacentBatteryChassisTransfer(level, origin.energyTargetPos(), energyTargetPos());
        }

        /** Caps FE from a Both-connected storage block into another one at an even fill; no cap otherwise. */
        private int bufferLimit(Level level, TransferOrigin origin, int alreadyMoved) {
            EnergyBufferLevel source = origin.energyBuffer();
            if (source == null || !bothMode) {
                return Integer.MAX_VALUE;
            }
            IEnergyStorage target = targetEnergyBuffer(level);
            if (target == null) {
                return Integer.MAX_VALUE;
            }
            return bufferEqualizationLimit(
                    Math.max(0L, source.stored() - alreadyMoved),
                    source.capacity(),
                    target.getEnergyStored(),
                    target.getMaxEnergyStored()
            );
        }

        private BlockPos energyTargetPos() {
            return switch (type) {
                case DIRECT -> pos;
                case CABLE_UNIVERSAL -> pos == null || side == null ? null : pos.relative(side);
            };
        }

        private static boolean isAdjacentBatteryChassisTransfer(Level level, BlockPos sourcePos, BlockPos targetPos) {
            if (level == null
                    || sourcePos == null
                    || targetPos == null
                    || !isFaceAdjacent(sourcePos, targetPos)
                    || !level.isLoaded(sourcePos)
                    || !level.isLoaded(targetPos)) {
                return false;
            }
            return level.getBlockEntity(sourcePos) instanceof BatteryChassisBlockEntity
                    && level.getBlockEntity(targetPos) instanceof BatteryChassisBlockEntity;
        }

        private static boolean isFaceAdjacent(BlockPos first, BlockPos second) {
            int distance = Math.abs(first.getX() - second.getX())
                    + Math.abs(first.getY() - second.getY())
                    + Math.abs(first.getZ() - second.getZ());
            return distance == 1;
        }

        private IEnergyStorage targetEnergyBuffer(Level level) {
            if (!level.isLoaded(connectorPos) || !(level.getBlockEntity(connectorPos) instanceof CableBlockEntity cable)) {
                return null;
            }
            return switch (type) {
                case DIRECT -> cable.directTargetEnergyBuffer(connectorFace);
                case CABLE_UNIVERSAL -> cable.universalConnector(connectorFace) == null
                        ? null
                        : cable.universalConnector(connectorFace).targetEnergyBuffer();
            };
        }

        private int receive(Level level, int channel, int amount, boolean simulate) {
            if (!level.isLoaded(connectorPos) || !(level.getBlockEntity(connectorPos) instanceof CableBlockEntity cable)) {
                return 0;
            }
            return switch (type) {
                case DIRECT -> cable.sendToDirectTarget(connectorFace, amount, simulate);
                case CABLE_UNIVERSAL -> cable.universalConnector(connectorFace) == null
                        ? 0
                        : cable.universalConnector(connectorFace).receiveFromCableNetwork(connectorFace, channel, amount, simulate);
            };
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("DisabledLinks", disabledLinkMask());
        if (color != null) {
            tag.putString("Color", color.getSerializedName());
        }
        CompoundTag connectorsTag = new CompoundTag();
        for (Direction direction : DIRECTIONS) {
            ConnectorData connector = connectors.get(direction);
            if (connector == null) {
                continue;
            }
            CompoundTag connectorTag = new CompoundTag();
            connectorTag.putString("Tier", connector.tier().getSerializedName());
            connectorTag.putInt("Channel", connector.channel());
            connectorTag.putString("AttachAs", connector.attachAs().getSerializedName());
            connectorTag.putString("Mode", connector.mode().getSerializedName());
            connectorTag.putString("Distribution", connector.distributionMode().getSerializedName());
            connectorsTag.put(direction.getSerializedName(), connectorTag);
        }
        tag.put("Connectors", connectorsTag);
        CompoundTag universalConnectorsTag = new CompoundTag();
        for (Direction direction : DIRECTIONS) {
            CableUniversalConnectorData connector = universalConnectors.get(direction);
            if (connector == null) {
                continue;
            }
            universalConnectorsTag.put(direction.getSerializedName(), connector.save(registries));
        }
        tag.put("UniversalConnectors", universalConnectorsTag);
    }

    /** The client needs which faces hold which connector so Jade and pick-block name the targeted connector. */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (color != null) {
            components.set(DataComponents.BASE_COLOR, color);
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        color = componentInput.get(DataComponents.BASE_COLOR);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Color");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        DyeColor previousColor = color;
        color = tag.contains("Color", Tag.TAG_STRING) ? DyeColor.byName(tag.getString("Color"), null) : null;
        if (color != previousColor && level != null && level.isClientSide) {
            // Colour lives here rather than in the block state, so the client must re-mesh the tint itself.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        disabledLinks.clear();
        int mask = tag.getInt("DisabledLinks");
        for (Direction direction : DIRECTIONS) {
            if ((mask & (1 << direction.ordinal())) != 0) {
                disabledLinks.add(direction);
            }
        }

        connectors.clear();
        CompoundTag connectorsTag = tag.getCompound("Connectors");
        for (Direction direction : DIRECTIONS) {
            String key = direction.getSerializedName();
            if (!connectorsTag.contains(key, Tag.TAG_COMPOUND)) {
                continue;
            }
            ConnectorData.load(connectorsTag.getCompound(key), direction)
                    .ifPresent(connector -> connectors.put(direction, connector));
        }

        universalConnectors.clear();
        CompoundTag universalConnectorsTag = tag.getCompound("UniversalConnectors");
        for (Direction direction : DIRECTIONS) {
            String key = direction.getSerializedName();
            if (!universalConnectorsTag.contains(key, Tag.TAG_COMPOUND)) {
                continue;
            }
            CableUniversalConnectorData connector = new CableUniversalConnectorData(this, direction);
            connector.load(universalConnectorsTag.getCompound(key), registries);
            universalConnectors.put(direction, connector);
        }

        invalidateNetworkCache();
    }

    private int disabledLinkMask() {
        int mask = 0;
        for (Direction direction : disabledLinks) {
            mask |= 1 << direction.ordinal();
        }
        return mask;
    }

    private static ItemStack connectorStack(EnergyConnectorTier tier) {
        return new ItemStack(ModItems.energyConnector(tier).get());
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

    public static int dataEnergyTargetAccessIndex() {
        return DATA_ENERGY_TARGET_ACCESS;
    }

    public static int dataCount() {
        return DATA_ENERGY_TARGET_ACCESS + 1;
    }

    public static final class ConnectorData {
        private final EnergyConnectorTier tier;
        private int channel;
        private Direction attachAs;
        private CableConnectorMode mode;
        private EnergyDistributionMode distributionMode;

        private ConnectorData(
                EnergyConnectorTier tier,
                int channel,
                Direction attachAs,
                CableConnectorMode mode,
                EnergyDistributionMode distributionMode
        ) {
            this.tier = tier;
            this.channel = Math.max(0, Math.min(MAX_CHANNEL, channel));
            this.attachAs = attachAs;
            this.mode = mode;
            this.distributionMode = distributionMode == null ? EnergyDistributionMode.ROUND_ROBIN : distributionMode;
        }

        public static ConnectorData defaults(EnergyConnectorTier tier, Direction face) {
            return new ConnectorData(
                    tier,
                    0,
                    face.getOpposite(),
                    CableConnectorMode.BOTH,
                    EnergyDistributionMode.ROUND_ROBIN
            );
        }

        public static java.util.Optional<ConnectorData> load(CompoundTag tag, Direction face) {
            java.util.Optional<EnergyConnectorTier> tier = EnergyConnectorTier.bySerializedName(tag.getString("Tier"));
            if (tier.isEmpty()) {
                return java.util.Optional.empty();
            }
            Direction attachAs = Direction.byName(tag.getString("AttachAs"));
            if (attachAs == null) {
                attachAs = face.getOpposite();
            }
            CableConnectorMode mode = CableConnectorMode.bySerializedName(tag.getString("Mode"))
                    .orElse(CableConnectorMode.BOTH);
            EnergyDistributionMode distributionMode = EnergyDistributionMode.bySerializedName(tag.getString("Distribution"))
                    .orElse(EnergyDistributionMode.ROUND_ROBIN);
            return java.util.Optional.of(new ConnectorData(
                    tier.get(),
                    tag.getInt("Channel"),
                    attachAs,
                    mode,
                    distributionMode
            ));
        }

        public EnergyConnectorTier tier() {
            return tier;
        }

        public int channel() {
            return channel;
        }

        public void setChannel(int channel) {
            this.channel = Math.max(0, Math.min(MAX_CHANNEL, channel));
        }

        public Direction attachAs() {
            return attachAs;
        }

        public void setAttachAs(Direction attachAs) {
            this.attachAs = attachAs;
        }

        public CableConnectorMode mode() {
            return mode;
        }

        public void setMode(CableConnectorMode mode) {
            this.mode = mode;
        }

        public EnergyDistributionMode distributionMode() {
            return distributionMode;
        }

        public void setDistributionMode(EnergyDistributionMode distributionMode) {
            this.distributionMode = distributionMode == null ? EnergyDistributionMode.ROUND_ROBIN : distributionMode;
        }

        public int transferRate() {
            return tier.enabled() ? tier.transferRate() : 0;
        }
    }

    private final class CableEnergyStorage implements IEnergyStorage {
        private final Direction entrySide;

        private CableEnergyStorage(Direction entrySide) {
            this.entrySide = entrySide;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return receiveFromSide(entrySide, toReceive, simulate);
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
            ConnectorData connector = connectors.get(entrySide);
            return connector == null ? 0 : connector.transferRate();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            ConnectorData connector = connectors.get(entrySide);
            return connector != null && connector.transferRate() > 0 && connector.mode().acceptsNetworkInput();
        }
    }
}
