package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.menu.AlloyFurnaceMenu;
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

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Work Orders on the Alloy Furnace: loading through its Orders tab, and real alloy crafts counting toward a chip. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlloyFurnaceWorkOrderGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private AlloyFurnaceWorkOrderGameTests() {
    }

    /** A Bronze craft counts its ingots toward a Bronze job, and the job finishes in its bay. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void alloyCraftsCountTowardTheirJob(GameTestHelper helper) {
        AlloyFurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.TITANIUM).get(), AlloyFurnaceBlockEntity.class);
        ItemStackHandler gear = furnace.getGearInventory();
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_CRUCIBLE, new ItemStack(ModItems.alloyCrucible(AlloyCrucibleMaterial.TITANIUM).get()));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        WorkOrder bronze = new WorkOrder("alloy_furnace", 1, new WorkObjective(RNGTech.id("bronze"),
                new WorkPart(WorkObjectiveType.ALLOY, WorkFilter.ITEM, 0, "rngtech:bronze_ingot", 4)), 4, 0, WorkOrder.BASE_BOUNTY, 0);
        WorkOrder crusher = new WorkOrder("crusher", 1, new WorkObjective(RNGTech.id("crush"),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.ANY, 0, "", 10)), 4, 0, WorkOrder.BASE_BOUNTY, 0);
        player.getInventory().setItem(0, JobChipItem.of(ModItems.JOB_CHIP.get(), crusher, MachineTraits.EMPTY));
        player.getInventory().setItem(1, JobChipItem.of(ModItems.JOB_CHIP.get(), bronze, MachineTraits.EMPTY));
        AlloyFurnaceMenu menu = (AlloyFurnaceMenu) furnace.createMenu(1, player.getInventory(), player);
        check(menu.clickMenuButton(player, WorkOrderMenuSupport.BUTTON_SELECT_TAB + AlloyFurnaceMenu.TAB_ORDERS)
                && menu.selectedTab() == AlloyFurnaceMenu.TAB_ORDERS, "The Alloy Furnace menu did not open the Orders tab");
        shiftClick(menu, player, 0);
        check(furnace.workOrders().isEmpty(), "The Alloy Furnace loaded a Crusher chip");
        shiftClick(menu, player, 1);
        check(furnace.workOrders().orders().size() == 1 && furnace.workOrders().active() == 0, "The Alloy Furnace did not load its chip: " + furnace.workOrders());
        check(menu.workOrderView().slots().size() == 1, "The Alloy Furnace did not sync its chip to the Orders tab");

        ItemStackHandler process = furnace.getProcessInventory();
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.COPPER_INGOT, 3));
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START + 1, new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id("tin_ingot")), 1));
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START + 2, new ItemStack(Items.CHARCOAL, 1));
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));
        helper.succeedWhen(() -> {
            LoadedWorkOrder job = furnace.workOrders().orders().isEmpty() ? null : furnace.workOrders().orders().getFirst();
            check(job != null && job.fulfilled() && furnace.workOrders().active() == MachineWorkOrders.NONE,
                    "Waiting for the Bronze craft to finish the job: " + furnace.workOrders() + "; " + MachineTestSupport.describe(furnace));
        });
    }

    private static void shiftClick(AlloyFurnaceMenu menu, Player player, int inventorySlot) {
        for (int index = 0; index < menu.slots.size() - MachineWorkOrders.SLOTS; index++) {
            Slot slot = menu.getSlot(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == inventorySlot) {
                menu.quickMoveStack(player, index);
                return;
            }
        }
    }
}
