package com.rngtech.content.item;

import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.Rarity;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public final class CraftedTraitOutputs {
    public static ItemStack seedIfMissing(ItemStack stack, RandomSource random) {
        if (!canReceiveCraftedTraits(stack)
                || stack.has(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get())) {
            return stack;
        }
        stack.set(
                ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get(),
                UnidentifiedTraitRoll.unseededCrafting(targetKey(stack), componentStage(stack))
        );
        return stack;
    }

    public static ItemStack rollIfMissing(ItemStack stack, RandomSource random) {
        if (stack.isEmpty() || RecyclingData.isStripped(stack) || stack.has(ModDataComponents.MACHINE_TRAITS.get())) {
            return stack;
        }

        UnidentifiedTraitRoll pendingRoll = stack.get(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get());
        RandomSource rollRandom = pendingRoll == null || pendingRoll.isUnseeded()
                ? random
                : RandomSource.create(pendingRoll.seed());
        if (applyRolledTraits(stack, rollRandom)) {
            stack.remove(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get());
            stack.remove(ModDataComponents.IDENTIFY_TRAIT_ROLL_ON_CRAFT.get());
        }
        return stack;
    }

    public static ItemStack handleCrafted(ItemStack stack, RandomSource random) {
        if (stack.getOrDefault(ModDataComponents.IDENTIFY_TRAIT_ROLL_ON_CRAFT.get(), false)) {
            return rollIfMissing(stack, random);
        }
        return seedIfMissing(stack, random);
    }

    public static ItemStack prepareIdentification(ItemStack stack) {
        if (isUnidentified(stack)) {
            stack.set(ModDataComponents.IDENTIFY_TRAIT_ROLL_ON_CRAFT.get(), true);
        }
        return stack;
    }

    public static boolean isUnidentified(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.has(ModDataComponents.MACHINE_TRAITS.get())
                && stack.has(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get());
    }

    public static boolean canReceiveCraftedTraits(ItemStack stack) {
        if (stack.isEmpty()
                || RecyclingData.isStripped(stack)
                || stack.has(ModDataComponents.MACHINE_TRAITS.get())) {
            return false;
        }
        if (stack.getItem() instanceof ComponentRecyclerBlockItem recycler && recycler.chassis().manual()) {
            return false;
        }
        return stack.getItem() instanceof MachineBlockItem
                || stack.getItem() instanceof MachinePartItem
                || stack.getItem() instanceof BatteryCellItem
                || stack.getItem() instanceof MinersCompanionItem
                || stack.getItem() instanceof ForestryCartItem
                || stack.getItem() instanceof ToolHeadItem
                || stack.getItem() instanceof ToolRodItem;
    }

    public static ItemStack unidentifiedVariant(ItemStack stack) {
        ItemStack variant = stack.copyWithCount(1);
        variant.remove(ModDataComponents.MACHINE_TRAITS.get());
        variant.remove(ModDataComponents.IDENTIFY_TRAIT_ROLL_ON_CRAFT.get());
        if (canReceiveCraftedTraits(variant)) {
            variant.set(
                    ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get(),
                    UnidentifiedTraitRoll.unseededCrafting(targetKey(variant), componentStage(variant))
            );
        }
        return variant;
    }

    public static Component unidentifiedName(Component baseName) {
        return Component.translatable("rngtech.item.unidentified", baseName);
    }

    private static boolean applyRolledTraits(ItemStack stack, RandomSource random) {
        if (stack.getItem() instanceof MachineBlockItem machine) {
            stack.set(
                    ModDataComponents.MACHINE_TRAITS.get(),
                    MachineTraitRoller.roll(
                            ModifierEligibilityProfiles.forMachine(machine.machineType()),
                            machine.traitRollComponentStage(),
                            random
                    )
            );
            return true;
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            MachineTraits rolled = MachineTraitRoller.roll(
                    ModifierEligibilityProfiles.forMachinePart(part.partType(), part.machineType()),
                    part.componentStage(),
                    random
            );
            stack.set(
                    ModDataComponents.MACHINE_TRAITS.get(),
                    new MachineTraits(
                            rolled.rarity(),
                            part.modifierSet().refinementPotential() + rolled.refinementPotential(),
                            rolled.modifiers()
                    )
            );
            return true;
        }
        if (stack.getItem() instanceof BatteryCellItem cell) {
            stack.set(
                    ModDataComponents.MACHINE_TRAITS.get(),
                    cell.material().unique()
                            ? new MachineTraits(Rarity.UNIQUE, 0, List.of())
                            : MachineTraitRoller.roll(
                                    ModifierEligibilityProfiles.forBatteryCell(false),
                                    cell.material().stage(),
                                    random
                            )
            );
            return true;
        }
        if (stack.getItem() instanceof MinersCompanionItem) {
            stack.set(
                    ModDataComponents.MACHINE_TRAITS.get(),
                    MachineTraitRoller.roll(
                            ModifierEligibilityProfiles.forMachine(MachineType.MINERS_COMPANION),
                            MinersCompanionItem.COMPONENT_STAGE,
                            random
                    )
            );
            return true;
        }
        if (stack.getItem() instanceof ForestryCartItem) {
            stack.set(
                    ModDataComponents.MACHINE_TRAITS.get(),
                    MachineTraitRoller.roll(
                            ModifierEligibilityProfiles.forMachine(MachineType.FORESTRY_COMPANION),
                            ForestryCartItem.COMPONENT_STAGE,
                            random
                    )
            );
            return true;
        }
        if (stack.getItem() instanceof ToolHeadItem head) {
            stack.set(
                    ModDataComponents.MACHINE_TRAITS.get(),
                    MachineTraitRoller.roll(
                            ModifierEligibilityProfiles.forToolHead(head.family() == ToolHeadFamily.PICK),
                            head.material().stage(),
                            random
                    )
            );
            return true;
        }
        if (stack.getItem() instanceof ToolRodItem rod) {
            stack.set(
                    ModDataComponents.MACHINE_TRAITS.get(),
                    MachineTraitRoller.roll(
                            ModifierEligibilityProfiles.forMachine(MachineType.TOOL_ROD),
                            rod.material().stage(),
                            random
                    )
            );
            return true;
        }
        return false;
    }

    private static int componentStage(ItemStack stack) {
        if (stack.getItem() instanceof BatteryCellItem cell) {
            return cell.material().stage();
        }
        if (stack.getItem() instanceof MinersCompanionItem) {
            return MinersCompanionItem.COMPONENT_STAGE;
        }
        if (stack.getItem() instanceof ForestryCartItem) {
            return ForestryCartItem.COMPONENT_STAGE;
        }
        if (stack.getItem() instanceof MachineBlockItem machine) {
            return machine.traitRollComponentStage();
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            return part.componentStage();
        }
        if (stack.getItem() instanceof ToolHeadItem head) {
            return head.material().stage();
        }
        if (stack.getItem() instanceof ToolRodItem rod) {
            return rod.material().stage();
        }
        return 0;
    }

    private static String targetKey(ItemStack stack) {
        if (stack.getItem() instanceof BatteryCellItem cell) {
            return key(cell.machineType().name());
        }
        if (stack.getItem() instanceof MinersCompanionItem) {
            return key(MachineType.MINERS_COMPANION.name());
        }
        if (stack.getItem() instanceof ForestryCartItem) {
            return key(MachineType.FORESTRY_COMPANION.name());
        }
        if (stack.getItem() instanceof MachineBlockItem machine) {
            return key(machine.machineType().name());
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            return key(part.machineType().name()) + "/" + key(part.partType().name());
        }
        if (stack.getItem() instanceof ToolHeadItem) {
            return key(MachineType.TOOL_HEAD.name());
        }
        if (stack.getItem() instanceof ToolRodItem) {
            return key(MachineType.TOOL_ROD.name());
        }
        return "";
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private CraftedTraitOutputs() {
    }
}
