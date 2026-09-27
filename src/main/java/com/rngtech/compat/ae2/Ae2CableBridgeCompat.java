package com.rngtech.compat.ae2;

import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.CableUniversalConnectorData;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.NetworkBridgeType;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;

import appeng.api.AECapabilities;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Ae2CableBridgeCompat {
    private static final Map<BlockEntity, BridgeHost> HOSTS = new WeakHashMap<>();
    private static final IGridNodeListener<BridgeEndpointNode> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(BridgeEndpointNode nodeOwner, IGridNode node) {
            nodeOwner.setChanged();
        }
    };

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.CABLE.get(),
                (cable, context) -> host(cable)
        );
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.UNIVERSAL_CONNECTOR.get(),
                (connector, context) -> host(connector)
        );
    }

    private static BridgeHost host(BlockEntity blockEntity) {
        synchronized (HOSTS) {
            return HOSTS.computeIfAbsent(blockEntity, BridgeHost::new);
        }
    }

    private Ae2CableBridgeCompat() {
    }

    private static final class BridgeHost implements IInWorldGridNodeHost {
        private final BlockEntity blockEntity;
        private final EnumMap<Direction, BridgeEndpointNode> endpoints = new EnumMap<>(Direction.class);
        private boolean syncingEndpoints;

        private BridgeHost(BlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public IGridNode getGridNode(Direction dir) {
            syncEndpoints();
            if (dir == null || blockEntity.isRemoved() || !isActive(dir)) {
                return null;
            }
            return endpoint(dir).gridNode(true);
        }

        @Override
        public AECableType getCableConnectionType(Direction dir) {
            syncEndpoints();
            return dir != null && !blockEntity.isRemoved() && isActive(dir) ? AECableType.GLASS : AECableType.NONE;
        }

        private BridgeEndpointNode endpoint(Direction side) {
            return endpoints.computeIfAbsent(side, key -> new BridgeEndpointNode(this, key));
        }

        private void syncEndpoints() {
            if (syncingEndpoints) {
                return;
            }

            syncingEndpoints = true;
            try {
                List<BridgeEndpointNode> removedEndpoints = new ArrayList<>();
                endpoints.entrySet().removeIf(entry -> {
                    if (!blockEntity.isRemoved() && isActive(entry.getKey())) {
                        return false;
                    }
                    removedEndpoints.add(entry.getValue());
                    return true;
                });
                for (BridgeEndpointNode endpoint : removedEndpoints) {
                    endpoint.destroy();
                }
            } finally {
                syncingEndpoints = false;
            }
        }

        private boolean isActive(Direction side) {
            CableUniversalConnectorData connector = connector(side);
            return connector != null
                    && connector.bridgeType() == NetworkBridgeType.AE2
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

        private Level level() {
            return blockEntity.getLevel();
        }

        private BlockPos pos() {
            return blockEntity.getBlockPos();
        }

        private void setChanged() {
            blockEntity.setChanged();
        }
    }

    private static final class BridgeEndpointNode {
        private final BridgeHost host;
        private final Direction side;
        private final Map<PeerKey, IGridConnection> peerConnections = new HashMap<>();
        private IManagedGridNode managedNode;
        private boolean syncing;

        private BridgeEndpointNode(BridgeHost host, Direction side) {
            this.host = host;
            this.side = side;
        }

        private IGridNode gridNode(boolean syncPeers) {
            if (!host.isActive(side)) {
                destroy();
                return null;
            }
            IManagedGridNode managed = managedNode();
            Level level = host.level();
            if (!managed.isReady() && level != null && !level.isClientSide) {
                managed.create(level, host.pos());
            }
            IGridNode node = managed.getNode();
            if (syncPeers) {
                syncPeerConnections(node);
            }
            return node;
        }

        private IManagedGridNode managedNode() {
            if (managedNode == null) {
                managedNode = GridHelper.createManagedNode(this, NODE_LISTENER)
                        .setInWorldNode(true)
                        .setExposedOnSides(Set.of(side))
                        .setIdlePowerUsage(0.0D)
                        .setVisualRepresentation(ModItems.AE2_NETWORK_CONNECTOR.get());
            }
            return managedNode;
        }

        private void syncPeerConnections(IGridNode node) {
            Level level = host.level();
            if (syncing || level == null || level.isClientSide || node == null) {
                return;
            }
            CableUniversalConnectorData connector = host.connector(side);
            if (connector == null) {
                destroy();
                return;
            }

            syncing = true;
            try {
                Set<PeerKey> targets = new HashSet<>();
                for (CableBlockEntity.BridgeEndpoint endpoint : CableBlockEntity.bridgeEndpoints(
                        level,
                        host.pos(),
                        NetworkBridgeType.AE2,
                        connector.bridgeChannel()
                )) {
                    PeerKey key = new PeerKey(endpoint.pos(), endpoint.side());
                    if (key.matches(host.pos(), side)) {
                        continue;
                    }
                    targets.add(key);
                    peerConnections.computeIfAbsent(key, peer -> createConnection(level, node, peer));
                }
                peerConnections.entrySet().removeIf(entry -> {
                    if (targets.contains(entry.getKey())) {
                        return false;
                    }
                    entry.getValue().destroy();
                    return true;
                });
            } finally {
                syncing = false;
            }
        }

        private IGridConnection createConnection(Level level, IGridNode node, PeerKey peer) {
            if (!(level.getBlockEntity(peer.pos()) instanceof CableBlockEntity cable)) {
                return null;
            }
            IGridNode peerNode = host(cable).endpoint(peer.side()).gridNode(false);
            if (peerNode == null) {
                return null;
            }
            try {
                return GridHelper.createConnection(node, peerNode);
            } catch (IllegalStateException ignored) {
                return null;
            }
        }

        private void destroy() {
            for (IGridConnection connection : peerConnections.values()) {
                if (connection != null) {
                    connection.destroy();
                }
            }
            peerConnections.clear();
            if (managedNode != null) {
                managedNode.destroy();
                managedNode = null;
            }
        }

        private void setChanged() {
            host.setChanged();
        }
    }

    private record PeerKey(BlockPos pos, Direction side) {
        private boolean matches(BlockPos pos, Direction side) {
            return this.pos.equals(pos) && this.side == side;
        }
    }
}
