package com.rngtech.content.affixsorter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * An Affix Sorter's ordered rule list and the route for items no rule matches. Rule Capacity limits how many rules
 * may be enabled, not how many the list stores.
 */
public record SorterRules(List<SorterRule> rules, SorterRule.Route fallback) {
    public static final int MAX_RULES = 32;

    public static final SorterRules EMPTY = new SorterRules(List.of(), SorterRule.Route.REJECT);

    public static final Codec<SorterRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SorterRule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(SorterRules::rules),
            SorterRule.Route.CODEC.optionalFieldOf("fallback", SorterRule.Route.REJECT).forGetter(SorterRules::fallback)
    ).apply(instance, SorterRules::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SorterRules> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public SorterRules {
        rules = rules == null ? List.of() : List.copyOf(rules.subList(0, Math.min(rules.size(), MAX_RULES)));
        fallback = fallback == null ? SorterRule.Route.REJECT : fallback;
    }

    public int enabledCount() {
        return (int) rules.stream().filter(SorterRule::enabled).count();
    }

    public SorterRules withRule(int index, SorterRule rule) {
        if (index < 0 || index >= rules.size()) {
            return this;
        }
        List<SorterRule> next = new ArrayList<>(rules);
        next.set(index, rule);
        return new SorterRules(next, fallback);
    }

    public SorterRules withAdded(SorterRule rule) {
        if (rules.size() >= MAX_RULES) {
            return this;
        }
        List<SorterRule> next = new ArrayList<>(rules);
        next.add(rule);
        return new SorterRules(next, fallback);
    }

    public SorterRules withRemoved(int index) {
        if (index < 0 || index >= rules.size()) {
            return this;
        }
        List<SorterRule> next = new ArrayList<>(rules);
        next.remove(index);
        return new SorterRules(next, fallback);
    }

    /** Moves the rule at {@code index} one place up ({@code -1}) or down ({@code 1}). */
    public SorterRules withMoved(int index, int direction) {
        int target = index + direction;
        if (index < 0 || index >= rules.size() || target < 0 || target >= rules.size()) {
            return this;
        }
        List<SorterRule> next = new ArrayList<>(rules);
        SorterRule moved = next.remove(index);
        next.add(target, moved);
        return new SorterRules(next, fallback);
    }

    public SorterRules withFallback(SorterRule.Route route) {
        return new SorterRules(rules, route);
    }

    /** Switches off enabled rules from the bottom until no more than {@code capacity} stay enabled. */
    public SorterRules limitedTo(int capacity) {
        int allowed = Math.max(0, capacity);
        List<SorterRule> next = new ArrayList<>(rules.size());
        for (SorterRule rule : rules) {
            if (rule.enabled() && allowed > 0) {
                allowed--;
                next.add(rule);
            } else {
                next.add(rule.enabled() ? rule.withEnabled(false) : rule);
            }
        }
        return new SorterRules(next, fallback);
    }
}
