package com.rngtech.content.loot;

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
 * Sets a reward's count from {@code counts}, one value per tier from I to V, by the rewarded job's tier. Bounty then
 * scales that count when the reward is rolled. Outside a Work Order reward the stack passes through unchanged.
 */
public final class SetOrderCountFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetOrderCountFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
            .and(Codec.intRange(1, 99).listOf(WorkOrder.MAX_TIER, WorkOrder.MAX_TIER).fieldOf("counts").forGetter(function -> function.counts))
            .apply(instance, SetOrderCountFunction::new));

    private final List<Integer> counts;

    private SetOrderCountFunction(List<LootItemCondition> conditions, List<Integer> counts) {
        super(conditions);
        this.counts = List.copyOf(counts);
    }

    @Override
    public LootItemFunctionType<SetOrderCountFunction> getType() {
        return ModLoot.SET_ORDER_COUNT.get();
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
        return stack.copyWithCount(counts.get(Mth.clamp(challenge.orderTier(), WorkOrder.MIN_TIER, WorkOrder.MAX_TIER) - 1));
    }
}
