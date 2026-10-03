package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** One node of an ascendancy tree. Every node except the root has exactly one parent in the same ascendancy. */
public record AscendancyNode(
        String ascendancy, String id, Kind kind, String parent, int x, int y,
        List<MachineModifierEffect> effects, List<MegaPassiveNode.TaggedEffect> tagged, Set<String> behaviors,
        Map<MachineStat, Double> fixed, Map<MachineStat, Double> ceilings,
        List<MegaPassiveNode.AttributeScaling> scaling, Map<PassiveStatType, Integer> passive, int recipeHardnessCeiling
) implements MasteryEffectSource {
    public enum Kind { ROOT, SMALL, NOTABLE }

    public String translationKey() { return "rngtech.mastery.ascendancy." + ascendancy + "." + id; }

    /** Roots and notables draw {@code textures/gui/mastery/ascendancy/<ascendancy>/<id>.png}; small nodes show their first stat. */
    public String masteryIconKey() {
        return kind == Kind.SMALL ? MegaPassiveNode.statIconKey(effects, tagged) : "ascendancy/" + ascendancy + "/" + id;
    }

    public boolean grantsNothing() {
        return effects.isEmpty() && tagged.isEmpty() && behaviors.isEmpty() && fixed.isEmpty() && ceilings.isEmpty()
                && scaling.isEmpty() && passive.isEmpty() && recipeHardnessCeiling <= 0;
    }
}
