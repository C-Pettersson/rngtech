package com.rngtech.rpg;

import com.rngtech.content.energy.HeatCoreMaterial;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Shift-hover stat breakdowns name every source and recombine to the value the stat pipeline produced. */
public final class StatBreakdownChecks {
    private static final Component PART = Component.literal("Steel Heat Core");
    private static int checks;

    private StatBreakdownChecks() {
    }

    public static int run() {
        checks = 0;
        recordingIsOffByDefault();
        keywordMathRecombines();
        sameSourceMerges();
        overridesRecombine();
        partContributionsCarryThePartLabel();
        flatCapacityFoldsIntoCapacity();
        crusherYieldSoftCapRecombines();
        energyUsageReductionsRecombine();
        textListsEverySource();
        return checks;
    }

    private static void recordingIsOffByDefault() {
        MachineStatAccumulator stats = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.0));
        stats.apply(modifier(ModifierSlot.IMPLICIT, ModifierOperation.INCREASED_PERCENT, 50));
        require(stats.breakdown(MachineStat.PROCESSING_SPEED).isEmpty(), "ordinary stat pipelines record nothing");
        require(stats.breakdowns().isEmpty(), "ordinary stat pipelines list no breakdowns");
    }

    private static void keywordMathRecombines() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.MAX_TEMPERATURE, 20.0));
            recorded.apply(modifier(ModifierSlot.PREFIX, ModifierOperation.ADD, 100));
            recorded.apply(Component.literal("Node A"), modifier(ModifierSlot.IMPLICIT, ModifierOperation.INCREASED_PERCENT, 30));
            recorded.apply(Component.literal("Node B"), modifier(ModifierSlot.IMPLICIT, ModifierOperation.DECREASED_PERCENT, 10));
            recorded.apply(Component.literal("Node C"), modifier(ModifierSlot.IMPLICIT, ModifierOperation.MORE, 1.5));
            recorded.apply(MachineStatAccumulator.NO_BATTERY_SOURCE, modifier(ModifierSlot.IMPLICIT, ModifierOperation.LESS, 0.8));
            recorded.apply(Component.literal("Neutral"), modifier(ModifierSlot.IMPLICIT, ModifierOperation.MORE, 1.0));
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.MAX_TEMPERATURE).orElseThrow();
        near(stats.value(MachineStat.MAX_TEMPERATURE), 172.8, "fixture matches the keyword math");
        near(breakdown.finalValue(), 172.8, "breakdown carries the final value");
        near(breakdown.recompute(), breakdown.finalValue(), "terms recombine to the final value");
        near(breakdown.increasedPercent(), 20, "reduced records as a negative increase");
        near(breakdown.more(), 1.2, "less records as a factor below one");
        require(breakdown.terms(StatBreakdown.Kind.ADD).getFirst().source().getString().contains("breakdown.source.affix"),
                "affixes label themselves");
        require(breakdown.terms(StatBreakdown.Kind.MORE).stream().noneMatch(term -> term.source().getString().equals("Neutral")),
                "neutral modifiers stay out of the breakdown");
        require(breakdown.terms().size() == 6, "one term per source and kind: " + breakdown.terms());
    }

    private static void sameSourceMerges() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of());
            try (MachineStatAccumulator.Source ignored = recorded.source(PART)) {
                recorded.apply(modifier(ModifierSlot.IMPLICIT, ModifierOperation.ADD, 2));
                recorded.apply(modifier(ModifierSlot.IMPLICIT, ModifierOperation.ADD, 3));
                recorded.apply(modifier(ModifierSlot.IMPLICIT, ModifierOperation.MORE, 1.5));
                recorded.apply(modifier(ModifierSlot.IMPLICIT, ModifierOperation.MORE, 2.0));
            }
            recorded.apply(modifier(ModifierSlot.IMPLICIT, ModifierOperation.ADD, 1));
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.MAX_TEMPERATURE).orElseThrow();
        List<StatBreakdown.Term> added = breakdown.terms(StatBreakdown.Kind.ADD);
        require(added.size() == 2, "one ADD term per source");
        near(added.getFirst().value(), 5, "same-source adds sum");
        require(added.get(1).source().getString().equals("rngtech.stat.breakdown.source.machine"), "unlabelled contributions fall back to Machine");
        near(breakdown.terms(StatBreakdown.Kind.MORE).getFirst().value(), 3, "same-source more factors multiply");
        near(breakdown.recompute(), stats.value(MachineStat.MAX_TEMPERATURE), "merged terms still recombine");
    }

    private static void overridesRecombine() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.MAX_TEMPERATURE, 400.0));
            try (MachineStatAccumulator.Source ignored = recorded.source(Component.literal("Keystone"))) {
                recorded.setAbsolute(MachineStat.MAX_TEMPERATURE, 1000);
                recorded.setAbsolute(MachineStat.MAX_TEMPERATURE, 900);
            }
            try (MachineStatAccumulator.Source ignored = recorded.source(Component.literal("Ceiling"))) {
                recorded.capAbsolute(MachineStat.MAX_TEMPERATURE, 850);
            }
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.MAX_TEMPERATURE).orElseThrow();
        near(breakdown.lowest(StatBreakdown.Kind.FIXED).value(), 900, "lowest fixed value wins");
        near(breakdown.recompute(), 850, "ceiling applies after the fixed value");
        near(breakdown.recompute(), stats.value(MachineStat.MAX_TEMPERATURE), "overrides recombine");
    }

    private static void partContributionsCarryThePartLabel() {
        MachineTraits partTraits = new MachineTraits(Rarity.MAGIC, 0, List.of(
                new MachineModifier(ModifierSlot.SUFFIX, MachineStat.HEAT_TRANSFER, ModifierOperation.INCREASED_PERCENT, 20)
        ));
        MachineTraits machineTraits = new MachineTraits(Rarity.MAGIC, 3, List.of(
                new MachineModifier(ModifierSlot.PREFIX, MachineStat.HEAT_TRANSFER, ModifierOperation.INCREASED_PERCENT, 15),
                new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.MAX_TEMPERATURE, ModifierOperation.ADD, 50)
        ));
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(
                    MachineStat.HEAT_TRANSFER, 1.0,
                    MachineStat.MAX_TEMPERATURE, 600.0,
                    MachineStat.FUEL_EFFICIENCY, 1.0
            ));
            recorded.apply(machineTraits);
            try (MachineStatAccumulator.Source ignored = recorded.source(PART)) {
                ComponentBaseStatCatalog.applyContribution(
                        recorded,
                        ComponentBaseStatCatalog.heatCore(HeatCoreMaterial.STEEL),
                        partTraits
                );
            }
            return recorded;
        });
        for (StatBreakdown breakdown : stats.breakdowns()) {
            near(breakdown.recompute(), stats.value(breakdown.stat()), breakdown.stat() + " recombines through a Gear part");
        }
        StatBreakdown heatTransfer = stats.breakdown(MachineStat.HEAT_TRANSFER).orElseThrow();
        require(heatTransfer.terms(StatBreakdown.Kind.MORE).stream().anyMatch(term -> term.source().equals(PART)),
                "a part's resolved multiplier is one line named after the part");
        require(heatTransfer.terms(StatBreakdown.Kind.INCREASED).size() == 1,
                "the part's local affix stays inside the part: " + heatTransfer.terms());
        StatBreakdown temperature = stats.breakdown(MachineStat.MAX_TEMPERATURE).orElseThrow();
        require(temperature.terms(StatBreakdown.Kind.ADD).stream().anyMatch(term -> term.source().getString().equals("rngtech.modifier_slot.implicit")),
                "implicits are labelled Implicit");
        StatBreakdown potential = stats.breakdown(MachineStat.REFINEMENT_POTENTIAL).orElseThrow();
        require(potential.terms(StatBreakdown.Kind.ADD).getFirst().source().getString().equals("rngtech.stat.breakdown.source.rarity"),
                "refinement potential is labelled Rarity");
    }

    private static void flatCapacityFoldsIntoCapacity() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.ENERGY_CAPACITY, 1000.0));
            recorded.apply(new MachineModifier(ModifierSlot.PREFIX, MachineStat.ENERGY_CAPACITY_FLAT, ModifierOperation.ADD, 500));
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.ENERGY_CAPACITY_FLAT).orElseThrow();
        require(breakdown.stat() == MachineStat.ENERGY_CAPACITY, "flat capacity explains Energy Capacity");
        near(breakdown.recompute(), 1500, "flat capacity adds to capacity");
        require(stats.breakdowns().stream().noneMatch(entry -> entry.stat() == MachineStat.ENERGY_CAPACITY_FLAT),
                "flat capacity has no separate breakdown");
    }

    private static void crusherYieldSoftCapRecombines() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.2))
                    .withCrusherYieldRules();
            recorded.apply(Component.literal("Jaws"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 100));
            recorded.apply(Component.literal("Tree"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 189));
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.OUTPUT_AMOUNT).orElseThrow();
        near(breakdown.recompute(), stats.value(MachineStat.OUTPUT_AMOUNT), "the soft-capped yield bucket recombines");
        near(breakdown.paidIncreasedPercent(), 289.0 * 100 / 389, "the breakdown pays the soft-capped bonus");
        String text = StatBreakdownText.lines(breakdown).stream().map(Component::getString).collect(Collectors.joining("\n"));
        require(text.contains("rngtech.stat.breakdown.soft_cap"),
                "the breakdown shows the soft-cap math:\n" + text);
    }

    private static void energyUsageReductionsRecombine() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.ENERGY_USAGE, 1.0));
            try (MachineStatAccumulator.Source ignored = recorded.source(Component.literal("Mixed"))) {
                recorded.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_USAGE, ModifierOperation.DECREASED_PERCENT, 60));
                recorded.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_USAGE, ModifierOperation.INCREASED_PERCENT, 50));
            }
            recorded.apply(Component.literal("Keystone"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_USAGE, ModifierOperation.MORE, 1.5));
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.ENERGY_USAGE).orElseThrow();
        near(breakdown.recompute(), stats.value(MachineStat.ENERGY_USAGE), "dividing Energy Usage recombines");
        near(breakdown.reductionsPercent(), 60, "one source keeps its reduction apart from its increase");
        String text = StatBreakdownText.lines(breakdown).stream().map(Component::getString).collect(Collectors.joining("\n"));
        require(text.contains("rngtech.stat.breakdown.divided"), "the breakdown explains dividing reductions:\n" + text);
    }

    private static void textListsEverySource() {
        StatBreakdown breakdown = new StatBreakdown(MachineStat.PROCESSING_SPEED, List.of(
                new StatBreakdown.Term(StatBreakdown.Kind.BASE, 1.0, Component.literal("Base")),
                new StatBreakdown.Term(StatBreakdown.Kind.ADD, 0.2, PART),
                new StatBreakdown.Term(StatBreakdown.Kind.INCREASED, 45, Component.literal("Hasty")),
                new StatBreakdown.Term(StatBreakdown.Kind.MORE, 1.35, Component.literal("Overclock")),
                new StatBreakdown.Term(StatBreakdown.Kind.CEILING, 1.62, Component.literal("Governor"))
        ), 1.62);
        String text = StatBreakdownText.lines(breakdown).stream().map(Component::getString).collect(Collectors.joining("\n"));
        for (String expected : List.of("Steel Heat Core", "Hasty", "Overclock", "+45%", "×1.35", "+0.2")) {
            require(text.contains(expected), "breakdown text shows " + expected + " in:\n" + text);
        }
        require(StatBreakdownText.lines(breakdown).size() == 10, "title, three sections with one line each, formula and cap:\n" + text);
    }

    private static MachineModifier modifier(ModifierSlot slot, ModifierOperation operation, double value) {
        return new MachineModifier(slot, MachineStat.MAX_TEMPERATURE, operation, value);
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void near(double actual, double expected, String label) {
        checks++;
        if (Math.abs(actual - expected) > 0.0001) {
            throw new AssertionError(label + ": expected " + expected + " but was " + actual);
        }
    }
}
