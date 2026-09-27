package com.rngtech.rpg.progression;

import java.util.List;

public final class MachinePassiveClasses {
    public static final MachinePassiveClass CRUSHER =
            MachinePassiveClass.of("crusher", 14, 32, 14, "STARTER");
    public static final MachinePassiveClass FURNACE =
            MachinePassiveClass.of("furnace", 14, 14, 32, "STARTER");
    public static final MachinePassiveClass FORESTRY_COMPANION =
            MachinePassiveClass.of("forestry_companion", 20, 20, 20, "STARTER");

    public static final List<MachinePassiveClass> ALL = List.of(CRUSHER, FURNACE, FORESTRY_COMPANION);

    private MachinePassiveClasses() {
    }
}
