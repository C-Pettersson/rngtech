package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.menu.MelterMenu;
import com.rngtech.content.menu.WorkOrderMenuSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderOperations;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Work Orders on the Melter: a chip loaded through its Orders tab, and real Lubricant counting in mB. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MelterWorkOrderGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    private MelterWorkOrderGameTests() {
    }

    /** Two melts of Lubricant, 2000 mB, finish a Lubricant job in its bay. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void meltsCountTheirFluid(GameTestHelper helper) {
        MelterBlockEntity melter = MachineTestSupport.place(helper, MACHINE, ModBlocks.MELTER.get(), MelterBlockEntity.class);
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.TITANIUM).get()));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        WorkOrder lubricant = new WorkOrder("melter", 1, new WorkObjective(RNGTech.id("lubricant"),
                new WorkPart(WorkObjectiveType.MELT, WorkFilter.ITEM, 0, "rngtech:lubricant", 2000)), 4, 0, WorkOrder.BASE_BOUNTY, 0);
        player.getInventory().setItem(0, JobChipItem.of(ModItems.JOB_CHIP.get(), lubricant, MachineTraits.EMPTY));
        MelterMenu menu = (MelterMenu) melter.createMenu(1, player.getInventory(), player);
        check(menu.clickMenuButton(player, WorkOrderMenuSupport.BUTTON_SELECT_TAB + MelterMenu.TAB_ORDERS)
                && menu.selectedTab() == MelterMenu.TAB_ORDERS, "The Melter menu did not open the Orders tab");
        for (int index = 0; index < menu.slots.size() - MachineWorkOrders.SLOTS; index++) {
            Slot slot = menu.getSlot(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == 0) {
                menu.quickMoveStack(player, index);
            }
        }
        check(melter.workOrders().orders().size() == 1 && melter.workOrders().active() == MachineWorkOrders.NONE
                && WorkOrderOperations.perform(player, melter, "activate", "0") && melter.workOrders().active() == 0, "The Melter did not load its chip: " + melter.workOrders());
        check(JobChipItem.objectiveText(lubricant.objective()).getString().contains("2 B of"), "The job text lost its amount in SI form: "
                + JobChipItem.objectiveText(lubricant.objective()).getString());
        check(JobChipItem.amountText(WorkObjectiveType.MELT, 250).equals("250 mB") && JobChipItem.amountText(WorkObjectiveType.MELT, 32_000).equals("32 B")
                && JobChipItem.amountText(WorkObjectiveType.MELT, 22_500).equals("22.5 B") && JobChipItem.amountText(WorkObjectiveType.CRUSH, 32_000).equals("32000"),
                "Fluid amounts are not in SI form");

        melter.getInputTank().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        IItemHandler top = melter.getItemHandler(Direction.UP);
        insert(top, new ItemStack(Items.HONEY_BOTTLE, 2));
        insert(top, new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("plant_reagent")), 4));
        MachineTestSupport.powerEachTick(helper, melter.getEnergyStorage(null));
        helper.succeedWhen(() -> {
            LoadedWorkOrder job = melter.workOrders().orders().isEmpty() ? null : melter.workOrders().orders().getFirst();
            check(job != null && job.fulfilled(), "Waiting for 2000 mB of Lubricant: " + melter.workOrders() + "; " + MachineTestSupport.describe(melter));
        });
    }

    private static void insert(IItemHandler handler, ItemStack stack) {
        ItemStack left = stack;
        for (int slot = 0; slot < handler.getSlots() && !left.isEmpty(); slot++) {
            left = handler.insertItem(slot, left, false);
        }
    }
}
