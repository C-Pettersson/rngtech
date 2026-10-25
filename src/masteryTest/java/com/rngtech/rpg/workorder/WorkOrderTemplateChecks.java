package com.rngtech.rpg.workorder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/** Job templates: parsing any subset of fields, rolling the rest, and pooling them in the terminal's listing. */
public final class WorkOrderTemplateChecks {
    private static int checks;

    private WorkOrderTemplateChecks() { }

    public static int run() {
        checks = 0;
        List<WorkObjectiveEntry> shipped = WorkOrderRuleChecks.shippedObjectives();
        Map<ResourceLocation, WorkObjectiveEntry> byId = new HashMap<>();
        shipped.forEach(entry -> byId.put(entry.id(), entry));
        partial(byId);
        full(byId);
        inline(byId);
        invalid();
        pools(shipped);
        saves();
        return checks;
    }

    /** A template that names only an objective takes its family and rolls everything else the way a random job does. */
    private static void partial(Map<ResourceLocation, WorkObjectiveEntry> byId) {
        WorkOrderTemplate template = parse("partial", "{\"objective\": \"rngtech:crusher_stage\"}");
        require(!template.pooled() && template.tier() == 0 && template.family(byId).equals("crusher"), "an unset weight keeps a template out of the pool");
        for (int tier = WorkOrder.MIN_TIER; tier <= WorkOrder.MAX_TIER; tier++) {
            WorkOrder order = template.roll(tier, byId, new SplittableRandom(tier), item -> true);
            WorkObjectiveEntry entry = byId.get(ResourceLocation.parse("rngtech:crusher_stage"));
            long count = order.objective().totalTarget();
            require(order.tier() == tier && order.family().equals("crusher") && order.template().equals("rngtech:partial")
                    && order.minStage() == Math.min(WorkOrder.tierStage(tier), WorkOrderFamilies.topStage("crusher"))
                    && count >= entry.goal(tier).min() && count <= entry.goal(tier).max()
                    && order.bounty() == WorkOrder.BASE_BOUNTY + order.objective().bonus() && order.minMasteryLevel() == 0,
                    "a partial template rolls its unset fields like a random job at tier " + tier + ": " + order);
        }
    }

    /** Every fixed field is kept, and a fixed tier wins over the tier asked for. */
    private static void full(Map<ResourceLocation, WorkObjectiveEntry> byId) {
        WorkOrderTemplate template = parse("full", """
                {"family": "furnace", "tier": 4, "objective": {"type": "smelt", "parts": [{"filter": "stage", "min": 6, "count": 300}]},
                 "min_stage": 7, "min_mastery_level": 40, "bounty": 175, "prestige": 2, "name": "Foundry Contract",
                 "rewards": [{"item": "rngtech:chaos_crystal", "count": 5}], "reward_table": "mypack:work_orders/foundry", "weight": 3}""");
        WorkOrder order = template.roll(1, byId, new SplittableRandom(1L), item -> true);
        WorkPart part = order.objective().parts().getFirst();
        require(order.tier() == 4 && order.family().equals("furnace") && order.minStage() == 7 && order.minMasteryLevel() == 40
                && order.bounty() == 175 && order.prestige() == 2 && order.name().equals("Foundry Contract")
                && order.rewardTable().equals("mypack:work_orders/foundry") && template.pooled() && template.weight() == 3,
                "a full template keeps every fixed field: " + order);
        require(part.type().equals("smelt") && part.filter().equals("stage") && part.minimum() == 6 && part.target() == 300,
                "an inline part keeps its type, filter, minimum, and fixed count");
        require(template.rewards().equals(List.of(new WorkOrderTemplate.Reward("rngtech:chaos_crystal", 5))), "fixed rewards are read");
        require(template.fitsTier(4) && !template.fitsTier(3), "a fixed tier fits only its own listings");
    }

    /** Inline item parts with ranges roll within them on their step and earn the highest band any part lands in. */
    private static void inline(Map<ResourceLocation, WorkObjectiveEntry> byId) {
        WorkOrderTemplate template = parse("inline", """
                {"family": "crusher", "objective": {"type": "crush", "parts": [
                    {"item": "rngtech:raw_tin", "count": {"min": 64, "max": 128, "step": 16}},
                    {"item": "rngtech:raw_lead", "count": 32}]}}""");
        Set<Integer> bonuses = new HashSet<>();
        for (int seed = 0; seed < 200; seed++) {
            WorkOrder order = template.roll(2, byId, new SplittableRandom(seed), item -> true);
            WorkPart tin = order.objective().parts().get(0);
            WorkPart lead = order.objective().parts().get(1);
            require(tin.filter().equals("item") && tin.item().equals("rngtech:raw_tin") && tin.target() >= 64 && tin.target() <= 128
                    && tin.target() % 16 == 0 && lead.target() == 32, "inline parts roll within their range on their step");
            require(order.bounty() == WorkOrder.BASE_BOUNTY + order.objective().bonus(), "an inline job adds its band bonus to Bounty");
            bonuses.add(order.objective().bonus());
        }
        require(bonuses.equals(Set.of(0, 10, 20)), "inline ranges reach every band: " + bonuses);
    }

    private static void invalid() {
        rejects("{\"tier\": 2}", "a template without an objective");
        rejects("{\"objective\": {\"type\": \"crush\", \"parts\": [{\"count\": 5}]}}", "inline parts without a family");
        rejects("{\"family\": \"loom\", \"objective\": {\"type\": \"crush\", \"parts\": [{\"count\": 5}]}}", "an unknown family");
        rejects("{\"tier\": 6, \"objective\": \"rngtech:crusher_any\"}", "a tier above V");
        rejects("{\"family\": \"crusher\", \"objective\": {\"type\": \"crush\", \"parts\": [{\"filter\": \"item\", \"count\": 5}]}}", "an item part without an item");
        rejects("{\"family\": \"crusher\", \"objective\": {\"type\": \"weld\", \"parts\": [{\"count\": 5}]}}", "an unknown job type");
        rejects("{\"objective\": \"rngtech:crusher_any\", \"bounty\": -5}", "a negative Bounty");
        rejects("{\"objective\": \"rngtech:crusher_any\", \"rewards\": [{\"item\": \"rngtech:chaos_crystal\", \"count\": 0}]}", "an empty reward");
        WorkOrderTemplate missing = parse("missing", "{\"objective\": \"rngtech:no_such_objective\", \"weight\": 5}");
        require(missing.roll(1, Map.of(), new SplittableRandom(1L), item -> true) == null && !missing.canRoll(1, Map.of(), item -> true),
                "a template naming an objective that is not loaded does not roll");
    }

    /** The terminal's listing: random objectives only while random jobs are on, plus pooled templates by weight. */
    private static void pools(List<WorkObjectiveEntry> shipped) {
        WorkOrderTemplate pooledThree = parse("pooled_three", "{\"objective\": \"rngtech:furnace_heat\", \"tier\": 3, \"weight\": 2}");
        WorkOrderTemplate questOnly = parse("quest_only", "{\"objective\": \"rngtech:crusher_any\"}");
        require(WorkOrderRules.roll(shipped, false, List.of(questOnly), 0, new SplittableRandom(1L), item -> true) == null,
                "with random jobs off and no pooled template, nothing lists");
        Set<String> sources = new HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            WorkOrder off = WorkOrderRules.roll(shipped, false, List.of(pooledThree, questOnly), 0, new SplittableRandom(seed), item -> true);
            require(off != null && off.tier() == 3 && off.template().equals("rngtech:pooled_three"),
                    "with random jobs off, only pooled templates list, at the tiers they fit: " + off);
            require(WorkOrderRules.roll(shipped, false, List.of(pooledThree), 2, new SplittableRandom(seed), item -> true) == null,
                    "a listing of a tier no pooled template fits stays empty with random jobs off");
            WorkOrder on = WorkOrderRules.roll(shipped, true, List.of(pooledThree, questOnly), 3, new SplittableRandom(seed), item -> true);
            sources.add(on.template().isEmpty() ? "random" : on.template());
        }
        require(sources.equals(Set.of("random", "rngtech:pooled_three")), "with random jobs on, a listing draws random jobs and pooled templates: " + sources);
        WorkOrder plain = WorkOrderRules.roll(shipped, 2, new SplittableRandom(7L), item -> true);
        require(plain.template().isEmpty() && plain.name().isEmpty() && plain.rewardTable().isEmpty(), "a random job has no template, name, or table");
    }

    /** A template job's id, name, and table survive a save, and an older save without them still loads. */
    private static void saves() {
        WorkObjective objective = new WorkObjective(ResourceLocation.parse("rngtech:saves"), new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.ANY, 0, "", 64));
        WorkOrder order = new WorkOrder("crusher", 2, objective, 5, 0, 120, 1).fromTemplate("mypack:contract", "Contract", "mypack:table");
        WorkOrder loaded = WorkOrder.CODEC.parse(JsonOps.INSTANCE, WorkOrder.CODEC.encodeStart(JsonOps.INSTANCE, order).getOrThrow()).getOrThrow();
        require(loaded.equals(order), "a template job round-trips through its save");
        JsonObject old = WorkOrder.CODEC.encodeStart(JsonOps.INSTANCE, new WorkOrder("crusher", 2, objective, 5, 0, 120, 1)).getOrThrow().getAsJsonObject();
        old.remove("template");
        old.remove("name");
        old.remove("reward_table");
        require(WorkOrder.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow().template().isEmpty(), "a save without template fields loads");
        FulfilledWorkOrder done = new FulfilledWorkOrder(2, "crusher", "", 120, 1, null, "mypack:table");
        require(FulfilledWorkOrder.CODEC.parse(JsonOps.INSTANCE, FulfilledWorkOrder.CODEC.encodeStart(JsonOps.INSTANCE, done).getOrThrow()).getOrThrow()
                .rewardTable().equals("mypack:table"), "a Completed Job Chip keeps its reward table");
    }

    private static WorkOrderTemplate parse(String path, String json) {
        return WorkOrderTemplate.parse(ResourceLocation.fromNamespaceAndPath("rngtech", path), JsonParser.parseString(json).getAsJsonObject());
    }

    private static void rejects(String json, String label) {
        try {
            parse("invalid", json);
        } catch (RuntimeException expected) {
            checks++;
            return;
        }
        throw new AssertionError("accepted " + label);
    }

    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
