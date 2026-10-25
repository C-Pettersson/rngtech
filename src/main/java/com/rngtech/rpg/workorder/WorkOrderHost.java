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

    /** The world's game time, which Rush deadlines count in; 0 with no world. */
    default long workOrderGameTime() { return 0; }

    /** Drops Rush chips whose deadline has passed. Machines call it about once a second. */
    default void workOrderTick() {
        MachineWorkOrders orders = workOrders();
        if (orders.isEmpty()) { return; }
        MachineWorkOrders next = orders.withoutExpired(workOrderGameTime());
        if (next != orders) { setWorkOrders(next); }
    }

    /** Resets the active chip's progress to 0 when it carries the reset condition {@code condition}, such as Jammed Line on a jam. */
    default void workOrderSetback(String condition) {
        MachineWorkOrders orders = workOrders();
        LoadedWorkOrder active = orders.activeOrder();
        if (active != null && WorkOrderConditions.has(active.traits(), condition)) {
            setWorkOrders(orders.withReplaced(orders.active(), active.withoutProgress()));
        }
    }

    /** The ascendancy a Completed Job Chip records for its reward rolls, or "" for none. */
    default String workOrderAscendancy() { return ""; }

    /** Counts finished work toward the active chip. A chip that reaches its target stays in its bay, finished and inactive. */
    default void workOrderWork(WorkReport report) {
        MachineWorkOrders orders = workOrders();
        WorkOrderRules.Progress result = WorkOrderRules.progress(orders.withoutExpired(workOrderGameTime()), report);
        if (result.orders() != orders) { setWorkOrders(result.orders()); }
    }
}
