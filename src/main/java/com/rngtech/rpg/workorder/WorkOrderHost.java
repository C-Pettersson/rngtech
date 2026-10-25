package com.rngtech.rpg.workorder;

/** A machine that loads Work Orders and counts its finished work toward the active one. */
public interface WorkOrderHost {
    /** The family id orders must name to load here. */
    String workOrderFamily();

    /** The chassis stage an order's stage requirement checks. */
    int workOrderStage();

    /** The Mastery level an order's level requirement checks, or 0 for a machine without Mastery. */
    int workOrderMasteryLevel();

    MachineWorkOrders workOrders();

    void setWorkOrders(MachineWorkOrders orders);

    /** The ascendancy a Completed Job Chip records for its reward rolls, or "" for none. */
    default String workOrderAscendancy() { return ""; }

    /** Counts finished work toward the active chip. A chip that reaches its target stays in its bay, finished and inactive. */
    default void workOrderWork(WorkReport report) {
        MachineWorkOrders orders = workOrders();
        WorkOrderRules.Progress result = WorkOrderRules.progress(orders, report);
        if (result.orders() != orders) { setWorkOrders(result.orders()); }
    }
}
