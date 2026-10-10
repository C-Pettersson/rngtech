package com.rngtech.rpg;

/**
 * Escapement: the first cycle after a machine idles, or after its recipe or mold changes, runs faster by Escapement
 * Speed. Machines spread a craft's FE over its ticks, so a faster cycle never makes a craft cheaper. A reload counts as
 * idling.
 */
public final class EscapementState {
    private Object recipe;
    private boolean primed = true;

    /** Notes the recipe the machine is running; a different recipe, such as after a mold swap, primes the next cycle. */
    public void observe(Object current) {
        if (current != recipe) {
            recipe = current;
            primed = true;
        }
    }

    public void idle() {
        recipe = null;
        primed = true;
    }

    public void completed() {
        primed = false;
    }

    public boolean primed() {
        return primed;
    }

    /** The cycle speed multiplier: Escapement Speed while primed and the machine has Escapement, otherwise 1. */
    public double speed(boolean escapement, MachineStatAccumulator stats) {
        return escapement && primed ? 1.0 + Math.max(0.0, stats.value(MachineStat.ESCAPEMENT_SPEED)) / 100.0 : 1.0;
    }
}
