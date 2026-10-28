package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.item.PlacementPreview;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The client placement ghost for Cable and Universal Connectors shows the state that the real use then places. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlacementPreviewGameTests {
    private static final BlockPos CABLE_FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos CABLE = new BlockPos(2, 2, 1);
    private static final BlockPos NEW_CABLE = new BlockPos(1, 2, 1);
    private static final BlockPos CABLE_TARGET = new BlockPos(3, 2, 1);
    private static final BlockPos BESIDE_CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos PLATE_BASE = new BlockPos(1, 1, 3);
    private static final BlockPos PLATE = new BlockPos(2, 1, 3);
    private static final BlockPos PLATE_TOP = new BlockPos(2, 2, 3);

    private PlacementPreviewGameTests() {
    }

    /**
     * Places a cable beside a cable, fits a connector to it from the machine side and on an open face, places a plate
     * beside it while sneaking, places a plate, adds a second plate face, and turns the plate into a cable. Each preview must equal the state the use leaves, and a use that fails
     * must have no preview.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void previewMatchesTheBlockTheUsePlaces(GameTestHelper helper) {
        helper.setBlock(CABLE_FLOOR, Blocks.STONE);
        helper.setBlock(CABLE, ModBlocks.CABLE.get());
        helper.setBlock(CABLE_TARGET, Blocks.STONE);
        helper.setBlock(PLATE_BASE, Blocks.STONE);
        helper.setBlock(PLATE_TOP, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cable = new ItemStack(ModItems.CABLE.get(), 64);
        ItemStack connector = new ItemStack(ModItems.UNIVERSAL_CONNECTOR.get(), 64);

        BlockState placedCable = use(helper, player, cable, CABLE_FLOOR, Direction.UP, NEW_CABLE, "Cable on the floor");
        check(CableBlock.hasCableConnection(placedCable, Direction.EAST), "The new cable did not link to its neighbour");

        BlockState fitted = use(helper, player, connector, CABLE_TARGET, Direction.WEST, CABLE, "Connector from the machine");
        check(CableBlock.hasConnector(fitted, Direction.EAST), "The connector did not mount on the cable's machine face");
        useFails(helper, player, connector, CABLE, Direction.EAST, "Connector on a used cable face");
        BlockState openFace = use(helper, player, connector, CABLE, Direction.UP, CABLE, "Connector on an open cable face");
        check(CableBlock.hasConnector(openFace, Direction.UP), "The connector did not mount on the open cable face");
        player.setShiftKeyDown(true);
        BlockState sneakPlate = use(helper, player, connector, CABLE, Direction.SOUTH, BESIDE_CABLE, "Sneaking on an open cable face");
        check(sneakPlate.getBlock() instanceof UniversalConnectorBlock, "Sneaking on an open cable face did not place a plate");
        player.setShiftKeyDown(false);

        BlockState plate = use(helper, player, connector, PLATE_BASE, Direction.EAST, PLATE, "Standalone plate");
        check(plate.getValue(UniversalConnectorBlock.FACING) == Direction.WEST, "The plate does not face the block it was used on");
        use(helper, player, connector, PLATE_TOP, Direction.DOWN, PLATE, "Second plate face");
        useFails(helper, player, connector, PLATE_TOP, Direction.DOWN, "Plate face that is already used");

        BlockState merged = use(helper, player, cable, PLATE, Direction.NORTH, PLATE, "Cable into the plate");
        check(CableBlock.hasConnector(merged, Direction.WEST) && CableBlock.hasConnector(merged, Direction.UP),
                "The cable did not keep both plate faces as connectors");
        helper.succeed();
    }

    private static BlockState use(
            GameTestHelper helper,
            Player player,
            ItemStack stack,
            BlockPos clicked,
            Direction face,
            BlockPos expectedPos,
            String scenario
    ) {
        UseOnContext context = context(helper, player, stack, clicked, face);
        PlacementPreview preview = ((PlacementPreview.Source) stack.getItem()).placementPreview(context);
        check(preview != null, scenario + ": no preview");
        check(preview.pos().equals(helper.absolutePos(expectedPos)), scenario + ": preview at " + preview.pos());
        InteractionResult result = stack.getItem().useOn(context);
        check(result.consumesAction(), scenario + ": the use returned " + result);
        BlockState placed = helper.getLevel().getBlockState(preview.pos());
        check(placed == preview.state(), scenario + ": previewed " + preview.state() + " but placed " + placed);
        return placed;
    }

    private static void useFails(
            GameTestHelper helper,
            Player player,
            ItemStack stack,
            BlockPos clicked,
            Direction face,
            String scenario
    ) {
        UseOnContext context = context(helper, player, stack, clicked, face);
        PlacementPreview preview = ((PlacementPreview.Source) stack.getItem()).placementPreview(context);
        check(preview == null, scenario + ": previewed " + (preview == null ? null : preview.state()));
        InteractionResult result = stack.getItem().useOn(context);
        check(!result.consumesAction(), scenario + ": the use returned " + result);
    }

    private static UseOnContext context(GameTestHelper helper, Player player, ItemStack stack, BlockPos clicked, Direction face) {
        BlockPos pos = helper.absolutePos(clicked);
        Vec3 hit = Vec3.atCenterOf(pos).relative(face, 0.5D);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }
}
