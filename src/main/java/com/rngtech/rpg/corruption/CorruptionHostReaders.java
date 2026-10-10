package com.rngtech.rpg.corruption;

import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierEffectDefinition;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierEligibilityProfiles;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Which stats and behaviors a refinement host reads, keyed by eligibility profile id. A host reads every stat its own
 * affix pool rolls and every behavior that pool can enable, plus the extras listed here: stats its machine reads from
 * host stats even though no affix rolls them, such as a Crush Head's Processing Level.
 */
public final class CorruptionHostReaders {
    private static final Set<MachineStat> CALIBRATION_GEAR_EXTRAS = EnumSet.of(
            MachineStat.COIL_REACH,
            MachineStat.CATALYST_EFFICIENCY,
            MachineStat.PROCESSING_SPEED,
            MachineStat.CALIBRATION_PRECISION
    );
    private static final Set<MachineStat> BATCHING_EXTRAS = EnumSet.of(MachineStat.BATCH_SIZE);
    private static final Map<String, Set<MachineStat>> EXTRA_STATS = Map.of(
            "crush_head", EnumSet.of(MachineStat.PROCESSING_LEVEL, MachineStat.JAM_CHANCE, MachineStat.JAM_RECOVERY),
            "alloy_crucible", EnumSet.of(MachineStat.BLEND_SPEED),
            "resonance_coil", CALIBRATION_GEAR_EXTRAS,
            "control_board", CALIBRATION_GEAR_EXTRAS,
            "stabilizer_matrix", CALIBRATION_GEAR_EXTRAS,
            "melter", BATCHING_EXTRAS,
            "metal_press", BATCHING_EXTRAS,
            "resonance_calibrator", BATCHING_EXTRAS
    );
    private static final Map<String, Set<MachineBehavior>> EXTRA_BEHAVIORS = Map.of(
            "servo", EnumSet.of(MachineBehavior.AUTO_PURGE)
    );
    /** Hosts that read another profile's stats, such as the Unique Potato cell reading Battery Cell stats. */
    private static final Map<String, String> SHARED_READERS = Map.of("unique_battery_cell", "battery_cell");
    private static final Map<String, ModifierEligibilityProfile> PROFILES = profiles();

    private CorruptionHostReaders() {
    }

    public static boolean isKnownHost(String hostId) {
        return PROFILES.containsKey(hostId);
    }

    public static boolean readsStat(String hostId, MachineStat stat) {
        if (EXTRA_STATS.getOrDefault(hostId, Set.of()).contains(stat)) {
            return true;
        }
        ModifierEligibilityProfile profile = PROFILES.get(SHARED_READERS.getOrDefault(hostId, hostId));
        return profile != null && profile.definitions().stream().anyMatch(definition -> rolls(definition, stat));
    }

    public static boolean readsBehavior(String hostId, MachineBehavior behavior) {
        if (EXTRA_BEHAVIORS.getOrDefault(hostId, Set.of()).contains(behavior)) {
            return true;
        }
        ModifierEligibilityProfile profile = PROFILES.get(SHARED_READERS.getOrDefault(hostId, hostId));
        return profile != null && profile.rollableBehaviors().contains(behavior);
    }

    private static boolean rolls(ModifierDefinition definition, MachineStat stat) {
        if (definition.stat() == stat) {
            return true;
        }
        for (ModifierEffectDefinition effect : definition.effects()) {
            if (effect.stat() == stat) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, ModifierEligibilityProfile> profiles() {
        Map<String, ModifierEligibilityProfile> profiles = new HashMap<>();
        for (ModifierEligibilityProfile profile : ModifierEligibilityProfiles.allProfiles()) {
            profiles.put(profile.id(), profile);
        }
        return Map.copyOf(profiles);
    }
}
