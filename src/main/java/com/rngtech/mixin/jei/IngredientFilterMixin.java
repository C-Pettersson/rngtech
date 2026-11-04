package com.rngtech.mixin.jei;

import com.rngtech.compat.jei.JeiSearchQuery;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "mezz.jei.gui.ingredients.IngredientFilter", remap = false)
public abstract class IngredientFilterMixin {
    @ModifyVariable(method = "parseSearchTokens", at = @At("HEAD"), argsOnly = true, require = 0)
    private String rngtech$quoteSearchTerms(String filterText) {
        return JeiSearchQuery.quotePhrases(filterText);
    }
}
