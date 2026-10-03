package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class OutputAmountTracker {
    public static final int PROGRESS_SCALE = 1000;
    private static final double EPSILON = 1.0E-9;

    private final BonusBanks<ItemStack> banks = new BonusBanks<>(ItemStack::isSameItemSameComponents);

    /** A remembered input's bank, as saved on the block and, with Wide Ledger, on the dropped machine. */
    public record SavedBank(ItemStack input, double progress, int cycles) {
        public static final Codec<SavedBank> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("input").forGetter(SavedBank::input),
                Codec.DOUBLE.optionalFieldOf("progress", 0.0).forGetter(SavedBank::progress),
                Codec.INT.optionalFieldOf("cycles", 0).forGetter(SavedBank::cycles)
        ).apply(instance, SavedBank::new));
        public static final Codec<List<SavedBank>> LIST_CODEC = CODEC.sizeLimitedListOf(64);
    }

    /** Items produced by one craft, and how many of them the bank paid. */
    public record Payout(int total, int banked) {
    }

    /** How many inputs keep their own bank; one resets the bank whenever the input changes. */
    public void setMemory(int memory) {
        banks.setMemory(memory);
    }

    public void updateInput(ItemStack input) {
        if (input.isEmpty()) {
            return;
        }
        ItemStack current = banks.currentKey();
        if (current == null || !ItemStack.isSameItemSameComponents(current, input)) {
            banks.select(input.copyWithCount(1));
        }
    }

    public void reset() {
        banks.resetCurrent();
    }

    public int nextOutputCount(int baseCount, double outputAmount) {
        return count(baseCount, outputAmount, banks.currentProgress()).total();
    }

    public int nextOutputCount(ItemStack input, int baseCount, double outputAmount) {
        return count(baseCount, outputAmount, progressFor(input)).total();
    }

    public int nextOutputCount(int baseCount, double outputAmount, int crafts) {
        return countCrafts(baseCount, outputAmount, crafts, banks.currentProgress());
    }

    public int nextOutputCount(ItemStack input, int baseCount, double outputAmount, int crafts) {
        return countCrafts(baseCount, outputAmount, crafts, progressFor(input));
    }

    public int consumeOutputCount(int baseCount, double outputAmount) {
        return consume(baseCount, outputAmount).total();
    }

    public Payout consume(int baseCount, double outputAmount) {
        OutputCount count = count(baseCount, outputAmount, banks.currentProgress());
        banks.setCurrentProgress(count.nextBonusProgress());
        return new Payout(count.total(), count.bonus());
    }

    /** Banks whole items, such as salvage that did not fit the output, for a later craft of the current input. */
    public void bank(double items) {
        banks.bank(items);
    }

    /** Counts an eligible cycle on the current input and returns true when it completes the Super Output cadence. */
    public boolean countCadence(int cadence) {
        return banks.countCycle(cadence);
    }

    public double bonusProgress() {
        return clampUnit(banks.currentProgress());
    }

    public int scaledBonusProgress() {
        return scaled(bonusProgress());
    }

    public boolean hasBankedProgressForDifferentInput(ItemStack input) {
        return !input.isEmpty() && banks.wouldEvictProgress(input);
    }

    public static int scaledBonusIncrement(int baseCount, double outputAmount) {
        return scaled(fractionalOutput(baseCount, outputAmount));
    }

    public List<SavedBank> saved() {
        List<SavedBank> saved = new ArrayList<>();
        for (BonusBanks.Bank<ItemStack> bank : banks.banks()) {
            saved.add(new SavedBank(bank.key(), bank.progress(), bank.cycles()));
        }
        return List.copyOf(saved);
    }

    public void restore(List<SavedBank> saved) {
        List<BonusBanks.Bank<ItemStack>> restored = new ArrayList<>();
        for (SavedBank bank : saved) {
            if (!bank.input().isEmpty()) {
                restored.add(new BonusBanks.Bank<>(bank.input().copyWithCount(1), bank.progress(), bank.cycles()));
            }
        }
        banks.restore(restored);
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (BonusBanks.Bank<ItemStack> bank : banks.banks()) {
            CompoundTag entry = new CompoundTag();
            entry.put("Input", bank.key().save(registries));
            entry.putDouble("Progress", bank.progress());
            entry.putInt("Cycles", bank.cycles());
            list.add(entry);
        }
        tag.put("OutputBanks", list);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        List<SavedBank> saved = new ArrayList<>();
        if (tag.contains("OutputBanks", Tag.TAG_LIST)) {
            for (Tag element : tag.getList("OutputBanks", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) element;
                ItemStack input = ItemStack.parseOptional(registries, entry.getCompound("Input"));
                saved.add(new SavedBank(input, entry.getDouble("Progress"), entry.getInt("Cycles")));
            }
        } else {
            // Saves from before bank memory held one input.
            ItemStack input = ItemStack.parseOptional(registries, tag.getCompound("OutputBonusInput"));
            saved.add(new SavedBank(input, clampUnit(tag.getDouble("OutputBonusProgress")), 0));
        }
        restore(saved);
    }

    private double progressFor(ItemStack input) {
        return input.isEmpty() ? banks.currentProgress() : banks.progressFor(input);
    }

    private static int countCrafts(int baseCount, double outputAmount, int crafts, double progress) {
        int total = 0;
        for (int craft = 0; craft < Math.max(0, crafts); craft++) {
            OutputCount count = count(baseCount, outputAmount, progress);
            total += count.total();
            progress = count.nextBonusProgress();
        }
        return total;
    }

    private static OutputCount count(int baseCount, double outputAmount, double currentBonusProgress) {
        double scaledOutput = scaledOutput(baseCount, outputAmount);
        int guaranteed = Math.max(1, (int) Math.floor(scaledOutput + EPSILON));
        double nextProgress = clampBank(currentBonusProgress) + Math.max(0.0, scaledOutput - guaranteed);
        int bonus = (int) Math.floor(nextProgress + EPSILON);
        nextProgress -= bonus;
        if (nextProgress >= 1.0 - EPSILON) {
            bonus++;
            nextProgress = 0.0;
        }
        return new OutputCount(guaranteed + bonus, bonus, clampUnit(nextProgress));
    }

    private static double fractionalOutput(int baseCount, double outputAmount) {
        double scaledOutput = scaledOutput(baseCount, outputAmount);
        int guaranteed = Math.max(1, (int) Math.floor(scaledOutput + EPSILON));
        return Math.max(0.0, scaledOutput - guaranteed);
    }

    private static double scaledOutput(int baseCount, double outputAmount) {
        return Math.max(1.0, Math.max(0, baseCount) * Math.max(0.0, outputAmount));
    }

    private static int scaled(double value) {
        return Math.max(0, Math.min(PROGRESS_SCALE, (int) Math.round(clampUnit(value) * PROGRESS_SCALE)));
    }

    private static double clampUnit(double value) {
        if (value <= 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value);
    }

    private static double clampBank(double value) {
        return Math.max(0.0, Math.min(BonusBanks.MAX_PROGRESS, value));
    }

    private record OutputCount(int total, int bonus, double nextBonusProgress) {
    }
}
