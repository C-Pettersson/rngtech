package com.rngtech.content.configurator;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public record ConfiguratorTarget(BlockPos pos, ConfiguratorTargetType type, int sideOrdinal) {
    public static ConfiguratorTarget resolve(Level level, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (state.getBlock() instanceof UniversalConnectorBlock && blockEntity instanceof UniversalConnectorBlockEntity) {
            return new ConfiguratorTarget(pos, ConfiguratorTargetType.UNIVERSAL_CONNECTOR, -1);
        }
        if (!(state.getBlock() instanceof CableBlock) || !(blockEntity instanceof CableBlockEntity cable)) {
            return null;
        }

        Direction side = CableBlock.resolveInstalledConnectorDirection(state, hitResult, true);
        if (side == null) {
            return null;
        }
        if (cable.hasUniversalConnector(side)) {
            return new ConfiguratorTarget(pos, ConfiguratorTargetType.CABLE_UNIVERSAL_CONNECTOR, side.ordinal());
        }
        if (cable.hasConnector(side)) {
            return new ConfiguratorTarget(pos, ConfiguratorTargetType.CABLE_ENERGY_CONNECTOR, side.ordinal());
        }
        return null;
    }

    public static ConfiguratorTarget read(RegistryFriendlyByteBuf buffer) {
        return new ConfiguratorTarget(
                buffer.readBlockPos(),
                ConfiguratorTargetType.byOrdinal(buffer.readByte()),
                buffer.readByte()
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByte(type.ordinal());
        buffer.writeByte(sideOrdinal);
    }

    public Direction side() {
        Direction[] values = Direction.values();
        return sideOrdinal >= 0 && sideOrdinal < values.length ? values[sideOrdinal] : null;
    }
}
