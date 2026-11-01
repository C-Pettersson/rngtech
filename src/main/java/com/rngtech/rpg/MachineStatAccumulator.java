package com.rngtech.rpg;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.energy.SolidFuelBurnerChassis;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.recycling.ComponentRecyclerChassis;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class MachineStatAccumulator {
    public static final double FURNACE_BASE_MAX_TEMPERATURE = 600.0;
    public static final double PRIMITIVE_FURNACE_MAX_TEMPERATURE = 800.0;
    public static final Component NO_BATTERY_SOURCE = Component.translatable("rngtech.stat.breakdown.source.no_battery");
    private static final ThreadLocal<Boolean> RECORDING = ThreadLocal.withInitial(() -> false);
    private static final Component BASE_SOURCE = Component.translatable("rngtech.stat.breakdown.source.base");
    private static final Component RARITY_SOURCE = Component.translatable("rngtech.stat.breakdown.source.rarity");
    private static final Component FALLBACK_SOURCE = Component.translatable("rngtech.stat.breakdown.source.machine");
    private static final Component CRUSHER_SOURCE = Component.translatable("rngtech.stat.breakdown.source.crusher");
    private static final Component OUTPUT_CAP_SOURCE = Component.translatable("rngtech.stat.breakdown.source.output_cap");
    private static final Source NO_SOURCE = () -> {
    };
    /** Increased Bonus Output pays no more than Maximum Bonus Output, which starts here and flat additions raise. */
    public static final double BASE_MAX_OUTPUT_AMOUNT = 75.0;
    /** Maximum Bonus Output itself stops here, however many +1% sources a machine stacks. */
    public static final double MAX_OUTPUT_AMOUNT_CEILING = 90.0;
    /** Two top-tier Super Output rolls plus the Assayer would reach 26%; the Crusher stops at 25%. */
    public static final double CRUSHER_SUPER_OUTPUT_CEILING = 25.0;
    /** Stats whose reductions divide instead of emptying the increased bucket, so no single roll reaches the floor. */
    private static final Set<MachineStat> DIVIDING_REDUCTION_STATS = EnumSet.of(MachineStat.ENERGY_USAGE);

    private final Map<MachineStat, Double> baseValues = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> additiveValues = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> increasedPercentValues = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> reducedPercentValues = new EnumMap<>(MachineStat.class);
    /** Stats whose increased bucket pays no more than another stat's value, keyed by the capped stat. */
    private final Map<MachineStat, MachineStat> increasedCaps = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> moreValues = new EnumMap<>(MachineStat.class);
    private double flatEnergyGenerationBonus;
    private double partEnergyGenerationMore = 1.0;
    private final Map<MachineStat, Double> absoluteValues = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> absoluteCeilings = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> absoluteFloors = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, Double> bonusMoreValues = new EnumMap<>(MachineStat.class);
    /** Stats whose increased and reduced bucket pays into another stat instead, keyed by the converted stat. */
    private final Map<MachineStat, IncreasedConversion> increasedConversions = new EnumMap<>(MachineStat.class);
    private final Map<MachineStat, List<StatBreakdown.Term>> recorded =
            RECORDING.get() ? new EnumMap<>(MachineStat.class) : null;
    private final Deque<Component> sources = new ArrayDeque<>();
    private String ascendancy = "";
    private boolean frozen;
    /** Heated machines shed heat actively, so a Cooling Rate reduction rolled for the old idle-cooling meaning counts as an increase. */
    private boolean activeCooling;

    /**
     * Runs {@code statsFactory} with breakdown recording on: every accumulator it creates remembers which labelled
     * source contributed each term, so {@link #breakdown} can explain the final value.
     */
    public static MachineStatAccumulator recording(Supplier<MachineStatAccumulator> statsFactory) {
        boolean previous = RECORDING.get();
        RECORDING.set(true);
        try {
            return statsFactory.get();
        } finally {
            RECORDING.set(previous);
        }
    }

    /** Whether a stat breakdown is recording on this thread, so cached stats would lose their sources. */
    public static boolean isRecording() {
        return RECORDING.get();
    }

    /** Makes these stats read-only, so a cached instance can be shared; any later change throws. */
    public MachineStatAccumulator freeze() {
        frozen = true;
        return this;
    }

    /** A copy that can take more effects, for callers that add conditional effects to shared stats. */
    public MachineStatAccumulator mutableCopy() {
        MachineStatAccumulator copy = new MachineStatAccumulator();
        copy.baseValues.putAll(baseValues);
        copy.additiveValues.putAll(additiveValues);
        copy.increasedPercentValues.putAll(increasedPercentValues);
        copy.reducedPercentValues.putAll(reducedPercentValues);
        copy.increasedCaps.putAll(increasedCaps);
        copy.moreValues.putAll(moreValues);
        copy.flatEnergyGenerationBonus = flatEnergyGenerationBonus;
        copy.partEnergyGenerationMore = partEnergyGenerationMore;
        copy.absoluteValues.putAll(absoluteValues);
        copy.absoluteCeilings.putAll(absoluteCeilings);
        copy.absoluteFloors.putAll(absoluteFloors);
        copy.bonusMoreValues.putAll(bonusMoreValues);
        copy.increasedConversions.putAll(increasedConversions);
        if (copy.recorded != null && recorded != null) {
            recorded.forEach((stat, terms) -> copy.recorded.put(stat, new ArrayList<>(terms)));
        }
        copy.ascendancy = ascendancy;
        copy.activeCooling = activeCooling;
        return copy;
    }

    /** These stats when they can take more effects, otherwise a {@link #mutableCopy}. */
    public MachineStatAccumulator mutable() {
        return frozen ? mutableCopy() : this;
    }

    private void checkMutable() {
        if (frozen) {
            throw new IllegalStateException("Cached machine stats are read-only; add effects to mutable() instead");
        }
    }

    /** Labels every contribution made until the returned scope closes; the innermost label wins. */
    public Source source(Component label) {
        if (recorded == null) {
            return NO_SOURCE;
        }
        sources.push(label);
        return sources::pop;
    }

    public interface Source extends AutoCloseable {
        @Override
        void close();
    }

    /** Records the host's chosen ascendancy, so Gear stats that need one apply only on its machines. */
    public void setAscendancy(String ascendancy) {
        checkMutable();
        this.ascendancy = ascendancy == null ? "" : ascendancy;
    }

    /** Whether the host chose {@code required}; an empty requirement always holds. */
    public boolean hasAscendancy(String required) {
        return required.isEmpty() || required.equals(ascendancy);
    }

    public void setAbsolute(MachineStat stat, double value) {
        checkMutable();
        absoluteValues.merge(accumulationStat(stat), value, Math::min);
        record(StatBreakdown.Kind.FIXED, stat, value);
    }

    public void capAbsolute(MachineStat stat, double value) {
        checkMutable();
        absoluteCeilings.merge(accumulationStat(stat), value, Math::min);
        record(StatBreakdown.Kind.CEILING, stat, value);
    }

    /** Keeps {@code stat} at or above {@code value}; the part above the floor is the stat's bonus. */
    public void floorAbsolute(MachineStat stat, double value) {
        checkMutable();
        absoluteFloors.merge(accumulationStat(stat), value, Math::max);
        record(StatBreakdown.Kind.FLOOR, stat, value);
    }

    /** Scales only the part of {@code stat} above its floor, so a penalty can take away bonus but never the floor. */
    public void scaleBonus(MachineStat stat, double factor) {
        checkMutable();
        bonusMoreValues.merge(accumulationStat(stat), factor, (current, next) -> current * next);
        record(StatBreakdown.Kind.BONUS_MORE, stat, factor);
    }

    /**
     * Converts {@code from}'s whole increased and reduced bucket into flat {@code to}: {@code perPercent} of {@code to}
     * for each net 1% increased, or less of it for a net reduction. The conversion reads the bucket when a value is
     * asked for, so increases and reductions applied after this call are converted too.
     */
    public void convertIncreased(MachineStat from, MachineStat to, double perPercent) {
        checkMutable();
        Component source = sources.isEmpty() ? FALLBACK_SOURCE : sources.peek();
        increasedConversions.put(accumulationStat(from), new IncreasedConversion(accumulationStat(to), perPercent, source));
        if (recorded != null) {
            recorded.computeIfAbsent(accumulationStat(from), ignored -> new ArrayList<>())
                    .add(new StatBreakdown.Term(StatBreakdown.Kind.CONVERTED, perPercent, source, accumulationStat(to).ordinal()));
        }
    }

    private record IncreasedConversion(MachineStat to, double perPercent, Component source) {
    }

    /** What every conversion into {@code to} adds to it. */
    private double convertedInto(MachineStat to) {
        double converted = 0.0;
        for (Map.Entry<MachineStat, IncreasedConversion> entry : increasedConversions.entrySet()) {
            if (entry.getValue().to() == to) {
                converted += entry.getValue().perPercent() * increasedPercentValues.getOrDefault(entry.getKey(), 0.0);
            }
        }
        return converted;
    }

    public Optional<StatBreakdown> breakdown(MachineStat stat) {
        if (recorded == null) {
            return Optional.empty();
        }
        MachineStat resolvedStat = accumulationStat(stat);
        List<StatBreakdown.Term> terms = new ArrayList<>();
        double base = baseValues.getOrDefault(resolvedStat, 0.0);
        if (base != 0.0) {
            terms.add(new StatBreakdown.Term(StatBreakdown.Kind.BASE, base, BASE_SOURCE));
        }
        terms.addAll(recorded.getOrDefault(resolvedStat, List.of()));
        // The cap is another stat's final value, so it is resolved here rather than when the rule was recorded.
        MachineStat capStat = increasedCaps.get(resolvedStat);
        if (capStat != null) {
            terms.add(new StatBreakdown.Term(StatBreakdown.Kind.INCREASED_CAP, value(capStat), Component.translatable(capStat.translationKey())));
        }
        increasedConversions.forEach((from, conversion) -> {
            double converted = conversion.perPercent() * increasedPercentValues.getOrDefault(from, 0.0);
            if (conversion.to() == resolvedStat && converted != 0.0) {
                terms.add(new StatBreakdown.Term(StatBreakdown.Kind.ADD, converted, conversion.source()));
            }
        });
        return Optional.of(new StatBreakdown(resolvedStat, terms, value(resolvedStat)));
    }

    /** Breakdowns for every stat with a base or a recorded contribution. */
    public List<StatBreakdown> breakdowns() {
        if (recorded == null) {
            return List.of();
        }
        List<StatBreakdown> breakdowns = new ArrayList<>();
        for (MachineStat stat : MachineStat.values()) {
            if (stat == accumulationStat(stat)
                    && (baseValues.getOrDefault(stat, 0.0) != 0.0 || recorded.containsKey(stat))) {
                breakdown(stat).ifPresent(breakdowns::add);
            }
        }
        return breakdowns;
    }

    public static MachineStatAccumulator fromRanges(List<MachineStatRange> ranges, RandomSource random) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        stats.rollBaseValues(ranges, random);
        return stats;
    }

    public static MachineStatAccumulator componentBase(Map<MachineStat, Double> values) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        stats.baseValues.putAll(values);
        return stats;
    }

    public static MachineStatAccumulator crusherBase(CrusherChassisMaterial chassis) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        stats.baseValues.put(MachineStat.INPUT_SLOTS, (double) RNGTechConfig.CRUSHER_BASE_INPUT_SLOTS.get());
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        // A multiplier on the configured under-level jam chance, so "less Jam Chance" has something to scale.
        stats.baseValues.put(MachineStat.JAM_CHANCE, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, RNGTechConfig.CRUSHER_ENERGY_CAPACITY.get() * chassis.energyCapacity());
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, RNGTechConfig.CRUSHER_MAX_ENERGY_INPUT.get() * chassis.energyTransfer());
        stats.baseValues.put(MachineStat.EFFICIENCY, chassis.efficiency());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, chassis.processingSpeed());
        stats.baseValues.put(MachineStat.OUTPUT_AMOUNT, chassis.outputAmount());
        stats.withCrusherYieldRules();
        stats.baseValues.put(
                MachineStat.BATCH_SIZE,
                switch (chassis) {
                    case TUNGSTENSTEEL -> 4.0;
                    case EXOTIC -> 9.0;
                    default -> 1.0;
                }
        );
        return stats;
    }

    /** The Crusher's yield rules: the Bonus Output cap and a Super Output ceiling. */
    MachineStatAccumulator withCrusherYieldRules() {
        withOutputCap();
        try (Source ignored = source(CRUSHER_SOURCE)) {
            capAbsolute(MachineStat.SUPER_OUTPUT_CHANCE, CRUSHER_SUPER_OUTPUT_CEILING);
        }
        return this;
    }

    /**
     * The Bonus Output cap, for every machine that reads Output Amount: its increased bucket pays no more than Maximum
     * Bonus Output, which starts at {@link #BASE_MAX_OUTPUT_AMOUNT} and stops at {@link #MAX_OUTPUT_AMOUNT_CEILING}.
     */
    MachineStatAccumulator withOutputCap() {
        checkMutable();
        baseValues.put(MachineStat.MAX_OUTPUT_AMOUNT, BASE_MAX_OUTPUT_AMOUNT);
        try (Source ignored = source(OUTPUT_CAP_SOURCE)) {
            capAbsolute(MachineStat.MAX_OUTPUT_AMOUNT, MAX_OUTPUT_AMOUNT_CEILING);
        }
        increasedCaps.put(MachineStat.OUTPUT_AMOUNT, MachineStat.MAX_OUTPUT_AMOUNT);
        return this;
    }

    /** Whether reductions on {@code stat} divide instead of subtracting from the increased bucket. */
    public static boolean dividesReductions(MachineStat stat) {
        return DIVIDING_REDUCTION_STATS.contains(accumulationStat(stat));
    }

    /** An increased bucket under a cap of {@code capPercent}: the bucket itself up to the cap, the cap beyond it. */
    public static double capped(double increasedPercent, double capPercent) {
        return Math.min(increasedPercent, capPercent);
    }

    /** The scale of a dividing stat: increases multiply, reductions divide. */
    public static double dividedScale(double increasesPercent, double reductionsPercent) {
        return (1.0 + increasesPercent / 100.0) / (1.0 + reductionsPercent / 100.0);
    }

    public static MachineStatAccumulator solidFuelBurnerBase(int transferRate, double stability) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.STABILITY, stability);
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.FUEL_DURATION, 1.0);
        stats.baseValues.put(MachineStat.HEAT_ISOLATION, 1.0);
        return stats;
    }

    public static MachineStatAccumulator solidFuelBurnerBase(SolidFuelBurnerChassis chassis) {
        return solidFuelBurnerBase(chassis.transferRate(), chassis.stability());
    }

    public static MachineStatAccumulator bioGeneratorBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.POTATO_POWER, 1.0);
        stats.baseValues.put(MachineStat.CARROT_POWER, 1.0);
        stats.baseValues.put(MachineStat.BREAD_POWER, 1.0);
        stats.baseValues.put(MachineStat.SAPLING_POWER, 1.0);
        stats.baseValues.put(MachineStat.SEED_POWER, 1.0);
        stats.baseValues.put(MachineStat.PLANT_POWER, 1.0);
        stats.baseValues.put(MachineStat.ORGANIC_REAGENT_POWER, 1.0);
        stats.baseValues.put(MachineStat.COMPOSTED_BIOMASS_POWER, 1.0);
        stats.baseValues.put(MachineStat.ALGAE_POWER, 1.0);
        stats.baseValues.put(MachineStat.RICH_BIOMASS_POWER, 1.0);
        stats.baseValues.put(MachineStat.FUEL_DURATION, 1.0);
        return stats;
    }

    public static MachineStatAccumulator bioOrganicProcessorBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 2.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator solarPanelBase(SolarPanelMaterial material) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) material.energyCapacity());
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) material.transferRate());
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, (double) material.clearGeneration());
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PEAK_SOLAR_GENERATION, 1.0);
        return stats;
    }

    public static MachineStatAccumulator solarArrayControllerBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.BATTERY_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.SOLAR_PANEL_LIMIT, 1.0);
        stats.baseValues.put(MachineStat.MOONLIGHT_CONVERSION, 0.0);
        stats.baseValues.put(MachineStat.WEATHER_RECOVERY, 0.0);
        stats.baseValues.put(MachineStat.SOLAR_PANEL_SYNCHRONIZATION, 0.0);
        stats.baseValues.put(MachineStat.SOLAR_PANEL_ARBITRATION, 0.0);
        stats.baseValues.put(MachineStat.OVERFLOW_SHUNTING, 0.0);
        stats.baseValues.put(MachineStat.CLEAR_SKY_AMPLIFICATION, 0.0);
        stats.baseValues.put(MachineStat.LUNAR_INVERSION, 0.0);
        return stats;
    }

    public static MachineStatAccumulator potentialReactorBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator corrosionCellBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 2.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 0.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 4000.0);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator ammoniaSynthesizerBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 1000.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 8000.0);
        return stats;
    }

    public static MachineStatAccumulator ammoniaFuelCellBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 1000.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 8000.0);
        return stats;
    }

    public static MachineStatAccumulator generatorProcessingBase(long energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator vacuumCollapseGeneratorBase(long energyCapacity, int transferRate) {
        MachineStatAccumulator stats = generatorProcessingBase(energyCapacity, transferRate);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        return stats;
    }

    /** {@code PhaseArrayRules}: the Emitter's charge store and rate, and the line's base Partial window. */
    public static MachineStatAccumulator phaseEmitterBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyPhaseArrayZeroes(stats);
        stats.baseValues.put(MachineStat.CHARGE_CAPACITY, (double) PhaseArrayRules.emitterChargeCapacity());
        stats.baseValues.put(MachineStat.CHARGE_RATE, (double) PhaseArrayRules.emitterChargeRate());
        stats.baseValues.put(MachineStat.PARTIAL_WINDOW, (double) PhaseArrayRules.basePartialWindow());
        return stats;
    }

    /** {@code PhaseArrayRules}: a Coil's Hold Chance and its share of charge. */
    public static MachineStatAccumulator phaseCoilBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyPhaseArrayZeroes(stats);
        stats.baseValues.put(MachineStat.CHARGE_CAPACITY, (double) PhaseArrayRules.coilChargeCapacity());
        stats.baseValues.put(MachineStat.CHARGE_RATE, (double) PhaseArrayRules.coilChargeRate());
        stats.baseValues.put(MachineStat.HOLD_CHANCE, PhaseArrayRules.coilHoldChance());
        stats.baseValues.put(MachineStat.OVERHOLD_CHANCE, PhaseArrayRules.coilOverholdChance());
        return stats;
    }

    private static void applyPhaseArrayZeroes(MachineStatAccumulator stats) {
        for (MachineStat stat : PhaseArrayRules.LINE_STATS) {
            stats.baseValues.put(stat, 0.0);
        }
    }

    public static MachineStatAccumulator componentRecyclerBase(ComponentRecyclerChassis chassis) {
        MachineStatAccumulator stats = componentRecyclerBase(chassis.energyCapacity(), chassis.energyTransfer());
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, chassis.processingSpeed());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        stats.baseValues.put(MachineStat.EFFICIENCY, chassis.efficiency());
        stats.baseValues.put(MachineStat.STABILITY, chassis.stability());
        if (chassis.manual()) {
            stats.baseValues.put(MachineStat.PROCESSING_LEVEL, (double) chassis.stage());
            stats.baseValues.put(MachineStat.ENERGY_CAPACITY, 0.0);
            stats.baseValues.put(MachineStat.ENERGY_TRANSFER, 0.0);
        }
        return stats;
    }

    public static MachineStatAccumulator componentRecyclerBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 3.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        return stats;
    }

    public static MachineStatAccumulator metalPressBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        applyHeatedMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 0.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.OVERHEAT_TOLERANCE, 1.0);
        return stats;
    }

    public static MachineStatAccumulator alloyFurnaceBase(AlloyFurnaceChassisMaterial chassis) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        applyHeatedMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) chassis.energyCapacity());
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) chassis.energyTransfer());
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, chassis.processingSpeed());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.HEAT_TRANSFER, chassis.heatTransfer());
        stats.baseValues.put(MachineStat.THERMAL_CONDUCTIVITY, chassis.heatTransfer());
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 300.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, chassis.stability());
        return stats;
    }

    public static MachineStatAccumulator melterBase(int energyCapacity, int transferRate, int fluidTransferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        applyHeatedMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 2.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) fluidTransferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 0.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        // Each Melter tank holds four buckets; the Pressure Vessel raises it.
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 4000.0);
        return stats;
    }

    public static MachineStatAccumulator batteryAssemblerBase(int energyCapacity, int transferRate, int fluidTransferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 4.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 4.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) fluidTransferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 4000.0);
        return stats;
    }

    public static MachineStatAccumulator gasChemistryProcessorBase(int energyCapacity, int transferRate, int fluidTransferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) fluidTransferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 0.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 8000.0);
        return stats;
    }

    public static MachineStatAccumulator gasCombustorBase(int energyCapacity, int transferRate, int fluidTransferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) fluidTransferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 8000.0);
        return stats;
    }

    public static MachineStatAccumulator fluidHeatGeneratorBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, 0.0);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 1000.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 4000.0);
        return stats;
    }

    public static MachineStatAccumulator compressorTankBase(CompressorTankMaterial material) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.ADDON_SLOTS, 5.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) material.energyCapacity());
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) material.energyTransfer());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, (double) material.baseRate());
        stats.baseValues.put(
                MachineStat.FLUID_CAPACITY,
                (material.looseBuckets() + material.compressedPhysicalBuckets() * material.compressionRatio())
                        * 1000.0
        );
        stats.baseValues.put(MachineStat.COMPRESSION_RATIO, (double) material.compressionRatio());
        return stats;
    }

    public static MachineStatAccumulator resonanceCalibratorBase(int energyCapacity, int transferRate) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.INPUT_SLOTS, 4.0);
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, (double) energyCapacity);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.CALIBRATION_QUALITY, 1.0);
        stats.baseValues.put(MachineStat.CALIBRATION_PRECISION, 1.0);
        stats.baseValues.put(MachineStat.CATALYST_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.REFINEMENT_POTENTIAL_BONUS, 0.0);
        return stats;
    }

    public static MachineStatAccumulator resonanceCalibratorBase(ResonanceCalibratorChassis chassis) {
        MachineStatAccumulator stats = resonanceCalibratorBase(chassis.energyCapacity(), chassis.energyTransfer());
        stats.baseValues.put(MachineStat.BATCH_SIZE, (double) chassis.lanes());
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, chassis.processingSpeed());
        stats.baseValues.put(MachineStat.EFFICIENCY, chassis.efficiency());
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        stats.baseValues.put(MachineStat.STABILITY, chassis.stability());
        stats.baseValues.put(MachineStat.CALIBRATION_QUALITY, 1.0 + chassis.qualityPercent() / 100.0);
        stats.baseValues.put(MachineStat.CALIBRATION_PRECISION, 1.0 + chassis.precisionPercent() / 100.0);
        stats.baseValues.put(MachineStat.REFINEMENT_POTENTIAL_BONUS, (double) chassis.refinementPotentialBonus());
        return stats;
    }

    public static MachineStatAccumulator furnaceBase() {
        return furnaceBase(FurnaceChassisMaterial.PRIMITIVE);
    }

    public static MachineStatAccumulator furnaceBase(FurnaceChassisMaterial chassis) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        applyBaseFurnaceValues(stats);
        applyHeatedMachineValues(stats);
        stats.baseValues.put(
                MachineStat.INPUT_SLOTS,
                (double) Math.max(RNGTechConfig.FURNACE_BASE_INPUT_SLOTS.get(), chassis.processingSlots())
        );
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, (double) chassis.processingSlots());
        stats.baseValues.put(MachineStat.ADDON_SLOTS, (double) chassis.heatCoreSlots());
        stats.baseValues.put(MachineStat.HEAT_TRANSFER, chassis.heatTransfer());
        stats.baseValues.put(MachineStat.THERMAL_CONDUCTIVITY, chassis.heatTransfer());
        stats.baseValues.put(
                MachineStat.PROCESSING_SPEED,
                chassis == FurnaceChassisMaterial.BRONZE ? 1.0 : chassis.processingSpeed()
        );
        stats.baseValues.put(MachineStat.EFFICIENCY, chassis.electric() ? 1.0 : chassis.efficiency());
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, chassis.fuelEfficiency());
        stats.baseValues.put(MachineStat.FUEL_DURATION, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, chassis.electric() ? (double) chassis.energyCapacity() : 0.0);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, chassis.electric() ? (double) chassis.energyTransfer() : 0.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, chassis.energyUsage());
        if (chassis == FurnaceChassisMaterial.PRIMITIVE) {
            stats.baseValues.put(MachineStat.MAX_TEMPERATURE, PRIMITIVE_FURNACE_MAX_TEMPERATURE);
        }
        return stats;
    }

    public static MachineStatAccumulator batteryBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        return stats;
    }

    public static MachineStatAccumulator batteryChassisBase(BatteryChassisMaterial material) {
        return batteryChassisBase(
                material.slots(),
                material.transferRate(),
                material.burstTransfer(),
                material.burstDuration(),
                material.efficiency(),
                material.stability(),
                material.idleLossPercentPerMinute(),
                material.globalModifierStrength()
        );
    }

    public static MachineStatAccumulator batteryCellBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, 0.0);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.IDLE_LOSS, 0.0);
        return stats;
    }

    public static MachineStatAccumulator minersCompanionBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.BLOCK_FILTER_SLOTS, 3.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        return stats;
    }

    public static MachineStatAccumulator forestryCompanionBase() {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.CART_SPEED, 1.0);
        stats.baseValues.put(MachineStat.WORK_RANGE, 1.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 8000.0);
        return stats;
    }

    public static MachineStatAccumulator batteryChassisBase(
            int slots,
            int transferRate,
            double burstTransfer,
            int burstDuration,
            double efficiency,
            double stability,
            double idleLossPercentPerMinute,
            double globalModifierStrength
    ) {
        MachineStatAccumulator stats = new MachineStatAccumulator();
        applyBaseMachineValues(stats);
        stats.baseValues.put(MachineStat.BATTERY_SLOTS, (double) slots);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, (double) transferRate);
        stats.baseValues.put(MachineStat.BURST_TRANSFER, burstTransfer);
        stats.baseValues.put(MachineStat.BURST_DURATION, (double) burstDuration);
        stats.baseValues.put(MachineStat.EFFICIENCY, efficiency);
        stats.baseValues.put(MachineStat.STABILITY, stability);
        stats.baseValues.put(MachineStat.IDLE_LOSS, idleLossPercentPerMinute);
        stats.baseValues.put(MachineStat.GLOBAL_MODIFIER_STRENGTH, globalModifierStrength);
        return stats;
    }

    private static void applyBaseMachineValues(MachineStatAccumulator stats) {
        stats.baseValues.put(MachineStat.OUTPUT_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.ADDON_SLOTS, 1.0);
        stats.baseValues.put(MachineStat.OUTPUT_AMOUNT, 1.0);
        try (Source ignored = stats.source(BASE_SOURCE)) {
            stats.floorAbsolute(MachineStat.OUTPUT_AMOUNT, 1.0);
        }
        stats.baseValues.put(MachineStat.SUPER_OUTPUT_CHANCE, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_SPEED, 1.0);
        stats.baseValues.put(MachineStat.INSTANT_PROCESS_CHANCE, 0.0);
        stats.baseValues.put(MachineStat.PROCESSING_LEVEL, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_USAGE, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_CAPACITY, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_GENERATION, 1.0);
        stats.baseValues.put(MachineStat.ENERGY_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.BATCH_SIZE, 1.0);
        stats.baseValues.put(MachineStat.BATCH_OVERHEAD, BatchProcessing.BASE_BATCH_OVERHEAD);
        stats.baseValues.put(MachineStat.BUFFER_SIZE, 1.0);
        stats.baseValues.put(MachineStat.STABILITY, 1.0);
        stats.baseValues.put(MachineStat.UPGRADE_LIMIT, 1.0);
        stats.baseValues.put(MachineStat.REFINEMENT_POTENTIAL, 0.0);
        stats.baseValues.put(MachineStat.BURST_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.BURST_DURATION, 0.0);
        stats.baseValues.put(MachineStat.IDLE_LOSS, 0.0);
        stats.baseValues.put(MachineStat.BATTERY_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.GLOBAL_MODIFIER_STRENGTH, 1.0);
        stats.baseValues.put(MachineStat.FLUID_CAPACITY, 0.0);
        stats.baseValues.put(MachineStat.COMPRESSION_RATIO, 1.0);
        stats.baseValues.put(MachineStat.FLUID_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.CALIBRATION_QUALITY, 1.0);
        stats.baseValues.put(MachineStat.CALIBRATION_PRECISION, 1.0);
        stats.baseValues.put(MachineStat.CATALYST_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.REFINEMENT_POTENTIAL_BONUS, 0.0);
        stats.baseValues.put(MachineStat.OUTPUT_GUARD_GRACE, 0.0);
        stats.baseValues.put(MachineStat.NO_BATTERY_OUTPUT_RETENTION, 0.0);
        stats.baseValues.put(MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION, 0.0);
        stats.baseValues.put(MachineStat.CRUSHER_INPUT_FILTER, 0.0);
        stats.baseValues.put(MachineStat.CRUSHER_SALVAGE_CHANCE, 0.0);
        stats.baseValues.put(MachineStat.BLOCK_FILTER_SLOTS, 0.0);
        stats.baseValues.put(MachineStat.CONTROL, 0.0);
        stats.baseValues.put(MachineStat.DRIVE, 0.0);
        stats.baseValues.put(MachineStat.RESERVE, 0.0);
    }

    private static void applyBaseFurnaceValues(MachineStatAccumulator stats) {
        stats.baseValues.put(MachineStat.HEAT_TRANSFER, 1.0);
        stats.baseValues.put(MachineStat.HEAT_ISOLATION, 1.0);
        stats.baseValues.put(MachineStat.MAX_TEMPERATURE, FURNACE_BASE_MAX_TEMPERATURE);
        stats.baseValues.put(MachineStat.TEMPERATURE_STABILITY, 1.0);
        stats.baseValues.put(MachineStat.FUEL_EFFICIENCY, 1.0);
        stats.baseValues.put(MachineStat.THERMAL_MASS, 1.0);
        stats.baseValues.put(MachineStat.COOLING_RATE, 1.0);
        stats.baseValues.put(MachineStat.OVERHEAT_TOLERANCE, 1.0);
    }

    public void apply(ModifierSet set) {
        for (MachineModifier modifier : set.modifiers()) {
            if (recorded == null || modifier.slot().isAffix()) {
                apply(modifier);
                continue;
            }
            try (Source ignored = source(MachineModifierText.slotLabel(modifier.slot()))) {
                apply(modifier);
            }
        }
        try (Source ignored = source(RARITY_SOURCE)) {
            add(MachineStat.REFINEMENT_POTENTIAL, set.refinementPotential());
        }
    }

    public void apply(MachineTraits traits) {
        apply(traits.modifierSet());
    }

    /** Applies {@code modifier} under {@code source}, so a stat breakdown names where it came from. */
    public void apply(Component source, MachineModifier modifier) {
        try (Source ignored = source(source)) {
            apply(modifier);
        }
    }

    public void apply(MachineModifier modifier) {
        Component label = recorded == null ? null : modifierSource(modifier);
        if (label != null) {
            sources.push(label);
        }
        try {
            for (MachineModifierEffect effect : modifier.effects()) {
                apply(effect);
                if (isFlatEnergyGenerationAffix(modifier, effect)) {
                    addFlatEnergyGenerationBonus(effect.value());
                }
            }
        } finally {
            if (label != null) {
                sources.pop();
            }
        }
    }

    /** Affixes name themselves; anonymous modifiers inherit the enclosing {@link #source} label. */
    private static Component modifierSource(MachineModifier modifier) {
        if (!modifier.slot().isAffix()) {
            return null;
        }
        return Component.translatable(
                "rngtech.stat.breakdown.source.affix",
                MachineModifierText.displayName(modifier),
                MachineModifierText.slotLabel(modifier.slot())
        );
    }

    private void apply(MachineModifierEffect effect) {
        if (activeCooling && effect.stat() == MachineStat.COOLING_RATE) {
            applyActiveCooling(effect);
            return;
        }
        switch (effect.operation()) {
            case ADD -> add(effect.stat(), effect.value());
            case INCREASED_PERCENT -> addIncreasedPercent(effect.stat(), effect.value());
            case DECREASED_PERCENT -> addIncreasedPercent(effect.stat(), -effect.value());
            case MORE -> addMore(effect.stat(), effect.value());
            case LESS -> addMore(effect.stat(), effect.value());
        }
    }

    /** On a heated machine every Cooling Rate modifier speeds active cooling: reductions apply as the matching increase. */
    private void applyActiveCooling(MachineModifierEffect effect) {
        double value = effect.value();
        switch (effect.operation()) {
            case ADD -> add(MachineStat.COOLING_RATE, value);
            case INCREASED_PERCENT, DECREASED_PERCENT -> addIncreasedPercent(MachineStat.COOLING_RATE, Math.abs(value));
            case MORE, LESS -> addMore(MachineStat.COOLING_RATE, value > 0.0 && value < 1.0 ? 1.0 / value : value);
        }
    }

    /** Marks the Furnace, Alloy Furnace, Metal Press, and Melter, which run on the shared heat engine. */
    private static void applyHeatedMachineValues(MachineStatAccumulator stats) {
        stats.activeCooling = true;
        stats.baseValues.put(MachineStat.THERMAL_CONDUCTIVITY, 1.0);
    }

    public double value(MachineStat stat) {
        return valueWithIncreased(stat, 0.0);
    }

    /** {@code stat} with {@code extraPercent} added to its increased bucket, as a conditional increase would combine. */
    public double valueWithIncreased(MachineStat stat, double extraPercent) {
        MachineStat resolvedStat = accumulationStat(stat);
        double base = baseValues.getOrDefault(resolvedStat, 0.0);
        double added = additiveValues.getOrDefault(resolvedStat, 0.0) + convertedInto(resolvedStat);
        double more = moreValues.getOrDefault(resolvedStat, 1.0);
        double ordinary = (base + added) * increasedScale(resolvedStat, extraPercent) * more;
        double floored = floored(ordinary, absoluteFloors.get(resolvedStat), bonusMoreValues.getOrDefault(resolvedStat, 1.0));
        return Math.min(absoluteValues.getOrDefault(resolvedStat, floored),
                absoluteCeilings.getOrDefault(resolvedStat, Double.POSITIVE_INFINITY));
    }

    /** {@code ordinary} raised to {@code floor}, with only the bonus above the floor scaled by {@code bonusMore}. */
    public static double floored(double ordinary, Double floor, double bonusMore) {
        return floor == null ? ordinary * bonusMore : floor + Math.max(0.0, ordinary - floor) * bonusMore;
    }

    /** The summed increased and reduced percent on {@code stat}, before any cap. */
    public double increasedPercent(MachineStat stat) {
        return increasedPercentValues.getOrDefault(accumulationStat(stat), 0.0);
    }

    /** The value of the stat that caps {@code stat}'s increased bucket, or null when it has none. */
    public Double increasedCap(MachineStat stat) {
        MachineStat capStat = increasedCaps.get(accumulationStat(stat));
        return capStat == null ? null : value(capStat);
    }

    /**
     * The increased bucket on {@code stat} plus {@code extraPercent}, after the stat's cap: Bonus Output pays no more
     * than Maximum Bonus Output. Stats without a cap return the bucket unchanged.
     */
    public double effectiveIncreasedPercent(MachineStat stat, double extraPercent) {
        MachineStat resolvedStat = accumulationStat(stat);
        double increased = increasedPercentValues.getOrDefault(resolvedStat, 0.0) + extraPercent;
        if (increasedConversions.containsKey(resolvedStat)) {
            increased = 0.0;
        }
        Double cap = increasedCap(resolvedStat);
        return cap == null ? increased : capped(increased, cap);
    }

    /** Dividing stats scale by {@code (1 + increases) / (1 + reductions)}; the rest by {@code 1 + bucket}. */
    private double increasedScale(MachineStat stat, double extraPercent) {
        if (DIVIDING_REDUCTION_STATS.contains(stat)) {
            double reduced = reducedPercentValues.getOrDefault(stat, 0.0) + Math.max(0.0, -extraPercent);
            double increased = increasedPercentValues.getOrDefault(stat, 0.0) + extraPercent + reduced;
            return dividedScale(increased, reduced);
        }
        return Math.max(0.0, 1.0 + effectiveIncreasedPercent(stat, extraPercent) / 100.0);
    }

    public double generatedEnergyTotal(double baseEnergy, int processingTicks) {
        double statScale = percentAndMore(MachineStat.ENERGY_GENERATION);
        double nonFlatAdditive = additiveValues.getOrDefault(MachineStat.ENERGY_GENERATION, 0.0)
                - flatEnergyGenerationBonus;
        double generationMultiplier = Math.max(
                0.1,
                (baseValues.getOrDefault(MachineStat.ENERGY_GENERATION, 0.0) + nonFlatAdditive) * statScale
        );
        double flatGeneration = flatEnergyGenerationBonus * Math.max(1, processingTicks) * flatEnergyGenerationScale();
        return baseEnergy * generationMultiplier + flatGeneration;
    }

    /**
     * Merges an installed part's locally resolved generation. Part modifiers are local: they already scaled the
     * part's own multiplier and flat FE/t, so neither is rescaled by this or any other part's multiplier here.
     */
    void applyPartEnergyGeneration(MachineStatAccumulator part, boolean multiplier) {
        checkMutable();
        double generation = part.valueWithoutFlatEnergyGenerationBonus(MachineStat.ENERGY_GENERATION);
        if (!multiplier) {
            add(MachineStat.ENERGY_GENERATION, generation);
        } else if (Math.abs(generation - 1.0) > 0.0001) {
            addMore(MachineStat.ENERGY_GENERATION, generation);
            partEnergyGenerationMore *= generation;
        }
        double flat = part.effectiveFlatEnergyGenerationBonus();
        add(MachineStat.ENERGY_GENERATION, flat);
        addFlatEnergyGenerationBonus(flat);
    }

    double valueWithoutFlatEnergyGenerationBonus(MachineStat stat) {
        if (stat != MachineStat.ENERGY_GENERATION) {
            return value(stat);
        }
        double nonFlatAdditive = additiveValues.getOrDefault(stat, 0.0) - flatEnergyGenerationBonus;
        return (baseValues.getOrDefault(stat, 0.0) + nonFlatAdditive) * percentAndMore(stat);
    }

    public double effectiveEnergyGenerationMultiplier() {
        return valueWithoutFlatEnergyGenerationBonus(MachineStat.ENERGY_GENERATION);
    }

    public double effectiveFlatEnergyGenerationBonus() {
        return flatEnergyGenerationBonus * flatEnergyGenerationScale();
    }

    private void addFlatEnergyGenerationBonus(double value) {
        checkMutable();
        flatEnergyGenerationBonus += value;
    }

    /** Machine-wide increased and more generation, excluding installed part multipliers. */
    private double flatEnergyGenerationScale() {
        if (partEnergyGenerationMore <= 0.0) {
            return 0.0;
        }
        return percentAndMore(MachineStat.ENERGY_GENERATION) / partEnergyGenerationMore;
    }

    public double baseValue(MachineStat stat) {
        return baseValues.getOrDefault(accumulationStat(stat), 0.0);
    }

    public int intValue(MachineStat stat) {
        return Math.max(0, (int) Math.floor(value(stat)));
    }

    public int adjustedProcessingTicks(int baseTicks) {
        double speed = Math.max(0.1, value(MachineStat.PROCESSING_SPEED));
        return Math.max(1, (int) Math.ceil(baseTicks / speed));
    }

    public int adjustedHeatProcessingTicks(int baseTicks) {
        double heatTransfer = Math.max(0.1, value(MachineStat.HEAT_TRANSFER));
        return Math.max(1, (int) Math.ceil(adjustedProcessingTicks(baseTicks) / heatTransfer));
    }

    public int adjustedEnergyCost(int baseEnergyCost) {
        double usage = Math.max(0.1, value(MachineStat.ENERGY_USAGE));
        return Math.max(1, (int) Math.ceil(baseEnergyCost * usage));
    }

    public int adjustedEnergyCostForProgress(int baseEnergyCost, int adjustedTicks, int progress) {
        return distributedEnergyCostForProgress(adjustedEnergyCost(baseEnergyCost), adjustedTicks, progress);
    }

    private static int distributedEnergyCostForProgress(int totalCost, int adjustedTicks, int progress) {
        int safeTicks = Math.max(1, adjustedTicks);
        int currentProgress = Math.max(0, Math.min(progress, safeTicks - 1));
        return cumulativeProcessingEnergyCost(totalCost, safeTicks, currentProgress + 1)
                - cumulativeProcessingEnergyCost(totalCost, safeTicks, currentProgress);
    }

    private static int cumulativeProcessingEnergyCost(int totalCost, int adjustedTicks, int progress) {
        return (int) Math.ceil(totalCost * (double) progress / adjustedTicks);
    }

    public int adjustedFuelTicks(int baseFuelTicks) {
        double fuelEfficiency = Math.max(0.1, value(MachineStat.FUEL_EFFICIENCY));
        double efficiency = Math.max(0.1, value(MachineStat.EFFICIENCY));
        double fuelDuration = Math.max(0.1, value(MachineStat.FUEL_DURATION));
        return Math.max(1, (int) Math.ceil(baseFuelTicks * fuelEfficiency * efficiency * fuelDuration));
    }

    public void rollBaseValues(List<MachineStatRange> ranges, RandomSource random) {
        checkMutable();
        for (MachineStatRange range : ranges) {
            baseValues.put(range.stat(), range.roll(random));
        }
    }

    private void add(MachineStat stat, double value) {
        checkMutable();
        additiveValues.merge(accumulationStat(stat), value, Double::sum);
        record(StatBreakdown.Kind.ADD, stat, value);
    }

    private void addIncreasedPercent(MachineStat stat, double value) {
        checkMutable();
        increasedPercentValues.merge(accumulationStat(stat), value, Double::sum);
        record(StatBreakdown.Kind.INCREASED, stat, value);
        if (value < 0.0) {
            reducedPercentValues.merge(accumulationStat(stat), -value, Double::sum);
        }
    }

    private void addMore(MachineStat stat, double value) {
        checkMutable();
        moreValues.merge(accumulationStat(stat), value, (current, next) -> current * next);
        record(StatBreakdown.Kind.MORE, stat, value);
    }

    private void record(StatBreakdown.Kind kind, MachineStat stat, double value) {
        if (recorded == null || isNeutral(kind, value)) {
            return;
        }
        Component source = sources.isEmpty() ? FALLBACK_SOURCE : sources.peek();
        List<StatBreakdown.Term> terms = recorded.computeIfAbsent(accumulationStat(stat), ignored -> new ArrayList<>());
        for (int index = 0; index < terms.size(); index++) {
            StatBreakdown.Term term = terms.get(index);
            if (term.kind() == kind && term.source().equals(source) && keepsSign(kind, stat, term.value(), value)) {
                terms.set(index, new StatBreakdown.Term(kind, merge(kind, term.value(), value), source));
                return;
            }
        }
        terms.add(new StatBreakdown.Term(kind, value, source));
    }

    /** A dividing stat keeps its increases and reductions in separate terms, so the breakdown can split them again. */
    private static boolean keepsSign(StatBreakdown.Kind kind, MachineStat stat, double current, double next) {
        return kind != StatBreakdown.Kind.INCREASED || !dividesReductions(stat) || (current < 0.0) == (next < 0.0);
    }

    private static boolean isNeutral(StatBreakdown.Kind kind, double value) {
        return switch (kind) {
            case ADD, INCREASED -> value == 0.0;
            case MORE, BONUS_MORE -> value == 1.0;
            default -> false;
        };
    }

    private static double merge(StatBreakdown.Kind kind, double current, double next) {
        return switch (kind) {
            case MORE -> current * next;
            case FIXED, CEILING, INCREASED_CAP -> Math.min(current, next);
            default -> current + next;
        };
    }

    private double percentAndMore(MachineStat stat) {
        MachineStat resolvedStat = accumulationStat(stat);
        double increased = increasedPercentValues.getOrDefault(resolvedStat, 0.0);
        double more = moreValues.getOrDefault(resolvedStat, 1.0);
        return (1.0 + increased / 100.0) * more;
    }

    public static MachineStat accumulationStat(MachineStat stat) {
        return stat == MachineStat.ENERGY_CAPACITY_FLAT ? MachineStat.ENERGY_CAPACITY : stat;
    }

    private static boolean isFlatEnergyGenerationAffix(MachineModifier modifier, MachineModifierEffect effect) {
        return effect.stat() == MachineStat.ENERGY_GENERATION
                && effect.operation() == ModifierOperation.ADD
                && (ModifierEligibilityProfiles.ENERGY_GENERATION_FLAT_AFFIX_ID.equals(modifier.affixId())
                        || ModifierEligibilityProfiles.ENERGY_GENERATION_FLAT_AFFIX_ID.equals(modifier.modGroup()));
    }
}
