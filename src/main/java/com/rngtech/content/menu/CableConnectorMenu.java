package com.rngtech.content.menu;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CableConnectorMenu extends AbstractContainerMenu {
    public static final int BUTTON_CHANNEL_DOWN = 0;
    public static final int BUTTON_CHANNEL_UP = 1;
    public static final int BUTTON_MODE = 2;
    public static final int BUTTON_DISTRIBUTION = 3;
    public static final int BUTTON_ATTACH_AS_BASE = 10;
    private static final int DATA_COUNT = CableBlockEntity.dataCount();

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final CableBlockEntity cable;
    private final Direction face;

    public CableConnectorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                Direction.values()[extraData.readByte()],
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public CableConnectorMenu(
            int containerId,
            Inventory playerInventory,
            CableBlockEntity cable,
            Direction face,
            ContainerData data
    ) {
        super(ModMenus.CABLE_CONNECTOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(cable.getLevel(), cable.getBlockPos());
        this.data = data;
        this.cable = cable;
        this.face = face;
        addDataSlots(data);
    }

    public Direction face() {
        return face;
    }

    public int channel() {
        return data.get(CableBlockEntity.dataChannelIndex());
    }

    public Direction attachAs() {
        int ordinal = data.get(CableBlockEntity.dataAttachAsIndex());
        Direction[] values = Direction.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : face.getOpposite();
    }

    public int modeOrdinal() {
        return data.get(CableBlockEntity.dataModeIndex());
    }

    public int distributionOrdinal() {
        return data.get(CableBlockEntity.dataDistributionIndex());
    }

    public int transferRate() {
        return data.get(CableBlockEntity.dataTransferRateIndex());
    }

    public boolean targetHasEnergyAccess() {
        return data.get(CableBlockEntity.dataEnergyTargetAccessIndex()) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) {
            return isKnownButton(id);
        }
        if (id == BUTTON_CHANNEL_DOWN) {
            return cable.incrementChannel(face, -1);
        }
        if (id == BUTTON_CHANNEL_UP) {
            return cable.incrementChannel(face, 1);
        }
        if (id == BUTTON_MODE) {
            return cable.cycleMode(face);
        }
        if (id == BUTTON_DISTRIBUTION) {
            return cable.cycleDistributionMode(face);
        }
        int attachAs = id - BUTTON_ATTACH_AS_BASE;
        Direction[] values = Direction.values();
        if (attachAs >= 0 && attachAs < values.length) {
            return cable.setAttachAs(face, values[attachAs]);
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof CableBlock
                        && level.getBlockEntity(pos) instanceof CableBlockEntity cableEntity
                        && cableEntity.hasConnector(face)
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static boolean isKnownButton(int id) {
        if (id == BUTTON_CHANNEL_DOWN || id == BUTTON_CHANNEL_UP || id == BUTTON_MODE || id == BUTTON_DISTRIBUTION) {
            return true;
        }
        int attachAs = id - BUTTON_ATTACH_AS_BASE;
        return attachAs >= 0 && attachAs < Direction.values().length;
    }

    private static CableBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CableBlockEntity cable) {
            return cable;
        }
        throw new IllegalStateException("Expected cable block entity at " + pos);
    }
}
