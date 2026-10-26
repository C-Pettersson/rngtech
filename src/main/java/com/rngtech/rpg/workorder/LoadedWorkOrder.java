package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineTraits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * A Job Chip in a machine's drive bay: the job and its traits as the chip carried them, plus each part's progress, the
 * best streak, and the Rush deadline. {@code rushDeadline} is world game time, or 0 without Rush. {@code workCarry} is
 * recipe time counted toward a work part but short of one whole item, in ticks.
 */
public record LoadedWorkOrder(WorkOrder order, MachineTraits traits, List<Long> progress, long bestStreak, long rushDeadline,
        long workCarry) {
    public static final Codec<LoadedWorkOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WorkOrder.CODEC.fieldOf("order").forGetter(LoadedWorkOrder::order),
            MachineTraits.CODEC.optionalFieldOf("traits", MachineTraits.EMPTY).forGetter(LoadedWorkOrder::traits),
            Codec.LONG.listOf().optionalFieldOf("progress", List.of()).forGetter(LoadedWorkOrder::progress),
            Codec.LONG.optionalFieldOf("best_streak", 0L).forGetter(LoadedWorkOrder::bestStreak),
            Codec.LONG.optionalFieldOf("rush_deadline", 0L).forGetter(LoadedWorkOrder::rushDeadline),
            Codec.LONG.optionalFieldOf("work_carry", 0L).forGetter(LoadedWorkOrder::workCarry)
    ).apply(instance, LoadedWorkOrder::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoadedWorkOrder> STREAM_CODEC = new StreamCodec<>() {
        @Override public LoadedWorkOrder decode(RegistryFriendlyByteBuf buffer) {
            return new LoadedWorkOrder(WorkOrder.STREAM_CODEC.decode(buffer), MachineTraits.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(WorkObjective.MAX_PARTS)).decode(buffer), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readVarLong());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, LoadedWorkOrder loaded) {
            WorkOrder.STREAM_CODEC.encode(buffer, loaded.order); MachineTraits.STREAM_CODEC.encode(buffer, loaded.traits);
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(WorkObjective.MAX_PARTS)).encode(buffer, loaded.progress);
            buffer.writeVarLong(loaded.bestStreak); buffer.writeVarLong(loaded.rushDeadline); buffer.writeVarLong(loaded.workCarry);
        }
    };

    public LoadedWorkOrder {
        traits = traits == null ? MachineTraits.EMPTY : traits;
        List<WorkPart> parts = order.objective().parts();
        List<Long> clamped = new ArrayList<>(parts.size());
        long total = 0;
        for (int index = 0; index < parts.size(); index++) {
            long value = progress != null && index < progress.size() ? progress.get(index) : 0L;
            long part = Mth.clamp(value, 0, parts.get(index).target());
            clamped.add(part);
            total += part;
        }
        progress = List.copyOf(clamped);
        bestStreak = Math.max(total, bestStreak);
        rushDeadline = Math.max(0, rushDeadline);
        workCarry = Math.max(0, workCarry);
    }

    public LoadedWorkOrder(WorkOrder order, MachineTraits traits, List<Long> progress, long bestStreak, long rushDeadline) {
        this(order, traits, progress, bestStreak, rushDeadline, 0);
    }

    /** A freshly loaded chip with no progress. */
    public LoadedWorkOrder(WorkOrder order, MachineTraits traits) {
        this(order, traits, List.of(), 0, 0);
    }

    /** The part's progress set to {@code value}, kept within its target. */
    public LoadedWorkOrder withPartProgress(int part, long value) {
        List<Long> next = new ArrayList<>(progress);
        next.set(part, value);
        return new LoadedWorkOrder(order, traits, next, bestStreak, rushDeadline, workCarry);
    }

    /** Every part's progress summed. */
    public long totalProgress() {
        long total = 0;
        for (long part : progress) { total += part; }
        return total;
    }

    public boolean rushed() { return rushDeadline > 0; }

    /** A Rush chip whose deadline has passed before its job was done. A finished chip never expires. */
    public boolean expired(long gameTime) { return rushed() && !fulfilled() && gameTime >= rushDeadline; }

    /** This chip with {@code value} as its job, such as with Rush's larger reward. */
    public LoadedWorkOrder withOrder(WorkOrder value) {
        return new LoadedWorkOrder(value, traits, progress, bestStreak, rushDeadline, workCarry);
    }

    /** This chip with {@code value} ticks of recipe time carried toward its next whole item of work. */
    public LoadedWorkOrder withWorkCarry(long value) {
        return new LoadedWorkOrder(order, traits, progress, bestStreak, rushDeadline, value);
    }

    /** This chip with every part's progress back at 0, as a reset condition leaves it. Rush keeps its deadline. */
    public LoadedWorkOrder withoutProgress() {
        return new LoadedWorkOrder(order, traits, List.of(), 0, rushDeadline);
    }

    /** This chip under Rush, due at game time {@code deadline}. */
    public LoadedWorkOrder withRush(long deadline) {
        return new LoadedWorkOrder(order, traits, progress, bestStreak, deadline, workCarry);
    }

    /** Every part has reached its target. */
    public boolean fulfilled() {
        List<WorkPart> parts = order.objective().parts();
        for (int index = 0; index < parts.size(); index++) {
            if (progress.get(index) < parts.get(index).target()) { return false; }
        }
        return true;
    }
}
