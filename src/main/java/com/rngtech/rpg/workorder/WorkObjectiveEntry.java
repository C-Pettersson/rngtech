package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineStat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * One job from {@code data/<namespace>/work_orders/objectives/}: the family that can draw it, its verb and filter, a draw
 * weight, and for each of the five tiers a count range, a step the rolled count lands on, and the filter's minimum. An
 * item job also lists its item options and how many parts it names; the rolled count is split between its parts. A stat
 * goal names its {@code stat}, and its tiers' targets are read in the stat's own units and kept in hundredths.
 */
public record WorkObjectiveEntry(ResourceLocation id, String family, WorkObjectiveType type, WorkFilter filter, int weight,
        int minParts, int maxParts, List<Option> options, List<Goal> tiers, String stat) {
    /** An item a job can name, the stage of its recipe, and its draw weight. */
    public record Option(String item, int stage, int weight) {
        public Option {
            if (item == null || item.isEmpty() || stage < 0 || weight <= 0) { throw new IllegalArgumentException("an option needs an item, a stage, and a weight"); }
        }
    }

    /** A tier's goal. The count rolls in {@code min..max} on multiples of {@code step}; a fixed count has min equal to max. */
    public record Goal(long min, long max, long step, int minimum) {
        /** Draft Bounty bonus, in percent, for a count in the low, middle, and high third of the range. */
        private static final int[] BAND_BONUS = {0, 10, 20};

        public Goal {
            step = Math.max(1, step);
            if (min <= 0 || max < min || minimum < 0) { throw new IllegalArgumentException("a goal's counts must run low to high above 0"); }
        }

        public Goal(long count, int minimum) {
            this(count, count, 1, minimum);
        }

        /** A count in the range on a multiple of the step, or {@code min} when no multiple fits. */
        public long roll(RandomGenerator random) {
            long low = (min + step - 1) / step;
            long high = max / step;
            return low > high ? min : (low + random.nextLong(high - low + 1)) * step;
        }

        /** The Bounty bonus of the band {@code count} falls in. A fixed count has no bands. */
        public int bonus(long count) {
            if (max == min) { return 0; }
            double position = (Math.clamp(count, min, max) - min) / (double) (max - min);
            return BAND_BONUS[position < 1.0 / 3.0 ? 0 : position < 2.0 / 3.0 ? 1 : 2];
        }
    }

    public WorkObjectiveEntry {
        tiers = List.copyOf(tiers);
        options = List.copyOf(options);
        if (tiers.size() != WorkOrder.MAX_TIER) { throw new IllegalArgumentException(id + ": a job needs one goal per tier"); }
        if (weight <= 0) { throw new IllegalArgumentException(id + ": weight must be positive"); }
        if (filter.named() && options.isEmpty()) { throw new IllegalArgumentException(id + ": an item or group job needs options"); }
        if (filter == WorkFilter.GROUP) {
            for (Option option : options) {
                if (!WorkOrderFamilies.group(family, option.item())) { throw new IllegalArgumentException(id + ": " + family + " machines report no group " + option.item()); }
            }
        }
        minParts = Math.clamp(minParts, 1, WorkObjective.MAX_PARTS);
        maxParts = Math.clamp(maxParts, minParts, WorkObjective.MAX_PARTS);
        stat = stat == null ? "" : stat;
        if (filter == WorkFilter.STAT && !WorkOrderFamilies.goalStat(family, MachineStat.bySerializedName(stat))) {
            throw new IllegalArgumentException(id + ": a " + family + " stat goal cannot name " + stat);
        }
        if (filter == WorkFilter.STREAK && !WorkOrderFamilies.streaks(family)) {
            throw new IllegalArgumentException(id + ": " + family + " machines report no failures to break a streak");
        }
    }

    public WorkObjectiveEntry(ResourceLocation id, String family, WorkObjectiveType type, WorkFilter filter, int weight,
            int minParts, int maxParts, List<Option> options, List<Goal> tiers) {
        this(id, family, type, filter, weight, minParts, maxParts, options, tiers, "");
    }

    public Goal goal(int tier) {
        return tiers.get(Math.clamp(tier, WorkOrder.MIN_TIER, WorkOrder.MAX_TIER) - 1);
    }

    /** The options a job of a stage capped at {@code stageCap} can name. A group needs no loaded recipe check. */
    public List<Option> options(int stageCap, Predicate<String> itemAvailable) {
        return options.stream().filter(option -> option.stage() <= stageCap && (filter == WorkFilter.GROUP || itemAvailable.test(option.item()))).toList();
    }

    /**
     * The tier's job with a rolled count and its band's Bounty bonus, or null when an item job has no option a chassis at
     * {@code stageCap} can run. An item job names one to three different items and splits the count between them.
     */
    public WorkObjective roll(int tier, int stageCap, RandomGenerator random, Predicate<String> itemAvailable) {
        Goal goal = goal(tier);
        long count = goal.roll(random);
        int bonus = goal.bonus(count);
        if (filter == WorkFilter.STAT) {
            return new WorkObjective(id, List.of(new WorkPart(type, filter, 0, MachineStat.bySerializedName(stat).getSerializedName(), count)), bonus);
        }
        if (!filter.named()) {
            return new WorkObjective(id, List.of(new WorkPart(type, filter, goal.minimum(), "", count)), bonus);
        }
        List<Option> pool = new ArrayList<>(options(stageCap, itemAvailable));
        if (pool.isEmpty()) { return null; }
        int parts = Math.min(pool.size(), minParts + random.nextInt(maxParts - minParts + 1));
        long share = Math.max(goal.step(), count / parts / goal.step() * goal.step());
        List<WorkPart> picked = new ArrayList<>();
        for (int index = 0; index < parts; index++) {
            Option option = pick(pool, random);
            pool.remove(option);
            picked.add(new WorkPart(type, filter, 0, option.item(), share));
        }
        return new WorkObjective(id, picked, bonus);
    }

    private static Option pick(List<Option> pool, RandomGenerator random) {
        int total = pool.stream().mapToInt(Option::weight).sum();
        int roll = random.nextInt(total);
        for (Option option : pool) {
            roll -= option.weight();
            if (roll < 0) { return option; }
        }
        return pool.getLast();
    }

    public static WorkObjectiveEntry parse(ResourceLocation id, JsonObject json) {
        String family = json.get("family").getAsString().toLowerCase(Locale.ROOT);
        if (!WorkOrderFamilies.known(family)) { throw new IllegalArgumentException(id + ": unknown family " + family); }
        WorkObjectiveType type = WorkObjectiveType.byId(json.get("type").getAsString());
        if (type == null) { throw new IllegalArgumentException(id + ": unknown job type " + json.get("type")); }
        WorkFilter filter = WorkFilter.byId(json.has("filter") ? json.get("filter").getAsString() : "any");
        if (filter == null) { throw new IllegalArgumentException(id + ": unknown filter " + json.get("filter")); }
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        int minParts = 1;
        int maxParts = 1;
        if (json.has("parts")) {
            JsonObject parts = json.getAsJsonObject("parts");
            minParts = parts.get("min").getAsInt();
            maxParts = parts.get("max").getAsInt();
        }
        List<Option> options = new ArrayList<>();
        if (json.has("options")) {
            for (JsonElement element : json.getAsJsonArray("options")) {
                JsonObject option = element.getAsJsonObject();
                try {
                    options.add(new Option(option.get("item").getAsString(), option.get("stage").getAsInt(),
                            option.has("weight") ? option.get("weight").getAsInt() : 1));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(id + ": " + exception.getMessage(), exception);
                }
            }
        }
        String stat = json.has("stat") ? json.get("stat").getAsString() : "";
        List<Goal> goals = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("tiers")) {
            JsonObject goal = element.getAsJsonObject();
            int minimum = goal.has("min") ? goal.get("min").getAsInt() : 0;
            JsonElement count = goal.has("target") ? goal.get("target") : goal.get("count");
            try {
                goals.add(goal(count, minimum, filter == WorkFilter.STAT ? WorkPart.STAT_SCALE : 1));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(id + ": " + exception.getMessage(), exception);
            }
        }
        return new WorkObjectiveEntry(id, family, type, filter, weight, minParts, maxParts, options, goals, stat);
    }

    /** A count or target, fixed or a {@code min}/{@code max}/{@code step} range, times {@code scale}. */
    static Goal goal(JsonElement count, int minimum, int scale) {
        if (count.isJsonObject()) {
            JsonObject range = count.getAsJsonObject();
            return new Goal(Math.round(range.get("min").getAsDouble() * scale), Math.round(range.get("max").getAsDouble() * scale),
                    range.has("step") ? Math.round(range.get("step").getAsDouble() * scale) : scale, minimum);
        }
        return new Goal(Math.round(count.getAsDouble() * scale), minimum);
    }
}
