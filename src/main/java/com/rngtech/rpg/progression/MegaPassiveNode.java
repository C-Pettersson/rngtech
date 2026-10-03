package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record MegaPassiveNode(
        int index, String id, String name, PassiveNodeKind kind, int x, int y,
        List<String> links, List<MachineModifierEffect> effects, List<TaggedEffect> tagged, Set<String> behaviors,
        Map<MachineStat, Double> fixed, Map<MachineStat, Double> ceilings,
        List<AttributeScaling> scaling, Map<PassiveStatType, Integer> passive, int recipeHardnessCeiling
) implements PassiveNode, MasteryEffectSource {
    public record AttributeScaling(MachineStat attribute, MachineStat stat, ModifierOperation operation, double perPoint) {
    }

    /** An effect that only applies to machines carrying {@code tag}. */
    public record TaggedEffect(MachineTag tag, MachineModifierEffect effect) {
        public boolean appliesTo(MachineMasteryFamily family) {
            return family.has(tag) && family.supports(effect.stat());
        }
    }

    @Override public int requiredLevel() { return 1; }
    @Override public int x() { return x - size() / 2; }
    @Override public int y() { return y - size() / 2; }
    @Override public boolean alwaysAllocated() { return false; }
    @Override public boolean grantsNothing() { return effects.isEmpty() && tagged.isEmpty(); }
    @Override public List<MegaPassiveNode> parents() { return links.stream().map(MegaPassiveTree::node).toList(); }
    @Override public MachineModifier modifier() { return MachineModifier.roll(id, id, ModifierSlot.IMPLICIT, 0, effects); }
    /** Starts and keystones are drawn with their own {@code textures/gui/mastery/<id>.png}; other nodes show their first stat. */
    @Override public String masteryIconKey() {
        if (kind == PassiveNodeKind.STARTER || kind == PassiveNodeKind.KEYSTONE) { return id; }
        return statIconKey(effects, tagged);
    }
    /** The shared stat icon for a node's first effect. */
    static String statIconKey(List<MachineModifierEffect> effects, List<TaggedEffect> tagged) {
        MachineModifierEffect first = !effects.isEmpty() ? effects.getFirst() : !tagged.isEmpty() ? tagged.getFirst().effect() : null;
        if (first == null) { return "stability"; }
        return switch (first.stat()) {
            case DRIVE, PROCESSING_SPEED -> "processing_speed";
            case RESERVE, ENERGY_CAPACITY, ENERGY_CAPACITY_FLAT -> "energy_capacity";
            case OUTPUT_AMOUNT, CRUSHER_SALVAGE_CHANCE -> "output_yield";
            case ENERGY_USAGE -> "energy_efficiency";
            default -> "control";
        };
    }
    @Override public String translationKey() { return "rngtech.mastery.shared." + id; }
    @Override public Set<PassiveNodeFlag> flags() {
        return behaviors.contains("MUTE_MACHINE_SOUND") ? Set.of(PassiveNodeFlag.MUTE_MACHINE_SOUND) : Set.of();
    }
    @Override public int passiveStat(PassiveStatType stat) { return passive.getOrDefault(stat, 0); }
    @Override public boolean isUnlocked(PassiveProgressionView progression) {
        return kind == PassiveNodeKind.STARTER ? id.equals(progression.startNodeId()) : progression.hasNode(index);
    }
    @Override public int size() {
        return switch (kind) { case STARTER -> 40; case TRAVEL -> 12; case NODE -> 18; case NOTABLE -> 34; case KEYSTONE -> 48; };
    }
    public boolean blocksBatteryCell() { return behaviors.contains("BLOCK_BATTERY"); }
    public boolean requiresMatchingCrushHeadStage() { return behaviors.contains("MATCHING_HEAD"); }
    public boolean enablesDenseParallel() { return behaviors.contains("DENSE_PARALLEL"); }
}
