package com.rngtech.content.energy;

import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * Where a Solar Panel's deck points. A generating panel tracks the sun, or the moon when its controller converts
 * moonlight; both cross the sky from east to west, so a north-south tracker only needs the half of the sky they are
 * in. The server stores just that half, and the client turns the deck smoothly from the world time.
 */
public enum SolarTracker implements StringRepresentable {
    STOWED("stowed"),
    EAST("east"),
    WEST("west");

    /** Deck tilt at sunrise and sunset, in degrees. */
    public static final float MAX_TILT = 25.0F;
    /** The torque tube's height in model pixels, matching tools/blockmodels/solar.py. */
    public static final float PIVOT_Y = 4.0F;
    private static final long HALF_DAY = 12000L;

    private final String name;

    SolarTracker(String name) {
        this.name = name;
    }

    /** The half of the sky the sun (by day) or the moon (by night) is in. */
    public static SolarTracker tracking(long dayTime) {
        return Math.floorMod(dayTime, HALF_DAY) < HALF_DAY / 2 ? EAST : WEST;
    }

    /**
     * The deck tilt in degrees about the north-south axis while tracking: east at rise, level at the highest point,
     * west at set. Negative tilts face east.
     */
    public static float tilt(double dayTime) {
        double progress = Math.floorMod((long) Math.floor(dayTime), HALF_DAY) + (dayTime - Math.floor(dayTime));
        return -MAX_TILT * Mth.cos((float) (progress / HALF_DAY * Math.PI));
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
