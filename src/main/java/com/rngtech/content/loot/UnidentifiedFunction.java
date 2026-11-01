package com.rngtech.content.loot;

import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.registry.ModLoot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Drops a trait-rolling item, such as a jewel, Unidentified, as a crafted one leaves the grid: each player rolls their
 * own copy when they identify it. Items that roll no traits pass through unchanged.
 */
public final class UnidentifiedFunction extends LootItemConditionalFunction {
    public static final MapCodec<UnidentifiedFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).apply(instance, UnidentifiedFunction::new)
    );

    private UnidentifiedFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<UnidentifiedFunction> getType() {
        return ModLoot.UNIDENTIFIED.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        return CraftedTraitOutputs.seedIfMissing(stack, context.getRandom());
    }
}
