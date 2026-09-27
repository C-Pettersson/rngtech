package com.rngtech.content.menu;

import com.rngtech.content.blockentity.DebugBatteryBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DebugBatteryMenu extends AbstractContainerMenu {
    public static final int BUTTON_ZERO = 0;
    public static final int BUTTON_MAX = 1;
    public static final int BUTTON_DEC_1K = 2;
    public static final int BUTTON_INC_1K = 3;
    public static final int BUTTON_DEC_10K = 4;
    public static final int BUTTON_INC_10K = 5;
    public static final int BUTTON_DEC_100K = 6;
    public static final int BUTTON_INC_100K = 7;
    public static final int BUTTON_DEC_1M = 8;
    public static final int BUTTON_INC_1M = 9;

    private static final int DATA_OUTPUT_RATE = 0;
    private static final int DATA_COUNT = 1;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final DebugBatteryBlockEntity battery;

    public DebugBatteryMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, blockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(DATA_COUNT));
    }

    public DebugBatteryMenu(
            int containerId,
            Inventory playerInventory,
            DebugBatteryBlockEntity battery,
            ContainerData data
    ) {
        super(ModMenus.DEBUG_BATTERY.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(battery.getLevel(), battery.getBlockPos());
        this.data = data;
        this.battery = battery;
        addDataSlots(data);
    }

    public int outputRate() {
        return data.get(DATA_OUTPUT_RATE);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) {
            return id >= BUTTON_ZERO && id <= BUTTON_INC_1M;
        }

        switch (id) {
            case BUTTON_ZERO -> battery.setOutputRate(0);
            case BUTTON_MAX -> battery.setOutputRate(Integer.MAX_VALUE);
            case BUTTON_DEC_1K -> battery.adjustOutputRate(-1_000L);
            case BUTTON_INC_1K -> battery.adjustOutputRate(1_000L);
            case BUTTON_DEC_10K -> battery.adjustOutputRate(-10_000L);
            case BUTTON_INC_10K -> battery.adjustOutputRate(10_000L);
            case BUTTON_DEC_100K -> battery.adjustOutputRate(-100_000L);
            case BUTTON_INC_100K -> battery.adjustOutputRate(100_000L);
            case BUTTON_DEC_1M -> battery.adjustOutputRate(-1_000_000L);
            case BUTTON_INC_1M -> battery.adjustOutputRate(1_000_000L);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DEBUG_BATTERY.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static DebugBatteryBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof DebugBatteryBlockEntity battery) {
            return battery;
        }
        throw new IllegalStateException("Expected debug battery block entity at " + pos);
    }
}
