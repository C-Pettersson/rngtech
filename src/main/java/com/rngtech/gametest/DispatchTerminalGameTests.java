package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.DispatchTerminalBlock;
import com.rngtech.content.blockentity.DispatchTerminalBlockEntity;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.DispatchTerminalMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderRewards;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** The Dispatch Terminal: a first offer of six orders, take-only slots, Reroll for a Requisition, and offers kept on its drop. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DispatchTerminalGameTests {
    private DispatchTerminalGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void terminalListsDownloadsAndRefreshes(GameTestHelper helper) {
        DispatchTerminalBlockEntity terminal = MachineTestSupport.place(helper, new BlockPos(2, 1, 2), ModBlocks.DISPATCH_TERMINAL.get(), DispatchTerminalBlockEntity.class);
        check(!terminal.filled(), "A new terminal started filled");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        DispatchTerminalMenu menu = (DispatchTerminalMenu) terminal.createMenu(1, player.getInventory(), player);
        check(terminal.filled(), "Opening a new terminal did not fill its offer");
        for (int slot = 0; slot < DispatchTerminalBlockEntity.OFFERS; slot++) {
            check(JobChipItem.data(terminal.offers().getStackInSlot(slot)) != null, "Offer " + slot + " is not a readable order");
        }

        check(!menu.getSlot(0).mayPlace(new ItemStack(ModItems.JOB_CHIP.get())) && !menu.getSlot(0).mayPickup(player),
                "An offer slot accepted a placed order or a direct pickup");
        ItemStack offered = terminal.offers().getStackInSlot(0).copy();
        check(!WorkOrderRewards.items(JobChipItem.data(offered).rewards()).isEmpty(), "An offered order lists no rewards");
        check(menu.clickMenuButton(player, DispatchTerminalMenu.BUTTON_TAKE), "Take failed");
        check(!menu.clickMenuButton(player, DispatchTerminalMenu.BUTTON_TAKE), "Take worked on an empty slot");
        check(terminal.offers().getStackInSlot(0).isEmpty(), "Taking an order did not empty its slot");
        check(ItemStack.isSameItemSameComponents(firstOrder(player), offered), "Taking an order did not put it in the inventory");
        terminal.ensureFilled();
        check(terminal.offers().getStackInSlot(0).isEmpty(), "A filled terminal refilled a taken slot on its own");

        check(!menu.clickMenuButton(player, DispatchTerminalMenu.BUTTON_REROLL), "Reroll worked without a Requisition");
        check(terminal.offers().getStackInSlot(0).isEmpty(), "A refused Reroll changed the offer");
        player.getInventory().add(new ItemStack(ModItems.REQUISITION.get(), 2));
        ItemStack before = terminal.offers().getStackInSlot(1).copy();
        check(menu.clickMenuButton(player, DispatchTerminalMenu.BUTTON_REROLL), "Reroll failed with a Requisition");
        check(!terminal.offers().getStackInSlot(0).isEmpty(), "Reroll did not refill the taken slot");
        check(DispatchTerminalMenu.requisitionSlot(player.getInventory()) >= 0
                && player.getInventory().getItem(DispatchTerminalMenu.requisitionSlot(player.getInventory())).getCount() == 1,
                "Reroll did not spend exactly one Requisition");
        check(!ItemStack.isSameItemSameComponents(before, terminal.offers().getStackInSlot(1))
                        || !ItemStack.isSameItemSameComponents(terminal.offers().getStackInSlot(0), terminal.offers().getStackInSlot(1)),
                "Reroll left the offer unchanged");
        helper.succeed();
    }

    /** The terminal lights one lamp per job still listed. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void lampsFollowTheListing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        DispatchTerminalBlockEntity terminal = MachineTestSupport.place(helper, pos, ModBlocks.DISPATCH_TERMINAL.get(), DispatchTerminalBlockEntity.class);
        check(helper.getBlockState(pos).getValue(DispatchTerminalBlock.LISTINGS) == 0, "A new terminal lights lamps");
        check(terminal.ensureFilled(), "The terminal could not roll an offer");
        check(helper.getBlockState(pos).getValue(DispatchTerminalBlock.LISTINGS) == DispatchTerminalBlockEntity.OFFERS, "A full terminal does not light six lamps");
        terminal.offers().extractItem(4, 1, false);
        check(helper.getBlockState(pos).getValue(DispatchTerminalBlock.LISTINGS) == DispatchTerminalBlockEntity.OFFERS - 1, "Downloading a job left its lamp lit");
        helper.succeed();
    }

    /** A broken terminal keeps its offer, empty slots included, so placing it again never refills it for free. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void droppedTerminalKeepsItsListing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 2);
        DispatchTerminalBlockEntity terminal = MachineTestSupport.place(helper, pos, ModBlocks.DISPATCH_TERMINAL.get(), DispatchTerminalBlockEntity.class);
        check(terminal.ensureFilled(), "The terminal could not roll an offer");
        terminal.offers().extractItem(2, 1, false);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), terminal);
        ItemStack dropped = drops.stream().filter(stack -> stack.is(ModItems.DISPATCH_TERMINAL.get())).findFirst().orElse(ItemStack.EMPTY);
        check(dropped.has(DataComponents.CONTAINER), "The dropped terminal lost its offer");

        DispatchTerminalBlockEntity placed = MachineTestSupport.place(helper, new BlockPos(3, 1, 2), ModBlocks.DISPATCH_TERMINAL.get(), DispatchTerminalBlockEntity.class);
        placed.applyComponentsFromItemStack(dropped);
        check(placed.filled() && placed.offers().getStackInSlot(2).isEmpty(), "The placed terminal refilled its taken slot");
        for (int slot = 0; slot < DispatchTerminalBlockEntity.OFFERS; slot++) {
            check(ItemStack.isSameItemSameComponents(placed.offers().getStackInSlot(slot), terminal.offers().getStackInSlot(slot)),
                    "The placed terminal changed offer " + slot);
        }
        helper.succeed();
    }

    private static ItemStack firstOrder(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (JobChipItem.isWorkOrder(player.getInventory().getItem(slot))) {
                return player.getInventory().getItem(slot);
            }
        }
        return ItemStack.EMPTY;
    }
}
