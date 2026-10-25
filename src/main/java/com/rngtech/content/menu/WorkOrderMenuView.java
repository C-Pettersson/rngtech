package com.rngtech.content.menu;

import com.rngtech.rpg.workorder.WorkOrderHost;

/** A machine menu with an Orders tab. */
public interface WorkOrderMenuView {
    /** The machine; on the client only its synced data in {@link #workOrderView} is meaningful. */
    WorkOrderHost workOrderHost();

    /** The family id orders must name to load into this machine. */
    String workOrderFamily();

    /** The machine's synced chassis stage, for the load list's requirement check. */
    int workOrderStage();

    /** The machine's synced Mastery level, or 0 for a machine without Mastery. */
    int workOrderMasteryLevel();

    WorkOrderMenuSupport.View workOrderView();
}
