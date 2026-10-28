package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.BatteryChassisBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.menu.BatteryChassisMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Chassis affixes act on the installed cells, and a burst chassis spends a window of over-rated output. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatteryChassisBurstGameTests {
    private BatteryChassisBurstGameTests() {
    }

    /** +50% Energy Transfer and +20% Capacity on an Iron chassis (strength 1.3) reach each cell 1.3 times as strongly. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void chassisAffixesScaleCellsByModifierStrength(GameTestHelper helper) {
        BatteryChassisBlockEntity chassis = chassis(helper, BatteryChassisMaterial.IRON, new BlockPos(1, 1, 1));
        chassis.setMachineTraits(new MachineTraits(Rarity.MAGIC, 0, List.of(
                new MachineModifier(ModifierSlot.SUFFIX, MachineStat.ENERGY_TRANSFER, ModifierOperation.INCREASED_PERCENT, 50.0),
                new MachineModifier(ModifierSlot.PREFIX, MachineStat.ENERGY_CAPACITY, ModifierOperation.INCREASED_PERCENT, 20.0)
        )));
        chassis.getInventory().setStackInSlot(0, charged(BatteryCellMaterial.IRON, 0.5));
        BatteryChassisMenu menu = menu(helper, chassis);
        int strength = 13;
        int expectedOutput = (int) Math.round(BatteryCellMaterial.IRON.outputRate() * (1.0 + 0.5 * strength / 10.0));
        int expectedInput = (int) Math.round(BatteryCellMaterial.IRON.inputRate() * (1.0 + 0.5 * strength / 10.0));
        int expectedCapacity = (int) Math.round(BatteryCellMaterial.IRON.capacity() * (1.0 + 0.2 * strength / 10.0));
        check(menu.cellOutputSum() == expectedOutput, "cell output was " + menu.cellOutputSum() + ", expected " + expectedOutput);
        check(menu.cellInputSum() == expectedInput, "cell input was " + menu.cellInputSum() + ", expected " + expectedInput);
        int capacity = chassis.getEnergyStorage(null).getMaxEnergyStored();
        check(capacity == expectedCapacity, "capacity was " + capacity + ", expected " + expectedCapacity);
        helper.succeed();
    }

    /**
     * A Gold chassis (2.5x burst, 0.80 stability, 0.92 efficiency) on one Iron cell delivers its rated output plus a burst
     * that loses a fifth of its FE, spends one window tick per bursting tick, and falls back to rated output when empty.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void goldChassisBurstsThenFallsBackToRatedOutput(GameTestHelper helper) {
        BatteryChassisMaterial gold = BatteryChassisMaterial.GOLD;
        BatteryChassisBlockEntity chassis = chassis(helper, gold, new BlockPos(1, 1, 1));
        chassis.getInventory().setStackInSlot(0, charged(BatteryCellMaterial.IRON, 1.0));
        IEnergyStorage storage = chassis.getEnergyStorage(null);
        BatteryChassisMenu menu = menu(helper, chassis);
        int rated = BatteryCellMaterial.IRON.outputRate();
        int ratedDelivery = (int) Math.floor(rated * gold.efficiency());
        int burstDraw = (int) Math.round(rated * gold.burstTransfer());
        int[] firstWindow = new int[1];
        helper.startSequence()
                .thenExecute(() -> {
                    firstWindow[0] = menu.burstWindow();
                    check(firstWindow[0] == gold.burstDuration(), "burst window started at " + firstWindow[0]);
                    int before = storage.getEnergyStored();
                    int delivered = storage.extractEnergy(Integer.MAX_VALUE, false);
                    int drawn = before - storage.getEnergyStored();
                    check(delivered > ratedDelivery, "burst delivered " + delivered + ", no more than rated " + ratedDelivery);
                    check(drawn <= burstDraw, "burst drew " + drawn + " from the cell, above " + burstDraw);
                    check(delivered < Math.floor(drawn * gold.efficiency()), "burst FE " + delivered + " of " + drawn + " lost nothing to stability");
                })
                .thenExecuteAfter(1, () -> check(menu.burstWindow() == firstWindow[0] - 1,
                        "a bursting tick left the window at " + menu.burstWindow()))
                .thenExecuteFor(gold.burstDuration() + 5, () -> storage.extractEnergy(Integer.MAX_VALUE, false))
                .thenExecute(() -> {
                    check(menu.burstWindow() == 0, "the window was " + menu.burstWindow() + " after bursting every tick");
                    int delivered = storage.extractEnergy(Integer.MAX_VALUE, false);
                    check(delivered <= ratedDelivery, "an empty window still delivered " + delivered);
                })
                .thenExecuteAfter(2, () -> check(menu.burstWindow() > 0, "the window did not refill without a burst"))
                .thenSucceed();
    }

    private static BatteryChassisBlockEntity chassis(GameTestHelper helper, BatteryChassisMaterial material, BlockPos pos) {
        return MachineTestSupport.place(helper, pos, ModBlocks.batteryChassis(material).get(), BatteryChassisBlockEntity.class);
    }

    private static BatteryChassisMenu menu(GameTestHelper helper, BatteryChassisBlockEntity chassis) {
        Inventory inventory = helper.makeMockPlayer(GameType.SURVIVAL).getInventory();
        return new BatteryChassisMenu(0, inventory, chassis, chassis.getMenuData());
    }

    private static ItemStack charged(BatteryCellMaterial material, double fill) {
        ItemStack cell = new ItemStack(ModItems.batteryCell(material).get());
        int capacity = BatteryCellItem.energyCapacity(cell);
        BatteryCellItem.setEnergy(cell, (int) (capacity * fill), capacity);
        return cell;
    }
}
