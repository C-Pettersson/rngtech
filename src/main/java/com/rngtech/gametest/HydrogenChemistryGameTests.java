package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AlgaePhotobioreactorBlockEntity;
import com.rngtech.content.blockentity.AmmoniaFuelCellBlockEntity;
import com.rngtech.content.blockentity.AmmoniaSynthesizerBlockEntity;
import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.blockentity.InertAtmosphere;
import com.rngtech.content.blockentity.MachineInfoSnapshot;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The Stage 6 gases' second uses: the water-gas shift, Hydrogen in the Fuel Cell, Urea from the Ammonia Synthesizer,
 * and plates annealed in a Hydrogen Quench Metal Press.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HydrogenChemistryGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final int FILL = 4000;

    private HydrogenChemistryGameTests() {
    }

    /** A Reformer fed Carbon Monoxide shifts it into Hydrogen and Carbon Exhaust. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void reformerShiftsCarbonMonoxideIntoHydrogen(GameTestHelper helper) {
        GasChemistryBlockEntity reformer = MachineTestSupport.place(helper, MACHINE, ModBlocks.STEAM_METHANE_REFORMER.get(), GasChemistryBlockEntity.class);
        reformer.getGearInventory().insertItem(
                GasChemistryBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()), false);
        reformer.getGearInventory().insertItem(GasChemistryBlockEntity.SLOT_CATALYST_BED, new ItemStack(ModItems.REFORMING_CATALYST_BED.get()), false);
        IFluidHandler fluids = reformer.getFluidHandler(Direction.NORTH);
        fluids.fill(new FluidStack(Fluids.WATER, FILL), IFluidHandler.FluidAction.EXECUTE);
        int filled = fluids.fill(new FluidStack(ModFluids.CARBON_MONOXIDE_SOURCE.get(), FILL), IFluidHandler.FluidAction.EXECUTE);
        check(filled == FILL, "The Reformer took only " + filled + " mB of Carbon Monoxide");
        MachineTestSupport.powerEachTick(helper, reformer.getEnergyStorage(Direction.NORTH));
        helper.succeedWhen(() -> {
            int hydrogen = fluids.drain(new FluidStack(ModFluids.HYDROGEN_SOURCE.get(), FILL), IFluidHandler.FluidAction.SIMULATE).getAmount();
            int exhaust = fluids.drain(new FluidStack(ModFluids.CARBON_EXHAUST_SOURCE.get(), FILL), IFluidHandler.FluidAction.SIMULATE).getAmount();
            check(hydrogen > 0 && exhaust > 0, "No shift output yet: " + hydrogen + " mB Hydrogen, " + exhaust + " mB Carbon Exhaust; "
                    + MachineTestSupport.describe(reformer));
        });
    }

    /** The Fuel Cell takes Hydrogen and makes FE from it. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void fuelCellBurnsHydrogen(GameTestHelper helper) {
        AmmoniaFuelCellBlockEntity cell = MachineTestSupport.place(helper, MACHINE, ModBlocks.AMMONIA_FUEL_CELL.get(), AmmoniaFuelCellBlockEntity.class);
        cell.getGearInventory().setStackInSlot(AmmoniaFuelCellBlockEntity.SLOT_MEMBRANE, new ItemStack(ModItems.FUEL_CELL_MEMBRANE.get()));
        int filled = cell.getFluidHandler(Direction.NORTH).fill(new FluidStack(ModFluids.HYDROGEN_SOURCE.get(), FILL), IFluidHandler.FluidAction.EXECUTE);
        check(filled == FILL, "The Fuel Cell took only " + filled + " mB of Hydrogen");
        helper.succeedWhen(() -> check(cell.machineInfo().energy() > 0, "No FE from Hydrogen yet; " + MachineTestSupport.describe(cell)));
    }

    /**
     * A Fuel Cell whose buffer is full runs only on the ticks after a consumer drains some FE. Its lamp must stay lit
     * through those gaps instead of flashing.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void fullFuelCellLampDoesNotFlash(GameTestHelper helper) {
        AmmoniaFuelCellBlockEntity cell = MachineTestSupport.place(helper, MACHINE, ModBlocks.AMMONIA_FUEL_CELL.get(), AmmoniaFuelCellBlockEntity.class);
        cell.getGearInventory().setStackInSlot(AmmoniaFuelCellBlockEntity.SLOT_MEMBRANE, new ItemStack(ModItems.FUEL_CELL_MEMBRANE.get()));
        cell.getFluidHandler(Direction.NORTH).fill(new FluidStack(ModFluids.HYDROGEN_SOURCE.get(), FILL), IFluidHandler.FluidAction.EXECUTE);
        int[] fullTicks = new int[1];
        helper.onEachTick(() -> {
            cell.getEnergyStorage(Direction.NORTH).extractEnergy(50, false);
            if (cell.machineInfo().outputSummary() == MachineInfoSnapshot.OutputSummary.ENERGY_FULL) {
                fullTicks[0]++;
            }
            if (fullTicks[0] > 0) {
                check(cell.getBlockState().getValue(BaseMachineBlock.ACTIVE), "The lamp went out while the full cell kept running; "
                        + MachineTestSupport.describe(cell));
            }
        });
        helper.succeedWhen(() -> check(fullTicks[0] >= 100, "The cell has been full for only " + fullTicks[0] + " ticks"));
    }

    /** Ammonia piped into a synthesizer with Carbon Exhaust makes Urea, which only comes out. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void synthesizerMakesUreaFromAmmoniaAndExhaust(GameTestHelper helper) {
        AmmoniaSynthesizerBlockEntity synthesizer =
                MachineTestSupport.place(helper, MACHINE, ModBlocks.AMMONIA_SYNTHESIZER.get(), AmmoniaSynthesizerBlockEntity.class);
        synthesizer.getGearInventory().insertItem(AmmoniaSynthesizerBlockEntity.SLOT_CATALYST_BED, new ItemStack(ModItems.AMMONIA_CATALYST_BED.get()), false);
        IFluidHandler fluids = synthesizer.getFluidHandler(Direction.NORTH);
        fluids.fill(new FluidStack(ModFluids.AMMONIA_SOURCE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        fluids.fill(new FluidStack(ModFluids.CARBON_EXHAUST_SOURCE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        MachineTestSupport.powerEachTick(helper, synthesizer.getEnergyStorage(Direction.NORTH));
        check(synthesizer.getItemHandler(Direction.DOWN).insertItem(0, new ItemStack(ModItems.UREA.get()), true).getCount() == 1,
                "Automation must not insert into the Urea slot");
        helper.succeedWhen(() -> check(synthesizer.getUreaOutput().getStackInSlot(AmmoniaSynthesizerBlockEntity.SLOT_UREA).is(ModItems.UREA.get()),
                "No Urea yet; " + MachineTestSupport.describe(synthesizer)));
    }

    /** A Photobioreactor saved before the Urea slot loads with its old five slots grown, and keeps ticking. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 40)
    public static void photobioreactorSavedBeforeUreaLoads(GameTestHelper helper) {
        AlgaePhotobioreactorBlockEntity reactor =
                MachineTestSupport.place(helper, MACHINE, ModBlocks.ALGAE_PHOTOBIOREACTOR.get(), AlgaePhotobioreactorBlockEntity.class);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag tag = reactor.saveWithFullMetadata(registries);
        tag.getCompound("Inventory").putInt("Size", AlgaePhotobioreactorBlockEntity.SLOT_UREA);
        reactor.loadWithComponents(tag, registries);
        check(reactor.getInventory().getSlots() == AlgaePhotobioreactorBlockEntity.SLOT_COUNT,
                "An old reactor loaded with " + reactor.getInventory().getSlots() + " slots");
        helper.runAfterDelay(20, helper::succeed);
    }

    /** Without Bright Anneal or Hydrogen Quench, the Metal Press tank refuses Hydrogen. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void pressWithoutAnnealingNodeRefusesHydrogen(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.METAL_PRESS.get(), MetalPressBlockEntity.class);
        int filled = press.getFluidHandler(Direction.NORTH).fill(new FluidStack(ModFluids.HYDROGEN_SOURCE.get(), FILL), IFluidHandler.FluidAction.EXECUTE);
        check(filled == 0, "A press without an annealing node took " + filled + " mB of Hydrogen");
        helper.succeed();
    }

    /** Hydrogen Quench anneals plates for 250 mB each. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void hydrogenQuenchAnnealsPlates(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.METAL_PRESS.get(), MetalPressBlockEntity.class);
        press.setMachineProgression(new MachineProgressionState(
                MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL), 0, MachineProgressionState.MAX_LEVEL,
                List.of(), "", List.of(), false, "drop_forge", List.of("drop_hammer", "quench_line", "hydrogen_quench"), 2));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_SERVO, new ItemStack(ModItems.STEEL_SERVO.get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD, new ItemStack(ModItems.PLATE_MOLD.get()));
        press.getProcessInventory().setStackInSlot(MetalPressBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 2));
        IFluidHandler tank = press.getFluidHandler(Direction.NORTH);
        int filled = tank.fill(new FluidStack(ModFluids.HYDROGEN_SOURCE.get(), FILL), IFluidHandler.FluidAction.EXECUTE);
        check(filled == FILL, "A Hydrogen Quench press took only " + filled + " mB of Hydrogen");
        MachineTestSupport.powerEachTick(helper, press.getEnergyStorage(null));
        helper.succeedWhen(() -> {
            ItemStack output = press.getProcessInventory().getStackInSlot(MetalPressBlockEntity.SLOT_OUTPUT);
            check(output.is(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "iron_plate"))),
                    "No iron plate yet; " + MachineTestSupport.describe(press));
            check(CraftedTraitOutputs.isAnnealed(output), "The plate came out plain");
            int left = tank.getFluidInTank(0).getAmount();
            check(left == FILL - InertAtmosphere.HYDROGEN_PER_ANNEAL * output.getCount(),
                    output.getCount() + " annealed plates left " + left + " mB of Hydrogen");
        });
    }
}
