package com.rngtech.rpg.dormant;

import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveTree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * The Mastery soul a Dormant Machine holds, with what it costs to wake and where it may be released. A soul rolls from a
 * site and a seed, so the same seed always gives the same soul.
 */
public record DormantSoul(MachineMasteryFamily family, MachineProgressionState state, long wakeFe, int wakeInput, int releaseStage) {
    private static final Codec<MachineMasteryFamily> FAMILY_CODEC = Codec.STRING.xmap(
            name -> MachineMasteryFamily.valueOf(name.toUpperCase(Locale.ROOT)), family -> family.name().toLowerCase(Locale.ROOT));
    public static final Codec<DormantSoul> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FAMILY_CODEC.fieldOf("family").forGetter(DormantSoul::family),
            MachineProgressionState.CODEC.fieldOf("progression").forGetter(DormantSoul::state),
            Codec.LONG.fieldOf("wake_fe").forGetter(DormantSoul::wakeFe),
            Codec.INT.fieldOf("wake_input").forGetter(DormantSoul::wakeInput),
            Codec.INT.optionalFieldOf("release_stage", 0).forGetter(DormantSoul::releaseStage)
    ).apply(instance, DormantSoul::new));

    public DormantSoul {
        state = state.forFamily(family);
        wakeFe = Math.max(1L, wakeFe);
        wakeInput = Math.max(1, wakeInput);
        releaseStage = Math.max(0, releaseStage);
    }

    public int level() {
        return state.level();
    }

    public DormantSoul withState(MachineProgressionState next) {
        return new DormantSoul(family, next, wakeFe, wakeInput, releaseStage);
    }

    /** Rolls a family, a level in the band, and a build, then prices the wake by the level's place in the band. */
    public static DormantSoul roll(DormantSite site, long seed) {
        RandomGenerator random = new SplittableRandom(seed);
        MachineMasteryFamily family = weighted(site.families(), random);
        int level = site.minLevel() + random.nextInt(site.maxLevel() - site.minLevel() + 1);
        List<String> build = build(site, family, random);
        MachineProgressionState state = new MachineProgressionState(MachineProgressionState.xpForLevel(level), 0, level, List.of(),
                family.startNodeId(), build, true, "", List.of(), 0, true);
        state = MegaPassiveTree.follow(state, candidate -> true);
        return new DormantSoul(family, state, site.wakeFe(level), site.wakeInput(), site.releaseStage());
    }

    private static List<String> build(DormantSite site, MachineMasteryFamily family, RandomGenerator random) {
        int maxPoints = site.maxLevel() - 1;
        List<DormantSite.Archetype> archetypes = site.builds().getOrDefault(family, List.of());
        if (archetypes.isEmpty()) { return DormantArchetype.generate(family, random, maxPoints); }
        int total = archetypes.stream().mapToInt(DormantSite.Archetype::weight).sum();
        int pick = random.nextInt(total);
        for (DormantSite.Archetype archetype : archetypes) {
            pick -= archetype.weight();
            if (pick < 0) { return DormantArchetype.through(family, archetype.targets(), maxPoints); }
        }
        return List.of();
    }

    private static MachineMasteryFamily weighted(Map<MachineMasteryFamily, Integer> weights, RandomGenerator random) {
        // Iterate in enum order so the roll does not depend on map iteration order.
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int pick = random.nextInt(total);
        for (MachineMasteryFamily family : MachineMasteryFamily.values()) {
            pick -= weights.getOrDefault(family, 0);
            if (pick < 0) { return family; }
        }
        throw new IllegalStateException("A site needs at least one family");
    }
}
