package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.refinement.RefinementOperation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record AffixForgeJeiRecipe(
        ResourceLocation id,
        ItemStack catalyst,
        RefinementOperation operation,
        ItemStack requiredUpgrade,
        List<ItemStack> optionalModifiers,
        String actionKey,
        String selectionKey,
        String costKey,
        String requirementKey
) {
    public AffixForgeJeiRecipe {
        catalyst = catalyst.copy();
        requiredUpgrade = requiredUpgrade.copy();
        optionalModifiers = optionalModifiers.stream().map(ItemStack::copy).toList();
    }

    static List<AffixForgeJeiRecipe> recipes() {
        List<ItemStack> lenses = ModItems.modifierLenses().stream()
                .map(item -> new ItemStack(item.get()))
                .toList();
        List<ItemStack> addModifiers = List.of(
                new ItemStack(ModItems.CONSERVATION_CRYSTAL.get())
        );
        List<ItemStack> randomUpgradeModifiers = List.of(
                new ItemStack(ModItems.FRUGALITY_CRYSTAL.get())
        );
        List<ItemStack> upgradeModifiers = List.of(
                new ItemStack(ModItems.CONSERVATION_CRYSTAL.get()),
                new ItemStack(ModItems.TRANSMUTATION_CRYSTAL.get()),
                new ItemStack(ModItems.RESONANCE_CRYSTAL.get()),
                new ItemStack(ModItems.DESTABILIZATION_CRYSTAL.get())
        );

        return List.of(
                recipe(
                        "affix_injector",
                        new ItemStack(ModItems.AFFIX_INJECTOR.get()),
                        RefinementOperation.ADD_MODIFIER,
                        ItemStack.EMPTY,
                        withLenses(addModifiers, lenses),
                        "rngtech.jei.affix_forge.action.add",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.injector",
                        "rngtech.jei.affix_forge.requirement.base"
                ),
                recipe(
                        "affix_modifier",
                        new ItemStack(ModItems.AFFIX_MODIFIER.get()),
                        RefinementOperation.UPGRADE_RANDOM_MODIFIER,
                        ItemStack.EMPTY,
                        withLenses(randomUpgradeModifiers, lenses),
                        "rngtech.jei.affix_forge.action.random_upgrade",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.random_upgrade",
                        "rngtech.jei.affix_forge.requirement.base"
                ),
                recipe(
                        "affix_upgrade",
                        new ItemStack(ModItems.AFFIX_UPGRADE.get()),
                        RefinementOperation.UPGRADE_SELECTED_MODIFIER,
                        new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()),
                        withLenses(upgradeModifiers, lenses),
                        "rngtech.jei.affix_forge.action.greater_upgrade",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.selected_upgrade",
                        "rngtech.jei.affix_forge.requirement.resonance_matrix"
                ),
                recipe(
                        "ascension_matrix",
                        new ItemStack(ModItems.ASCENSION_MATRIX.get()),
                        RefinementOperation.ASCEND_RARITY,
                        new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()),
                        List.of(),
                        "rngtech.jei.affix_forge.action.ascend",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.all",
                        "rngtech.jei.affix_forge.requirement.resonance_matrix"
                ),
                recipe(
                        "ascension_catalyst",
                        new ItemStack(ModItems.ASCENSION_CATALYST.get()),
                        RefinementOperation.ASCENSION_CATALYST,
                        new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()),
                        List.of(),
                        "rngtech.jei.affix_forge.action.catalyze_ascension",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.ascension_catalyst",
                        "rngtech.jei.affix_forge.requirement.resonance_matrix"
                ),
                recipe(
                        "nullifier_coil",
                        new ItemStack(ModItems.NULLIFIER_COIL.get()),
                        RefinementOperation.REMOVE_MODIFIER,
                        ItemStack.EMPTY,
                        List.of(),
                        "rngtech.jei.affix_forge.action.remove",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.random",
                        "rngtech.jei.affix_forge.requirement.base"
                ),
                recipe(
                        "chaos_crystal",
                        new ItemStack(ModItems.CHAOS_CRYSTAL.get()),
                        RefinementOperation.CHAOS_CRYSTAL,
                        new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()),
                        List.of(),
                        "rngtech.jei.affix_forge.action.reroll",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.random",
                        "rngtech.jei.affix_forge.requirement.resonance_matrix"
                ),
                recipe(
                        "expansion_crystal",
                        new ItemStack(ModItems.EXPANSION_CRYSTAL.get()),
                        RefinementOperation.EXPANSION_CRYSTAL,
                        new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()),
                        List.of(),
                        "rngtech.jei.affix_forge.action.fill",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.each",
                        "rngtech.jei.affix_forge.requirement.resonance_matrix"
                ),
                recipe(
                        "volatile_catalyst",
                        new ItemStack(ModItems.VOLATILE_CATALYST.get()),
                        RefinementOperation.CORRUPT,
                        new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()),
                        List.of(new ItemStack(ModItems.STABILIZATION_CRYSTAL.get())),
                        "rngtech.jei.affix_forge.action.corrupt",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.none",
                        "rngtech.jei.affix_forge.requirement.resonance_matrix"
                ),
                recipe(
                        "null_crystal",
                        new ItemStack(ModItems.NULL_CRYSTAL.get()),
                        RefinementOperation.NULL_CRYSTAL,
                        new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()),
                        List.of(),
                        "rngtech.jei.affix_forge.action.remove",
                        "rngtech.jei.affix_forge.selection.none",
                        "rngtech.jei.affix_forge.cost.random",
                        "rngtech.jei.affix_forge.requirement.resonance_matrix"
                )
        );
    }

    private static List<ItemStack> withLenses(List<ItemStack> modifiers, List<ItemStack> lenses) {
        List<ItemStack> stacks = new ArrayList<>(modifiers);
        stacks.addAll(lenses);
        return List.copyOf(stacks);
    }

    private static AffixForgeJeiRecipe recipe(
            String path,
            ItemStack catalyst,
            RefinementOperation operation,
            ItemStack requiredUpgrade,
            List<ItemStack> optionalModifiers,
            String actionKey,
            String selectionKey,
            String costKey,
            String requirementKey
    ) {
        return new AffixForgeJeiRecipe(
                RNGTech.id("affix_forge/" + path),
                catalyst,
                operation,
                requiredUpgrade,
                optionalModifiers,
                actionKey,
                selectionKey,
                costKey,
                requirementKey
        );
    }
}
