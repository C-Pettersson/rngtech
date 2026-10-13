package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AmmoniaSynthesizerBlockEntity;
import com.rngtech.content.blockentity.AmmoniaSynthesizerVisualState;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.blockentity.GasProcessorVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The shaped Coal Gasifier, Steam Methane Reformer, and Ammonia Synthesizer send their renderers what they show: whether
 * they run, their tank levels and held items, and their installed Gear. None of them occludes its neighbours.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasProcessorGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final int TANK = 8000;
    private static final int FILL = 4000;

    private GasProcessorGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void runningGasifierSyncsFeedTanksAndGear(GameTestHelper helper) {
        GasChemistryBlockEntity gasifier = MachineTestSupport.place(helper, MACHINE, ModBlocks.COAL_GASIFIER.get(), GasChemistryBlockEntity.class);
        gasifier.getGearInventory().insertItem(
                GasChemistryBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.ALUMINUM).get()), false);
        gasifier.getGearInventory().insertItem(GasChemistryBlockEntity.SLOT_SERVO, new ItemStack(ModItems.STEEL_SERVO.get()), false);
        gasifier.getGearInventory().insertItem(
                GasChemistryBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.SPARKSTEEL).get()), false);
        gasifier.getProcessInventory().insertItem(GasChemistryBlockEntity.SLOT_PROCESS_INPUT, new ItemStack(ModItems.materialItem("coal_dust").get(), 32), false);
        gasifier.getFluidHandler(Direction.NORTH).fill(new FluidStack(Fluids.WATER, FILL), IFluidHandler.FluidAction.EXECUTE);
        MachineTestSupport.powerEachTick(helper, gasifier.getEnergyStorage(Direction.NORTH));
        check(!gasifier.getBlockState().canOcclude(), "The shaped gasifier should not occlude its neighbours");
        check(gasifier.getUpdatePacket() != null, "The gasifier should send its visual state to clients");
        helper.runAtTickTime(60, () -> {
            GasProcessorVisualState state = GasProcessorVisualState.load(gasifier.getUpdateTag(helper.getLevel().registryAccess()));
            check(state.active(), "The gasifier should be running: " + MachineTestSupport.describe(gasifier));
            check(state.progress() > 0.0F, "The cycle progress should advance, was " + state.progress());
            check(state.water() > 0.0F && state.water() <= FILL / (float) TANK, "The water level should show the water left, was " + state.water());
            check(state.feed() > 0.0F, "The hopper should show the Coal Dust, was " + state.feed());
            check("aluminum".equals(state.heatCore()), "The Heat Core should sync as aluminum, was '" + state.heatCore() + "'");
            check("steel".equals(state.servo()), "The Servo should sync as steel, was '" + state.servo() + "'");
            check("sparksteel".equals(state.cell()), "The Battery Cell should sync as sparksteel, was '" + state.cell() + "'");
            check(!state.catalyst(), "The gasifier has no catalyst slot");
            helper.succeed();
        });
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void runningReformerSyncsFeedGasAndCatalyst(GameTestHelper helper) {
        GasChemistryBlockEntity reformer = MachineTestSupport.place(helper, MACHINE, ModBlocks.STEAM_METHANE_REFORMER.get(), GasChemistryBlockEntity.class);
        reformer.getGearInventory().insertItem(
                GasChemistryBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()), false);
        reformer.getGearInventory().insertItem(GasChemistryBlockEntity.SLOT_CATALYST_BED, new ItemStack(ModItems.REFORMING_CATALYST_BED.get()), false);
        IFluidHandler fluids = reformer.getFluidHandler(Direction.NORTH);
        fluids.fill(new FluidStack(Fluids.WATER, FILL), IFluidHandler.FluidAction.EXECUTE);
        fluids.fill(new FluidStack(ModFluids.METHANE_SOURCE.get(), FILL), IFluidHandler.FluidAction.EXECUTE);
        MachineTestSupport.powerEachTick(helper, reformer.getEnergyStorage(Direction.NORTH));
        check(!reformer.getBlockState().canOcclude(), "The shaped reformer should not occlude its neighbours");
        check(reformer.getUpdatePacket() != null, "The reformer should send its visual state to clients");
        helper.runAtTickTime(60, () -> {
            GasProcessorVisualState state = GasProcessorVisualState.load(reformer.getUpdateTag(helper.getLevel().registryAccess()));
            check(state.active(), "The reformer should be running: " + MachineTestSupport.describe(reformer));
            check(state.inputFluid() == ModFluids.METHANE_SOURCE.get(), "The feed sight glass should show Methane, was " + state.inputFluid());
            check(state.input() > 0.0F && state.input() <= FILL / (float) TANK, "The feed level should show the Methane left, was " + state.input());
            check(state.water() > 0.0F, "The steam drum should show water, was " + state.water());
            check(state.catalyst(), "The Reforming Catalyst Bed should sync as installed");
            check("titanium".equals(state.heatCore()), "The Heat Core should sync as titanium, was '" + state.heatCore() + "'");
            check(state.servo().isEmpty() && state.cell().isEmpty(), "Empty Servo and Battery Cell slots should sync as empty");
            helper.succeed();
        });
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void runningSynthesizerSyncsGasesAndCatalyst(GameTestHelper helper) {
        AmmoniaSynthesizerBlockEntity synthesizer =
                MachineTestSupport.place(helper, MACHINE, ModBlocks.AMMONIA_SYNTHESIZER.get(), AmmoniaSynthesizerBlockEntity.class);
        synthesizer.getGearInventory().insertItem(AmmoniaSynthesizerBlockEntity.SLOT_CATALYST_BED, new ItemStack(ModItems.AMMONIA_CATALYST_BED.get()), false);
        IFluidHandler fluids = synthesizer.getFluidHandler(Direction.NORTH);
        fluids.fill(new FluidStack(ModFluids.NITROGEN_SOURCE.get(), FILL), IFluidHandler.FluidAction.EXECUTE);
        fluids.fill(new FluidStack(ModFluids.HYDROGEN_SOURCE.get(), FILL), IFluidHandler.FluidAction.EXECUTE);
        MachineTestSupport.powerEachTick(helper, synthesizer.getEnergyStorage(Direction.NORTH));
        check(!synthesizer.getBlockState().canOcclude(), "The shaped synthesizer should not occlude its neighbours");
        check(synthesizer.getUpdatePacket() != null, "The synthesizer should send its visual state to clients");
        helper.runAtTickTime(60, () -> {
            AmmoniaSynthesizerVisualState state = AmmoniaSynthesizerVisualState.load(synthesizer.getUpdateTag(helper.getLevel().registryAccess()));
            check(state.active(), "The synthesizer should be running: " + MachineTestSupport.describe(synthesizer));
            check(state.catalyst(), "The Ammonia Catalyst Bed should sync as installed");
            check(state.progress() > 0.0F, "The cycle progress should advance, was " + state.progress());
            check(state.nitrogen() > 0.0F && state.nitrogen() <= FILL / (float) TANK, "The Nitrogen level should show, was " + state.nitrogen());
            check(state.hydrogen() > 0.0F && state.hydrogen() <= FILL / (float) TANK, "The Hydrogen level should show, was " + state.hydrogen());
            helper.succeed();
        });
    }
}
