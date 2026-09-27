package com.rngtech.rpg.progression;

import java.util.List;

final class ForestryCompanionPassiveTreeDefinition {
    private static final int EXPECTED_TOTAL_NODES = 100;
    private static final int EXPECTED_TRAVEL_NODES = 48;
    private static final int EXPECTED_NOTABLES = 14;
    private static final int EXPECTED_KEYSTONES = 5;

    static PassiveTreeDefinition<ForestryCompanionPassiveNode> create() {
        PassiveTreeDefinition<ForestryCompanionPassiveNode> definition = PassiveTreeDefinition.of(
                ForestryCompanionPassiveNode.class,
                MachinePassiveClasses.ALL,
                PassiveTreeConstants.poeStyle(),
                ForestryCompanionPassiveTreeLayout.groups(),
                nodeSpecs()
        );
        definition.validate(
                ForestryCompanionPassiveNode.STARTER,
                EXPECTED_TOTAL_NODES,
                EXPECTED_TRAVEL_NODES,
                EXPECTED_NOTABLES,
                EXPECTED_KEYSTONES
        );
        return definition;
    }

    private static List<PassiveTreeNodeSpec<ForestryCompanionPassiveNode>> nodeSpecs() {
        return List.of(
                spec(
                        ForestryCompanionPassiveNode.STARTER,
                        ForestryCompanionPassiveNode.BRAKE_TRIM,
                        ForestryCompanionPassiveNode.PACING_WHEEL,
                        ForestryCompanionPassiveNode.SIGNAL_CHECK
                ),
                spec(
                        ForestryCompanionPassiveNode.BRAKE_TRIM,
                        ForestryCompanionPassiveNode.PACING_WHEEL,
                        ForestryCompanionPassiveNode.GEAR_ADVANCE
                ),
                spec(
                        ForestryCompanionPassiveNode.PACING_WHEEL,
                        ForestryCompanionPassiveNode.CONSERVATION_LOOP,
                        ForestryCompanionPassiveNode.RAILSIDE_CELL_INDEX
                ),
                spec(ForestryCompanionPassiveNode.CONSERVATION_LOOP, ForestryCompanionPassiveNode.SIGNAL_CHECK),
                spec(
                        ForestryCompanionPassiveNode.SIGNAL_CHECK,
                        ForestryCompanionPassiveNode.BRAKE_TRIM,
                        ForestryCompanionPassiveNode.RETURN_RAIL_SEALS
                ),
                spec(
                        ForestryCompanionPassiveNode.ROUTE_SURVEY,
                        ForestryCompanionPassiveNode.LOW_LOSS_WHEELS,
                        ForestryCompanionPassiveNode.SWITCHBACK_CACHE,
                        ForestryCompanionPassiveNode.SIGNAL_REPEATER
                ),
                spec(ForestryCompanionPassiveNode.LOW_LOSS_WHEELS, ForestryCompanionPassiveNode.ENERGY_DISPATCH_TABLE),
                spec(
                        ForestryCompanionPassiveNode.ENERGY_DISPATCH_TABLE,
                        ForestryCompanionPassiveNode.RETURN_RAIL_SEALS
                ),
                spec(
                        ForestryCompanionPassiveNode.RETURN_RAIL_SEALS,
                        ForestryCompanionPassiveNode.EFFICIENT_ROUTING,
                        ForestryCompanionPassiveNode.SAPLING_QUEUE
                ),
                spec(ForestryCompanionPassiveNode.EFFICIENT_ROUTING, ForestryCompanionPassiveNode.ROUTE_SURVEY),
                spec(
                        ForestryCompanionPassiveNode.ROUTE_BEACON,
                        ForestryCompanionPassiveNode.SIGNAL_REPEATER,
                        ForestryCompanionPassiveNode.PATH_CLEARANCE_EYE
                ),
                spec(ForestryCompanionPassiveNode.SIGNAL_REPEATER, ForestryCompanionPassiveNode.QUIET_AXLES),
                spec(ForestryCompanionPassiveNode.QUIET_AXLES, ForestryCompanionPassiveNode.RETURN_CARTOGRAPHY),
                spec(ForestryCompanionPassiveNode.RETURN_CARTOGRAPHY),
                spec(
                        ForestryCompanionPassiveNode.BRAKE_RECOVERY_LOOP,
                        ForestryCompanionPassiveNode.ROUTE_BALANCER,
                        ForestryCompanionPassiveNode.SAW_TOOTH_INDEX
                ),
                spec(
                        ForestryCompanionPassiveNode.ROUTE_BALANCER,
                        ForestryCompanionPassiveNode.SWITCHBACK_CACHE,
                        ForestryCompanionPassiveNode.LIGHT_AXLE
                ),
                spec(ForestryCompanionPassiveNode.SWITCHBACK_CACHE, ForestryCompanionPassiveNode.MAINTENANCE_SPUR),
                spec(ForestryCompanionPassiveNode.MAINTENANCE_SPUR, ForestryCompanionPassiveNode.BRAKE_RECOVERY_LOOP),
                spec(
                        ForestryCompanionPassiveNode.RAIL_POLISH,
                        ForestryCompanionPassiveNode.LOW_DRAG_AXLES,
                        ForestryCompanionPassiveNode.COASTING_CLUTCH
                ),
                spec(ForestryCompanionPassiveNode.LOW_DRAG_AXLES, ForestryCompanionPassiveNode.COASTING_FLYWHEEL),
                spec(ForestryCompanionPassiveNode.COASTING_FLYWHEEL, ForestryCompanionPassiveNode.TIMETABLE_CACHE),
                spec(
                        ForestryCompanionPassiveNode.TIMETABLE_CACHE,
                        ForestryCompanionPassiveNode.PATH_CLEARANCE_EYE,
                        ForestryCompanionPassiveNode.MAINTENANCE_SPUR
                ),
                spec(
                        ForestryCompanionPassiveNode.PATH_CLEARANCE_EYE,
                        ForestryCompanionPassiveNode.SERVICE_SPUR_MARKERS
                ),
                spec(ForestryCompanionPassiveNode.SERVICE_SPUR_MARKERS),
                spec(ForestryCompanionPassiveNode.COASTING_CLUTCH),
                spec(
                        ForestryCompanionPassiveNode.LIGHT_AXLE,
                        ForestryCompanionPassiveNode.QUICK_SCAN,
                        ForestryCompanionPassiveNode.CUTTING_CADENCE
                ),
                spec(
                        ForestryCompanionPassiveNode.QUICK_SCAN,
                        ForestryCompanionPassiveNode.ROLLER_BEARINGS,
                        ForestryCompanionPassiveNode.SNAPSHOT_PREFETCH
                ),
                spec(ForestryCompanionPassiveNode.ROLLER_BEARINGS, ForestryCompanionPassiveNode.GEAR_ADVANCE),
                spec(ForestryCompanionPassiveNode.GEAR_ADVANCE, ForestryCompanionPassiveNode.LIGHT_AXLE),
                spec(ForestryCompanionPassiveNode.FAST_PLANTER, ForestryCompanionPassiveNode.SAW_RHYTHM),
                spec(ForestryCompanionPassiveNode.SAW_RHYTHM, ForestryCompanionPassiveNode.SCAN_PREDICTOR),
                spec(
                        ForestryCompanionPassiveNode.SCAN_PREDICTOR,
                        ForestryCompanionPassiveNode.CUTTING_CADENCE,
                        ForestryCompanionPassiveNode.PATHING_RELAY
                ),
                spec(ForestryCompanionPassiveNode.CUTTING_CADENCE, ForestryCompanionPassiveNode.SAW_TOOTH_INDEX),
                spec(ForestryCompanionPassiveNode.SAW_TOOTH_INDEX, ForestryCompanionPassiveNode.ADVANCE_TIMER),
                spec(ForestryCompanionPassiveNode.ADVANCE_TIMER, ForestryCompanionPassiveNode.FAST_PLANTER),
                spec(ForestryCompanionPassiveNode.PLANTING_CLOCK, ForestryCompanionPassiveNode.PLANTING_SERVO),
                spec(
                        ForestryCompanionPassiveNode.PLANTING_SERVO,
                        ForestryCompanionPassiveNode.LOGGER_FEED_ROLLERS,
                        ForestryCompanionPassiveNode.FORESTRY_OVERDRIVE
                ),
                spec(ForestryCompanionPassiveNode.LOGGER_FEED_ROLLERS, ForestryCompanionPassiveNode.FIELD_CACHE),
                spec(
                        ForestryCompanionPassiveNode.FIELD_CACHE,
                        ForestryCompanionPassiveNode.PATHING_RELAY,
                        ForestryCompanionPassiveNode.CUT_ORDER_ROUTINE
                ),
                spec(ForestryCompanionPassiveNode.PATHING_RELAY),
                spec(ForestryCompanionPassiveNode.FORESTRY_OVERDRIVE),
                spec(ForestryCompanionPassiveNode.CUT_ORDER_ROUTINE, ForestryCompanionPassiveNode.SNAPSHOT_LEDGER),
                spec(
                        ForestryCompanionPassiveNode.SNAPSHOT_LEDGER,
                        ForestryCompanionPassiveNode.SAW_TENSIONER,
                        ForestryCompanionPassiveNode.MANUAL_THROTTLE
                ),
                spec(ForestryCompanionPassiveNode.SAW_TENSIONER, ForestryCompanionPassiveNode.RETURN_MOMENTUM),
                spec(
                        ForestryCompanionPassiveNode.RETURN_MOMENTUM,
                        ForestryCompanionPassiveNode.HOT_SWAP_ROUTINE,
                        ForestryCompanionPassiveNode.CANOPY_PROFILE
                ),
                spec(ForestryCompanionPassiveNode.HOT_SWAP_ROUTINE, ForestryCompanionPassiveNode.SNAPSHOT_PREFETCH),
                spec(ForestryCompanionPassiveNode.SNAPSHOT_PREFETCH),
                spec(ForestryCompanionPassiveNode.MANUAL_THROTTLE),
                spec(ForestryCompanionPassiveNode.ROOT_MAPPING, ForestryCompanionPassiveNode.CANOPY_PROFILE),
                spec(
                        ForestryCompanionPassiveNode.CANOPY_PROFILE,
                        ForestryCompanionPassiveNode.HEAVY_SAW_FRAME,
                        ForestryCompanionPassiveNode.BROAD_TREE_PROTOCOL
                ),
                spec(ForestryCompanionPassiveNode.HEAVY_SAW_FRAME, ForestryCompanionPassiveNode.CANOPY_ANCHORS),
                spec(
                        ForestryCompanionPassiveNode.CANOPY_ANCHORS,
                        ForestryCompanionPassiveNode.ROOT_BOUNDARY_FLAGS,
                        ForestryCompanionPassiveNode.SHEAR_BYPASS_TRACE
                ),
                spec(ForestryCompanionPassiveNode.ROOT_BOUNDARY_FLAGS, ForestryCompanionPassiveNode.ROOT_SAMPLE_GRID),
                spec(
                        ForestryCompanionPassiveNode.ROOT_SAMPLE_GRID,
                        ForestryCompanionPassiveNode.LOG_CARTOGRAPHER,
                        ForestryCompanionPassiveNode.PLANTING_CELL_LEDGER
                ),
                spec(ForestryCompanionPassiveNode.LOG_CARTOGRAPHER, ForestryCompanionPassiveNode.ROOT_MAPPING),
                spec(ForestryCompanionPassiveNode.BROAD_TREE_PROTOCOL),
                spec(ForestryCompanionPassiveNode.TRUNK_PROFILE_CACHE, ForestryCompanionPassiveNode.TRUNK_BRAKE),
                spec(ForestryCompanionPassiveNode.TRUNK_BRAKE, ForestryCompanionPassiveNode.LIMB_CLEARANCE_GAUGE),
                spec(
                        ForestryCompanionPassiveNode.LIMB_CLEARANCE_GAUGE,
                        ForestryCompanionPassiveNode.SHEAR_BYPASS_TRACE,
                        ForestryCompanionPassiveNode.LEAF_CHANNEL
                ),
                spec(ForestryCompanionPassiveNode.SHEAR_BYPASS_TRACE, ForestryCompanionPassiveNode.TRUNK_PROFILE_CACHE),
                spec(
                        ForestryCompanionPassiveNode.LEAF_CHANNEL,
                        ForestryCompanionPassiveNode.WIDE_CANOPY_GUARDS,
                        ForestryCompanionPassiveNode.CANOPY_SAFETY_LATCH
                ),
                spec(
                        ForestryCompanionPassiveNode.WIDE_CANOPY_GUARDS,
                        ForestryCompanionPassiveNode.SERRATED_LEAF_PROTOCOL
                ),
                spec(ForestryCompanionPassiveNode.SERRATED_LEAF_PROTOCOL),
                spec(
                        ForestryCompanionPassiveNode.CANOPY_WORKPLAN,
                        ForestryCompanionPassiveNode.CANOPY_SAFETY_LATCH,
                        ForestryCompanionPassiveNode.STORMFALL_PROTOCOL
                ),
                spec(ForestryCompanionPassiveNode.CANOPY_SAFETY_LATCH, ForestryCompanionPassiveNode.STABILITY_COUPLER),
                spec(ForestryCompanionPassiveNode.STABILITY_COUPLER, ForestryCompanionPassiveNode.CANOPY_WATCH),
                spec(ForestryCompanionPassiveNode.CANOPY_WATCH, ForestryCompanionPassiveNode.MANAGED_CELL_GRID),
                spec(ForestryCompanionPassiveNode.STORMFALL_PROTOCOL),
                spec(
                        ForestryCompanionPassiveNode.SAPLING_QUEUE,
                        ForestryCompanionPassiveNode.ROUTE_MEMORY,
                        ForestryCompanionPassiveNode.CARGO_SIDING
                ),
                spec(
                        ForestryCompanionPassiveNode.ROUTE_MEMORY,
                        ForestryCompanionPassiveNode.FOREST_LOOP,
                        ForestryCompanionPassiveNode.AUX_CELL_CONTACTS
                ),
                spec(ForestryCompanionPassiveNode.FOREST_LOOP, ForestryCompanionPassiveNode.CARGO_RAKE),
                spec(ForestryCompanionPassiveNode.CARGO_RAKE, ForestryCompanionPassiveNode.SAPLING_QUEUE),
                spec(
                        ForestryCompanionPassiveNode.CARGO_SIDING,
                        ForestryCompanionPassiveNode.DEPOT_SORTER,
                        ForestryCompanionPassiveNode.SERVO_GOVERNOR
                ),
                spec(ForestryCompanionPassiveNode.DEPOT_SORTER, ForestryCompanionPassiveNode.BIN_PARTITIONS),
                spec(ForestryCompanionPassiveNode.BIN_PARTITIONS, ForestryCompanionPassiveNode.DROPS_ROUTING_TABLE),
                spec(ForestryCompanionPassiveNode.DROPS_ROUTING_TABLE, ForestryCompanionPassiveNode.DROP_FILTER_GRID),
                spec(ForestryCompanionPassiveNode.DROP_FILTER_GRID, ForestryCompanionPassiveNode.CARGO_BRACE),
                spec(ForestryCompanionPassiveNode.CARGO_BRACE, ForestryCompanionPassiveNode.SAPLING_RESEED_PATH),
                spec(
                        ForestryCompanionPassiveNode.SAPLING_RESEED_PATH,
                        ForestryCompanionPassiveNode.SAPLING_LEDGER,
                        ForestryCompanionPassiveNode.SAPLING_BUFFER
                ),
                spec(ForestryCompanionPassiveNode.SAPLING_LEDGER, ForestryCompanionPassiveNode.RETURN_DEPOT_BUFFER),
                spec(
                        ForestryCompanionPassiveNode.RETURN_DEPOT_BUFFER,
                        ForestryCompanionPassiveNode.DEPOT_RECEIVER,
                        ForestryCompanionPassiveNode.MAGNET_MODE
                ),
                spec(ForestryCompanionPassiveNode.DEPOT_RECEIVER),
                spec(ForestryCompanionPassiveNode.MAGNET_MODE),
                spec(ForestryCompanionPassiveNode.SERVO_GOVERNOR, ForestryCompanionPassiveNode.SNAP_FEED),
                spec(ForestryCompanionPassiveNode.SNAP_FEED, ForestryCompanionPassiveNode.RETURN_CARTOGRAPHY),
                spec(
                        ForestryCompanionPassiveNode.CARGO_LOOPBACK,
                        ForestryCompanionPassiveNode.BACKUP_BUS,
                        ForestryCompanionPassiveNode.CARGO_RAKE
                ),
                spec(
                        ForestryCompanionPassiveNode.BACKUP_BUS,
                        ForestryCompanionPassiveNode.SAPLING_BUFFER,
                        ForestryCompanionPassiveNode.SPARE_CANISTER
                ),
                spec(
                        ForestryCompanionPassiveNode.SAPLING_BUFFER,
                        ForestryCompanionPassiveNode.CARGO_LOOPBACK,
                        ForestryCompanionPassiveNode.SEEDLING_MAGNET
                ),
                spec(ForestryCompanionPassiveNode.SEEDLING_MAGNET),
                spec(
                        ForestryCompanionPassiveNode.RAILSIDE_CELL_INDEX,
                        ForestryCompanionPassiveNode.PLANTING_CELL_LEDGER
                ),
                spec(
                        ForestryCompanionPassiveNode.PLANTING_CELL_LEDGER,
                        ForestryCompanionPassiveNode.CELL_TENDER_MATRIX
                ),
                spec(ForestryCompanionPassiveNode.CELL_TENDER_MATRIX, ForestryCompanionPassiveNode.MANAGED_CELL_GRID),
                spec(ForestryCompanionPassiveNode.MANAGED_CELL_GRID, ForestryCompanionPassiveNode.GROVE_REGISTRY),
                spec(ForestryCompanionPassiveNode.GROVE_REGISTRY, ForestryCompanionPassiveNode.ROUTE_LOCK),
                spec(ForestryCompanionPassiveNode.ROUTE_LOCK, ForestryCompanionPassiveNode.AUX_CELL_CONTACTS),
                spec(ForestryCompanionPassiveNode.AUX_CELL_CONTACTS),
                spec(
                        ForestryCompanionPassiveNode.RESERVE_RAIL_BUS,
                        ForestryCompanionPassiveNode.CHARGE_WINDOW,
                        ForestryCompanionPassiveNode.ROUTE_LOCK
                ),
                spec(
                        ForestryCompanionPassiveNode.CHARGE_WINDOW,
                        ForestryCompanionPassiveNode.CELL_RETURN_TRACE,
                        ForestryCompanionPassiveNode.CANOPY_WATCH
                ),
                spec(ForestryCompanionPassiveNode.CELL_RETURN_TRACE, ForestryCompanionPassiveNode.SPARE_CANISTER),
                spec(ForestryCompanionPassiveNode.SPARE_CANISTER, ForestryCompanionPassiveNode.RESERVE_RAIL_BUS)
        );
    }

    private static PassiveTreeNodeSpec<ForestryCompanionPassiveNode> spec(
            ForestryCompanionPassiveNode node,
            ForestryCompanionPassiveNode... linkedNodes
    ) {
        ForestryCompanionPassiveTreeLayout.Placement placement =
                ForestryCompanionPassiveTreeLayout.placement(node.name());
        return new PassiveTreeNodeSpec<>(
                node,
                placement.groupId(),
                placement.orbit(),
                placement.orbitIndex(),
                node.kind(),
                List.of(linkedNodes)
        );
    }

    private ForestryCompanionPassiveTreeDefinition() {
    }
}
