package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.cable.ItemConnectorTier;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.wrench.ConnectorOverlaySnapshot;
import com.rngtech.content.wrench.LinkStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A standalone Universal Connector reports the same link status as one mounted on a cable. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConnectorLinkStatusGameTests {
    private static final BlockPos CABLE = new BlockPos(1, 1, 1);
    private static final BlockPos CABLE_TARGET = new BlockPos(2, 1, 1);
    private static final BlockPos STANDALONE = new BlockPos(1, 1, 3);
    private static final BlockPos STANDALONE_TARGET = new BlockPos(2, 1, 3);

    private ConnectorLinkStatusGameTests() {
    }

    /**
     * Each connector is alone on its network with an item connector and an energy connector installed against a chest.
     * The item module has no partner, the chest has no energy to give, and the status reaches the wrench snapshot.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 40)
    public static void standaloneConnectorReportsTheSameLinkStatusAsACableConnector(GameTestHelper helper) {
        helper.setBlock(CABLE_TARGET, Blocks.CHEST);
        helper.setBlock(STANDALONE_TARGET, Blocks.CHEST);

        helper.setBlock(CABLE, ModBlocks.CABLE.get());
        BlockPos cablePos = helper.absolutePos(CABLE);
        if (!(helper.getLevel().getBlockEntity(cablePos) instanceof CableBlockEntity cable)
                || !cable.installUniversalConnector(Direction.EAST)) {
            throw new IllegalStateException("Could not install a Universal Connector on the cable");
        }
        BlockState cableState = helper.getLevel().getBlockState(cablePos);
        helper.getLevel().setBlock(cablePos, cableState.setValue(CableBlock.connectorProperty(Direction.EAST), true), Block.UPDATE_ALL);
        UniversalConnectorAccess mounted = cable.universalConnectorAccess(Direction.EAST);

        helper.setBlock(STANDALONE, ModBlocks.UNIVERSAL_CONNECTOR.get().defaultBlockState()
                .setValue(UniversalConnectorBlock.FACING, Direction.EAST));
        if (!(helper.getBlockEntity(STANDALONE) instanceof UniversalConnectorBlockEntity standalone)) {
            throw new IllegalStateException("The standalone Universal Connector has no block entity");
        }

        install(mounted);
        install(standalone);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    check(mounted.itemLinkStatus(0) == LinkStatus.NO_PARTNER,
                            "The cable connector's item module reported " + mounted.itemLinkStatus(0));
                    check(standalone.itemLinkStatus(0) == mounted.itemLinkStatus(0),
                            "The standalone connector's item module reported " + standalone.itemLinkStatus(0));
                    check(standalone.energyLinkStatus() == mounted.energyLinkStatus(),
                            "The standalone connector's energy status was " + standalone.energyLinkStatus()
                                    + ", the cable connector's " + mounted.energyLinkStatus());
                    check(standalone.energyLinkStatus().isProblem(), "A chest target reported no energy problem");
                    check(standalone.fluidLinkStatus(0) == mounted.fluidLinkStatus(0), "The fluid module statuses differ");

                    ConnectorOverlaySnapshot snapshot = ConnectorOverlaySnapshot.from(standalone, false, Direction.EAST);
                    check(snapshot.itemModules().get(0).linkStatus() == LinkStatus.NO_PARTNER,
                            "The wrench snapshot dropped the standalone item status");
                    check(snapshot.energyLinkStatus() == standalone.energyLinkStatus(),
                            "The wrench snapshot dropped the standalone energy status");
                })
                .thenSucceed();
    }

    private static void install(UniversalConnectorAccess connector) {
        connector.getInventory().setStackInSlot(
                UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR,
                new ItemStack(ModItems.energyConnector(EnergyConnectorTier.COPPER).get())
        );
        connector.getInventory().setStackInSlot(
                UniversalConnectorBlockEntity.itemConnectorSlot(0),
                new ItemStack(ModItems.itemConnector(ItemConnectorTier.COPPER).get())
        );
    }
}
