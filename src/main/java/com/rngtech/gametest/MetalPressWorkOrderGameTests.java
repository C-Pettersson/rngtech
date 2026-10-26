package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkPart;
import com.rngtech.rpg.workorder.WorkReport;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Work Orders on the Metal Press: real presses counting toward a chip, and failures breaking a streak. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MetalPressWorkOrderGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private MetalPressWorkOrderGameTests() {
    }

    /** A crude press loads an Iron Plate job through its Orders tab, and its plates finish the job in the bay. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void pressesCountTowardTheirJob(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.CRUDE_METAL_PRESS.get(), MetalPressBlockEntity.class);
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD, new ItemStack(ModItems.PLATE_MOLD.get()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        WorkOrder plates = new WorkOrder("metal_press", 1, new WorkObjective(RNGTech.id("plates"),
                new WorkPart(WorkObjectiveType.PRESS, WorkFilter.ITEM, 0, "rngtech:iron_plate", 2)), 3, 0, WorkOrder.BASE_BOUNTY, 0);
        player.getInventory().setItem(0, JobChipItem.of(ModItems.JOB_CHIP.get(), plates, MachineTraits.EMPTY));
        MetalPressMenu menu = (MetalPressMenu) press.createMenu(1, player.getInventory(), player);
        check(menu.clickMenuButton(player, WorkOrderMenuSupport.BUTTON_SELECT_TAB + MetalPressMenu.TAB_ORDERS)
                && menu.selectedTab() == MetalPressMenu.TAB_ORDERS, "The Metal Press menu did not open the Orders tab");
        for (int index = 0; index < menu.slots.size() - MachineWorkOrders.SLOTS; index++) {
            Slot slot = menu.getSlot(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == 0) {
                menu.quickMoveStack(player, index);
            }
        }
        check(press.workOrders().orders().size() == 1 && press.workOrders().active() == 0, "The crude press did not load its chip: " + press.workOrders());

        press.getProcessInventory().setStackInSlot(MetalPressBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 4));
        MachineTestSupport.powerEachTick(helper, press.getEnergyStorage(null));
        helper.succeedWhen(() -> {
            LoadedWorkOrder job = press.workOrders().orders().isEmpty() ? null : press.workOrders().orders().getFirst();
            check(job != null && job.fulfilled(), "Waiting for two plates to finish the job: " + press.workOrders() + "; " + MachineTestSupport.describe(press));
        });
    }

    /** A failure sets a running streak back to 0 and keeps the best streak. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void failuresBreakStreaks(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.CRUDE_METAL_PRESS.get(), MetalPressBlockEntity.class);
        WorkOrder streak = new WorkOrder("metal_press", 1, new WorkObjective(RNGTech.id("streak"),
                new WorkPart(WorkObjectiveType.PRESS, WorkFilter.STREAK, 0, "", 50)), 3, 0, WorkOrder.BASE_BOUNTY, 0);
        press.setWorkOrders(MachineWorkOrders.EMPTY.withLoaded(new LoadedWorkOrder(streak, MachineTraits.EMPTY)));
        press.workOrderWork(new WorkReport(WorkObjectiveType.PRESS, 1, 0, "rngtech:iron_plate", 12));
        check(press.workOrders().activeOrder().totalProgress() == 12, "The streak did not count the presses");
        press.workOrderStreakBroken();
        LoadedWorkOrder broken = press.workOrders().activeOrder();
        check(broken.totalProgress() == 0 && broken.bestStreak() == 12, "A failure did not break the streak and keep the best: " + broken);
        helper.succeed();
    }
}
