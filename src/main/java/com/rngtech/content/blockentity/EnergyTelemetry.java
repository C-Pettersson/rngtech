package com.rngtech.content.blockentity;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.Supplier;

public final class EnergyTelemetry {
    private final Supplier<Level> level;
    private long tick = Long.MIN_VALUE;
    private int inputThisTick;
    private int outputThisTick;
    private int lastInput;
    private int lastOutput;

    public EnergyTelemetry(Supplier<Level> level) {
        this.level = level;
    }

    public IEnergyStorage track(IEnergyStorage storage) {
        return new TrackedEnergyStorage(storage);
    }

    public void recordInput(int amount) {
        if (amount > 0 && roll()) {
            inputThisTick += amount;
        }
    }

    public void recordOutput(int amount) {
        if (amount > 0 && roll()) {
            outputThisTick += amount;
        }
    }

    public int lastInput() {
        roll();
        return lastInput;
    }

    public int lastOutput() {
        roll();
        return lastOutput;
    }

    private boolean roll() {
        Level currentLevel = level.get();
        if (currentLevel == null) {
            return false;
        }
        long now = currentLevel.getGameTime();
        if (now != tick) {
            boolean previousTick = now == tick + 1;
            lastInput = previousTick ? inputThisTick : 0;
            lastOutput = previousTick ? outputThisTick : 0;
            inputThisTick = 0;
            outputThisTick = 0;
            tick = now;
        }
        return true;
    }

    private final class TrackedEnergyStorage implements IEnergyStorage {
        private final IEnergyStorage delegate;

        private TrackedEnergyStorage(IEnergyStorage delegate) {
            this.delegate = delegate;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = delegate.receiveEnergy(toReceive, simulate);
            if (!simulate) {
                recordInput(received);
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = delegate.extractEnergy(toExtract, simulate);
            if (!simulate) {
                recordOutput(extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return delegate.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return delegate.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return delegate.canExtract();
        }

        @Override
        public boolean canReceive() {
            return delegate.canReceive();
        }
    }
}
