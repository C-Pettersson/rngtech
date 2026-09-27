package com.rngtech.util;

import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.IntSupplier;

public final class MachineEnergyStorage implements IEnergyStorage {
    private final IntSupplier capacitySupplier;
    private final IntSupplier maxReceiveSupplier;
    private final IntSupplier maxExtractSupplier;
    private final Runnable onChanged;
    private int energy;

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        this(() -> capacity, () -> maxReceive, () -> maxExtract, onChanged);
    }

    public MachineEnergyStorage(IntSupplier capacitySupplier, int maxReceive, int maxExtract, Runnable onChanged) {
        this(capacitySupplier, () -> maxReceive, () -> maxExtract, onChanged);
    }

    public MachineEnergyStorage(
            IntSupplier capacitySupplier,
            IntSupplier maxReceiveSupplier,
            IntSupplier maxExtractSupplier,
            Runnable onChanged
    ) {
        this.capacitySupplier = capacitySupplier;
        this.maxReceiveSupplier = maxReceiveSupplier;
        this.maxExtractSupplier = maxExtractSupplier;
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!canReceive() || toReceive <= 0) {
            return 0;
        }
        int received = Math.min(capacity() - getEnergyStored(), Math.min(maxReceive(), toReceive));
        if (!simulate && received > 0) {
            energy = getEnergyStored() + received;
            onChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (!canExtract() || toExtract <= 0) {
            return 0;
        }
        int extracted = Math.min(getEnergyStored(), Math.min(maxExtract(), toExtract));
        if (!simulate && extracted > 0) {
            energy = getEnergyStored() - extracted;
            onChanged.run();
        }
        return extracted;
    }

    public int consumeEnergy(int toConsume, boolean simulate) {
        if (toConsume <= 0) {
            return 0;
        }
        int consumed = Math.min(getEnergyStored(), toConsume);
        if (!simulate && consumed > 0) {
            energy = getEnergyStored() - consumed;
            onChanged.run();
        }
        return consumed;
    }

    public int generateEnergy(int toGenerate, boolean simulate) {
        if (toGenerate <= 0) {
            return 0;
        }
        int generated = Math.min(capacity() - getEnergyStored(), toGenerate);
        if (!simulate && generated > 0) {
            energy = getEnergyStored() + generated;
            onChanged.run();
        }
        return generated;
    }

    @Override
    public int getEnergyStored() {
        return Math.min(energy, capacity());
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity();
    }

    @Override
    public boolean canExtract() {
        return maxExtract() > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive() > 0;
    }

    public void setEnergy(int energy) {
        int clamped = Math.max(0, Math.min(capacity(), energy));
        if (this.energy != clamped) {
            this.energy = clamped;
            onChanged.run();
        }
    }

    private int capacity() {
        return Math.max(0, capacitySupplier.getAsInt());
    }

    private int maxReceive() {
        return Math.max(0, maxReceiveSupplier.getAsInt());
    }

    private int maxExtract() {
        return Math.max(0, maxExtractSupplier.getAsInt());
    }
}
