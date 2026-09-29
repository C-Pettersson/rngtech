package com.rngtech.rpg.progression;

import java.util.List;

public final class MachinePassiveClasses {
    public static final MachinePassiveClass CRUSHER =
            MachinePassiveClass.of("crusher", 0, 20, 0, "start_drive");
    public static final MachinePassiveClass FURNACE =
            MachinePassiveClass.of("furnace", 0, 10, 10, "start_drive_reserve");
    public static final MachinePassiveClass FORESTRY_COMPANION =
            MachinePassiveClass.of("forestry_companion", 10, 10, 0, "start_control_drive");

    public static final List<MachinePassiveClass> ALL = List.of(CRUSHER, FURNACE, FORESTRY_COMPANION);

    private MachinePassiveClasses() {
    }
}
