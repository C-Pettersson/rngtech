package com.rngtech.content.cable;

import net.minecraft.core.Direction;

/** Executable checks for which cable arm a Wrench click targets. */
public final class CableArmTargetChecks {
    private static int checks;

    private CableArmTargetChecks() {
    }

    public static void run() {
        topOfAnArmTargetsThatArm();
        coreTargetsNoArm();
        armEndsTargetTheirArm();
        System.out.println("Cable arm targeting: " + checks + " checks passed");
    }

    /** Clicking the top of the east arm in a straight east-west run must target east, not up. */
    private static void topOfAnArmTargetsThatArm() {
        require(CableArmTarget.armAt(0.8, 10.0 / 16.0, 0.5) == Direction.EAST, "top of the east arm targets east");
        require(CableArmTarget.armAt(0.2, 6.0 / 16.0, 0.5) == Direction.WEST, "underside of the west arm targets west");
        require(CableArmTarget.armAt(0.5, 0.9, 10.0 / 16.0) == Direction.UP, "side of the up arm targets up");
    }

    /** A core face hit leaves the clicked face to decide, which is how a disabled link is re-enabled. */
    private static void coreTargetsNoArm() {
        require(CableArmTarget.armAt(0.5, 10.0 / 16.0, 0.5) == null, "top of the core targets no arm");
        require(CableArmTarget.armAt(10.0 / 16.0, 0.5, 0.5) == null, "east face of the core targets no arm");
    }

    private static void armEndsTargetTheirArm() {
        require(CableArmTarget.armAt(0.5, 0.5, 0.0) == Direction.NORTH, "north arm end targets north");
        require(CableArmTarget.armAt(0.5, 0.0, 0.5) == Direction.DOWN, "down arm end targets down");
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
