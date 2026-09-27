package com.rngtech.rpg.progression;

import java.util.List;
import java.util.Objects;

public record PassiveTreeConstants(List<Integer> skillsPerOrbit, List<Integer> orbitRadii) {
    public PassiveTreeConstants {
        skillsPerOrbit = List.copyOf(Objects.requireNonNull(skillsPerOrbit, "skillsPerOrbit"));
        orbitRadii = List.copyOf(Objects.requireNonNull(orbitRadii, "orbitRadii"));
        if (skillsPerOrbit.isEmpty()) {
            throw new IllegalArgumentException("Passive tree constants must define at least one orbit");
        }
        if (skillsPerOrbit.size() != orbitRadii.size()) {
            throw new IllegalArgumentException("Passive tree orbit counts and radii must have the same size");
        }
        for (int index = 0; index < skillsPerOrbit.size(); index++) {
            if (skillsPerOrbit.get(index) <= 0) {
                throw new IllegalArgumentException("Passive tree orbit node count must be positive at orbit " + index);
            }
            if (orbitRadii.get(index) < 0) {
                throw new IllegalArgumentException("Passive tree orbit radius must be non-negative at orbit " + index);
            }
        }
    }

    public static PassiveTreeConstants poeStyle() {
        return new PassiveTreeConstants(PassiveTreeLayouts.POE_SKILLS_PER_ORBIT, PassiveTreeLayouts.POE_ORBIT_RADII);
    }
}
