package com.rngtech.rpg;

import com.rngtech.content.energy.HeatCoreMaterial;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Arrays;
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
        crusherYieldCapRecombines();
        multiplierFormulaUsesFactors();
        floorAndBonusScaleRecombine();
        energyUsageReductionsRecombine();
        textListsEverySource();
        sameNamedSourcesShareALine();
        conversionNamesWhatItFeeds();
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

    /** A converted bucket shows once, as the amount it feeds into the target stat, not in both breakdowns. */
    private static void conversionNamesWhatItFeeds() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.PROCESSING_SPEED, 1.25, MachineStat.INSTANT_PROCESS_CHANCE, 0.0));
            recorded.apply(Component.literal("Gear"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 10));
            try (MachineStatAccumulator.Source ignored = recorded.source(Component.literal("Surge"))) {
                recorded.convertIncreased(MachineStat.PROCESSING_SPEED, MachineStat.INSTANT_PROCESS_CHANCE, 0.3);
            }
            return recorded;
        });
        StatBreakdown speed = stats.breakdown(MachineStat.PROCESSING_SPEED).orElseThrow();
        near(speed.recompute(), 1.25, "converted increases leave speed at its base");
        String text = StatBreakdownText.lines(speed).stream().map(Component::getString).collect(Collectors.joining("\n"));
        require(text.contains("rngtech.stat.breakdown.converted") && !text.contains("rngtech.stat.breakdown.increased"),
                "the speed breakdown lists the conversion, not the increases:\n" + text);
        StatBreakdown instant = stats.breakdown(MachineStat.INSTANT_PROCESS_CHANCE).orElseThrow();
        near(instant.recompute(), 3.0, "the conversion feeds Instant Process");
        require(speed.terms(StatBreakdown.Kind.CONVERTED).getFirst().targetStat() == MachineStat.INSTANT_PROCESS_CHANCE, "the conversion names its target");
    }

    /** Dozens of travel nodes share a display name; the text sums them on one line instead of listing each. */
    private static void sameNamedSourcesShareALine() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.DRIVE, 0.0));
            recorded.apply(Component.literal("Drive"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.DRIVE, ModifierOperation.ADD, 2));
            recorded.apply(Component.literal("Drive").withStyle(net.minecraft.ChatFormatting.ITALIC),
                    new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.DRIVE, ModifierOperation.ADD, 2));
            recorded.apply(Component.literal("Drive").withStyle(net.minecraft.ChatFormatting.BOLD),
                    new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.DRIVE, ModifierOperation.ADD, 2));
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.DRIVE).orElseThrow();
        require(breakdown.terms(StatBreakdown.Kind.ADD).size() == 3, "the breakdown keeps each node as its own term");
        List<String> lines = StatBreakdownText.lines(breakdown).stream().map(Component::getString).toList();
        require(lines.stream().filter(line -> line.contains("Drive") && line.contains("×3")).count() == 1
                        && lines.stream().noneMatch(line -> line.trim().startsWith("+2")),
                "same-named sources share one summed line: " + lines);
    }

    private static void crusherYieldCapRecombines() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.2))
                    .withCrusherYieldRules();
            recorded.apply(Component.literal("Jaws"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 100));
            recorded.apply(Component.literal("Tree"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 189));
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.OUTPUT_AMOUNT).orElseThrow();
        near(breakdown.recompute(), stats.value(MachineStat.OUTPUT_AMOUNT), "the capped yield bucket recombines");
        near(breakdown.paidIncreasedPercent(), MachineStatAccumulator.BASE_MAX_OUTPUT_AMOUNT, "the breakdown pays the capped bonus");
        String text = StatBreakdownText.lines(breakdown).stream().map(Component::getString).collect(Collectors.joining("\n"));
        require(text.contains("rngtech.stat.breakdown.increased_cap"), "the breakdown shows the over-cap amount:\n" + text);

        MachineStatAccumulator raised = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.0))
                    .withCrusherYieldRules();
            recorded.apply(Component.literal("Overflow Gate"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.MAX_OUTPUT_AMOUNT, ModifierOperation.ADD, 2));
            recorded.apply(Component.literal("Jaws"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 70));
            return recorded;
        });
        StatBreakdown underCap = raised.breakdown(MachineStat.OUTPUT_AMOUNT).orElseThrow();
        near(underCap.lowest(StatBreakdown.Kind.INCREASED_CAP).value(), 77, "the cap term carries the raised Maximum Bonus Output");
        near(underCap.recompute(), 1.7, "a bucket under the cap pays every percent");
        String headroom = StatBreakdownText.lines(underCap).stream().map(Component::getString).collect(Collectors.joining("\n"));
        require(headroom.contains("rngtech.stat.breakdown.increased_headroom"), "the breakdown shows the headroom left:\n" + headroom);
    }

    private static void multiplierFormulaUsesFactors() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.2))
                    .withCrusherYieldRules();
            recorded.apply(Component.literal("Crush Head"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 25));
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.OUTPUT_AMOUNT).orElseThrow();
        near(breakdown.recompute(), 1.5, "+25% increased under the cap scales a ×1.2 yield to ×1.5");
        Component formula = StatBreakdownText.lines(breakdown).stream()
                .filter(line -> line.getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals("rngtech.stat.breakdown.formula"))
                .findFirst()
                .orElseThrow();
        List<String> operands = Arrays.stream(((TranslatableContents) formula.getContents()).getArgs()).map(String::valueOf).toList();
        require(operands.equals(List.of("1.2", "1.25", "1", "1.5 (+50%)")),
                "a multiplier stat's formula multiplies factors, not its +20% display: " + operands);
    }

    private static void floorAndBonusScaleRecombine() {
        MachineStatAccumulator stats = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.0))
                    .withCrusherYieldRules();
            recorded.floorAbsolute(MachineStat.OUTPUT_AMOUNT, 1.0);
            recorded.apply(Component.literal("Jaws"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.INCREASED_PERCENT, 60));
            try (MachineStatAccumulator.Source ignored = recorded.source(MachineStatAccumulator.NO_BATTERY_SOURCE)) {
                recorded.scaleBonus(MachineStat.OUTPUT_AMOUNT, 0.75);
            }
            return recorded;
        });
        StatBreakdown breakdown = stats.breakdown(MachineStat.OUTPUT_AMOUNT).orElseThrow();
        near(breakdown.recompute(), stats.value(MachineStat.OUTPUT_AMOUNT), "the floor and bonus scale recombine");
        near(breakdown.recompute(), 1 + 0.75 * 0.6, "only the bonus above the floor scales");
        String text = StatBreakdownText.lines(breakdown).stream().map(Component::getString).collect(Collectors.joining("\n"));
        require(text.contains("rngtech.stat.breakdown.bonus_formula"), "the breakdown shows the bonus-only math:\n" + text);

        MachineStatAccumulator floored = MachineStatAccumulator.recording(() -> {
            MachineStatAccumulator recorded = MachineStatAccumulator.componentBase(Map.of(MachineStat.OUTPUT_AMOUNT, 1.0));
            recorded.floorAbsolute(MachineStat.OUTPUT_AMOUNT, 1.0);
            recorded.apply(Component.literal("Curse"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.OUTPUT_AMOUNT, ModifierOperation.DECREASED_PERCENT, 30));
            return recorded;
        });
        StatBreakdown flooredBreakdown = floored.breakdown(MachineStat.OUTPUT_AMOUNT).orElseThrow();
        near(flooredBreakdown.recompute(), 1.0, "a reduced value recombines at the floor");
        String flooredText = StatBreakdownText.lines(flooredBreakdown).stream().map(Component::getString).collect(Collectors.joining("\n"));
        require(flooredText.contains("rngtech.stat.breakdown.floor"), "the breakdown says the floor lifted the value:\n" + flooredText);
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
