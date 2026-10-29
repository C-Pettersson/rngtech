package com.rngtech.rpg.workorder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A job as it was rolled: the catalog entry it came from, one to {@value #MAX_PARTS} counted parts, and the Bounty bonus
 * of the band its count fell in. The job is done when every part is.
 */
public record WorkObjective(ResourceLocation id, List<WorkPart> parts, int bonus) {
    public static final int MAX_PARTS = 3;

    public static final Codec<WorkObjective> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(WorkObjective::id),
            WorkPart.CODEC.sizeLimitedListOf(MAX_PARTS).fieldOf("parts").forGetter(WorkObjective::parts),
            Codec.INT.optionalFieldOf("bonus", 0).forGetter(WorkObjective::bonus)
    ).apply(instance, WorkObjective::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkObjective> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, WorkObjective::id,
            WorkPart.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PARTS)), WorkObjective::parts,
            ByteBufCodecs.VAR_INT, WorkObjective::bonus,
            WorkObjective::new);

    public WorkObjective {
        parts = List.copyOf(parts.size() > MAX_PARTS ? parts.subList(0, MAX_PARTS) : parts);
        if (parts.isEmpty()) { throw new IllegalArgumentException(id + ": a job needs at least one part"); }
        bonus = Math.max(0, bonus);
    }

    public WorkObjective(ResourceLocation id, WorkPart part) {
        this(id, List.of(part), 0);
    }

    /** The sum of every part's target. */
    public long totalTarget() {
        long total = 0;
        for (WorkPart part : parts) { total += part.target(); }
        return total;
    }
}
