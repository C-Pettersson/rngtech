package com.rngtech.content.blockentity;

import com.rngtech.RNGTech;
import com.rngtech.content.menu.DebugBatteryMenu;
import com.rngtech.content.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class DebugBatteryBlockEntity extends BlockEntity implements MenuProvider {
    private static final int DATA_OUTPUT_RATE = 0;
    private static final int DEFAULT_OUTPUT_RATE = 1000;

    private final IEnergyStorage energyStorage = new DebugEnergyStorage();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == DATA_OUTPUT_RATE ? outputRate : 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_OUTPUT_RATE + 1;
        }
    };
    private int outputRate = DEFAULT_OUTPUT_RATE;
    private long lastOutputTick = Long.MIN_VALUE;
    private int outputUsedThisTick;

    public DebugBatteryBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DEBUG_BATTERY.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DebugBatteryBlockEntity battery) {
        if (!RNGTech.isDebugContentEnabled()) {
            return;
        }
        battery.exportEnergy(level, pos);
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public int outputRate() {
        return outputRate;
    }

    public void adjustOutputRate(long delta) {
        setOutputRate(clampOutput((long) outputRate + delta));
    }

    public void setOutputRate(int outputRate) {
        int clamped = clampOutput(outputRate);
        if (this.outputRate != clamped) {
            this.outputRate = clamped;
            setChanged();
        }
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return energyStorage;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.debug_battery");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DebugBatteryMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("OutputRate", outputRate);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("OutputRate")) {
            outputRate = clampOutput(tag.getInt("OutputRate"));
        }
    }

    private void exportEnergy(Level level, BlockPos pos) {
        if (availableOutput(level) <= 0) {
            return;
        }

        for (Direction direction : Direction.values()) {
            int remaining = availableOutput(level);
            if (remaining <= 0) {
                return;
            }
            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    pos.relative(direction),
                    direction.getOpposite()
            );
            if (target == null || !target.canReceive()) {
                continue;
            }
            int received = target.receiveEnergy(remaining, false);
            if (received > 0) {
                recordOutput(level, Math.min(remaining, received));
            }
        }
    }

    private int availableOutput(Level level) {
        resetOutputBudget(level);
        return Math.max(0, outputRate - outputUsedThisTick);
    }

    private void recordOutput(Level level, int amount) {
        if (amount <= 0) {
            return;
        }
        resetOutputBudget(level);
        outputUsedThisTick = (int) Math.min((long) outputRate, (long) outputUsedThisTick + amount);
    }

    private void resetOutputBudget(Level level) {
        long gameTime = level.getGameTime();
        if (lastOutputTick != gameTime) {
            lastOutputTick = gameTime;
            outputUsedThisTick = 0;
        }
    }

    private static int clampOutput(long value) {
        if (value <= 0L) {
            return 0;
        }
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private final class DebugEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0 || outputRate <= 0) {
                return 0;
            }
            int extracted = level == null ? Math.min(toExtract, outputRate) : Math.min(toExtract, availableOutput(level));
            if (!simulate && level != null) {
                recordOutput(level, extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return outputRate > 0 ? Integer.MAX_VALUE : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return outputRate > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
