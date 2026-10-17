package com.rngtech.content.blockentity;

import com.rngtech.content.block.DormantMachineBlock;
import com.rngtech.content.dormant.DormantCore;
import com.rngtech.content.dormant.DormantPhase;
import com.rngtech.content.menu.DormantMachineMenu;
import com.rngtech.content.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

/** The block form of a Dormant Machine. Its phase lives in the block state; everything else lives in its {@link DormantCore}. */
public class DormantMachineBlockEntity extends BlockEntity implements MenuProvider, MachineInfoProvider, DormantCore.Host {
    private final DormantCore core = new DormantCore(this);

    public DormantMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DORMANT_MACHINE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DormantMachineBlockEntity machine) {
        machine.core.tick((ServerLevel) level);
    }

    public DormantCore core() {
        return core;
    }

    @Override
    public Level hostLevel() {
        return level;
    }

    @Override
    public BlockPos sitePos() {
        return worldPosition;
    }

    @Override
    public DormantPhase phase() {
        BlockState state = getBlockState();
        return state.hasProperty(DormantMachineBlock.PHASE) ? state.getValue(DormantMachineBlock.PHASE) : DormantPhase.ASLEEP;
    }

    @Override
    public void setPhase(DormantPhase phase) {
        if (level != null && phase() != phase) {
            level.setBlock(worldPosition, getBlockState().setValue(DormantMachineBlock.PHASE, phase), Block.UPDATE_ALL);
        }
    }

    @Override
    public void changed() {
        setChanged();
    }

    @Override
    public boolean entitySite() {
        return false;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return core.energy();
    }

    public IItemHandler getItemHandler(Direction side) {
        return core.itemHandler(side);
    }

    @Override
    public Component getDisplayName() {
        return DormantMachineMenu.siteName(core.site());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DormantMachineMenu(containerId, inventory, core, core.menuData(), viewer -> !isRemoved()
                && level.getBlockEntity(worldPosition) == this && viewer.canInteractWithBlock(worldPosition, 4.0D));
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        return core.machineInfo();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        core.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        core.load(tag, registries);
    }
}
