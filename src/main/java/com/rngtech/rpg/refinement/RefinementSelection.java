package com.rngtech.rpg.refinement;

import com.rngtech.rpg.ModifierSlot;

public record RefinementSelection(Kind kind, int affixIndex, ModifierSlot emptySlot) {
    private static final RefinementSelection NONE = new RefinementSelection(Kind.NONE, -1, ModifierSlot.PREFIX);

    public static RefinementSelection none() {
        return NONE;
    }

    public static RefinementSelection existingModifier(int affixIndex) {
        return new RefinementSelection(Kind.EXISTING_MODIFIER, Math.max(0, affixIndex), ModifierSlot.PREFIX);
    }

    public static RefinementSelection emptySlot(ModifierSlot slot) {
        if (!slot.isAffix()) {
            throw new IllegalArgumentException("Refinement selection slot must be an affix slot: " + slot);
        }
        return new RefinementSelection(Kind.EMPTY_SLOT, -1, slot);
    }

    public boolean isNone() {
        return kind == Kind.NONE;
    }

    public enum Kind {
        NONE,
        EXISTING_MODIFIER,
        EMPTY_SLOT
    }
}
