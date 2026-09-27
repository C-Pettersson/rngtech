package com.rngtech.content.blockentity;

import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.menu.UniversalConnectorAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class AdjacentEnergyConnector {
    private static final Direction[] DIRECTIONS = Direction.values();

    private AdjacentEnergyConnector() {
    }

    public static Info forSource(Level level, BlockPos targetPos) {
        return collect(level, targetPos, true);
    }

    public static Info forSink(Level level, BlockPos targetPos) {
        return collect(level, targetPos, false);
    }

    private static Info collect(Level level, BlockPos targetPos, boolean source) {
        if (level == null) {
            return Info.NONE;
        }

        int transferRate = 0;
        int lastInput = 0;
        int lastOutput = 0;
        for (Direction direction : DIRECTIONS) {
            Direction connectorFace = direction.getOpposite();
            UniversalConnectorAccess access = connectorAccess(level, targetPos.relative(direction), connectorFace);
            if (access == null) {
                continue;
            }

            ContainerData data = access.menuData();
            if (data.get(UniversalConnectorBlockEntity.dataHasConnectorIndex()) == 0) {
                continue;
            }

            CableConnectorMode mode = connectorMode(data.get(UniversalConnectorBlockEntity.dataModeIndex()));
            if ((source && !mode.acceptsNetworkInput()) || (!source && !mode.sendsNetworkOutput())) {
                continue;
            }

            transferRate += data.get(UniversalConnectorBlockEntity.dataTransferRateIndex());
            lastInput += data.get(UniversalConnectorBlockEntity.dataLastEnergyInputIndex());
            lastOutput += data.get(UniversalConnectorBlockEntity.dataLastEnergyOutputIndex());
        }
        return transferRate <= 0 ? Info.NONE : new Info(transferRate, lastInput, lastOutput);
    }

    private static UniversalConnectorAccess connectorAccess(Level level, BlockPos pos, Direction face) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CableBlockEntity cable) {
            return cable.universalConnectorAccess(face);
        }
        if (blockEntity instanceof UniversalConnectorBlockEntity connector && connector.targetDirection() == face) {
            return connector.universalConnectorAccess(face);
        }
        return null;
    }

    private static CableConnectorMode connectorMode(int ordinal) {
        CableConnectorMode[] values = CableConnectorMode.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CableConnectorMode.BOTH;
    }

    public record Info(int transferRate, int lastInput, int lastOutput) {
        public static final Info NONE = new Info(0, 0, 0);

        public boolean present() {
            return transferRate > 0;
        }
    }
}
