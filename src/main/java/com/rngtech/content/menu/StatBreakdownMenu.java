package com.rngtech.content.menu;

import com.rngtech.rpg.MachineStatAccumulator;

/** A menu whose stat panel can explain its numbers: the server recomputes these stats with breakdown recording. */
public interface StatBreakdownMenu {
    MachineStatAccumulator breakdownStats();
}
