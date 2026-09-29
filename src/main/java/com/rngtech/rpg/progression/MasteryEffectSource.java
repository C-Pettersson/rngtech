package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Allocated Mastery content that contributes effects: shared-tree nodes and ascendancy nodes. */
public interface MasteryEffectSource {
    String id();
    List<MachineModifierEffect> effects();
    List<MegaPassiveNode.TaggedEffect> tagged();
    Set<String> behaviors();
    Map<MachineStat, Double> fixed();
    Map<MachineStat, Double> ceilings();
    List<MegaPassiveNode.AttributeScaling> scaling();
    Map<PassiveStatType, Integer> passive();
    int recipeHardnessCeiling();
}
