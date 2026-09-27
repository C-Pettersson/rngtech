package com.rngtech.content.menu;

import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.ItemConnectorMode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

public interface UniversalConnectorAccess {
    ItemStackHandler getInventory();

    ItemStackHandler getFilterInventory();

    ContainerData menuData();

    Level getConnectorLevel();

    BlockPos getConnectorPos();

    Direction defaultAttachAs();

    boolean isMenuAvailable(Player player);

    boolean incrementChannel(int amount);

    boolean setChannel(int channel);

    boolean incrementBridgeChannel(int amount);

    boolean setBridgeChannel(int channel);

    boolean setAttachAs(Direction attachAs);

    boolean cycleMode();

    boolean setMode(CableConnectorMode mode);

    boolean cycleEnergyDistributionMode();

    boolean setEnergyDistributionMode(EnergyDistributionMode distributionMode);

    boolean clearNetworkCache();

    boolean incrementFluidChannel(int moduleIndex, int amount);

    boolean setFluidChannel(int moduleIndex, int channel);

    boolean cycleFluidAttachAs(int moduleIndex);

    boolean setFluidAttachAs(int moduleIndex, Direction attachAs);

    boolean cycleFluidMode(int moduleIndex);

    boolean setFluidMode(int moduleIndex, FluidConnectorMode mode);

    boolean setFluidFilter(int moduleIndex, int filterIndex, ItemStack stack);

    boolean incrementItemChannel(int moduleIndex, int amount);

    boolean setItemChannel(int moduleIndex, int channel);

    boolean cycleItemAttachAs(int moduleIndex);

    boolean setItemAttachAs(int moduleIndex, Direction attachAs);

    boolean cycleItemMode(int moduleIndex);

    boolean setItemMode(int moduleIndex, ItemConnectorMode mode);

    boolean setItemFilter(int moduleIndex, int filterIndex, ItemStack stack);
}
