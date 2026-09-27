package com.rngtech.content.menu;

import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.PassiveProgressionView;

import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;

import java.util.function.IntPredicate;
import java.util.function.IntSupplier;

public final class MasteryMenuSupport {
    public static final int FIELD_COUNT = 10;

    public static final int FIELD_XP_LOW = 0;
    public static final int FIELD_XP_HIGH = 1;
    public static final int FIELD_LEVEL = 2;
    public static final int FIELD_XP_IN_LEVEL = 3;
    public static final int FIELD_XP_TO_NEXT_LEVEL = 4;
    public static final int FIELD_UNSPENT_POINTS = 5;
    public static final int FIELD_UNLOCKED_NODE_MASK_LOW = 6;
    public static final int FIELD_UNLOCKED_NODE_MASK_HIGH = 7;
    public static final int FIELD_UNLOCKED_NODE_MASK_HIGH_LOW = 8;
    public static final int FIELD_UNLOCKED_NODE_MASK_HIGH_HIGH = 9;

    public static int get(MachineProgressionState progression, int field) {
        return switch (field) {
            case FIELD_XP_LOW -> (int) progression.xp();
            case FIELD_XP_HIGH -> (int) (progression.xp() >>> Integer.SIZE);
            case FIELD_LEVEL -> progression.level();
            case FIELD_XP_IN_LEVEL -> clampLongToInt(progression.xpInCurrentLevel());
            case FIELD_XP_TO_NEXT_LEVEL -> clampLongToInt(progression.xpNeededForNextLevel());
            case FIELD_UNSPENT_POINTS -> progression.unspentPoints();
            case FIELD_UNLOCKED_NODE_MASK_LOW -> (int) progression.unlockedNodeMask();
            case FIELD_UNLOCKED_NODE_MASK_HIGH -> (int) (progression.unlockedNodeMask() >>> Integer.SIZE);
            case FIELD_UNLOCKED_NODE_MASK_HIGH_LOW -> (int) progression.unlockedNodeMaskHigh();
            case FIELD_UNLOCKED_NODE_MASK_HIGH_HIGH -> (int) (progression.unlockedNodeMaskHigh() >>> Integer.SIZE);
            default -> 0;
        };
    }

    public static long machineXp(ContainerData data, int baseIndex) {
        return Integer.toUnsignedLong(data.get(baseIndex + FIELD_XP_LOW))
                | Integer.toUnsignedLong(data.get(baseIndex + FIELD_XP_HIGH)) << Integer.SIZE;
    }

    public static int machineLevel(ContainerData data, int baseIndex) {
        return data.get(baseIndex + FIELD_LEVEL);
    }

    public static int machineXpInLevel(ContainerData data, int baseIndex) {
        return data.get(baseIndex + FIELD_XP_IN_LEVEL);
    }

    public static int machineXpToNextLevel(ContainerData data, int baseIndex) {
        return data.get(baseIndex + FIELD_XP_TO_NEXT_LEVEL);
    }

    public static float machineXpProgress(ContainerData data, int baseIndex) {
        int needed = machineXpToNextLevel(data, baseIndex);
        return needed <= 0 ? 1.0F : Mth.clamp(machineXpInLevel(data, baseIndex) / (float) needed, 0.0F, 1.0F);
    }

    public static int unspentPassivePoints(ContainerData data, int baseIndex) {
        return data.get(baseIndex + FIELD_UNSPENT_POINTS);
    }

    public static long unlockedPassiveNodeMask(ContainerData data, int baseIndex) {
        return Integer.toUnsignedLong(data.get(baseIndex + FIELD_UNLOCKED_NODE_MASK_LOW))
                | Integer.toUnsignedLong(data.get(baseIndex + FIELD_UNLOCKED_NODE_MASK_HIGH)) << Integer.SIZE;
    }

    public static long unlockedPassiveNodeMaskHigh(ContainerData data, int baseIndex) {
        return Integer.toUnsignedLong(data.get(baseIndex + FIELD_UNLOCKED_NODE_MASK_HIGH_LOW))
                | Integer.toUnsignedLong(data.get(baseIndex + FIELD_UNLOCKED_NODE_MASK_HIGH_HIGH)) << Integer.SIZE;
    }

    public static boolean hasPassiveNodeIndex(ContainerData data, int baseIndex, int index) {
        if (index < 0) {
            return false;
        }
        long nodeMask = 1L << (index % Long.SIZE);
        long bank = index < Long.SIZE
                ? unlockedPassiveNodeMask(data, baseIndex)
                : unlockedPassiveNodeMaskHigh(data, baseIndex);
        return (bank & nodeMask) == nodeMask;
    }

    public static PassiveProgressionView progressionView(
            IntPredicate hasNode,
            IntSupplier level,
            IntSupplier unspentPoints
    ) {
        return new PassiveProgressionView() {
            @Override
            public boolean hasNode(int index) {
                return hasNode.test(index);
            }

            @Override
            public int level() {
                return level.getAsInt();
            }

            @Override
            public int unspentPoints() {
                return unspentPoints.getAsInt();
            }
        };
    }

    private static int clampLongToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private MasteryMenuSupport() {
    }
}
