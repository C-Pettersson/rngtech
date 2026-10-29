package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.ModifierValueRange;
import com.rngtech.rpg.Rarity;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Job Chip conditions: rarity odds and counts, family and job filtering, rewards, and that every pressure condition is a cost. */
public final class WorkOrderConditionChecks {
    private static int checks;

    private WorkOrderConditionChecks() { }

    public static int run() {
        checks = 0;
        rarityOdds();
        rolls();
        costsOnly();
        deepOre();
        handFeeding();
        templateRarity();
        return checks;
    }

    private static void rarityOdds() {
        Map<Rarity, Integer> counts = new EnumMap<>(Rarity.class);
        RandomSource random = RandomSource.create(11L);
        int total = 20_000;
        for (int roll = 0; roll < total; roll++) {
            counts.merge(WorkOrderConditions.rollRarity(random), 1, Integer::sum);
        }
        require(near(counts.getOrDefault(Rarity.NORMAL, 0), total, 0.50) && near(counts.getOrDefault(Rarity.MAGIC, 0), total, 0.35)
                && near(counts.getOrDefault(Rarity.RARE, 0), total, 0.15) && !counts.containsKey(Rarity.UNIQUE),
                "chips roll Normal, Magic, and Rare about 50/35/15: " + counts);
    }

    private static void rolls() {
        WorkOrder crusherStage = order("crusher", WorkFilter.STAGE);
        WorkOrder crusherAny = order("crusher", WorkFilter.ANY);
        WorkOrder furnaceStage = order("furnace", WorkFilter.STAGE);
        Set<String> seenCrusher = new HashSet<>();
        Set<String> seenFurnace = new HashSet<>();
        for (int seed = 0; seed < 2_000; seed++) {
            for (WorkOrder order : List.of(crusherStage, crusherAny, furnaceStage)) {
                for (Rarity rarity : List.of(Rarity.NORMAL, Rarity.MAGIC, Rarity.RARE)) {
                    WorkOrderConditions.Rolled rolled = WorkOrderConditions.roll(order, rarity, RandomSource.create(seed * 31L + rarity.ordinal()));
                    List<MachineModifier> modifiers = rolled.traits().modifiers();
                    int count = modifiers.size();
                    long prefixes = modifiers.stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count();
                    long suffixes = modifiers.stream().filter(modifier -> modifier.slot() == ModifierSlot.SUFFIX).count();
                    int limit = rarity == Rarity.RARE ? 3 : 1;
                    require(rolled.traits().rarity() == rarity && prefixes <= limit && suffixes <= limit, "a chip keeps its rarity's prefix and suffix limits");
                    require(switch (rarity) {
                        case NORMAL -> count == 0;
                        case MAGIC -> count >= 1 && count <= 2;
                        default -> count >= 3 && count <= 4;
                    }, rarity + " rolls its condition count: " + count + " on " + order.family());
                    require(modifiers.stream().map(MachineModifier::modGroup).distinct().count() == count, "conditions never repeat a group");
                    int bounty = order.bounty();
                    int prestige = order.prestige();
                    for (MachineModifier modifier : modifiers) {
                        WorkOrderConditions.Condition condition = WorkOrderConditions.get(modifier.affixId());
                        require(condition != null && (condition.families().isEmpty() || condition.families().contains(order.family())),
                                "only conditions the family reads roll: " + modifier.affixId() + " on " + order.family());
                        require(!WorkOrderConditions.DEEP_ORE.equals(modifier.affixId()) || order != crusherAny, "Deep Ore rolls only on stage jobs");
                        bounty += condition.bounty(modifier.tier());
                        prestige += condition.prestige();
                        (order.family().equals("crusher") ? seenCrusher : seenFurnace).add(modifier.affixId());
                    }
                    require(rolled.order().bounty() == bounty && rolled.order().prestige() == prestige,
                            "each condition adds its Bounty and Prestige to the job");
                }
            }
        }
        require(seenCrusher.equals(Set.of(WorkOrderConditions.STARVED_FEED, WorkOrderConditions.POWER_SURCHARGE, WorkOrderConditions.SHORT_CREW,
                WorkOrderConditions.HAND_FEEDING, WorkOrderConditions.JAMMED_LINE, WorkOrderConditions.DEEP_ORE, WorkOrderConditions.SHORT_FUSE)),
                "every Crusher condition rolls: " + seenCrusher);
        require(seenFurnace.equals(Set.of(WorkOrderConditions.STARVED_FEED, WorkOrderConditions.POWER_SURCHARGE, WorkOrderConditions.SHORT_CREW,
                WorkOrderConditions.COLD_START, WorkOrderConditions.HAND_FEEDING, WorkOrderConditions.BRITTLE_LINING, WorkOrderConditions.DEEP_ORE,
                WorkOrderConditions.SHORT_FUSE)),
                "every Furnace condition rolls: " + seenFurnace);
        require(WorkOrderConditions.profile("crusher").definitions().size() == 7 && WorkOrderConditions.profile("furnace").definitions().size() == 8,
                "each family's profile holds the conditions it reads");
    }

    /**
     * No pressure condition can raise a stat's benefit; rule conditions change no stat at all. Short Fuse's value is the
     * share it cuts from the Rush clock, and like every rule condition it is never applied to the machine's stats.
     */
    private static void costsOnly() {
        for (WorkOrderConditions.Condition condition : WorkOrderConditions.all()) {
            for (ModifierValueRange range : condition.definition().tierRanges()) {
                MachineStat stat = condition.definition().stat();
                ModifierOperation operation = condition.definition().operation();
                boolean cost;
                if (!condition.pressure()) {
                    cost = WorkOrderConditions.SHORT_FUSE.equals(condition.id())
                            ? range.min() > 0 && range.max() < 100
                            : operation == ModifierOperation.ADD && range.min() == 0 && range.max() == 0;
                } else if (operation == ModifierOperation.LESS) {
                    cost = range.max() < 1;
                } else if (stat == MachineStat.ENERGY_USAGE) {
                    cost = operation == ModifierOperation.INCREASED_PERCENT && range.min() > 0;
                } else if (operation == ModifierOperation.DECREASED_PERCENT) {
                    cost = range.min() > 0;
                } else {
                    cost = operation == ModifierOperation.ADD && range.max() < 0;
                }
                require(cost, condition.id() + " is a cost, never a gain");
            }
            require(condition.definition().slot() == (condition.pressure() && !WorkOrderConditions.HAND_FEEDING.equals(condition.id()) ? ModifierSlot.PREFIX : ModifierSlot.SUFFIX),
                    condition.id() + ": pressure conditions are prefixes and rule conditions suffixes");
        }
    }

    /**
     * Deep Ore raises a stage part's minimum to the next stage with work that the tier's chassis runs, skipping stages no
     * recipe reaches, and costs nothing when none is left, such as a Tier V Crusher job at Processing Level 8.
     */
    private static void deepOre() {
        WorkPart part = new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.STAGE, 4, "", 100);
        MachineTraits deep = new MachineTraits(Rarity.MAGIC, 0, List.of(WorkOrderConditions.get(WorkOrderConditions.DEEP_ORE).definition().roll(1, RandomSource.create(1L))), List.of());
        WorkOrder order = new WorkOrder("crusher", 2, new WorkObjective(ResourceLocation.parse("rngtech:deep"), part), 5, 0, 100, 0);
        WorkReport atFour = new WorkReport(WorkObjectiveType.CRUSH, 4, 0, "", 1);
        WorkReport atFive = new WorkReport(WorkObjectiveType.CRUSH, 5, 0, "", 1);
        require(WorkOrderConditions.counts(order, part, atFour, MachineTraits.EMPTY) && !WorkOrderConditions.counts(order, part, atFour, deep)
                && WorkOrderConditions.counts(order, part, atFive, deep) && WorkOrderConditions.minimum(order, part, deep) == 5,
                "Deep Ore counts only work one level above the job's minimum");
        MachineWorkOrders orders = MachineWorkOrders.EMPTY.withLoaded(new LoadedWorkOrder(order, deep)).withActive(0);
        require(WorkOrderRules.progress(orders, atFour).orders() == orders && WorkOrderRules.progress(orders, atFive).orders().activeOrder().totalProgress() == 1,
                "the machine's counting follows Deep Ore");

        WorkPart top = new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.STAGE, 8, "", 100);
        WorkOrder topJob = new WorkOrder("crusher", 5, new WorkObjective(ResourceLocation.parse("rngtech:deep_top"), top), 8, 0, 100, 0);
        require(WorkOrderConditions.minimum(topJob, top, deep) == 8 && WorkOrderConditions.counts(topJob, top, new WorkReport(WorkObjectiveType.CRUSH, 8, 0, "", 1), deep),
                "Deep Ore costs nothing on a Tier V Crusher job at Processing Level 8, the top level");
        require(WorkOrderFamilies.deepMinimum("crusher", 4, 6) == 6 && WorkOrderFamilies.deepMinimum("crusher", 5, 6) == 8
                && WorkOrderFamilies.deepMinimum("alloy_furnace", 3, 4) == 6, "Deep Ore skips stages no recipe reaches");
    }

    /**
     * of Hand Feeding rolls on every family whose machine rolls Instant Process Chance, and sets that chance to 0 through
     * the machine's stats. The Forestry Companion has no Instant Process, so it never rolls there, and the Melter takes no
     * Cold Start.
     */
    private static void handFeeding() {
        WorkOrderConditions.Condition feeding = WorkOrderConditions.get(WorkOrderConditions.HAND_FEEDING);
        require(feeding.families().equals(Set.of("crusher", "furnace", "alloy_furnace", "metal_press", "melter", "resonance_calibrator",
                "component_assembler", "component_recycler")), "of Hand Feeding rolls on every machine with Instant Process: " + feeding.families());
        require(!feeding.families().contains("forestry"), "the Forestry Companion has no Instant Process to stop");
        require(!WorkOrderConditions.get(WorkOrderConditions.COLD_START).families().contains("melter"), "the Melter takes no Cold Start");
        MachineTraits traits = new MachineTraits(Rarity.MAGIC, 0, List.of(feeding.definition().roll(1, RandomSource.create(4L))), List.of());
        for (String family : feeding.families()) {
            require(WorkOrderConditions.profile(family).definitions().contains(feeding.definition()), family + "'s profile holds of Hand Feeding");
            WorkOrder order = new WorkOrder(family, 1, new WorkObjective(ResourceLocation.parse("rngtech:" + family + "_any"),
                    new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.ANY, 0, "", 100)), 4, 0, WorkOrder.BASE_BOUNTY, 0);
            MachineStatAccumulator stats = new MachineStatAccumulator();
            stats.apply(Component.literal("Gear"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.INSTANT_PROCESS_CHANCE, ModifierOperation.ADD, 25));
            WorkOrderConditions.apply(stats, new LoadedWorkOrder(order, traits));
            require(stats.value(MachineStat.INSTANT_PROCESS_CHANCE) == 0, "of Hand Feeding stops Instant Process on " + family);
        }
    }

    private static void templateRarity() {
        WorkOrderTemplate rare = WorkOrderTemplate.parse(ResourceLocation.parse("rngtech:rare"),
                JsonParser.parseString("{\"objective\": \"rngtech:crusher_any\", \"rarity\": \"rare\"}").getAsJsonObject());
        require(rare.rarity() == Rarity.RARE, "a template can fix its rarity");
        try {
            WorkOrderTemplate.parse(ResourceLocation.parse("rngtech:unique"),
                    JsonParser.parseString("{\"objective\": \"rngtech:crusher_any\", \"rarity\": \"unique\"}").getAsJsonObject());
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("a Unique template was accepted");
    }

    private static WorkOrder order(String family, WorkFilter filter) {
        WorkObjective objective = new WorkObjective(ResourceLocation.parse("rngtech:" + family + "_" + filter.id()),
                new WorkPart(family.equals("crusher") ? WorkObjectiveType.CRUSH : WorkObjectiveType.SMELT, filter, filter == WorkFilter.STAGE ? 4 : 0, "", 100));
        return new WorkOrder(family, 2, objective, 5, 0, WorkOrder.BASE_BOUNTY, 0);
    }

    private static boolean near(int count, int total, double share) {
        return Math.abs(count / (double) total - share) < 0.02;
    }

    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
