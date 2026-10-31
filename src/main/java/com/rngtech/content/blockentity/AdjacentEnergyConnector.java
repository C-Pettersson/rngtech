package com.rngtech.content.blockentity;

import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.menu.UniversalConnectorAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Sums the energy connectors that actually move FE for a machine: Universal Connector Energy rows and direct cable
 * connectors facing it, in a mode that matches the requested direction, attached as a side where the machine exposes FE.
 */
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

    /** Reports the connector as the limit when the machine needs more FE/t than its sink connectors carry. */
    public static MachineInfoSnapshot.EnergyBottleneck inputBottleneck(Info connector, int demand) {
        return connector.present() && demand > connector.transferRate()
                ? MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_INPUT
                : MachineInfoSnapshot.EnergyBottleneck.NONE;
    }

    /** Reports the connector as the limit when the machine generates more FE/t than its source connectors carry. */
    public static MachineInfoSnapshot.EnergyBottleneck outputBottleneck(Info connector, int generation) {
        return connector.present() && generation > connector.transferRate()
                ? MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_OUTPUT
                : MachineInfoSnapshot.EnergyBottleneck.NONE;
    }

    private static Info collect(Level level, BlockPos targetPos, boolean source) {
        if (level == null) {
            return Info.NONE;
        }

        int transferRate = 0;
        int lastInput = 0;
        int lastOutput = 0;
        for (Direction direction : DIRECTIONS) {
            for (Connector connector : connectors(level, targetPos.relative(direction), direction.getOpposite())) {
                if (connector.transferRate() <= 0) {
                    continue;
                }
                if ((source && !connector.mode().acceptsNetworkInput()) || (!source && !connector.mode().sendsNetworkOutput())) {
                    continue;
                }
                if (level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, connector.attachAs()) == null) {
                    continue;
                }

                transferRate += connector.transferRate();
                lastInput += connector.lastInput();
                lastOutput += connector.lastOutput();
            }
        }
        return transferRate <= 0 ? Info.NONE : new Info(transferRate, lastInput, lastOutput);
    }

    private static List<Connector> connectors(Level level, BlockPos pos, Direction face) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CableBlockEntity cable) {
            CableBlockEntity.ConnectorData direct = cable.connector(face);
            if (direct != null) {
                return List.of(new Connector(
                        direct.mode(),
                        direct.attachAs(),
                        direct.transferRate(),
                        cable.lastDirectEnergyInput(face),
                        cable.lastDirectEnergyOutput(face)
                ));
            }
            return universalConnectors(cable.universalConnectorAccess(face));
        }
        if (blockEntity instanceof UniversalConnectorBlockEntity connector && connector.targetDirection() == face) {
            return universalConnectors(connector.universalConnectorAccess(face));
        }
        return List.of();
    }

    private static List<Connector> universalConnectors(UniversalConnectorAccess access) {
        if (access == null) {
            return List.of();
        }
        ContainerData data = access.menuData();
        List<Connector> connectors = new ArrayList<>(UniversalConnectorBlockEntity.ENERGY_MODULE_SLOT_COUNT);
        for (int module = 0; module < UniversalConnectorBlockEntity.ENERGY_MODULE_SLOT_COUNT; module++) {
            connectors.add(new Connector(
                    connectorMode(data.get(UniversalConnectorBlockEntity.dataEnergyModeIndex(module))),
                    direction(data.get(UniversalConnectorBlockEntity.dataEnergyAttachAsIndex(module))),
                    data.get(UniversalConnectorBlockEntity.dataEnergyTransferRateIndex(module)),
                    data.get(UniversalConnectorBlockEntity.dataEnergyLastInputIndex(module)),
                    data.get(UniversalConnectorBlockEntity.dataEnergyLastOutputIndex(module))
            ));
        }
        return connectors;
    }

    private static CableConnectorMode connectorMode(int ordinal) {
        CableConnectorMode[] values = CableConnectorMode.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CableConnectorMode.BOTH;
    }

    private static Direction direction(int ordinal) {
        return ordinal >= 0 && ordinal < DIRECTIONS.length ? DIRECTIONS[ordinal] : null;
    }

    private record Connector(CableConnectorMode mode, Direction attachAs, int transferRate, int lastInput, int lastOutput) {
    }

    public record Info(int transferRate, int lastInput, int lastOutput) {
        public static final Info NONE = new Info(0, 0, 0);

        public boolean present() {
            return transferRate > 0;
        }
    }
}
