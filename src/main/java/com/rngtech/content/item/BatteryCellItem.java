package com.rngtech.content.item;

import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.unique.UniqueDefinition;
import com.rngtech.rpg.unique.UniqueItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;
import java.util.Locale;

public class BatteryCellItem extends Item {
    private static final MachineTraits UNIQUE_TRAITS = new MachineTraits(Rarity.UNIQUE, 0, List.of());

    private final BatteryCellMaterial material;

    public BatteryCellItem(BatteryCellMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    public static IEnergyStorage energyStorage(ItemStack stack) {
        if (RecyclingData.isStripped(stack)) {
            return null;
        }
        return new StackEnergyStorage(stack);
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return stack.getItem() instanceof BatteryCellItem && !RecyclingData.isStripped(stack);
    }

    public static int energyStored(ItemStack stack) {
        return energyStored(stack, energyCapacity(stack));
    }

    public static int energyStored(ItemStack stack, int capacity) {
        return cellItem(stack) == null
                ? 0
                : Mth.clamp(rawEnergyStored(stack), 0, Math.max(0, capacity));
    }

    public static int rawEnergyStored(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BATTERY_CELL_ENERGY.get(), 0);
    }

    public static int energyCapacity(ItemStack stack) {
        return stats(stack).capacity();
    }

    public static int maxInput(ItemStack stack) {
        return stats(stack).maxInput();
    }

    public static int maxOutput(ItemStack stack) {
        return stats(stack).maxOutput();
    }

    public static double efficiency(ItemStack stack) {
        return stats(stack).efficiency();
    }

    public static double idleLossPercentPerMinute(ItemStack stack) {
        return stats(stack).idleLossPercentPerMinute();
    }

    /**
     * The cell's capacity, rates, efficiency, and idle loss from one stats build. They depend on the item, its traits
     * component, and whether it is stripped, never on its charge, so a holder can keep them until one of those changes.
     */
    public static CellStats stats(ItemStack stack) {
        BatteryCellItem cell = cellItem(stack);
        if (cell == null) {
            return CellStats.NONE;
        }
        MachineStatAccumulator stats = effectiveStats(stack);
        double transfer = stats.value(MachineStat.ENERGY_TRANSFER);
        double outputScale = cell.material.inputRate() <= 0 ? 0.0 : transfer / cell.material.inputRate();
        return new CellStats(
                positiveSaturatedRound(stats.value(MachineStat.ENERGY_CAPACITY)),
                positiveSaturatedRound(transfer),
                positiveSaturatedRound(cell.material.outputRate() * outputScale),
                stats.value(MachineStat.EFFICIENCY),
                Math.max(0.0, stats.value(MachineStat.IDLE_LOSS))
        );
    }

    /** Charges {@code stack} as its FE capability would, with stats read earlier. */
    public static int receiveEnergy(ItemStack stack, CellStats stats, int toReceive, boolean simulate) {
        if (stats.maxInput() <= 0 || toReceive <= 0) {
            return 0;
        }
        int stored = energyStored(stack, stats.capacity());
        int received = Math.min(stats.capacity() - stored, Math.min(stats.maxInput(), toReceive));
        if (!simulate && received > 0) {
            setEnergy(stack, stored + received, stats.capacity());
        }
        return received;
    }

    /** Drains {@code stack} as its FE capability would, with stats read earlier. */
    public static int extractEnergy(ItemStack stack, CellStats stats, int toExtract, boolean simulate) {
        if (stats.maxOutput() <= 0 || toExtract <= 0) {
            return 0;
        }
        int stored = energyStored(stack, stats.capacity());
        int extracted = Math.min(stored, Math.min(stats.maxOutput(), toExtract));
        if (!simulate && extracted > 0) {
            setEnergy(stack, stored - extracted, stats.capacity());
        }
        return extracted;
    }

    public static int removeEnergy(ItemStack stack, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int stored = energyStored(stack);
        int removed = Math.min(stored, amount);
        if (removed > 0) {
            setEnergy(stack, stored - removed);
        }
        return removed;
    }

    public static MachineTraits traits(ItemStack stack) {
        if (RecyclingData.isStripped(stack)) {
            return MachineTraits.EMPTY;
        }
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        if (traits == null) {
            BatteryCellItem cell = cellItem(stack);
            traits = cell != null && cell.material.unique() ? UNIQUE_TRAITS : MachineTraits.EMPTY;
        }
        return MachineImplicitCatalog.effectiveTraits(traits, stack);
    }

    public BatteryCellMaterial material() {
        return material;
    }

    public MachineType machineType() {
        return MachineType.BATTERY_CELL;
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
        CraftedTraitOutputs.handleCrafted(stack, level.random);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (material.unique()) {
            Component name = super.getName(stack);
            return UniqueTooltip.name(CraftedTraitOutputs.isUnidentified(stack) ? CraftedTraitOutputs.unidentifiedName(name) : name);
        }
        if (CraftedTraitOutputs.isUnidentified(stack)) {
            return CraftedTraitOutputs.unidentifiedName(super.getName(stack));
        }
        MachineTraits traits = traits(stack);
        if (traits.isEmpty()) {
            return super.getName(stack);
        }
        return MachineNameGenerator.generatedName(traits, super.getName(stack));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return material.unique() || super.isFoil(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return energyCapacity(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = energyCapacity(stack);
        if (capacity <= 0) {
            return 0;
        }
        return Math.round(13.0F * energyStored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.48F, 0.85F, 0.95F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (RecyclingData.isStripped(stack)) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.recycling_stripped").withStyle(ChatFormatting.GRAY));
        }
        UniqueDefinition unique = UniqueItems.definition(stack);
        if (unique != null) {
            UniqueTooltip.append(stack, unique, tooltipComponents);
            appendEnergyTooltip(stack, tooltipComponents);
            return;
        }
        MachineTraitTooltip.appendUnidentified(stack, tooltipComponents);
        MachineTraits traits = traits(stack);
        if (CraftedTraitOutputs.isUnidentified(stack)) {
            MachineTraitTooltip.appendUnrolledTraitHeader(MachineTraitRoller.refinementPotentialRange(material.stage()), tooltipComponents);
        } else if (stack.has(ModDataComponents.MACHINE_TRAITS.get()) || RecyclingData.isStripped(stack)) {
            MachineTraitTooltip.appendTraitHeader(traits, tooltipComponents, true);
        } else {
            MachineTraitTooltip.appendUnrolledTraitHeader(MachineTraitRoller.refinementPotentialRange(material.stage()), tooltipComponents);
        }
        if (MachineTraitTooltip.shouldShowMaterialData()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.battery_cell.material",
                    Component.translatable(material.translationKey())
            ).withStyle(ChatFormatting.GRAY));
        }
        appendEnergyTooltip(stack, tooltipComponents);
        MachineTraitTooltip.appendIdentity(stack, tooltipComponents);
        MachineTraitTooltip.appendComponentBaseStats(stack, tooltipComponents);
        MachineTraitTooltip.appendTraitDetails(traits, tooltipComponents);
        if (!RecyclingData.isStripped(stack)) {
            MachineTraitTooltip.appendTraitKeyHints(tooltipComponents);
        }
    }

    private static void appendEnergyTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.battery.energy",
                energyStored(stack),
                energyCapacity(stack)
        ).withStyle(ChatFormatting.GRAY));
        if (MachineTraitTooltip.shouldShowMaterialData()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.battery.transfer",
                    maxInput(stack),
                    maxOutput(stack)
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.battery_cell.efficiency",
                    decimal(efficiency(stack))
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.battery_cell.idle_loss",
                    decimal(idleLossPercentPerMinute(stack))
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    private static MachineStatAccumulator effectiveStats(ItemStack stack) {
        MachineStatAccumulator stats = ComponentBaseStatCatalog.effectiveStats(stack);
        return stats == null ? MachineStatAccumulator.batteryCellBase() : stats;
    }

    public static void setEnergy(ItemStack stack, int energy, int capacity) {
        if (cellItem(stack) == null) {
            return;
        }
        stack.set(ModDataComponents.BATTERY_CELL_ENERGY.get(), Mth.clamp(energy, 0, Math.max(0, capacity)));
    }

    public static boolean clampEnergy(ItemStack stack, int capacity) {
        int stored = rawEnergyStored(stack);
        int clamped = energyStored(stack, capacity);
        if (stored == clamped) {
            return false;
        }
        setEnergy(stack, clamped, capacity);
        return true;
    }

    private static void setEnergy(ItemStack stack, int energy) {
        stack.set(ModDataComponents.BATTERY_CELL_ENERGY.get(), Mth.clamp(energy, 0, energyCapacity(stack)));
    }

    private static BatteryCellItem cellItem(ItemStack stack) {
        return stack.getItem() instanceof BatteryCellItem cell && !RecyclingData.isStripped(stack) ? cell : null;
    }

    public static void rollTraitsIfMissing(ItemStack stack, Level level) {
        CraftedTraitOutputs.rollIfMissing(stack, level.random);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static int positiveSaturatedRound(double value) {
        if (Double.isNaN(value) || value <= 1.0) {
            return 1;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.round(value));
    }

    private static final class StackEnergyStorage implements IEnergyStorage {
        private final ItemStack stack;

        private StackEnergyStorage(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return BatteryCellItem.receiveEnergy(stack, stats(stack), toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return BatteryCellItem.extractEnergy(stack, stats(stack), toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStored(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity(stack);
        }

        @Override
        public boolean canExtract() {
            return maxOutput(stack) > 0;
        }

        @Override
        public boolean canReceive() {
            return maxInput(stack) > 0;
        }
    }

    /** A cell's stats as its holder reads them; {@link #NONE} for a missing or stripped cell. */
    public record CellStats(int capacity, int maxInput, int maxOutput, double efficiency, double idleLossPercentPerMinute) {
        public static final CellStats NONE = new CellStats(0, 0, 0, 1.0, 0.0);
    }
}
