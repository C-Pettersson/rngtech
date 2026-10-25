package com.rngtech.rpg.workorder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * One counted part of a job: a verb, a filter with its minimum or item, and the count that completes it. The verb and
 * filter stay names, so a part whose verb or filter a later version drops still loads; it just stops counting.
 */
public record WorkPart(String type, String filter, int minimum, String item, long target) {
    public static final Codec<WorkPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(WorkPart::type),
            Codec.STRING.optionalFieldOf("filter", "any").forGetter(WorkPart::filter),
            Codec.INT.optionalFieldOf("min", 0).forGetter(WorkPart::minimum),
            Codec.STRING.optionalFieldOf("item", "").forGetter(WorkPart::item),
            Codec.LONG.fieldOf("target").forGetter(WorkPart::target)
    ).apply(instance, WorkPart::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkPart> STREAM_CODEC = new StreamCodec<>() {
        @Override public WorkPart decode(RegistryFriendlyByteBuf buffer) {
            return new WorkPart(buffer.readUtf(32), buffer.readUtf(32), buffer.readVarInt(), buffer.readUtf(256), buffer.readVarLong());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, WorkPart part) {
            buffer.writeUtf(part.type, 32); buffer.writeUtf(part.filter, 32); buffer.writeVarInt(part.minimum);
            buffer.writeUtf(part.item, 256); buffer.writeVarLong(part.target);
        }
    };

    public WorkPart {
        type = type == null ? "" : type;
        filter = filter == null ? "" : filter;
        minimum = Math.max(0, minimum);
        item = item == null ? "" : item;
        target = Math.max(1, target);
    }

    public WorkPart(WorkObjectiveType type, WorkFilter filter, int minimum, String item, long target) {
        this(type.id(), filter.id(), minimum, item, target);
    }

    /** Whether {@code report} counts toward this part. */
    public boolean counts(WorkReport report) {
        if (!report.type().id().equals(type)) { return false; }
        WorkFilter known = WorkFilter.byId(filter);
        if (known == null) { return false; }
        return switch (known) {
            case ANY -> true;
            case STAGE -> report.stage() >= minimum;
            case SETTING -> report.setting() >= minimum;
            case ITEM -> !item.isEmpty() && item.equals(report.item());
        };
    }
}
