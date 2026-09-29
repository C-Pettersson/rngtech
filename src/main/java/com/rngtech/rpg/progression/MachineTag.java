package com.rngtech.rpg.progression;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Machine groups that a tagged passive effect can target. Keystones tag their payoff with the group that also pays
 * their cost, so a machine the cost cannot reach does not receive the payoff for free.
 */
public enum MachineTag {
    /** Furnace, Alloy Furnace, and Metal Press: heat machines whose maximum temperature accepts absolute limits. */
    HEATED,
    CRUSHING,
    /** Machines that can produce bonus output through Output Amount, Super Output, or salvage. */
    BONUS_OUTPUT,
    ENERGY_BUFFER;

    public String translationKey() {
        return "rngtech.mastery.tag." + name().toLowerCase(Locale.ROOT);
    }

    public List<MachineMasteryFamily> members() {
        return Arrays.stream(MachineMasteryFamily.values()).filter(family -> family.has(this)).toList();
    }
}
