package com.rngtech.rpg.jewel;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierDefinition;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.Set;

/**
 * The attribute a jewel is cut for. Every colour rolls the whole jewel pool; colour only weights the pool toward its
 * attribute's themes, and keeps the attribute-threshold chase line of the other two colours out.
 */
public enum JewelColour implements StringRepresentable {
    CONTROL(MachineStat.CONTROL, Set.of(
            JewelAffixes.FRUGAL_GROUP, JewelAffixes.STEADY_GROUP, JewelAffixes.INSULATED_GROUP,
            JewelAffixes.CONTROL_GROUP, JewelAffixes.STUDY_GROUP, JewelAffixes.POISE_GROUP)),
    DRIVE(MachineStat.DRIVE, Set.of(
            JewelAffixes.CONDUCTIVE_GROUP, JewelAffixes.YIELDING_GROUP, JewelAffixes.FORTUNATE_GROUP,
            JewelAffixes.PACE_GROUP, JewelAffixes.DRIVE_GROUP, JewelAffixes.FLASHES_GROUP, JewelAffixes.MOMENTUM_GROUP)),
    RESERVE(MachineStat.RESERVE, Set.of(
            JewelAffixes.CAPACITIVE_GROUP, JewelAffixes.DEEP_GROUP, JewelAffixes.THRIFTY_GROUP,
            JewelAffixes.OUTPUT_GROUP, JewelAffixes.RESERVE_GROUP, JewelAffixes.DEPTH_GROUP));

    public static final Codec<JewelColour> CODEC = StringRepresentable.fromEnum(JewelColour::values);
    /** A home line's share of the pool against an ordinary line's {@code 100}. */
    public static final int HOME_WEIGHT = 150;
    /** An away line's share: still common enough that a Control jewel can carry speed. */
    public static final int AWAY_WEIGHT = 60;
    public static final int COMPONENT_STAGE = 3;

    private final MachineStat attribute;
    private final Set<String> homeGroups;
    private final String serializedName;

    JewelColour(MachineStat attribute, Set<String> homeGroups) {
        this.attribute = attribute;
        this.homeGroups = homeGroups;
        this.serializedName = name().toLowerCase(Locale.ROOT);
    }

    public MachineStat attribute() {
        return attribute;
    }

    public String itemId() {
        return serializedName + "_jewel";
    }

    public String profileId() {
        return serializedName + "_jewel";
    }

    public String translationKey() {
        return "rngtech.jewel.colour." + serializedName;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    /** The colour whose profile has {@code profileId}, or null for any other profile. */
    public static JewelColour forProfile(String profileId) {
        for (JewelColour colour : values()) {
            if (colour.profileId().equals(profileId)) {
                return colour;
            }
        }
        return null;
    }

    /**
     * This colour's roll weight for {@code definition}: home lines at {@link #HOME_WEIGHT}, away lines at
     * {@link #AWAY_WEIGHT}, chase lines at their own weight, and another colour's threshold line never.
     */
    public int rollWeight(ModifierDefinition definition) {
        String group = definition.modGroup();
        JewelColour thresholdColour = JewelAffixes.thresholdColour(definition.id());
        if (thresholdColour != null) {
            return thresholdColour == this ? definition.rollWeight() : 0;
        }
        if (JewelAffixes.CHASE_GROUPS.contains(group)) {
            return definition.rollWeight();
        }
        return homeGroups.contains(group) ? HOME_WEIGHT : AWAY_WEIGHT;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
