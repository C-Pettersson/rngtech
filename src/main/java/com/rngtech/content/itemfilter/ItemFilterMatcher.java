package com.rngtech.content.itemfilter;

import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.item.AdvancedItemFilterItem;
import com.rngtech.content.item.AlloyCrucibleItem;
import com.rngtech.content.item.AmmoniaPartItem;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BioChamberItem;
import com.rngtech.content.item.CalibrationGearItem;
import com.rngtech.content.item.CavitationPartItem;
import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.item.DisassemblyHeadItem;
import com.rngtech.content.item.FluidPumpItem;
import com.rngtech.content.item.GasChemistryPartItem;
import com.rngtech.content.item.MachineBlockItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.MaterialItem;
import com.rngtech.content.item.MinersCompanionItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolarArrayExtenderItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.item.ToolHeadItem;
import com.rngtech.content.item.ToolRodItem;
import com.rngtech.content.item.VacuumCollapsePartItem;
import com.rngtech.content.material.MaterialCatalog;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

public final class ItemFilterMatcher {
    public static boolean passesConnectorFilters(List<ItemStack> filters, ItemStack candidate) {
        if (candidate.isEmpty()) {
            return true;
        }
        return passesFilters(filters, true, new CandidateMatcher() {
            @Override
            public boolean normalMatches(ItemStack filter) {
                return ItemStack.isSameItemSameComponents(filter, candidate);
            }

            @Override
            public boolean advancedMatches(AdvancedItemFilterSettings settings) {
                return matchesAdvanced(settings, candidate);
            }
        });
    }

    public static boolean passesCompanionFilters(List<ItemStack> filters, ItemStack dropStack, Item brokenBlockItem) {
        if (dropStack.isEmpty() && brokenBlockItem == Items.AIR) {
            return false;
        }
        ItemStack brokenBlockStack = brokenBlockItem == Items.AIR ? ItemStack.EMPTY : new ItemStack(brokenBlockItem);
        return passesFilters(filters, false, new CandidateMatcher() {
            @Override
            public boolean normalMatches(ItemStack filter) {
                if (!dropStack.isEmpty() && ItemStack.isSameItem(filter, dropStack)) {
                    return true;
                }
                return !brokenBlockStack.isEmpty() && filter.is(brokenBlockItem);
            }

            @Override
            public boolean advancedMatches(AdvancedItemFilterSettings settings) {
                if (!dropStack.isEmpty() && matchesAdvanced(settings, dropStack)) {
                    return true;
                }
                return !brokenBlockStack.isEmpty() && matchesAdvanced(settings, brokenBlockStack);
            }
        });
    }

    public static boolean matchesAdvanced(AdvancedItemFilterSettings settings, ItemStack candidate) {
        if (candidate.isEmpty() || !settings.hasActiveCriteria()) {
            return false;
        }
        if (settings.exactItemsEnabled() && !matchesSample(settings, candidate)) {
            return false;
        }
        if (settings.tagEnabled() && !matchesTag(settings, candidate)) {
            return false;
        }
        if (settings.namespaceEnabled() && !matchesNamespace(settings, candidate)) {
            return false;
        }
        if (settings.stageEnabled() && !matchesStage(settings, candidate)) {
            return false;
        }
        if (settings.stabilityEnabled() && !matchesStability(settings, candidate)) {
            return false;
        }
        if (!matchesIdentity(settings, candidate)) {
            return false;
        }
        return matchesRarity(settings, candidate);
    }

    private static boolean passesFilters(List<ItemStack> filters, boolean emptyResult, CandidateMatcher matcher) {
        List<ItemStack> activeFilters = activeFilters(filters);
        if (activeFilters.isEmpty()) {
            return emptyResult;
        }

        for (ItemStack filter : activeFilters) {
            if (filter.getItem() instanceof AdvancedItemFilterItem) {
                AdvancedItemFilterSettings settings = settings(filter);
                if (settings.mode() == AdvancedItemFilterSettings.Mode.DENY && matcher.advancedMatches(settings)) {
                    return false;
                }
            }
        }

        boolean hasAllow = false;
        for (ItemStack filter : activeFilters) {
            if (filter.getItem() instanceof AdvancedItemFilterItem) {
                AdvancedItemFilterSettings settings = settings(filter);
                if (settings.mode() == AdvancedItemFilterSettings.Mode.ALLOW) {
                    hasAllow = true;
                    if (matcher.advancedMatches(settings)) {
                        return true;
                    }
                }
            } else {
                hasAllow = true;
                if (matcher.normalMatches(filter)) {
                    return true;
                }
            }
        }
        return !hasAllow;
    }

    private static AdvancedItemFilterSettings settings(ItemStack filter) {
        return filter.getOrDefault(ModDataComponents.ADVANCED_ITEM_FILTER.get(), AdvancedItemFilterSettings.EMPTY);
    }

    private static List<ItemStack> activeFilters(List<ItemStack> filters) {
        List<ItemStack> active = new ArrayList<>();
        if (filters != null) {
            for (ItemStack filter : filters) {
                if (filter != null && !filter.isEmpty()) {
                    active.add(filter);
                }
            }
        }
        return active;
    }

    private static boolean matchesSample(AdvancedItemFilterSettings settings, ItemStack candidate) {
        for (ItemStack sample : settings.samples()) {
            if (sample.isEmpty()) {
                continue;
            }
            if (settings.strictComponents()
                    ? ItemStack.isSameItemSameComponents(sample, candidate)
                    : ItemStack.isSameItem(sample, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesTag(AdvancedItemFilterSettings settings, ItemStack candidate) {
        for (net.minecraft.resources.ResourceLocation tag : settings.tags()) {
            if (candidate.is(TagKey.create(Registries.ITEM, tag))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesNamespace(AdvancedItemFilterSettings settings, ItemStack candidate) {
        String namespace = BuiltInRegistries.ITEM.getKey(candidate.getItem()).getNamespace().toLowerCase(Locale.ROOT);
        return settings.namespaces().contains(namespace);
    }

    private static boolean matchesStage(AdvancedItemFilterSettings settings, ItemStack candidate) {
        OptionalInt stage = componentStage(candidate);
        return stage.isPresent() && stage.getAsInt() >= settings.minStage() && stage.getAsInt() <= settings.maxStage();
    }

    private static boolean matchesStability(AdvancedItemFilterSettings settings, ItemStack candidate) {
        OptionalInt stability = stabilityPercent(candidate);
        return stability.isPresent()
                && stability.getAsInt() >= settings.minStability()
                && stability.getAsInt() <= settings.maxStability();
    }

    private static boolean matchesIdentity(AdvancedItemFilterSettings settings, ItemStack candidate) {
        return switch (settings.identityMode()) {
            case ANY -> true;
            case IDENTIFIED -> isTraitTarget(candidate) && !CraftedTraitOutputs.isUnidentified(candidate);
            case UNIDENTIFIED -> CraftedTraitOutputs.isUnidentified(candidate);
        };
    }

    private static boolean matchesRarity(AdvancedItemFilterSettings settings, ItemStack candidate) {
        if (settings.rarityMode() == AdvancedItemFilterSettings.RarityMode.ANY) {
            return true;
        }
        if (!isTraitTarget(candidate) || CraftedTraitOutputs.isUnidentified(candidate)) {
            return false;
        }
        MachineTraits traits = RefinementTargets.storedTraits(candidate);
        return traits.rarity() == rarity(settings.rarityMode());
    }

    private static Rarity rarity(AdvancedItemFilterSettings.RarityMode mode) {
        return switch (mode) {
            case NORMAL -> Rarity.NORMAL;
            case MAGIC -> Rarity.MAGIC;
            case RARE -> Rarity.RARE;
            case UNIQUE -> Rarity.UNIQUE;
            case ANY -> Rarity.NORMAL;
        };
    }

    private static boolean isTraitTarget(ItemStack stack) {
        return stack.has(ModDataComponents.MACHINE_TRAITS.get())
                || stack.has(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get())
                || stack.getItem() instanceof MachineBlockItem
                || stack.getItem() instanceof MachinePartItem
                || stack.getItem() instanceof BatteryCellItem
                || stack.getItem() instanceof MinersCompanionItem
                || stack.getItem() instanceof ToolHeadItem
                || stack.getItem() instanceof ToolRodItem;
    }

    private static OptionalInt componentStage(ItemStack stack) {
        CalibrationState calibration = stack.get(ModDataComponents.CALIBRATION_STATE.get());
        if (calibration != null) {
            return OptionalInt.of(calibration.stage());
        }
        Item item = stack.getItem();
        if (item instanceof MaterialItem materialItem && materialItem.definition().belongsToMaterialFamily()) {
            try {
                return OptionalInt.of(MaterialCatalog.materialFamily(materialItem.definition().materialId()).stage());
            } catch (IllegalArgumentException ignored) {
                return OptionalInt.empty();
            }
        }
        if (item instanceof BatteryCellItem cell) {
            return OptionalInt.of(cell.material().stage());
        }
        if (item instanceof MinersCompanionItem) {
            return OptionalInt.of(MinersCompanionItem.COMPONENT_STAGE);
        }
        if (item instanceof MachineBlockItem machine) {
            return OptionalInt.of(machine.recyclingComponentStage());
        }
        if (item instanceof ToolHeadItem head) {
            return OptionalInt.of(head.material().stage());
        }
        if (item instanceof ToolRodItem rod) {
            return OptionalInt.of(rod.material().stage());
        }
        if (item instanceof AlloyCrucibleItem crucible) {
            return OptionalInt.of(crucible.stage());
        }
        if (item instanceof AmmoniaPartItem part) {
            return OptionalInt.of(part.stage());
        }
        if (item instanceof BioChamberItem) {
            return OptionalInt.of(1);
        }
        if (item instanceof CalibrationGearItem gear) {
            return OptionalInt.of(gear.stage());
        }
        if (item instanceof CavitationPartItem part) {
            return OptionalInt.of(part.stage());
        }
        if (item instanceof DisassemblyHeadItem head) {
            return OptionalInt.of(head.stage());
        }
        if (item instanceof FluidPumpItem pump) {
            return OptionalInt.of(pump.stage());
        }
        if (item instanceof GasChemistryPartItem part) {
            return OptionalInt.of(part.stage());
        }
        if (item instanceof ServoItem servo) {
            return OptionalInt.of(servo.stage());
        }
        if (item instanceof SolarArrayExtenderItem extender) {
            return OptionalInt.of(extender.material().stage());
        }
        if (item instanceof SolidFuelBurnerPartItem part) {
            return OptionalInt.of(part.stage());
        }
        if (item instanceof VacuumCollapsePartItem part) {
            return OptionalInt.of(part.stage());
        }
        if (item instanceof MachinePartItem) {
            return OptionalInt.of(RefinementTargets.componentStage(stack));
        }
        return OptionalInt.empty();
    }

    private static OptionalInt stabilityPercent(ItemStack stack) {
        CalibrationState calibration = stack.get(ModDataComponents.CALIBRATION_STATE.get());
        if (calibration != null) {
            return OptionalInt.of(calibration.stability());
        }
        MachineStatAccumulator componentStats = ComponentBaseStatCatalog.effectiveStats(stack);
        if (componentStats != null && ComponentBaseStatCatalog.summaryStats(stack).contains(MachineStat.STABILITY)) {
            return OptionalInt.of(percent(componentStats.value(MachineStat.STABILITY)));
        }
        MachineStatAccumulator machineStats = MachineBaseStatCatalog.forStack(stack);
        if (machineStats != null && MachineBaseStatCatalog.summaryStats(stack).contains(MachineStat.STABILITY)) {
            machineStats.apply(RefinementTargets.traits(stack));
            return OptionalInt.of(percent(machineStats.value(MachineStat.STABILITY)));
        }
        return OptionalInt.empty();
    }

    private static int percent(double value) {
        return (int) Math.round(value * 100.0D);
    }

    private interface CandidateMatcher {
        boolean normalMatches(ItemStack filter);

        boolean advancedMatches(AdvancedItemFilterSettings settings);
    }

    private ItemFilterMatcher() {
    }
}
