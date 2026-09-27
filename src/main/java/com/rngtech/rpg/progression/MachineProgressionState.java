package com.rngtech.rpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record MachineProgressionState(
        long xp,
        int xpRemainder,
        int level,
        int spentPoints,
        long unlockedNodeMask,
        long unlockedNodeMaskHigh
) implements PassiveProgressionView {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 30;
    public static final int XP_REMAINDER_SCALE = 4;
    private static final long[] LEVEL_XP = {
            0L,
            100L,
            243L,
            448L,
            741L,
            1_160L,
            1_760L,
            2_619L,
            3_848L,
            5_600L,
            8_100L,
            11_650L,
            16_650L,
            23_650L,
            33_500L,
            47_300L,
            66_600L,
            93_600L,
            131_300L,
            184_000L,
            257_800L,
            361_100L,
            505_700L,
            708_100L,
            991_500L,
            1_388_300L,
            1_943_800L,
            2_721_500L,
            3_810_300L,
            5_334_600L
    };

    public static final MachineProgressionState EMPTY = new MachineProgressionState(0L, 0, MIN_LEVEL, 0, 0, 0);

    public static final Codec<MachineProgressionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(MachineProgressionState::xp),
            Codec.intRange(0, XP_REMAINDER_SCALE - 1)
                    .optionalFieldOf("xp_remainder", 0)
                    .forGetter(MachineProgressionState::xpRemainder),
            Codec.intRange(MIN_LEVEL, MAX_LEVEL)
                    .optionalFieldOf("level", MIN_LEVEL)
                    .forGetter(MachineProgressionState::level),
            Codec.intRange(0, Integer.MAX_VALUE)
                    .optionalFieldOf("spent_points", 0)
                    .forGetter(MachineProgressionState::spentPoints),
            Codec.LONG.optionalFieldOf("unlocked_node_mask", 0L).forGetter(MachineProgressionState::unlockedNodeMask),
            Codec.LONG.optionalFieldOf("unlocked_node_mask_high", 0L)
                    .forGetter(MachineProgressionState::unlockedNodeMaskHigh)
    ).apply(instance, MachineProgressionState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineProgressionState> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public MachineProgressionState decode(RegistryFriendlyByteBuf buffer) {
                    return new MachineProgressionState(
                            buffer.readVarLong(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarLong(),
                            buffer.readVarLong()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, MachineProgressionState state) {
                    buffer.writeVarLong(state.xp());
                    buffer.writeVarInt(state.xpRemainder());
                    buffer.writeVarInt(state.level());
                    buffer.writeVarInt(state.spentPoints());
                    buffer.writeVarLong(state.unlockedNodeMask());
                    buffer.writeVarLong(state.unlockedNodeMaskHigh());
                }
            };

    public MachineProgressionState {
        xp = Math.max(0L, xp);
        xpRemainder = Mth.clamp(xpRemainder, 0, XP_REMAINDER_SCALE - 1);
        level = Mth.clamp(Math.max(level, levelForXp(xp)), MIN_LEVEL, MAX_LEVEL);
        spentPoints = Mth.clamp(spentPoints, 0, totalPointsForLevel(level));
    }

    public MachineProgressionState(long xp, int xpRemainder, int level, int spentPoints, long unlockedNodeMask) {
        this(xp, xpRemainder, level, spentPoints, unlockedNodeMask, 0L);
    }

    public MachineProgressionState withAddedXp(long amount) {
        return withAddedScaledXp(amount, XP_REMAINDER_SCALE);
    }

    public MachineProgressionState withAddedScaledXp(long amount, int xpQuarters) {
        if (amount <= 0L || level >= MAX_LEVEL && xp >= xpForLevel(MAX_LEVEL)) {
            return this;
        }
        int clampedQuarters = Mth.clamp(xpQuarters, 0, XP_REMAINDER_SCALE);
        if (clampedQuarters <= 0) {
            return this;
        }
        long scaledAmount = Long.MAX_VALUE / clampedQuarters < amount
                ? Long.MAX_VALUE
                : amount * clampedQuarters;
        long scaledWithRemainder = Long.MAX_VALUE - xpRemainder < scaledAmount
                ? Long.MAX_VALUE
                : scaledAmount + xpRemainder;
        long wholeXp = scaledWithRemainder / XP_REMAINDER_SCALE;
        int nextRemainder = (int) (scaledWithRemainder % XP_REMAINDER_SCALE);
        long nextXp = Long.MAX_VALUE - xp < wholeXp ? Long.MAX_VALUE : xp + wholeXp;
        return new MachineProgressionState(
                nextXp,
                nextRemainder,
                levelForXp(nextXp),
                spentPoints,
                unlockedNodeMask,
                unlockedNodeMaskHigh
        );
    }

    public MachineProgressionState withUnlockedNode(int nodeIndex) {
        if (nodeIndex < 0 || hasNode(nodeIndex) || unspentPoints() <= 0) {
            return this;
        }
        long nodeMask = 1L << (nodeIndex % Long.SIZE);
        if (nodeIndex < Long.SIZE) {
            return new MachineProgressionState(
                    xp,
                    xpRemainder,
                    level,
                    spentPoints + 1,
                    unlockedNodeMask | nodeMask,
                    unlockedNodeMaskHigh
            );
        }
        return new MachineProgressionState(
                xp,
                xpRemainder,
                level,
                spentPoints + 1,
                unlockedNodeMask,
                unlockedNodeMaskHigh | nodeMask
        );
    }

    public boolean hasNode(int nodeIndex) {
        if (nodeIndex < 0) {
            return false;
        }
        long nodeMask = 1L << (nodeIndex % Long.SIZE);
        long bank = nodeIndex < Long.SIZE ? unlockedNodeMask : unlockedNodeMaskHigh;
        return (bank & nodeMask) == nodeMask;
    }

    public int totalPoints() {
        return totalPointsForLevel(level);
    }

    public int unspentPoints() {
        return Math.max(0, totalPoints() - spentPoints);
    }

    public long xpForCurrentLevel() {
        return xpForLevel(level);
    }

    public long xpForNextLevel() {
        return level >= MAX_LEVEL ? xpForLevel(MAX_LEVEL) : xpForLevel(level + 1);
    }

    public long xpInCurrentLevel() {
        return Math.max(0L, xp - xpForCurrentLevel());
    }

    public long xpNeededForNextLevel() {
        return level >= MAX_LEVEL ? 0L : Math.max(0L, xpForNextLevel() - xpForCurrentLevel());
    }

    public static int levelForXp(long xp) {
        int level = MIN_LEVEL;
        for (int index = 0; index < LEVEL_XP.length; index++) {
            if (xp >= LEVEL_XP[index]) {
                level = index + 1;
            }
        }
        return Mth.clamp(level, MIN_LEVEL, MAX_LEVEL);
    }

    public static long xpForLevel(int level) {
        int index = Mth.clamp(level, MIN_LEVEL, MAX_LEVEL) - 1;
        return LEVEL_XP[index];
    }

    private static int totalPointsForLevel(int level) {
        return Math.max(0, Mth.clamp(level, MIN_LEVEL, MAX_LEVEL) - MIN_LEVEL);
    }
}
