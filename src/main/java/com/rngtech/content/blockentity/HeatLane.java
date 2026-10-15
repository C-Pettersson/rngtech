package com.rngtech.content.blockentity;

import net.minecraft.nbt.CompoundTag;

/** One heated chamber's state: its temperature and the work done on the current cycle. {@link HeatEngine} steps it. */
public final class HeatLane {
    /** Degrees Celsius. */
    double temperature = HeatEngine.AMBIENT;
    /** Reference ticks of progress on the current cycle. */
    double work;
    /** Whether the current cycle has started, so its load dip has been applied. */
    boolean started;
    /** FE paid ahead of the heat it bought, carried between ticks. */
    double energyCredit;
    /** Burn time owed for heat already delivered, in burn ticks. */
    double burnDebt;
    /** Heat delivered last tick, in HU. */
    double lastHeat;
    /** Heat the chamber lost through its walls last tick, in HU. */
    double lastHolding;
    /** Heat the input absorbed last tick, in HU. */
    double lastAbsorbed;

    public int temperature() {
        return (int) Math.round(temperature);
    }

    public double exactTemperature() {
        return temperature;
    }

    public double work() {
        return work;
    }

    public boolean started() {
        return started;
    }

    public double lastHeat() {
        return lastHeat;
    }

    public double lastHolding() {
        return lastHolding;
    }

    public double lastAbsorbed() {
        return lastAbsorbed;
    }

    public void setTemperature(double temperature) {
        this.temperature = Math.max(HeatEngine.AMBIENT, temperature);
    }

    /** Clears the cycle; the chamber keeps its heat. */
    public boolean resetCycle() {
        boolean changed = work != 0.0 || started;
        work = 0.0;
        started = false;
        return changed;
    }

    public void save(CompoundTag tag) {
        tag.putDouble("Temperature", temperature);
        tag.putDouble("Work", work);
        tag.putBoolean("Started", started);
        tag.putDouble("EnergyCredit", energyCredit);
        tag.putDouble("BurnDebt", burnDebt);
    }

    public void load(CompoundTag tag) {
        temperature = Math.max(HeatEngine.AMBIENT, tag.getDouble("Temperature"));
        work = Math.max(0.0, tag.getDouble("Work"));
        started = tag.getBoolean("Started");
        energyCredit = Math.max(0.0, tag.getDouble("EnergyCredit"));
        burnDebt = Math.max(0.0, tag.getDouble("BurnDebt"));
    }
}
