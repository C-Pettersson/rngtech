package com.rngtech.content.loot;

import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModLoot;
import com.rngtech.rpg.workorder.WorkOrder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Set;

/**
 * Sets a Glyph's tier to the rewarded order's tier plus {@code offset}, kept within I to V. Other items, and rolls
 * outside a Work Order reward, pass through unchanged.
 */
public final class SetOrderTierFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetOrderTierFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
            .and(Codec.INT.optionalFieldOf("offset", 0).forGetter(function -> function.offset))
            .apply(instance, SetOrderTierFunction::new));

    private final int offset;

    private SetOrderTierFunction(List<LootItemCondition> conditions, int offset) {
        super(conditions);
        this.offset = offset;
    }

    @Override
    public LootItemFunctionType<SetOrderTierFunction> getType() {
        return ModLoot.SET_ORDER_TIER.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(ChallengeLoot.CONTEXT);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        ChallengeContext challenge = context.getParamOrNull(ChallengeLoot.CONTEXT);
        if (challenge == null || challenge.orderTier() <= 0) {
            return stack;
        }
        int tier = Mth.clamp(challenge.orderTier() + offset, WorkOrder.MIN_TIER, WorkOrder.MAX_TIER);
        if (GlyphItem.isGlyph(stack)) {
            stack.set(ModDataComponents.GLYPH_TIER.get(), tier);
        }
        return stack;
    }
}
