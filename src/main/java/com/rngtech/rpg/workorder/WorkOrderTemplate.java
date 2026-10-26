package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.Rarity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * A pack maker's job from {@code data/<namespace>/work_orders/templates/}. It sets any of the job's fields and leaves the
 * rest to roll: the objective (an objective id, or inline parts with fixed counts or ranges), the family, tier, chassis
 * stage, Mastery level, Bounty, Prestige, a display name, a rarity, fixed rewards, and a reward table. A positive {@code weight}
 * puts it in the Dispatch Terminal's listing beside the objectives, on the same weight scale; 0 keeps it for quests and
 * commands only. Unset numbers are {@link #UNSET}, an unset tier is 0, an unset rarity is null, and unset text is "".
 * Conditions always roll; a rarity fixes how many.
 */
public record WorkOrderTemplate(ResourceLocation id, String family, int tier, ResourceLocation objectiveId, WorkObjectiveType type,
        List<PartSpec> parts, int minStage, int minMasteryLevel, int bounty, int prestige, String name, Rarity rarity, List<Reward> rewards,
        String rewardTable, int weight) {
    public static final int UNSET = -1;

    /** One inline part: its filter, the filter's minimum, the item an item part names, and its count. */
    public record PartSpec(WorkFilter filter, String item, WorkObjectiveEntry.Goal count) {
        public PartSpec {
            item = item == null ? "" : item;
            if (filter == WorkFilter.ITEM && item.isEmpty()) { throw new IllegalArgumentException("an item part needs an item"); }
        }
    }

    /** A fixed reward stack, by item id. */
    public record Reward(String item, int count) {
        public Reward {
            if (item == null || item.isEmpty() || count <= 0) { throw new IllegalArgumentException("a reward needs an item and a count above 0"); }
        }
    }

    public WorkOrderTemplate {
        family = family == null ? "" : family;
        parts = List.copyOf(parts);
        rewards = List.copyOf(rewards);
        name = name == null ? "" : name;
        rewardTable = rewardTable == null ? "" : rewardTable;
        if (tier != 0 && (tier < WorkOrder.MIN_TIER || tier > WorkOrder.MAX_TIER)) { throw new IllegalArgumentException(id + ": tier must be 1 to 5"); }
        if (objectiveId == null && parts.isEmpty()) { throw new IllegalArgumentException(id + ": a template needs an objective"); }
        if (objectiveId == null && (type == null || family.isEmpty())) { throw new IllegalArgumentException(id + ": inline parts need a type and a family"); }
        if (parts.size() > WorkObjective.MAX_PARTS) { throw new IllegalArgumentException(id + ": a job has at most three parts"); }
        if (!family.isEmpty() && !WorkOrderFamilies.known(family)) { throw new IllegalArgumentException(id + ": unknown family " + family); }
        if (weight < 0) { throw new IllegalArgumentException(id + ": weight must not be negative"); }
        if (rarity == Rarity.UNIQUE) { throw new IllegalArgumentException(id + ": a Job Chip is Normal, Magic, or Rare"); }
        for (PartSpec part : parts) {
            if (part.filter() == WorkFilter.STAT && !WorkOrderFamilies.goalStat(family, MachineStat.bySerializedName(part.item()))) {
                throw new IllegalArgumentException(id + ": a " + family + " stat goal cannot name " + part.item());
            }
            if (part.filter() == WorkFilter.GROUP && !WorkOrderFamilies.group(family, part.item())) {
                throw new IllegalArgumentException(id + ": " + family + " machines report no group " + part.item());
            }
            if (part.filter() == WorkFilter.STREAK && !WorkOrderFamilies.streaks(family)) {
                throw new IllegalArgumentException(id + ": " + family + " machines report no failures to break a streak");
            }
        }
    }

    /** In the Dispatch Terminal's listing, not only given by command or quest. */
    public boolean pooled() {
        return weight > 0;
    }

    /** Whether a listing of {@code tier} can draw this template: it fixes that tier or none. */
    public boolean fitsTier(int tier) {
        return this.tier == 0 || this.tier == tier;
    }

    /** The family the job is for: the template's own, or its objective's. */
    public String family(Map<ResourceLocation, WorkObjectiveEntry> objectives) {
        if (!family.isEmpty()) { return family; }
        WorkObjectiveEntry entry = objectives.get(objectiveId);
        return entry == null ? "" : entry.family();
    }

    /** Whether the template can roll at {@code tier}: its objective is loaded and, for an item objective, has an option. */
    public boolean canRoll(int tier, Map<ResourceLocation, WorkObjectiveEntry> objectives, Predicate<String> itemAvailable) {
        if (!fitsTier(tier)) { return false; }
        if (objectiveId == null) { return true; }
        WorkObjectiveEntry entry = objectives.get(objectiveId);
        if (entry == null || (!family.isEmpty() && !family.equals(entry.family()))) { return false; }
        return !entry.filter().named() || !entry.options(stage(tier, entry.family()), itemAvailable).isEmpty();
    }

    /**
     * The template's job at {@code tier}, or at its own tier when it fixes one: its fixed fields, and rolls for the rest the
     * way a random job rolls them. The rewards are not rolled here; the caller adds fixed rewards or rolls the reward table.
     * Returns null when the template cannot roll.
     */
    public WorkOrder roll(int tier, Map<ResourceLocation, WorkObjectiveEntry> objectives, RandomGenerator random, Predicate<String> itemAvailable) {
        int rolledTier = this.tier == 0 ? Math.clamp(tier, WorkOrder.MIN_TIER, WorkOrder.MAX_TIER) : this.tier;
        if (!canRoll(rolledTier, objectives, itemAvailable)) { return null; }
        String jobFamily = family(objectives);
        WorkObjective objective = objectiveId != null
                ? objectives.get(objectiveId).roll(rolledTier, stage(rolledTier, jobFamily), random, itemAvailable)
                : inline(random);
        if (objective == null) { return null; }
        int required = minStage != UNSET ? minStage : WorkOrderFamilies.requiredStage(jobFamily, rolledTier);
        return new WorkOrder(jobFamily, rolledTier, objective, required, minMasteryLevel == UNSET ? 0 : minMasteryLevel,
                bounty == UNSET ? WorkOrder.BASE_BOUNTY + objective.bonus() : bounty, prestige == UNSET ? 0 : prestige)
                .fromTemplate(id.toString(), name, rewardTable);
    }

    /** The stage that caps the template's item options: its own chassis stage, or the family's cap for the tier. */
    private int stage(int tier, String jobFamily) {
        return minStage != UNSET ? minStage : WorkOrderFamilies.stageCap(jobFamily, tier);
    }

    /** The inline parts with their counts rolled. The Bounty bonus is the highest band any ranged part lands in. */
    private WorkObjective inline(RandomGenerator random) {
        List<WorkPart> rolled = new ArrayList<>();
        int bonus = 0;
        for (PartSpec part : parts) {
            long count = part.count().roll(random);
            bonus = Math.max(bonus, part.count().bonus(count));
            rolled.add(new WorkPart(type, part.filter(), part.count().minimum(), part.item(), count));
        }
        return new WorkObjective(id, rolled, bonus);
    }

    public static WorkOrderTemplate parse(ResourceLocation id, JsonObject json) {
        try {
            String family = json.has("family") ? json.get("family").getAsString().toLowerCase(Locale.ROOT) : "";
            int tier = json.has("tier") ? json.get("tier").getAsInt() : 0;
            ResourceLocation objectiveId = null;
            WorkObjectiveType type = null;
            List<PartSpec> parts = new ArrayList<>();
            JsonElement objective = json.get("objective");
            if (objective == null) {
                throw new IllegalArgumentException("a template needs an objective");
            } else if (objective.isJsonPrimitive()) {
                objectiveId = ResourceLocation.parse(objective.getAsString());
            } else {
                JsonObject inline = objective.getAsJsonObject();
                type = WorkObjectiveType.byId(inline.get("type").getAsString());
                if (type == null) { throw new IllegalArgumentException("unknown job type " + inline.get("type")); }
                for (JsonElement element : inline.getAsJsonArray("parts")) {
                    parts.add(part(element.getAsJsonObject()));
                }
            }
            List<Reward> rewards = new ArrayList<>();
            if (json.has("rewards")) {
                for (JsonElement element : json.getAsJsonArray("rewards")) {
                    JsonObject reward = element.getAsJsonObject();
                    rewards.add(new Reward(reward.get("item").getAsString(), reward.has("count") ? reward.get("count").getAsInt() : 1));
                }
            }
            return new WorkOrderTemplate(id, family, tier, objectiveId, type, parts, number(json, "min_stage"), number(json, "min_mastery_level"),
                    number(json, "bounty"), number(json, "prestige"), json.has("name") ? json.get("name").getAsString() : "", rarity(json), rewards,
                    json.has("reward_table") ? ResourceLocation.parse(json.get("reward_table").getAsString()).toString() : "",
                    json.has("weight") ? json.get("weight").getAsInt() : 0);
        } catch (RuntimeException exception) {
            throw exception.getMessage() != null && exception.getMessage().startsWith(id.toString()) ? exception
                    : new IllegalArgumentException(id + ": " + exception.getMessage(), exception);
        }
    }

    private static PartSpec part(JsonObject json) {
        WorkFilter filter = WorkFilter.byId(json.has("filter") ? json.get("filter").getAsString() : json.has("item") ? "item" : "any");
        if (filter == null) { throw new IllegalArgumentException("unknown filter " + json.get("filter")); }
        int minimum = json.has("min") ? json.get("min").getAsInt() : 0;
        boolean stat = filter == WorkFilter.STAT;
        JsonElement count = json.has("target") ? json.get("target") : json.get("count");
        if (count == null) { throw new IllegalArgumentException("a part needs a count"); }
        WorkObjectiveEntry.Goal goal = WorkObjectiveEntry.goal(count, minimum, stat ? WorkPart.STAT_SCALE : 1);
        if (stat && MachineStat.bySerializedName(json.has("stat") ? json.get("stat").getAsString() : "") == null) {
            throw new IllegalArgumentException("a stat part needs a known stat");
        }
        String item = stat ? MachineStat.bySerializedName(json.get("stat").getAsString()).getSerializedName()
                : filter == WorkFilter.GROUP ? json.get("group").getAsString()
                : json.has("item") ? json.get("item").getAsString() : "";
        return new PartSpec(filter, item, goal);
    }

    private static Rarity rarity(JsonObject json) {
        if (!json.has("rarity")) { return null; }
        String name = json.get("rarity").getAsString().toLowerCase(Locale.ROOT);
        for (Rarity rarity : Rarity.values()) {
            if (rarity.getSerializedName().equals(name)) { return rarity; }
        }
        throw new IllegalArgumentException("unknown rarity " + name);
    }

    private static int number(JsonObject json, String key) {
        if (!json.has(key)) { return UNSET; }
        int value = json.get(key).getAsInt();
        if (value < 0) { throw new IllegalArgumentException(key + " must not be negative"); }
        return value;
    }
}
