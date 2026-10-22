package com.rngtech.rpg.refinement;

/**
 * The timing of a pulse as the player watches it: Coils engage one by one from the Emitter, the slipped Coil flashes,
 * lit Coils unwind back toward the Emitter on a slip or hold a moment on a hold, and then the outcome applies. Pure
 * arithmetic on ticks since Fire, so the Cradle, the screen, and the checks all read the same frames.
 */
public final class PhasePulseSequence {
    /** Ticks between one Coil engaging and the next. */
    public static final int ENGAGE_TICKS = 8;
    /** Ticks the slipped Coil flashes before the lit ones unwind. */
    public static final int SLIP_TICKS = 12;
    /** Ticks between one lit Coil going dark and the next, back toward the Emitter. */
    public static final int UNWIND_TICKS = 5;
    /** Ticks a fully held line stays lit before the outcome applies. */
    public static final int HOLD_TICKS = 16;
    /** Ticks a Fracture shows before the outcome applies; no Coil engages. */
    public static final int FRACTURE_TICKS = 20;

    public enum Stage {
        ENGAGING,
        SLIPPED,
        UNWINDING,
        HOLDING,
        FRACTURED,
        DONE
    }

    /**
     * What the line shows {@code tick} ticks after Fire: {@code lit} Coils from the Emitter are lit, {@code flashing}
     * is the slipped position while it flashes (else 0), and {@code stage} names the moment.
     */
    public record Frame(Stage stage, int lit, int flashing) {
        public boolean done() {
            return stage == Stage.DONE;
        }
    }

    private PhasePulseSequence() {
    }

    /** Coils that engage before the sequence stops: every held Coil, plus the slipped one if any. */
    public static int engagedCoils(PhasePulse.Result result) {
        return result.slippedPosition() > 0 ? result.slippedPosition() : result.held();
    }

    /** The tick on which the outcome applies. */
    public static int totalTicks(PhasePulse.Result result) {
        if (result.outcome() == PhasePulse.Outcome.FRACTURE) {
            return FRACTURE_TICKS;
        }
        int engaged = engagedCoils(result);
        int engaging = engaged * ENGAGE_TICKS;
        if (result.slippedPosition() > 0) {
            return engaging + SLIP_TICKS + result.held() * UNWIND_TICKS;
        }
        return engaging + HOLD_TICKS;
    }

    public static Frame frame(PhasePulse.Result result, int tick) {
        if (tick >= totalTicks(result)) {
            return new Frame(Stage.DONE, 0, 0);
        }
        if (result.outcome() == PhasePulse.Outcome.FRACTURE) {
            return new Frame(Stage.FRACTURED, 0, 0);
        }
        int engaged = engagedCoils(result);
        int engaging = engaged * ENGAGE_TICKS;
        if (tick < engaging) {
            return new Frame(Stage.ENGAGING, Math.min(result.held(), tick / ENGAGE_TICKS + 1), 0);
        }
        if (result.slippedPosition() == 0) {
            return new Frame(Stage.HOLDING, result.held(), 0);
        }
        int sinceSlip = tick - engaging;
        if (sinceSlip < SLIP_TICKS) {
            return new Frame(Stage.SLIPPED, result.held(), result.slippedPosition());
        }
        int dark = (sinceSlip - SLIP_TICKS) / UNWIND_TICKS + 1;
        return new Frame(Stage.UNWINDING, Math.max(0, result.held() - dark), 0);
    }

    /** The Coil position that engages on {@code tick}, or 0 when none does. */
    public static int engagingOn(PhasePulse.Result result, int tick) {
        if (result.outcome() == PhasePulse.Outcome.FRACTURE || tick % ENGAGE_TICKS != 0) {
            return 0;
        }
        int position = tick / ENGAGE_TICKS + 1;
        return position <= engagedCoils(result) ? position : 0;
    }

    /** Pitch for a Coil's engage tone: it climbs along the line so the ear can count. */
    public static float engagePitch(int position) {
        return 0.7F + 0.12F * (position - 1);
    }
}
