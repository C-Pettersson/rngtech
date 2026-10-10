package com.rngtech.client.screen;

import com.rngtech.content.item.UniqueTooltip;
import com.rngtech.content.menu.AlloyFurnaceMenu;
import com.rngtech.content.menu.BatteryChassisMenu;
import com.rngtech.content.menu.CavitationGeneratorMenu;
import com.rngtech.content.menu.CompressorTankMenu;
import com.rngtech.content.menu.CorrosionCellMenu;
import com.rngtech.content.menu.CrusherMenu;
import com.rngtech.content.menu.ForestryCartMenu;
import com.rngtech.content.menu.FurnaceMenu;
import com.rngtech.content.menu.GasChemistryMenu;
import com.rngtech.content.menu.MasteryMenuView;
import com.rngtech.content.menu.MelterMenu;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.menu.MinersCompanionMenu;
import com.rngtech.content.menu.ResonanceCalibratorMenu;
import com.rngtech.content.menu.SolidFuelBurnerMenu;
import com.rngtech.rpg.unique.UniqueStatReaders.Reader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** The machine whose screen is open, so Unique tooltips can dim the stats it does not read. */
public final class UniqueHostReaders {
    private UniqueHostReaders() {
    }

    public static UniqueTooltip.OpenHost current() {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            return null;
        }
        Reader reader = reader(screen.getMenu());
        if (reader == null) {
            return null;
        }
        String ascendancy = screen.getMenu() instanceof MasteryMenuView<?> mastery ? mastery.masterySnapshot().ascendancy() : null;
        return new UniqueTooltip.OpenHost(reader, ascendancy);
    }

    private static Reader reader(AbstractContainerMenu menu) {
        if (menu instanceof CrusherMenu) {
            return Reader.CRUSHER;
        }
        if (menu instanceof FurnaceMenu) {
            return Reader.FURNACE;
        }
        if (menu instanceof AlloyFurnaceMenu) {
            return Reader.ALLOY_FURNACE;
        }
        if (menu instanceof MetalPressMenu) {
            return Reader.METAL_PRESS;
        }
        if (menu instanceof MelterMenu) {
            return Reader.MELTER;
        }
        if (menu instanceof SolidFuelBurnerMenu) {
            return Reader.SOLID_FUEL_BURNER;
        }
        if (menu instanceof CavitationGeneratorMenu) {
            return Reader.CAVITATION_GENERATOR;
        }
        if (menu instanceof GasChemistryMenu) {
            return Reader.GAS_CHEMISTRY;
        }
        if (menu instanceof CompressorTankMenu) {
            return Reader.COMPRESSOR_TANK;
        }
        if (menu instanceof CorrosionCellMenu) {
            return Reader.CORROSION_CELL;
        }
        if (menu instanceof ForestryCartMenu) {
            return Reader.FORESTRY_CART;
        }
        if (menu instanceof ResonanceCalibratorMenu) {
            return Reader.RESONANCE_CALIBRATOR;
        }
        if (menu instanceof BatteryChassisMenu) {
            return Reader.BATTERY_CHASSIS;
        }
        if (menu instanceof MinersCompanionMenu) {
            return Reader.MINERS_COMPANION;
        }
        return null;
    }
}
