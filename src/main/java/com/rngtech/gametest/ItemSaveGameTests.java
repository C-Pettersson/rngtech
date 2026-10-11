package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Item stacks keep their machine data through a save and load, and the Component Recycler's stripped flag with it. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemSaveGameTests {
    private static final MachineTraits TRAITS = new MachineTraits(Rarity.RARE, 2, List.of(
            new MachineModifier(ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 40)
    ));

    private ItemSaveGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void machineItemKeepsItsDataThroughASave(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModBlocks.crusherChassis(CrusherChassisMaterial.WOODEN).get());
        stack.set(ModDataComponents.MACHINE_TRAITS.get(), TRAITS);
        stack.set(ModDataComponents.MACHINE_PROGRESSION.get(), MachineProgressionState.EMPTY.withAddedXp(500));

        ItemStack loaded = roundTrip(helper, stack);
        check(TRAITS.equals(loaded.get(ModDataComponents.MACHINE_TRAITS.get())), "The stack lost its traits");
        check(stack.get(ModDataComponents.MACHINE_PROGRESSION.get()).equals(loaded.get(ModDataComponents.MACHINE_PROGRESSION.get())),
                "The stack lost its progression");
        check(!RecyclingData.isStripped(loaded), "A stack that was not stripped loaded as stripped");
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void strippedFlagSurvivesASaveAndDropsTheRolls(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModBlocks.crusherChassis(CrusherChassisMaterial.WOODEN).get());
        stack.set(ModDataComponents.MACHINE_TRAITS.get(), TRAITS);
        stack.set(ModDataComponents.MACHINE_PROGRESSION.get(), MachineProgressionState.EMPTY.withAddedXp(500));

        ItemStack stripped = RecyclingData.strippedCopy(stack);
        check(RecyclingData.isStripped(stripped), "The stripped copy is not marked stripped");
        ItemStack loaded = roundTrip(helper, stripped);
        check(RecyclingData.isStripped(loaded), "The stripped flag was lost in a save and load");
        check(!loaded.has(ModDataComponents.MACHINE_TRAITS.get()), "A stripped stack came back with traits");
        check(!loaded.has(ModDataComponents.MACHINE_PROGRESSION.get()), "A stripped stack came back with progression");
        helper.succeed();
    }

    private static ItemStack roundTrip(GameTestHelper helper, ItemStack stack) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        Tag saved = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        return ItemStack.CODEC.parse(ops, saved).getOrThrow();
    }
}
