package com.rngtech.rpg;

import com.rngtech.content.calibration.CalibrationGearMaterial;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.CathodeMaterial;
import com.rngtech.content.energy.CavitationRotorMaterial;
import com.rngtech.content.energy.CollapseNozzleMaterial;
import com.rngtech.content.energy.ContainmentLiningMaterial;
import com.rngtech.content.energy.FuelBoxMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.energy.ReactorChamberMaterial;
import com.rngtech.content.energy.RecoveryFilterMaterial;
import com.rngtech.content.energy.SolarArrayExtenderMaterial;
import com.rngtech.content.energy.VacuumCollapsePartMaterial;
import com.rngtech.content.item.AlloyCrucibleItem;
import com.rngtech.content.item.AmmoniaPartItem;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BioChamberItem;
import com.rngtech.content.item.CalibrationGearItem;
import com.rngtech.content.item.CathodeItem;
import com.rngtech.content.item.CavitationPartItem;
import com.rngtech.content.item.CollapseNozzleItem;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.item.DisassemblyHeadItem;
import com.rngtech.content.item.FluidPumpItem;
import com.rngtech.content.item.GasChemistryPartItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolarArrayExtenderItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.item.VacuumCollapsePartItem;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ComponentBaseStatCatalog {
    private static final Set<MachineStat> HARD_GATE_STATS =
            EnumSet.of(MachineStat.PROCESSING_LEVEL);

    /**
     * Affix stats that describe the host machine rather than the part, such as a Fluid Pump's Fluid Capacity: they apply
     * to the machine as rolled, so a percent affix scales the machine's tanks.
     */
    private static final Set<MachineStat> HOST_STATS = Set.of(MachineStat.FLUID_CAPACITY);

    /**
     * How a part stat reaches the host. {@code INCREASED} joins the host's increased bucket: the part's base is a percent,
     * and its rolls on that stat pass straight through instead of scaling the part locally.
     */
    private enum MergeRule {
        ADD,
        MORE,
        INCREASED
    }

    record Profile(
            MachineStatAccumulator baseStats,
            Map<MachineStat, MergeRule> mergeRules,
            List<MachineStat> summaryStats
    ) {
    }

    public static MachineStatAccumulator baseStats(ItemStack stack) {
        Profile profile = profile(stack);
        return profile == null ? null : profile.baseStats();
    }

    public static MachineStatAccumulator effectiveStats(ItemStack stack) {
        Profile profile = profile(stack);
        if (profile == null) {
            return null;
        }
        return effectiveStats(stack, profile);
    }

    private static MachineStatAccumulator effectiveStats(ItemStack stack, Profile profile) {
        return effectiveStats(profile, componentTraits(stack));
    }

    static MachineStatAccumulator effectiveStats(Profile profile, MachineTraits traits) {
        MachineStatAccumulator stats = profile.baseStats();
        for (MachineModifier modifier : traits.modifiers()) {
            if (!modifier.slot().isAffix() || HARD_GATE_STATS.contains(modifier.stat())) {
                continue;
            }
            if (modifier.effects().stream().noneMatch(effect -> profile.mergeRules().get(effect.stat()) == MergeRule.INCREASED)) {
                stats.apply(modifier);
                continue;
            }
            // Effects that join the host's increased bucket pass through at merge; the rest stay local, such as a yield
            // prefix's speed penalty.
            for (MachineModifierEffect effect : modifier.effects()) {
                if (profile.mergeRules().get(effect.stat()) != MergeRule.INCREASED) {
                    stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, effect.stat(), effect.operation(), effect.value()));
                }
            }
        }
        // Corruption implicits apply after affixes and may move hard-gate stats such as Processing Level.
        for (MachineModifier corrupted : corruptionEffects(traits)) {
            MergeRule rule = profile.mergeRules().get(corrupted.stat());
            if (rule != null && rule != MergeRule.INCREASED) {
                stats.apply(corrupted);
            }
        }
        return stats;
    }

    /** Each corruption implicit effect as its own single-effect modifier. */
    private static List<MachineModifier> corruptionEffects(MachineTraits traits) {
        List<MachineModifier> effects = new ArrayList<>();
        for (MachineModifier modifier : traits.modifiers()) {
            if (modifier.slot().isCorruption()) {
                modifier.effects().forEach(effect -> effects.add(
                        new MachineModifier(ModifierSlot.CORRUPTION, effect.stat(), effect.operation(), effect.value())
                ));
            }
        }
        return effects;
    }

    /** Whether {@code stat} on this part joins the host's increased bucket, so its base reads as a percent. */
    public static boolean mergesAsIncreased(ItemStack stack, MachineStat stat) {
        Profile profile = profile(stack);
        return profile != null && profile.mergeRules().get(stat) == MergeRule.INCREASED;
    }

    /**
     * Whether a rolled modifier on a part is local in a way that changes the result: a percent roll that scales the
     * part's own value, or a flat roll that the same part's percent roll on that stat scales. Rolls that pass straight
     * through to the machine, apply to the host, or target a stat the part does not carry are not.
     */
    public static boolean isLocalModifier(ItemStack stack, MachineModifier modifier) {
        Profile profile = profile(stack);
        if (profile == null || !modifier.slot().isAffix()) {
            return false;
        }
        List<MachineModifier> rolled = componentTraits(stack).modifiers();
        for (MachineModifierEffect effect : modifier.effects()) {
            MachineStat stat = effect.stat();
            if (!profile.mergeRules().containsKey(stat) || HOST_STATS.contains(stat)
                    || profile.mergeRules().get(stat) == MergeRule.INCREASED) {
                continue;
            }
            if (isScaling(effect.operation()) || rolled.stream().anyMatch(other -> other != modifier && scalesStat(other, stat))) {
                return true;
            }
        }
        return false;
    }

    private static boolean scalesStat(MachineModifier modifier, MachineStat stat) {
        return modifier.slot().isAffix()
                && modifier.effects().stream().anyMatch(effect -> effect.stat() == stat && isScaling(effect.operation()));
    }

    private static boolean isScaling(ModifierOperation operation) {
        return operation != ModifierOperation.ADD;
    }

    public static List<MachineStat> summaryStats(ItemStack stack) {
        Profile profile = profile(stack);
        return profile == null ? List.of() : profile.summaryStats();
    }

    public static void applyEffectiveContribution(MachineStatAccumulator target, ItemStack stack) {
        Profile profile = profile(stack);
        if (profile == null) {
            return;
        }

        try (MachineStatAccumulator.Source ignored = target.source(stack.getHoverName())) {
            applyContribution(target, profile, componentTraits(stack));
            for (MachineModifier modifier : componentTraits(stack).modifiers()) {
                if (modifier.slot().isAffix() && HOST_STATS.contains(modifier.stat())) {
                    target.apply(modifier);
                }
            }
        }
    }

    public static void applyVacuumCollapseNozzleContribution(MachineStatAccumulator target, ItemStack stack) {
        if (!(stack.getItem() instanceof CollapseNozzleItem nozzle) || !nozzle.material().vacuumCollapseCompatible()) {
            return;
        }

        try (MachineStatAccumulator.Source ignored = target.source(stack.getHoverName())) {
            applyContribution(target, vacuumCollapseNozzle(nozzle.material()), componentTraits(stack));
        }
    }

    /** Registry-free part merge, shared with headless balance simulation. */
    static void applyContribution(MachineStatAccumulator target, Profile profile, MachineTraits traits) {
        applyProfileContribution(target, profile, effectiveStats(profile, traits), traits);
    }

    private static void applyProfileContribution(
            MachineStatAccumulator target,
            Profile profile,
            MachineStatAccumulator contribution,
            MachineTraits traits
    ) {
        for (Map.Entry<MachineStat, MergeRule> entry : profile.mergeRules().entrySet()) {
            MachineStat stat = entry.getKey();
            double value = contribution.value(stat);
            if (stat == MachineStat.ENERGY_GENERATION) {
                target.applyPartEnergyGeneration(contribution, entry.getValue() == MergeRule.MORE);
                continue;
            }
            switch (entry.getValue()) {
                case ADD -> applyAdd(target, stat, value);
                case MORE -> applyMore(target, stat, value);
                case INCREASED -> applyIncreased(target, profile.baseStats().baseValue(stat), stat, traits);
            }
        }

        // Corruption stats the part does not carry, such as a Crush Head's Jam Recovery, act on the host directly.
        for (MachineModifier corrupted : corruptionEffects(traits)) {
            if (!profile.mergeRules().containsKey(corrupted.stat())) {
                target.apply(corrupted);
            }
        }

        int refinementPotential = traits.refinementPotential();
        if (refinementPotential > 0) {
            applyAdd(target, MachineStat.REFINEMENT_POTENTIAL, refinementPotential);
        }
    }

    private static Profile profile(ItemStack stack) {
        return stack.isEmpty() ? null : profile(stack.getItem());
    }

    private static Profile profile(Item item) {
        if (item instanceof BatteryCellItem cell) {
            return batteryCell(cell.material());
        }
        if (item instanceof BioChamberItem) {
            return bioChamber();
        }
        if (item instanceof CrushHeadItem head) {
            return crushHead(head.material());
        }
        if (item instanceof AlloyCrucibleItem crucible) {
            return alloyCrucible(crucible.material());
        }
        if (item instanceof SolidFuelBurnerPartItem part) {
            return part.partType() == MachinePartType.HEAT_CORE
                    ? heatCore(part.heatCoreMaterial())
                    : fuelBox(part.fuelBoxMaterial());
        }
        if (item instanceof PotentialReactorPartItem part) {
            return switch (part.partType()) {
                case REACTOR_CHAMBER -> reactorChamber(part.chamberMaterial());
                case RECOVERY_FILTER -> recoveryFilter(part.filterMaterial());
                case CONTAINMENT_LINING -> containmentLining(part.liningMaterial());
                default -> null;
            };
        }
        if (item instanceof VacuumCollapsePartItem part) {
            return vacuumCollapsePart(part.partType(), part.material());
        }
        if (item instanceof CollapseNozzleItem nozzle) {
            return collapseNozzle(nozzle.material());
        }
        if (item instanceof CavitationPartItem part) {
            return switch (part.partType()) {
                case CAVITATION_ROTOR -> cavitationRotor(part.rotorMaterial());
                default -> null;
            };
        }
        if (item instanceof DisassemblyHeadItem head) {
            return disassemblyHead(head.material());
        }
        if (item instanceof ServoItem servo) {
            return servo(servo.material());
        }
        if (item instanceof FluidPumpItem pump) {
            return fluidPump(pump.material());
        }
        if (item instanceof CathodeItem cathode) {
            return cathode(cathode.material());
        }
        if (item instanceof AmmoniaPartItem part) {
            return switch (part.partType()) {
                case AMMONIA_CATALYST_BED -> ammoniaCatalystBed();
                case FUEL_CELL_MEMBRANE -> fuelCellMembrane();
                default -> null;
            };
        }
        if (item instanceof GasChemistryPartItem part) {
            return part.partType() == MachinePartType.REFORMING_CATALYST_BED ? reformingCatalystBed(part.stage()) : null;
        }
        if (item instanceof SolarArrayExtenderItem extender) {
            return solarArrayExtender(extender.material());
        }
        if (item instanceof CalibrationGearItem gear) {
            return switch (gear.partType()) {
                case RESONANCE_COIL -> resonanceCoil(gear.material());
                case CONTROL_BOARD -> controlBoard(gear.material());
                case STABILIZER_MATRIX -> stabilizerMatrix(gear.material());
                default -> null;
            };
        }
        return null;
    }

    private static Profile batteryCell(BatteryCellMaterial material) {
        return builder()
                .add(MachineStat.ENERGY_CAPACITY, material.capacity())
                .add(MachineStat.ENERGY_TRANSFER, material.inputRate())
                .more(MachineStat.EFFICIENCY, material.efficiency())
                .add(MachineStat.IDLE_LOSS, material.idleLossPercentPerMinute())
                .build();
    }

    static Profile bioChamber() {
        return builder()
                .more(MachineStat.FUEL_EFFICIENCY, 1.0)
                .more(MachineStat.POTATO_POWER, 1.0)
                .more(MachineStat.CARROT_POWER, 1.0)
                .more(MachineStat.BREAD_POWER, 1.0)
                .more(MachineStat.SAPLING_POWER, 1.0)
                .more(MachineStat.SEED_POWER, 1.0)
                .more(MachineStat.PLANT_POWER, 1.0)
                .more(MachineStat.ORGANIC_REAGENT_POWER, 1.0)
                .more(MachineStat.COMPOSTED_BIOMASS_POWER, 1.0)
                .more(MachineStat.ALGAE_POWER, 1.0)
                .more(MachineStat.RICH_BIOMASS_POWER, 1.0)
                .more(MachineStat.ENERGY_GENERATION, 1.0)
                .more(MachineStat.FUEL_DURATION, 1.0)
                .buildWithoutSummary();
    }

    static Profile crushHead(CrushHeadMaterial material) {
        ProfileBuilder builder = builder()
                .add(MachineStat.PROCESSING_LEVEL, material.processingLevel())
                .more(MachineStat.PROCESSING_SPEED, crushHeadProcessingSpeed(material))
                .increased(MachineStat.OUTPUT_AMOUNT, crushHeadOutputPercent(material))
                .add(MachineStat.INSTANT_PROCESS_CHANCE, 0.0)
                .add(MachineStat.SUPER_OUTPUT_CHANCE, 0.0)
                .add(MachineStat.CRUSHER_SALVAGE_CHANCE, 0.0);
        return builder.build();
    }

    private static Profile alloyCrucible(AlloyCrucibleMaterial material) {
        return builder()
                .add(MachineStat.INPUT_SLOTS, material.inputSlots())
                .more(MachineStat.STABILITY, material.stability())
                .more(MachineStat.TEMPERATURE_STABILITY, material.temperatureStability())
                .more(MachineStat.HEAT_TRANSFER, material.heatTransfer())
                .more(MachineStat.HEAT_ISOLATION, 1.0)
                .more(MachineStat.WARMUP_TIME, 1.0)
                .more(MachineStat.COOLING_RATE, 1.0)
                .more(MachineStat.OVERHEAT_TOLERANCE, 1.0)
                .more(MachineStat.PROCESSING_SPEED, material.processingSpeed())
                .build();
    }

    static Profile heatCore(HeatCoreMaterial material) {
        return builder()
                .add(MachineStat.ENERGY_GENERATION, material.energyGeneration())
                .more(MachineStat.FUEL_EFFICIENCY, material.fuelEfficiency())
                .more(MachineStat.HEAT_ISOLATION, material.heatIsolation())
                .more(MachineStat.HEAT_TRANSFER, material.heatTransfer())
                .add(MachineStat.MAX_TEMPERATURE, material.maxTemperatureBonus())
                .more(MachineStat.WARMUP_TIME, 1.0)
                .more(MachineStat.COOLING_RATE, 1.0)
                .more(MachineStat.TEMPERATURE_STABILITY, material.temperatureStability())
                .more(MachineStat.OVERHEAT_TOLERANCE, 1.0)
                .build();
    }

    static Profile fuelBox(FuelBoxMaterial material) {
        return builder()
                .add(MachineStat.INPUT_SLOTS, material.fuelSlots())
                .more(MachineStat.FUEL_EFFICIENCY, material.fuelEfficiency())
                .more(MachineStat.STABILITY, material.stability())
                .build();
    }

    static Profile reactorChamber(ReactorChamberMaterial material) {
        return builder()
                .add(MachineStat.PROCESSING_LEVEL, material.stage())
                .add(MachineStat.ENERGY_GENERATION, material.energyGenerationBonus())
                .more(MachineStat.PROCESSING_SPEED, 1.0)
                .more(MachineStat.STABILITY, material.stability())
                .build();
    }

    static Profile recoveryFilter(RecoveryFilterMaterial material) {
        return builder()
                .more(MachineStat.EFFICIENCY, material.efficiency())
                .more(MachineStat.PROCESSING_SPEED, material.processingSpeed())
                .more(MachineStat.OUTPUT_AMOUNT, 1.0)
                .build();
    }

    static Profile containmentLining(ContainmentLiningMaterial material) {
        return builder()
                .more(MachineStat.STABILITY, material.stability())
                .build();
    }

    static Profile vacuumCollapsePart(MachinePartType partType, VacuumCollapsePartMaterial material) {
        return switch (partType) {
            case VOID_CHAMBER -> builder()
                    .add(MachineStat.PROCESSING_LEVEL, material.stage())
                    .more(MachineStat.ENERGY_GENERATION, material.generation())
                    .more(MachineStat.STABILITY, material.stability())
                    .build();
            case COLLAPSE_NOZZLE -> builder()
                    .more(MachineStat.ENERGY_GENERATION, material.generation())
                    .more(MachineStat.PROCESSING_SPEED, 1.0 + (material.stage() - 6) * 0.05)
                    .more(MachineStat.STABILITY, material.stability())
                    .build();
            case DIMENSIONAL_STABILIZER -> builder()
                    .more(MachineStat.STABILITY, material.stability())
                    .more(MachineStat.EFFICIENCY, 1.0 + (material.stage() - 6) * 0.04)
                    .build();
            default -> null;
        };
    }

    static Profile cavitationRotor(CavitationRotorMaterial material) {
        return builder()
                .add(MachineStat.PROCESSING_LEVEL, material.stage())
                .add(MachineStat.DURABILITY, CavitationRotorMaterial.BASE_DURABILITY)
                .more(MachineStat.ENERGY_GENERATION, material.generationMultiplier())
                .more(MachineStat.PROCESSING_SPEED, material.processingSpeedMultiplier())
                .more(MachineStat.STABILITY, 1.0 / material.wearMultiplier())
                .more(MachineStat.OUTPUT_AMOUNT, material.outputMultiplier())
                .build();
    }

    static Profile collapseNozzle(CollapseNozzleMaterial material) {
        return builder()
                .more(MachineStat.ENERGY_GENERATION, material.generationMultiplier())
                .more(MachineStat.TEMPERATURE_STABILITY, 1.0 / material.strainMultiplier())
                .more(MachineStat.OUTPUT_AMOUNT, material.outputMultiplier())
                .more(MachineStat.FLUID_TRANSFER, material.fluidTransferMultiplier())
                .build();
    }

    static Profile vacuumCollapseNozzle(CollapseNozzleMaterial material) {
        return builder()
                .more(MachineStat.ENERGY_GENERATION, material.vacuumGenerationMultiplier())
                .more(MachineStat.PROCESSING_SPEED, material.vacuumProcessingSpeedMultiplier())
                .more(MachineStat.STABILITY, material.vacuumStabilityMultiplier())
                .build();
    }

    private static Profile disassemblyHead(DisassemblyHeadMaterial material) {
        return builder()
                .add(MachineStat.PROCESSING_LEVEL, material.stage())
                .more(MachineStat.PROCESSING_SPEED, material.processingSpeed())
                .more(MachineStat.STABILITY, material.stability())
                .build();
    }

    static Profile servo(ServoMaterial material) {
        return builder()
                .more(MachineStat.PROCESSING_SPEED, percentMultiplier(material.processingSpeedPercent()))
                .more(MachineStat.ENERGY_USAGE, 1.0)
                .more(MachineStat.HEAT_TRANSFER, 1.0)
                .more(MachineStat.HEAT_ISOLATION, 1.0)
                .more(MachineStat.WARMUP_TIME, 1.0)
                .more(MachineStat.COOLING_RATE, 1.0)
                .more(MachineStat.STABILITY, percentMultiplier(material.stabilityPercent()))
                .more(MachineStat.TEMPERATURE_STABILITY, percentMultiplier(material.temperatureStabilityPercent()))
                .more(MachineStat.OVERHEAT_TOLERANCE, percentMultiplier(material.overheatTolerancePercent()))
                .more(MachineStat.FLUID_TRANSFER, 1.0)
                .build();
    }

    /** Every stat the Cathode can roll must merge here, or its affixes would be dropped. */
    static Profile cathode(CathodeMaterial material) {
        return builder()
                .more(MachineStat.ENERGY_GENERATION, 1.0)
                .more(MachineStat.EFFICIENCY, material.efficiency())
                .more(MachineStat.PROCESSING_SPEED, 1.0)
                .more(MachineStat.STABILITY, 1.0)
                .build();
    }

    private static Profile fluidPump(FluidPumpMaterial material) {
        return builder()
                .add(MachineStat.FLUID_TRANSFER, material.transferRate())
                .build();
    }

    private static Profile ammoniaCatalystBed() {
        return builder()
                .more(MachineStat.ENERGY_USAGE, 1.0)
                .more(MachineStat.PROCESSING_SPEED, 1.0)
                .more(MachineStat.EFFICIENCY, 1.0)
                .more(MachineStat.STABILITY, 1.0)
                .more(MachineStat.FLUID_TRANSFER, 1.0)
                .add(MachineStat.INSTANT_PROCESS_CHANCE, 0.0)
                .buildWithoutSummary();
    }

    static Profile fuelCellMembrane() {
        return builder()
                .more(MachineStat.ENERGY_GENERATION, 1.0)
                .more(MachineStat.PROCESSING_SPEED, 1.0)
                .more(MachineStat.EFFICIENCY, 1.0)
                .more(MachineStat.STABILITY, 1.0)
                .more(MachineStat.FLUID_TRANSFER, 1.0)
                .buildWithoutSummary();
    }

    private static Profile reformingCatalystBed(int stage) {
        return builder()
                .add(MachineStat.PROCESSING_LEVEL, stage)
                .more(MachineStat.PROCESSING_SPEED, 1.0 + stage * 0.03)
                .more(MachineStat.EFFICIENCY, 1.0 + stage * 0.02)
                .more(MachineStat.STABILITY, 1.0 + stage * 0.02)
                .add(MachineStat.FLUID_TRANSFER, stage * 150.0)
                .build();
    }

    static Profile solarArrayExtender(SolarArrayExtenderMaterial material) {
        return builder()
                .add(MachineStat.SOLAR_PANEL_LIMIT, material.rangeBonus())
                .more(MachineStat.ENERGY_GENERATION, material.generationMultiplier())
                .add(material.implicitBonusStat(), material.implicitBonusPercent())
                .build();
    }

    private static Profile resonanceCoil(CalibrationGearMaterial material) {
        return builder()
                .add(MachineStat.PROCESSING_LEVEL, material.stage())
                .more(MachineStat.CALIBRATION_QUALITY, percentMultiplier(material.qualityPercent()))
                .more(MachineStat.ENERGY_USAGE, 1.0)
                .more(MachineStat.PROCESSING_SPEED, resonanceCoilProcessingSpeed(material))
                .build();
    }

    private static Profile controlBoard(CalibrationGearMaterial material) {
        return builder()
                .more(MachineStat.CALIBRATION_PRECISION, percentMultiplier(material.precisionPercent()))
                .more(MachineStat.STABILITY, percentMultiplier(Math.max(2, material.stage() * 2)))
                .add(MachineStat.REFINEMENT_POTENTIAL_BONUS, material.refinementPotentialBonus())
                .build();
    }

    private static Profile stabilizerMatrix(CalibrationGearMaterial material) {
        return builder()
                .more(MachineStat.STABILITY, percentMultiplier(Math.max(4, material.stage() * 3)))
                .more(MachineStat.CALIBRATION_QUALITY, percentMultiplier(material.qualityPercent() / 2.0))
                .more(MachineStat.CATALYST_EFFICIENCY, percentMultiplier(material.catalystEfficiencyPercent()))
                .more(MachineStat.ENERGY_USAGE, 1.0)
                .build();
    }

    private static MachineTraits componentTraits(ItemStack stack) {
        if (stack.getItem() instanceof BatteryCellItem) {
            return BatteryCellItem.traits(stack);
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            return part.traits(stack);
        }
        return MachineTraits.EMPTY;
    }

    private static double crushHeadProcessingSpeed(CrushHeadMaterial material) {
        return switch (material) {
            case FLINT -> 0.65;
            case IRON -> 1.05;
            case COPPER -> 1.20;
            case BRONZE -> 1.10;
            case STEEL -> 1.15;
            case ALUMINUM -> 1.35;
            case TITANIUM -> 1.25;
            case TUNGSTENSTEEL -> 1.10;
            case EXOTIC -> 1.35;
        };
    }

    /** Percent the head adds to the Crusher's Output Amount bucket. */
    private static double crushHeadOutputPercent(CrushHeadMaterial material) {
        return switch (material) {
            case BRONZE -> 10;
            case STEEL -> 5;
            case TITANIUM -> 15;
            case TUNGSTENSTEEL -> 30;
            case EXOTIC -> 25;
            default -> 0;
        };
    }

    private static double resonanceCoilProcessingSpeed(CalibrationGearMaterial material) {
        return material == CalibrationGearMaterial.COPPER || material == CalibrationGearMaterial.NULLITE
                ? percentMultiplier(material.stage() * 3.0)
                : 1.0;
    }

    private static double percentMultiplier(double percent) {
        return 1.0 + percent / 100.0;
    }

    private static void applyAdd(MachineStatAccumulator target, MachineStat stat, double value) {
        if (Math.abs(value) > 0.0001) {
            target.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, ModifierOperation.ADD, value));
        }
    }

    /** The part's base percent, then its rolls on {@code stat}, join the host's increased bucket unscaled. */
    private static void applyIncreased(MachineStatAccumulator target, double basePercent, MachineStat stat, MachineTraits traits) {
        if (Math.abs(basePercent) > 0.0001) {
            target.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, ModifierOperation.INCREASED_PERCENT, basePercent));
        }
        for (MachineModifier modifier : traits.modifiers()) {
            if (!modifier.slot().isAffix() && !modifier.slot().isCorruption()) {
                continue;
            }
            for (MachineModifierEffect effect : modifier.effects()) {
                if (effect.stat() == stat) {
                    target.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, effect.operation(), effect.value()));
                }
            }
        }
    }

    private static void applyMore(MachineStatAccumulator target, MachineStat stat, double value) {
        if (Math.abs(value - 1.0) > 0.0001) {
            target.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, ModifierOperation.MORE, value));
        }
    }

    private static ProfileBuilder builder() {
        return new ProfileBuilder();
    }

    private static final class ProfileBuilder {
        private final Map<MachineStat, Double> values = new EnumMap<>(MachineStat.class);
        private final Map<MachineStat, MergeRule> mergeRules = new EnumMap<>(MachineStat.class);
        private final List<MachineStat> summaryStats = new ArrayList<>();

        private ProfileBuilder add(MachineStat stat, double value) {
            return stat(stat, value, MergeRule.ADD);
        }

        private ProfileBuilder more(MachineStat stat, double value) {
            return stat(stat, value, MergeRule.MORE);
        }

        private ProfileBuilder increased(MachineStat stat, double percent) {
            return stat(stat, percent, MergeRule.INCREASED);
        }

        private ProfileBuilder stat(MachineStat stat, double value, MergeRule rule) {
            values.put(stat, value);
            mergeRules.put(stat, rule);
            summaryStats.add(stat);
            return this;
        }

        private Profile build() {
            return new Profile(
                    MachineStatAccumulator.componentBase(values),
                    Map.copyOf(mergeRules),
                    List.copyOf(summaryStats)
            );
        }

        private Profile buildWithoutSummary() {
            return new Profile(MachineStatAccumulator.componentBase(values), Map.copyOf(mergeRules), List.of());
        }
    }

    private ComponentBaseStatCatalog() {
    }
}
