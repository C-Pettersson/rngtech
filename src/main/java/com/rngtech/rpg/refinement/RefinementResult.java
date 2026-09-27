package com.rngtech.rpg.refinement;

import com.rngtech.rpg.MachineTraits;

public record RefinementResult(
        boolean success,
        MachineTraits traits,
        int consumedPotential,
        String messageKey,
        boolean consumeCatalyst,
    boolean consumeModifier
) {
    public static RefinementResult success(MachineTraits traits, int consumedPotential, String messageKey) {
        return success(traits, consumedPotential, messageKey, true, false);
    }

    public static RefinementResult success(
            MachineTraits traits,
            int consumedPotential,
            String messageKey,
            boolean consumeCatalyst,
            boolean consumeModifier
    ) {
        return new RefinementResult(true, traits, consumedPotential, messageKey, consumeCatalyst, consumeModifier);
    }

    public static RefinementResult failure(MachineTraits traits, String messageKey) {
        return new RefinementResult(false, traits, 0, messageKey, false, false);
    }
}
