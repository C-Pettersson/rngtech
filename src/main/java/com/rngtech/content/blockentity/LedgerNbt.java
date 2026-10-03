package com.rngtech.content.blockentity;

import com.rngtech.rpg.OutputLedger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/** Saves and restores item-keyed output ledgers; entries for items that no longer exist are dropped. */
public final class LedgerNbt {
    public static void save(CompoundTag tag, String key, OutputLedger<Item> ledger) {
        ListTag list = new ListTag();
        ledger.entries().forEach((item, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Item", BuiltInRegistries.ITEM.getKey(item).toString());
            entry.putDouble("Progress", amount);
            list.add(entry);
        });
        tag.put(key, list);
    }

    public static void load(CompoundTag tag, String key, OutputLedger<Item> ledger) {
        Map<Item, Double> saved = new HashMap<>();
        for (Tag element : tag.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Item"));
            if (id != null) {
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> saved.put(item, entry.getDouble("Progress")));
            }
        }
        ledger.restore(saved);
    }

    /** Progress toward the next whole unit, per thousand, full once a unit waits for room. */
    public static int permille(double progress) {
        return progress >= 1.0 ? 1000 : (int) Math.round(Math.max(0.0, progress) * 1000);
    }

    private LedgerNbt() {
    }
}
