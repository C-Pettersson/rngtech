package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.ForestryCartMenu;
import com.rngtech.content.menu.WorkOrderMenuSupport;
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
import com.rngtech.rpg.workorder.WorkPart;
import com.rngtech.rpg.workorder.WorkReport;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Work Orders on the Forestry Companion: chips on the cart, its Orders tab, log and crop jobs, conditions, and saves. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForestryWorkOrderGameTests {
    private ForestryWorkOrderGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void cartRunsJobChips(GameTestHelper helper) {
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 1.1, 2.5));
        ForestryCartEntity cart = new ForestryCartEntity(helper.getLevel(), at.x, at.y, at.z);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MachineTraits starved = new MachineTraits(Rarity.MAGIC, 0,
                List.of(WorkOrderConditions.get(WorkOrderConditions.STARVED_FEED).definition().roll(3, RandomSource.create(5L))), List.of());
        WorkOrder logs = new WorkOrder("forestry", 2, new WorkObjective(RNGTech.id("logs"), List.of(
                new WorkPart(WorkObjectiveType.HARVEST, WorkFilter.GROUP, 0, "logs", 20),
                new WorkPart(WorkObjectiveType.HARVEST, WorkFilter.GROUP, 0, "crops", 4)), 0), 4, 0, WorkOrder.BASE_BOUNTY, 0);
        WorkOrder crusher = new WorkOrder("crusher", 1, new WorkObjective(RNGTech.id("crush"),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.ANY, 0, "", 10)), 4, 0, WorkOrder.BASE_BOUNTY, 0);
        player.getInventory().setItem(0, JobChipItem.of(ModItems.JOB_CHIP.get(), crusher, MachineTraits.EMPTY));
        player.getInventory().setItem(1, JobChipItem.of(ModItems.JOB_CHIP.get(), logs, starved));
        double speed = cart.effectiveStats().value(MachineStat.PROCESSING_SPEED);

        ForestryCartMenu menu = (ForestryCartMenu) cart.createMenu(1, player.getInventory(), player);
        check(menu.clickMenuButton(player, WorkOrderMenuSupport.BUTTON_SELECT_TAB + ForestryCartMenu.TAB_ORDERS)
                && menu.selectedTab() == ForestryCartMenu.TAB_ORDERS, "The cart menu did not open the Orders tab");
        shiftClick(menu, player, 0);
        check(cart.workOrders().isEmpty(), "The cart loaded a Crusher chip");
        shiftClick(menu, player, 1);
        check(cart.workOrders().orders().size() == 1 && cart.workOrders().active() == MachineWorkOrders.NONE
                && WorkOrderOperations.perform(player, cart, "activate", "0") && cart.workOrders().active() == 0, "The cart did not load its chip: " + cart.workOrders());
        check(cart.effectiveStats().value(MachineStat.PROCESSING_SPEED) < speed * 0.76, "Starved Feed did not slow the cart");

        cart.workOrderWork(new WorkReport(WorkObjectiveType.HARVEST, 2, 0, "", 12, "logs"));
        cart.workOrderWork(new WorkReport(WorkObjectiveType.HARVEST, 0, 0, "minecraft:wheat", 1, "crops"));
        check(cart.workOrders().activeOrder().progress().equals(List.of(12L, 1L)), "Logs and crops did not count: " + cart.workOrders());

        CompoundTag saved = cart.saveWithoutId(new CompoundTag());
        ForestryCartEntity loaded = new ForestryCartEntity(helper.getLevel(), at.x, at.y, at.z);
        loaded.load(saved);
        check(loaded.workOrders().equals(cart.workOrders()), "The cart lost its chips through a save: " + loaded.workOrders());
        helper.succeed();
    }

    private static void shiftClick(ForestryCartMenu menu, Player player, int inventorySlot) {
        for (int index = 0; index < menu.slots.size() - MachineWorkOrders.SLOTS; index++) {
            Slot slot = menu.getSlot(index);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == inventorySlot) {
                menu.quickMoveStack(player, index);
                return;
            }
        }
    }
}
