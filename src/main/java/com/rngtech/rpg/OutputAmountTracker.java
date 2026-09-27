package com.rngtech.rpg;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class OutputAmountTracker {
    public static final int PROGRESS_SCALE = 1000;
    private static final double EPSILON = 1.0E-9;

    private ItemStack trackedInput = ItemStack.EMPTY;
    private double bonusProgress;

    public void updateInput(ItemStack input) {
        if (input.isEmpty()) {
            return;
        }

        if (trackedInput.isEmpty() || !ItemStack.isSameItemSameComponents(trackedInput, input)) {
            trackedInput = input.copyWithCount(1);
            bonusProgress = 0.0;
        }
    }

    public void reset() {
        trackedInput = ItemStack.EMPTY;
        bonusProgress = 0.0;
    }

    public int nextOutputCount(int baseCount, double outputAmount) {
        return count(baseCount, outputAmount, bonusProgress).total();
    }

    public int nextOutputCount(ItemStack input, int baseCount, double outputAmount) {
        return count(baseCount, outputAmount, bonusProgressFor(input)).total();
    }

    public int nextOutputCount(int baseCount, double outputAmount, int crafts) {
        int total = 0;
        double progress = bonusProgress;
        for (int craft = 0; craft < Math.max(0, crafts); craft++) {
            OutputCount count = count(baseCount, outputAmount, progress);
            total += count.total();
            progress = count.nextBonusProgress();
        }
        return total;
    }

    public int nextOutputCount(ItemStack input, int baseCount, double outputAmount, int crafts) {
        int total = 0;
        double progress = bonusProgressFor(input);
        for (int craft = 0; craft < Math.max(0, crafts); craft++) {
            OutputCount count = count(baseCount, outputAmount, progress);
            total += count.total();
            progress = count.nextBonusProgress();
        }
        return total;
    }

    public int consumeOutputCount(int baseCount, double outputAmount) {
        OutputCount count = count(baseCount, outputAmount, bonusProgress);
        bonusProgress = count.nextBonusProgress();
        return count.total();
    }

    public double bonusProgress() {
        return clampUnit(bonusProgress);
    }

    public int scaledBonusProgress() {
        return scaled(bonusProgress());
    }

    public boolean hasBankedProgressForDifferentInput(ItemStack input) {
        return bonusProgress() > 0.0
                && !trackedInput.isEmpty()
                && !input.isEmpty()
                && !ItemStack.isSameItemSameComponents(trackedInput, input);
    }

    public static int scaledBonusIncrement(int baseCount, double outputAmount) {
        return scaled(fractionalOutput(baseCount, outputAmount));
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("OutputBonusProgress", bonusProgress);
        tag.put("OutputBonusInput", trackedInput.saveOptional(registries));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        bonusProgress = clampUnit(tag.getDouble("OutputBonusProgress"));
        trackedInput = ItemStack.parseOptional(registries, tag.getCompound("OutputBonusInput"));
        if (!trackedInput.isEmpty()) {
            trackedInput = trackedInput.copyWithCount(1);
        }
    }

    private static OutputCount count(int baseCount, double outputAmount, double currentBonusProgress) {
        double scaledOutput = scaledOutput(baseCount, outputAmount);
        int guaranteed = Math.max(1, (int) Math.floor(scaledOutput + EPSILON));
        double nextProgress = clampUnit(currentBonusProgress) + Math.max(0.0, scaledOutput - guaranteed);
        int bonus = (int) Math.floor(nextProgress + EPSILON);
        nextProgress -= bonus;
        if (nextProgress >= 1.0 - EPSILON) {
            bonus++;
            nextProgress = 0.0;
        }
        return new OutputCount(guaranteed + bonus, clampUnit(nextProgress));
    }

    private double bonusProgressFor(ItemStack input) {
        if (input.isEmpty() || trackedInput.isEmpty() || ItemStack.isSameItemSameComponents(trackedInput, input)) {
            return bonusProgress;
        }
        return 0.0;
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

    private record OutputCount(int total, double nextBonusProgress) {
    }
}
