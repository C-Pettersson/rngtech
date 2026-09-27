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

public enum CrusherPassiveNode implements StringRepresentable, PassiveNode {
    STARTER(1, PassiveNodeKind.STARTER, CrusherPassiveTreeLayout.STARTER, true, true, Identity.STARTER),
    KINETIC_FOUNDATION(2, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.KINETIC_FOUNDATION, Identity.CONTROL),
    EFFICIENT_DRIVE(3, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.EFFICIENT_DRIVE, Identity.CONTROL),
    JAW_ALIGNMENT(4, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.JAW_ALIGNMENT, Identity.CONTROL),
    CELL_BYPASS(
            7,
            PassiveNodeKind.KEYSTONE,
            CrusherPassiveTreeLayout.CELL_BYPASS,
            Identity.ENERGY_CAPACITY,
            Identity.ENERGY_EFFICIENCY,
            Special.CELL_BYPASS
    ),
    FRAME_RESERVOIR(2, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.FRAME_RESERVOIR, Identity.CONTROL),
    CAPACITOR_TRACE(3, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.CAPACITOR_TRACE, Identity.ENERGY_CAPACITY),
    COPPER_PLATES(4, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.COPPER_PLATES, Identity.ENERGY_CAPACITY),
    BUFFER_LAMINATE(5, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.BUFFER_LAMINATE, Identity.ENERGY_CAPACITY),
    LOW_LOSS_BELT(5, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.LOW_LOSS_BELT, Identity.ENERGY_EFFICIENCY),
    CURRENT_TRIM(4, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.CURRENT_TRIM, Identity.ENERGY_EFFICIENCY),
    STABLE_INTAKE(3, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.STABLE_INTAKE, Identity.ENERGY_EFFICIENCY),
    ORE_SAMPLER(6, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.ORE_SAMPLER, Identity.OUTPUT_YIELD),
    MICRO_GEARING(6, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.MICRO_GEARING, Identity.PROCESSING_SPEED),
    KINETIC_OVERDRIVE(7, PassiveNodeKind.NOTABLE, CrusherPassiveTreeLayout.KINETIC_OVERDRIVE, Identity.PROCESSING_SPEED),
    ORE_SIEVE(7, PassiveNodeKind.NOTABLE, CrusherPassiveTreeLayout.ORE_SIEVE, Identity.OUTPUT_YIELD),
    CRUSHING_MOMENTUM(6, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.CRUSHING_MOMENTUM, Identity.PROCESSING_SPEED),
    DUST_RECOVERY(6, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.DUST_RECOVERY, Identity.OUTPUT_YIELD),
    OUTPUT_KINETIC_CORE(
            5,
            PassiveNodeKind.NOTABLE,
            CrusherPassiveTreeLayout.OUTPUT_KINETIC_CORE,
            Identity.PROCESSING_SPEED,
            Identity.OUTPUT_YIELD
    ),
    CHARGED_FLYWHEEL(2, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.CHARGED_FLYWHEEL, Identity.CONTROL),
    SCREENED_OVERDRIVE(2, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.SCREENED_OVERDRIVE, Identity.CONTROL),
    POWERED_SIEVE(3, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.POWERED_SIEVE, Identity.CONTROL),
    SORTING_RAKE(4, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.SORTING_RAKE, Identity.CONTROL),
    MATERIAL_MEMORY(4, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.MATERIAL_MEMORY, Identity.CONTROL),
    FALSE_TOOTH_SPACERS(5, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.FALSE_TOOTH_SPACERS, Identity.CONTROL),
    SAMPLE_TRAY(
            6,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.SAMPLE_TRAY,
            Identity.CONTROL,
            true,
            Identity.OUTPUT_YIELD,
            false,
            Special.NONE
    ),
    CLEAN_DUST_LEDGE(
            6,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.CLEAN_DUST_LEDGE,
            Identity.CONTROL,
            true,
            Identity.OUTPUT_YIELD,
            false,
            Special.NONE
    ),
    SIEVE_PLATES(
            5,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.SIEVE_PLATES,
            Identity.CONTROL,
            true,
            Identity.OUTPUT_YIELD,
            false,
            Special.NONE
    ),
    CALIBRATED_FEED(
            4,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.CALIBRATED_FEED,
            Identity.CONTROL,
            true,
            Identity.OUTPUT_YIELD,
            false,
            Special.NONE
    ),
    OUTPUT_FEED_RAKE(
            3,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.OUTPUT_FEED_RAKE,
            Identity.CONTROL,
            true,
            Identity.OUTPUT_YIELD,
            false,
            Special.NONE
    ),
    HOPPER_RHYTHM(
            6,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.HOPPER_RHYTHM,
            Identity.PROCESSING_SPEED,
            Identity.ENERGY_EFFICIENCY
    ),
    REINFORCED_FLYWHEEL(
            6,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.REINFORCED_FLYWHEEL,
            Identity.PROCESSING_SPEED,
            Identity.ENERGY_EFFICIENCY
    ),
    KINETIC_LINKAGE(
            5,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.KINETIC_LINKAGE,
            Identity.PROCESSING_SPEED,
            Identity.ENERGY_EFFICIENCY
    ),
    LOAD_SMOOTHING(
            4,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.LOAD_SMOOTHING,
            Identity.PROCESSING_SPEED,
            Identity.ENERGY_EFFICIENCY
    ),
    IDLE_DAMPERS(
            3,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.IDLE_DAMPERS,
            Identity.PROCESSING_SPEED,
            Identity.ENERGY_EFFICIENCY
    ),
    HIGH_SPEED_BEARINGS(8, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.HIGH_SPEED_BEARINGS, Identity.PROCESSING_SPEED),
    COMPRESSION_RAMP(7, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.COMPRESSION_RAMP, Identity.PROCESSING_SPEED),
    BALANCED_THROW(6, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.BALANCED_THROW, Identity.PROCESSING_SPEED),
    MOMENTUM_COUPLER(7, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.MOMENTUM_COUPLER, Identity.PROCESSING_SPEED),
    PARALLEL_FEED_RAIL(7, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.PARALLEL_FEED_RAIL, Identity.PROCESSING_SPEED),
    INDUCTION_COILS(8, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.INDUCTION_COILS, Identity.PROCESSING_SPEED),
    SOFT_STARTER(7, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.SOFT_STARTER, Identity.PROCESSING_SPEED),
    HARDENED_BEARINGS(6, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.HARDENED_BEARINGS, Identity.PROCESSING_SPEED),
    OVERDRIVE_GOVERNOR(7, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.OVERDRIVE_GOVERNOR, Identity.PROCESSING_SPEED),
    FULL_SPECTRUM_SORTING(5, PassiveNodeKind.NOTABLE, CrusherPassiveTreeLayout.FULL_SPECTRUM_SORTING, Identity.OUTPUT_YIELD),
    RESERVE_CHANNELS(10, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.RESERVE_CHANNELS, Identity.ENERGY_CAPACITY),
    CELL_CONTACTS(9, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.CELL_CONTACTS, Identity.ENERGY_CAPACITY),
    MASSIVE_BUFFER(8, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.MASSIVE_BUFFER, Identity.ENERGY_CAPACITY),
    BATTERY_SHELL(7, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.BATTERY_SHELL, Identity.ENERGY_CAPACITY),
    AUXILIARY_BANK(6, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.AUXILIARY_BANK, Identity.ENERGY_CAPACITY),
    DEEP_RESERVE_CHANNELS(5, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.DEEP_RESERVE_CHANNELS, Identity.ENERGY_CAPACITY),
    BUFFER_OVERFLOW_BUS(4, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.BUFFER_OVERFLOW_BUS, Identity.ENERGY_CAPACITY),
    ENERGY_GOVERNOR(10, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.ENERGY_GOVERNOR, Identity.ENERGY_EFFICIENCY, true),
    ARC_PRESSURE_DRIVE(9, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.ARC_PRESSURE_DRIVE, Identity.ENERGY_EFFICIENCY, true),
    RECOVERY_BUS(8, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.RECOVERY_BUS, Identity.ENERGY_EFFICIENCY, true),
    INTERNAL_BUS(7, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.INTERNAL_BUS, Identity.ENERGY_EFFICIENCY, true),
    RETURN_BUS(6, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.RETURN_BUS, Identity.ENERGY_EFFICIENCY, true),
    LOAD_DAMPERS(5, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.LOAD_DAMPERS, Identity.ENERGY_EFFICIENCY, true),
    LOW_LOSS_RETURN(4, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.LOW_LOSS_RETURN, Identity.ENERGY_EFFICIENCY, true),
    BUFFERED_DISCHARGE(
            3,
            PassiveNodeKind.TRAVEL,
            CrusherPassiveTreeLayout.BUFFERED_DISCHARGE,
            Identity.ENERGY_EFFICIENCY,
            Identity.ENERGY_CAPACITY
    ),
    RIGID_BUS_BAR(6, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.RIGID_BUS_BAR, Identity.CONTROL),
    DENSE_BATCHING(
            11,
            PassiveNodeKind.KEYSTONE,
            CrusherPassiveTreeLayout.DENSE_BATCHING,
            Identity.PROCESSING_SPEED,
            Special.DENSE_BATCHING
    ),
    PRECISION_JAW_MOUNT(
            9,
            PassiveNodeKind.KEYSTONE,
            CrusherPassiveTreeLayout.PRECISION_JAW_MOUNT,
            Identity.OUTPUT_YIELD,
            Special.PRECISION_JAW_MOUNT
    ),
    YIELD_PRECONCENTRATOR(
            11,
            PassiveNodeKind.NODE,
            CrusherPassiveTreeLayout.YIELD_PRECONCENTRATOR,
            Identity.OUTPUT_YIELD,
            false,
            Identity.ENERGY_EFFICIENCY,
            true,
            Special.NONE
    ),
    FINE_DUST_RECOVERY(
            10,
            PassiveNodeKind.NODE,
            CrusherPassiveTreeLayout.FINE_DUST_RECOVERY,
            Identity.OUTPUT_YIELD,
            false,
            Identity.ENERGY_EFFICIENCY,
            true,
            Special.NONE
    ),
    SPECTRAL_SIEVE_PLATES(
            9,
            PassiveNodeKind.NODE,
            CrusherPassiveTreeLayout.SPECTRAL_SIEVE_PLATES,
            Identity.OUTPUT_YIELD,
            false,
            Identity.ENERGY_EFFICIENCY,
            true,
            Special.NONE
    ),
    RICH_ORE_SAMPLER(
            8,
            PassiveNodeKind.NODE,
            CrusherPassiveTreeLayout.RICH_ORE_SAMPLER,
            Identity.OUTPUT_YIELD,
            false,
            Identity.ENERGY_EFFICIENCY,
            true,
            Special.NONE
    ),
    YIELD_GOVERNOR(
            7,
            PassiveNodeKind.NODE,
            CrusherPassiveTreeLayout.YIELD_GOVERNOR,
            Identity.OUTPUT_YIELD,
            false,
            Identity.ENERGY_EFFICIENCY,
            true,
            Special.NONE
    ),
    PRECISION_JAW_ARRAY(
            6,
            PassiveNodeKind.NODE,
            CrusherPassiveTreeLayout.PRECISION_JAW_ARRAY,
            Identity.OUTPUT_YIELD,
            false,
            Identity.ENERGY_EFFICIENCY,
            true,
            Special.NONE
    ),
    DEEP_CAPACITOR_BANK(11, PassiveNodeKind.NOTABLE, CrusherPassiveTreeLayout.DEEP_CAPACITOR_BANK, Identity.ENERGY_CAPACITY),
    BATTERY_CORE_MATRIX(8, PassiveNodeKind.KEYSTONE, CrusherPassiveTreeLayout.BATTERY_CORE_MATRIX, Identity.ENERGY_CAPACITY),
    LOSSLESS_BUS_MATRIX(8, PassiveNodeKind.KEYSTONE, CrusherPassiveTreeLayout.LOSSLESS_BUS_MATRIX, Identity.ENERGY_EFFICIENCY),
    ACCELERATION_PLATES(6, PassiveNodeKind.NOTABLE, CrusherPassiveTreeLayout.ACCELERATION_PLATES, Identity.PROCESSING_SPEED),
    TORQUE_CHANNEL(5, PassiveNodeKind.NOTABLE, CrusherPassiveTreeLayout.TORQUE_CHANNEL, Identity.PROCESSING_SPEED),
    KINETIC_TENSIONER(4, PassiveNodeKind.NOTABLE, CrusherPassiveTreeLayout.KINETIC_TENSIONER, Identity.PROCESSING_SPEED),
    BUS_CAPACITOR_NODE(7, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.BUS_CAPACITOR_NODE, Identity.ENERGY_CAPACITY),
    UPPER_CELL_CONTACT(8, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.UPPER_CELL_CONTACT, Identity.ENERGY_CAPACITY),
    RESERVE_CELL_PLATES(9, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.RESERVE_CELL_PLATES, Identity.ENERGY_CAPACITY),
    OUTER_CAPACITOR_SHELL(10, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.OUTER_CAPACITOR_SHELL, Identity.ENERGY_CAPACITY),
    EXPANSION_BUFFER(11, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.EXPANSION_BUFFER, Identity.ENERGY_CAPACITY),
    RETURN_CAPACITOR_SHELL(11, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.RETURN_CAPACITOR_SHELL, Identity.ENERGY_CAPACITY),
    RESERVE_CELL_BANK(10, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.RESERVE_CELL_BANK, Identity.ENERGY_CAPACITY),
    LOWER_CELL_CONTACT(9, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.LOWER_CELL_CONTACT, Identity.ENERGY_CAPACITY),
    BUFFER_RING_BUS(8, PassiveNodeKind.NODE, CrusherPassiveTreeLayout.BUFFER_RING_BUS, Identity.ENERGY_CAPACITY),
    VIBRATION_BRACE(7, PassiveNodeKind.TRAVEL, CrusherPassiveTreeLayout.VIBRATION_BRACE, Identity.STABILITY),
    MUTE_MACHINE_SOUND(
            4,
            PassiveNodeKind.NODE,
            CrusherPassiveTreeLayout.MUTE_MACHINE_SOUND,
            false,
            true,
            Identity.MACHINE_SOUND,
            false,
            Identity.NONE,
            false,
            Special.MUTE_MACHINE_SOUND
    ),
    OVERSPEC_COMPONENT_MOUNT(
            7,
            PassiveNodeKind.NODE,
            CrusherPassiveTreeLayout.OVERSPEC_COMPONENT_MOUNT,
            false,
            true,
            Identity.COMPONENT_SUPPORT,
            false,
            Identity.NONE,
            false,
            Special.COMPONENT_STAGE_SUPPORT
    ),
    UNIVERSAL_COMPONENT_MOUNT(
            12,
            PassiveNodeKind.NOTABLE,
            CrusherPassiveTreeLayout.UNIVERSAL_COMPONENT_MOUNT,
            false,
            true,
            Identity.COMPONENT_SUPPORT,
            false,
            Identity.NONE,
            false,
            Special.COMPONENT_STAGE_SUPPORT
    );

    private static final int MAX_NODE_COUNT = Long.SIZE * 2;
    private static final List<List<CrusherPassiveNode>> LINKS = createLinks();

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
            throw new IllegalStateException("Crusher passive tree supports at most " + MAX_NODE_COUNT + " nodes");
        }
    }

    CrusherPassiveNode(int requiredLevel, PassiveNodeKind kind, PassiveTreeLayouts.Point center, Identity primary) {
        this(requiredLevel, kind, center, false, false, primary, false, Identity.NONE, false, Special.NONE);
    }

    CrusherPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            Identity primary,
            boolean primaryNegative
    ) {
        this(requiredLevel, kind, center, false, false, primary, primaryNegative, Identity.NONE, false, Special.NONE);
    }

    CrusherPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            Identity primary,
            Identity secondary
    ) {
        this(requiredLevel, kind, center, false, false, primary, false, secondary, false, Special.NONE);
    }

    CrusherPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            Identity primary,
            Identity secondary,
            Special special
    ) {
        this(requiredLevel, kind, center, false, false, primary, false, secondary, false, special);
    }

    CrusherPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            Identity primary,
            Special special
    ) {
        this(requiredLevel, kind, center, false, false, primary, false, Identity.NONE, false, special);
    }

    CrusherPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            boolean alwaysAllocated,
            boolean grantsNothing,
            Identity primary
    ) {
        this(requiredLevel, kind, center, alwaysAllocated, grantsNothing, primary, false, Identity.NONE, false, Special.NONE);
    }

    CrusherPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            Identity primary,
            boolean primaryNegative,
            Identity secondary,
            boolean secondaryNegative,
            Special special
    ) {
        this(requiredLevel, kind, center, false, false, primary, primaryNegative, secondary, secondaryNegative, special);
    }

    CrusherPassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            boolean alwaysAllocated,
            boolean grantsNothing,
            Identity primary,
            boolean primaryNegative,
            Identity secondary,
            boolean secondaryNegative,
            Special special
    ) {
        this.requiredLevel = requiredLevel;
        this.kind = kind;
        this.x = center.node(kind.size()).x();
        this.y = center.node(kind.size()).y();
        this.alwaysAllocated = alwaysAllocated;
        this.grantsNothing = grantsNothing;
        this.effects = grantsNothing ? List.of() : createEffects(kind, primary, primaryNegative, secondary, secondaryNegative, special);
        flags = special.flags();
        passiveStats = special.passiveStats();
        serializedName = name().toLowerCase(Locale.ROOT);
        masteryIconKey = masteryIconKey(serializedName, kind, primary, special);
    }

    private static String masteryIconKey(String serializedName, PassiveNodeKind kind, Identity primary, Special special) {
        if (special == Special.COMPONENT_STAGE_SUPPORT) {
            return primary.masteryIconKey();
        }
        if (kind == PassiveNodeKind.NOTABLE || kind == PassiveNodeKind.KEYSTONE) {
            return serializedName;
        }
        return primary.masteryIconKey();
    }

    private static List<List<CrusherPassiveNode>> createLinks() {
        CrusherPassiveNode[] values = values();
        List<List<CrusherPassiveNode>> links = new ArrayList<>(values.length);
        for (int index = 0; index < values.length; index++) {
            links.add(new ArrayList<>());
        }
        link(links, CAPACITOR_TRACE, COPPER_PLATES);
        link(links, BUFFER_LAMINATE, COPPER_PLATES);
        link(links, CURRENT_TRIM, LOW_LOSS_BELT);
        link(links, CURRENT_TRIM, STABLE_INTAKE);
        link(links, CLEAN_DUST_LEDGE, SAMPLE_TRAY);
        link(links, CLEAN_DUST_LEDGE, SIEVE_PLATES);
        link(links, CALIBRATED_FEED, SIEVE_PLATES);
        link(links, CALIBRATED_FEED, OUTPUT_FEED_RAKE);
        link(links, HOPPER_RHYTHM, REINFORCED_FLYWHEEL);
        link(links, KINETIC_LINKAGE, REINFORCED_FLYWHEEL);
        link(links, KINETIC_LINKAGE, LOAD_SMOOTHING);
        link(links, IDLE_DAMPERS, LOAD_SMOOTHING);
        link(links, COMPRESSION_RAMP, HIGH_SPEED_BEARINGS);
        link(links, BALANCED_THROW, COMPRESSION_RAMP);
        link(links, BALANCED_THROW, MOMENTUM_COUPLER);
        link(links, MOMENTUM_COUPLER, PARALLEL_FEED_RAIL);
        link(links, INDUCTION_COILS, PARALLEL_FEED_RAIL);
        link(links, INDUCTION_COILS, SOFT_STARTER);
        link(links, HARDENED_BEARINGS, SOFT_STARTER);
        link(links, HARDENED_BEARINGS, OVERDRIVE_GOVERNOR);
        link(links, CELL_CONTACTS, RESERVE_CHANNELS);
        link(links, CELL_CONTACTS, MASSIVE_BUFFER);
        link(links, BATTERY_SHELL, MASSIVE_BUFFER);
        link(links, AUXILIARY_BANK, BATTERY_SHELL);
        link(links, AUXILIARY_BANK, DEEP_RESERVE_CHANNELS);
        link(links, BUFFER_OVERFLOW_BUS, DEEP_RESERVE_CHANNELS);
        link(links, ARC_PRESSURE_DRIVE, ENERGY_GOVERNOR);
        link(links, ARC_PRESSURE_DRIVE, RECOVERY_BUS);
        link(links, INTERNAL_BUS, RECOVERY_BUS);
        link(links, INTERNAL_BUS, RETURN_BUS);
        link(links, LOAD_DAMPERS, RETURN_BUS);
        link(links, LOAD_DAMPERS, LOW_LOSS_RETURN);
        link(links, BUFFERED_DISCHARGE, BUFFER_OVERFLOW_BUS);
        link(links, BUFFERED_DISCHARGE, LOW_LOSS_RETURN);
        link(links, FINE_DUST_RECOVERY, YIELD_PRECONCENTRATOR);
        link(links, FINE_DUST_RECOVERY, SPECTRAL_SIEVE_PLATES);
        link(links, RICH_ORE_SAMPLER, SPECTRAL_SIEVE_PLATES);
        link(links, RICH_ORE_SAMPLER, YIELD_GOVERNOR);
        link(links, PRECISION_JAW_ARRAY, YIELD_GOVERNOR);
        link(links, ACCELERATION_PLATES, TORQUE_CHANNEL);
        link(links, TORQUE_CHANNEL, KINETIC_TENSIONER);
        link(links, BUS_CAPACITOR_NODE, UPPER_CELL_CONTACT);
        link(links, UPPER_CELL_CONTACT, RESERVE_CELL_PLATES);
        link(links, RESERVE_CELL_PLATES, OUTER_CAPACITOR_SHELL);
        link(links, OUTER_CAPACITOR_SHELL, EXPANSION_BUFFER);
        link(links, EXPANSION_BUFFER, RETURN_CAPACITOR_SHELL);
        link(links, RETURN_CAPACITOR_SHELL, RESERVE_CELL_BANK);
        link(links, RESERVE_CELL_BANK, LOWER_CELL_CONTACT);
        link(links, LOWER_CELL_CONTACT, BUFFER_RING_BUS);
        link(links, KINETIC_FOUNDATION, STARTER);
        link(links, EFFICIENT_DRIVE, KINETIC_FOUNDATION);
        link(links, EFFICIENT_DRIVE, JAW_ALIGNMENT);
        link(links, CELL_BYPASS, JAW_ALIGNMENT);
        link(links, FRAME_RESERVOIR, STARTER);
        link(links, CAPACITOR_TRACE, FRAME_RESERVOIR);
        link(links, FRAME_RESERVOIR, STABLE_INTAKE);
        link(links, BUFFER_LAMINATE, CELL_BYPASS);
        link(links, CELL_BYPASS, LOW_LOSS_BELT);
        link(links, CELL_BYPASS, ORE_SAMPLER);
        link(links, CELL_BYPASS, MICRO_GEARING);
        link(links, KINETIC_OVERDRIVE, MICRO_GEARING);
        link(links, ORE_SAMPLER, ORE_SIEVE);
        link(links, CRUSHING_MOMENTUM, KINETIC_OVERDRIVE);
        link(links, DUST_RECOVERY, ORE_SIEVE);
        link(links, CRUSHING_MOMENTUM, OUTPUT_KINETIC_CORE);
        link(links, DUST_RECOVERY, OUTPUT_KINETIC_CORE);
        link(links, CHARGED_FLYWHEEL, STARTER);
        link(links, SCREENED_OVERDRIVE, STARTER);
        link(links, POWERED_SIEVE, SCREENED_OVERDRIVE);
        link(links, POWERED_SIEVE, SORTING_RAKE);
        link(links, MATERIAL_MEMORY, POWERED_SIEVE);
        link(links, FALSE_TOOTH_SPACERS, JAW_ALIGNMENT);
        link(links, CHARGED_FLYWHEEL, IDLE_DAMPERS);
        link(links, CHARGED_FLYWHEEL, OUTPUT_FEED_RAKE);
        link(links, HOPPER_RHYTHM, OUTPUT_KINETIC_CORE);
        link(links, OUTPUT_KINETIC_CORE, SAMPLE_TRAY);
        link(links, HIGH_SPEED_BEARINGS, OVERDRIVE_GOVERNOR);
        link(links, BALANCED_THROW, OUTPUT_KINETIC_CORE);
        link(links, FULL_SPECTRUM_SORTING, HARDENED_BEARINGS);
        link(links, FULL_SPECTRUM_SORTING, MATERIAL_MEMORY);
        link(links, OUTPUT_KINETIC_CORE, SORTING_RAKE);
        link(links, BUFFERED_DISCHARGE, SCREENED_OVERDRIVE);
        link(links, CELL_BYPASS, RIGID_BUS_BAR);
        link(links, FALSE_TOOTH_SPACERS, RIGID_BUS_BAR);
        link(links, DENSE_BATCHING, RIGID_BUS_BAR);
        link(links, MATERIAL_MEMORY, PRECISION_JAW_MOUNT);
        link(links, FALSE_TOOTH_SPACERS, PRECISION_JAW_ARRAY);
        link(links, RESERVE_CHANNELS, DEEP_CAPACITOR_BANK);
        link(links, ENERGY_GOVERNOR, DEEP_CAPACITOR_BANK);
        link(links, BATTERY_SHELL, BATTERY_CORE_MATRIX);
        link(links, INTERNAL_BUS, LOSSLESS_BUS_MATRIX);
        link(links, EFFICIENT_DRIVE, KINETIC_TENSIONER);
        link(links, BUS_CAPACITOR_NODE, BUFFER_RING_BUS);
        link(links, RIGID_BUS_BAR, BUS_CAPACITOR_NODE);
        link(links, PARALLEL_FEED_RAIL, BUFFER_RING_BUS);
        link(links, RIGID_BUS_BAR, VIBRATION_BRACE);
        link(links, PARALLEL_FEED_RAIL, VIBRATION_BRACE);
        link(links, IDLE_DAMPERS, MUTE_MACHINE_SOUND);
        link(links, VIBRATION_BRACE, OVERSPEC_COMPONENT_MOUNT);
        link(links, OVERSPEC_COMPONENT_MOUNT, UNIVERSAL_COMPONENT_MOUNT);
        List<List<CrusherPassiveNode>> immutable = new ArrayList<>(links.size());
        for (List<CrusherPassiveNode> nodeLinks : links) {
            immutable.add(List.copyOf(nodeLinks));
        }
        return List.copyOf(immutable);
    }

    private static void link(List<List<CrusherPassiveNode>> links, CrusherPassiveNode first, CrusherPassiveNode second) {
        addLink(links.get(first.ordinal()), second);
        addLink(links.get(second.ordinal()), first);
    }

    private static void addLink(List<CrusherPassiveNode> links, CrusherPassiveNode node) {
        if (!links.contains(node)) {
            links.add(node);
        }
    }

    private static List<MachineModifierEffect> createEffects(
            PassiveNodeKind kind,
            Identity primary,
            boolean primaryNegative,
            Identity secondary,
            boolean secondaryNegative,
            Special special
    ) {
        return switch (special) {
            case CELL_BYPASS -> List.of(
                    fixed(MachineStat.ENERGY_CAPACITY, ModifierOperation.INCREASED_PERCENT, 100.0),
                    fixed(MachineStat.ENERGY_USAGE, ModifierOperation.MORE, 1.1),
                    fixed(MachineStat.NO_BATTERY_OUTPUT_RETENTION, ModifierOperation.ADD, 100.0)
            );
            case DENSE_BATCHING -> List.of(
                    fixed(MachineStat.PARALLEL_JOBS, ModifierOperation.ADD, 2.0),
                    fixed(MachineStat.PROCESSING_SPEED, ModifierOperation.LESS, 0.75),
                    fixed(MachineStat.ENERGY_USAGE, ModifierOperation.MORE, 1.25)
            );
            case PRECISION_JAW_MOUNT -> List.of(
                    fixed(MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 20.0),
                    fixed(MachineStat.CRUSHER_SALVAGE_CHANCE, ModifierOperation.ADD, 3.0)
            );
            case MUTE_MACHINE_SOUND, COMPONENT_STAGE_SUPPORT, NONE -> standardEffects(
                    kind,
                    primary,
                    primaryNegative,
                    secondary,
                    secondaryNegative
            );
        };
    }

    private static List<MachineModifierEffect> standardEffects(
            PassiveNodeKind kind,
            Identity primary,
            boolean primaryNegative,
            Identity secondary,
            boolean secondaryNegative
    ) {
        List<MachineModifierEffect> effects = new ArrayList<>();
        addIdentityEffect(effects, kind, primary, primaryNegative, 1.0);
        addIdentityEffect(effects, kind, secondary, secondaryNegative, 0.5);
        return List.copyOf(effects);
    }

    private static void addIdentityEffect(
            List<MachineModifierEffect> effects,
            PassiveNodeKind kind,
            Identity identity,
            boolean negative,
            double scale
    ) {
        switch (identity) {
            case ENERGY_CAPACITY, STORAGE -> addEnergyCapacityEffect(effects, kind, negative, scale);
            case ENERGY_EFFICIENCY -> {
                double reduction = magnitude(kind, 0.02, 0.03, 0.1, 0.15) * scale;
                effects.add(fixed(
                        MachineStat.ENERGY_USAGE,
                        negative ? ModifierOperation.MORE : ModifierOperation.LESS,
                        negative ? 1.0 + reduction : 1.0 - reduction
                ));
            }
            case ENERGY_TRANSFER -> effects.add(fixed(
                    MachineStat.ENERGY_TRANSFER,
                    ModifierOperation.ADD,
                    signed(magnitude(kind, 3.0, 10.0, 25.0, 40.0) * scale, negative)
            ));
            case OUTPUT_YIELD, THROUGHPUT -> effects.add(fixed(
                    MachineStat.OUTPUT_AMOUNT,
                    negative ? ModifierOperation.DECREASED_PERCENT : ModifierOperation.INCREASED_PERCENT,
                    magnitude(kind, 0.4, 3.0, 12.0, 20.0) * scale
            ));
            case PROCESSING_SPEED, KINETIC_FOUNDATION -> effects.add(fixed(
                    MachineStat.PROCESSING_SPEED,
                    negative ? ModifierOperation.DECREASED_PERCENT : ModifierOperation.INCREASED_PERCENT,
                    magnitude(kind, 0.4, 3.0, 12.0, 20.0) * scale
            ));
            case PRECISION, RECOVERY -> effects.add(fixed(
                    MachineStat.CRUSHER_SALVAGE_CHANCE,
                    ModifierOperation.ADD,
                    signed(magnitude(kind, 0.2, 1.0, 2.5, 4.0) * scale, negative)
            ));
            case STABILITY -> effects.add(fixed(
                    MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION,
                    ModifierOperation.ADD,
                    signed(magnitude(kind, 1.0, 4.0, 10.0, 16.0) * scale, negative)
            ));
            case CONTROL -> effects.add(fixed(
                    MachineStat.OUTPUT_GUARD_GRACE,
                    ModifierOperation.ADD,
                    signed(magnitude(kind, 5.0, 15.0, 40.0, 60.0) * scale, negative)
            ));
            case NONE, STARTER -> {
            }
        }
    }

    private static void addEnergyCapacityEffect(
            List<MachineModifierEffect> effects,
            PassiveNodeKind kind,
            boolean negative,
            double scale
    ) {
        if (kind == PassiveNodeKind.NOTABLE) {
            effects.add(fixed(
                    MachineStat.ENERGY_CAPACITY,
                    negative ? ModifierOperation.DECREASED_PERCENT : ModifierOperation.INCREASED_PERCENT,
                    25.0 * scale
            ));
            return;
        }
        effects.add(fixed(
                MachineStat.ENERGY_CAPACITY_FLAT,
                ModifierOperation.ADD,
                signed(magnitude(kind, 8.0, 30.0, 120.0, 220.0) * scale, negative)
        ));
    }

    private static MachineModifierEffect fixed(MachineStat stat, ModifierOperation operation, double value) {
        return MachineModifierEffect.fixed(stat, operation, value);
    }

    private static double signed(double value, boolean negative) {
        return negative ? -value : value;
    }

    private static double magnitude(PassiveNodeKind kind, double travel, double node, double notable, double keystone) {
        return switch (kind) {
            case KEYSTONE -> keystone;
            case NOTABLE -> notable;
            case NODE -> node;
            case STARTER, TRAVEL -> travel;
        };
    }

    @Override
    public int index() {
        return ordinal();
    }

    @Override
    public int requiredLevel() {
        return requiredLevel;
    }

    public CrusherPassiveNode parent() {
        List<CrusherPassiveNode> linked = parents();
        return linked.isEmpty() ? null : linked.get(0);
    }

    @Override
    public List<CrusherPassiveNode> parents() {
        return LINKS.get(ordinal());
    }

    @Override
    public PassiveNodeKind kind() {
        return kind;
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
    public String masteryIconKey() {
        return masteryIconKey;
    }

    public MachineModifierEffect effect() {
        if (effects.isEmpty()) {
            throw new IllegalStateException("Passive node " + name() + " does not define an effect");
        }
        return effects.get(0);
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
                "crusher_mastery:" + serializedName,
                ModifierSlot.IMPLICIT,
                primary.stat(),
                primary.operation(),
                0,
                ModifierValueRange.fixed(primary.value()),
                primary.value(),
                effects
        );
    }

    public boolean blocksBatteryCell() {
        return this == CELL_BYPASS;
    }

    public boolean requiresMatchingCrushHeadStage() {
        return this == PRECISION_JAW_MOUNT;
    }

    public boolean enablesDenseParallel() {
        return this == DENSE_BATCHING;
    }

    @Override
    public String translationKey() {
        return "rngtech.mastery.node." + serializedName;
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

    public static CrusherPassiveNode byButtonId(int buttonId) {
        return CrusherPassiveTree.TREE.byButtonId(buttonId);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    private enum Identity {
        NONE("control"),
        STARTER("starter"),
        KINETIC_FOUNDATION("processing_speed"),
        PROCESSING_SPEED("processing_speed"),
        THROUGHPUT("output_yield"),
        ENERGY_CAPACITY("energy_capacity"),
        ENERGY_EFFICIENCY("energy_efficiency"),
        ENERGY_TRANSFER("energy_capacity"),
        OUTPUT_YIELD("output_yield"),
        PRECISION("output_yield"),
        CONTROL("control"),
        STABILITY("stability"),
        STORAGE("energy_capacity"),
        RECOVERY("output_yield"),
        MACHINE_SOUND("control"),
        COMPONENT_SUPPORT("stability");

        private final String masteryIconKey;

        Identity(String masteryIconKey) {
            this.masteryIconKey = masteryIconKey;
        }

        private String masteryIconKey() {
            return masteryIconKey;
        }
    }

    private enum Special {
        NONE,
        CELL_BYPASS,
        DENSE_BATCHING,
        PRECISION_JAW_MOUNT,
        MUTE_MACHINE_SOUND(Set.of(PassiveNodeFlag.MUTE_MACHINE_SOUND), Map.of()),
        COMPONENT_STAGE_SUPPORT(Set.of(), Map.of(PassiveStatType.COMPONENT_STAGE_SUPPORT, 1));

        private final Set<PassiveNodeFlag> flags;
        private final Map<PassiveStatType, Integer> passiveStats;

        Special() {
            this(Set.of(), Map.of());
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
