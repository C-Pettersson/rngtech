package com.rngtech.rpg;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** One stat's final value with every recorded contribution, in the order the accumulator combines them. */
public record StatBreakdown(MachineStat stat, List<Term> terms, double finalValue) {
    public static final StreamCodec<RegistryFriendlyByteBuf, StatBreakdown> STREAM_CODEC = StreamCodec.composite(
            MachineStat.STREAM_CODEC,
            StatBreakdown::stat,
            Term.STREAM_CODEC.apply(ByteBufCodecs.list()),
            StatBreakdown::terms,
            ByteBufCodecs.DOUBLE,
            StatBreakdown::finalValue,
            StatBreakdown::new
    );

    public StatBreakdown {
        terms = List.copyOf(terms);
    }

    public enum Kind {
        BASE,
        ADD,
        INCREASED,
        MORE,
        FIXED,
        CEILING,
        SOFT_CAP,
        FLOOR,
        BONUS_MORE,
        /** The increased and reduced bucket is converted into another stat and no longer scales this one. */
        CONVERTED;

        static final StreamCodec<ByteBuf, Kind> STREAM_CODEC =
                ByteBufCodecs.idMapper(index -> values()[index], Kind::ordinal);
    }

    /** One contribution; {@code target} is the stat a {@link Kind#CONVERTED} term feeds, as its ordinal, or -1. */
    public record Term(Kind kind, double value, Component source, int target) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Term> STREAM_CODEC = StreamCodec.composite(
                Kind.STREAM_CODEC,
                Term::kind,
                ByteBufCodecs.DOUBLE,
                Term::value,
                ComponentSerialization.TRUSTED_STREAM_CODEC,
                Term::source,
                ByteBufCodecs.VAR_INT,
                Term::target,
                Term::new
        );

        public Term(Kind kind, double value, Component source) {
            this(kind, value, source, -1);
        }

        /** The stat a conversion feeds, or null for every other term. */
        public MachineStat targetStat() {
            return target >= 0 && target < MachineStat.values().length ? MachineStat.byId(target) : null;
        }
    }

    public List<Term> terms(Kind kind) {
        return terms.stream().filter(term -> term.kind() == kind).toList();
    }

    public double base() {
        return sum(Kind.BASE);
    }

    public double added() {
        return sum(Kind.ADD);
    }

    public double increasedPercent() {
        return sum(Kind.INCREASED);
    }

    public double more() {
        double more = 1.0;
        for (Term term : terms(Kind.MORE)) {
            more *= term.value();
        }
        return more;
    }

    /** The ordinary result before any fixed value or ceiling. */
    public double ordinary() {
        return (base() + added()) * increasedScale() * more();
    }

    /** The floor that wins, which is the highest one, or null when the stat has none. */
    public Double floor() {
        return terms(Kind.FLOOR).stream().map(Term::value).max(Double::compare).orElse(null);
    }

    /** The product of the factors that scale only the part above the floor. */
    public double bonusMore() {
        double more = 1.0;
        for (Term term : terms(Kind.BONUS_MORE)) {
            more *= term.value();
        }
        return more;
    }

    /** The ordinary result after the floor and the bonus factors, before any fixed value or ceiling. */
    public double floored() {
        return MachineStatAccumulator.floored(ordinary(), floor(), bonusMore());
    }

    /** What the increased bucket multiplies by, after any soft cap or dividing reductions. */
    public double increasedScale() {
        if (dividesReductions()) {
            return MachineStatAccumulator.dividedScale(increasesPercent(), reductionsPercent());
        }
        return Math.max(0.0, 1.0 + paidIncreasedPercent() / 100.0);
    }

    /** The increased bucket after this stat's soft cap, or the bucket itself when it has none. */
    public double paidIncreasedPercent() {
        double increased = terms(Kind.CONVERTED).isEmpty() ? increasedPercent() : 0.0;
        Term cap = lowest(Kind.SOFT_CAP);
        return cap == null ? increased : MachineStatAccumulator.softCapped(increased, cap.value());
    }

    public boolean dividesReductions() {
        return MachineStatAccumulator.dividesReductions(stat);
    }

    /** The positive increased terms. */
    public double increasesPercent() {
        return terms(Kind.INCREASED).stream().mapToDouble(Term::value).filter(value -> value > 0.0).sum();
    }

    /** The reduced terms, as a positive percent. */
    public double reductionsPercent() {
        return -terms(Kind.INCREASED).stream().mapToDouble(Term::value).filter(value -> value < 0.0).sum();
    }

    /** The lowest term of {@code kind}, which is the one that wins, or null when there is none. */
    public Term lowest(Kind kind) {
        Term lowest = null;
        for (Term term : terms(kind)) {
            if (lowest == null || term.value() < lowest.value()) {
                lowest = term;
            }
        }
        return lowest;
    }

    /** Recombines the terms with the accumulator's formula; matches {@link #finalValue()}. */
    public double recompute() {
        Term fixed = lowest(Kind.FIXED);
        Term ceiling = lowest(Kind.CEILING);
        double value = fixed == null ? floored() : fixed.value();
        return ceiling == null ? value : Math.min(value, ceiling.value());
    }

    private double sum(Kind kind) {
        double sum = 0.0;
        for (Term term : terms(kind)) {
            sum += term.value();
        }
        return sum;
    }
}
