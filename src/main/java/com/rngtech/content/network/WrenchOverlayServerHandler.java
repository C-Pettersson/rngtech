package com.rngtech.content.network;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.wrench.ConnectorOverlaySnapshot;
import com.rngtech.content.wrench.WrenchOverlayTarget;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public final class WrenchOverlayServerHandler {
    private WrenchOverlayServerHandler() {
    }

    public static void handle(WrenchOverlayRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.reply(new WrenchOverlayDataPayload(payload.target(), snapshotFor(player, payload.target())));
    }

    private static Optional<ConnectorOverlaySnapshot> snapshotFor(ServerPlayer player, WrenchOverlayTarget target) {
        if (!isHoldingWrench(player) || !isNear(player, target.pos())) {
            return Optional.empty();
        }

        Level level = player.level();
        BlockState state = level.getBlockState(target.pos());
        BlockEntity blockEntity = level.getBlockEntity(target.pos());
        if (target.isStandalone()) {
            if (state.getBlock() instanceof UniversalConnectorBlock
                    && blockEntity instanceof UniversalConnectorBlockEntity connector) {
                Direction face = state.getValue(UniversalConnectorBlock.FACING);
                return Optional.of(ConnectorOverlaySnapshot.from(connector, false, face));
            }
            return Optional.empty();
        }

        Direction side = target.side();
        if (side == null
                || !(state.getBlock() instanceof CableBlock)
                || !(blockEntity instanceof CableBlockEntity cable)
                || !cable.hasUniversalConnector(side)) {
            return Optional.empty();
        }

        UniversalConnectorAccess connector = cable.universalConnectorAccess(side);
        return connector == null
                ? Optional.empty()
                : Optional.of(ConnectorOverlaySnapshot.from(connector, true, side));
    }

    private static boolean isHoldingWrench(Player player) {
        return player.getMainHandItem().is(ModItems.WRENCH.get()) || player.getOffhandItem().is(ModItems.WRENCH.get());
    }

    private static boolean isNear(Player player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
