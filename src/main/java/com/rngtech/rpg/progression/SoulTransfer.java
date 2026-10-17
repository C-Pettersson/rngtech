package com.rngtech.rpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Predicate;

/** Rules for moving one machine's whole Mastery to another machine of the same family through a Soul Transfer. */
public final class SoulTransfer {
    public enum Release { RELEASE, WRONG_FAMILY, ENTRY_STAGE, CONFIRM_OVERWRITE }

    /** The Mastery a Soul Transfer absorbed, with the family it may be released into. */
    public record Charge(MachineMasteryFamily family, MachineProgressionState state) {
        private static final Codec<MachineMasteryFamily> FAMILY_CODEC = Codec.STRING.xmap(
                name -> MachineMasteryFamily.valueOf(name.toUpperCase(Locale.ROOT)), family -> family.name().toLowerCase(Locale.ROOT));
        public static final Codec<Charge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FAMILY_CODEC.fieldOf("family").forGetter(Charge::family),
                MachineProgressionState.CODEC.fieldOf("progression").forGetter(Charge::state)
        ).apply(instance, Charge::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Charge> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.idMapper(index -> MachineMasteryFamily.values()[index], MachineMasteryFamily::ordinal), Charge::family,
                MachineProgressionState.STREAM_CODEC, Charge::state,
                Charge::new);
    }

    private SoulTransfer() { }

    /** A machine with no XP and no Seal tiers has nothing to absorb or overwrite. */
    public static boolean hasSoul(MachineProgressionState state) {
        return state.xp() > 0 || state.sealTiers() > 0;
    }

    /** The recorded calibration stability is the success chance; an uncalibrated Soul Transfer from loot or quests always succeeds. */
    public static int successChance(OptionalInt stability, boolean alwaysSucceed) {
        return alwaysSucceed || stability.isEmpty() ? 100 : Mth.clamp(stability.getAsInt(), 0, 100);
    }

    /**
     * Applies a failed absorb's configured loss: a share of the XP, then whole levels. Points the lower level no longer
     * covers are refunded latest-allocated first, skipping any node the tree or installed Gear still needs; when no node
     * can go, the loss stops at the level the build still needs. The old build becomes the follow target, so it fills
     * back in as the machine earns XP again.
     */
    public static MachineProgressionState afterFailure(MachineProgressionState state, int xpPercent, int levels,
            Predicate<MachineProgressionState> gearAllows) {
        xpPercent = Mth.clamp(xpPercent, 0, 100);
        levels = Mth.clamp(levels, 0, MachineProgressionState.MAX_LEVEL);
        long xp = state.xp() - (state.xp() / 100 * xpPercent + state.xp() % 100 * xpPercent / 100);
        int level = MachineProgressionState.levelForXp(xp);
        if (levels > 0) {
            long progress = xp - MachineProgressionState.xpForLevel(level);
            level = Math.max(MachineProgressionState.MIN_LEVEL, level - levels);
            long span = MachineProgressionState.xpForLevel(level + 1) - MachineProgressionState.xpForLevel(level);
            xp = Math.min(xp, MachineProgressionState.xpForLevel(level) + Math.min(progress, Math.max(0, span - 1)));
        }
        if (xp == state.xp()) { return state; }
        List<String> allocated = MegaPassiveTree.allocationOrder(state.startNodeId(), state.allocatedNodes());
        List<String> kept = new ArrayList<>(allocated);
        while (kept.size() > level - 1) {
            String removable = refundable(state, kept, gearAllows);
            if (removable == null) { break; }
            kept.remove(removable);
        }
        if (kept.size() > level - 1) {
            level = kept.size() + 1;
            xp = Math.max(xp, MachineProgressionState.xpForLevel(level));
        }
        boolean trimmed = kept.size() < allocated.size();
        List<String> target = !trimmed || state.targetNodes().containsAll(allocated) ? state.targetNodes() : allocated;
        return new MachineProgressionState(xp, 0, level, kept, state.startNodeId(), target, trimmed || state.following(),
                state.ascendancy(), state.ascendancyNodes(), state.sealTiers(), state.freeReset());
    }

    /** The latest-allocated node whose refund keeps the tree connected and the installed Gear legal. */
    private static String refundable(MachineProgressionState state, List<String> kept, Predicate<MachineProgressionState> gearAllows) {
        for (int index = kept.size() - 1; index >= 0; index--) {
            List<String> rest = new ArrayList<>(kept);
            rest.remove(index);
            if (!MegaPassiveTree.connected(state.startNodeId(), rest)) { continue; }
            MachineProgressionState candidate = new MachineProgressionState(state.xp(), 0, state.level(), rest, state.startNodeId(),
                    List.of(), false, state.ascendancy(), state.ascendancyNodes(), state.sealTiers());
            if (gearAllows.test(candidate)) { return kept.get(index); }
        }
        return null;
    }

    /**
     * Checks a release. The target must be the same family, an ascended soul needs a chassis that could have taken the
     * first Seal, and replacing a target's own Mastery needs the player to confirm.
     */
    public static Release release(Charge charge, MachineMasteryFamily family, int entryStage, MachineProgressionState target, boolean confirmed) {
        if (charge.family() != family) { return Release.WRONG_FAMILY; }
        if (charge.state().sealTiers() > 0 && entryStage < AscendancyCatalog.ENTRY_STAGE) { return Release.ENTRY_STAGE; }
        if (hasSoul(target) && !confirmed) { return Release.CONFIRM_OVERWRITE; }
        return Release.RELEASE;
    }
}
