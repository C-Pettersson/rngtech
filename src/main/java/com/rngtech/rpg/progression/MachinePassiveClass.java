package com.rngtech.rpg.progression;

import java.util.List;
import java.util.Objects;

public record MachinePassiveClass(
        String id,
        String translationKey,
        int baseControl,
        int baseDrive,
        int baseReserve,
        String startNodeId,
        List<String> ascendancies
) {
    public MachinePassiveClass {
        id = requireText(id, "id");
        translationKey = requireText(translationKey, "translationKey");
        startNodeId = requireText(startNodeId, "startNodeId");
        requireNonNegative(baseControl, "baseControl");
        requireNonNegative(baseDrive, "baseDrive");
        requireNonNegative(baseReserve, "baseReserve");
        ascendancies = List.copyOf(Objects.requireNonNull(ascendancies, "ascendancies"));
    }

    public static MachinePassiveClass of(String id, int baseControl, int baseDrive, int baseReserve, String startNodeId) {
        return new MachinePassiveClass(
                id,
                "rngtech.mastery.class." + id,
                baseControl,
                baseDrive,
                baseReserve,
                startNodeId,
                List.of()
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Machine passive class " + fieldName + " must not be blank");
        }
        return value;
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException("Machine passive class " + fieldName + " must not be negative");
        }
    }
}
