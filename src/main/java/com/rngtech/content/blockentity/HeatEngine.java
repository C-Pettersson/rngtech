package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

/**
 * The shared heat model for the Furnace, Alloy Furnace, Metal Press, and Melter. A lane's heater pushes heat into the
 * chamber, the walls leak it, and the input absorbs it; machines pay FE or fuel for the heat they put in. Every method is
 * static so machines and the domain checks step lanes the same way.
 */
public final class HeatEngine {
    public static final int AMBIENT = 20;
    public static final double FE_PER_HU = 1.0;
    /** Heat one burn tick of Stage 0 fuel supplies. Fuel Efficiency and Fuel Duration already lengthen burn time. */
    public static final double HEAT_PER_BURN_TICK = 7.0;

    /** Wall loss in HU per tick at 1,000 °C above ambient, base Thermal Conductivity and Heat Insulation. */
    static final double LOSS_PER_TICK = 3.0;
    /** Loss grows with the cube of the temperature rise, so running hotter costs more FE per craft than it saves. */
    static final double LOSS_EXPONENT = 3.0;
    /** Heater power above holding heat at base Heat Transfer, in HU per tick, when the chamber is cold. */
    static final double APPROACH_PER_TICK = 10.0;
    /** HU that raise the chamber 1 °C at base Thermal Mass. */
    static final double HEAT_CAPACITY = 2.0;
    /** Share of the target's rise above ambient a cold item takes from the chamber when its cycle starts. */
    static final double LOAD_DIP = 0.05;
    /** Degrees shed per tick at base Cooling Rate while a lane is above the heat it may work at. */
    static final double ACTIVE_COOLING = 8.0;
    /** Wobble width per unit of push past the target, at base Temperature Stability and Thermal Mass. */
    static final double PUSH_WOBBLE = 0.5;
    /** Wobble width per point of Temperature Stability a recipe asks for beyond what the lane has. */
    static final double SHORTFALL_WOBBLE = 0.12;
    static final double MAX_SHORTFALL_WOBBLE = 0.25;
    static final int WOBBLE_PERIOD = 80;

    private HeatEngine() {
    }

    /** The recipe's heat band on this lane and the temperature the heater holds. */
    public record Envelope(
            int minimum,
            int target,
            int safeMaximum,
            int overheat,
            double requiredStability,
            int setpoint,
            boolean canFail,
            boolean coolHeaded,
            int reach
    ) {
        /** A lane hotter than this cools before it works: a hotter recipe's leftover heat, or a lowered slider. */
        public int workCeiling() {
            return Math.max(setpoint, overheat);
        }

        /** Cool Headed: a lane that has not started its cycle cools to the recipe target before it begins. */
        public Envelope coolingFirst(boolean coolHeaded) {
            return coolHeaded == this.coolHeaded || ungated() ? this
                    : new Envelope(minimum, target, safeMaximum, overheat, requiredStability, setpoint, canFail, coolHeaded, reach);
        }

        public int ceilingFor(HeatLane lane) {
            return coolHeaded && !lane.started ? target : workCeiling();
        }

        /** A recipe without a temperature requirement: it works at any heat and never runs too hot. */
        public boolean ungated() {
            return overheat == Integer.MAX_VALUE;
        }
    }

    /** Heat a lane's batch needs, and how fast it works, at a heat factor of 1. {@code dipShare} is the part of the load dip it takes. */
    public record Load(double heatPerTick, double workPerTick, int batch, double dipShare) {
        public Load(double heatPerTick, double workPerTick, int batch) {
            this(heatPerTick, workPerTick, batch, 1.0);
        }
    }

    /** What a working tick did. */
    public record Tick(boolean tooHot, boolean working, double heatFactor, double workTemperature, boolean starved) {
    }

    /** Splits a lane's FE last tick into heat lost through the walls (0), taken by the input (1), and warming the chamber (2). */
    public static double[] heatSplit(HeatLane lane) {
        double walls = Math.min(lane.lastHolding(), lane.lastHeat());
        double input = Math.min(lane.lastAbsorbed(), lane.lastHeat() - walls);
        return new double[] {walls, input, Math.max(0.0, lane.lastHeat() - walls - input)};
    }

    /** Hands the heater up to {@code heat} HU and returns what it could pay for. */
    @FunctionalInterface
    public interface HeatSupply {
        double deliver(double heat);
    }

    /** Draws up to {@code amount} FE, returning what it took. */
    @FunctionalInterface
    public interface EnergyDraw {
        int draw(int amount);
    }

    /** Burns up to {@code ticks} burn ticks of fuel, lighting new fuel as needed, and returns how many it burned. */
    @FunctionalInterface
    public interface FuelBurner {
        int burn(int ticks);
    }

    /**
     * The slider is a position on a track from the target (0) to Max Temperature (1), held back at the highest setpoint
     * the lane may reach. Unlocking more reach never moves a setpoint the lane could already hold.
     */
    public static int setpoint(int target, int maximum, int reach, double slider) {
        int top = Math.max(Math.max(target, maximum), reach);
        int wanted = target + (int) Math.round(Math.max(0.0, Math.min(1.0, slider)) * (top - target));
        return Math.max(target, Math.min(reach, wanted));
    }

    /**
     * The highest setpoint the slider reaches. Without {@code pushPastSafe} it stops at the safe maximum; with it, a
     * recipe that can fail goes up to Max Temperature and one that cannot stops at its overheat temperature.
     */
    public static int reach(int safeMaximum, int overheat, boolean canFail, boolean pushPastSafe, MachineStatAccumulator stats, int setpointCeiling) {
        int reach = !pushPastSafe ? safeMaximum : canFail ? Integer.MAX_VALUE : overheat;
        return Math.min(stats.intValue(MachineStat.MAX_TEMPERATURE), Math.min(setpointCeiling, reach));
    }

    public static Envelope envelope(
            int minimum,
            int target,
            int safeMaximum,
            double requiredStability,
            boolean canFail,
            boolean pushPastSafe,
            MachineStatAccumulator stats,
            double slider,
            int setpointCeiling
    ) {
        if (target <= AMBIENT) {
            return new Envelope(AMBIENT, AMBIENT, AMBIENT, Integer.MAX_VALUE, requiredStability, AMBIENT, false, false, AMBIENT);
        }
        int safe = Math.max(target, safeMaximum);
        int overheat = overheatTemperature(safe, stats);
        int reach = Math.max(target, reach(safe, overheat, canFail, pushPastSafe, stats, setpointCeiling));
        int setpoint = setpoint(target, stats.intValue(MachineStat.MAX_TEMPERATURE), reach, slider);
        return new Envelope(Math.min(minimum, target), target, safe, overheat, requiredStability, setpoint, canFail, false, reach);
    }

    /** The safe maximum stretched by Overheat Tolerance: above it, a recipe that can fail builds strain. */
    public static int overheatTemperature(int safeMaximum, MachineStatAccumulator stats) {
        double tolerance = Math.max(1.0, stats.value(MachineStat.OVERHEAT_TOLERANCE));
        return Math.max(safeMaximum, (int) Math.round(safeMaximum * tolerance));
    }

    /** HU that raise the chamber 1 °C. */
    public static double heatCapacity(MachineStatAccumulator stats) {
        return HEAT_CAPACITY * Math.max(0.25, stats.value(MachineStat.THERMAL_MASS));
    }

    /** Heat the walls leak per tick at {@code temperature}; the heater pays it before anything else. */
    public static double holdingHeat(double temperature, MachineStatAccumulator stats) {
        double rise = Math.max(0.0, temperature - AMBIENT) / 1000.0;
        double conductivity = Math.max(0.1, stats.value(MachineStat.THERMAL_CONDUCTIVITY));
        double insulation = Math.max(0.25, stats.value(MachineStat.HEAT_ISOLATION));
        return conductivity * LOSS_PER_TICK * Math.pow(rise, LOSS_EXPONENT) / insulation;
    }

    /** Heater power that raises the chamber, at least half its full value even at Max Temperature. */
    public static double approach(double temperature, MachineStatAccumulator stats) {
        double maximum = Math.max(AMBIENT + 1.0, stats.value(MachineStat.MAX_TEMPERATURE));
        double headroom = Math.max(0.0, Math.min(1.0, (maximum - temperature) / (maximum - AMBIENT)));
        return Math.max(0.1, stats.value(MachineStat.HEAT_TRANSFER)) * APPROACH_PER_TICK * (0.5 + 0.5 * headroom);
    }

    /** Reference ticks of work per tick at a heat factor of 1, before machine-specific speed. */
    public static double speed(MachineStatAccumulator stats) {
        return Math.max(0.1, stats.value(MachineStat.PROCESSING_SPEED)) * Math.max(0.1, stats.value(MachineStat.THERMAL_CONDUCTIVITY));
    }

    /** Degrees shed per tick on top of wall loss while a lane is above its work ceiling. */
    public static double activeCooling(MachineStatAccumulator stats) {
        return ACTIVE_COOLING * Math.max(0.1, stats.value(MachineStat.COOLING_RATE));
    }

    /** How fast the input takes heat: nothing below the minimum, slower up to the target, faster above it. */
    public static double heatFactor(double temperature, Envelope envelope) {
        if (envelope.ungated()) {
            return 1.0;
        }
        if (temperature < envelope.minimum()) {
            return 0.0;
        }
        if (temperature < envelope.target() && envelope.target() > envelope.minimum()) {
            return 0.5 + 0.5 * (temperature - envelope.minimum()) / (envelope.target() - envelope.minimum());
        }
        return 1.0 + 2.0 * Math.max(0.0, temperature - envelope.target()) / Math.max(1, envelope.target());
    }

    /** How far the working temperature swings around the lane temperature. */
    public static double wobbleAmplitude(Envelope envelope, MachineStatAccumulator stats) {
        double stability = Math.max(0.1, stats.value(MachineStat.TEMPERATURE_STABILITY));
        double mass = Math.max(0.25, stats.value(MachineStat.THERMAL_MASS));
        double push = Math.max(0, envelope.setpoint() - envelope.target()) / (double) Math.max(1, envelope.target());
        double shortfall = Math.max(0.0, envelope.requiredStability() - stability);
        return envelope.target() * (PUSH_WOBBLE * push / (stability * mass) + Math.min(MAX_SHORTFALL_WOBBLE, SHORTFALL_WOBBLE * shortfall));
    }

    /** The lane temperature plus a deterministic wave within the wobble amplitude. */
    public static double workTemperature(double temperature, Envelope envelope, MachineStatAccumulator stats, long time, int salt) {
        double amplitude = wobbleAmplitude(envelope, stats);
        if (amplitude <= 0.0) {
            return temperature;
        }
        double angle = (time + salt * 37L) * 2.0 * Math.PI / WOBBLE_PERIOD;
        return temperature + amplitude * (0.7 * Math.sin(angle) + 0.3 * Math.sin(2.3 * angle + salt));
    }

    /** Degrees a cold batch takes from the chamber when its cycle starts. */
    public static double loadDip(Envelope envelope, MachineStatAccumulator stats, int batch) {
        return LOAD_DIP * Math.max(0, envelope.target() - AMBIENT) * Math.max(1, batch) / Math.max(0.25, stats.value(MachineStat.THERMAL_MASS));
    }

    /**
     * One working tick: cool if the lane is above its work ceiling, otherwise run the heater toward the setpoint, let the
     * input absorb heat at the working temperature, and advance the cycle's work.
     */
    public static Tick work(HeatLane lane, Envelope envelope, MachineStatAccumulator stats, Load load, HeatSupply supply, long time, int salt) {
        double capacity = heatCapacity(stats);
        double holding = holdingHeat(lane.temperature, stats);
        int ceiling = envelope.ceilingFor(lane);
        if (lane.temperature > ceiling + 0.5) {
            lane.temperature = Math.max(ceiling, lane.temperature - holding / capacity - activeCooling(stats));
            record(lane, 0.0, holding, 0.0);
            return new Tick(true, false, 0.0, lane.temperature, false);
        }

        double workTemperature = workTemperature(lane.temperature, envelope, stats, time, salt);
        double factor = heatFactor(workTemperature, envelope);
        if (factor > 0.0 && !lane.started) {
            lane.started = true;
            if (lane.temperature > envelope.minimum()) {
                lane.temperature = Math.max(envelope.minimum(), lane.temperature - loadDip(envelope, stats, load.batch()) * load.dipShare());
                workTemperature = workTemperature(lane.temperature, envelope, stats, time, salt);
                factor = heatFactor(workTemperature, envelope);
            }
            holding = holdingHeat(lane.temperature, stats);
        }
        double absorbed = load.heatPerTick() * factor;
        double sustain = holding + absorbed;
        double wanted = lane.temperature < envelope.setpoint()
                ? sustain + Math.min(approach(lane.temperature, stats), (envelope.setpoint() - lane.temperature) * capacity)
                : Math.max(0.0, sustain - (lane.temperature - envelope.setpoint()) * capacity);
        double delivered = wanted <= 0.0 ? 0.0 : Math.min(wanted, supply.deliver(wanted));
        // Short of heat, the input takes only what the walls leave and works that much slower; the chamber does not sag.
        // Heat stored above the setpoint counts as available, so leftover heat still pays.
        double available = delivered + Math.max(0.0, (lane.temperature - envelope.setpoint()) * capacity);
        double share = absorbed <= 0.0 || available >= sustain ? 1.0 : Math.max(0.0, available - holding) / absorbed;
        absorbed *= share;
        lane.temperature = Math.max(AMBIENT, lane.temperature + (delivered - holding - absorbed) / capacity);
        lane.work += load.workPerTick() * factor * share;
        record(lane, delivered, holding, absorbed);
        return new Tick(false, factor > 0.0, factor, workTemperature, delivered + 1.0E-6 < Math.min(wanted, sustain));
    }

    /**
     * A lane waiting for output room: the heater keeps it at the setpoint, paying the wall loss, and no work is done, so
     * clearing the output resumes at once.
     */
    public static void hold(HeatLane lane, Envelope envelope, MachineStatAccumulator stats, HeatSupply supply) {
        double capacity = heatCapacity(stats);
        double holding = holdingHeat(lane.temperature, stats);
        double setpoint = Math.min(envelope.setpoint(), envelope.ceilingFor(lane));
        double wanted = lane.temperature < setpoint
                ? holding + Math.min(approach(lane.temperature, stats), (setpoint - lane.temperature) * capacity)
                : Math.max(0.0, holding - (lane.temperature - setpoint) * capacity);
        double delivered = wanted <= 0.0 ? 0.0 : Math.min(wanted, supply.deliver(wanted));
        lane.temperature = Math.max(AMBIENT, lane.temperature + (delivered - holding) / capacity);
        record(lane, delivered, holding, 0.0);
    }

    /** An idle lane: the heater is off and the walls leak heat, unless something holds it. */
    public static boolean idle(HeatLane lane, MachineStatAccumulator stats, boolean holdHeat) {
        double before = lane.temperature;
        double holding = holdingHeat(lane.temperature, stats);
        if (!holdHeat) {
            lane.temperature = Math.max(AMBIENT, lane.temperature - holding / heatCapacity(stats));
        }
        record(lane, 0.0, holding, 0.0);
        return Math.round(before) != Math.round(lane.temperature);
    }

    /** Whether the cycle has done {@code cycleWork} reference ticks of work. */
    public static boolean finished(HeatLane lane, double cycleWork) {
        return lane.work >= cycleWork - 1.0E-9;
    }

    /** Ticks a cycle takes at the setpoint, for display. */
    public static int cycleTicks(double cycleWork, Load load, Envelope envelope) {
        double rate = load.workPerTick() * heatFactor(envelope.setpoint(), envelope);
        return rate <= 0.0 ? 0 : Math.max(1, (int) Math.ceil(cycleWork / rate));
    }

    /** Work done so far in ticks at the setpoint, for display alongside {@link #cycleTicks}. */
    public static int elapsedTicks(HeatLane lane, Load load, Envelope envelope) {
        double rate = load.workPerTick() * heatFactor(envelope.setpoint(), envelope);
        return rate <= 0.0 ? 0 : (int) Math.floor(lane.work / rate);
    }

    /** FE per tick a lane draws while it works at its setpoint. */
    public static int steadyEnergyPerTick(Envelope envelope, Load load, MachineStatAccumulator stats) {
        double heat = holdingHeat(envelope.setpoint(), stats) + load.heatPerTick() * heatFactor(envelope.setpoint(), envelope);
        return (int) Math.ceil(heat * energyRate(stats));
    }

    /** FE a whole cycle costs at the setpoint: the recipe's heat plus what the walls leak meanwhile. */
    public static int steadyEnergyPerCycle(Envelope envelope, Load load, double cycleWork, MachineStatAccumulator stats) {
        double factor = heatFactor(envelope.setpoint(), envelope);
        double ticks = factor <= 0.0 ? 0.0 : cycleWork / (load.workPerTick() * factor);
        double heat = load.heatPerTick() * factor * ticks + holdingHeat(envelope.setpoint(), stats) * ticks;
        return (int) Math.ceil(heat * energyRate(stats));
    }

    /** FE per HU after Energy Usage. */
    public static double energyRate(MachineStatAccumulator stats) {
        return FE_PER_HU * Math.max(0.1, stats.value(MachineStat.ENERGY_USAGE));
    }

    /** Pays for heat in FE, carrying fractions of an FE between ticks so Energy Usage never rounds away. */
    public static HeatSupply electric(HeatLane lane, MachineStatAccumulator stats, EnergyDraw draw) {
        double rate = energyRate(stats);
        return heat -> {
            double needed = heat * rate;
            int request = (int) Math.ceil(Math.max(0.0, needed - lane.energyCredit));
            double available = lane.energyCredit + (request > 0 ? Math.max(0, draw.draw(request)) : 0);
            if (available + 1.0E-9 >= needed) {
                lane.energyCredit = available - needed;
                return heat;
            }
            lane.energyCredit = 0.0;
            return available / rate;
        };
    }

    /** Pays for heat in burn ticks of Stage 0 fuel, carrying fractions of a burn tick between ticks. */
    public static HeatSupply fuel(HeatLane lane, FuelBurner burner) {
        return heat -> {
            lane.burnDebt += heat / HEAT_PER_BURN_TICK;
            int owed = (int) Math.floor(lane.burnDebt);
            int burned = owed > 0 ? Math.max(0, burner.burn(owed)) : 0;
            lane.burnDebt -= burned;
            if (burned >= owed) {
                return heat;
            }
            double unpaid = lane.burnDebt;
            lane.burnDebt = 0.0;
            return Math.max(0.0, heat - unpaid * HEAT_PER_BURN_TICK);
        };
    }

    /** Pays a whole cycle's heat at once, for Instant Process; returns whether it was paid in full. */
    public static boolean payAtOnce(HeatSupply supply, double heat) {
        return supply.deliver(heat) + 1.0E-6 >= heat;
    }

    private static void record(HeatLane lane, double delivered, double holding, double absorbed) {
        lane.lastHeat = delivered;
        lane.lastHolding = holding;
        lane.lastAbsorbed = absorbed;
    }
}
