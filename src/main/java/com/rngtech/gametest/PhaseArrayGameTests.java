package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.PhaseCoilBlock;
import com.rngtech.content.block.PhaseEmitterBlock;
import com.rngtech.content.blockentity.PhaseCoilBlockEntity;
import com.rngtech.content.blockentity.PhaseCradleBlockEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.PhaseArrayRules;
import com.rngtech.rpg.refinement.PhaseArrayAction;
import com.rngtech.rpg.refinement.PhaseStrain;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Phase Array line: a Cradle, three to nine Coils, and an Emitter in a straight horizontal row form; anything else does
 * not. The empty 5x5x5 template fits a three-Coil line, the shortest legal one.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhaseArrayGameTests {
    private static final BlockPos CRADLE = new BlockPos(0, 1, 2);
    private static final int TIMEOUT = 100;

    private PhaseArrayGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void threeCoilLineForms(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        helper.succeedWhen(() -> {
            check(cradle.isLineFormed(), "A Cradle, three Coils and an Emitter should form, found " + cradle.lineCoilCount() + " Coils");
            check(cradle.lineCoilCount() == 3, "Three Coils should be counted, found " + cradle.lineCoilCount());
            check(cradle.lineAxis() == Direction.EAST, "The line runs east of the Cradle, found " + cradle.lineAxis());
            check(cradle.ownsMember(helper.absolutePos(CRADLE.east(4))), "The Emitter should be a formed member");
            check(helper.getBlockState(CRADLE.east(1)).getValue(PhaseCoilBlock.FORMED), "Coils should light when formed");
            check(helper.getBlockState(CRADLE.east(4)).getValue(PhaseEmitterBlock.FORMED), "The Emitter should light when formed");
        });
    }

    /** Blank Coils form a line that can fire nothing; inscribing them opens positions from the Emitter inward. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void inscribedCoilsOpenPositions(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        helper.runAfterDelay(5, () -> {
            check(cradle.isLineFormed(), "The blank line should still form");
            check(cradle.usableCoils() == 0, "Blank Coils open no positions, found " + cradle.usableCoils());
            coil(helper, CRADLE.east(3)).inscribe(1);
            coil(helper, CRADLE.east(2)).inscribe(1);
        });
        helper.succeedWhen(() -> check(cradle.usableCoils() == 2,
                "Two inscribed Coils next to the Emitter open two positions, found " + cradle.usableCoils()));
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void twoCoilsDoNotForm(GameTestHelper helper) {
        helper.setBlock(CRADLE.east(1), ModBlocks.PHASE_COIL.get());
        helper.setBlock(CRADLE.east(2), ModBlocks.PHASE_COIL.get());
        helper.setBlock(CRADLE.east(3), ModBlocks.PHASE_EMITTER.get());
        assertNeverForms(helper, cradle(helper), 2);
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void missingEmitterDoesNotForm(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        helper.setBlock(CRADLE.east(4), Blocks.AIR);
        assertNeverForms(helper, cradle(helper), 3);
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void lineOnAnySideForms(GameTestHelper helper) {
        BlockPos cradlePos = new BlockPos(4, 1, 2);
        for (int i = 1; i <= 3; i++) {
            helper.setBlock(cradlePos.west(i), ModBlocks.PHASE_COIL.get());
        }
        helper.setBlock(cradlePos.west(4), ModBlocks.PHASE_EMITTER.get());
        helper.setBlock(cradlePos, ModBlocks.EXOTIC_AFFIX_FORGE.get());
        PhaseCradleBlockEntity cradle = (PhaseCradleBlockEntity) helper.getBlockEntity(cradlePos);
        helper.succeedWhen(() -> {
            check(cradle.isLineFormed(), "A line west of the Cradle should form");
            check(cradle.lineAxis() == Direction.WEST, "The axis should point west, found " + cradle.lineAxis());
        });
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void brokenCoilUnforms(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        helper.runAfterDelay(5, () -> {
            check(cradle.isLineFormed(), "The line should form before a Coil is broken");
            helper.setBlock(CRADLE.east(2), Blocks.AIR);
        });
        helper.succeedWhen(() -> {
            check(!cradle.isLineFormed(), "Breaking a Coil should unform the line");
            check(!helper.getBlockState(CRADLE.east(1)).getValue(PhaseCoilBlock.FORMED), "The other Coils should go dark");
            check(!helper.getBlockState(CRADLE.east(4)).getValue(PhaseEmitterBlock.FORMED), "The Emitter should go dark");
        });
    }

    /** A charged Reforge on an inscribed three-Coil line fires: the target moves to the output with one Strain. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void reforgePulseFiresOnce(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        ItemStack target = new ItemStack(ModItems.PHASE_COIL.get());
        MachineTraits traits = MachineTraitRoller.roll(ModifierEligibilityProfiles.PHASE_COIL, PhaseArrayRules.PART_STAGE, helper.getLevel().random);
        RefinementTargets.setTraits(target, traits);
        // Registered up front: a delayed task must not add tick tasks while the harness iterates them. The charge
        // storage accepts nothing until the Cradle is charging, so early power is harmless.
        MachineTestSupport.powerEachTick(helper, cradle.getEnergyStorage(Direction.UP));
        helper.runAfterDelay(5, () -> {
            for (int i = 1; i <= 3; i++) {
                coil(helper, CRADLE.east(i)).inscribe(1);
            }
        });
        helper.runAfterDelay(10, () -> {
            check(cradle.usableCoils() == 3, "Three inscribed Coils should be usable, found " + cradle.usableCoils());
            cradle.getItems().setStackInSlot(PhaseCradleBlockEntity.SLOT_TARGET, target);
            cradle.setAction(PhaseArrayAction.REFORGE);
            check(cradle.status() == PhaseCradleBlockEntity.STATUS_IDLE, "A Reforge on a fresh target should be ready to charge, status " + cradle.status());
            cradle.toggleCharging();
        });
        helper.runAfterDelay(15, () -> check(cradle.charge() > 0, "Supply should charge the line"));
        boolean[] sawPulse = { false };
        helper.succeedWhen(() -> {
            if (cradle.status() == PhaseCradleBlockEntity.STATUS_READY) {
                check(cradle.charge() >= cradle.cost(cradle.recipe()), "Ready means the charge covers the cost");
                check(cradle.fire(), "A ready Cradle should fire");
                check(cradle.isPulsing(), "Fire starts the hold sequence");
                check(cradle.outputStack().isEmpty(), "The outcome waits for the sequence to end");
            }
            if (cradle.isPulsing()) {
                sawPulse[0] |= helper.getBlockState(CRADLE.east(4)).getValue(BaseMachineBlock.ACTIVE);
            }
            check(sawPulse[0], "The Emitter should light while the pulse runs");
            check(!cradle.isPulsing(), "The sequence should end");
            check(!helper.getBlockState(CRADLE.east(1)).getValue(BaseMachineBlock.ACTIVE), "Coils go dark after the pulse");
            ItemStack output = cradle.outputStack();
            check(!output.isEmpty(), "The refined item should be in the output");
            check(cradle.targetStack().isEmpty(), "The target slot should be empty after the pulse");
            check(PhaseStrain.get(output) >= 1, "A pulse adds Strain, found " + PhaseStrain.get(output));
            check(RefinementTargets.canRefine(output), "The output should still be refinable");
        });
    }

    private static void line(GameTestHelper helper, Direction axis, int coils) {
        for (int i = 1; i <= coils; i++) {
            helper.setBlock(CRADLE.relative(axis, i), ModBlocks.PHASE_COIL.get());
        }
        helper.setBlock(CRADLE.relative(axis, coils + 1), ModBlocks.PHASE_EMITTER.get());
    }

    private static PhaseCradleBlockEntity cradle(GameTestHelper helper) {
        helper.setBlock(CRADLE, ModBlocks.EXOTIC_AFFIX_FORGE.get());
        return (PhaseCradleBlockEntity) helper.getBlockEntity(CRADLE);
    }

    private static PhaseCoilBlockEntity coil(GameTestHelper helper, BlockPos pos) {
        return (PhaseCoilBlockEntity) helper.getBlockEntity(pos);
    }

    private static void assertNeverForms(GameTestHelper helper, PhaseCradleBlockEntity cradle, int coils) {
        helper.runAfterDelay(TIMEOUT - 10, () -> {
            check(!cradle.isLineFormed(), "An incomplete line should not form");
            check(cradle.lineCoilCount() == coils, "Expected " + coils + " Coils counted, found " + cradle.lineCoilCount());
            check(!helper.getBlockState(CRADLE.east(1)).getValue(PhaseCoilBlock.FORMED), "Coils should stay dark");
            helper.succeed();
        });
    }
}
