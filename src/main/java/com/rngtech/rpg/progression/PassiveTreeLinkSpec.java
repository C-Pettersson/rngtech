package com.rngtech.rpg.progression;

import java.util.Objects;

public record PassiveTreeLinkSpec<N extends Enum<N> & PassiveNode>(N first, N second) {
    public PassiveTreeLinkSpec {
        first = Objects.requireNonNull(first, "first");
        second = Objects.requireNonNull(second, "second");
        if (first == second) {
            throw new IllegalArgumentException("Passive tree link cannot target the same node twice: " + first.name());
        }
    }

    public String key() {
        String firstName = first.name();
        String secondName = second.name();
        return firstName.compareTo(secondName) <= 0 ? firstName + "|" + secondName : secondName + "|" + firstName;
    }
}
