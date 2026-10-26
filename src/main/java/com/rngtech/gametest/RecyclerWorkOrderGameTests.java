package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.ComponentRecyclerMenu;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.DisassemblyHeadMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderOperations;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderFamilies;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Work Orders on the Component Recycler: its stage requirement, its Orders tab, real recycling, and its drop. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecyclerWorkOrderGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private RecyclerWorkOrderGameTests() {
    }

    /**
     * An Iron recycler takes a Stage 1 chip but not a Stage 4 one, and two Broken Circuits finish its one-item job: each
     * counts its recipe time, so two short recycles make one item of work.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void recyclingCountsTowardTheJob(GameTestHelper helper) {
        ComponentRecyclerBlockEntity recycler = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.componentRecycler(ComponentRecyclerChassis.IRON).get(), ComponentRecyclerBlockEntity.class);
        recycler.getGearInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD,
                new ItemStack(ModItems.disassemblyHead(DisassemblyHeadMaterial.IRON).get()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, chip(4));
        player.getInventory().setItem(1, chip(1));
        ComponentRecyclerMenu menu = (ComponentRecyclerMenu) recycler.createMenu(1, player.getInventory(), player);
        check(menu.clickMenuButton(player, WorkOrderMenuSupport.BUTTON_SELECT_TAB + ComponentRecyclerMenu.TAB_ORDERS)
                && menu.selectedTab() == ComponentRecyclerMenu.TAB_ORDERS, "The recycler menu did not open the Orders tab");
        shiftClick(menu, player, 0);
        check(recycler.workOrders().isEmpty(), "A Stage 1 recycler loaded a Stage 4 chip");
        shiftClick(menu, player, 1);
        check(recycler.workOrders().orders().size() == 1 && menu.workOrderView().slots().size() == 1,
                "The recycler did not load and sync its chip: " + recycler.workOrders());
        check(WorkOrderOperations.perform(player, recycler, "activate", "0"), "The recycler refused to activate its chip");

        recycler.getProcessInventory().setStackInSlot(ComponentRecyclerBlockEntity.SLOT_INPUT,
                new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("broken_circuit")), 2));
        MachineTestSupport.powerEachTick(helper, recycler.getEnergyStorage(null));
        helper.succeedWhen(() -> {
            LoadedWorkOrder job = recycler.workOrders().orders().isEmpty() ? null : recycler.workOrders().orders().getFirst();
            check(job != null && job.fulfilled(), "Waiting for two recycles to make one item of work: " + recycler.workOrders() + "; " + MachineTestSupport.describe(recycler));
            List<ItemStack> drops = Block.getDrops(helper.getBlockState(MACHINE), helper.getLevel(), helper.absolutePos(MACHINE), recycler);
            check(drops.stream().anyMatch(stack -> recycler.workOrders().equals(stack.get(ModDataComponents.WORK_ORDERS.get()))),
                    "The dropped recycler lost its chips: " + drops);
        });
    }

    private static ItemStack chip(int stage) {
        WorkOrder order = new WorkOrder(WorkOrderFamilies.COMPONENT_RECYCLER, 1, new WorkObjective(RNGTech.id("recycle"),
                new WorkPart(WorkObjectiveType.RECYCLE, WorkFilter.ANY, 0, "", 1)), stage, 0, WorkOrder.BASE_BOUNTY, 0);
        return JobChipItem.of(ModItems.JOB_CHIP.get(), order, MachineTraits.EMPTY);
    }

    private static void shiftClick(ComponentRecyclerMenu menu, Player player, int inventorySlot) {
        for (int index = 0; index < menu.slots.size() - MachineWorkOrders.SLOTS; index++) {
            Slot slot = menu.getSlot(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == inventorySlot) {
                menu.quickMoveStack(player, index);
                return;
            }
        }
    }
}
