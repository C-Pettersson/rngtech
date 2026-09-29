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
import java.util.Set;

public enum FurnacePassiveNode implements StringRepresentable, PassiveNode {
    STARTER(1, PassiveNodeKind.STARTER, FurnacePassiveTreeLayout.STARTER, true, true, "starter"),

    EMBER_BRICKS(2, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.EMBER_BRICKS, "energy_capacity", flatHeat(25), stabilityDown(1)),
    FIREBOX_LINING(3, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.FIREBOX_LINING, "energy_capacity", flatHeat(25), stabilityDown(1)),
    CLAY_BAFFLES(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.CLAY_BAFFLES, "energy_capacity", flatHeat(50), stabilityDown(2)),
    STOKED_WALLS(5, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.STOKED_WALLS, "energy_capacity", flatHeat(50), stabilityDown(2)),
    HEAT_RESERVOIR(7, PassiveNodeKind.NOTABLE, FurnacePassiveTreeLayout.HEAT_RESERVOIR, "energy_capacity", flatHeat(150), stabilityDown(6)),
    DEEP_FIREBOX(8, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.DEEP_FIREBOX, "energy_capacity", flatHeat(75), stabilityDown(3)),
    REFRACTORY_SHELL(8, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.REFRACTORY_SHELL, "energy_capacity", flatHeat(50), stabilityDown(2)),
    ANNEALED_LINING(9, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.ANNEALED_LINING, "stability", flatHeat(50), stabilityUp(2)),
    FLAME_CHANNELS(9, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.FLAME_CHANNELS, "energy_capacity", flatHeat(50), stabilityDown(2)),
    WHITE_HOT_HEARTH(14, PassiveNodeKind.KEYSTONE, FurnacePassiveTreeLayout.WHITE_HOT_HEARTH, "energy_capacity", flatHeat(300), stabilityDown(12)),

    RADIANT_FOCUS(2, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.RADIANT_FOCUS, "energy_capacity", increasedHeat(5), stabilityDown(2)),
    REFRACTIVE_ARCH(3, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.REFRACTIVE_ARCH, "energy_capacity", increasedHeat(5), stabilityDown(2)),
    THERMAL_LENS(5, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.THERMAL_LENS, "energy_capacity", increasedHeat(8), stabilityDown(3)),
    HEAT_AMPLIFIER(8, PassiveNodeKind.NOTABLE, FurnacePassiveTreeLayout.HEAT_AMPLIFIER, "energy_capacity", increasedHeat(15), stabilityDown(8)),
    PRESSURIZED_FLAME(9, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.PRESSURIZED_FLAME, "energy_capacity", increasedHeat(8), stabilityDown(4)),
    CERAMIC_NOZZLES(10, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.CERAMIC_NOZZLES, "control", increasedHeat(5), stabilityDown(2)),
    FOCUSED_PLASMA(12, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.FOCUSED_PLASMA, "energy_capacity", increasedHeat(10), stabilityDown(5)),
    RADIANT_RETORT(11, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.RADIANT_RETORT, "stability", increasedHeat(5), overheatUp(5), stabilityDown(2)),
    INFERNO_MATRIX(18, PassiveNodeKind.KEYSTONE, FurnacePassiveTreeLayout.INFERNO_MATRIX, "energy_capacity", increasedHeat(30), stabilityDown(15)),

    DRAFT_VENTS(2, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.DRAFT_VENTS, "processing_speed", speedUp(3)),
    BELLOWS_PORTS(3, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.BELLOWS_PORTS, "processing_speed", speedUp(3)),
    QUICK_GRATE(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.QUICK_GRATE, "processing_speed", speedUp(4)),
    FORCED_DRAFT(5, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.FORCED_DRAFT, "processing_speed", speedUp(6)),
    RAPID_DRAW(8, PassiveNodeKind.NOTABLE, FurnacePassiveTreeLayout.RAPID_DRAW, "processing_speed", speedUp(12), flatHeat(-40)),
    PULL_CHAIN(9, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.PULL_CHAIN, "processing_speed", speedUp(4)),
    CHARGED_BELLOWS(10, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.CHARGED_BELLOWS, "processing_speed", speedUp(6), flatHeat(-20)),
    HOT_LOAD_PATH(10, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.HOT_LOAD_PATH, "processing_speed", speedUp(5)),
    FAST_UNLOAD(11, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.FAST_UNLOAD, "processing_speed", speedUp(5), flatHeat(-20)),
    FURNACE_OVERDRIVE(16, PassiveNodeKind.KEYSTONE, FurnacePassiveTreeLayout.FURNACE_OVERDRIVE, "processing_speed", speedUp(30), flatHeat(-100)),

    DAMPING_PLATE(2, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.DAMPING_PLATE, "stability", stabilityUp(3)),
    STABLE_DAMPERS(3, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.STABLE_DAMPERS, "stability", stabilityUp(4)),
    SOFT_WARMUP(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.SOFT_WARMUP, "control", warmupLess(0.94)),
    INSULATED_COOLDOWN(5, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.INSULATED_COOLDOWN, "control", coolingLess(0.94)),
    ANNEALING_PLATE(8, PassiveNodeKind.NOTABLE, FurnacePassiveTreeLayout.ANNEALING_PLATE, "stability", stabilityUp(10), overheatUp(8)),
    OVERHEAT_MARGIN(9, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.OVERHEAT_MARGIN, "stability", overheatUp(8)),
    THERMAL_GOVERNOR(10, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.THERMAL_GOVERNOR, "control", stabilityUp(6), warmupLess(0.97)),
    RADIANT_BRAKE(11, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.RADIANT_BRAKE, "control", coolingLess(0.90), stabilityUp(4)),
    HEAT_SOAK(12, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.HEAT_SOAK, "stability", overheatUp(12), coolingLess(0.92)),
    STABILITY_MATRIX(17, PassiveNodeKind.KEYSTONE, FurnacePassiveTreeLayout.STABILITY_MATRIX, "stability", stabilityUp(20), overheatUp(20), warmupLess(0.90)),

    LOW_LOSS_BUS(2, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.LOW_LOSS_BUS, "energy_efficiency", energyUsageLess(0.98)),
    COAL_BED_TUNING(3, PassiveNodeKind.TRAVEL, FurnacePassiveTreeLayout.COAL_BED_TUNING, "energy_efficiency", fuelEfficiencyUp(3)),
    AIR_MIXER(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.AIR_MIXER, "energy_efficiency", fuelEfficiencyUp(3), energyUsageLess(0.99)),
    EFFICIENT_BURN(5, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.EFFICIENT_BURN, "energy_efficiency", fuelEfficiencyUp(4)),
    RECUPERATOR_COILS(8, PassiveNodeKind.NOTABLE, FurnacePassiveTreeLayout.RECUPERATOR_COILS, "energy_efficiency", energyUsageLess(0.92), fuelEfficiencyUp(10)),
    RETURN_HEAT_EXCHANGER(9, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.RETURN_HEAT_EXCHANGER, "energy_efficiency", fuelEfficiencyUp(5), coolingLess(0.97)),
    CELL_DRIVE_TUNING(9, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.CELL_DRIVE_TUNING, "energy_efficiency", energyUsageLess(0.96)),
    FUEL_TRIM(10, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.FUEL_TRIM, "energy_efficiency", fuelEfficiencyUp(5)),
    ASH_RECYCLER(10, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.ASH_RECYCLER, "energy_efficiency", fuelEfficiencyUp(4)),
    RECUPERATOR_MATRIX(15, PassiveNodeKind.KEYSTONE, FurnacePassiveTreeLayout.RECUPERATOR_MATRIX, "energy_efficiency", energyUsageLess(0.85), fuelEfficiencyUp(20), coolingLess(0.92)),

    MUTE_MACHINE_SOUND(
            4,
            PassiveNodeKind.NODE,
            FurnacePassiveTreeLayout.MUTE_MACHINE_SOUND,
            false,
            true,
            "control",
            Special.MUTE_MACHINE_SOUND
    ),

    TALLER_STACK(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.TALLER_STACK, "energy_capacity", flatHeat(50), stabilityDown(2)),
    HEARTH_RISER(6, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.HEARTH_RISER, "energy_capacity", flatHeat(75), stabilityDown(3)),
    LENS_SPLITTER(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.LENS_SPLITTER, "energy_capacity", increasedHeat(5), stabilityDown(2)),
    FLARE_APERTURE(7, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.FLARE_APERTURE, "energy_capacity", increasedHeat(8), stabilityDown(4)),
    ASH_SLIDE(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.ASH_SLIDE, "processing_speed", speedUp(4), flatHeat(-10)),
    QUICK_EJECTOR(7, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.QUICK_EJECTOR, "processing_speed", speedUp(6), flatHeat(-20)),
    DRAFT_STABILIZER(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.DRAFT_STABILIZER, "stability", stabilityUp(4), warmupLess(0.98)),
    COOLING_BYPASS(7, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.COOLING_BYPASS, "control", coolingLess(0.96), overheatUp(5)),
    PREHEAT_DUCTS(4, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.PREHEAT_DUCTS, "energy_efficiency", fuelEfficiencyUp(3), warmupLess(0.98)),
    CELL_RECOVERY_TRACE(7, PassiveNodeKind.NODE, FurnacePassiveTreeLayout.CELL_RECOVERY_TRACE, "energy_efficiency", energyUsageLess(0.97)),
    THERMAL_MASS(
            12,
            PassiveNodeKind.KEYSTONE,
            FurnacePassiveTreeLayout.THERMAL_MASS,
            "energy_capacity",
            flatHeat(175),
            warmupMore(1.12),
            stabilityDown(6)
    ),
    OVERFIRE_APERTURE(
            14,
            PassiveNodeKind.KEYSTONE,
            FurnacePassiveTreeLayout.OVERFIRE_APERTURE,
            "energy_capacity",
            increasedHeat(16),
            heatTransferUp(8),
            stabilityDown(12),
            energyUsageMore(1.08)
    ),
    CONTINUOUS_DRAFT(
            12,
            PassiveNodeKind.KEYSTONE,
            FurnacePassiveTreeLayout.CONTINUOUS_DRAFT,
            "processing_speed",
            speedUp(20),
            flatHeat(-80),
            energyUsageMore(1.12),
            fuelEfficiencyDown(8)
    ),
    QUENCH_PROTOCOL(
            13,
            PassiveNodeKind.KEYSTONE,
            FurnacePassiveTreeLayout.QUENCH_PROTOCOL,
            "stability",
            stabilityUp(14),
            overheatUp(12),
            coolingUp(20),
            speedDown(6)
    ),
    CLOSED_LOOP_RECUPERATOR(
            13,
            PassiveNodeKind.KEYSTONE,
            FurnacePassiveTreeLayout.CLOSED_LOOP_RECUPERATOR,
            "energy_efficiency",
            energyUsageLess(0.90),
            fuelEfficiencyUp(12),
            speedDown(8),
            coolingLess(0.95)
    );

    private static final int MAX_NODE_COUNT = Long.SIZE * 2;
    private static final List<List<FurnacePassiveNode>> LINKS = createLinks();

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

    static {
        if (values().length > MAX_NODE_COUNT) {
            throw new IllegalStateException("Furnace passive tree supports at most " + MAX_NODE_COUNT + " nodes");
        }
    }

    FurnacePassiveNode(
            int requiredLevel,
            PassiveNodeKind kind,
            PassiveTreeLayouts.Point center,
            String masteryIconKey,
            EffectSpec... effects
    ) {
        this(requiredLevel, kind, center, false, false, masteryIconKey, Special.NONE, effects);
    }

    FurnacePassiveNode(
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

    FurnacePassiveNode(
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
        this.x = center.node(kind.size()).x();
        this.y = center.node(kind.size()).y();
        this.alwaysAllocated = alwaysAllocated;
        this.grantsNothing = grantsNothing;
        this.effects = createEffects(grantsNothing, effects);
        this.serializedName = name().toLowerCase(Locale.ROOT);
        this.masteryIconKey = masteryIconKey;
        this.flags = special.flags();
    }

    private static List<List<FurnacePassiveNode>> createLinks() {
        FurnacePassiveNode[] values = values();
        List<List<FurnacePassiveNode>> links = new ArrayList<>(values.length);
        for (int index = 0; index < values.length; index++) {
            links.add(new ArrayList<>());
        }

        link(links, STARTER, EMBER_BRICKS);
        link(links, EMBER_BRICKS, FIREBOX_LINING);
        link(links, FIREBOX_LINING, CLAY_BAFFLES);
        link(links, CLAY_BAFFLES, STOKED_WALLS);
        link(links, STOKED_WALLS, HEAT_RESERVOIR);
        link(links, HEAT_RESERVOIR, DEEP_FIREBOX);
        link(links, HEAT_RESERVOIR, REFRACTORY_SHELL);
        link(links, REFRACTORY_SHELL, ANNEALED_LINING);
        link(links, ANNEALED_LINING, FLAME_CHANNELS);
        link(links, DEEP_FIREBOX, WHITE_HOT_HEARTH);
        link(links, REFRACTORY_SHELL, WHITE_HOT_HEARTH);

        link(links, STARTER, RADIANT_FOCUS);
        link(links, RADIANT_FOCUS, REFRACTIVE_ARCH);
        link(links, REFRACTIVE_ARCH, THERMAL_LENS);
        link(links, THERMAL_LENS, HEAT_AMPLIFIER);
        link(links, HEAT_AMPLIFIER, PRESSURIZED_FLAME);
        link(links, HEAT_AMPLIFIER, CERAMIC_NOZZLES);
        link(links, CERAMIC_NOZZLES, FOCUSED_PLASMA);
        link(links, CERAMIC_NOZZLES, RADIANT_RETORT);
        link(links, PRESSURIZED_FLAME, INFERNO_MATRIX);
        link(links, FOCUSED_PLASMA, INFERNO_MATRIX);

        link(links, STARTER, DRAFT_VENTS);
        link(links, DRAFT_VENTS, BELLOWS_PORTS);
        link(links, BELLOWS_PORTS, QUICK_GRATE);
        link(links, QUICK_GRATE, FORCED_DRAFT);
        link(links, FORCED_DRAFT, RAPID_DRAW);
        link(links, RAPID_DRAW, PULL_CHAIN);
        link(links, PULL_CHAIN, CHARGED_BELLOWS);
        link(links, RAPID_DRAW, HOT_LOAD_PATH);
        link(links, HOT_LOAD_PATH, FAST_UNLOAD);
        link(links, CHARGED_BELLOWS, FURNACE_OVERDRIVE);
        link(links, FAST_UNLOAD, FURNACE_OVERDRIVE);

        link(links, STARTER, DAMPING_PLATE);
        link(links, DAMPING_PLATE, STABLE_DAMPERS);
        link(links, STABLE_DAMPERS, SOFT_WARMUP);
        link(links, SOFT_WARMUP, INSULATED_COOLDOWN);
        link(links, INSULATED_COOLDOWN, ANNEALING_PLATE);
        link(links, ANNEALING_PLATE, OVERHEAT_MARGIN);
        link(links, OVERHEAT_MARGIN, THERMAL_GOVERNOR);
        link(links, ANNEALING_PLATE, RADIANT_BRAKE);
        link(links, RADIANT_BRAKE, HEAT_SOAK);
        link(links, OVERHEAT_MARGIN, STABILITY_MATRIX);
        link(links, HEAT_SOAK, STABILITY_MATRIX);

        link(links, STARTER, LOW_LOSS_BUS);
        link(links, LOW_LOSS_BUS, COAL_BED_TUNING);
        link(links, COAL_BED_TUNING, AIR_MIXER);
        link(links, AIR_MIXER, EFFICIENT_BURN);
        link(links, EFFICIENT_BURN, RECUPERATOR_COILS);
        link(links, RECUPERATOR_COILS, RETURN_HEAT_EXCHANGER);
        link(links, RECUPERATOR_COILS, CELL_DRIVE_TUNING);
        link(links, RETURN_HEAT_EXCHANGER, FUEL_TRIM);
        link(links, CELL_DRIVE_TUNING, ASH_RECYCLER);
        link(links, FUEL_TRIM, RECUPERATOR_MATRIX);
        link(links, ASH_RECYCLER, RECUPERATOR_MATRIX);

        link(links, STOKED_WALLS, ANNEALED_LINING);
        link(links, HEAT_AMPLIFIER, RAPID_DRAW);
        link(links, RAPID_DRAW, RECUPERATOR_COILS);
        link(links, ANNEALING_PLATE, RECUPERATOR_COILS);
        link(links, FLAME_CHANNELS, THERMAL_GOVERNOR);
        link(links, RADIANT_RETORT, RAPID_DRAW);
        link(links, STARTER, MUTE_MACHINE_SOUND);
        link(links, MUTE_MACHINE_SOUND, EMBER_BRICKS);
        link(links, MUTE_MACHINE_SOUND, DAMPING_PLATE);
        link(links, FIREBOX_LINING, TALLER_STACK);
        link(links, TALLER_STACK, HEARTH_RISER);
        link(links, REFRACTIVE_ARCH, LENS_SPLITTER);
        link(links, LENS_SPLITTER, FLARE_APERTURE);
        link(links, BELLOWS_PORTS, ASH_SLIDE);
        link(links, ASH_SLIDE, QUICK_EJECTOR);
        link(links, STABLE_DAMPERS, DRAFT_STABILIZER);
        link(links, DRAFT_STABILIZER, COOLING_BYPASS);
        link(links, COAL_BED_TUNING, PREHEAT_DUCTS);
        link(links, PREHEAT_DUCTS, CELL_RECOVERY_TRACE);
        link(links, HEARTH_RISER, THERMAL_MASS);
        link(links, FLARE_APERTURE, OVERFIRE_APERTURE);
        link(links, QUICK_EJECTOR, CONTINUOUS_DRAFT);
        link(links, COOLING_BYPASS, QUENCH_PROTOCOL);
        link(links, CELL_RECOVERY_TRACE, CLOSED_LOOP_RECUPERATOR);

        List<List<FurnacePassiveNode>> immutable = new ArrayList<>(links.size());
        for (List<FurnacePassiveNode> nodeLinks : links) {
            immutable.add(List.copyOf(nodeLinks));
        }
        return List.copyOf(immutable);
    }

    private static void link(List<List<FurnacePassiveNode>> links, FurnacePassiveNode first, FurnacePassiveNode second) {
        addLink(links.get(first.ordinal()), second);
        addLink(links.get(second.ordinal()), first);
    }

    private static void addLink(List<FurnacePassiveNode> links, FurnacePassiveNode node) {
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

    private static EffectSpec flatHeat(double value) {
        return effect(MachineStat.MAX_TEMPERATURE, ModifierOperation.ADD, value);
    }

    private static EffectSpec increasedHeat(double value) {
        return effect(MachineStat.MAX_TEMPERATURE, ModifierOperation.INCREASED_PERCENT, value);
    }

    private static EffectSpec stabilityDown(double value) {
        return effect(MachineStat.TEMPERATURE_STABILITY, ModifierOperation.DECREASED_PERCENT, value);
    }

    private static EffectSpec stabilityUp(double value) {
        return effect(MachineStat.TEMPERATURE_STABILITY, ModifierOperation.INCREASED_PERCENT, value);
    }

    private static EffectSpec speedUp(double value) {
        return effect(MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, value);
    }

    private static EffectSpec speedDown(double value) {
        return effect(MachineStat.PROCESSING_SPEED, ModifierOperation.DECREASED_PERCENT, value);
    }

    private static EffectSpec heatTransferUp(double value) {
        return effect(MachineStat.HEAT_TRANSFER, ModifierOperation.INCREASED_PERCENT, value);
    }

    private static EffectSpec overheatUp(double value) {
        return effect(MachineStat.OVERHEAT_TOLERANCE, ModifierOperation.INCREASED_PERCENT, value);
    }

    private static EffectSpec warmupMore(double value) {
        return effect(MachineStat.WARMUP_TIME, ModifierOperation.MORE, value);
    }

    private static EffectSpec warmupLess(double value) {
        return effect(MachineStat.WARMUP_TIME, ModifierOperation.LESS, value);
    }

    private static EffectSpec coolingUp(double value) {
        return effect(MachineStat.COOLING_RATE, ModifierOperation.INCREASED_PERCENT, value);
    }

    private static EffectSpec coolingLess(double value) {
        return effect(MachineStat.COOLING_RATE, ModifierOperation.LESS, value);
    }

    private static EffectSpec energyUsageMore(double value) {
        return effect(MachineStat.ENERGY_USAGE, ModifierOperation.MORE, value);
    }

    private static EffectSpec energyUsageLess(double value) {
        return effect(MachineStat.ENERGY_USAGE, ModifierOperation.LESS, value);
    }

    private static EffectSpec fuelEfficiencyUp(double value) {
        return effect(MachineStat.FUEL_EFFICIENCY, ModifierOperation.INCREASED_PERCENT, value);
    }

    private static EffectSpec fuelEfficiencyDown(double value) {
        return effect(MachineStat.FUEL_EFFICIENCY, ModifierOperation.DECREASED_PERCENT, value);
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
    public List<FurnacePassiveNode> parents() {
        return LINKS.get(ordinal());
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
                "furnace_mastery:" + serializedName,
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
        return "rngtech.mastery.node.furnace." + serializedName;
    }

    @Override
    public Set<PassiveNodeFlag> flags() {
        return flags;
    }

    @Override
    public int passiveStat(PassiveStatType stat) {
        return 0;
    }

    public boolean isUnlocked(MachineProgressionState state) {
        return PassiveNode.super.isUnlocked(state);
    }

    public boolean parentUnlocked(MachineProgressionState state) {
        return PassiveNode.super.parentUnlocked(state);
    }

    public static FurnacePassiveNode byButtonId(int buttonId) {
        int index = buttonId - 100;
        return index >= 0 && index < values().length ? values()[index] : null;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    private record EffectSpec(MachineStat stat, ModifierOperation operation, double value) {
    }

    private enum Special {
        NONE,
        MUTE_MACHINE_SOUND(Set.of(PassiveNodeFlag.MUTE_MACHINE_SOUND));

        private final Set<PassiveNodeFlag> flags;

        Special() {
            this(Set.of());
        }

        Special(Set<PassiveNodeFlag> flags) {
            this.flags = Set.copyOf(flags);
        }

        private Set<PassiveNodeFlag> flags() {
            return flags;
        }
    }
}
