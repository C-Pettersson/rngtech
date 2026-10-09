package com.rngtech.content.wrench;

import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.menu.UniversalConnectorAccess;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public record ConnectorOverlaySnapshot(
        boolean cableSide,
        int mountedFaceOrdinal,
        String energyConnectorKey,
        int energyModeOrdinal,
        int energyDistributionOrdinal,
        int energyChannel,
        int energyAttachOrdinal,
        int energyTransferRate,
        int lastEnergyInput,
        int lastEnergyOutput,
        boolean energyTargetAccess,
        int bridgeTypeId,
        int bridgeChannel,
        boolean bridgeModLoaded,
        int bridgeEndpointCount,
        List<ModuleSnapshot> fluidModules,
        List<ModuleSnapshot> itemModules
) {
    private static final int MAX_KEY_LENGTH = 160;
    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectorOverlaySnapshot> STREAM_CODEC =
            StreamCodec.ofMember(ConnectorOverlaySnapshot::write, ConnectorOverlaySnapshot::read);

    public ConnectorOverlaySnapshot {
        fluidModules = List.copyOf(fluidModules);
        itemModules = List.copyOf(itemModules);
    }

    public static ConnectorOverlaySnapshot from(UniversalConnectorAccess access, boolean cableSide, Direction mountedFace) {
        ItemStackHandler inventory = access.getInventory();
        ContainerData data = access.menuData();
        List<ModuleSnapshot> fluidModules = new ArrayList<>(UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT);
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT; moduleIndex++) {
            fluidModules.add(fluidModule(inventory, data, moduleIndex));
        }

        List<ModuleSnapshot> itemModules = new ArrayList<>(UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT);
        for (int moduleIndex = 0; moduleIndex < UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT; moduleIndex++) {
            itemModules.add(itemModule(inventory, data, moduleIndex));
        }

        return new ConnectorOverlaySnapshot(
                cableSide,
                mountedFace.ordinal(),
                itemKey(inventory.getStackInSlot(UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR)),
                data.get(UniversalConnectorBlockEntity.dataModeIndex()),
                data.get(UniversalConnectorBlockEntity.dataDistributionIndex()),
                data.get(UniversalConnectorBlockEntity.dataChannelIndex()),
                data.get(UniversalConnectorBlockEntity.dataAttachAsIndex()),
                data.get(UniversalConnectorBlockEntity.dataTransferRateIndex()),
                data.get(UniversalConnectorBlockEntity.dataLastEnergyInputIndex()),
                data.get(UniversalConnectorBlockEntity.dataLastEnergyOutputIndex()),
                data.get(UniversalConnectorBlockEntity.dataEnergyTargetAccessIndex()) != 0,
                data.get(UniversalConnectorBlockEntity.dataBridgeTypeIndex()),
                data.get(UniversalConnectorBlockEntity.dataBridgeChannelIndex()),
                data.get(UniversalConnectorBlockEntity.dataBridgeModLoadedIndex()) != 0,
                data.get(UniversalConnectorBlockEntity.dataBridgeEndpointCountIndex()),
                fluidModules,
                itemModules
        );
    }

    public boolean hasEnergyConnector() {
        return !energyConnectorKey.isBlank();
    }

    public boolean hasBridgeConnector() {
        return bridgeTypeId != 0;
    }

    public int installedFluidModules() {
        return installedModules(fluidModules);
    }

    public int installedItemModules() {
        return installedModules(itemModules);
    }

    public Direction mountedFace() {
        return direction(mountedFaceOrdinal);
    }

    private static ModuleSnapshot fluidModule(ItemStackHandler inventory, ContainerData data, int moduleIndex) {
        ItemStack stack = inventory.getStackInSlot(UniversalConnectorBlockEntity.fluidConnectorSlot(moduleIndex));
        int shipment = stack.getItem() instanceof FluidConnectorItem connector ? connector.tier().fluidPerShipment() : 0;
        return new ModuleSnapshot(
                itemKey(stack),
                data.get(UniversalConnectorBlockEntity.dataFluidModeIndex(moduleIndex)),
                data.get(UniversalConnectorBlockEntity.dataFluidChannelIndex(moduleIndex)),
                data.get(UniversalConnectorBlockEntity.dataFluidAttachAsIndex(moduleIndex)),
                data.get(UniversalConnectorBlockEntity.dataFluidCooldownIndex(moduleIndex)),
                data.get(UniversalConnectorBlockEntity.dataFluidJamTicksIndex(moduleIndex)),
                shipment
        );
    }

    private static ModuleSnapshot itemModule(ItemStackHandler inventory, ContainerData data, int moduleIndex) {
        ItemStack stack = inventory.getStackInSlot(UniversalConnectorBlockEntity.itemConnectorSlot(moduleIndex));
        int shipment = stack.getItem() instanceof ItemConnectorItem connector ? connector.tier().itemsPerShipment() : 0;
        return new ModuleSnapshot(
                itemKey(stack),
                data.get(UniversalConnectorBlockEntity.dataItemModeIndex(moduleIndex)),
                data.get(UniversalConnectorBlockEntity.dataItemChannelIndex(moduleIndex)),
                data.get(UniversalConnectorBlockEntity.dataItemAttachAsIndex(moduleIndex)),
                data.get(UniversalConnectorBlockEntity.dataItemCooldownIndex(moduleIndex)),
                data.get(UniversalConnectorBlockEntity.dataItemJamTicksIndex(moduleIndex)),
                shipment
        );
    }

    private static int installedModules(List<ModuleSnapshot> modules) {
        int count = 0;
        for (ModuleSnapshot module : modules) {
            if (module.installed()) {
                count++;
            }
        }
        return count;
    }

    private static String itemKey(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getDescriptionId();
    }

    private static Direction direction(int ordinal) {
        Direction[] values = Direction.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Direction.NORTH;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(cableSide);
        buffer.writeByte(mountedFaceOrdinal);
        writeString(buffer, energyConnectorKey);
        buffer.writeByte(energyModeOrdinal);
        buffer.writeByte(energyDistributionOrdinal);
        buffer.writeByte(energyChannel);
        buffer.writeByte(energyAttachOrdinal);
        buffer.writeVarInt(energyTransferRate);
        buffer.writeVarInt(lastEnergyInput);
        buffer.writeVarInt(lastEnergyOutput);
        buffer.writeBoolean(energyTargetAccess);
        buffer.writeByte(bridgeTypeId);
        buffer.writeByte(bridgeChannel);
        buffer.writeBoolean(bridgeModLoaded);
        buffer.writeVarInt(bridgeEndpointCount);
        writeModules(buffer, fluidModules);
        writeModules(buffer, itemModules);
    }

    private static ConnectorOverlaySnapshot read(RegistryFriendlyByteBuf buffer) {
        boolean cableSide = buffer.readBoolean();
        int mountedFaceOrdinal = buffer.readByte();
        String energyConnectorKey = buffer.readUtf(MAX_KEY_LENGTH);
        int energyModeOrdinal = buffer.readByte();
        int energyDistributionOrdinal = buffer.readByte();
        int energyChannel = buffer.readByte();
        int energyAttachOrdinal = buffer.readByte();
        int energyTransferRate = buffer.readVarInt();
        int lastEnergyInput = buffer.readVarInt();
        int lastEnergyOutput = buffer.readVarInt();
        boolean energyTargetAccess = buffer.readBoolean();
        int bridgeTypeId = buffer.readByte();
        int bridgeChannel = buffer.readByte();
        boolean bridgeModLoaded = buffer.readBoolean();
        int bridgeEndpointCount = buffer.readVarInt();
        List<ModuleSnapshot> fluidModules = readModules(buffer);
        List<ModuleSnapshot> itemModules = readModules(buffer);
        return new ConnectorOverlaySnapshot(
                cableSide,
                mountedFaceOrdinal,
                energyConnectorKey,
                energyModeOrdinal,
                energyDistributionOrdinal,
                energyChannel,
                energyAttachOrdinal,
                energyTransferRate,
                lastEnergyInput,
                lastEnergyOutput,
                energyTargetAccess,
                bridgeTypeId,
                bridgeChannel,
                bridgeModLoaded,
                bridgeEndpointCount,
                fluidModules,
                itemModules
        );
    }

    private static void writeModules(RegistryFriendlyByteBuf buffer, List<ModuleSnapshot> modules) {
        buffer.writeByte(modules.size());
        for (ModuleSnapshot module : modules) {
            module.write(buffer);
        }
    }

    private static List<ModuleSnapshot> readModules(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readByte();
        List<ModuleSnapshot> modules = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            modules.add(ModuleSnapshot.read(buffer));
        }
        return modules;
    }

    private static void writeString(RegistryFriendlyByteBuf buffer, String value) {
        buffer.writeUtf(value == null ? "" : value, MAX_KEY_LENGTH);
    }

    public record ModuleSnapshot(
            String moduleKey,
            int modeOrdinal,
            int channel,
            int attachOrdinal,
            int cooldownTicks,
            int jamTicks,
            int shipment
    ) {
        public boolean installed() {
            return !moduleKey.isBlank();
        }

        public Direction attachAs() {
            return attachOrdinal < 0 ? null : ConnectorOverlaySnapshot.direction(attachOrdinal);
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            writeString(buffer, moduleKey);
            buffer.writeByte(modeOrdinal);
            buffer.writeByte(channel);
            buffer.writeByte(attachOrdinal);
            buffer.writeVarInt(cooldownTicks);
            buffer.writeVarInt(jamTicks);
            buffer.writeVarInt(shipment);
        }

        private static ModuleSnapshot read(RegistryFriendlyByteBuf buffer) {
            return new ModuleSnapshot(
                    buffer.readUtf(MAX_KEY_LENGTH),
                    buffer.readByte(),
                    buffer.readByte(),
                    buffer.readByte(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            );
        }
    }
}
