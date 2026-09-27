package com.rngtech.content.blockentity;

import com.rngtech.RNGTech;
import com.rngtech.content.menu.DebugTankMenu;
import com.rngtech.content.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DebugTankBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_FLUID_INPUT_CONTAINER = 0;
    public static final int SLOT_FLUID_OUTPUT_CONTAINER = 1;
    public static final int SLOT_COUNT = 2;

    private static final int DATA_HAS_FLUID = 0;
    private static final int DATA_COUNT = 1;

    private final ItemStackHandler containerInventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_FLUID_INPUT_CONTAINER -> isFluidInputContainer(stack);
                case SLOT_FLUID_OUTPUT_CONTAINER -> isFluidOutputContainer(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IFluidHandler fluidHandler = new InfiniteFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == DATA_HAS_FLUID && !templateFluid.isEmpty() ? 1 : 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };
    private FluidStack templateFluid = FluidStack.EMPTY;

    public DebugTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DEBUG_TANK.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DebugTankBlockEntity tank) {
        if (!RNGTech.isDebugContentEnabled()) {
            return;
        }
        tank.drainInputContainer();
        tank.fillOutputContainer();
    }

    public ItemStackHandler getContainerInventory() {
        return containerInventory;
    }

    public IItemHandler getItemHandler(Direction side) {
        return containerInventory;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return fluidHandler;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public boolean hasTemplateFluid() {
        return !templateFluid.isEmpty();
    }

    public void clearTemplateFluid() {
        if (!templateFluid.isEmpty()) {
            templateFluid = FluidStack.EMPTY;
            setChangedAndUpdate();
        }
    }

    public static boolean isFluidInputContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).isPresent();
    }

    public static boolean isFluidOutputContainer(ItemStack stack) {
        return !stack.isEmpty() && FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.debug_tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DebugTankMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < containerInventory.getSlots(); slot++) {
            ItemStack stack = containerInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                containerInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", containerInventory.serializeNBT(registries));
        tag.put("TemplateFluid", templateFluid.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        containerInventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        templateFluid = FluidStack.parseOptional(registries, tag.getCompound("TemplateFluid"));
        normalizeTemplateFluid();
    }

    private void drainInputContainer() {
        ItemStack stack = containerInventory.getStackInSlot(SLOT_FLUID_INPUT_CONTAINER);
        if (stack.isEmpty()) {
            return;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(stack, fluidHandler, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            containerInventory.setStackInSlot(SLOT_FLUID_INPUT_CONTAINER, result.getResult());
            setChangedAndUpdate();
        }
    }

    private void fillOutputContainer() {
        if (templateFluid.isEmpty()) {
            return;
        }
        ItemStack stack = containerInventory.getStackInSlot(SLOT_FLUID_OUTPUT_CONTAINER);
        if (stack.isEmpty()) {
            return;
        }
        FluidActionResult result = FluidUtil.tryFillContainer(stack, fluidHandler, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            containerInventory.setStackInSlot(SLOT_FLUID_OUTPUT_CONTAINER, result.getResult());
            setChangedAndUpdate();
        }
    }

    private void setTemplateFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        FluidStack next = stack.copyWithAmount(FluidType.BUCKET_VOLUME);
        if (templateFluid.isEmpty() || !FluidStack.isSameFluidSameComponents(templateFluid, next)) {
            templateFluid = next;
            setChangedAndUpdate();
        }
    }

    private void normalizeTemplateFluid() {
        if (templateFluid.isEmpty() || templateFluid.getAmount() <= 0) {
            templateFluid = FluidStack.EMPTY;
        } else {
            templateFluid = templateFluid.copyWithAmount(FluidType.BUCKET_VOLUME);
        }
    }

    private void setChangedAndUpdate() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private final class InfiniteFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0 || templateFluid.isEmpty()) {
                return FluidStack.EMPTY;
            }
            return templateFluid.copyWithAmount(Integer.MAX_VALUE);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? Integer.MAX_VALUE : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && !stack.isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            if (action.execute()) {
                setTemplateFluid(resource);
            }
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()
                    || templateFluid.isEmpty()
                    || !FluidStack.isSameFluidSameComponents(resource, templateFluid)) {
                return FluidStack.EMPTY;
            }
            return templateFluid.copyWithAmount(resource.getAmount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || templateFluid.isEmpty()) {
                return FluidStack.EMPTY;
            }
            return templateFluid.copyWithAmount(maxDrain);
        }
    }
}
