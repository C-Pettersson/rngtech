package com.rngtech.rpg.unique;

import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One catalog Unique. {@code id} is the item path, such as {@code fortress_heater_element}; {@code baseProfile} names the
 * normal material whose base stats the Unique starts from, or is empty.
 */
public record UniqueDefinition(
        String id,
        UniqueHost host,
        int slotStage,
        String baseProfile,
        List<UniqueStatLine> lines,
        List<MachineBehavior> behaviors,
        String sourceKey
) {
    public UniqueDefinition {
        lines = List.copyOf(lines);
        behaviors = List.copyOf(behaviors);
    }

    public String translationKey() {
        return "item.rngtech." + id;
    }

    public String descriptionKey() {
        return "rngtech.unique." + id + ".description";
    }

    public boolean hasRangedLines() {
        return lines.stream().anyMatch(UniqueStatLine::ranged);
    }

    public Optional<UniqueStatLine> line(com.rngtech.rpg.MachineStat stat) {
        return lines.stream().filter(line -> line.stat() == stat).findFirst();
    }

    /** The authored value of every line on a copy: fixed values, then stored rolls clamped into range, else midpoints. */
    public List<Double> values(List<MachineModifier> stored) {
        List<Double> values = new ArrayList<>();
        for (UniqueStatLine line : lines) {
            values.add(value(line, stored));
        }
        return values;
    }

    public double value(UniqueStatLine line, List<MachineModifier> stored) {
        if (!line.ranged()) {
            return line.worst();
        }
        return stored.stream()
                .filter(line::matches)
                .findFirst()
                .map(modifier -> line.clamp(line.authoredValue(modifier.value())))
                .orElseGet(line::midpoint);
    }

    /** Every ranged line rolled at {@code values}, as stored modifiers. */
    public List<MachineModifier> rollModifiers(List<Double> values) {
        List<MachineModifier> rolls = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).ranged()) {
                rolls.add(lines.get(index).rollModifier(values.get(index)));
            }
        }
        return rolls;
    }

    /** Identified traits: every ranged line rolled uniformly inside its range. Fixed lines need no storage. */
    public MachineTraits roll(RandomSource random) {
        List<Double> values = new ArrayList<>();
        for (UniqueStatLine line : lines) {
            values.add(line.roll(random));
        }
        return new MachineTraits(Rarity.UNIQUE, 0, rollModifiers(values));
    }

    /** Average roll quality of the ranged lines, or 1 when the Unique has none. */
    public double quality(List<MachineModifier> stored) {
        return lines.stream()
                .filter(UniqueStatLine::ranged)
                .mapToDouble(line -> line.quality(value(line, stored)))
                .average()
                .orElse(1.0);
    }

    public static List<MachineModifier> storedRolls(List<MachineModifier> modifiers) {
        return modifiers.stream().filter(modifier -> modifier.slot() == ModifierSlot.UNIQUE).toList();
    }
}
