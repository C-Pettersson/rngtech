package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.loot.ChallengeCondition;
import com.rngtech.content.loot.ChallengeLoot;
import com.rngtech.content.loot.UnidentifiedUniqueFunction;
import com.rngtech.content.loot.UniqueLootEnabledCondition;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModLoot {
    private static final DeferredRegister<LootItemConditionType> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, RNGTech.MOD_ID);
    private static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, RNGTech.MOD_ID);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> UNIQUE_LOOT_ENABLED =
            CONDITIONS.register("unique_loot_enabled", () -> new LootItemConditionType(UniqueLootEnabledCondition.CODEC));
    public static final Map<ChallengeCondition.Range, DeferredHolder<LootItemConditionType, LootItemConditionType>> CHALLENGE_RANGES =
            registerRanges();
    public static final Map<ChallengeCondition.Id, DeferredHolder<LootItemConditionType, LootItemConditionType>> CHALLENGE_IDS =
            registerIds();
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<UnidentifiedUniqueFunction>> UNIDENTIFIED_UNIQUE =
            FUNCTIONS.register("unidentified_unique", () -> new LootItemFunctionType<>(UnidentifiedUniqueFunction.CODEC));

    public static void register(IEventBus bus) {
        ChallengeLoot.registerParamSet();
        CONDITIONS.register(bus);
        FUNCTIONS.register(bus);
    }

    private static Map<ChallengeCondition.Range, DeferredHolder<LootItemConditionType, LootItemConditionType>> registerRanges() {
        Map<ChallengeCondition.Range, DeferredHolder<LootItemConditionType, LootItemConditionType>> holders =
                new EnumMap<>(ChallengeCondition.Range.class);
        for (ChallengeCondition.Range range : ChallengeCondition.Range.values()) {
            holders.put(range, CONDITIONS.register(range.id(), () -> new LootItemConditionType(range.codec())));
        }
        return Collections.unmodifiableMap(holders);
    }

    private static Map<ChallengeCondition.Id, DeferredHolder<LootItemConditionType, LootItemConditionType>> registerIds() {
        Map<ChallengeCondition.Id, DeferredHolder<LootItemConditionType, LootItemConditionType>> holders =
                new EnumMap<>(ChallengeCondition.Id.class);
        for (ChallengeCondition.Id kind : ChallengeCondition.Id.values()) {
            holders.put(kind, CONDITIONS.register(kind.id(), () -> new LootItemConditionType(kind.codec())));
        }
        return Collections.unmodifiableMap(holders);
    }

    private ModLoot() {
    }
}
