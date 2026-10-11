package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.BioGeneratorBlockEntity;
import com.rngtech.content.energy.BiogasDigestion;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
