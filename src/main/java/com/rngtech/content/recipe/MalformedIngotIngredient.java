package com.rngtech.content.recipe;

import com.rngtech.content.item.MalformedIngotItem;
import com.rngtech.content.material.MaterialCatalog;
import com.rngtech.content.material.MaterialEnablement;
import com.rngtech.content.material.MaterialFamily;
import com.rngtech.content.registry.ModRecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.stream.Stream;

/** A Malformed Ingot whose stored material is at least {@code min_stage}, so only failures on late recipes count. */
public record MalformedIngotIngredient(int minStage) implements ICustomIngredient {
    public static final MapCodec<MalformedIngotIngredient> CODEC = Codec.intRange(0, 8)
            .fieldOf("min_stage")
            .xmap(MalformedIngotIngredient::new, MalformedIngotIngredient::minStage);
    public static final StreamCodec<RegistryFriendlyByteBuf, MalformedIngotIngredient> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(MalformedIngotIngredient::new, MalformedIngotIngredient::minStage).cast();

    /** Whether a Malformed Ingot of {@code material} meets the stage; ingots without a known material never do. */
    public static boolean meetsStage(String material, int minStage) {
        return MaterialCatalog.materialStage(material) >= Math.max(0, minStage);
    }

    @Override
    public boolean test(ItemStack stack) {
        return MalformedIngotItem.isMalformedIngot(stack) && meetsStage(MalformedIngotItem.material(stack), minStage);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return MaterialCatalog.materialFamilies().stream()
                .filter(family -> family.stage() >= minStage)
                .map(MaterialFamily::id)
                .filter(MaterialEnablement::isEnabled)
                .map(MalformedIngotItem::create);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ModRecipes.MALFORMED_INGOT_INGREDIENT_TYPE.get();
    }
}
