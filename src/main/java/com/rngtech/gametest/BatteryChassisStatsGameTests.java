package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.BatteryChassisBlockEntity;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.menu.BatteryChassisMenu;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatDisplay;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Battery Chassis Stats tab shows each tier's base stats in their own units, as the menu reads them. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatteryChassisStatsGameTests {
    private static final double TOLERANCE = 0.005;

    private BatteryChassisStatsGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void statsTabShowsEachTiersBaseStats(GameTestHelper helper) {
        Inventory inventory = helper.makeMockPlayer(GameType.SURVIVAL).getInventory();
        BatteryChassisMaterial[] materials = BatteryChassisMaterial.values();
        for (int index = 0; index < materials.length; index++) {
            BatteryChassisMaterial material = materials[index];
            BatteryChassisBlockEntity chassis = MachineTestSupport.place(
                    helper,
                    new BlockPos(index % 5, 1 + index / 5, 2),
                    ModBlocks.batteryChassis(material).get(),
                    BatteryChassisBlockEntity.class
            );
            BatteryChassisMenu menu = new BatteryChassisMenu(0, inventory, chassis, chassis.getMenuData());
            String name = material.getSerializedName();
            expect(menu, BatteryChassisMenu.cellSlotsDataIndex(), material.slots(), name + " cell slots");
            expect(menu, BatteryChassisMenu.energyTransferDataIndex(), material.transferRate(), name + " energy transfer");
            expect(menu, BatteryChassisMenu.burstTransferDataIndex(), material.burstTransfer(), name + " burst transfer");
            expect(menu, BatteryChassisMenu.burstDurationDataIndex(), material.burstDuration(), name + " burst duration");
            expect(menu, BatteryChassisMenu.efficiencyDataIndex(), material.efficiency(), name + " efficiency");
            expect(menu, BatteryChassisMenu.stabilityDataIndex(), material.stability(), name + " stability");
            expect(menu, BatteryChassisMenu.idleLossDataIndex(), material.idleLossPercentPerMinute(), name + " idle loss");
            expect(menu, BatteryChassisMenu.globalModifierStrengthDataIndex(), material.globalModifierStrength(),
                    name + " modifier strength");
            check(menu.chassisCeiling() == material.transferRate(),
                    name + " Process tab transfer rating was " + menu.chassisCeiling() + ", expected " + material.transferRate());
        }
        expectText(MachineStat.ENERGY_TRANSFER, BatteryChassisMaterial.IRON.transferRate(), "128 FE/t");
        expectText(MachineStat.ENERGY_TRANSFER, BatteryChassisMaterial.EXOTIC.transferRate(), "4,096 FE/t");
        expectText(MachineStat.BURST_TRANSFER, BatteryChassisMaterial.IRON.burstTransfer(), "0%");
        expectText(MachineStat.BURST_TRANSFER, BatteryChassisMaterial.GOLD.burstTransfer(), "+150%");
        expectText(MachineStat.BURST_TRANSFER, BatteryChassisMaterial.SPARKSTEEL.burstTransfer(), "+40%");
        helper.succeed();
    }

    private static void expect(BatteryChassisMenu menu, int dataIndex, double expected, String what) {
        double shown = menu.statValue(dataIndex);
        check(Math.abs(shown - expected) <= TOLERANCE, what + " shown as " + shown + ", expected " + expected);
    }

    private static void expectText(MachineStat stat, double value, String expected) {
        String shown = MachineStatDisplay.statValue(stat, value);
        check(shown.equals(expected), stat + " " + value + " shown as '" + shown + "', expected '" + expected + "'");
    }
}
