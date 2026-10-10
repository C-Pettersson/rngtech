package com.rngtech.content.loot;

import com.rngtech.content.registry.ModLoot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Challenge loot conditions. Number conditions take an optional inclusive {@code min} and {@code max}; the family and
 * ascendancy conditions take an id. Outside a challenge roll they never pass.
 */
public final class ChallengeCondition {
    public enum Range {
        MACHINE_STAGE(ChallengeContext::machineStage),
        RECIPE_STAGE(ChallengeContext::recipeStage),
        CALIBRATION_STABILITY(ChallengeContext::stability),
        INSTABILITY(ChallengeContext::instability),
        MASTERY_LEVEL(ChallengeContext::masteryLevel);

        private final ToIntFunction<ChallengeContext> value;

        Range(ToIntFunction<ChallengeContext> value) {
            this.value = value;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public MapCodec<RangeCondition> codec() {
            return RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.INT.optionalFieldOf("min").forGetter(RangeCondition::min),
                    Codec.INT.optionalFieldOf("max").forGetter(RangeCondition::max)
            ).apply(instance, (min, max) -> new RangeCondition(this, min, max)));
        }
    }

    public enum Id {
        MACHINE_FAMILY("family", ChallengeContext::family),
        ASCENDANCY("ascendancy", ChallengeContext::ascendancy);

        private final String field;
        private final java.util.function.Function<ChallengeContext, String> value;

        Id(String field, java.util.function.Function<ChallengeContext, String> value) {
            this.field = field;
            this.value = value;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public MapCodec<IdCondition> codec() {
            return Codec.STRING.fieldOf(field).xmap(name -> new IdCondition(this, name), IdCondition::id);
        }
    }

    public record RangeCondition(Range range, Optional<Integer> min, Optional<Integer> max) implements LootItemCondition {
        @Override
        public LootItemConditionType getType() {
            return ModLoot.CHALLENGE_RANGES.get(range).get();
        }

        @Override
        public Set<LootContextParam<?>> getReferencedContextParams() {
            return Set.of(ChallengeLoot.CONTEXT);
        }

        @Override
        public boolean test(LootContext context) {
            ChallengeContext challenge = context.getParamOrNull(ChallengeLoot.CONTEXT);
            if (challenge == null) {
                return false;
            }
            int value = range.value.applyAsInt(challenge);
            return min.map(bound -> value >= bound).orElse(true) && max.map(bound -> value <= bound).orElse(true);
        }
    }

    public record IdCondition(Id kind, String id) implements LootItemCondition {
        @Override
        public LootItemConditionType getType() {
            return ModLoot.CHALLENGE_IDS.get(kind).get();
        }

        @Override
        public Set<LootContextParam<?>> getReferencedContextParams() {
            return Set.of(ChallengeLoot.CONTEXT);
        }

        @Override
        public boolean test(LootContext context) {
            ChallengeContext challenge = context.getParamOrNull(ChallengeLoot.CONTEXT);
            return challenge != null && normalized(kind.value.apply(challenge)).equals(normalized(id));
        }

        private static String normalized(String name) {
            int colon = name.indexOf(':');
            return (colon < 0 ? name : name.substring(colon + 1)).toLowerCase(Locale.ROOT);
        }
    }

    private ChallengeCondition() {
    }
}
