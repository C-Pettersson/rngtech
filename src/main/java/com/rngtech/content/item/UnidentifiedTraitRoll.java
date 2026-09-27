package com.rngtech.content.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record UnidentifiedTraitRoll(long seed, String target, int stage, String source) {
    public static final String CRAFTING_SOURCE = "crafting";
    public static final long UNSEEDED = 0L;

    public static final Codec<UnidentifiedTraitRoll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("seed").forGetter(UnidentifiedTraitRoll::seed),
            Codec.STRING.optionalFieldOf("target", "").forGetter(UnidentifiedTraitRoll::target),
            Codec.INT.optionalFieldOf("stage", 0).forGetter(UnidentifiedTraitRoll::stage),
            Codec.STRING.optionalFieldOf("source", CRAFTING_SOURCE).forGetter(UnidentifiedTraitRoll::source)
    ).apply(instance, UnidentifiedTraitRoll::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnidentifiedTraitRoll> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UnidentifiedTraitRoll decode(RegistryFriendlyByteBuf buffer) {
                    return new UnidentifiedTraitRoll(
                            ByteBufCodecs.VAR_LONG.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer)
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, UnidentifiedTraitRoll value) {
                    ByteBufCodecs.VAR_LONG.encode(buffer, value.seed);
                    ByteBufCodecs.STRING_UTF8.encode(buffer, value.target);
                    ByteBufCodecs.VAR_INT.encode(buffer, value.stage);
                    ByteBufCodecs.STRING_UTF8.encode(buffer, value.source);
                }
            };

    public static UnidentifiedTraitRoll crafting(long seed, String target, int stage) {
        return new UnidentifiedTraitRoll(seed, target, stage, CRAFTING_SOURCE);
    }

    public static UnidentifiedTraitRoll unseededCrafting(String target, int stage) {
        return crafting(UNSEEDED, target, stage);
    }

    public boolean isUnseeded() {
        return seed == UNSEEDED;
    }
}
