package com.rngtech.rpg.progression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ForestryCompanionPassiveTreeLayout {
    private static final List<GroupSeed> GROUP_SEEDS = groupSeeds();
    private static final Map<String, Placement> PLACEMENTS = placements();
    private static final List<PassiveTreeGroup> GROUPS = createGroups();

    static final PassiveTreeLayouts.Point STARTER = point("STARTER");
    static final PassiveTreeLayouts.Point ROUTE_SURVEY = point("ROUTE_SURVEY");
    static final PassiveTreeLayouts.Point LOW_LOSS_WHEELS = point("LOW_LOSS_WHEELS");
    static final PassiveTreeLayouts.Point EFFICIENT_ROUTING = point("EFFICIENT_ROUTING");
    static final PassiveTreeLayouts.Point CONSERVATION_LOOP = point("CONSERVATION_LOOP");
    static final PassiveTreeLayouts.Point QUIET_AXLES = point("QUIET_AXLES");

    static final PassiveTreeLayouts.Point LIGHT_AXLE = point("LIGHT_AXLE");
    static final PassiveTreeLayouts.Point QUICK_SCAN = point("QUICK_SCAN");
    static final PassiveTreeLayouts.Point FAST_PLANTER = point("FAST_PLANTER");
    static final PassiveTreeLayouts.Point SAW_RHYTHM = point("SAW_RHYTHM");
    static final PassiveTreeLayouts.Point FORESTRY_OVERDRIVE = point("FORESTRY_OVERDRIVE");

    static final PassiveTreeLayouts.Point ROOT_MAPPING = point("ROOT_MAPPING");
    static final PassiveTreeLayouts.Point CANOPY_PROFILE = point("CANOPY_PROFILE");
    static final PassiveTreeLayouts.Point HEAVY_SAW_FRAME = point("HEAVY_SAW_FRAME");
    static final PassiveTreeLayouts.Point BROAD_TREE_PROTOCOL = point("BROAD_TREE_PROTOCOL");

    static final PassiveTreeLayouts.Point SAPLING_QUEUE = point("SAPLING_QUEUE");
    static final PassiveTreeLayouts.Point CARGO_RAKE = point("CARGO_RAKE");
    static final PassiveTreeLayouts.Point ROUTE_MEMORY = point("ROUTE_MEMORY");
    static final PassiveTreeLayouts.Point FOREST_LOOP = point("FOREST_LOOP");
    static final PassiveTreeLayouts.Point MAGNET_MODE = point("MAGNET_MODE");
    static final PassiveTreeLayouts.Point SERRATED_LEAF_PROTOCOL = point("SERRATED_LEAF_PROTOCOL");
    static final PassiveTreeLayouts.Point MANUAL_THROTTLE = point("MANUAL_THROTTLE");
    static final PassiveTreeLayouts.Point COASTING_CLUTCH = point("COASTING_CLUTCH");
    static final PassiveTreeLayouts.Point SEEDLING_MAGNET = point("SEEDLING_MAGNET");
    static final PassiveTreeLayouts.Point RETURN_RAIL_SEALS = point("RETURN_RAIL_SEALS");
    static final PassiveTreeLayouts.Point ROUTE_BALANCER = point("ROUTE_BALANCER");
    static final PassiveTreeLayouts.Point ADVANCE_TIMER = point("ADVANCE_TIMER");
    static final PassiveTreeLayouts.Point SAW_TENSIONER = point("SAW_TENSIONER");
    static final PassiveTreeLayouts.Point CANOPY_ANCHORS = point("CANOPY_ANCHORS");
    static final PassiveTreeLayouts.Point DEPOT_SORTER = point("DEPOT_SORTER");
    static final PassiveTreeLayouts.Point BRAKE_RECOVERY_LOOP = point("BRAKE_RECOVERY_LOOP");
    static final PassiveTreeLayouts.Point LOW_DRAG_AXLES = point("LOW_DRAG_AXLES");
    static final PassiveTreeLayouts.Point SWITCHBACK_CACHE = point("SWITCHBACK_CACHE");
    static final PassiveTreeLayouts.Point TIMETABLE_CACHE = point("TIMETABLE_CACHE");
    static final PassiveTreeLayouts.Point SERVICE_SPUR_MARKERS = point("SERVICE_SPUR_MARKERS");
    static final PassiveTreeLayouts.Point SCAN_PREDICTOR = point("SCAN_PREDICTOR");
    static final PassiveTreeLayouts.Point PLANTING_SERVO = point("PLANTING_SERVO");
    static final PassiveTreeLayouts.Point LOGGER_FEED_ROLLERS = point("LOGGER_FEED_ROLLERS");
    static final PassiveTreeLayouts.Point SNAPSHOT_PREFETCH = point("SNAPSHOT_PREFETCH");
    static final PassiveTreeLayouts.Point HOT_SWAP_ROUTINE = point("HOT_SWAP_ROUTINE");
    static final PassiveTreeLayouts.Point ROOT_BOUNDARY_FLAGS = point("ROOT_BOUNDARY_FLAGS");
    static final PassiveTreeLayouts.Point TRUNK_PROFILE_CACHE = point("TRUNK_PROFILE_CACHE");
    static final PassiveTreeLayouts.Point LIMB_CLEARANCE_GAUGE = point("LIMB_CLEARANCE_GAUGE");
    static final PassiveTreeLayouts.Point WIDE_CANOPY_GUARDS = point("WIDE_CANOPY_GUARDS");
    static final PassiveTreeLayouts.Point STORMFALL_PROTOCOL = point("STORMFALL_PROTOCOL");
    static final PassiveTreeLayouts.Point BIN_PARTITIONS = point("BIN_PARTITIONS");
    static final PassiveTreeLayouts.Point SAPLING_RESEED_PATH = point("SAPLING_RESEED_PATH");
    static final PassiveTreeLayouts.Point DROPS_ROUTING_TABLE = point("DROPS_ROUTING_TABLE");
    static final PassiveTreeLayouts.Point RETURN_DEPOT_BUFFER = point("RETURN_DEPOT_BUFFER");
    static final PassiveTreeLayouts.Point CARGO_LOOPBACK = point("CARGO_LOOPBACK");
    static final PassiveTreeLayouts.Point RAILSIDE_CELL_INDEX = point("RAILSIDE_CELL_INDEX");
    static final PassiveTreeLayouts.Point PLANTING_CELL_LEDGER = point("PLANTING_CELL_LEDGER");
    static final PassiveTreeLayouts.Point MANAGED_CELL_GRID = point("MANAGED_CELL_GRID");
    static final PassiveTreeLayouts.Point GROVE_REGISTRY = point("GROVE_REGISTRY");

    static final PassiveTreeLayouts.Point SIGNAL_CHECK = point("SIGNAL_CHECK");
    static final PassiveTreeLayouts.Point BRAKE_TRIM = point("BRAKE_TRIM");
    static final PassiveTreeLayouts.Point ROUTE_LOCK = point("ROUTE_LOCK");
    static final PassiveTreeLayouts.Point PATHING_RELAY = point("PATHING_RELAY");
    static final PassiveTreeLayouts.Point SERVO_GOVERNOR = point("SERVO_GOVERNOR");
    static final PassiveTreeLayouts.Point STABILITY_COUPLER = point("STABILITY_COUPLER");
    static final PassiveTreeLayouts.Point ROLLER_BEARINGS = point("ROLLER_BEARINGS");
    static final PassiveTreeLayouts.Point PACING_WHEEL = point("PACING_WHEEL");
    static final PassiveTreeLayouts.Point GEAR_ADVANCE = point("GEAR_ADVANCE");
    static final PassiveTreeLayouts.Point CUTTING_CADENCE = point("CUTTING_CADENCE");
    static final PassiveTreeLayouts.Point SNAP_FEED = point("SNAP_FEED");
    static final PassiveTreeLayouts.Point RETURN_MOMENTUM = point("RETURN_MOMENTUM");
    static final PassiveTreeLayouts.Point AUX_CELL_CONTACTS = point("AUX_CELL_CONTACTS");
    static final PassiveTreeLayouts.Point FIELD_CACHE = point("FIELD_CACHE");
    static final PassiveTreeLayouts.Point SPARE_CANISTER = point("SPARE_CANISTER");
    static final PassiveTreeLayouts.Point SAPLING_BUFFER = point("SAPLING_BUFFER");
    static final PassiveTreeLayouts.Point CARGO_BRACE = point("CARGO_BRACE");
    static final PassiveTreeLayouts.Point BACKUP_BUS = point("BACKUP_BUS");

    static final PassiveTreeLayouts.Point ROUTE_BEACON = point("ROUTE_BEACON");
    static final PassiveTreeLayouts.Point SIGNAL_REPEATER = point("SIGNAL_REPEATER");
    static final PassiveTreeLayouts.Point MAINTENANCE_SPUR = point("MAINTENANCE_SPUR");
    static final PassiveTreeLayouts.Point CELL_RETURN_TRACE = point("CELL_RETURN_TRACE");
    static final PassiveTreeLayouts.Point SAPLING_LEDGER = point("SAPLING_LEDGER");
    static final PassiveTreeLayouts.Point CANOPY_SAFETY_LATCH = point("CANOPY_SAFETY_LATCH");
    static final PassiveTreeLayouts.Point LOG_CARTOGRAPHER = point("LOG_CARTOGRAPHER");
    static final PassiveTreeLayouts.Point SHEAR_BYPASS_TRACE = point("SHEAR_BYPASS_TRACE");
    static final PassiveTreeLayouts.Point DROP_FILTER_GRID = point("DROP_FILTER_GRID");
    static final PassiveTreeLayouts.Point DEPOT_RECEIVER = point("DEPOT_RECEIVER");
    static final PassiveTreeLayouts.Point CHARGE_WINDOW = point("CHARGE_WINDOW");
    static final PassiveTreeLayouts.Point RESERVE_RAIL_BUS = point("RESERVE_RAIL_BUS");
    static final PassiveTreeLayouts.Point PATH_CLEARANCE_EYE = point("PATH_CLEARANCE_EYE");
    static final PassiveTreeLayouts.Point COASTING_FLYWHEEL = point("COASTING_FLYWHEEL");
    static final PassiveTreeLayouts.Point RAIL_POLISH = point("RAIL_POLISH");
    static final PassiveTreeLayouts.Point SAW_TOOTH_INDEX = point("SAW_TOOTH_INDEX");
    static final PassiveTreeLayouts.Point PLANTING_CLOCK = point("PLANTING_CLOCK");
    static final PassiveTreeLayouts.Point SNAPSHOT_LEDGER = point("SNAPSHOT_LEDGER");
    static final PassiveTreeLayouts.Point LEAF_CHANNEL = point("LEAF_CHANNEL");
    static final PassiveTreeLayouts.Point TRUNK_BRAKE = point("TRUNK_BRAKE");
    static final PassiveTreeLayouts.Point CARGO_SIDING = point("CARGO_SIDING");
    static final PassiveTreeLayouts.Point RETURN_CARTOGRAPHY = point("RETURN_CARTOGRAPHY");
    static final PassiveTreeLayouts.Point CANOPY_WATCH = point("CANOPY_WATCH");
    static final PassiveTreeLayouts.Point ROOT_SAMPLE_GRID = point("ROOT_SAMPLE_GRID");
    static final PassiveTreeLayouts.Point ENERGY_DISPATCH_TABLE = point("ENERGY_DISPATCH_TABLE");
    static final PassiveTreeLayouts.Point CUT_ORDER_ROUTINE = point("CUT_ORDER_ROUTINE");
    static final PassiveTreeLayouts.Point CELL_TENDER_MATRIX = point("CELL_TENDER_MATRIX");
    static final PassiveTreeLayouts.Point CANOPY_WORKPLAN = point("CANOPY_WORKPLAN");

    static List<PassiveTreeGroup> groups() {
        return GROUPS;
    }

    static Placement placement(String nodeId) {
        Placement placement = PLACEMENTS.get(nodeId);
        if (placement == null) {
            throw new IllegalArgumentException("Missing Forestry Companion passive tree placement: " + nodeId);
        }
        return placement;
    }

    private static PassiveTreeLayouts.Point point(String nodeId) {
        return placement(nodeId).center();
    }

    static List<PassiveTreeLayouts.Point> linkPath(String fromId, String toId) {
        Placement from = placement(fromId);
        Placement to = placement(toId);
        List<PassiveTreeLayouts.Point> straight = List.of(from.center(), to.center());
        if (!from.groupId().equals(to.groupId()) || from.orbit() == 0 || from.orbit() != to.orbit()) {
            return straight;
        }
        PassiveTreeLayouts.Point center = group(from.groupId()).center();
        double fromAngle = angleFrom(center, from.center());
        double sweep = signedAngle(angleFrom(center, to.center()) - fromAngle);
        if (Math.abs(sweep) > Math.toRadians(120.0D) + 1.0E-9D) {
            return straight;
        }
        for (Placement candidate : PLACEMENTS.values()) {
            if (candidate == from || candidate == to || !candidate.groupId().equals(from.groupId()) || candidate.orbit() != from.orbit()) {
                continue;
            }
            double offset = signedAngle(angleFrom(center, candidate.center()) - fromAngle);
            if (offset * sweep > 0.0D && Math.abs(offset) < Math.abs(sweep) - 1.0E-9D) {
                return straight;
            }
        }
        int steps = Math.max(2, (int) Math.ceil(Math.abs(sweep) / Math.toRadians(7.5D)));
        double fromRadius = Math.hypot(from.center().x() - center.x(), from.center().y() - center.y());
        double toRadius = Math.hypot(to.center().x() - center.x(), to.center().y() - center.y());
        List<PassiveTreeLayouts.Point> points = new ArrayList<>();
        points.add(from.center());
        for (int index = 1; index < steps; index++) {
            double progress = (double) index / steps;
            double angle = fromAngle + sweep * progress;
            double radius = fromRadius + (toRadius - fromRadius) * progress;
            points.add(PassiveTreeLayouts.Point.of(
                    center.x() + (int) Math.round(Math.cos(angle) * radius),
                    center.y() + (int) Math.round(Math.sin(angle) * radius)
            ));
        }
        points.add(to.center());
        return List.copyOf(points);
    }

    private static double angleFrom(PassiveTreeLayouts.Point center, PassiveTreeLayouts.Point point) {
        return Math.atan2(point.y() - center.y(), point.x() - center.x());
    }

    private static double signedAngle(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    private static List<GroupSeed> groupSeeds() {
        return List.of(
                group("starter", 500, 500),
                group("start_arc", 500, 500),
                group("efficiency_core", 330, 430),
                group("quiet_beacon", 180, 355),
                group("recovery_loop", 390, 265),
                group("coasting_crescent", 210, 175),
                group("coasting_clutch_endpoint", 75, 95),
                group("speed_entry", 565, 320),
                group("cutting_wheel", 565, 125),
                group("overdrive_crescent", 760, 175),
                group("forestry_overdrive_endpoint", 850, 75),
                group("cutting_pocket", 790, 360),
                group("manual_throttle_endpoint", 970, 270),
                group("canopy_entry", 700, 550),
                group("broad_tree_endpoint", 965, 520),
                group("shear_crescent", 880, 670),
                group("leaf_pocket", 865, 825),
                group("serrated_leaf_endpoint", 1005, 875),
                group("storm_pocket", 665, 795),
                group("stormfall_endpoint", 700, 960),
                group("cargo_entry", 360, 650),
                group("cargo_core", 205, 560),
                group("output_pocket", 155, 775),
                group("magnet_endpoint", 55, 955),
                group("servo_spur", 145, 465),
                group("cargo_tail", 340, 855),
                group("seedling_endpoint", 330, 1030),
                group("managed_cells", 520, 720),
                group("reserve_tail", 515, 935)
        );
    }

    private static Map<String, Placement> placements() {
        Map<String, Placement> placements = new LinkedHashMap<>();
        put(placements, "STARTER", "starter", 0, 0);
        put(placements, "BRAKE_TRIM", "start_arc", 2, 0);
        put(placements, "PACING_WHEEL", "start_arc", 2, 5);
        put(placements, "CONSERVATION_LOOP", "start_arc", 2, 9);
        put(placements, "SIGNAL_CHECK", "start_arc", 2, 12);
        put(placements, "ROUTE_SURVEY", "efficiency_core", 1, 0);
        put(placements, "LOW_LOSS_WHEELS", "efficiency_core", 1, 1);
        put(placements, "ENERGY_DISPATCH_TABLE", "efficiency_core", 1, 2);
        put(placements, "RETURN_RAIL_SEALS", "efficiency_core", 1, 3);
        put(placements, "EFFICIENT_ROUTING", "efficiency_core", 1, 4);
        put(placements, "ROUTE_BEACON", "quiet_beacon", 1, 0);
        put(placements, "SIGNAL_REPEATER", "quiet_beacon", 1, 1);
        put(placements, "QUIET_AXLES", "quiet_beacon", 1, 3);
        put(placements, "RETURN_CARTOGRAPHY", "quiet_beacon", 1, 4);
        put(placements, "BRAKE_RECOVERY_LOOP", "recovery_loop", 1, 0);
        put(placements, "ROUTE_BALANCER", "recovery_loop", 1, 1);
        put(placements, "SWITCHBACK_CACHE", "recovery_loop", 1, 3);
        put(placements, "MAINTENANCE_SPUR", "recovery_loop", 1, 4);
        put(placements, "RAIL_POLISH", "coasting_crescent", 2, 0);
        put(placements, "LOW_DRAG_AXLES", "coasting_crescent", 2, 2);
        put(placements, "COASTING_FLYWHEEL", "coasting_crescent", 2, 4);
        put(placements, "TIMETABLE_CACHE", "coasting_crescent", 2, 6);
        put(placements, "PATH_CLEARANCE_EYE", "coasting_crescent", 2, 8);
        put(placements, "SERVICE_SPUR_MARKERS", "coasting_crescent", 2, 11);
        put(placements, "COASTING_CLUTCH", "coasting_clutch_endpoint", 0, 0);
        put(placements, "LIGHT_AXLE", "speed_entry", 1, 0);
        put(placements, "QUICK_SCAN", "speed_entry", 1, 1);
        put(placements, "ROLLER_BEARINGS", "speed_entry", 1, 3);
        put(placements, "GEAR_ADVANCE", "speed_entry", 1, 4);
        put(placements, "FAST_PLANTER", "cutting_wheel", 2, 0);
        put(placements, "SAW_RHYTHM", "cutting_wheel", 2, 2);
        put(placements, "SCAN_PREDICTOR", "cutting_wheel", 2, 5);
        put(placements, "CUTTING_CADENCE", "cutting_wheel", 2, 8);
        put(placements, "SAW_TOOTH_INDEX", "cutting_wheel", 2, 10);
        put(placements, "ADVANCE_TIMER", "cutting_wheel", 2, 13);
        put(placements, "PLANTING_CLOCK", "overdrive_crescent", 1, 0);
        put(placements, "PLANTING_SERVO", "overdrive_crescent", 1, 1);
        put(placements, "LOGGER_FEED_ROLLERS", "overdrive_crescent", 1, 2);
        put(placements, "FIELD_CACHE", "overdrive_crescent", 1, 3);
        put(placements, "PATHING_RELAY", "overdrive_crescent", 1, 4);
        put(placements, "FORESTRY_OVERDRIVE", "forestry_overdrive_endpoint", 0, 0);
        put(placements, "CUT_ORDER_ROUTINE", "cutting_pocket", 2, 0);
        put(placements, "SNAPSHOT_LEDGER", "cutting_pocket", 2, 2);
        put(placements, "SAW_TENSIONER", "cutting_pocket", 2, 5);
        put(placements, "RETURN_MOMENTUM", "cutting_pocket", 2, 8);
        put(placements, "HOT_SWAP_ROUTINE", "cutting_pocket", 2, 10);
        put(placements, "SNAPSHOT_PREFETCH", "cutting_pocket", 2, 13);
        put(placements, "MANUAL_THROTTLE", "manual_throttle_endpoint", 0, 0);
        put(placements, "ROOT_MAPPING", "canopy_entry", 2, 0);
        put(placements, "CANOPY_PROFILE", "canopy_entry", 2, 2);
        put(placements, "HEAVY_SAW_FRAME", "canopy_entry", 2, 4);
        put(placements, "CANOPY_ANCHORS", "canopy_entry", 2, 6);
        put(placements, "ROOT_BOUNDARY_FLAGS", "canopy_entry", 2, 8);
        put(placements, "ROOT_SAMPLE_GRID", "canopy_entry", 2, 10);
        put(placements, "LOG_CARTOGRAPHER", "canopy_entry", 2, 13);
        put(placements, "BROAD_TREE_PROTOCOL", "broad_tree_endpoint", 0, 0);
        put(placements, "TRUNK_PROFILE_CACHE", "shear_crescent", 1, 0);
        put(placements, "TRUNK_BRAKE", "shear_crescent", 1, 1);
        put(placements, "LIMB_CLEARANCE_GAUGE", "shear_crescent", 1, 3);
        put(placements, "SHEAR_BYPASS_TRACE", "shear_crescent", 1, 4);
        put(placements, "LEAF_CHANNEL", "leaf_pocket", 1, 0);
        put(placements, "WIDE_CANOPY_GUARDS", "leaf_pocket", 1, 3);
        put(placements, "SERRATED_LEAF_PROTOCOL", "serrated_leaf_endpoint", 0, 0);
        put(placements, "CANOPY_WORKPLAN", "storm_pocket", 1, 3);
        put(placements, "CANOPY_SAFETY_LATCH", "storm_pocket", 1, 1);
        put(placements, "STABILITY_COUPLER", "storm_pocket", 1, 0);
        put(placements, "CANOPY_WATCH", "storm_pocket", 1, 4);
        put(placements, "STORMFALL_PROTOCOL", "stormfall_endpoint", 0, 0);
        put(placements, "SAPLING_QUEUE", "cargo_entry", 1, 0);
        put(placements, "ROUTE_MEMORY", "cargo_entry", 1, 1);
        put(placements, "FOREST_LOOP", "cargo_entry", 1, 3);
        put(placements, "CARGO_RAKE", "cargo_entry", 1, 4);
        put(placements, "CARGO_SIDING", "cargo_core", 1, 0);
        put(placements, "DEPOT_SORTER", "cargo_core", 1, 2);
        put(placements, "BIN_PARTITIONS", "cargo_core", 1, 4);
        put(placements, "DROPS_ROUTING_TABLE", "output_pocket", 2, 0);
        put(placements, "DROP_FILTER_GRID", "output_pocket", 2, 2);
        put(placements, "CARGO_BRACE", "output_pocket", 2, 4);
        put(placements, "SAPLING_RESEED_PATH", "output_pocket", 2, 6);
        put(placements, "SAPLING_LEDGER", "output_pocket", 2, 8);
        put(placements, "RETURN_DEPOT_BUFFER", "output_pocket", 2, 10);
        put(placements, "DEPOT_RECEIVER", "output_pocket", 2, 13);
        put(placements, "MAGNET_MODE", "magnet_endpoint", 0, 0);
        put(placements, "SERVO_GOVERNOR", "servo_spur", 1, 1);
        put(placements, "SNAP_FEED", "servo_spur", 1, 4);
        put(placements, "CARGO_LOOPBACK", "cargo_tail", 1, 0);
        put(placements, "BACKUP_BUS", "cargo_tail", 1, 2);
        put(placements, "SAPLING_BUFFER", "cargo_tail", 1, 4);
        put(placements, "SEEDLING_MAGNET", "seedling_endpoint", 0, 0);
        put(placements, "RAILSIDE_CELL_INDEX", "managed_cells", 2, 0);
        put(placements, "PLANTING_CELL_LEDGER", "managed_cells", 2, 2);
        put(placements, "CELL_TENDER_MATRIX", "managed_cells", 2, 4);
        put(placements, "MANAGED_CELL_GRID", "managed_cells", 2, 6);
        put(placements, "GROVE_REGISTRY", "managed_cells", 2, 8);
        put(placements, "ROUTE_LOCK", "managed_cells", 2, 10);
        put(placements, "AUX_CELL_CONTACTS", "managed_cells", 2, 13);
        put(placements, "RESERVE_RAIL_BUS", "reserve_tail", 1, 0);
        put(placements, "CHARGE_WINDOW", "reserve_tail", 1, 1);
        put(placements, "CELL_RETURN_TRACE", "reserve_tail", 1, 3);
        put(placements, "SPARE_CANISTER", "reserve_tail", 1, 4);
        return Map.copyOf(placements);
    }

    private static List<PassiveTreeGroup> createGroups() {
        List<PassiveTreeGroup> groups = new ArrayList<>();
        for (GroupSeed seed : GROUP_SEEDS) {
            Map<Integer, List<Integer>> orbitOccupancy = new LinkedHashMap<>();
            List<String> nodeIds = new ArrayList<>();
            for (Map.Entry<String, Placement> entry : PLACEMENTS.entrySet()) {
                Placement placement = entry.getValue();
                if (!placement.groupId().equals(seed.id())) {
                    continue;
                }
                orbitOccupancy.computeIfAbsent(placement.orbit(), ignored -> new ArrayList<>()).add(placement.orbitIndex());
                nodeIds.add(entry.getKey());
            }
            if (nodeIds.isEmpty()) {
                throw new IllegalStateException("Forestry Companion passive tree group has no nodes: " + seed.id());
            }
            groups.add(new PassiveTreeGroup(
                    seed.id(),
                    seed.center(),
                    copyOrbitOccupancy(orbitOccupancy),
                    nodeIds
            ));
        }
        return List.copyOf(groups);
    }

    private static Map<Integer, List<Integer>> copyOrbitOccupancy(Map<Integer, List<Integer>> orbitOccupancy) {
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : orbitOccupancy.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return result;
    }

    private static void put(Map<String, Placement> placements, String nodeId, String groupId, int orbit, int orbitIndex) {
        GroupSeed group = group(groupId);
        PassiveTreeLayouts.Point center = PassiveTreeLayouts.orbitPoint(group.center(), orbit, orbitIndex);
        Placement previous = placements.put(nodeId, new Placement(groupId, orbit, orbitIndex, center));
        if (previous != null) {
            throw new IllegalStateException("Duplicate Forestry Companion passive tree placement: " + nodeId);
        }
    }

    private static GroupSeed group(String groupId) {
        for (GroupSeed group : GROUP_SEEDS) {
            if (group.id().equals(groupId)) {
                return group;
            }
        }
        throw new IllegalArgumentException("Missing Forestry Companion passive tree group: " + groupId);
    }

    private static GroupSeed group(String id, int x, int y) {
        return new GroupSeed(id, PassiveTreeLayouts.Point.of(x, y));
    }

    private ForestryCompanionPassiveTreeLayout() {
    }

    record Placement(String groupId, int orbit, int orbitIndex, PassiveTreeLayouts.Point center) {
    }

    private record GroupSeed(String id, PassiveTreeLayouts.Point center) {
    }
}
