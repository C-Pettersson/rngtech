package com.rngtech.rpg;

import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Deep Tanks, Twinned, and Studious: affixes several machine families share. */
public final class SharedAffixChecks {
    private static final List<ModifierEligibilityProfile> FLUID_MACHINES = List.of(
            ModifierEligibilityProfiles.CORROSION_CELL,
            ModifierEligibilityProfiles.MELTER,
            ModifierEligibilityProfiles.BATTERY_ASSEMBLER,
            ModifierEligibilityProfiles.COAL_GASIFIER,
            ModifierEligibilityProfiles.SYNGAS_COMBUSTOR,
            ModifierEligibilityProfiles.STEAM_METHANE_REFORMER,
            ModifierEligibilityProfiles.CAVITATION_GENERATOR,
            ModifierEligibilityProfiles.AMMONIA_SYNTHESIZER,
            ModifierEligibilityProfiles.AMMONIA_FUEL_CELL
    );
    private static final List<ModifierEligibilityProfile> BATCHING_MACHINES = List.of(
            ModifierEligibilityProfiles.FURNACE,
            ModifierEligibilityProfiles.ELECTRIC_FURNACE,
            ModifierEligibilityProfiles.ALLOY_FURNACE,
            ModifierEligibilityProfiles.MELTER,
            ModifierEligibilityProfiles.METAL_PRESS,
            ModifierEligibilityProfiles.RESONANCE_CALIBRATOR
    );

    private static int checks;

    private SharedAffixChecks() {
    }

    public static int run() {
        checks = 0;
        deepTanksRollOnFluidMachines();
        twinnedRollsOnBatchingMachinesOnly();
        studiousRollsWhereverAttributesDo();
        studiousScalesMasteryXp();
        return checks;
    }

    private static void deepTanksRollOnFluidMachines() {
        for (ModifierEligibilityProfile profile : FLUID_MACHINES) {
            require(rolls(profile, ModifierSlot.PREFIX, "fluid_capacity_flat"), profile.id() + " rolls flat Fluid Capacity");
            require(rolls(profile, ModifierSlot.SUFFIX, "increased_percent:fluid_capacity"), profile.id() + " rolls increased Fluid Capacity");
        }
        require(!rolls(ModifierEligibilityProfiles.FLUID_TANK, ModifierSlot.PREFIX, "fluid_capacity_flat"),
                "storage tanks keep their fixed capacity");
    }

    private static void twinnedRollsOnBatchingMachinesOnly() {
        for (ModifierEligibilityProfile profile : BATCHING_MACHINES) {
            ModifierDefinition twinned = definition(profile, "twinned");
            require(twinned != null && twinned.slot() == ModifierSlot.PREFIX, profile.id() + " rolls Twinned as a prefix");
            require(twinned.modGroup().equals("dense_parallel"), "Twinned shares Crusher Throughput's group");
            require(twinned.rollWeight() == 15, "Twinned is a rare chase line");
        }
        require(definition(ModifierEligibilityProfiles.CRUSHER, "twinned") == null, "the Crusher keeps Crusher Throughput instead");
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            if (!BATCHING_MACHINES.contains(profile)) {
                require(definition(profile, "twinned") == null, profile.id() + " does not batch, so it never rolls Twinned");
            }
        }
    }

    private static void studiousRollsWhereverAttributesDo() {
        Set<String> withAttributes = ModifierEligibilityProfiles.allProfiles().stream()
                .filter(profile -> profile.hasCapability(ModifierCapability.HAS_MASTERY_ATTRIBUTES))
                .map(ModifierEligibilityProfile::id)
                .collect(Collectors.toSet());
        Set<String> withStudious = ModifierEligibilityProfiles.allProfiles().stream()
                .filter(profile -> rolls(profile, ModifierSlot.SUFFIX, "mastery_xp_gain"))
                .map(ModifierEligibilityProfile::id)
                .collect(Collectors.toSet());
        require(!withAttributes.isEmpty() && withAttributes.equals(withStudious), "Studious rolls exactly where attribute suffixes do");
        require(MachineStat.isMachineWide(MachineStat.MASTERY_XP_GAIN), "a part's Studious roll applies to the whole machine");
    }

    private static void studiousScalesMasteryXp() {
        MachineProgressionState start = MachineProgressionState.EMPTY;
        require(start.withAddedScaledXp(40, 4, 1.0).equals(start.withAddedScaledXp(40, 4)), "no Studious roll changes nothing");
        near(start.withAddedScaledXp(40, 4, 1.25).xp(), 50, "25% increased gain on a full grant");
        near(start.withAddedScaledXp(40, 2, 1.25).xp(), 25, "the gain applies after the band's quarters");
        MachineProgressionState small = start.withAddedScaledXp(1, 4, 1.25);
        near(small.xp() * 4 + small.xpRemainder(), 5, "a 1 XP grant keeps its bonus as a quarter");

        MachineStatAccumulator stats = MachineStatAccumulator.componentBase(java.util.Map.of());
        near(MachineMasteryHost.xpGain(stats), 1.0, "a machine without Studious gains XP at the normal rate");
        stats.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.MASTERY_XP_GAIN, ModifierOperation.INCREASED_PERCENT, 18));
        stats.apply(new MachineModifier(ModifierSlot.SUFFIX, MachineStat.MASTERY_XP_GAIN, ModifierOperation.INCREASED_PERCENT, 12));
        near(MachineMasteryHost.xpGain(stats), 1.30, "Studious rolls on the machine and its Gear add up");
    }

    private static boolean rolls(ModifierEligibilityProfile profile, ModifierSlot slot, String group) {
        return profile.rollableDefinitions(slot).stream().anyMatch(definition -> definition.modGroup().equals(group));
    }

    private static ModifierDefinition definition(ModifierEligibilityProfile profile, String id) {
        return profile.definitions().stream().filter(definition -> definition.id().equals(id)).findFirst().orElse(null);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void near(double actual, double expected, String label) {
        checks++;
        if (Math.abs(actual - expected) > 0.0001) {
            throw new AssertionError(label + ": expected " + expected + " but was " + actual);
        }
    }
}
