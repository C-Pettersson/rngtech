package com.rngtech.content.dormant;

import com.rngtech.RNGTech;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Every Dormant Machine site the world has loaded, kept in the Overworld's saved data so the reset and restore commands
 * can reach sites in unloaded chunks. A command changes an entry's salt and marks it pending; the site rerolls the next
 * time its chunk is loaded.
 */
public final class DormantSiteIndex extends SavedData {
    private static final String NAME = RNGTech.MOD_ID + "_dormant_sites";

    public enum State implements StringRepresentable {
        UNTOUCHED, CHARGED, SPENT;

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** One site. {@code entity} marks a derelict Forestry Companion rather than a block. */
    public record Entry(GlobalPos pos, ResourceLocation site, State state, long salt, boolean pending, boolean entity) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                ResourceLocation.CODEC.fieldOf("site").forGetter(Entry::site),
                State.CODEC.fieldOf("state").forGetter(Entry::state),
                Codec.LONG.optionalFieldOf("salt", 0L).forGetter(Entry::salt),
                Codec.BOOL.optionalFieldOf("pending", false).forGetter(Entry::pending),
                Codec.BOOL.optionalFieldOf("entity", false).forGetter(Entry::entity)
        ).apply(instance, Entry::new));

        Entry with(State next) { return new Entry(pos, site, next, salt, pending, entity); }
        Entry rerolled(long nextSalt) { return new Entry(pos, site, state, nextSalt, true, entity); }
        Entry applied() { return new Entry(pos, site, State.UNTOUCHED, salt, false, entity); }
        Entry settled() { return new Entry(pos, site, state, salt, false, entity); }
    }

    private final Map<GlobalPos, Entry> entries = new LinkedHashMap<>();
    private int pendingCount;

    public static DormantSiteIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DormantSiteIndex::new, DormantSiteIndex::load, null), NAME);
    }

    public static GlobalPos key(Level level, BlockPos pos) {
        return GlobalPos.of(level.dimension(), pos.immutable());
    }

    public Entry entry(GlobalPos pos) {
        return entries.get(pos);
    }

    /** Adds a site the first time it loads. An existing entry is kept, including its salt and any pending reroll. */
    public Entry register(GlobalPos pos, ResourceLocation site, State state, boolean entity) {
        Entry existing = entries.get(pos);
        if (existing != null) { return existing; }
        Entry entry = new Entry(pos, site, state, 0L, false, entity);
        entries.put(pos, entry);
        setDirty();
        return entry;
    }

    public void mark(GlobalPos pos, State state) {
        Entry entry = entries.get(pos);
        if (entry != null && entry.state() != state) {
            entries.put(pos, entry.with(state));
            setDirty();
        }
    }

    public void applied(GlobalPos pos) {
        Entry entry = entries.get(pos);
        if (entry != null) {
            if (entry.pending()) { pendingCount--; }
            entries.put(pos, entry.applied());
            setDirty();
        }
    }

    /** Clears a pending reroll that could not be applied, such as a site a player built over. */
    public void skipped(GlobalPos pos) {
        Entry entry = entries.get(pos);
        if (entry != null && entry.pending()) {
            pendingCount--;
            entries.put(pos, entry.settled());
            setDirty();
        }
    }

    /** Marks untouched sites, and with {@code force} charged ones too, for a new random soul. */
    public int reset(boolean force, RandomSource random) {
        return reroll(entry -> entry.state() == State.UNTOUCHED || force && entry.state() == State.CHARGED, true, random);
    }

    /** Marks every site, husks included, to come back with its last soul or a new random one. */
    public int restore(boolean randomSoul, RandomSource random) {
        return reroll(entry -> true, randomSoul, random);
    }

    private int reroll(Predicate<Entry> eligible, boolean newSalt, RandomSource random) {
        int count = 0;
        for (Map.Entry<GlobalPos, Entry> slot : entries.entrySet()) {
            Entry entry = slot.getValue();
            if (!eligible.test(entry)) { continue; }
            if (!entry.pending()) { pendingCount++; }
            slot.setValue(entry.rerolled(newSalt ? random.nextLong() : entry.salt()));
            count++;
        }
        if (count > 0) { setDirty(); }
        return count;
    }

    public boolean hasPending() {
        return pendingCount > 0;
    }

    public List<Entry> pending(ResourceKey<Level> dimension) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (entry.pending() && entry.pos().dimension().equals(dimension)) { result.add(entry); }
        }
        return result;
    }

    public int size() {
        return entries.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            Entry.CODEC.encodeStart(NbtOps.INSTANCE, entry).result().ifPresent(list::add);
        }
        tag.put("sites", list);
        return tag;
    }

    private static DormantSiteIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        DormantSiteIndex index = new DormantSiteIndex();
        for (Tag element : tag.getList("sites", Tag.TAG_COMPOUND)) {
            Entry.CODEC.parse(NbtOps.INSTANCE, element).result().ifPresent(entry -> {
                index.entries.put(entry.pos(), entry);
                if (entry.pending()) { index.pendingCount++; }
            });
        }
        return index;
    }
}
