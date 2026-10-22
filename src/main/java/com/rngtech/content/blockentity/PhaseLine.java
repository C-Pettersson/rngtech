package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.PhaseCoilBlock;
import com.rngtech.content.block.PhaseCradleBlock;
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
     * {@code usableCoils} counts the leading Coils, from the Emitter inward, whose position the Cradle's seated Glyphs
     * open; it is 0 unless the line is complete.
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
    /** The line's shape only; the client uses this for the parts' positions. */
    public static Scan scan(Level level, BlockPos cradle) {
        return scan(level, cradle, 0);
    }

    /** {@code openPositions} counts the positions from the Emitter the Cradle's seated Glyphs open; usable Coils never exceed it. */
    public static Scan scan(Level level, BlockPos cradle, int openPositions) {
        Scan best = Scan.none();
        for (Direction axis : Direction.Plane.HORIZONTAL) {
            Scan scan = scanAxis(level, cradle, axis, openPositions);
            if (scan.complete()) {
                return scan;
            }
            if (scan.coilCount() > best.coilCount()) {
                best = scan;
            }
        }
        return best;
    }

    private static Scan scanAxis(Level level, BlockPos cradle, Direction axis, int openPositions) {
        List<BlockPos> coils = new ArrayList<>();
        BlockPos cursor = cradle.relative(axis);
        while (coils.size() < PhaseArrayRules.MAX_COILS && level.getBlockState(cursor).getBlock() instanceof PhaseCoilBlock) {
            coils.add(cursor.immutable());
            cursor = cursor.relative(axis);
        }
        BlockPos emitter = level.getBlockState(cursor).getBlock() instanceof PhaseEmitterBlock ? cursor.immutable() : null;
        boolean complete = emitter != null && coils.size() >= PhaseArrayRules.MIN_COILS;
        return new Scan(axis, List.copyOf(coils), emitter, complete, complete ? Math.min(coils.size(), openPositions) : 0);
    }

    /**
     * The direction of a line part next to {@code pos}, so a new part placed beside a line turns along it instead of
     * toward the player; null when none is adjacent.
     */
    public static Direction axisNextTo(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Block neighbour = level.getBlockState(pos.relative(direction)).getBlock();
            if (neighbour instanceof PhaseCoilBlock || neighbour instanceof PhaseEmitterBlock || neighbour instanceof PhaseCradleBlock) {
                return direction;
            }
        }
        return null;
    }

    /** Asks every Cradle whose line could include {@code pos} to rescan: the line is straight, so only the four rows matter. */
    public static void requestNearbyCradleRescans(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= CRADLE_NOTIFY_DISTANCE; distance++) {
                if (level.getBlockEntity(pos.relative(direction, distance)) instanceof PhaseCradleBlockEntity cradle) {
                    cradle.requestLineRescan();
                }
            }
        }
    }

    /** Sets the formed look on a Coil or Emitter; other blocks are left alone. */
    public static void setFormed(Level level, BlockPos pos, boolean formed) {
        setFormed(level, pos, formed, null);
    }

    /**
     * Sets the formed look and, with an {@code axis} (the direction from the Cradle outward), turns the member along
     * the line: a Coil's beam runs along it and the Emitter fires back toward the Cradle.
     */
    public static void setFormed(Level level, BlockPos pos, boolean formed, Direction axis) {
        BlockState state = level.getBlockState(pos);
        BlockState next = state;
        if (state.getBlock() instanceof PhaseCoilBlock) {
            next = state.setValue(PhaseCoilBlock.FORMED, formed);
            if (!formed) {
                next = next.setValue(PhaseCoilBlock.TIER, 0);
            }
            if (axis != null) {
                next = next.setValue(BaseMachineBlock.FACING, axis);
            }
        } else if (state.getBlock() instanceof PhaseEmitterBlock) {
            next = state.setValue(PhaseEmitterBlock.FORMED, formed);
            if (axis != null) {
                next = next.setValue(BaseMachineBlock.FACING, axis.getOpposite());
            }
        }
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_ALL);
        }
    }
}
