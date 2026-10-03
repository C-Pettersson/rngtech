package com.rngtech.client.screen;

import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.gear.GearSlotArea;
import com.rngtech.content.gear.GearSlotCatalog;
import com.rngtech.content.gear.GearSlotSpec;
import com.rngtech.content.menu.AlgaePhotobioreactorMenu;
import com.rngtech.content.menu.AlloyFurnaceMenu;
import com.rngtech.content.menu.AmmoniaFuelCellMenu;
import com.rngtech.content.menu.AmmoniaSynthesizerMenu;
import com.rngtech.content.menu.BatteryChassisMenu;
import com.rngtech.content.menu.BioGeneratorMenu;
import com.rngtech.content.menu.CavitationGeneratorMenu;
import com.rngtech.content.menu.ComponentRecyclerMenu;
import com.rngtech.content.menu.CompressorTankMenu;
import com.rngtech.content.menu.CorrosionCellMenu;
import com.rngtech.content.menu.CrusherMenu;
import com.rngtech.content.menu.ForestryCartMenu;
import com.rngtech.content.menu.ForestryCartStationMenu;
import com.rngtech.content.menu.FurnaceMenu;
import com.rngtech.content.menu.GasChemistryMenu;
import com.rngtech.content.menu.MelterMenu;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.menu.MinersCompanionMenu;
import com.rngtech.content.menu.PotentialReactorMenu;
import com.rngtech.content.menu.ResonanceCalibratorMenu;
import com.rngtech.content.menu.SolarArrayControllerMenu;
import com.rngtech.content.menu.SolidFuelBurnerMenu;
import com.rngtech.content.menu.ToolBenchMenu;
import com.rngtech.content.menu.VacuumCollapseGeneratorMenu;
import com.rngtech.content.registry.ModItems;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;
import java.util.Optional;

final class GearSlotTooltips {
    static void render(
            AbstractContainerScreen<? extends AbstractContainerMenu> screen,
            GuiGraphics guiGraphics,
            Font font,
            int mouseX,
            int mouseY
    ) {
        AbstractContainerMenu menu = screen.getMenu();
        if (!isGearTab(menu)) {
            return;
        }

        hoveredEmptyItemHandlerSlot(screen, mouseX, mouseY)
                .flatMap(slot -> gearSlot(menu, slot))
                .ifPresent(spec -> guiGraphics.renderTooltip(
                        font,
                        tooltipLines(spec),
                        mouseX,
                        mouseY
                ));
    }

    private static List<FormattedCharSequence> tooltipLines(GearSlotSpec spec) {
        return spec.tooltipLines(Screen.hasShiftDown()).stream().map(component -> component.getVisualOrderText()).toList();
    }

    private static Optional<Slot> hoveredEmptyItemHandlerSlot(
            AbstractContainerScreen<? extends AbstractContainerMenu> screen,
            int mouseX,
            int mouseY
    ) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot instanceof SlotItemHandler) || !slot.isActive() || slot.hasItem()) {
                continue;
            }
            int x = left + slot.x;
            int y = top + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    private static Optional<GearSlotSpec> gearSlot(AbstractContainerMenu menu, Slot slot) {
        if (menu instanceof ForestryCartMenu cart && !cart.isGearSlot(slot)) {
            return Optional.empty();
        }
        ItemStack machineStack = machineStack(menu);
        if (machineStack.isEmpty()) {
            return Optional.empty();
        }
        return GearSlotCatalog.slotFor(machineStack, slotArea(menu, slot), slot.getSlotIndex());
    }

    private static GearSlotArea slotArea(AbstractContainerMenu menu, Slot slot) {
        if (menu instanceof FurnaceMenu furnace
                && furnace.isElectric()
                && slot.getSlotIndex() == FurnaceBlockEntity.SLOT_FUEL) {
            return GearSlotArea.MACHINE;
        }
        if (menu instanceof CrusherMenu || menu instanceof ToolBenchMenu) {
            return GearSlotArea.MACHINE;
        }
        if (menu instanceof BatteryChassisMenu) {
            return GearSlotArea.CELL;
        }
        return GearSlotArea.GEAR;
    }

    private static ItemStack machineStack(AbstractContainerMenu menu) {
        if (menu instanceof CrusherMenu crusher) {
            return crusher.getSlot(crusher.refinementTargetSlot()).getItem();
        }
        if (menu instanceof FurnaceMenu furnace) {
            return furnace.getSlot(furnace.refinementTargetSlot()).getItem();
        }
        if (menu instanceof AlloyFurnaceMenu alloyFurnace) {
            return alloyFurnace.getSlot(alloyFurnace.refinementTargetSlot()).getItem();
        }
        if (menu instanceof BatteryChassisMenu batteryChassis) {
            return batteryChassis.getSlot(batteryChassis.refinementTargetSlot()).getItem();
        }
        if (menu instanceof SolidFuelBurnerMenu solidFuelBurner) {
            return solidFuelBurner.getSlot(solidFuelBurner.refinementTargetSlot()).getItem();
        }
        if (menu instanceof BioGeneratorMenu bioGenerator) {
            return bioGenerator.getSlot(bioGenerator.refinementTargetSlot()).getItem();
        }
        if (menu instanceof AlgaePhotobioreactorMenu) {
            return new ItemStack(ModItems.ALGAE_PHOTOBIOREACTOR.get());
        }
        if (menu instanceof SolarArrayControllerMenu solarArray) {
            return solarArray.getSlot(solarArray.refinementTargetSlot()).getItem();
        }
        if (menu instanceof PotentialReactorMenu potentialReactor) {
            return potentialReactor.getSlot(potentialReactor.refinementTargetSlot()).getItem();
        }
        if (menu instanceof CorrosionCellMenu corrosionCell) {
            return corrosionCell.getSlot(corrosionCell.refinementTargetSlot()).getItem();
        }
        if (menu instanceof CavitationGeneratorMenu cavitation) {
            return cavitation.getSlot(cavitation.refinementTargetSlot()).getItem();
        }
        if (menu instanceof AmmoniaSynthesizerMenu ammoniaSynthesizer) {
            return ammoniaSynthesizer.getSlot(ammoniaSynthesizer.refinementTargetSlot()).getItem();
        }
        if (menu instanceof AmmoniaFuelCellMenu ammoniaFuelCell) {
            return ammoniaFuelCell.getSlot(ammoniaFuelCell.refinementTargetSlot()).getItem();
        }
        if (menu instanceof GasChemistryMenu gasChemistry) {
            return gasChemistry.getSlot(gasChemistry.refinementTargetSlot()).getItem();
        }
        if (menu instanceof VacuumCollapseGeneratorMenu vacuumCollapse) {
            return vacuumCollapse.getSlot(vacuumCollapse.refinementTargetSlot()).getItem();
        }
        if (menu instanceof ComponentRecyclerMenu recycler) {
            return recycler.getSlot(recycler.refinementTargetSlot()).getItem();
        }
        if (menu instanceof MetalPressMenu metalPress) {
            return metalPress.getSlot(metalPress.refinementTargetSlot()).getItem();
        }
        if (menu instanceof ResonanceCalibratorMenu calibrator) {
            return calibrator.getSlot(calibrator.refinementTargetSlot()).getItem();
        }
        if (menu instanceof MelterMenu melter) {
            return melter.getSlot(melter.refinementTargetSlot()).getItem();
        }
        if (menu instanceof CompressorTankMenu compressorTank) {
            return compressorTank.getSlot(compressorTank.refinementTargetSlot()).getItem();
        }
        if (menu instanceof ToolBenchMenu) {
            return new ItemStack(ModItems.TOOL_BENCH.get());
        }
        if (menu instanceof MinersCompanionMenu minersCompanion) {
            return minersCompanion.minersCompanionStack();
        }
        if (menu instanceof ForestryCartMenu) {
            return new ItemStack(ModItems.FORESTRY_CART.get());
        }
        if (menu instanceof ForestryCartStationMenu) {
            return new ItemStack(ModItems.FORESTRY_CART_STATION.get());
        }
        return ItemStack.EMPTY;
    }

    private static boolean isGearTab(AbstractContainerMenu menu) {
        if (menu instanceof CrusherMenu crusher) {
            return crusher.selectedTab() == CrusherMenu.TAB_GEAR;
        }
        if (menu instanceof FurnaceMenu furnace) {
            return furnace.selectedTab() == FurnaceMenu.TAB_GEAR;
        }
        if (menu instanceof AlloyFurnaceMenu alloyFurnace) {
            return alloyFurnace.selectedTab() == AlloyFurnaceMenu.TAB_GEAR;
        }
        if (menu instanceof BatteryChassisMenu batteryChassis) {
            return batteryChassis.selectedTab() == BatteryChassisMenu.TAB_GEAR;
        }
        if (menu instanceof SolidFuelBurnerMenu solidFuelBurner) {
            return solidFuelBurner.selectedTab() == SolidFuelBurnerMenu.TAB_GEAR;
        }
        if (menu instanceof BioGeneratorMenu bioGenerator) {
            return bioGenerator.selectedTab() == BioGeneratorMenu.TAB_GEAR;
        }
        if (menu instanceof AlgaePhotobioreactorMenu algae) {
            return algae.selectedTab() == AlgaePhotobioreactorMenu.TAB_GEAR;
        }
        if (menu instanceof SolarArrayControllerMenu solarArray) {
            return solarArray.selectedTab() == SolarArrayControllerMenu.TAB_GEAR;
        }
        if (menu instanceof PotentialReactorMenu potentialReactor) {
            return potentialReactor.selectedTab() == PotentialReactorMenu.TAB_GEAR;
        }
        if (menu instanceof CorrosionCellMenu corrosionCell) {
            return corrosionCell.selectedTab() == CorrosionCellMenu.TAB_GEAR;
        }
        if (menu instanceof CavitationGeneratorMenu cavitation) {
            return cavitation.selectedTab() == CavitationGeneratorMenu.TAB_GEAR;
        }
        if (menu instanceof AmmoniaSynthesizerMenu ammoniaSynthesizer) {
            return ammoniaSynthesizer.selectedTab() == AmmoniaSynthesizerMenu.TAB_GEAR;
        }
        if (menu instanceof AmmoniaFuelCellMenu ammoniaFuelCell) {
            return ammoniaFuelCell.selectedTab() == AmmoniaFuelCellMenu.TAB_GEAR;
        }
        if (menu instanceof GasChemistryMenu gasChemistry) {
            return gasChemistry.selectedTab() == GasChemistryMenu.TAB_GEAR;
        }
        if (menu instanceof VacuumCollapseGeneratorMenu vacuumCollapse) {
            return vacuumCollapse.selectedTab() == VacuumCollapseGeneratorMenu.TAB_GEAR;
        }
        if (menu instanceof ComponentRecyclerMenu recycler) {
            return recycler.selectedTab() == ComponentRecyclerMenu.TAB_GEAR;
        }
        if (menu instanceof MetalPressMenu metalPress) {
            return metalPress.selectedTab() == MetalPressMenu.TAB_GEAR;
        }
        if (menu instanceof ResonanceCalibratorMenu calibrator) {
            return calibrator.selectedTab() == ResonanceCalibratorMenu.TAB_GEAR;
        }
        if (menu instanceof MelterMenu melter) {
            return melter.selectedTab() == MelterMenu.TAB_GEAR;
        }
        if (menu instanceof CompressorTankMenu compressorTank) {
            return compressorTank.selectedTab() == CompressorTankMenu.TAB_GEAR;
        }
        if (menu instanceof ToolBenchMenu toolBench) {
            return toolBench.selectedTab() == ToolBenchMenu.TAB_GEAR;
        }
        if (menu instanceof MinersCompanionMenu minersCompanion) {
            return minersCompanion.selectedTab() == MinersCompanionMenu.TAB_GEAR;
        }
        if (menu instanceof ForestryCartMenu cart) {
            return cart.selectedTab() == ForestryCartMenu.TAB_CART;
        }
        if (menu instanceof ForestryCartStationMenu station) {
            return station.selectedTab() == ForestryCartStationMenu.TAB_GEAR;
        }
        return false;
    }

    private GearSlotTooltips() {
    }
}
