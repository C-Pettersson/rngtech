package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.item.SoulTransferItem;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A Soul Transfer moves one machine's Mastery into one other machine of the same family. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulTransferGameTests {
    private SoulTransferGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void soulMovesFromOneCrusherToAnother(GameTestHelper helper) {
        CrusherBlockEntity source = crusher(helper, new BlockPos(1, 1, 1), CrusherChassisMaterial.IRON);
        CrusherBlockEntity target = crusher(helper, new BlockPos(3, 1, 1), CrusherChassisMaterial.STEEL);
        BlockPos furnacePos = new BlockPos(5, 1, 1);
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, furnacePos,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.IRON).get(), FurnaceBlockEntity.class);
        source.setMachineProgression(levelled(10));
        target.setMachineProgression(levelled(3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModItems.SOUL_TRANSFER.get());

        check(use(player, stack, source) == InteractionResult.SUCCESS, "An uncalibrated Soul Transfer should always absorb");
        check(source.masteryState().xp() == 0 && source.masteryState().level() == 1, "The source kept its Mastery after an absorb");
        check(stack.get(ModDataComponents.SOUL_CHARGE.get()).state().level() == 10, "The Soul Transfer does not hold the absorbed level");

        check(SoulTransferItem.useOnHost(player, stack, furnace, helper.absolutePos(furnacePos)) == InteractionResult.FAIL,
                "A Crusher soul went into a Furnace");
        check(use(player, stack, target) == InteractionResult.FAIL && target.masteryState().level() == 3,
                "A release replaced a machine's Mastery without a sneak-use");

        player.setShiftKeyDown(true);
        check(use(player, stack, target) == InteractionResult.SUCCESS, "A confirmed release failed");
        check(target.masteryState().equals(levelled(10)), "The target did not receive the whole soul");
        check(stack.isEmpty(), "The Soul Transfer was not used up by the release");
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void failedAbsorbOnlyUsesUpTheItem(GameTestHelper helper) {
        CrusherBlockEntity source = crusher(helper, new BlockPos(1, 1, 1), CrusherChassisMaterial.IRON);
        source.setMachineProgression(levelled(10));
        ItemStack stack = new ItemStack(ModItems.SOUL_TRANSFER.get());
        stack.set(ModDataComponents.SOURCE_CALIBRATION.get(), new CalibrationState(CalibrationFamily.LOGIC, 2, 0, 0));
        check(SoulTransferItem.successChance(stack) == 0, "Zero stability should never absorb");

        use(helper.makeMockPlayer(GameType.SURVIVAL), stack, source);
        check(stack.isEmpty(), "A failed absorb kept the Soul Transfer");
        check(source.masteryState().equals(levelled(10)), "A failed absorb changed the machine's Mastery by default");
        helper.succeed();
    }

    private static InteractionResult use(Player player, ItemStack stack, CrusherBlockEntity crusher) {
        return SoulTransferItem.useOnHost(player, stack, crusher, crusher.getBlockPos());
    }

    private static CrusherBlockEntity crusher(GameTestHelper helper, BlockPos pos, CrusherChassisMaterial material) {
        return MachineTestSupport.place(helper, pos, ModBlocks.crusherChassis(material).get(), CrusherBlockEntity.class);
    }

    private static MachineProgressionState levelled(int level) {
        return MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER).withAddedXp(MachineProgressionState.xpForLevel(level));
    }
}
