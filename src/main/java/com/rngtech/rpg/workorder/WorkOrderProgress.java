package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineTraits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** The progress a Job Chip pulled out of its drive bay keeps, so inserting it again resumes where the machine left off. */
public record WorkOrderProgress(List<Long> progress, long bestStreak, long rushDeadline) {
    public static final WorkOrderProgress NONE = new WorkOrderProgress(List.of(), 0, 0);

    public static final Codec<WorkOrderProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.listOf().optionalFieldOf("progress", List.of()).forGetter(WorkOrderProgress::progress),
            Codec.LONG.optionalFieldOf("best_streak", 0L).forGetter(WorkOrderProgress::bestStreak),
            Codec.LONG.optionalFieldOf("rush_deadline", 0L).forGetter(WorkOrderProgress::rushDeadline)
    ).apply(instance, WorkOrderProgress::new));

    public static final StreamCodec<ByteBuf, WorkOrderProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(WorkObjective.MAX_PARTS)), WorkOrderProgress::progress,
            ByteBufCodecs.VAR_LONG, WorkOrderProgress::bestStreak,
            ByteBufCodecs.VAR_LONG, WorkOrderProgress::rushDeadline,
            WorkOrderProgress::new);

    public WorkOrderProgress {
        progress = progress == null ? List.of() : List.copyOf(progress.stream().map(value -> Math.max(0L, value)).toList());
        bestStreak = Math.max(0, bestStreak);
        rushDeadline = Math.max(0, rushDeadline);
    }

    public static WorkOrderProgress of(LoadedWorkOrder loaded) {
        return new WorkOrderProgress(loaded.progress(), loaded.bestStreak(), loaded.rushDeadline());
    }

    public boolean isNone() {
        return progress.stream().allMatch(value -> value == 0) && bestStreak == 0 && rushDeadline == 0;
    }

    /** {@code order} loaded with this progress. */
    public LoadedWorkOrder load(WorkOrder order, MachineTraits traits) {
        return new LoadedWorkOrder(order, traits, progress, bestStreak, rushDeadline);
    }
}
