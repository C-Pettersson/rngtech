package com.rngtech.content.workorder;

import com.rngtech.content.material.MaterialCatalog;
import com.rngtech.content.recipe.CrusherRecipe;
import com.rngtech.content.recipe.FurnaceRecipe;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Which inputs count toward a job part, read from the loaded recipes the same way the machines report their work. */
public final class WorkOrderEligibility {
    private WorkOrderEligibility() {
    }

    /** The stage a Furnace smelt reports: the stage of the material it fails or malforms into, or 0. */
    public static int smeltStage(FurnaceRecipe recipe) {
        return Math.max(0, MaterialCatalog.materialStage(material(recipe)));
    }

    /**
     * Whether a part has a Shift list: a stage part, whose job line names a Processing Level or Stage but not what reaches
     * it. Item parts name their item, and on any-work and heat parts every recipe of the machine counts.
     */
    public static boolean lists(WorkPart part) {
        return WorkFilter.STAGE.id().equals(part.filter()) && WorkObjectiveType.byId(part.type()) != null;
    }

    /**
     * A stage part's Shift list, sorted by name: the Crusher inputs whose recipes need the Processing Level or more, or the
     * materials of the Furnace recipes at the Stage or more. A smelt's stage is its material's, and each material comes in
     * several forms, so a Furnace list names materials instead of items.
     */
    public static List<Component> names(RecipeManager recipes, WorkPart part) {
        return WorkObjectiveType.byId(part.type()) == WorkObjectiveType.SMELT
                ? materials(recipes, part).stream().map(id -> (Component) Component.literal(MaterialCatalog.materialFamily(id).displayName())).toList()
                : items(recipes, part).stream().map(Item::getDescription).toList();
    }

    /** The inputs of the Crusher recipes that need the part's Processing Level or more, sorted by name. */
    public static List<Item> items(RecipeManager recipes, WorkPart part) {
        Set<Item> items = new LinkedHashSet<>();
        for (RecipeHolder<CrusherRecipe> holder : recipes.getAllRecipesFor(ModRecipes.CRUSHER_TYPE.get())) {
            if (holder.value().requiredProcessingLevel() >= part.minimum()) {
                for (ItemStack stack : holder.value().ingredient().getItems()) {
                    items.add(stack.getItem());
                }
            }
        }
        return items.stream().sorted(Comparator.comparing(item -> item.getDescription().getString())).toList();
    }

    /** The material ids of the Furnace recipes at the part's Stage or more, sorted by display name. */
    public static List<String> materials(RecipeManager recipes, WorkPart part) {
        Set<String> materials = new LinkedHashSet<>();
        for (RecipeHolder<FurnaceRecipe> holder : recipes.getAllRecipesFor(ModRecipes.FURNACE_TYPE.get())) {
            if (smeltStage(holder.value()) >= part.minimum()) {
                materials.add(material(holder.value()));
            }
        }
        return materials.stream().sorted(Comparator.comparing(id -> MaterialCatalog.materialFamily(id).displayName())).toList();
    }

    /** The material a Furnace recipe fails or malforms into, which sets its stage. */
    public static String material(FurnaceRecipe recipe) {
        return recipe.failureMaterial().isEmpty() ? recipe.malformedMaterial() : recipe.failureMaterial();
    }
}
