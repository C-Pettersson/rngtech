package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.WoodenDehumidifierBlockEntity;
import com.rngtech.content.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Wooden Dehumidifier structure: only the hollow 3x3x3 frame cube with the controller in the middle of one side forms. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WoodenDehumidifierGameTests {
    private static final BlockPos CENTER = new BlockPos(2, 2, 2);
    private static final int TIMEOUT = 100;

    private WoodenDehumidifierGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void fullCubeForms(GameTestHelper helper) {
        cube(helper);
        WoodenDehumidifierBlockEntity controller = controller(helper, Direction.NORTH, Direction.NORTH);
        helper.succeedWhen(() -> {
            check(controller.isStructureValid(), "A full cube should form, found " + controller.frameCount() + " frames");
            check(controller.frameCount() == 25, "A full cube has 25 frames, counted " + controller.frameCount());
            check(controller.ownsFrame(helper.absolutePos(CENTER.offset(1, 1, 1))), "Cube frames should be formed");
        });
    }

    /** A controller facing into its cube still forms; the cube is looked for on every horizontal side. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void cubeOnAnySideForms(GameTestHelper helper) {
        cube(helper);
        WoodenDehumidifierBlockEntity controller = controller(helper, Direction.NORTH, Direction.SOUTH);
        helper.succeedWhen(() -> check(controller.isStructureValid(), "The cube behind the controller's back should form"));
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void missingFrameDoesNotForm(GameTestHelper helper) {
        cube(helper);
        helper.setBlock(CENTER.offset(1, 1, 1), Blocks.AIR);
        assertNeverForms(helper, controller(helper, Direction.NORTH, Direction.NORTH), 24);
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void filledCentreDoesNotForm(GameTestHelper helper) {
        cube(helper);
        helper.setBlock(CENTER, Blocks.STONE);
        assertNeverForms(helper, controller(helper, Direction.NORTH, Direction.NORTH), 25);
    }

    /** 25 frames in a flat layer touching the controller formed under the old rule; it is not a cube. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void otherShapesDoNotForm(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(4, 1, 4))) {
            helper.setBlock(pos, ModBlocks.WOODEN_DEHUMIDIFIER_FRAME.get());
        }
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.WOODEN_DEHUMIDIFIER.get().defaultBlockState());
        WoodenDehumidifierBlockEntity controller = (WoodenDehumidifierBlockEntity) helper.getBlockEntity(new BlockPos(2, 2, 2));
        helper.runAfterDelay(TIMEOUT - 10, () -> {
            check(!controller.isStructureValid(), "A flat layer of frames should not form");
            helper.succeed();
        });
    }

    /** Frames on every block of the 3x3x3 cube around CENTER except the centre itself. */
    private static void cube(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-1, -1, -1), CENTER.offset(1, 1, 1))) {
            if (!pos.equals(CENTER)) {
                helper.setBlock(pos, ModBlocks.WOODEN_DEHUMIDIFIER_FRAME.get());
            }
        }
    }

    /** Replaces the middle block of the cube's `side` face with a controller facing `facing`. */
    private static WoodenDehumidifierBlockEntity controller(GameTestHelper helper, Direction side, Direction facing) {
        BlockPos pos = CENTER.relative(side);
        helper.setBlock(pos, ModBlocks.WOODEN_DEHUMIDIFIER.get().defaultBlockState().setValue(BaseMachineBlock.FACING, facing));
        return (WoodenDehumidifierBlockEntity) helper.getBlockEntity(pos);
    }

    private static void assertNeverForms(GameTestHelper helper, WoodenDehumidifierBlockEntity controller, int frames) {
        helper.runAfterDelay(TIMEOUT - 10, () -> {
            check(!controller.isStructureValid(), "An incomplete cube should not form");
            check(controller.frameCount() == frames, "Expected " + frames + " frames counted, found " + controller.frameCount());
            check(!controller.ownsFrame(helper.absolutePos(CENTER.offset(-1, -1, -1))), "Frames should stay unformed");
            helper.succeed();
        });
    }
}
