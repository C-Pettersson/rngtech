package com.rngtech.content.loot;

import com.rngtech.RNGTech;

import com.google.common.collect.BiMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;

import java.util.List;

/**
 * The {@code rngtech:challenge} loot table type, whose conditions read a {@link ChallengeContext} through {@link #CONTEXT}.
 * Machines roll no loot on their own events; Work Orders roll {@link #WORK_ORDER} for a job's reward, and packs change it
 * by datapack.
 */
public final class ChallengeLoot {
    public static final LootContextParam<ChallengeContext> CONTEXT = new LootContextParam<>(RNGTech.id("challenge"));
    public static final LootContextParamSet PARAM_SET = LootContextParamSet.builder()
            .required(LootContextParams.ORIGIN)
            .required(CONTEXT)
            .optional(LootContextParams.THIS_ENTITY)
            .build();

    /** Rolled once per reward roll of a Fulfilled Work Order; {@code rngtech:order_roll} tells the rolls apart. */
    public static final ResourceKey<LootTable> WORK_ORDER = ResourceKey.create(Registries.LOOT_TABLE, RNGTech.id("challenges/work_order"));

    private ChallengeLoot() {
    }

    /** Registers the {@code rngtech:challenge} loot table type before any loot table loads. */
    public static void registerParamSet() {
        BiMap<ResourceLocation, LootContextParamSet> registry =
                ObfuscationReflectionHelper.getPrivateValue(LootContextParamSets.class, null, "REGISTRY");
        if (registry != null && !registry.containsKey(RNGTech.id("challenge"))) {
            registry.put(RNGTech.id("challenge"), PARAM_SET);
        }
    }

    /** Rolls {@code table} at {@code pos}; a missing table rolls nothing. */
    public static List<ItemStack> roll(Level level, BlockPos pos, ResourceKey<LootTable> table, ChallengeContext context) {
        if (!(level instanceof ServerLevel server)) {
            return List.of();
        }
        LootTable lootTable = server.getServer().reloadableRegistries().getLootTable(table);
        if (lootTable == LootTable.EMPTY) {
            return List.of();
        }
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(CONTEXT, context)
                .create(PARAM_SET);
        return lootTable.getRandomItems(params);
    }
}
