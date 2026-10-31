package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.cable.StallBackoff;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Cable network snapshots rebuild when their network changes, and only then; dye decides which touching cables link. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CableNetworkGameTests {
    private static final BlockPos CABLE_A = new BlockPos(1, 1, 1);
    private static final BlockPos MACHINE_A = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE_B = new BlockPos(1, 1, 3);
    private static final BlockPos TARGET_B = new BlockPos(2, 1, 3);

    private CableNetworkGameTests() {
    }

    /**
     * Two separate networks, each with a Universal Connector facing east. A machine switching between working and idle
     * next to network A keeps both snapshots; replacing or removing that machine rebuilds A and leaves B and a row
     * stalled on B alone.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 40)
    public static void onlyTargetBlockChangesRebuildTheirOwnNetwork(GameTestHelper helper) {
        helper.setBlock(MACHINE_A, ModBlocks.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.BRONZE).get());
        helper.setBlock(TARGET_B, Blocks.CHEST);
        connectorCable(helper, CABLE_A);
        connectorCable(helper, CABLE_B);
        long[] before = new long[2];
        StallBackoff stalledOnB = new StallBackoff();
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    before[0] = version(helper, CABLE_A);
                    before[1] = version(helper, CABLE_B);
                    check(before[0] != 0L && before[1] != 0L, "A cable read no network");
                    check(before[0] != before[1], "Separate networks share a snapshot");
                    stalledOnB.start(before[1], StallBackoff.MAX_RETRY_TICKS);

                    toggleActive(helper, MACHINE_A);
                    toggleActive(helper, MACHINE_A);
                    check(version(helper, CABLE_A) == before[0], "A machine switching its working state rebuilt its cable's network");
                    check(version(helper, CABLE_B) == before[1], "A machine switching its working state rebuilt another network");

                    helper.setBlock(MACHINE_A, Blocks.CHEST);
                    long replaced = version(helper, CABLE_A);
                    check(replaced != before[0], "Replacing a connector's target kept the old network snapshot");
                    helper.setBlock(MACHINE_A, Blocks.AIR);
                    check(version(helper, CABLE_A) != replaced, "Removing a connector's target kept the old network snapshot");

                    check(version(helper, CABLE_B) == before[1], "A change on one network rebuilt another");
                    check(stalledOnB.waiting(version(helper, CABLE_B)), "A change on one network woke a row stalled on another");
                })
                .thenSucceed();
    }

    /** A Water Bottle washes the dye off a placed cable, links it to a differently dyed neighbour, and leaves a Glass Bottle. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void waterBottleWashesDyedCable(GameTestHelper helper) {
        BlockPos red = new BlockPos(1, 1, 1);
        BlockPos blue = red.east();
        dyedCable(helper, red, DyeColor.RED);
        dyedCable(helper, blue, DyeColor.BLUE);
        check(!helper.getBlockState(red).getValue(CableBlock.cableProperty(Direction.EAST)), "Red and blue cables should not link");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.POTION, Potions.WATER));
        BlockPos absolute = helper.absolutePos(blue);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        check(helper.getBlockState(blue).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                .consumesAction(), "A Water Bottle should wash a blue cable");
        check(helper.getBlockEntity(blue) instanceof CableBlockEntity cable && cable.color() == null, "The cable should be undyed");
        check(player.getMainHandItem().is(Items.GLASS_BOTTLE), "Washing should leave a Glass Bottle: " + player.getMainHandItem());
        check(helper.getBlockState(red).getValue(CableBlock.cableProperty(Direction.EAST)), "The washed cable should link to the red one");
        helper.succeed();
    }

    private static void dyedCable(GameTestHelper helper, BlockPos pos, DyeColor color) {
        helper.setBlock(pos, ModBlocks.CABLE.get());
        if (!(helper.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            throw new IllegalStateException("No cable at " + pos);
        }
        cable.setColor(color);
        CableBlock.refreshCableLinks(helper.getLevel(), helper.absolutePos(pos));
    }

    private static void connectorCable(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.CABLE.get());
        BlockPos absolute = helper.absolutePos(pos);
        if (!(helper.getLevel().getBlockEntity(absolute) instanceof CableBlockEntity cable)
                || !cable.installUniversalConnector(Direction.EAST)) {
            throw new IllegalStateException("Could not install a Universal Connector at " + pos);
        }
        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.getLevel().setBlock(absolute, state.setValue(CableBlock.connectorProperty(Direction.EAST), true), Block.UPDATE_ALL);
    }

    private static void toggleActive(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        BaseMachineBlock.setActive(helper.getLevel(), absolute, state, !state.getValue(BaseMachineBlock.ACTIVE));
    }

    private static long version(GameTestHelper helper, BlockPos pos) {
        return CableBlockEntity.networkVersion(helper.getLevel(), helper.absolutePos(pos));
    }
}
