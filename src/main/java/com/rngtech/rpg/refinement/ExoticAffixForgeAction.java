package com.rngtech.rpg.refinement;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum ExoticAffixForgeAction implements StringRepresentable {
    REFORGE("reforge"),
    REFINE_ALL("refine_all"),
    UPGRADE_RANDOM_MODIFIER("upgrade_random_modifier"),
    REFINE_SELECTED_MODIFIER("refine_selected_modifier"),
    ADD_MODIFIER("add_modifier"),
    REMOVE_SELECTED_MODIFIER("remove_selected_modifier");

    public static final Codec<ExoticAffixForgeAction> CODEC =
            StringRepresentable.fromEnum(ExoticAffixForgeAction::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, ExoticAffixForgeAction> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ExoticAffixForgeAction decode(RegistryFriendlyByteBuf buffer) {
                    return buffer.readEnum(ExoticAffixForgeAction.class);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ExoticAffixForgeAction action) {
                    buffer.writeEnum(action);
                }
            };

    private final String serializedName;

    ExoticAffixForgeAction(String serializedName) {
        this.serializedName = serializedName;
    }

    public static ExoticAffixForgeAction byOrdinal(int ordinal) {
        ExoticAffixForgeAction[] actions = values();
        return ordinal >= 0 && ordinal < actions.length ? actions[ordinal] : REFINE_ALL;
    }

    public String translationKey() {
        return "rngtech.exotic_affix_forge.action." + serializedName;
    }

    public boolean requiresExistingModifier() {
        return this == REFINE_SELECTED_MODIFIER || this == REMOVE_SELECTED_MODIFIER;
    }

    public boolean requiresEmptySlot() {
        return this == ADD_MODIFIER;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
