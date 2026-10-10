package com.rngtech.content.loot;

import com.rngtech.content.item.UnidentifiedTraitRoll;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModLoot;
import com.rngtech.rpg.unique.UniqueDefinition;
import com.rngtech.rpg.unique.UniqueItems;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Drops a Unique unidentified with a seeded roll, so the copy's values are fixed when it drops and revealed when the
 * player identifies it. Uniques without ranged lines and other items pass through unchanged.
 */
public final class UnidentifiedUniqueFunction extends LootItemConditionalFunction {
    public static final String LOOT_SOURCE = "loot";
    public static final MapCodec<UnidentifiedUniqueFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).apply(instance, UnidentifiedUniqueFunction::new)
    );

    private UnidentifiedUniqueFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<UnidentifiedUniqueFunction> getType() {
        return ModLoot.UNIDENTIFIED_UNIQUE.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        return seed(stack, context.getRandom().nextLong());
    }

    /** Marks {@code stack} unidentified with {@code seed}; zero means unseeded, so it is replaced. */
    public static ItemStack seed(ItemStack stack, long seed) {
        UniqueDefinition unique = UniqueItems.definition(stack);
        if (unique == null || !unique.hasRangedLines() || stack.has(ModDataComponents.MACHINE_TRAITS.get())) {
            return stack;
        }
        stack.set(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get(), new UnidentifiedTraitRoll(
                seed == UnidentifiedTraitRoll.UNSEEDED ? 1L : seed,
                "unique/" + unique.id(),
                unique.slotStage(),
                LOOT_SOURCE
        ));
        return stack;
    }
}
