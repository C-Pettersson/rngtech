package com.rngtech.compat.refinedstorage;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.CableUniversalConnectorData;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.registry.ModBlockEntities;

import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.support.network.ConnectionSink;
import com.refinedmods.refinedstorage.common.api.support.network.ConnectionStrategy;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class RefinedStorageCableBridgeCompat {
    private static final Map<BlockEntity, BridgeContainerProvider> PROVIDERS = new WeakHashMap<>();

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
                ModBlockEntities.CABLE.get(),
                (cable, context) -> provider(cable)
        );
        event.registerBlockEntity(
                RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
                ModBlockEntities.UNIVERSAL_CONNECTOR.get(),
                (connector, context) -> provider(connector)
        );
    }

    private static BridgeContainerProvider provider(BlockEntity blockEntity) {
        synchronized (PROVIDERS) {
            return PROVIDERS.computeIfAbsent(blockEntity, BridgeContainerProvider::new);
        }
    }

    private RefinedStorageCableBridgeCompat() {
    }

    private static final class BridgeContainerProvider implements NetworkNodeContainerProvider {
        private final BlockEntity blockEntity;
        private final EnumMap<Direction, InWorldNetworkNodeContainer> containers = new EnumMap<>(Direction.class);
        private final Set<InWorldNetworkNodeContainer> importedContainers = new HashSet<>();
        private boolean syncing;

        private BridgeContainerProvider(BlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public Set<InWorldNetworkNodeContainer> getContainers() {
            sync(blockEntity.getLevel());
            Set<InWorldNetworkNodeContainer> allContainers = new HashSet<>(containers.values());
            allContainers.addAll(importedContainers);
            return Collections.unmodifiableSet(allContainers);
        }

        @Override
        public void addContainer(InWorldNetworkNodeContainer container) {
            importedContainers.add(container);
        }

        @Override
        public boolean canBuild(ServerPlayer player) {
            if (player == null || !player.mayBuild()) {
                return false;
            }

            Set<InWorldNetworkNodeContainer> networkedContainers = new HashSet<>();
            for (InWorldNetworkNodeContainer container : getContainers()) {
                if (container.getNode().getNetwork() != null) {
                    networkedContainers.add(container);
                }
            }
            return networkedContainers.isEmpty()
                    || SecurityHelper.isAllowed(player, BuiltinPermission.BUILD, networkedContainers);
        }

        @Override
        public void initialize(Level level, Runnable listener) {
            sync(level);
            for (InWorldNetworkNodeContainer container : containers.values()) {
                RefinedStorageApi.INSTANCE.initializeNetworkNodeContainer(container, level, listener);
            }
        }

        @Override
        public void update(Level level) {
            sync(level);
            for (InWorldNetworkNodeContainer container : containers.values()) {
                RefinedStorageApi.INSTANCE.updateNetworkNodeContainer(container, level);
            }
        }

        @Override
        public void remove(Level level) {
            List<InWorldNetworkNodeContainer> removedContainers = drainContainers();
            importedContainers.clear();
            if (level == null) {
                return;
            }

            syncing = true;
            try {
                removeNetworkNodeContainers(removedContainers, level);
            } finally {
                syncing = false;
            }
        }

        private void sync(Level level) {
            if (syncing) {
                return;
            }

            syncing = true;
            try {
                if (blockEntity.isRemoved()) {
                    List<InWorldNetworkNodeContainer> removedContainers = drainContainers();
                    importedContainers.clear();
                    removeNetworkNodeContainers(removedContainers, level);
                    return;
                }

                Set<Direction> activeSides = new HashSet<>();
                for (Direction side : Direction.values()) {
                    if (!isActive(side)) {
                        continue;
                    }
                    activeSides.add(side);
                    containers.computeIfAbsent(side, this::createContainer);
                }

                List<InWorldNetworkNodeContainer> removedContainers = new ArrayList<>();
                containers.entrySet().removeIf(entry -> {
                    if (activeSides.contains(entry.getKey())) {
                        return false;
                    }
                    removedContainers.add(entry.getValue());
                    return true;
                });
                removeNetworkNodeContainers(removedContainers, level);
            } finally {
                syncing = false;
            }
        }

        private List<InWorldNetworkNodeContainer> drainContainers() {
            List<InWorldNetworkNodeContainer> removedContainers = new ArrayList<>(containers.values());
            containers.clear();
            return removedContainers;
        }

        private static void removeNetworkNodeContainers(List<InWorldNetworkNodeContainer> containers, Level level) {
            if (level == null) {
                return;
            }
            for (InWorldNetworkNodeContainer container : containers) {
                RefinedStorageApi.INSTANCE.removeNetworkNodeContainer(container, level);
            }
        }

        private InWorldNetworkNodeContainer createContainer(Direction side) {
            SimpleNetworkNode node = new SimpleNetworkNode(0);
            node.setEnergyUsage(0);
            return RefinedStorageApi.INSTANCE
                    .createNetworkNodeContainer(blockEntity, node)
                    .name("rngtech_rs_bridge_" + side.getSerializedName())
                    .priority(0)
                    .keyProvider(() -> side)
                    .connectionStrategy(new BridgeConnectionStrategy(this, side))
                    .build();
        }

        private boolean isActive(Direction side) {
            CableUniversalConnectorData connector = connector(side);
            return connector != null
                    && connector.bridgeType() == NetworkBridgeType.REFINED_STORAGE
                    && connector.hasActiveBridgeModule();
        }

        private CableUniversalConnectorData connector(Direction side) {
            if (blockEntity instanceof CableBlockEntity cable) {
                return cable.universalConnector(side);
            }
            if (blockEntity instanceof UniversalConnectorBlockEntity connector) {
                return connector.universalConnector(side);
            }
            return null;
        }
    }

    private record BridgeConnectionStrategy(BridgeContainerProvider provider, Direction side) implements ConnectionStrategy {
        @Override
        public void addOutgoingConnections(ConnectionSink sink) {
            BlockEntity blockEntity = provider.blockEntity;
            Level level = blockEntity.getLevel();
            CableUniversalConnectorData connector = provider.connector(side);
            if (level == null || connector == null) {
                return;
            }

            sink.tryConnectInSameDimension(blockEntity.getBlockPos().relative(side), side.getOpposite());
            for (CableBlockEntity.BridgeEndpoint endpoint : CableBlockEntity.bridgeEndpoints(
                    level,
                    blockEntity.getBlockPos(),
                    NetworkBridgeType.REFINED_STORAGE,
                    connector.bridgeChannel()
            )) {
                if (endpoint.pos().equals(blockEntity.getBlockPos()) && endpoint.side() == side) {
                    continue;
                }
                sink.tryConnectInSameDimension(endpoint.pos(), endpoint.side(), CableBlock.class);
            }
        }

        @Override
        public boolean canAcceptIncomingConnection(Direction direction, BlockState state) {
            return direction == side;
        }
    }
}
