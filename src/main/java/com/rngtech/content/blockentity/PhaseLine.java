package com.rngtech.content.blockentity;

import com.rngtech.content.block.PhaseCoilBlock;
import com.rngtech.content.block.PhaseEmitterBlock;
import com.rngtech.rpg.PhaseArrayRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Phase Array's straight line: the Cradle, then {@link PhaseArrayRules#MIN_COILS} to {@link PhaseArrayRules#MAX_COILS}
 * Phase Coils, then a Phase Emitter, on one horizontal axis. The Cradle scans for it the way the Wooden Dehumidifier
 * scans for its cube: on a timer and whenever a member block changes.
 */
public final class PhaseLine {
    /** How far from a member a Cradle can be: the longest line, counted from the far end. */
    private static final int CRADLE_NOTIFY_DISTANCE = PhaseArrayRules.MAX_COILS + 1;

    private PhaseLine() {
    }

    /**
     * One scan of a line from the Cradle outward along {@code axis}. {@code coils} are the Coils found in order from the
     * Cradle, {@code emitter} the Emitter after them or {@code null}, and {@code complete} whether that makes a legal line.
     * {@code usableCoils} counts the leading Coils, from the Emitter inward, whose inscribed Glyph tier meets their
     * position's floor; it is 0 unless the line is complete.
     */
    public record Scan(Direction axis, List<BlockPos> coils, BlockPos emitter, boolean complete, int usableCoils) {
        public static Scan none() {
            return new Scan(Direction.NORTH, List.of(), null, false, 0);
        }

        public int coilCount() {
            return coils.size();
        }

        /** Every block the line owns while formed: the Coils and the Emitter. */
        public Set<BlockPos> members() {
            Set<BlockPos> members = new LinkedHashSet<>(coils);
            if (emitter != null) {
                members.add(emitter);
            }
            return members;
        }

        /** Coils in line order, position 1 next to the Emitter. */
        public List<BlockPos> coilsFromEmitter() {
            List<BlockPos> ordered = new ArrayList<>(coils);
            java.util.Collections.reverse(ordered);
            return ordered;
        }

        /** What a complete line still needs, for the screen and Jade. */
        public String missing() {
            if (complete) {
                return "none";
            }
            if (coils.size() < PhaseArrayRules.MIN_COILS) {
                return "coils";
            }
            return "emitter";
        }
    }

    /** The complete line on any horizontal side of the Cradle, or else the side with the most Coils. */
    public static Scan scan(Level level, BlockPos cradle) {
        Scan best = Scan.none();
        for (Direction axis : Direction.Plane.HORIZONTAL) {
            Scan scan = scanAxis(level, cradle, axis);
            if (scan.complete()) {
                return scan;
            }
            if (scan.coilCount() > best.coilCount()) {
                best = scan;
            }
        }
        return best;
    }

    private static Scan scanAxis(Level level, BlockPos cradle, Direction axis) {
        List<BlockPos> coils = new ArrayList<>();
        BlockPos cursor = cradle.relative(axis);
        while (coils.size() < PhaseArrayRules.MAX_COILS && level.getBlockState(cursor).getBlock() instanceof PhaseCoilBlock) {
            coils.add(cursor.immutable());
            cursor = cursor.relative(axis);
        }
        BlockPos emitter = level.getBlockState(cursor).getBlock() instanceof PhaseEmitterBlock ? cursor.immutable() : null;
        boolean complete = emitter != null && coils.size() >= PhaseArrayRules.MIN_COILS;
        Scan scan = new Scan(axis, List.copyOf(coils), emitter, complete, 0);
        return complete ? new Scan(axis, scan.coils(), emitter, true, usableCoils(level, scan)) : scan;
    }

    private static int usableCoils(Level level, Scan scan) {
        int usable = 0;
        for (BlockPos pos : scan.coilsFromEmitter()) {
            int tier = level.getBlockEntity(pos) instanceof PhaseCoilBlockEntity coil ? coil.coilTier() : 0;
            if (tier < PhaseArrayRules.lowestGlyphTier(usable + 1)) {
                break;
            }
            usable++;
        }
        return usable;
    }

    /** Asks every Cradle whose line could include {@code pos} to rescan: the line is straight, so only the four rows matter. */
    public static void requestNearbyCradleRescans(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= CRADLE_NOTIFY_DISTANCE; distance++) {
                if (level.getBlockEntity(pos.relative(direction, distance)) instanceof ExoticAffixForgeBlockEntity cradle) {
                    cradle.requestLineRescan();
                }
            }
        }
    }

    /** Sets the formed look on a Coil or Emitter; other blocks are left alone. */
    public static void setFormed(Level level, BlockPos pos, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PhaseCoilBlock && state.getValue(PhaseCoilBlock.FORMED) != formed) {
            level.setBlock(pos, state.setValue(PhaseCoilBlock.FORMED, formed), Block.UPDATE_ALL);
        } else if (state.getBlock() instanceof PhaseEmitterBlock && state.getValue(PhaseEmitterBlock.FORMED) != formed) {
            level.setBlock(pos, state.setValue(PhaseEmitterBlock.FORMED, formed), Block.UPDATE_ALL);
        }
    }
}
