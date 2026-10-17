package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.RNGTechConfig;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.DormantMachineBlockEntity;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.dormant.DormantPhase;
import com.rngtech.content.dormant.DormantSiteCatalog;
import com.rngtech.content.dormant.DormantSiteIndex;
import com.rngtech.content.dormant.DormantSites;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.SoulTransferItem;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.dormant.DormantSoul;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.SoulTransfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Dormant Machines wake on FE or carried cells, give their soul to a Soul Transfer, and reroll on operator commands. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DormantMachineGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private DormantMachineGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void feWakesAWorkshop(GameTestHelper helper) {
        DormantMachineBlockEntity machine = place(helper);
        MachineTestSupport.powerEachTick(helper, machine.getEnergyStorage(null));
        helper.succeedWhen(() -> check(machine.phase() == DormantPhase.AWAKE, "FE did not wake the machine: " + MachineTestSupport.describe(machine)));
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void cellsDrainTogetherAndOnlyEmptiesComeOutBelow(GameTestHelper helper) {
        DormantMachineBlockEntity machine = place(helper);
        for (int slot = 0; slot < DormantMachineBlockEntity.CELL_SLOTS; slot++) {
            machine.cells().setStackInSlot(slot, chargedCell(BatteryCellMaterial.LEAD));
        }
        check(machine.getItemHandler(Direction.DOWN).extractItem(0, 1, true).isEmpty(),
                "A charged cell came out from below");
        check(machine.getItemHandler(Direction.UP).extractItem(0, 1, true).isEmpty(),
                "A cell came out from above");
        helper.startSequence()
                .thenExecuteAfter(2, () -> check(machine.charge() >= 9 * 128, "Nine cells did not drain together on one tick: " + machine.charge()))
                .thenWaitUntil(() -> check(machine.phase() == DormantPhase.AWAKE, "Cells did not wake the machine: " + MachineTestSupport.describe(machine)))
                .thenSucceed();
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void soulTransferTakesAnAwakeSoulAndLeavesAHusk(GameTestHelper helper) {
        DormantMachineBlockEntity machine = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    ItemStack early = new ItemStack(ModItems.SOUL_TRANSFER.get());
                    check(SoulTransferItem.useOnDormant(player, early, machine) == InteractionResult.FAIL && !early.isEmpty(),
                            "A sleeping machine gave up its soul");
                    MachineTestSupport.powerEachTick(helper, machine.getEnergyStorage(null));
                })
                .thenWaitUntil(() -> check(machine.phase() == DormantPhase.AWAKE, "The machine never woke"))
                .thenExecute(() -> {
                    DormantSoul soul = machine.soul();
                    ItemStack stack = new ItemStack(ModItems.SOUL_TRANSFER.get());
                    check(SoulTransferItem.useOnDormant(player, stack, machine) == InteractionResult.SUCCESS, "An uncalibrated Soul Transfer should absorb");
                    SoulTransfer.Charge charge = stack.get(ModDataComponents.SOUL_CHARGE.get());
                    check(charge != null && charge.family() == soul.family() && charge.state().equals(soul.state()), "The Soul Transfer does not hold the soul");
                    check(charge.releaseStage() == 1, "The Collapsed Workshop's release stage was not recorded");
                    check(machine.phase() == DormantPhase.SPENT && machine.soul() == null, "The machine was not spent");
                    check(SoulTransferItem.useOnDormant(player, new ItemStack(ModItems.SOUL_TRANSFER.get()), machine) == InteractionResult.FAIL,
                            "A husk gave up a second soul");
                })
                .thenSucceed();
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void failedAbsorbFollowsThePackSetting(GameTestHelper helper) {
        DormantMachineBlockEntity keeps = place(helper, POS);
        DormantMachineBlockEntity husks = place(helper, new BlockPos(4, 1, 1));
        MachineTestSupport.powerEachTick(helper, keeps.getEnergyStorage(null));
        MachineTestSupport.powerEachTick(helper, husks.getEnergyStorage(null));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenWaitUntil(() -> check(keeps.phase() == DormantPhase.AWAKE && husks.phase() == DormantPhase.AWAKE, "The machines never woke"))
                .thenExecute(() -> {
                    DormantSoul soul = keeps.soul();
                    ItemStack doomed = failingTransfer();
                    SoulTransferItem.useOnDormant(player, doomed, keeps);
                    check(doomed.isEmpty(), "A failed absorb kept the Soul Transfer");
                    check(keeps.phase() == DormantPhase.AWAKE && keeps.soul().equals(soul), "By default a failed absorb changed the soul");

                    RNGTechConfig.DormantFailedAbsorb previous = RNGTechConfig.DORMANT_FAILED_ABSORB.get();
                    RNGTechConfig.DORMANT_FAILED_ABSORB.set(RNGTechConfig.DormantFailedAbsorb.HUSK);
                    try {
                        SoulTransferItem.useOnDormant(player, failingTransfer(), husks);
                    } finally {
                        RNGTechConfig.DORMANT_FAILED_ABSORB.set(previous);
                    }
                    check(husks.phase() == DormantPhase.SPENT && husks.soul() == null, "HUSK did not destroy the soul");
                })
                .thenSucceed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void releaseStageGatesTheTarget(GameTestHelper helper) {
        BlockPos ironPos = new BlockPos(1, 1, 1);
        BlockPos steelPos = new BlockPos(3, 1, 1);
        CrusherBlockEntity iron = MachineTestSupport.place(helper, ironPos, ModBlocks.crusherChassis(CrusherChassisMaterial.IRON).get(), CrusherBlockEntity.class);
        CrusherBlockEntity steel = MachineTestSupport.place(helper, steelPos, ModBlocks.crusherChassis(CrusherChassisMaterial.STEEL).get(), CrusherBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MachineProgressionState state = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER).withAddedXp(MachineProgressionState.xpForLevel(12));
        ItemStack stack = new ItemStack(ModItems.SOUL_TRANSFER.get());
        stack.set(ModDataComponents.SOUL_CHARGE.get(), new SoulTransfer.Charge(MachineMasteryFamily.CRUSHER, state, CrusherChassisMaterial.STEEL.stage()));
        check(SoulTransferItem.useOnHost(player, stack, iron, helper.absolutePos(ironPos)) == InteractionResult.FAIL && iron.masteryState().level() == 1,
                "A soul went into a machine below its release stage");
        check(SoulTransferItem.useOnHost(player, stack, steel, helper.absolutePos(steelPos)) == InteractionResult.SUCCESS && steel.masteryState().level() == 12,
                "A soul did not release at its release stage");
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void resetAndRestoreReroll(GameTestHelper helper) {
        DormantMachineBlockEntity machine = place(helper);
        BlockPos absolute = helper.absolutePos(POS);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    var level = helper.getLevel();
                    var random = level.getRandom();
                    DormantSiteIndex index = new DormantSiteIndex();
                    GlobalPos key = DormantSiteIndex.key(level, absolute);
                    index.register(key, DormantSiteCatalog.DEFAULT_SITE, DormantSiteIndex.State.CHARGED);
                    machine.getEnergyStorage(null).receiveEnergy(500, false);
                    check(machine.charge() == 500, "The machine did not take charge");

                    check(index.reset(false, random) == 0, "A plain reset touched a charged site");
                    check(index.reset(true, random) == 1, "A forced reset skipped a charged site");
                    check(DormantSites.apply(level, index, new ArrayList<>()) == 1 && machine.charge() == 0
                            && machine.phase() == DormantPhase.ASLEEP && !index.entry(key).pending(), "A forced reset did not reroll and clear the charge");

                    DormantSoul rolled = machine.soul();
                    machine.setSoulState(SoulTransfer.afterFailure(rolled.state(), 90, 0, candidate -> true));
                    index.restore(false, random);
                    DormantSites.apply(level, index, new ArrayList<>());
                    check(machine.soul().equals(rolled), "restore same did not bring back the last rolled soul");

                    index.restore(true, random);
                    DormantSites.apply(level, index, new ArrayList<>());
                    check(!machine.soul().equals(rolled), "restore random kept the same soul");

                    level.setBlock(absolute, Blocks.AIR.defaultBlockState(), 3);
                    index.restore(false, random);
                    check(DormantSites.apply(level, index, new ArrayList<>()) == 1
                            && level.getBlockEntity(absolute) instanceof DormantMachineBlockEntity, "A mined-out site did not come back on empty ground");

                    level.setBlock(absolute, Blocks.STONE.defaultBlockState(), 3);
                    index.restore(false, random);
                    List<BlockPos> skipped = new ArrayList<>();
                    check(DormantSites.apply(level, index, skipped) == 0 && skipped.contains(absolute) && level.getBlockState(absolute).is(Blocks.STONE),
                            "A built-over site was replaced");
                })
                .thenSucceed();
    }

    private static DormantMachineBlockEntity place(GameTestHelper helper) {
        return place(helper, POS);
    }

    private static DormantMachineBlockEntity place(GameTestHelper helper, BlockPos pos) {
        return MachineTestSupport.place(helper, pos, ModBlocks.DORMANT_MACHINE.get(), DormantMachineBlockEntity.class);
    }

    private static ItemStack chargedCell(BatteryCellMaterial material) {
        ItemStack cell = new ItemStack(ModItems.BATTERY_CELLS.get(material).get());
        int capacity = BatteryCellItem.energyCapacity(cell);
        BatteryCellItem.setEnergy(cell, capacity, capacity);
        return cell;
    }

    private static ItemStack failingTransfer() {
        ItemStack stack = new ItemStack(ModItems.SOUL_TRANSFER.get());
        stack.set(ModDataComponents.SOURCE_CALIBRATION.get(), new CalibrationState(CalibrationFamily.LOGIC, 2, 0, 0));
        return stack;
    }
}
