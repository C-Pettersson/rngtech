package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AmmoniaSynthesizerBlockEntity;
import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Deep Tanks resize a machine's tanks, and a Studious roll on Gear speeds the whole machine's Mastery XP. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SharedAffixGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private SharedAffixGameTests() {
    }

    /** 8,000 mB base plus 2,000 flat, then 50% increased: every Synthesizer tank holds 15,000 mB. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 40)
    public static void deepTanksResizeAmmoniaSynthesizerTanks(GameTestHelper helper) {
        AmmoniaSynthesizerBlockEntity synthesizer = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.AMMONIA_SYNTHESIZER.get(), AmmoniaSynthesizerBlockEntity.class);
        synthesizer.setMachineTraits(new MachineTraits(Rarity.MAGIC, 0, List.of(
                new MachineModifier(ModifierSlot.PREFIX, MachineStat.FLUID_CAPACITY, ModifierOperation.ADD, 2000),
                new MachineModifier(ModifierSlot.SUFFIX, MachineStat.FLUID_CAPACITY, ModifierOperation.INCREASED_PERCENT, 50))));
        helper.runAtTickTime(2, () -> {
            IFluidHandler fluids = synthesizer.getFluidHandler(Direction.NORTH);
            check(fluids.getTankCapacity(0) == 15000, "The nitrogen tank should hold 15,000 mB, was " + fluids.getTankCapacity(0));
            int filled = fluids.fill(new FluidStack(ModFluids.NITROGEN_SOURCE.get(), 12000), IFluidHandler.FluidAction.EXECUTE);
            check(filled == 12000, "A boosted tank should take 12,000 mB of nitrogen, took " + filled);
            helper.succeed();
        });
    }

    /** The Cavitation Generator's output tank keeps twice its input tank when Fluid Capacity grows. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 40)
    public static void deepTanksKeepCavitationOutputAtTwiceTheInput(GameTestHelper helper) {
        CavitationGeneratorBlockEntity generator = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.CAVITATION_GENERATOR.get(), CavitationGeneratorBlockEntity.class);
        generator.setMachineTraits(new MachineTraits(Rarity.MAGIC, 0, List.of(
                new MachineModifier(ModifierSlot.PREFIX, MachineStat.FLUID_CAPACITY, ModifierOperation.ADD, 4000))));
        helper.runAtTickTime(2, () -> {
            IFluidHandler fluids = generator.getFluidHandler(Direction.NORTH);
            check(fluids.getTankCapacity(0) == 8000, "The input tank should hold 8,000 mB, was " + fluids.getTankCapacity(0));
            check(fluids.getTankCapacity(1) == 16000, "The output tank should hold 16,000 mB, was " + fluids.getTankCapacity(1));
            helper.succeed();
        });
    }

    /** A Studious Heat Core is machine-wide: the Furnace earns 25% more Mastery XP. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void studiousHeatCoreSpeedsFurnaceMastery(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.STEEL).get(), FurnaceBlockEntity.class);
        ItemStack core = new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get());
        core.set(ModDataComponents.MACHINE_TRAITS.get(), new MachineTraits(Rarity.MAGIC, 0, List.of(
                new MachineModifier(ModifierSlot.SUFFIX, MachineStat.MASTERY_XP_GAIN, ModifierOperation.INCREASED_PERCENT, 25))));
        furnace.getGearInventory().setStackInSlot(0, core);
        check(Math.abs(furnace.masteryXpGain() - 1.25) < 1.0E-9, "The Furnace should gain XP 1.25x faster, was " + furnace.masteryXpGain());
        long before = furnace.machineProgression().xp();
        furnace.grantMasteryXp(100, 4);
        long gained = furnace.machineProgression().xp() - before;
        check(gained == 125, "A 100 XP grant should give 125 XP, gave " + gained);
        helper.succeed();
    }
}
