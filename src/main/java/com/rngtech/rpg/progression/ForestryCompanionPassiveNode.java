package com.rngtech.rpg.progression;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.ModifierValueRange;

import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum ForestryCompanionPassiveNode implements StringRepresentable, PassiveNode {
    STARTER(1, PassiveNodeKind.STARTER, ForestryCompanionPassiveTreeLayout.STARTER, true, true, "starter"),

    ROUTE_SURVEY(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.ROUTE_SURVEY, "control", control(5)),
    LOW_LOSS_WHEELS(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.LOW_LOSS_WHEELS,
            "energy_efficiency",
            control(2),
            reserve(1)
    ),
    EFFICIENT_ROUTING(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.EFFICIENT_ROUTING, "energy_efficiency", control(3)),
    CONSERVATION_LOOP(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.CONSERVATION_LOOP,
            "energy_efficiency",
            energyUsageLess(0.92),
            speedUp(5),
            reserve(2)
    ),
    QUIET_AXLES(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.QUIET_AXLES,
            true,
            "control",
            Special.MUTE_MACHINE_SOUND
    ),

    LIGHT_AXLE(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.LIGHT_AXLE, "processing_speed", drive(2), control(1)),
    QUICK_SCAN(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.QUICK_SCAN, "processing_speed", drive(2)),
    FAST_PLANTER(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.FAST_PLANTER, "processing_speed", drive(3)),
    SAW_RHYTHM(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.SAW_RHYTHM,
            "processing_speed",
            speedUp(10),
            energyUsageMore(1.08)),
    FORESTRY_OVERDRIVE(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.FORESTRY_OVERDRIVE,
            "kinetic_overdrive",
            speedUp(10),
            energyUsageMore(1.35)
    ),

    ROOT_MAPPING(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.ROOT_MAPPING, "stability", reserve(2), drive(1)),
    CANOPY_PROFILE(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.CANOPY_PROFILE,
            "full_spectrum_sorting",
            drive(1),
            reserve(2)),
    HEAVY_SAW_FRAME(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.HEAVY_SAW_FRAME,
            "torque_channel",
            speedUp(6),
            treeLimit(3)
    ),
    BROAD_TREE_PROTOCOL(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.BROAD_TREE_PROTOCOL,
            "full_spectrum_sorting",
            treeLimit(8),
            speedDown(6)
    ),

    SAPLING_QUEUE(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.SAPLING_QUEUE, "control", reserve(2), drive(2)),
    CARGO_RAKE(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.CARGO_RAKE,
            "output_yield",
            treeLimit(1),
            energyUsageLess(0.99)
    ),
    ROUTE_MEMORY(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.ROUTE_MEMORY,
            "control",
            speedUp(4),
            energyUsageLess(0.98)
    ),
    FOREST_LOOP(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.FOREST_LOOP,
            "torque_channel",
            speedUp(6),
            treeLimit(3),
            energyUsageMore(1.04)),
    MAGNET_MODE(
            1,
            PassiveNodeKind.KEYSTONE,
            ForestryCompanionPassiveTreeLayout.MAGNET_MODE,
            true,
            "output_yield",
            Special.MAGNET_MODE
    ),
    SERRATED_LEAF_PROTOCOL(
            1,
            PassiveNodeKind.KEYSTONE,
            ForestryCompanionPassiveTreeLayout.SERRATED_LEAF_PROTOCOL,
            true,
            "stability",
            Special.SERRATED_LEAF_PROTOCOL
    ),
    MANUAL_THROTTLE(
            1,
            PassiveNodeKind.KEYSTONE,
            ForestryCompanionPassiveTreeLayout.MANUAL_THROTTLE,
            true,
            "kinetic_overdrive",
            Special.MANUAL_THROTTLE
    ),
    COASTING_CLUTCH(
            1,
            PassiveNodeKind.KEYSTONE,
            ForestryCompanionPassiveTreeLayout.COASTING_CLUTCH,
            true,
            "control",
            Special.COASTING_CLUTCH
    ),
    SEEDLING_MAGNET(
            1,
            PassiveNodeKind.KEYSTONE,
            ForestryCompanionPassiveTreeLayout.SEEDLING_MAGNET,
            true,
            "output_yield",
            Special.SEEDLING_MAGNET
    ),
    RETURN_RAIL_SEALS(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.RETURN_RAIL_SEALS,
            "energy_efficiency",
            control(4)
    ),
    ROUTE_BALANCER(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.ROUTE_BALANCER,
            "control",
            speedUp(4),
            energyUsageLess(0.98)
    ),
    ADVANCE_TIMER(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.ADVANCE_TIMER,
            "processing_speed",
            drive(3)
    ),
    SAW_TENSIONER(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.SAW_TENSIONER,
            "torque_channel",
            speedUp(9),
            treeLimit(2),
            energyUsageMore(1.05)
    ),
    CANOPY_ANCHORS(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.CANOPY_ANCHORS,
            "stability",
            reserve(3)
    ),
    DEPOT_SORTER(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.DEPOT_SORTER,
            "output_yield",
            speedUp(5),
            energyUsageLess(0.97),
            treeLimit(2)
    ),
    BRAKE_RECOVERY_LOOP(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.BRAKE_RECOVERY_LOOP,
            "energy_efficiency",
            control(2),
            drive(1)
    ),
    LOW_DRAG_AXLES(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.LOW_DRAG_AXLES,
            "processing_speed",
            drive(2)
    ),
    SWITCHBACK_CACHE(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.SWITCHBACK_CACHE,
            "control",
            control(2),
            drive(1)
    ),
    TIMETABLE_CACHE(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.TIMETABLE_CACHE,
            "control",
            speedUp(5)
    ),
    SERVICE_SPUR_MARKERS(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.SERVICE_SPUR_MARKERS,
            "processing_speed",
            drive(2),
            reserve(1)
    ),
    SCAN_PREDICTOR(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.SCAN_PREDICTOR,
            "control",
            control(2),
            reserve(1)
    ),
    PLANTING_SERVO(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.PLANTING_SERVO,
            "processing_speed",
            speedUp(7),
            energyUsageMore(1.02)
    ),
    LOGGER_FEED_ROLLERS(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.LOGGER_FEED_ROLLERS,
            "processing_speed",
            speedUp(8),
            energyUsageMore(1.03)
    ),
    SNAPSHOT_PREFETCH(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.SNAPSHOT_PREFETCH,
            "processing_speed",
            speedUp(6),
            treeLimit(1)
    ),
    HOT_SWAP_ROUTINE(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.HOT_SWAP_ROUTINE,
            "kinetic_overdrive",
            speedUp(10),
            energyUsageMore(1.12)
    ),
    ROOT_BOUNDARY_FLAGS(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.ROOT_BOUNDARY_FLAGS,
            "stability",
            reserve(2),
            drive(1)
    ),
    TRUNK_PROFILE_CACHE(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.TRUNK_PROFILE_CACHE,
            "full_spectrum_sorting",
            drive(1),
            reserve(2)
    ),
    LIMB_CLEARANCE_GAUGE(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.LIMB_CLEARANCE_GAUGE,
            "stability",
            treeLimit(2),
            speedDown(2)
    ),
    WIDE_CANOPY_GUARDS(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.WIDE_CANOPY_GUARDS,
            "stability",
            treeLimit(3),
            energyUsageLess(0.98)
    ),
    STORMFALL_PROTOCOL(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.STORMFALL_PROTOCOL,
            "full_spectrum_sorting",
            treeLimit(6),
            speedDown(5),
            energyUsageMore(1.05)
    ),
    BIN_PARTITIONS(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.BIN_PARTITIONS,
            "output_yield",
            reserve(2)
    ),
    SAPLING_RESEED_PATH(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.SAPLING_RESEED_PATH,
            "output_yield",
            reserve(2),
            drive(1)
    ),
    DROPS_ROUTING_TABLE(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.DROPS_ROUTING_TABLE,
            "output_yield",
            reserve(1),
            control(1)
    ),
    RETURN_DEPOT_BUFFER(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.RETURN_DEPOT_BUFFER,
            "output_yield",
            reserve(1),
            control(1)
    ),
    CARGO_LOOPBACK(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.CARGO_LOOPBACK,
            "torque_channel",
            speedUp(4),
            energyUsageLess(0.97)
    ),
    RAILSIDE_CELL_INDEX(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.RAILSIDE_CELL_INDEX,
            false,
            "control",
            Special.MANAGED_CELLS_TINY,
            reserve(4)
    ),
    PLANTING_CELL_LEDGER(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.PLANTING_CELL_LEDGER,
            true,
            "control",
            Special.MANAGED_CELLS_SMALL
    ),
    MANAGED_CELL_GRID(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.MANAGED_CELL_GRID,
            true,
            "control",
            Special.MANAGED_CELLS_MEDIUM
    ),
    GROVE_REGISTRY(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.GROVE_REGISTRY,
            false,
            "control",
            Special.MANAGED_CELLS_LARGE,
            speedUp(6),
            reserve(4),
            control(2)
    ),
    SIGNAL_CHECK(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.SIGNAL_CHECK, "control", control(2)),
    BRAKE_TRIM(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.BRAKE_TRIM, "control", control(2)),
    ROUTE_LOCK(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.ROUTE_LOCK, "control", control(2)),
    PATHING_RELAY(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.PATHING_RELAY, "control", control(2)),
    SERVO_GOVERNOR(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.SERVO_GOVERNOR,
            "control",
            speedUp(3),
            energyUsageLess(0.99)
    ),
    STABILITY_COUPLER(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.STABILITY_COUPLER,
            "control",
            control(1),
            reserve(1)
    ),
    ROLLER_BEARINGS(1, PassiveNodeKind.NODE, ForestryCompanionPassiveTreeLayout.ROLLER_BEARINGS, "processing_speed", speedUp(4)),
    PACING_WHEEL(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.PACING_WHEEL, "processing_speed", drive(2)),
    GEAR_ADVANCE(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.GEAR_ADVANCE, "processing_speed", drive(2)),
    CUTTING_CADENCE(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.CUTTING_CADENCE, "processing_speed", drive(2)),
    SNAP_FEED(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.SNAP_FEED,
            "processing_speed",
            speedUp(3),
            energyUsageMore(1.02)
    ),
    RETURN_MOMENTUM(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.RETURN_MOMENTUM,
            "stability",
            reserve(2),
            control(1)
    ),
    AUX_CELL_CONTACTS(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.AUX_CELL_CONTACTS, "stability", reserve(2)),
    FIELD_CACHE(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.FIELD_CACHE, "stability", reserve(2)),
    SPARE_CANISTER(1, PassiveNodeKind.NODE, ForestryCompanionPassiveTreeLayout.SPARE_CANISTER, "stability", energyUsageLess(0.98)),
    SAPLING_BUFFER(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.SAPLING_BUFFER, "stability", reserve(2)),
    CARGO_BRACE(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.CARGO_BRACE,
            "stability",
            treeLimit(1),
            energyUsageLess(0.99)
    ),
    BACKUP_BUS(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.BACKUP_BUS,
            "stability",
            energyUsageLess(0.98),
            reserve(2)
    ),
    ROUTE_BEACON(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.ROUTE_BEACON,
            "control",
            control(1),
            reserve(1)
    ),
    SIGNAL_REPEATER(1, PassiveNodeKind.NODE, ForestryCompanionPassiveTreeLayout.SIGNAL_REPEATER, "control", speedUp(2), energyUsageLess(0.99)),
    MAINTENANCE_SPUR(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.MAINTENANCE_SPUR,
            "energy_efficiency",
            control(2)
    ),
    CELL_RETURN_TRACE(1, PassiveNodeKind.TRAVEL, ForestryCompanionPassiveTreeLayout.CELL_RETURN_TRACE, "stability", reserve(2)),
    SAPLING_LEDGER(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.SAPLING_LEDGER,
            "output_yield",
            speedUp(2),
            reserve(2)
    ),
    CANOPY_SAFETY_LATCH(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.CANOPY_SAFETY_LATCH,
            "stability",
            treeLimit(1),
            energyUsageLess(0.98)
    ),
    LOG_CARTOGRAPHER(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.LOG_CARTOGRAPHER,
            "full_spectrum_sorting",
            treeLimit(2),
            speedDown(2)
    ),
    SHEAR_BYPASS_TRACE(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.SHEAR_BYPASS_TRACE,
            "energy_efficiency",
            energyUsageLess(0.98),
            reserve(1)
    ),
    DROP_FILTER_GRID(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.DROP_FILTER_GRID,
            "output_yield",
            speedUp(2),
            reserve(2)
    ),
    DEPOT_RECEIVER(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.DEPOT_RECEIVER,
            "output_yield",
            speedUp(3),
            energyUsageLess(0.99)
    ),
    CHARGE_WINDOW(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.CHARGE_WINDOW,
            "energy_efficiency",
            reserve(2)
    ),
    RESERVE_RAIL_BUS(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.RESERVE_RAIL_BUS,
            "stability",
            reserve(2),
            control(1)
    ),
    PATH_CLEARANCE_EYE(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.PATH_CLEARANCE_EYE,
            "control",
            control(1),
            drive(1)
    ),
    COASTING_FLYWHEEL(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.COASTING_FLYWHEEL,
            "processing_speed",
            drive(2)
    ),
    RAIL_POLISH(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.RAIL_POLISH,
            "processing_speed",
            drive(1),
            control(1)
    ),
    SAW_TOOTH_INDEX(1, PassiveNodeKind.NODE, ForestryCompanionPassiveTreeLayout.SAW_TOOTH_INDEX, "processing_speed", speedUp(4)),
    PLANTING_CLOCK(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.PLANTING_CLOCK,
            "processing_speed",
            drive(2)
    ),
    SNAPSHOT_LEDGER(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.SNAPSHOT_LEDGER,
            "stability",
            reserve(2),
            control(1)
    ),
    LEAF_CHANNEL(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.LEAF_CHANNEL,
            "energy_efficiency",
            reserve(2)
    ),
    TRUNK_BRAKE(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.TRUNK_BRAKE,
            "stability",
            control(1),
            reserve(1)
    ),
    CARGO_SIDING(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.CARGO_SIDING,
            "output_yield",
            energyUsageLess(0.98),
            reserve(1)
    ),
    RETURN_CARTOGRAPHY(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.RETURN_CARTOGRAPHY,
            "control",
            control(1)
    ),
    CANOPY_WATCH(
            1,
            PassiveNodeKind.NODE,
            ForestryCompanionPassiveTreeLayout.CANOPY_WATCH,
            "full_spectrum_sorting",
            treeLimit(1),
            energyUsageLess(0.99)
    ),
    ROOT_SAMPLE_GRID(
            1,
            PassiveNodeKind.TRAVEL,
            ForestryCompanionPassiveTreeLayout.ROOT_SAMPLE_GRID,
            "full_spectrum_sorting",
            drive(1),
            reserve(1)
    ),
    ENERGY_DISPATCH_TABLE(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.ENERGY_DISPATCH_TABLE,
            "energy_efficiency",
            energyUsageLess(0.94),
            speedUp(6),
            control(2),
            reserve(2)
    ),
    CUT_ORDER_ROUTINE(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.CUT_ORDER_ROUTINE,
            "processing_speed",
            speedUp(10),
            drive(2),
            energyUsageMore(1.08)
    ),
    CELL_TENDER_MATRIX(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.CELL_TENDER_MATRIX,
            false,
            "control",
            Special.MANAGED_CELLS_SMALL,
            speedUp(4),
            reserve(4),
            control(2),
            drive(2)
    ),
    CANOPY_WORKPLAN(
            1,
            PassiveNodeKind.NOTABLE,
            ForestryCompanionPassiveTreeLayout.CANOPY_WORKPLAN,
            "stability",
            treeLimit(3),
            energyUsageLess(0.98),
            control(2)
    );

    private static final int MAX_NODE_COUNT = Long.SIZE * 2;
    private static final List<List<ForestryCompanionPassiveNode>> LINKS = createLinks();

    private final int requiredLevel;
    private final PassiveNodeKind kind;
    private final int x;
    private final int y;
    private final boolean alwaysAllocated;
    private final boolean grantsNothing;
    private final List<MachineModifierEffect> effects;
    private final String serializedName;
    private final String masteryIconKey;
    private final Set<PassiveNodeFlag> flags;
    private final Map<PassiveStatType, Integer> passiveStats;

    static {
        if (values().length > MAX_NODE_COUNT) {
            throw new IllegalStateException("Forestry Companion passive tree supports at most " + MAX_NODE_COUNT + " nodes");
        }
    }

    ForestryCompanionPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            String masteryIconKey,
            EffectSpec... effects
    ) {
        this(requiredLevel, kind, center, false, false, masteryIconKey, Special.NONE, effects);
    }

    ForestryCompanionPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            boolean grantsNothing,
            String masteryIconKey,
            Special special,
            EffectSpec... effects
    ) {
        this(requiredLevel, kind, center, false, grantsNothing, masteryIconKey, special, effects);
    }

    ForestryCompanionPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            boolean alwaysAllocated,
            boolean grantsNothing,
            String masteryIconKey,
            EffectSpec... effects
    ) {
        this(requiredLevel, kind, center, alwaysAllocated, grantsNothing, masteryIconKey, Special.NONE, effects);
    }

    ForestryCompanionPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            boolean alwaysAllocated,
            boolean grantsNothing,
            String masteryIconKey,
            Special special,
            EffectSpec... effects
    ) {
        this.requiredLevel = requiredLevel;
        this.kind = kind;
        this.x = center.node(displaySize(kind)).x();
        this.y = center.node(displaySize(kind)).y();
        this.alwaysAllocated = alwaysAllocated;
        this.grantsNothing = grantsNothing;
        this.effects = createEffects(grantsNothing, effects);
        this.serializedName = name().toLowerCase(Locale.ROOT);
        this.masteryIconKey = masteryIconKey;
        this.flags = special.flags();
        this.passiveStats = special.passiveStats();
    }

    private static List<List<ForestryCompanionPassiveNode>> createLinks() {
        return ForestryCompanionPassiveTreeDefinition.create().createLinkLists();
    }

    private static void link(
            List<List<ForestryCompanionPassiveNode>> links,
            ForestryCompanionPassiveNode first,
            ForestryCompanionPassiveNode second
    ) {
        addLink(links.get(first.ordinal()), second);
        addLink(links.get(second.ordinal()), first);
    }

    private static void addLink(List<ForestryCompanionPassiveNode> links, ForestryCompanionPassiveNode node) {
        if (!links.contains(node)) {
            links.add(node);
        }
    }

    private static List<MachineModifierEffect> createEffects(boolean grantsNothing, EffectSpec[] effects) {
        if (grantsNothing) {
            return List.of();
        }
        List<MachineModifierEffect> result = new ArrayList<>(effects.length);
        for (EffectSpec effect : effects) {
            result.add(MachineModifierEffect.fixed(effect.stat(), effect.operation(), effect.value()));
        }
        return List.copyOf(result);
    }

    private static EffectSpec speedUp(double value) {
        return effect(MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, value);
    }

    private static EffectSpec speedDown(double value) {
        return effect(MachineStat.PROCESSING_SPEED, ModifierOperation.DECREASED_PERCENT, value);
    }

    private static EffectSpec energyUsageLess(double value) {
        return effect(MachineStat.ENERGY_USAGE, ModifierOperation.LESS, value);
    }

    private static EffectSpec energyUsageMore(double value) {
        return effect(MachineStat.ENERGY_USAGE, ModifierOperation.MORE, value);
    }

    private static EffectSpec treeLimit(double value) {
        return effect(MachineStat.TREE_FELL_LIMIT, ModifierOperation.ADD, value);
    }

    private static EffectSpec control(double value) {
        return effect(MachineStat.CONTROL, ModifierOperation.ADD, value);
    }

    private static EffectSpec drive(double value) {
        return effect(MachineStat.DRIVE, ModifierOperation.ADD, value);
    }

    private static EffectSpec reserve(double value) {
        return effect(MachineStat.RESERVE, ModifierOperation.ADD, value);
    }

    private static EffectSpec effect(MachineStat stat, ModifierOperation operation, double value) {
        return new EffectSpec(stat, operation, value);
    }

    @Override
    public int index() {
        return ordinal();
    }

    @Override
    public int requiredLevel() {
        return requiredLevel;
    }

    @Override
    public PassiveNodeKind kind() {
        return kind;
    }

    private static int displaySize(PassiveNodeKind kind) {
        return switch (kind) {
            case STARTER -> 40;
            case TRAVEL -> 12;
            case NODE -> 16;
            case NOTABLE -> 48;
            case KEYSTONE -> 60;
        };
    }

    @Override
    public int size() {
        return displaySize(kind);
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public boolean alwaysAllocated() {
        return alwaysAllocated;
    }

    @Override
    public boolean grantsNothing() {
        return grantsNothing;
    }

    @Override
    public List<ForestryCompanionPassiveNode> parents() {
        return LINKS.get(ordinal());
    }

    @Override
    public List<PassiveTreeLayouts.Point> linkPathTo(PassiveNode other) {
        if (other instanceof ForestryCompanionPassiveNode forestryNode) {
            return ForestryCompanionPassiveTreeLayout.linkPath(name(), forestryNode.name());
        }
        return PassiveNode.super.linkPathTo(other);
    }

    @Override
    public List<MachineModifierEffect> effects() {
        return effects;
    }

    @Override
    public MachineModifier modifier() {
        MachineModifierEffect primary = effect();
        return new MachineModifier(
                "",
                "forestry_companion_mastery:" + serializedName,
                ModifierSlot.IMPLICIT,
                primary.stat(),
                primary.operation(),
                0,
                ModifierValueRange.fixed(primary.value()),
                primary.value(),
                effects
        );
    }

    private MachineModifierEffect effect() {
        if (effects.isEmpty()) {
            throw new IllegalStateException("Passive node " + name() + " does not define an effect");
        }
        return effects.get(0);
    }

    @Override
    public String masteryIconKey() {
        return masteryIconKey;
    }

    @Override
    public String translationKey() {
        if (kind == PassiveNodeKind.TRAVEL) {
            return "rngtech.mastery.node.attribute";
        }
        return "rngtech.mastery.node.forestry_companion." + serializedName;
    }

    @Override
    public Set<PassiveNodeFlag> flags() {
        return flags;
    }

    @Override
    public int passiveStat(PassiveStatType stat) {
        return passiveStats.getOrDefault(stat, 0);
    }

    public boolean isUnlocked(MachineProgressionState state) {
        return PassiveNode.super.isUnlocked(state);
    }

    public boolean parentUnlocked(MachineProgressionState state) {
        return PassiveNode.super.parentUnlocked(state);
    }

    public static ForestryCompanionPassiveNode byButtonId(int buttonId) {
        return ForestryCompanionPassiveTree.TREE.byButtonId(buttonId);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    private record EffectSpec(MachineStat stat, ModifierOperation operation, double value) {
    }

    private enum Special {
        NONE,
        MUTE_MACHINE_SOUND(Set.of(PassiveNodeFlag.MUTE_MACHINE_SOUND)),
        MAGNET_MODE,
        SERRATED_LEAF_PROTOCOL,
        MANUAL_THROTTLE,
        COASTING_CLUTCH,
        SEEDLING_MAGNET,
        MANAGED_CELLS_TINY(Set.of(), Map.of(PassiveStatType.MANAGED_CELLS, 8)),
        MANAGED_CELLS_SMALL(Set.of(), Map.of(PassiveStatType.MANAGED_CELLS, 16)),
        MANAGED_CELLS_MEDIUM(Set.of(), Map.of(PassiveStatType.MANAGED_CELLS, 24)),
        MANAGED_CELLS_LARGE(Set.of(), Map.of(PassiveStatType.MANAGED_CELLS, 48));

        private final Set<PassiveNodeFlag> flags;
        private final Map<PassiveStatType, Integer> passiveStats;

        Special() {
            this(Set.of(), Map.of());
        }

        Special(Set<PassiveNodeFlag> flags) {
            this(flags, Map.of());
        }

        Special(Set<PassiveNodeFlag> flags, Map<PassiveStatType, Integer> passiveStats) {
            this.flags = Set.copyOf(flags);
            this.passiveStats = Map.copyOf(passiveStats);
        }

        private Set<PassiveNodeFlag> flags() {
            return flags;
        }

        private Map<PassiveStatType, Integer> passiveStats() {
            return passiveStats;
        }
    }
}
