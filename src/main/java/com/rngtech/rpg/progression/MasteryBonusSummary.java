package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierOperation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Allocated Mastery bonuses for one machine, combined the way the stat pipeline combines them. */
public final class MasteryBonusSummary {
    private static final double EPSILON = 1.0E-9;
    private static final List<MachineStat> ATTRIBUTES = List.of(MachineStat.CONTROL, MachineStat.DRIVE, MachineStat.RESERVE);

    public enum Kind { EFFECT, CONVERSION, SCALING, FIXED, CEILING, HARDNESS, PASSIVE, BEHAVIOR }

    /**
     * One summary line. {@code effect} holds the combined change for effects and conversions, the combined per-point
     * value for scaling, and the limit for fixed values and ceilings. {@code attribute} names the source attribute of a
     * conversion or scaling line. Sources list one allocated node per contribution.
     */
    public record Line(Kind kind, boolean active, MachineModifierEffect effect, MachineStat attribute, String behavior,
                       PassiveStatType passive, int amount, List<MegaPassiveNode> sources) {
        public Line {
            sources = List.copyOf(sources);
        }
    }

    private MasteryBonusSummary() { }

    public static List<Line> of(MachineProgressionState state, MasteryApplicability machine, double control, double drive, double reserve) {
        List<MegaPassiveNode> allocated = state.allocatedNodes().stream().map(MegaPassiveTree::node).filter(Objects::nonNull).toList();
        Map<EffectKey, Total> effects = new LinkedHashMap<>();
        Map<ScalingKey, Total> scaling = new LinkedHashMap<>();
        Map<MachineStat, Total> fixed = new EnumMap<>(MachineStat.class);
        Map<MachineStat, Total> ceilings = new EnumMap<>(MachineStat.class);
        Map<PassiveStatType, Total> passive = new EnumMap<>(PassiveStatType.class);
        Map<String, Total> behaviors = new LinkedHashMap<>();
        Total hardness = null;
        for (MegaPassiveNode node : allocated) {
            node.effects().forEach(effect -> addEffect(effects, effect, machine.masterySupports(effect.stat()), node));
            node.tagged().forEach(tagged -> addEffect(effects, tagged.effect(), machine.masterySupports(tagged), node));
            node.scaling().forEach(s -> scaling.computeIfAbsent(new ScalingKey(s.attribute(), s.stat(), s.operation()), key -> new Total(0)).add(s.perPoint(), node));
            node.fixed().forEach((stat, value) -> fixed.computeIfAbsent(stat, key -> new Total(Double.POSITIVE_INFINITY)).min(value, node));
            node.ceilings().forEach((stat, value) -> ceilings.computeIfAbsent(stat, key -> new Total(Double.POSITIVE_INFINITY)).min(value, node));
            node.passive().forEach((stat, value) -> passive.computeIfAbsent(stat, key -> new Total(0)).add(value, node));
            node.behaviors().forEach(behavior -> behaviors.computeIfAbsent(behavior, key -> new Total(0)).add(1, node));
            if (node.recipeHardnessCeiling() > 0) {
                hardness = hardness == null ? new Total(Double.POSITIVE_INFINITY) : hardness;
                hardness.min(node.recipeHardnessCeiling(), node);
            }
        }

        List<Line> lines = new ArrayList<>();
        effects.entrySet().stream().sorted(Comparator.comparing((Map.Entry<EffectKey, Total> entry) -> order(entry.getKey().stat()))
                        .thenComparing(entry -> entry.getKey().bucket()))
                .forEach(entry -> combined(entry.getKey().stat(), entry.getKey().bucket(), entry.getValue().value)
                        .ifPresent(effect -> lines.add(line(Kind.EFFECT, entry.getKey().active(), effect, entry.getValue()))));
        if (!behaviors.containsKey("NO_INHERENT_ATTRIBUTES")) {
            for (MegaPassiveTree.Conversion conversion : MegaPassiveTree.inherentConversions(machine.masteryFamily(), control, drive, reserve)) {
                if (Math.abs(conversion.effect().value()) > EPSILON) {
                    lines.add(new Line(Kind.CONVERSION, machine.masterySupports(conversion.effect().stat()), conversion.effect(), conversion.attribute(), null, null, 0, List.of()));
                }
            }
        }
        scaling.forEach((key, total) -> lines.add(new Line(Kind.SCALING, machine.masterySupports(key.stat()),
                MachineModifierEffect.fixed(key.stat(), key.operation(), total.value), key.attribute(), null, null, 0, total.sources)));
        fixed.forEach((stat, total) -> lines.add(line(Kind.FIXED, machine.masterySupportsAbsolute(stat), MachineModifierEffect.fixed(stat, ModifierOperation.ADD, total.value), total)));
        ceilings.forEach((stat, total) -> lines.add(line(Kind.CEILING, machine.masterySupportsAbsolute(stat), MachineModifierEffect.fixed(stat, ModifierOperation.ADD, total.value), total)));
        if (hardness != null) {
            lines.add(new Line(Kind.HARDNESS, machine.masterySupports(MachineStat.PROCESSING_LEVEL), null, null, null, null, (int) hardness.value, hardness.sources));
        }
        passive.forEach((stat, total) -> lines.add(new Line(Kind.PASSIVE, machine.masteryFamily().supports(stat), null, null, null, stat, (int) total.value, total.sources)));
        behaviors.forEach((behavior, total) -> lines.add(new Line(Kind.BEHAVIOR, machine.masterySupportsBehavior(behavior), null, null, behavior, null, 0, total.sources)));
        return List.copyOf(lines);
    }

    private static void addEffect(Map<EffectKey, Total> effects, MachineModifierEffect effect, boolean active, MegaPassiveNode node) {
        ModifierOperation bucket = switch (effect.operation()) {
            case ADD -> ModifierOperation.ADD;
            case INCREASED_PERCENT, DECREASED_PERCENT -> ModifierOperation.INCREASED_PERCENT;
            case MORE, LESS -> ModifierOperation.MORE;
        };
        Total total = effects.computeIfAbsent(new EffectKey(effect.stat(), bucket, active), key -> new Total(bucket == ModifierOperation.MORE ? 1 : 0));
        switch (effect.operation()) {
            case DECREASED_PERCENT -> total.add(-effect.value(), node);
            case MORE, LESS -> total.multiply(effect.value(), node);
            default -> total.add(effect.value(), node);
        }
    }

    /** Expresses a bucket total with the keyword a single equivalent modifier would use; a neutral total has no line. */
    private static Optional<MachineModifierEffect> combined(MachineStat stat, ModifierOperation bucket, double value) {
        if (bucket == ModifierOperation.MORE) {
            if (Math.abs(value - 1) <= EPSILON) { return Optional.empty(); }
            return Optional.of(MachineModifierEffect.fixed(stat, value > 1 ? ModifierOperation.MORE : ModifierOperation.LESS, value));
        }
        if (Math.abs(value) <= EPSILON) { return Optional.empty(); }
        if (bucket == ModifierOperation.INCREASED_PERCENT && value < 0) {
            return Optional.of(MachineModifierEffect.fixed(stat, ModifierOperation.DECREASED_PERCENT, -value));
        }
        return Optional.of(MachineModifierEffect.fixed(stat, bucket, value));
    }

    private static Line line(Kind kind, boolean active, MachineModifierEffect effect, Total total) {
        return new Line(kind, active, effect, null, null, null, 0, total.sources);
    }

    /** Attributes first, then the remaining stats in declaration order. */
    private static int order(MachineStat stat) {
        int attribute = ATTRIBUTES.indexOf(stat);
        return attribute >= 0 ? attribute - ATTRIBUTES.size() : stat.ordinal();
    }

    private record EffectKey(MachineStat stat, ModifierOperation bucket, boolean active) {
    }

    private record ScalingKey(MachineStat attribute, MachineStat stat, ModifierOperation operation) {
    }

    private static final class Total {
        private double value;
        private final List<MegaPassiveNode> sources = new ArrayList<>();

        private Total(double initial) {
            value = initial;
        }

        private void add(double amount, MegaPassiveNode node) {
            value += amount;
            sources.add(node);
        }

        private void multiply(double factor, MegaPassiveNode node) {
            value *= factor;
            sources.add(node);
        }

        private void min(double limit, MegaPassiveNode node) {
            value = Math.min(value, limit);
            sources.add(node);
        }
    }
}
