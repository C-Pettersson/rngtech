package com.rngtech.rpg.refinement;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** What a Phase Array operation does on Success. The recipe names one of these; the Cradle resolves it. */
public enum PhaseArrayAction implements StringRepresentable {
    /** Rerolls rarity, affixes, and RP as if newly crafted. Strain stays. */
    REFORGE(Selection.NONE),
    /** Rerolls every affix's value within its tier. */
    ATTUNE(Selection.NONE),
    /** Raises the selected affix one tier, up to the upgrade cap. */
    RESONATE(Selection.AFFIX),
    /** Removes the selected affix. */
    EXCISE(Selection.AFFIX),
    /** Adds the chosen affix to the chosen empty slot; the tier rolls with the item's weights. */
    IMPRINT(Selection.EMPTY_SLOT_AND_DEFINITION),
    /** Swaps the selected affix for another legal affix of the same slot type, keeping its tier. */
    TRANSMUTE(Selection.AFFIX),
    /** Sets Strain to 0. */
    TEMPER(Selection.NONE),
    /** Raises the selected top-tier affix into Tier 7. Exotic items only. */
    EXALT(Selection.AFFIX),
    /** Moves the selected donor affix onto the target's chosen empty slot. The donor is destroyed. */
    GRAFT(Selection.EMPTY_SLOT_AND_DONOR_AFFIX);

    /** What the player must pick before the operation can fire. */
    public enum Selection {
        NONE,
        AFFIX,
        EMPTY_SLOT_AND_DEFINITION,
        EMPTY_SLOT_AND_DONOR_AFFIX
    }

    private final Selection selection;

    PhaseArrayAction(Selection selection) {
        this.selection = selection;
    }

    public Selection selection() {
        return selection;
    }

    public boolean needsDonor() {
        return selection == Selection.EMPTY_SLOT_AND_DONOR_AFFIX;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "rngtech.phase_array.action." + getSerializedName();
    }
}
