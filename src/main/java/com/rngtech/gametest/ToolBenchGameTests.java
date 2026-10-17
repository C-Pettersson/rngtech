package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.ToolBenchBlock;
import com.rngtech.content.blockentity.ToolBenchBlockEntity;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.menu.ToolBenchMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tool Bench Gear: shift-clicks into its slots from any tab, and shows on the block once installed. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ToolBenchGameTests {
    private static final BlockPos BENCH = new BlockPos(2, 2, 2);
    /** Menu index of the first hotbar slot: eleven bench slots, then 27 inventory slots. */
    private static final int FIRST_HOTBAR_SLOT = 11 + 27;

    private ToolBenchGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void gearShiftClicksFromBuildTab(GameTestHelper helper) {
        helper.setBlock(BENCH, ModBlocks.TOOL_BENCH.get());
        ToolBenchBlockEntity bench = (ToolBenchBlockEntity) helper.getBlockEntity(BENCH);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModItems.TINY_ANVIL.get()));
        player.getInventory().setItem(1, new ItemStack(ModItems.energyConnector(EnergyConnectorTier.BASIC).get()));
        ToolBenchMenu menu = new ToolBenchMenu(0, player.getInventory(), bench);
        check(menu.selectedTab() == ToolBenchMenu.TAB_ASSEMBLY, "The menu should open on the Build tab");

        menu.quickMoveStack(player, FIRST_HOTBAR_SLOT);
        menu.quickMoveStack(player, FIRST_HOTBAR_SLOT + 1);

        check(bench.getInventory().getStackInSlot(ToolBenchBlockEntity.SLOT_TINY_ANVIL).is(ModItems.TINY_ANVIL.get()),
                "A shift-clicked Tiny Anvil should install from the Build tab");
        check(!bench.getInventory().getStackInSlot(ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR).isEmpty(),
                "A shift-clicked Energy Connector should install from the Build tab");
        check(player.getInventory().getItem(0).isEmpty() && player.getInventory().getItem(1).isEmpty(),
                "Installed Gear should leave the player's hotbar");
        check(helper.getBlockState(BENCH).getValue(ToolBenchBlock.HAS_TINY_ANVIL), "The bench should show the installed anvil");
        check(helper.getBlockState(BENCH).getValue(ToolBenchBlock.HAS_ENERGY_CONNECTOR), "The bench should show the cable port");
        helper.succeed();
    }

    /** The cable port shows only while an Energy Connector is installed. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void cablePortFollowsConnector(GameTestHelper helper) {
        helper.setBlock(BENCH, ModBlocks.TOOL_BENCH.get());
        ToolBenchBlockEntity bench = (ToolBenchBlockEntity) helper.getBlockEntity(BENCH);
        check(!helper.getBlockState(BENCH).getValue(ToolBenchBlock.HAS_ENERGY_CONNECTOR), "A new bench should have no cable port");
        bench.getInventory().setStackInSlot(
                ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR,
                new ItemStack(ModItems.energyConnector(EnergyConnectorTier.BASIC).get())
        );
        check(helper.getBlockState(BENCH).getValue(ToolBenchBlock.HAS_ENERGY_CONNECTOR), "An installed connector should show the port");
        bench.getInventory().setStackInSlot(ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR, ItemStack.EMPTY);
        check(!helper.getBlockState(BENCH).getValue(ToolBenchBlock.HAS_ENERGY_CONNECTOR), "Removing the connector should hide the port");
        helper.succeed();
    }
}
