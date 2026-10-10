package com.rngtech.rpg.refinement;

import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BatteryChassisBlockItem;
import com.rngtech.content.item.ComponentRecyclerBlockItem;
import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.item.CrusherChassisBlockItem;
import com.rngtech.content.item.DisassemblyHeadItem;
import com.rngtech.content.item.FluidPumpItem;
import com.rngtech.content.item.ForestryCartItem;
import com.rngtech.content.item.FurnaceChassisBlockItem;
import com.rngtech.content.item.MachineBlockItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.MinersCompanionItem;
import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerBlockItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.item.ToolHeadItem;
import com.rngtech.content.item.ToolRodItem;
import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierEligibilityProfile;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.corruption.CorruptionCatalog;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class RefinementTargets {
    public static boolean canRefine(ItemStack stack) {
        return !RecyclingData.isStripped(stack)
                && !CraftedTraitOutputs.isUnidentified(stack)
                && (stack.getItem() instanceof MachineBlockItem
                || stack.getItem() instanceof MachinePartItem
                || stack.getItem() instanceof BatteryCellItem
                || stack.getItem() instanceof MinersCompanionItem
                || stack.getItem() instanceof ForestryCartItem
                || stack.getItem() instanceof ToolHeadItem
                || stack.getItem() instanceof ToolRodItem);
    }

    /**
     * Whether a Volatile Catalyst may target this stack: finished parts and cells with a corruption pool. Machine and
     * chassis items, tool parts, companions, unidentified and recycling-stripped stacks are not corrupted as items;
     * placed machines are corrupted through their Refinement tabs instead.
     */
    public static boolean canCorrupt(ItemStack stack) {
        return canRefine(stack)
                && (stack.getItem() instanceof MachinePartItem || stack.getItem() instanceof BatteryCellItem)
                && CorruptionCatalog.active().canCorrupt(eligibilityProfile(stack).id());
    }

    public static boolean isCorrupted(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits != null && traits.isCorrupted();
    }

    public static MachineType machineType(ItemStack stack) {
        if (stack.getItem() instanceof BatteryCellItem cell) {
            return cell.machineType();
        }
        if (stack.getItem() instanceof MinersCompanionItem) {
            return MachineType.MINERS_COMPANION;
        }
        if (stack.getItem() instanceof ForestryCartItem) {
            return MachineType.FORESTRY_COMPANION;
        }
        if (stack.getItem() instanceof MachineBlockItem machine) {
            return machine.machineType();
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            return part.machineType();
        }
        if (stack.getItem() instanceof ToolHeadItem) {
            return MachineType.TOOL_HEAD;
        }
        if (stack.getItem() instanceof ToolRodItem) {
            return MachineType.TOOL_ROD;
        }
        throw new IllegalArgumentException("Item stack is not a refinement target: " + stack);
    }

    public static ModifierEligibilityProfile eligibilityProfile(ItemStack stack) {
        if (stack.getItem() instanceof BatteryCellItem cell) {
            return ModifierEligibilityProfiles.forBatteryCell(cell.material().unique());
        }
        if (stack.getItem() instanceof MinersCompanionItem) {
            return ModifierEligibilityProfiles.forMachine(MachineType.MINERS_COMPANION);
        }
        if (stack.getItem() instanceof ForestryCartItem) {
            return ModifierEligibilityProfiles.forMachine(MachineType.FORESTRY_COMPANION);
        }
        if (stack.getItem() instanceof MachineBlockItem machine) {
            return ModifierEligibilityProfiles.forMachine(machine.machineType());
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            return ModifierEligibilityProfiles.forMachinePart(part.partType(), part.machineType());
        }
        if (stack.getItem() instanceof ToolHeadItem head) {
            return ModifierEligibilityProfiles.forToolHead(head.family() == ToolHeadFamily.PICK);
        }
        if (stack.getItem() instanceof ToolRodItem) {
            return ModifierEligibilityProfiles.forMachine(MachineType.TOOL_ROD);
        }
        throw new IllegalArgumentException("Item stack is not a refinement target: " + stack);
    }

    public static int componentStage(ItemStack stack) {
        if (stack.getItem() instanceof BatteryCellItem cell) {
            return cell.material().stage();
        }
        if (stack.getItem() instanceof MinersCompanionItem) {
            return MinersCompanionItem.COMPONENT_STAGE;
        }
        if (stack.getItem() instanceof ForestryCartItem) {
            return ForestryCartItem.COMPONENT_STAGE;
        }
        if (stack.getItem() instanceof CrusherChassisBlockItem chassis) {
            return chassis.material().stage();
        }
        if (stack.getItem() instanceof FurnaceChassisBlockItem chassis) {
            return chassis.material().stage();
        }
        if (stack.getItem() instanceof BatteryChassisBlockItem chassis) {
            return chassis.material().stage();
        }
        if (stack.getItem() instanceof SolidFuelBurnerBlockItem burner) {
            return burner.chassis().stage();
        }
        if (stack.getItem() instanceof ComponentRecyclerBlockItem recycler) {
            return recycler.chassis().stage();
        }
        if (stack.getItem() instanceof CrushHeadItem head) {
            return head.material().stage();
        }
        if (stack.getItem() instanceof SolidFuelBurnerPartItem part) {
            return part.stage();
        }
        if (stack.getItem() instanceof PotentialReactorPartItem part) {
            return part.stage();
        }
        if (stack.getItem() instanceof DisassemblyHeadItem head) {
            return head.stage();
        }
        if (stack.getItem() instanceof ServoItem servo) {
            return servo.stage();
        }
        if (stack.getItem() instanceof FluidPumpItem pump) {
            return pump.stage();
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            return part.recyclingComponentStage();
        }
        if (stack.getItem() instanceof MachineBlockItem machine) {
            return machine.recyclingComponentStage();
        }
        if (stack.getItem() instanceof ToolHeadItem head) {
            return head.material().stage();
        }
        if (stack.getItem() instanceof ToolRodItem rod) {
            return rod.material().stage();
        }
        return 0;
    }

    public static int modifierRollComponentStage(ItemStack stack) {
        if (stack.getItem() instanceof MachineBlockItem machine) {
            return machine.traitRollComponentStage();
        }
        return componentStage(stack);
    }

    public static MachineTraits traits(ItemStack stack) {
        if (stack.getItem() instanceof BatteryCellItem) {
            return BatteryCellItem.traits(stack);
        }
        if (stack.getItem() instanceof MinersCompanionItem) {
            return MinersCompanionItem.traits(stack);
        }
        if (stack.getItem() instanceof ForestryCartItem) {
            return ForestryCartItem.traits(stack);
        }
        if (stack.getItem() instanceof ToolHeadItem head) {
            return head.traits(stack);
        }
        if (stack.getItem() instanceof ToolRodItem rod) {
            return rod.traits(stack);
        }
        if (ModularToolItem.isModularTool(stack)) {
            return MachineTraits.EMPTY;
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            return part.traits(stack);
        }

        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return MachineImplicitCatalog.effectiveTraits(traits == null ? MachineTraits.EMPTY : traits, stack);
    }

    public static MachineTraits storedTraits(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        if (traits == null && stack.getItem() instanceof BatteryCellItem cell && cell.material().unique()) {
            return new MachineTraits(com.rngtech.rpg.Rarity.UNIQUE, 0, java.util.List.of());
        }
        if (traits == null && stack.getItem() instanceof MinersCompanionItem) {
            return new MachineTraits(
                    Rarity.NORMAL,
                    MachineTraitRoller.refinementPotentialRange(
                            ModifierEligibilityProfiles.forMachine(MachineType.MINERS_COMPANION),
                            MinersCompanionItem.COMPONENT_STAGE
                    ).max(),
                    List.of()
            );
        }
        if (traits == null && stack.getItem() instanceof ForestryCartItem) {
            return new MachineTraits(
                    Rarity.NORMAL,
                    MachineTraitRoller.refinementPotentialRange(
                            ModifierEligibilityProfiles.forMachine(MachineType.FORESTRY_COMPANION),
                            ForestryCartItem.COMPONENT_STAGE
                    ).max(),
                    List.of()
            );
        }
        if (traits == null && stack.getItem() instanceof MachinePartItem part) {
            return new MachineTraits(part.modifierSet().rarity(), part.modifierSet().refinementPotential(), java.util.List.of());
        }
        if (traits == null && stack.getItem() instanceof ToolHeadItem head) {
            return unrolledToolTraits(head.material().stage());
        }
        if (traits == null && stack.getItem() instanceof ToolRodItem rod) {
            return unrolledToolTraits(rod.material().stage());
        }
        if (ModularToolItem.isModularTool(stack)) {
            return MachineTraits.EMPTY;
        }
        return MachineImplicitCatalog.storedTraits(traits == null ? MachineTraits.EMPTY : traits);
    }

    public static void setTraits(ItemStack stack, MachineTraits traits) {
        stack.set(ModDataComponents.MACHINE_TRAITS.get(), traits);
    }

    private RefinementTargets() {
    }

    private static MachineTraits unrolledToolTraits(int stage) {
        return new MachineTraits(
                Rarity.NORMAL,
                MachineTraitRoller.refinementPotentialRange(
                        ModifierEligibilityProfiles.forMachine(MachineType.MODULAR_TOOL),
                        stage
                ).max(),
                List.of()
        );
    }
}
