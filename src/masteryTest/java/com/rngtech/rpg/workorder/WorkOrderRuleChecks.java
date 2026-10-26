package com.rngtech.rpg.workorder;

import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Stream;

/** Rolling, loading, counting, and reward rolls for Work Orders, run from {@code MasteryChecks}. */
public final class WorkOrderRuleChecks {
    private static int checks;

    private WorkOrderRuleChecks() { }

    public static int run() {
        checks = 0;
        List<WorkObjectiveEntry> shipped = shippedObjectives();
        rolls(shipped);
        ranges(shipped);
        loadRules();
        counting();
        itemJobs(shipped);
        rewardAmounts();
        rush();
        statGoals(shipped);
        streaks();
        groups();
        reachable(shipped);
        return checks;
    }

    private static void rolls(List<WorkObjectiveEntry> shipped) {
        for (int tier = WorkOrder.MIN_TIER; tier <= WorkOrder.MAX_TIER; tier++) {
            WorkOrder first = WorkOrderRules.roll(shipped, tier, new SplittableRandom(42L));
            require(first != null && first.equals(WorkOrderRules.roll(List.copyOf(shipped).reversed(), tier, new SplittableRandom(42L))),
                    "a roll for a seed and tier does not depend on load order");
            require(first.tier() == tier && first.minMasteryLevel() == 0 && first.bounty() == WorkOrder.BASE_BOUNTY + first.objective().bonus()
                    && first.prestige() == 0, "a rolled order has base Bounty plus its band bonus and no Mastery level requirement");
            require(first.minStage() == Math.min(WorkOrder.tierStage(tier), WorkOrderFamilies.topStage(first.family())),
                    "a rolled order asks for its tier's stage, capped at the family's top stage");
        }
        require(WorkOrderRules.roll(List.of(), 1, new SplittableRandom(1L)) == null, "nothing rolls without objectives");

        var melter = entry("rngtech:melt", "melter", 1);
        require(WorkOrderRules.roll(List.of(melter), 5, new SplittableRandom(1L)).minStage() == 6,
                "a Tier V Melter order asks for the Melter's top stage");
        var heavy = entry("rngtech:heavy", "crusher", 3);
        var light = entry("rngtech:light", "furnace", 1);
        var random = new SplittableRandom(7L);
        int heavyDraws = 0;
        Set<String> families = new HashSet<>();
        for (int i = 0; i < 4_000; i++) {
            WorkOrder rolled = WorkOrderRules.roll(List.of(heavy, light), 2, random);
            families.add(rolled.family());
            if (rolled.objective().id().equals(heavy.id())) { heavyDraws++; }
        }
        require(families.size() == 2 && heavyDraws > 2_700 && heavyDraws < 3_300, "objective weights set the roll odds, and each objective names its family");
        WorkObjective fixed = heavy.roll(2, 8, random, item -> true);
        require(fixed.totalTarget() == 20 && fixed.parts().getFirst().minimum() == 2 && fixed.bonus() == 0, "a fixed goal rolls its count with no band bonus");

        var tiers = new int[WorkOrder.MAX_TIER + 1];
        for (int i = 0; i < 10_000; i++) { tiers[WorkOrderRules.roll(List.of(heavy, light), random).tier()]++; }
        boolean even = true;
        for (int tier = WorkOrder.MIN_TIER; tier <= WorkOrder.MAX_TIER; tier++) { even &= tiers[tier] > 1_800 && tiers[tier] < 2_200; }
        require(even, "a board roll picks every tier equally often");
    }

    private static void ranges(List<WorkObjectiveEntry> shipped) {
        var goal = new WorkObjectiveEntry.Goal(208, 304, 8, 3);
        var random = new SplittableRandom(3L);
        int[] bands = new int[3];
        boolean onStep = true;
        for (int i = 0; i < 6_000; i++) {
            long count = goal.roll(random);
            onStep &= count >= 208 && count <= 304 && count % 8 == 0;
            int bonus = goal.bonus(count);
            double position = (count - 208) / 96.0;
            int band = position < 1.0 / 3.0 ? 0 : position < 2.0 / 3.0 ? 1 : 2;
            require(bonus == new int[] {0, 10, 20}[band], "the band bonus matches the band " + count + " falls in");
            bands[band]++;
        }
        require(onStep, "a rolled count stays in its range on a multiple of its step");
        require(bands[0] > 1_500 && bands[1] > 1_500 && bands[2] > 1_500, "counts land in all three bands");
        require(goal.bonus(208) == 0 && goal.bonus(304) == 20 && goal.bonus(256) == 10, "the low, middle, and high bands pay 0, 10, and 20");
        require(new WorkObjectiveEntry.Goal(9, 15, 8, 0).roll(random) == 9, "a range with no multiple of the step rolls its minimum");
        for (WorkObjectiveEntry entry : shipped) {
            for (int tier = WorkOrder.MIN_TIER; tier <= WorkOrder.MAX_TIER; tier++) {
                WorkOrder order = WorkOrderRules.roll(List.of(entry), tier, random);
                require(order.bounty() == WorkOrder.BASE_BOUNTY + order.objective().bonus(), entry.id() + " pays its band bonus as Bounty");
                var tierGoal = entry.goal(tier);
                require(tierGoal.max() > tierGoal.min(), entry.id() + " Tier " + tier + " rolls a count range");
            }
        }
    }

    private static void loadRules() {
        var empty = MachineWorkOrders.EMPTY;
        var tierOne = order("crusher", 4, 0);
        require(WorkOrderRules.loadFailure(empty, "crusher", 4, 1, tierOne) == null, "a level 1 Stage 4 Crusher loads an order with no level requirement");
        require("blank".equals(WorkOrderRules.loadFailure(empty, "crusher", 8, 100, null)), "a blank order never loads");
        require("family".equals(WorkOrderRules.loadFailure(empty, "furnace", 8, 100, tierOne)), "an order loads only on its family");
        require("stage".equals(WorkOrderRules.loadFailure(empty, "crusher", 3, 100, tierOne)), "an order needs its stage");
        var veteran = order("crusher", 4, 60);
        require("level".equals(WorkOrderRules.loadFailure(empty, "crusher", 8, 59, veteran))
                && WorkOrderRules.loadFailure(empty, "crusher", 8, 60, veteran) == null, "an order with a level requirement needs that Mastery level");
        require("level".equals(WorkOrderRules.loadFailure(empty, "crusher", 8, 0, veteran)),
                "a machine without Mastery cannot load an order that needs a Mastery level");
        var full = empty.withLoaded(loaded(tierOne)).withLoaded(loaded(tierOne)).withLoaded(loaded(tierOne));
        require("full".equals(WorkOrderRules.loadFailure(full, "crusher", 8, 100, tierOne)), "a machine holds three orders");
    }

    private static void counting() {
        var first = loaded(new WorkOrder("crusher", 2, single(WorkFilter.STAGE, 6, "", 10), 5, 0, 100, 0));
        var second = loaded(new WorkOrder("crusher", 1, single(WorkFilter.STAGE, 3, "", 5), 4, 0, 100, 0));
        var orders = MachineWorkOrders.EMPTY.withLoaded(first).withLoaded(second);
        require(WorkOrderRules.progress(orders, crush(WorkObjectiveType.SMELT, 99, "", 3)).orders() == orders, "other work does not count");
        require(WorkOrderRules.progress(orders, crush(WorkObjectiveType.CRUSH, 5, "", 3)).orders() == orders, "work below the minimum does not count");
        require(WorkOrderRules.progress(orders.withActive(MachineWorkOrders.NONE), crush(WorkObjectiveType.CRUSH, 8, "", 3)).orders()
                .equals(orders.withActive(MachineWorkOrders.NONE)), "a machine with no active chip counts nothing");
        var three = WorkOrderRules.progress(orders, crush(WorkObjectiveType.CRUSH, 6, "", 3));
        require(three.fulfilled() == null && three.orders().orders().get(0).totalProgress() == 3 && three.orders().orders().get(1).totalProgress() == 0,
                "matching work counts each item a batch completes, toward the active chip only");
        var switched = WorkOrderRules.progress(three.orders().withActive(1), crush(WorkObjectiveType.CRUSH, 4, "", 2)).orders();
        require(switched.orders().get(0).totalProgress() == 3 && switched.orders().get(1).totalProgress() == 2,
                "after a switch, work counts toward the new active chip and the old one keeps its progress");
        var done = WorkOrderRules.progress(switched, crush(WorkObjectiveType.CRUSH, 8, "", 9));
        require(done.fulfilled() != null && done.fulfilled().fulfilled() && done.fulfilled().totalProgress() == 5, "the chip fulfils at its target");
        require(done.orders().orders().size() == 2 && done.orders().active() == MachineWorkOrders.NONE
                && done.orders().orders().get(0).totalProgress() == 3 && done.orders().orders().get(1).fulfilled(),
                "a fulfilled chip stays in its bay, inactive, and no other chip becomes active");
        require(done.orders().withActive(1) == done.orders(), "a fulfilled chip cannot be made active again");
        require(new MachineWorkOrders(done.orders().orders(), 1).active() == MachineWorkOrders.NONE, "a stored active index never points at a fulfilled chip");

        var items = new WorkObjective(id("items"), List.of(new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.ITEM, 0, "rngtech:raw_tin", 4),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.ITEM, 0, "rngtech:raw_lead", 3)), 0);
        var multi = MachineWorkOrders.EMPTY.withLoaded(loaded(new WorkOrder("crusher", 1, items, 4, 0, 100, 0)));
        var tin = WorkOrderRules.progress(multi, crush(WorkObjectiveType.CRUSH, 2, "rngtech:raw_tin", 6));
        require(tin.fulfilled() == null && tin.orders().activeOrder().progress().equals(List.of(4L, 0L)),
                "an item part counts only its item and stops at its target");
        require(WorkOrderRules.progress(tin.orders(), crush(WorkObjectiveType.CRUSH, 2, "rngtech:raw_iron", 5)).orders() == tin.orders(),
                "an item no part names does not count");
        var both = WorkOrderRules.progress(tin.orders(), crush(WorkObjectiveType.CRUSH, 3, "rngtech:raw_lead", 3));
        require(both.fulfilled() != null && both.fulfilled().progress().equals(List.of(4L, 3L)), "a job with several parts is done when every part is");
    }

    private static void itemJobs(List<WorkObjectiveEntry> shipped) {
        var random = new SplittableRandom(5L);
        for (WorkObjectiveEntry entry : shipped) {
            if (!entry.filter().named()) { continue; }
            int[] sizes = new int[WorkObjective.MAX_PARTS + 1];
            for (int i = 0; i < 2_000; i++) {
                int tier = WorkOrder.MIN_TIER + i % WorkOrder.MAX_TIER;
                int cap = WorkOrderFamilies.stageCap(entry.family(), tier);
                WorkObjective job = entry.roll(tier, cap, random, item -> true);
                List<String> named = job.parts().stream().map(WorkPart::item).toList();
                require(named.size() == named.stream().distinct().count(), entry.id() + " never names one item twice");
                require(job.totalTarget() <= entry.goal(tier).max(), entry.id() + " splits its count between its parts");
                for (WorkPart part : job.parts()) {
                    int stage = entry.options().stream().filter(option -> option.item().equals(part.item())).findFirst().orElseThrow().stage();
                    require(stage <= cap, entry.id() + " names " + part.item() + " only where a Stage " + cap + " chassis can run it");
                }
                sizes[job.parts().size()]++;
            }
            for (int size = entry.minParts(); size <= entry.maxParts(); size++) {
                require(sizes[size] > 0, entry.id() + " rolls jobs of " + size + " items");
            }
            if (entry.filter() == WorkFilter.ITEM) {
                require(entry.roll(1, 4, random, item -> false) == null, entry.id() + " rolls nothing when no option has a loaded recipe");
            }
        }
    }

    private static void rewardAmounts() {
        var random = new SplittableRandom(11L);
        boolean always = true;
        int extra = 0;
        int samples = 20_000;
        for (int i = 0; i < samples; i++) {
            always &= WorkOrderRules.scaledAmount(3, 100, random) == 3;
            int amount = WorkOrderRules.scaledAmount(3, 150, random);
            always &= amount == 4 || amount == 5;
            if (amount == 5) { extra++; }
        }
        require(always, "100% Bounty pays the base amount, and 150% of 3 pays 4 or 5");
        require(extra > samples * 0.48 && extra < samples * 0.52, "150% of 3 pays 5 half the time");
        require(WorkOrderRules.scaledAmount(1, 0, random) == 1, "a reward always pays at least one");
        require(WorkOrderRules.scaledAmount(2, 300, random) == 6, "300% Bounty triples the amount");
    }

    /** Every shipped objective's minimum is met by at least one recipe a chassis at the tier's stage can run. */
    private static void reachable(List<WorkObjectiveEntry> shipped) {
        List<Integer> crushLevels = crusherLevels();
        for (WorkObjectiveEntry entry : shipped) {
            for (int tier = WorkOrder.MIN_TIER; tier <= WorkOrder.MAX_TIER; tier++) {
                int minimum = entry.goal(tier).minimum();
                int stage = WorkOrderFamilies.stageCap(entry.family(), tier);
                boolean ok = switch (entry.filter()) {
                    case ANY -> true;
                    case STAGE -> switch (entry.type()) {
                        case CRUSH -> crushLevels.stream().anyMatch(level -> level >= minimum && level <= stage);
                        case ASSEMBLE -> cellStages().stream().anyMatch(cell -> cell >= minimum && cell <= stage);
                        case ALLOY -> alloyRecipes().stream().anyMatch(recipe -> recipe.get("minimum_component_stage").getAsInt() >= minimum
                                && recipe.get("minimum_component_stage").getAsInt() <= stage);
                        default -> minimum <= stage;
                    };
                    case SETTING -> minimum <= 100;
                    case ITEM -> !entry.options(stage, item -> true).isEmpty()
                            && entry.options().stream().allMatch(option -> hasRecipe(entry.type(), option.item()));
                    case STAT -> WorkOrderFamilies.goalStat(entry.family(), MachineStat.bySerializedName(entry.stat()));
                    case STREAK -> WorkOrderFamilies.streaks(entry.family());
                    case GROUP -> !entry.options(stage, item -> false).isEmpty()
                            && entry.options().stream().allMatch(option -> WorkOrderFamilies.group(entry.family(), option.item()));
                };
                require(ok, entry.id() + " Tier " + tier + " is reachable on a Stage " + stage + " chassis");
            }
        }
    }

    static List<WorkObjectiveEntry> shippedObjectives() {
        Path root = projectRoot().resolve("src/main/resources/data/rngtech/work_orders/objectives");
        List<WorkObjectiveEntry> entries = new ArrayList<>();
        for (Path file : jsonFiles(root)) {
            String name = file.getFileName().toString().replace(".json", "");
            entries.add(WorkObjectiveEntry.parse(id(name), read(file)));
        }
        require(entries.stream().anyMatch(entry -> entry.family().equals("crusher"))
                && entries.stream().anyMatch(entry -> entry.family().equals("furnace")), "the Crusher and Furnace ship objectives");
        return entries;
    }

    private static List<Integer> crusherLevels() {
        List<Integer> levels = new ArrayList<>();
        for (Path file : jsonFiles(projectRoot().resolve("src/main/resources/data/rngtech/recipe/crusher"))) {
            JsonObject recipe = read(file);
            if (recipe.has("required_processing_level")) { levels.add(recipe.get("required_processing_level").getAsInt()); }
        }
        return levels;
    }

    private static WorkOrder order(String family, int minStage, int minMasteryLevel) {
        return new WorkOrder(family, 1, single(WorkFilter.STAGE, 3, "", 256), minStage, minMasteryLevel, 100, 0);
    }

    /**
     * Stat goals: each family ships some, their targets roll in hundredths of the stat, a goal tracks the machine's value at
     * each completed job, can fall back, and is met only at or above the target. The tiers' targets come from the level-1
     * processing balance simulation, so they are pinned here until it can run with Mastery again.
     */
    private static void statGoals(List<WorkObjectiveEntry> shipped) {
        List<WorkObjectiveEntry> goals = shipped.stream().filter(entry -> entry.filter() == WorkFilter.STAT).toList();
        require(goals.stream().anyMatch(entry -> entry.family().equals("crusher")) && goals.stream().anyMatch(entry -> entry.family().equals("furnace")),
                "the Crusher and Furnace ship stat goals");
        WorkObjectiveEntry speed = goals.stream().filter(entry -> entry.family().equals("crusher") && entry.stat().equals("processing_speed")).findFirst().orElseThrow();
        require(speed.goal(1).min() == 240 && speed.goal(1).max() == 310 && speed.goal(1).step() == 10, "a stat goal's target is kept in hundredths");
        WorkObjective objective = speed.roll(1, 4, new SplittableRandom(3L), item -> true);
        WorkPart part = objective.parts().getFirst();
        require(part.statGoal() && part.item().equals("processing_speed") && part.target() >= 240 && part.target() <= 310 && part.target() % 10 == 0,
                "a stat goal rolls its target on its step: " + part);
        WorkReport crush = new WorkReport(WorkObjectiveType.CRUSH, 8, 0, "", 50);
        require(!part.counts(crush), "a stat goal counts no work");

        var orders = MachineWorkOrders.EMPTY.withLoaded(loaded(new WorkOrder("crusher", 1, objective, 4, 0, 100, 0)));
        require(WorkOrderRules.reachStats(orders, stat -> 0) == orders, "a stat at 0 changes nothing");
        var partway = WorkOrderRules.reachStats(orders, stat -> stat.equals("processing_speed") ? 1.5 : 99);
        require(partway.activeOrder().totalProgress() == 150 && !partway.activeOrder().fulfilled(), "a goal shows the machine's value below the target");
        var lower = WorkOrderRules.reachStats(partway, stat -> 1.2);
        require(lower.activeOrder().totalProgress() == 120, "a goal's progress falls when the stat falls");
        var met = WorkOrderRules.reachStats(lower, stat -> 9.0);
        require(met.activeOrder() == null && met.orders().getFirst().fulfilled() && met.orders().getFirst().totalProgress() == part.target(),
                "a goal at or above its target is met, and the chip leaves the active slot");
        require(WorkOrderRules.reachStats(met.withActive(0), stat -> 0) .equals(met.withActive(0)), "a met goal never falls back");
        try {
            WorkObjectiveEntry.parse(id("bad_goal"), JsonParser.parseString(
                    "{\"family\": \"crusher\", \"type\": \"crush\", \"filter\": \"stat\", \"stat\": \"heat_transfer\", \"tiers\": [{\"target\": 2}, {\"target\": 2}, {\"target\": 2}, {\"target\": 2}, {\"target\": 2}]}").getAsJsonObject());
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("a Crusher stat goal named a stat the Crusher does not read");
    }

    /** Streak jobs: they count like any work, a failure sets an unfinished streak back to 0, and the chip keeps its best streak. */
    private static void streaks() {
        var objective = new WorkObjective(id("streak"), new WorkPart(WorkObjectiveType.PRESS, WorkFilter.STREAK, 0, "", 10));
        var orders = MachineWorkOrders.EMPTY.withLoaded(loaded(new WorkOrder("metal_press", 1, objective, 4, 0, 100, 0)));
        var six = WorkOrderRules.progress(orders, crush(WorkObjectiveType.PRESS, 0, "rngtech:iron_plate", 6)).orders();
        require(six.activeOrder().totalProgress() == 6, "a streak counts any work of its verb");
        var broken = WorkOrderRules.breakStreaks(six);
        require(broken.activeOrder().totalProgress() == 0 && broken.activeOrder().bestStreak() == 6, "a failure breaks the streak and keeps the best");
        require(WorkOrderRules.breakStreaks(broken) == broken, "breaking an empty streak changes nothing");
        var four = WorkOrderRules.progress(broken, crush(WorkObjectiveType.PRESS, 0, "", 4)).orders();
        require(four.activeOrder().totalProgress() == 4 && four.activeOrder().bestStreak() == 6, "a new streak starts from 0 under the best");
        var done = WorkOrderRules.progress(four, crush(WorkObjectiveType.PRESS, 0, "", 6)).orders();
        require(done.orders().getFirst().fulfilled() && WorkOrderRules.breakStreaks(done) == done, "a finished streak cannot be broken");
        require(WorkOrderFamilies.streaks("metal_press") && !WorkOrderFamilies.streaks("crusher"), "only machines that report failures roll streaks");

        var calibrated = new WorkObjective(id("calibrated"), new WorkPart(WorkObjectiveType.CALIBRATE, WorkFilter.STREAK, 80, "", 5));
        var run = MachineWorkOrders.EMPTY.withLoaded(loaded(new WorkOrder("resonance_calibrator", 1, calibrated, 4, 0, 100, 0)));
        var three = WorkOrderRules.progress(run, new WorkReport(WorkObjectiveType.CALIBRATE, 2, 85, "", 3)).orders();
        require(three.activeOrder().totalProgress() == 3, "calibrations at the streak's stability count");
        var low = WorkOrderRules.progress(three, new WorkReport(WorkObjectiveType.CALIBRATE, 2, 79, "", 1)).orders();
        require(low.activeOrder().totalProgress() == 0 && low.activeOrder().bestStreak() == 3, "a calibration below the streak's stability breaks it");
        require(WorkOrderRules.progress(three, new WorkReport(WorkObjectiveType.CRUSH, 2, 0, "", 1)).orders() == three, "other work leaves a streak alone");

        var stable = new WorkObjective(id("stable"), new WorkPart(WorkObjectiveType.CALIBRATE, WorkFilter.SETTING, 70, "", 10));
        var fine = new MachineTraits(Rarity.MAGIC, 0, List.of(WorkOrderConditions.get(WorkOrderConditions.FINE_TOLERANCE).definition()
                .roll(1, net.minecraft.util.RandomSource.create(1L))), List.of());
        var plain = MachineWorkOrders.EMPTY.withLoaded(loaded(new WorkOrder("resonance_calibrator", 1, stable, 4, 0, 100, 0)));
        var tolerant = MachineWorkOrders.EMPTY.withLoaded(new LoadedWorkOrder(new WorkOrder("resonance_calibrator", 1, stable, 4, 0, 100, 0), fine));
        var good = new WorkReport(WorkObjectiveType.CALIBRATE, 2, 75, "", 4);
        var poor = new WorkReport(WorkObjectiveType.CALIBRATE, 2, 60, "", 1);
        require(WorkOrderRules.progress(WorkOrderRules.progress(plain, good).orders(), poor).orders().activeOrder().totalProgress() == 4,
                "a poor calibration only misses a stability job");
        require(WorkOrderRules.progress(WorkOrderRules.progress(tolerant, good).orders(), poor).orders().activeOrder().totalProgress() == 0,
                "under Fine Tolerance a poor calibration resets a stability job");
    }

    /** Group jobs count work of one kind, such as Metal Press plates of any metal or one calibration family. */
    private static void groups() {
        var plates = new WorkObjective(id("plates"), List.of(new WorkPart(WorkObjectiveType.PRESS, WorkFilter.GROUP, 0, "plate", 4),
                new WorkPart(WorkObjectiveType.PRESS, WorkFilter.GROUP, 0, "gear", 2)), 0);
        var orders = MachineWorkOrders.EMPTY.withLoaded(loaded(new WorkOrder("metal_press", 1, plates, 4, 0, 100, 0)));
        var iron = WorkOrderRules.progress(orders, new WorkReport(WorkObjectiveType.PRESS, 1, 0, "rngtech:iron_plate", 3, "plate")).orders();
        var gold = WorkOrderRules.progress(iron, new WorkReport(WorkObjectiveType.PRESS, 1, 0, "rngtech:gold_plate", 3, "plate")).orders();
        require(gold.activeOrder().progress().equals(List.of(4L, 0L)), "any metal's plates count toward a plate part, and stop at its target");
        require(WorkOrderRules.progress(gold, new WorkReport(WorkObjectiveType.PRESS, 1, 0, "rngtech:iron_casing", 2, "casing")).orders() == gold,
                "a group no part names does not count");
        var gears = WorkOrderRules.progress(gold, new WorkReport(WorkObjectiveType.PRESS, 1, 0, "rngtech:iron_gear", 2, "gear")).orders();
        require(gears.orders().getFirst().fulfilled(), "a group job is done when every group is");
        require(WorkOrderFamilies.group("resonance_calibrator", "logic") && !WorkOrderFamilies.group("resonance_calibrator", "plate")
                && !WorkOrderFamilies.group("crusher", "plate"), "each family's group jobs name only the groups its machines report");
        require(WorkOrderFamilies.allGroups().size() == 10 && WorkOrderFamilies.allGroups().indexOf("logic") >= 0, "every group syncs by its index");
    }

    /** Rush deadlines: half the tier's target time, a rounded-up bonus, and expiry that spares finished chips. */
    private static void rush() {
        require(WorkOrder.rushTicks(1) == 15 * 60 * 20 / 2 && WorkOrder.rushTicks(5) == 30 * 60 * 20 / 2, "Rush is half the tier's target time");
        require(WorkOrder.rushAmount(4) == 6 && WorkOrder.rushAmount(3) == 5 && WorkOrder.rushAmount(1) == 2 && WorkOrder.rushAmount(100) == 150,
                "Rush adds half again, rounded up");
        var order = new WorkOrder("crusher", 1, single(WorkFilter.ANY, 0, "", 10), 4, 0, 100, 0);
        var running = loaded(order).withRush(1_000);
        var finished = loaded(order).withPartProgress(0, 10).withRush(1_000);
        var plain = loaded(order);
        require(running.rushed() && !running.expired(999) && running.expired(1_000) && !finished.expired(5_000) && !plain.expired(5_000),
                "a Rush chip expires at its deadline unless its job is done");
        var orders = MachineWorkOrders.EMPTY.withLoaded(plain).withLoaded(running).withLoaded(finished).withActive(1);
        require(orders.withoutExpired(999) == orders, "nothing expires before the deadline");
        var after = orders.withoutExpired(1_000);
        require(after.orders().size() == 2 && after.orders().get(0).equals(plain) && after.orders().get(1).equals(finished)
                && after.active() == MachineWorkOrders.NONE, "an expired chip is lost, a finished one stays, and none is left active");
    }

    private static LoadedWorkOrder loaded(WorkOrder order) {
        return new LoadedWorkOrder(order, MachineTraits.EMPTY);
    }

    private static WorkObjectiveEntry entry(String id, String family, int weight) {
        List<WorkObjectiveEntry.Goal> goals = new ArrayList<>();
        for (int tier = 1; tier <= WorkOrder.MAX_TIER; tier++) { goals.add(new WorkObjectiveEntry.Goal(tier * 10L, tier)); }
        return new WorkObjectiveEntry(ResourceLocation.parse(id), family, WorkObjectiveType.CRUSH, WorkFilter.STAGE, weight, 1, 1, List.of(), goals);
    }

    private static WorkObjective single(WorkFilter filter, int minimum, String item, long target) {
        return new WorkObjective(id("test"), new WorkPart(WorkObjectiveType.CRUSH, filter, minimum, item, target));
    }

    private static WorkReport crush(WorkObjectiveType type, int stage, String item, long amount) {
        return new WorkReport(type, stage, stage, item, amount);
    }

    /** Whether an item option has its recipe: a Crusher recipe named after the raw item, or a Furnace smelt from the crushed item. */
    private static boolean hasRecipe(WorkObjectiveType type, String item) {
        String path = item.substring(item.indexOf(':') + 1);
        Path recipes = projectRoot().resolve("src/main/resources/data/rngtech/recipe");
        return switch (type) {
            case CRUSH -> Files.exists(recipes.resolve("crusher/" + path + ".json"));
            case SMELT -> Files.exists(recipes.resolve("furnace/metals/" + path.replace("crushed_", "") + "_from_crushed.json"));
            case ASSEMBLE -> assemblyOutputs().contains(item);
            case ALLOY -> alloyRecipes().stream().anyMatch(recipe -> recipe.getAsJsonObject("result").get("id").getAsString().equals(item));
            case PRESS -> Files.exists(recipes.resolve("metal_press/" + path + ".json"));
            case MELT -> jsonFiles(recipes.resolve("melter")).stream()
                    .anyMatch(file -> read(file).getAsJsonObject("fluid_output").get("id").getAsString().equals(item));
            case CALIBRATE -> jsonFiles(recipes).stream().filter(file -> file.getFileName().toString().startsWith("calibration_"))
                    .anyMatch(file -> read(file).getAsJsonObject("result").getAsJsonObject("stack").get("id").getAsString().equals(item));
        };
    }

    private static List<JsonObject> alloyRecipes() {
        return jsonFiles(projectRoot().resolve("src/main/resources/data/rngtech/recipe/alloy_furnace")).stream().map(WorkOrderRuleChecks::read).toList();
    }

    /** What the Component Assembler's shipped recipes make, by item id. */
    private static List<String> assemblyOutputs() {
        return jsonFiles(projectRoot().resolve("src/main/resources/data/rngtech/recipe/battery_assembly")).stream()
                .map(file -> read(file).getAsJsonObject("result").get("id").getAsString()).toList();
    }

    /** The stages of the Battery Cells the Component Assembler makes, from each cell's material. */
    private static List<Integer> cellStages() {
        return assemblyOutputs().stream().filter(item -> item.endsWith("_battery_cell"))
                .map(item -> BatteryCellMaterial.valueOf(item.substring(item.indexOf(':') + 1, item.length() - "_battery_cell".length()).toUpperCase(Locale.ROOT)).stage())
                .toList();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("rngtech", path);
    }

    private static List<Path> jsonFiles(Path root) {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(file -> file.toString().endsWith(".json")).sorted().toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + root, exception);
        }
    }

    private static JsonObject read(Path file) {
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + file, exception);
        }
    }

    private static Path projectRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("settings.gradle"))) { return dir; }
        }
        throw new IllegalStateException("No project root above " + Path.of("").toAbsolutePath());
    }

    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
