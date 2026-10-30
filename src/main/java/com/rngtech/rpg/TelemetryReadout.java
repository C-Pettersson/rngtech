package com.rngtech.rpg;

import java.util.Locale;

/** The gauge a Telemetry body sends to comparators in place of the work-state signal. */
public enum TelemetryReadout {
    ENERGY,
    PROGRESS,
    OUTPUT,
    HEAT;

    private final String affixId = "telemetry_" + name().toLowerCase(Locale.ROOT);

    public String affixId() {
        return affixId;
    }

    public static TelemetryReadout byAffixId(String affixId) {
        for (TelemetryReadout readout : values()) {
            if (readout.affixId.equals(affixId)) {
                return readout;
            }
        }
        return null;
    }
}
