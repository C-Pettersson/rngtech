package com.rngtech.content.loot;

import com.rngtech.RNGTech;

import com.google.common.collect.BiMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.List;

/**
 * Loot tables RNGTech rolls when a machine event happens, so packs can reward machine challenges. Every shipped table is
 * empty; packs fill them by datapack, and conditions read the event through {@link #CONTEXT}.
 */
public final class ChallengeLoot {
    public static final LootContextParam<ChallengeContext> CONTEXT = new LootContextParam<>(RNGTech.id("challenge"));
    public static final LootContextParamSet PARAM_SET = LootContextParamSet.builder()
            .required(LootContextParams.ORIGIN)
            .required(CONTEXT)
            .optional(LootContextParams.THIS_ENTITY)
            .build();

    public static final ResourceKey<LootTable> CALIBRATION = table("calibration");
    public static final ResourceKey<LootTable> CRUSHER_JAM = table("crusher_jam");
    public static final ResourceKey<LootTable> HEAT_FAILURE = table("heat_failure");
    public static final ResourceKey<LootTable> VACUUM_COLLAPSE = table("vacuum_collapse");
    public static final ResourceKey<LootTable> FORESTRY_HARVEST = table("forestry_harvest");
    public static final ResourceKey<LootTable> MASTERY_LEVEL = table("mastery_level");
    public static final List<ResourceKey<LootTable>> TABLES =
            List.of(CALIBRATION, CRUSHER_JAM, HEAT_FAILURE, VACUUM_COLLAPSE, FORESTRY_HARVEST, MASTERY_LEVEL);

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

    /** Rolls {@code table} for an event at {@code pos}; a missing table rolls nothing. */
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

    /**
     * Rolls {@code table} and merges the results into {@code outputSlots} of {@code inventory} where they fit, dropping the
     * rest on top of the machine. Output slots usually refuse insertion, so this merges directly.
     */
    public static void reward(
            Level level,
            BlockPos pos,
            ResourceKey<LootTable> table,
            ChallengeContext context,
            IItemHandlerModifiable inventory,
            int... outputSlots
    ) {
        for (ItemStack stack : roll(level, pos, table, context)) {
            ItemStack remaining = stack.copy();
            for (int slot : outputSlots) {
                if (inventory == null || remaining.isEmpty()) {
                    break;
                }
                ItemStack current = inventory.getStackInSlot(slot);
                int limit = Math.min(remaining.getMaxStackSize(), inventory.getSlotLimit(slot));
                if (current.isEmpty()) {
                    int moved = Math.min(limit, remaining.getCount());
                    inventory.setStackInSlot(slot, remaining.copyWithCount(moved));
                    remaining.shrink(moved);
                } else if (ItemStack.isSameItemSameComponents(current, remaining) && current.getCount() < limit) {
                    int moved = Math.min(limit - current.getCount(), remaining.getCount());
                    inventory.setStackInSlot(slot, current.copyWithCount(current.getCount() + moved));
                    remaining.shrink(moved);
                }
            }
            dropOnTop(level, pos, remaining);
        }
    }

    /** Rolls {@code table} and drops every result on top of the machine. */
    public static void drop(Level level, BlockPos pos, ResourceKey<LootTable> table, ChallengeContext context) {
        reward(level, pos, table, context, null);
    }

    public static void dropOnTop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    private static ResourceKey<LootTable> table(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, RNGTech.id("challenges/" + name));
    }
}
