package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.DispatchTerminalBlockEntity;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderDiscovery;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Machines hide their Orders tab until the player finds Work Orders. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkOrderDiscoveryGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final BlockPos TERMINAL = new BlockPos(4, 1, 2);

    private WorkOrderDiscoveryGameTests() {
    }

    /** A new player sees no Orders tab, and carrying a chip to the machine shows it from then on. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void carryingAChipShowsTheTab(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.CRUDE_METAL_PRESS.get(), MetalPressBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        check(!menu(press, player).workOrdersShown(), "A new player sees the Orders tab");

        player.getInventory().setItem(0, new ItemStack(ModItems.JOB_CHIP.get()));
        check(menu(press, player).workOrdersShown(), "Carrying a Job Chip did not show the Orders tab");
        player.getInventory().clearContent();
        check(menu(press, player).workOrdersShown(), "The Orders tab hid again once the chip left the inventory");
        helper.succeed();
    }

    /** Opening a Dispatch Terminal finds Work Orders. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void openingTheTerminalShowsTheTab(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.CRUDE_METAL_PRESS.get(), MetalPressBlockEntity.class);
        DispatchTerminalBlockEntity terminal = MachineTestSupport.place(helper, TERMINAL, ModBlocks.DISPATCH_TERMINAL.get(), DispatchTerminalBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        terminal.createMenu(1, player.getInventory(), player);
        check(WorkOrderDiscovery.found(player) && menu(press, player).workOrdersShown(), "Opening a Dispatch Terminal did not show the Orders tab");
        helper.succeed();
    }

    /** A machine with a chip loaded shows its tab to anyone, and finds Work Orders for them. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void aLoadedChipShowsTheTab(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.CRUDE_METAL_PRESS.get(), MetalPressBlockEntity.class);
        WorkOrder plates = new WorkOrder("metal_press", 1, new WorkObjective(RNGTech.id("plates"),
                new WorkPart(WorkObjectiveType.PRESS, WorkFilter.GROUP, 0, "plate", 2)), 3, 0, WorkOrder.BASE_BOUNTY, 0);
        press.setWorkOrders(MachineWorkOrders.EMPTY.withLoaded(new LoadedWorkOrder(plates, MachineTraits.EMPTY)));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        check(menu(press, player).workOrdersShown() && WorkOrderDiscovery.found(player), "A machine with a loaded chip hid its Orders tab");
        helper.succeed();
    }

    private static MetalPressMenu menu(MetalPressBlockEntity press, Player player) {
        return (MetalPressMenu) press.createMenu(1, player.getInventory(), player);
    }
}
