package com.rngtech.content.blockentity;

import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BatteryCellItem.CellStats;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.Supplier;

/**
 * A holder's one Battery Cell slot, read through {@link InstalledCellStats} so a steady holder builds the cell's stats
 * only when the cell is swapped, refined, emptied, or stripped. Each read takes the slot's current stack, so a change
 * shows on the next read.
 */
public final class InstalledBatteryCell {
    private final InstalledCellStats cells = new InstalledCellStats();
    private final Supplier<ItemStack> slot;
    private final IEnergyStorage storage = new CellStorage();

    /** {@code slot} gives the slot's current stack, or {@link ItemStack#EMPTY} while the slot is unusable. */
    public InstalledBatteryCell(Supplier<ItemStack> slot) {
        this.slot = slot;
    }

    /** The cell's stats, {@link CellStats#NONE} for an empty slot or a stripped or non-cell stack. */
    public CellStats stats() {
        return cells.refresh(slot.get());
    }

    public int capacity() {
        return stats().capacity();
    }

    public int energyStored() {
        ItemStack stack = slot.get();
        return BatteryCellItem.energyStored(stack, cells.refresh(stack).capacity());
    }

    /**
     * The cell's FE storage, acting as the cell's own capability would but with the cached stats, or {@code null} when
     * the slot holds no usable cell, as that capability would be.
     */
    public IEnergyStorage storage() {
        return stats() == CellStats.NONE ? null : storage;
    }

    /** How many times the cell's stats were built, for checks that a steady holder builds none. */
    public int builds() {
        return cells.builds();
    }

    private final class CellStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            ItemStack stack = slot.get();
            return BatteryCellItem.receiveEnergy(stack, cells.refresh(stack), toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            ItemStack stack = slot.get();
            return BatteryCellItem.extractEnergy(stack, cells.refresh(stack), toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity();
        }

        @Override
        public boolean canExtract() {
            return stats().maxOutput() > 0;
        }

        @Override
        public boolean canReceive() {
            return stats().maxInput() > 0;
        }
    }
}
