package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.BioGeneratorBlockEntity;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.energy.BiogasDigestion;
import com.rngtech.content.item.BiogasMembraneItem;
import com.rngtech.content.machine.FluidPumpMaterial;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The Bio Generator digests feed into a gas holder and burns the gas in an engine. An unrolled generator's rated
 * retention keeps the old fuel balance; the retention dial trades FE per item against FE/t; nothing is wasted.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BioGeneratorGameTests {
    private static final BlockPos GENERATOR = new BlockPos(2, 1, 2);
    /** Rich Biomass in an unrolled generator: 400 ticks at 8 FE/t. */
    private static final int RICH_BIOMASS_FE = 3200;

    private BioGeneratorGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 700)
    public static void ratedRetentionKeepsTheFuelBalance(GameTestHelper helper) {
        digestOneRichBiomass(helper, BiogasDigestion.RATED_RETENTION_DAYS, RICH_BIOMASS_FE, 8);
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 500)
    public static void shortRetentionTradesFuelForRate(GameTestHelper helper) {
        digestOneRichBiomass(helper, BiogasDigestion.MIN_RETENTION_DAYS, RICH_BIOMASS_FE * 4 / 5, 13);
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1000)
    public static void longRetentionTradesRateForFuel(GameTestHelper helper) {
        digestOneRichBiomass(helper, BiogasDigestion.MAX_RETENTION_DAYS, RICH_BIOMASS_FE * 8 / 7, 5);
    }

    /** With nowhere to send FE, digestion keeps filling the gas holder and then waits; every FE still comes out later. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 900)
    public static void blockedOutputBanksGasWithoutWaste(GameTestHelper helper) {
        BioGeneratorBlockEntity generator = generatorWithRichBiomass(helper, BiogasDigestion.RATED_RETENTION_DAYS);
        IEnergyStorage output = generator.getEnergyStorage(back(generator));
        int buffer = output.getMaxEnergyStored();
        boolean[] draining = new boolean[1];
        long[] total = new long[1];
        helper.onEachTick(() -> {
            if (draining[0]) {
                total[0] += output.extractEnergy(Integer.MAX_VALUE, false);
            }
        });
        helper.startSequence()
                .thenIdle(400)
                .thenExecute(() -> {
                    check(output.getEnergyStored() == buffer, "The FE buffer did not fill: " + output.getEnergyStored() + "/" + buffer);
                    check(Math.abs(generator.gasStored() - BiogasDigestion.HOLDER_CAPACITY) < 1.0E-6,
                            "Digestion did not keep filling the gas holder while output was blocked: " + generator.gasStored());
                    draining[0] = true;
                })
                .thenWaitUntil(() -> check(total[0] == RICH_BIOMASS_FE && generator.gasStored() <= 0.0,
                        "Drained " + total[0] + " FE of " + RICH_BIOMASS_FE + ", gas left " + generator.gasStored()))
                .thenSucceed();
    }

    /**
     * Drains the generator every tick until one Rich Biomass is spent. The engine must wait for the holder's start
     * pressure, run at the retention's FE/t, and deliver the retention's FE for the item.
     */
    private static void digestOneRichBiomass(GameTestHelper helper, int days, int expectedFe, int expectedRate) {
        BioGeneratorBlockEntity generator = generatorWithRichBiomass(helper, days);
        IEnergyStorage output = generator.getEnergyStorage(back(generator));
        long[] total = new long[1];
        int[] peak = new int[1];
        double[] gasAtStart = {-1.0};
        helper.onEachTick(() -> {
            int extracted = output.extractEnergy(Integer.MAX_VALUE, false);
            if (extracted > 0 && gasAtStart[0] < 0.0) {
                gasAtStart[0] = generator.gasStored() + extracted;
            }
            total[0] += extracted;
            peak[0] = Math.max(peak[0], extracted);
        });
        helper.succeedWhen(() -> {
            check(generator.getFuelInventory().getStackInSlot(BioGeneratorBlockEntity.SLOT_FUEL).isEmpty(), "The Rich Biomass was not loaded");
            check(generator.gasStored() <= 0.0 && !generator.engineRunning(), "Still burning gas: " + generator.gasStored());
            check(Math.abs(total[0] - expectedFe) <= 1, days + " days gave " + total[0] + " FE, expected " + expectedFe);
            check(peak[0] == expectedRate, days + " days ran at " + peak[0] + " FE/t, expected " + expectedRate);
            check(gasAtStart[0] >= BiogasDigestion.ENGINE_START_GAS - 1.0E-6,
                    "The engine started on " + gasAtStart[0] + " FE of gas, before the holder reached its start pressure");
        });
    }

    /**
     * Automation works on every face, the front included; the back port is only the visual marker. Each face takes
     * fuel, and each face hands FE to a neighbour that asks for it.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void everyFaceTakesFuelAndGivesEnergy(GameTestHelper helper) {
        BioGeneratorBlockEntity generator = MachineTestSupport.place(helper, GENERATOR, ModBlocks.BIO_GENERATOR.get(), BioGeneratorBlockEntity.class);
        for (Direction side : Direction.values()) {
            ItemStack fuel = new ItemStack(ModItems.RICH_BIOMASS.get());
            check(generator.getItemHandler(side).insertItem(0, fuel, true).isEmpty(), side + " does not take fuel");
        }
        generator.getItemHandler(Direction.NORTH).insertItem(0, new ItemStack(ModItems.RICH_BIOMASS.get()), false);
        helper.startSequence()
                .thenWaitUntil(() -> check(generator.getEnergyStorage(Direction.UP).getEnergyStored() >= Direction.values().length,
                        "No FE built up: " + generator.getEnergyStorage(Direction.UP).getEnergyStored()))
                .thenExecute(() -> {
                    for (Direction side : Direction.values()) {
                        IEnergyStorage storage = generator.getEnergyStorage(side);
                        check(storage != null && storage.extractEnergy(1, false) == 1, side + " does not give FE");
                    }
                })
                .thenSucceed();
    }

    /** A bare Fluid Pump stops the engine, exports 70% of the Methane on any face, and vents the CO2. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 700)
    public static void pumpExportsMethaneAndVentsCarbon(GameTestHelper helper) {
        BioGeneratorBlockEntity generator = generatorWithRichBiomass(helper, BiogasDigestion.RATED_RETENTION_DAYS);
        generator.getGearInventory().setStackInSlot(BioGeneratorBlockEntity.SLOT_GAS_LINE_A, pump());
        int expected = (int) Math.floor(RICH_BIOMASS_FE * BiogasDigestion.METHANE_PER_FE * BiogasDigestion.PUMP_RECOVERY);
        helper.succeedWhen(() -> {
            check(generator.getFuelInventory().getStackInSlot(BioGeneratorBlockEntity.SLOT_FUEL).isEmpty(), "The Rich Biomass was not loaded");
            check(generator.gasStored() <= 0.0, "Gas left in the holder: " + generator.gasStored());
            check(Math.abs(generator.methaneTank().getFluidAmount() - expected) <= 1,
                    "Exported " + generator.methaneTank().getFluidAmount() + " mB Methane, expected " + expected);
            check(generator.exhaustTank().isEmpty(), "A bare pump captured CO2: " + generator.exhaustTank().getFluidAmount());
            check(generator.getEnergyStorage(Direction.UP).getEnergyStored() == 0, "The engine ran in take-off mode");
            for (Direction side : Direction.values()) {
                FluidStack drained = generator.getFluidHandler(side).drain(1, IFluidHandler.FluidAction.SIMULATE);
                check(drained.getFluid() == ModFluids.METHANE_SOURCE.get() && drained.getAmount() == 1, side + " does not give Methane");
                check(generator.getFluidHandler(side).fill(new FluidStack(ModFluids.METHANE_SOURCE.get(), 100), IFluidHandler.FluidAction.SIMULATE) == 0,
                        side + " accepts fluid");
            }
        });
    }

    /**
     * Energy Generation only digests faster in take-off mode: a generator rolled to twice the rate empties a Rich Biomass
     * in half the time but gets the same Methane from it as an unrolled one, so the farm stays the cost.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 320)
    public static void energyGenerationOnlySpeedsTheTakeOff(GameTestHelper helper) {
        BioGeneratorBlockEntity generator = generatorWithRichBiomass(helper, BiogasDigestion.RATED_RETENTION_DAYS);
        generator.setMachineTraits(new MachineTraits(Rarity.MAGIC, 0, List.of(new MachineModifier(
                ModifierSlot.SUFFIX, MachineStat.ENERGY_GENERATION, ModifierOperation.INCREASED_PERCENT, 100.0)), List.of()));
        generator.getGearInventory().setStackInSlot(BioGeneratorBlockEntity.SLOT_GAS_LINE_A, pump());
        check(Math.abs(generator.effectiveStats().effectiveEnergyGenerationMultiplier() - 2.0) < 1.0E-6,
                "The roll did not double generation: " + generator.effectiveStats().effectiveEnergyGenerationMultiplier());
        int expected = (int) Math.floor(RICH_BIOMASS_FE * BiogasDigestion.METHANE_PER_FE * BiogasDigestion.PUMP_RECOVERY);
        helper.succeedWhen(() -> {
            check(generator.getFuelInventory().getStackInSlot(BioGeneratorBlockEntity.SLOT_FUEL).isEmpty() && generator.gasStored() <= 0.0,
                    "Still digesting: " + generator.gasStored());
            check(Math.abs(generator.methaneTank().getFluidAmount() - expected) <= 1,
                    "A doubled rate gave " + generator.methaneTank().getFluidAmount() + " mB Methane, expected the unrolled " + expected);
        });
    }

    /** A pump and a perfectly calibrated membrane recover 98% of the Methane and capture all the CO2. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 700)
    public static void membraneRecoversMoreAndCapturesCarbon(GameTestHelper helper) {
        BioGeneratorBlockEntity generator = generatorWithRichBiomass(helper, BiogasDigestion.RATED_RETENTION_DAYS);
        generator.getGearInventory().setStackInSlot(BioGeneratorBlockEntity.SLOT_GAS_LINE_A, membrane(100));
        generator.getGearInventory().setStackInSlot(BioGeneratorBlockEntity.SLOT_GAS_LINE_B, pump());
        int methane = (int) Math.floor(RICH_BIOMASS_FE * BiogasDigestion.METHANE_PER_FE * 0.98);
        int carbon = (int) Math.floor(RICH_BIOMASS_FE * BiogasDigestion.CARBON_EXHAUST_PER_FE);
        helper.succeedWhen(() -> {
            check(generator.gasStored() <= 0.0 && generator.getFuelInventory().getStackInSlot(BioGeneratorBlockEntity.SLOT_FUEL).isEmpty(),
                    "Still digesting: " + generator.gasStored());
            check(Math.abs(generator.methaneTank().getFluidAmount() - methane) <= 1,
                    "Exported " + generator.methaneTank().getFluidAmount() + " mB Methane, expected " + methane);
            check(Math.abs(generator.exhaustTank().getFluidAmount() - carbon) <= 1,
                    "Captured " + generator.exhaustTank().getFluidAmount() + " mB Carbon Exhaust, expected " + carbon);
        });
    }

    /** With the Methane tank full, gas banks in the holder and digestion waits; draining the tank lets every mB out. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void fullMethaneTankBanksGasWithoutWaste(GameTestHelper helper) {
        BioGeneratorBlockEntity generator = generatorWithRichBiomass(helper, BiogasDigestion.RATED_RETENTION_DAYS);
        generator.getGearInventory().setStackInSlot(BioGeneratorBlockEntity.SLOT_GAS_LINE_A, pump());
        generator.methaneTank().fill(new FluidStack(ModFluids.METHANE_SOURCE.get(), BiogasDigestion.TAKE_OFF_TANK_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        IFluidHandler output = generator.getFluidHandler(back(generator));
        int expected = (int) Math.floor(RICH_BIOMASS_FE * BiogasDigestion.METHANE_PER_FE * BiogasDigestion.PUMP_RECOVERY);
        boolean[] draining = new boolean[1];
        long[] total = new long[1];
        helper.onEachTick(() -> {
            if (draining[0]) {
                total[0] += output.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
        });
        helper.startSequence()
                .thenIdle(400)
                .thenExecute(() -> {
                    check(Math.abs(generator.gasStored() - BiogasDigestion.HOLDER_CAPACITY) < 1.0E-6,
                            "Digestion did not keep filling the holder while the Methane tank was full: " + generator.gasStored());
                    draining[0] = true;
                })
                .thenWaitUntil(() -> check(generator.gasStored() <= 0.0 && Math.abs(total[0] - BiogasDigestion.TAKE_OFF_TANK_CAPACITY - expected) <= 1,
                        "Drained " + total[0] + " mB, expected " + (BiogasDigestion.TAKE_OFF_TANK_CAPACITY + expected) + ", gas left " + generator.gasStored()))
                .thenSucceed();
    }

    /** A membrane with no pump changes nothing: the engine still burns the gas for FE. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 300)
    public static void membraneWithoutPumpKeepsTheEngine(GameTestHelper helper) {
        BioGeneratorBlockEntity generator = generatorWithRichBiomass(helper, BiogasDigestion.RATED_RETENTION_DAYS);
        generator.getGearInventory().setStackInSlot(BioGeneratorBlockEntity.SLOT_GAS_LINE_A, membrane(50));
        helper.succeedWhen(() -> {
            check(!generator.takeOffMode(), "A membrane alone switched to take-off");
            check(generator.getEnergyStorage(Direction.UP).getEnergyStored() > 0, "The engine made no FE");
            check(generator.methaneTank().isEmpty(), "Methane came out without a pump");
        });
    }

    /** The Component Assembler records the calibrated component's stability, which sets the membrane's recovery. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void assemblerRecordsMembraneCalibration(GameTestHelper helper) {
        BatteryAssemblerBlockEntity assembler = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.BATTERY_ASSEMBLER.get(), BatteryAssemblerBlockEntity.class);
        ItemStack component = new ItemStack(ModItems.calibratedComponent(CalibrationFamily.STORAGE).get());
        component.set(ModDataComponents.CALIBRATION_STATE.get(), new CalibrationState(CalibrationFamily.STORAGE, 2, 80, 0));
        assembler.getProcessInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_0, component);
        assembler.getProcessInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_1, item("rngtech:rubber", 2));
        assembler.getProcessInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_2, item("rngtech:titanium_plate", 1));
        MachineTestSupport.powerEachTick(helper, assembler.getEnergyStorage(Direction.EAST));
        helper.succeedWhen(() -> {
            ItemStack output = assembler.getProcessInventory().getStackInSlot(BatteryAssemblerBlockEntity.SLOT_OUTPUT_0);
            check(output.getItem() instanceof BiogasMembraneItem, "No membrane yet: " + output);
            CalibrationState recorded = output.get(ModDataComponents.SOURCE_CALIBRATION.get());
            check(recorded != null && recorded.stability() == 80, "The membrane did not record its component: " + recorded);
            check(Math.abs(BiogasMembraneItem.recovery(output) - BiogasDigestion.membraneRecovery(80)) < 1.0E-9,
                    "Recovery " + BiogasMembraneItem.recovery(output));
        });
    }

    private static ItemStack pump() {
        return new ItemStack(ModItems.fluidPump(FluidPumpMaterial.OSMIUM).get());
    }

    private static ItemStack membrane(int stability) {
        ItemStack membrane = new ItemStack(ModItems.BIOGAS_MEMBRANE.get());
        membrane.set(ModDataComponents.SOURCE_CALIBRATION.get(), new CalibrationState(CalibrationFamily.STORAGE, 2, stability, 0));
        return membrane;
    }

    private static ItemStack item(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), count);
    }

    private static Direction back(BioGeneratorBlockEntity generator) {
        return generator.getBlockState().getValue(BaseMachineBlock.FACING).getOpposite();
    }

    private static BioGeneratorBlockEntity generatorWithRichBiomass(GameTestHelper helper, int days) {
        BioGeneratorBlockEntity generator = MachineTestSupport.place(helper, GENERATOR, ModBlocks.BIO_GENERATOR.get(), BioGeneratorBlockEntity.class);
        generator.setRetentionDays(days);
        generator.getFuelInventory().setStackInSlot(BioGeneratorBlockEntity.SLOT_FUEL, new ItemStack(ModItems.RICH_BIOMASS.get()));
        return generator;
    }
}
