package com.rngtech.content.recycling;

import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BatteryChassisBlockItem;
import com.rngtech.content.item.ComponentRecyclerBlockItem;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.item.CrusherChassisBlockItem;
import com.rngtech.content.item.FluidPumpItem;
import com.rngtech.content.item.FurnaceChassisBlockItem;
import com.rngtech.content.item.MachineBlockItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.PotentialReactorBlockItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.content.item.ResonanceCalibratorBlockItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerBlockItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.world.item.ItemStack;

public final class RecyclingData {
    private static final double STAGE_ZERO_FUEL_SCALE = 0.25;
    private static final double FUEL_SCALE_PER_STAGE = 0.25;
    private static final double FUEL_SCALE_PER_MASTERY_LEVEL = 0.01;
    private static final double FUEL_SCALE_PER_SEAL_TIER = 2.0;

    public static boolean isStripped(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RECYCLING_STRIPPED.get(), false);
    }

    public static boolean hasRecyclableRpgValue(ItemStack stack) {
        MachineTraits traits = recyclableTraits(stack);
        if (traits.isEmpty()) {
            return false;
        }
        if (traits.rarity() == Rarity.UNIQUE) {
            return false;
        }
        return traits.rarity() != Rarity.NORMAL
                || traits.refinementPotential() > 0
                || traits.modifiers().stream().anyMatch(modifier -> modifier.slot().isAffix());
    }

    public static int rpgEnergyValue(ItemStack stack) {
        MachineTraits traits = recyclableTraits(stack);
        if (traits.isEmpty() || traits.rarity() == Rarity.UNIQUE) {
            return 0;
        }

        int value = rarityValue(traits.rarity()) + traits.refinementPotential() * 120;
        for (MachineModifier modifier : traits.modifiers()) {
            if (!modifier.slot().isAffix()) {
                continue;
            }
            value += switch (Math.max(1, modifier.tier())) {
                case 1 -> 500;
                case 2 -> 1400;
                case 3 -> 4000;
                default -> 9000;
            };
        }
        return Math.max(0, value);
    }

    /** FE a Potential Reactor gets from burning this item, before reactor stats and repeat fatigue. */
    public static int reactorFuelValue(ItemStack stack) {
        MachineProgressionState progression = stack.get(ModDataComponents.MACHINE_PROGRESSION.get());
        return reactorFuelValue(
                rpgEnergyValue(stack),
                componentStage(stack),
                progression == null ? 0 : progression.level() - 1,
                progression == null ? 0 : progression.sealTiers()
        );
    }

    /**
     * Trait value scaled so the reactor recoups retired gear rather than paying for junk: Stage 0 items keep a
     * quarter, each stage adds a quarter, each Mastery level adds 1%, and each Ascendancy Seal tier adds 200%.
     */
    public static int reactorFuelValue(int traitValue, int stage, int masteryLevels, int sealTiers) {
        double scale = (STAGE_ZERO_FUEL_SCALE + FUEL_SCALE_PER_STAGE * Math.max(0, stage))
                * (1.0 + FUEL_SCALE_PER_MASTERY_LEVEL * Math.max(0, masteryLevels))
                * (1.0 + FUEL_SCALE_PER_SEAL_TIER * Math.max(0, sealTiers));
        return (int) Math.round(Math.max(0, traitValue) * scale);
    }

    private static MachineTraits recyclableTraits(ItemStack stack) {
        if (stack.isEmpty() || isStripped(stack) || !RefinementTargets.canRefine(stack)) {
            return MachineTraits.EMPTY;
        }
        return RefinementTargets.storedTraits(stack);
    }

    public static ItemStack strippedCopy(ItemStack stack) {
        ItemStack stripped = stack.copy();
        stripped.setCount(1);
        stripped.remove(ModDataComponents.MACHINE_TRAITS.get());
        stripped.remove(ModDataComponents.MACHINE_PROGRESSION.get());
        stripped.remove(ModDataComponents.UNIDENTIFIED_TRAIT_ROLL.get());
        stripped.remove(ModDataComponents.IDENTIFY_TRAIT_ROLL_ON_CRAFT.get());
        stripped.remove(ModDataComponents.BATTERY_CELL_ENERGY.get());
        stripped.set(ModDataComponents.RECYCLING_STRIPPED.get(), true);
        return stripped;
    }

    public static int componentStage(ItemStack stack) {
        if (stack.getItem() instanceof BatteryCellItem cell) {
            return cell.material().stage();
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
        if (stack.getItem() instanceof ResonanceCalibratorBlockItem calibrator) {
            return calibrator.chassis().stage();
        }
        if (stack.getItem() instanceof PotentialReactorBlockItem) {
            return 3;
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
        if (stack.getItem() instanceof DisassemblyHeadItemAccess head) {
            return head.recyclingStage();
        }
        if (stack.getItem() instanceof ServoItem servo) {
            return servo.stage();
        }
        if (stack.getItem() instanceof FluidPumpItem pump) {
            return pump.stage();
        }
        int refinementStage = RefinementTargets.componentStage(stack);
        if (refinementStage > 0) {
            return refinementStage;
        }
        if (stack.getItem() instanceof MachineBlockItem machine) {
            return machine.recyclingComponentStage();
        }
        if (stack.getItem() instanceof MachinePartItem part) {
            return part.recyclingComponentStage();
        }
        return 0;
    }

    private static int rarityValue(Rarity rarity) {
        return switch (rarity) {
            case NORMAL -> 0;
            case MAGIC -> 1200;
            case RARE -> 3600;
            case UNIQUE -> 0;
        };
    }

    private RecyclingData() {
    }
}
