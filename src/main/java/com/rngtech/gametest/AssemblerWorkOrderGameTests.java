package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.BatteryAssemblerMenu;
import com.rngtech.content.menu.DriveBaySlot;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderOperations;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderConditions;
import com.rngtech.rpg.workorder.WorkOrderFamilies;
import com.rngtech.rpg.workorder.WorkPart;
import com.rngtech.rpg.workorder.WorkReport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Work Orders on the Component Assembler, a machine without Mastery or chassis stages. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssemblerWorkOrderGameTests {
    private AssemblerWorkOrderGameTests() {
    }

    /** A finished assembly reports its output, and a Battery Cell's material stage. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void assembliesReportTheirOutput(GameTestHelper helper) {
        WorkReport cell = BatteryAssemblerBlockEntity.assemblyReport(new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("copper_battery_cell"))));
        WorkReport servo = BatteryAssemblerBlockEntity.assemblyReport(new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("titanium_servo")), 2));
        check(cell.type() == WorkObjectiveType.ASSEMBLE && cell.stage() == 2 && cell.item().equals("rngtech:copper_battery_cell") && cell.amount() == 1,
                "A Copper Battery Cell did not report as a Stage 2 assembly: " + cell);
        check(servo.stage() == 0 && servo.amount() == 2, "A servo reported a cell stage: " + servo);
        helper.succeed();
    }

    /**
     * The Assembler loads its own family's chips through its Orders tab with no stage requirement, counts assemblies,
     * pays its conditions' costs while a chip is active, and keeps its chips on its drop.
     */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void assemblerRunsJobChips(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        BatteryAssemblerBlockEntity assembler = MachineTestSupport.place(helper, pos, ModBlocks.BATTERY_ASSEMBLER.get(), BatteryAssemblerBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MachineTraits starved = new MachineTraits(Rarity.MAGIC, 0,
                List.of(WorkOrderConditions.get(WorkOrderConditions.STARVED_FEED).definition().roll(3, RandomSource.create(4L))), List.of());
        WorkOrder cells = new WorkOrder(WorkOrderFamilies.COMPONENT_ASSEMBLER, 3, new WorkObjective(RNGTech.id("cells"),
                new WorkPart(WorkObjectiveType.ASSEMBLE, WorkFilter.STAGE, 2, "", 3)), 0, 0, WorkOrder.BASE_BOUNTY, 0);
        WorkOrder crusher = new WorkOrder("crusher", 1, new WorkObjective(RNGTech.id("crush"),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.ANY, 0, "", 10)), 4, 0, WorkOrder.BASE_BOUNTY, 0);
        player.getInventory().setItem(0, JobChipItem.of(ModItems.JOB_CHIP.get(), cells, starved));
        player.getInventory().setItem(1, JobChipItem.of(ModItems.JOB_CHIP.get(), crusher, MachineTraits.EMPTY));
        double speed = assembler.effectiveStats().value(MachineStat.PROCESSING_SPEED);

        BatteryAssemblerMenu menu = (BatteryAssemblerMenu) assembler.createMenu(1, player.getInventory(), player);
        check(menu.clickMenuButton(player, WorkOrderMenuSupport.BUTTON_SELECT_TAB + BatteryAssemblerMenu.TAB_ORDERS)
                && menu.selectedTab() == BatteryAssemblerMenu.TAB_ORDERS, "The Assembler menu did not open the Orders tab");
        shiftClick(menu, player, 1);
        check(assembler.workOrders().isEmpty(), "The Assembler loaded a Crusher chip");
        shiftClick(menu, player, 0);
        check(assembler.workOrders().orders().size() == 1 && assembler.workOrders().active() == MachineWorkOrders.NONE
                && WorkOrderOperations.perform(player, assembler, "activate", "0") && assembler.workOrders().active() == 0,
                "The Assembler did not load its own chip with no stage requirement: " + assembler.workOrders());
        check(menu.workOrderView().slots().size() == 1 && menu.workOrderView().slots().getFirst().tier() == 3,
                "The Assembler did not sync its chip to the Orders tab");
        double slowed = assembler.effectiveStats().value(MachineStat.PROCESSING_SPEED);
        check(slowed < speed * 0.76, "Starved Feed did not slow the Assembler: " + speed + " -> " + slowed);

        assembler.workOrderWork(BatteryAssemblerBlockEntity.assemblyReport(new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("titanium_servo")))));
        assembler.workOrderWork(BatteryAssemblerBlockEntity.assemblyReport(new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("copper_battery_cell")), 2)));
        check(assembler.workOrders().activeOrder().totalProgress() == 2, "Only the Battery Cells should count: " + assembler.workOrders());

        List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), assembler);
        check(drops.stream().anyMatch(stack -> assembler.workOrders().equals(stack.get(ModDataComponents.WORK_ORDERS.get()))),
                "The dropped Assembler lost its chips: " + drops);
        menu.clicked(firstBay(menu), 0, ClickType.PICKUP, player);
        check(assembler.workOrders().isEmpty() && JobChipItem.isWorkOrder(menu.getCarried()), "The chip did not come back out of its bay");
        helper.succeed();
    }

    private static void shiftClick(BatteryAssemblerMenu menu, Player player, int inventorySlot) {
        for (int index = 0; index < firstBay(menu); index++) {
            Slot slot = menu.getSlot(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == inventorySlot) {
                menu.quickMoveStack(player, index);
                return;
            }
        }
    }

    /** The menu index of the first drive bay: the bays follow the player inventory, ahead of the jewel slots. */
    private static int firstBay(BatteryAssemblerMenu menu) {
        for (int index = 0; index < menu.slots.size(); index++) {
            if (menu.getSlot(index) instanceof DriveBaySlot) {
                return index;
            }
        }
        throw new IllegalStateException("The menu has no drive bays");
    }
}
