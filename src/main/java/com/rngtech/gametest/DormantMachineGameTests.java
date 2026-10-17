package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.RNGTechConfig;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.DormantMachineBlockEntity;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.dormant.DormantCore;
import com.rngtech.content.dormant.DormantLayout;
import com.rngtech.content.dormant.DormantPhase;
import com.rngtech.content.dormant.DormantSiteCatalog;
import com.rngtech.content.dormant.DormantSiteIndex;
import com.rngtech.content.dormant.DormantSitePiece;
import com.rngtech.content.dormant.DormantSites;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.entity.DerelictCartEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Dormant Machines wake on FE or carried cells, give their soul to a Soul Transfer, and reroll on operator commands. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DormantMachineGameTests {
    private static final String SITE_TEMPLATE = "gametest_dormant_site";
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private DormantMachineGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void feWakesAWorkshop(GameTestHelper helper) {
        DormantMachineBlockEntity block = place(helper, POS);
        MachineTestSupport.powerEachTick(helper, block.getEnergyStorage(null));
        helper.succeedWhen(() -> check(block.phase() == DormantPhase.AWAKE, "FE did not wake the machine: " + MachineTestSupport.describe(block)));
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void cellsDrainTogetherAndOnlyEmptiesComeOutBelow(GameTestHelper helper) {
        DormantMachineBlockEntity block = place(helper, POS);
        DormantCore machine = block.core();
        fillCells(machine, BatteryCellMaterial.LEAD);
        check(block.getItemHandler(Direction.DOWN).extractItem(0, 1, true).isEmpty(), "A charged cell came out from below");
        check(block.getItemHandler(Direction.UP).extractItem(0, 1, true).isEmpty(), "A cell came out from above");
        helper.startSequence()
                .thenExecuteAfter(2, () -> check(machine.charge() >= 9 * 128, "Nine cells did not drain together on one tick: " + machine.charge()))
                .thenWaitUntil(() -> check(machine.phase() == DormantPhase.AWAKE, "Cells did not wake the machine: " + MachineTestSupport.describe(block)))
                .thenSucceed();
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void soulTransferTakesAnAwakeSoulAndLeavesAHusk(GameTestHelper helper) {
        DormantMachineBlockEntity block = place(helper, POS);
        DormantCore machine = block.core();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    ItemStack early = new ItemStack(ModItems.SOUL_TRANSFER.get());
                    check(SoulTransferItem.useOnDormant(player, early, machine) == InteractionResult.FAIL && !early.isEmpty(),
                            "A sleeping machine gave up its soul");
                    MachineTestSupport.powerEachTick(helper, block.getEnergyStorage(null));
                })
                .thenWaitUntil(() -> check(machine.phase() == DormantPhase.AWAKE, "The machine never woke"))
                .thenExecute(() -> {
                    DormantSoul soul = machine.soul();
                    ItemStack stack = new ItemStack(ModItems.SOUL_TRANSFER.get());
                    check(SoulTransferItem.useOnDormant(player, stack, machine) == InteractionResult.SUCCESS, "An uncalibrated Soul Transfer should absorb");
                    SoulTransfer.Charge charge = stack.get(ModDataComponents.SOUL_CHARGE.get());
                    check(charge != null && charge.family() == soul.family() && charge.state().equals(soul.state()), "The Soul Transfer does not hold the soul");
                    check(charge.releaseStage() == 0, "A default site recorded a release stage");
                    check(machine.phase() == DormantPhase.SPENT && machine.soul() == null, "The machine was not spent");
                    check(SoulTransferItem.useOnDormant(player, new ItemStack(ModItems.SOUL_TRANSFER.get()), machine) == InteractionResult.FAIL,
                            "A husk gave up a second soul");
                })
                .thenSucceed();
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void failedAbsorbFollowsThePackSetting(GameTestHelper helper) {
        DormantMachineBlockEntity keepsBlock = place(helper, POS);
        DormantMachineBlockEntity husksBlock = place(helper, new BlockPos(3, 1, 1));
        DormantCore keeps = keepsBlock.core();
        DormantCore husks = husksBlock.core();
        MachineTestSupport.powerEachTick(helper, keepsBlock.getEnergyStorage(null));
        MachineTestSupport.powerEachTick(helper, husksBlock.getEnergyStorage(null));
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
        DormantMachineBlockEntity block = place(helper, POS);
        DormantCore machine = block.core();
        BlockPos absolute = helper.absolutePos(POS);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    ServerLevel level = helper.getLevel();
                    var random = level.getRandom();
                    DormantSiteIndex index = new DormantSiteIndex();
                    GlobalPos key = DormantSiteIndex.key(level, absolute);
                    index.register(key, DormantSiteCatalog.DEFAULT_SITE, DormantSiteIndex.State.CHARGED, false);
                    block.getEnergyStorage(null).receiveEnergy(500, false);
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

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void derelictCartWakesFromCellsAndBreaksIntoScrap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(new BlockPos(2, 1, 2));
        DerelictCartEntity cart = DormantSites.spawnCart(level, absolute, Direction.NORTH);
        cart.core().setSite(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "abandoned_cart_siding"));
        level.addFreshEntity(cart);
        fillCells(cart.core(), BatteryCellMaterial.LEAD);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.startSequence()
                .thenExecuteAfter(1, () -> check(!cart.hurt(level.damageSources().playerAttack(player), 10.0F) && cart.isAlive(),
                        "A cart holding a soul broke"))
                .thenWaitUntil(() -> check(cart.phase() == DormantPhase.AWAKE, "Cells did not wake the cart"))
                .thenExecute(() -> {
                    check(cart.core().soul().family() == MachineMasteryFamily.FORESTRY, "An Abandoned Cart Siding rolled a non-Forestry soul");
                    ItemStack stack = new ItemStack(ModItems.SOUL_TRANSFER.get());
                    check(SoulTransferItem.useOnDormant(player, stack, cart.core()) == InteractionResult.SUCCESS
                            && stack.has(ModDataComponents.SOUL_CHARGE.get()), "The cart's soul was not absorbed");
                    check(cart.phase() == DormantPhase.SPENT, "The cart was not spent");
                    check(cart.hurt(level.damageSources().playerAttack(player), 1.0F) && cart.isRemoved(), "A spent cart did not break");
                    List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(2.0D));
                    check(drops.stream().anyMatch(drop -> drop.getItem().is(ModItems.materialItem("scrap").get())), "A spent cart dropped no Scrap");
                    check(drops.stream().filter(drop -> drop.getItem().getItem() instanceof BatteryCellItem).count() == DormantCore.CELL_SLOTS,
                            "A broken cart kept its cells");
                })
                .thenSucceed();
    }

    @GameTest(template = SITE_TEMPLATE)
    public static void workshopSiteHoldsOneMachine(GameTestHelper helper) {
        siteHoldsOneMachine(helper, DormantLayout.WORKSHOP, "collapsed_workshop");
    }

    @GameTest(template = SITE_TEMPLATE)
    public static void vaultSiteHoldsOneMachine(GameTestHelper helper) {
        siteHoldsOneMachine(helper, DormantLayout.VAULT, "buried_calibration_vault");
    }

    @GameTest(template = SITE_TEMPLATE)
    public static void foundrySiteHoldsOneMachine(GameTestHelper helper) {
        siteHoldsOneMachine(helper, DormantLayout.FOUNDRY, "flooded_foundry");
    }

    @GameTest(template = SITE_TEMPLATE)
    public static void smeltworksSiteHoldsOneMachine(GameTestHelper helper) {
        siteHoldsOneMachine(helper, DormantLayout.SMELTWORKS, "nether_smeltworks");
    }

    @GameTest(template = SITE_TEMPLATE)
    public static void relaySiteHoldsOneMachine(GameTestHelper helper) {
        siteHoldsOneMachine(helper, DormantLayout.RELAY, "end_relay");
    }

    @GameTest(template = SITE_TEMPLATE)
    public static void sidingSiteHoldsOneDerelictCart(GameTestHelper helper) {
        BoundingBox box = build(helper, DormantLayout.SIDING, "abandoned_cart_siding");
        List<DerelictCartEntity> carts = helper.getLevel().getEntitiesOfClass(DerelictCartEntity.class, AABB.of(box).inflate(1.0D));
        check(carts.size() == 1, "The siding should hold one derelict cart, found " + carts.size());
        check(carts.getFirst().core().site().getPath().equals("abandoned_cart_siding"), "The cart names the wrong site");
        check(helper.getLevel().getBlockState(carts.getFirst().sitePos()).is(Blocks.RAIL), "The cart does not stand on a rail");
        carts.getFirst().discard();
        helper.succeed();
    }

    private static void siteHoldsOneMachine(GameTestHelper helper, DormantLayout layout, String site) {
        BoundingBox box = build(helper, layout, site);
        List<DormantMachineBlockEntity> machines = new ArrayList<>();
        BlockPos.betweenClosedStream(box).forEach(pos -> {
            if (helper.getLevel().getBlockEntity(pos) instanceof DormantMachineBlockEntity machine) { machines.add(machine); }
        });
        check(machines.size() == 1, layout + " should hold one Dormant Machine, found " + machines.size());
        check(machines.getFirst().core().site().getPath().equals(site) && DormantSiteCatalog.get(machines.getFirst().core().site()) != null,
                layout + " names a missing site: " + machines.getFirst().core().site());
        helper.succeed();
    }

    /** Builds one site's ruin inside the test area, the way world generation does. */
    private static BoundingBox build(GameTestHelper helper, DormantLayout layout, String site) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        DormantSitePiece piece = new DormantSitePiece(layout, ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, site),
                origin.getX(), origin.getY(), origin.getZ(), Direction.NORTH);
        BoundingBox box = piece.getBoundingBox();
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box, new ChunkPos(origin), origin);
        return box;
    }

    private static DormantMachineBlockEntity place(GameTestHelper helper, BlockPos pos) {
        return MachineTestSupport.place(helper, pos, ModBlocks.DORMANT_MACHINE.get(), DormantMachineBlockEntity.class);
    }

    private static void fillCells(DormantCore core, BatteryCellMaterial material) {
        for (int slot = 0; slot < DormantCore.CELL_SLOTS; slot++) {
            ItemStack cell = new ItemStack(ModItems.BATTERY_CELLS.get(material).get());
            int capacity = BatteryCellItem.energyCapacity(cell);
            BatteryCellItem.setEnergy(cell, capacity, capacity);
            core.cells().setStackInSlot(slot, cell);
        }
    }

    private static ItemStack failingTransfer() {
        ItemStack stack = new ItemStack(ModItems.SOUL_TRANSFER.get());
        stack.set(ModDataComponents.SOURCE_CALIBRATION.get(), new CalibrationState(CalibrationFamily.LOGIC, 2, 0, 0));
        return stack;
    }
}
