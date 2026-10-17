package com.rngtech.content.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The highest ore hardness level an item mines, set per item in the {@code rngtech:ore_reach} data map. */
public record OreReach(int level) {
    public static final Codec<OreReach> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, OreHardness.MAX_LEVEL).fieldOf("level").forGetter(OreReach::level)
    ).apply(instance, OreReach::new));
}
