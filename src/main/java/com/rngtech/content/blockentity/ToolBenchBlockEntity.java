package com.rngtech.content.blockentity;

import com.rngtech.content.block.ToolBenchBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.MinersCompanionItem;
import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.item.ToolHeadItem;
import com.rngtech.content.item.ToolRodItem;
import com.rngtech.content.menu.ToolBenchMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.tool.FieldToolAssembly;
import com.rngtech.content.tool.FieldToolEnchantments;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementResult;
import com.rngtech.rpg.refinement.RefinementTargets;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ToolBenchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_TOOL = 0;
    public static final int SLOT_HEAD = 1;
    public static final int SLOT_ROD = 2;
    public static final int SLOT_BATTERY_CELL = 3;
    public static final int SLOT_TINY_ANVIL = 4;
    public static final int SLOT_REPAIR_MATERIAL = 5;
    public static final int SLOT_REFINEMENT_CONSUMABLE = 6;
    public static final int SLOT_ENERGY_CONNECTOR = 7;
    public static final int SLOT_COUNT = 8;

    private boolean suppressAnvilStateSync;
    private final IEnergyStorage energyStorage = new ToolBenchEnergyStorage();

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_TOOL -> acceptsToolSlot(stack);
                case SLOT_HEAD -> stack.getItem() instanceof ToolHeadItem;
                case SLOT_ROD -> stack.getItem() instanceof ToolRodItem;
                case SLOT_BATTERY_CELL -> BatteryCellItem.isBatteryCell(stack);
                case SLOT_TINY_ANVIL -> stack.is(ModItems.TINY_ANVIL.get());
                case SLOT_REPAIR_MATERIAL -> ModularToolItem.isKnownRepairMaterial(stack) || isDiamondTip(stack);
                case SLOT_REFINEMENT_CONSUMABLE -> isRefinementConsumable(stack);
                case SLOT_ENERGY_CONNECTOR -> stack.getItem() instanceof EnergyConnectorItem;
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_ENERGY_CONNECTOR ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == SLOT_TINY_ANVIL) {
                syncAnvilBlockState();
            } else if (slot == SLOT_ENERGY_CONNECTOR) {
                invalidateEnergyCapability();
            }
        }
    };

    public ToolBenchBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TOOL_BENCH.get(), pos, blockState);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return hasEnergyConnector() ? energyStorage : null;
    }

    public static boolean acceptsToolSlot(ItemStack stack) {
        return ModularToolItem.isModularTool(stack) || MinersCompanionItem.isMinersCompanion(stack);
    }

    public static boolean isRefinementConsumable(ItemStack stack) {
        return stack.getItem() instanceof RefinementConsumableItem item
                && isRefinementOperationSupported(item.operation());
    }

    public String apply(Player player) {
        ItemStack tool = inventory.getStackInSlot(SLOT_TOOL);
        return tool.isEmpty() ? assembleFresh(player) : swapParts(player, tool);
    }

    public boolean applyRefinement(Player player, boolean headTarget) {
        ItemStack target = refinementTarget(headTarget);
        if (!RefinementTargets.canRefine(target)) {
            showFailure(player, "rngtech.tool_bench.failure.invalid_refinement_target");
            return false;
        }

        ItemStack consumable = inventory.getStackInSlot(SLOT_REFINEMENT_CONSUMABLE);
        if (!(consumable.getItem() instanceof RefinementConsumableItem refinementItem)
                || !isRefinementOperationSupported(refinementItem.operation())) {
            showFailure(player, "rngtech.tool_bench.failure.invalid_refinement_consumable");
            return false;
        }

        RefinementResult result = RefinementEngine.apply(
                RefinementTargets.eligibilityProfile(target),
                RefinementTargets.storedTraits(target),
                refinementItem.operation(),
                RefinementTargets.modifierRollComponentStage(target),
                player.level().random
        );
        if (!result.success()) {
            showFailure(player, result.messageKey());
            return false;
        }

        ItemStack refinedTarget = target.copy();
        RefinementTargets.setTraits(refinedTarget, result.traits());
        if (!setRefinementTarget(headTarget, refinedTarget)) {
            showFailure(player, "rngtech.tool_bench.failure.invalid_refinement_target");
            return false;
        }
        if (result.consumeCatalyst()) {
            inventory.extractItem(SLOT_REFINEMENT_CONSUMABLE, 1, false);
        }
        setChanged();
        showSuccess(player, result);
        return true;
    }

    public ItemStack refinementTarget(boolean headTarget) {
        ItemStack tool = inventory.getStackInSlot(SLOT_TOOL);
        if (tool.getItem() instanceof ModularToolItem toolItem) {
            FieldToolAssembly assembly = ModularToolItem.assembly(tool);
            if (assembly.isValidFor(toolItem.family())) {
                return headTarget ? assembly.head() : assembly.rod();
            }
        }
        return inventory.getStackInSlot(headTarget ? SLOT_HEAD : SLOT_ROD);
    }

    public String removeBattery(Player player) {
        ItemStack tool = inventory.getStackInSlot(SLOT_TOOL);
        if (!(tool.getItem() instanceof ModularToolItem toolItem)) {
            return "rngtech.tool_bench.failure.invalid_tool";
        }
        FieldToolAssembly assembly = ModularToolItem.assembly(tool);
        if (!assembly.isValidFor(toolItem.family())) {
            return "rngtech.tool_bench.failure.invalid_combination";
        }
        if (assembly.batteryCell().isEmpty()) {
            return "rngtech.tool_bench.failure.no_battery";
        }
        ItemStack result = tool.copy();
        result.set(ModDataComponents.FIELD_TOOL_ASSEMBLY.get(), assembly.withBatteryCell(ItemStack.EMPTY));
        result.remove(ModDataComponents.MACHINE_TRAITS.get());
        preserveDamageRatio(tool, result);
        inventory.setStackInSlot(SLOT_TOOL, result);
        giveOrDrop(player, assembly.batteryCell());
        setChanged();
        return "rngtech.tool_bench.success.removed_battery";
    }

    public String disassemble(Player player) {
        ItemStack tool = inventory.getStackInSlot(SLOT_TOOL);
        if (!(tool.getItem() instanceof ModularToolItem toolItem)) {
            return "rngtech.tool_bench.failure.invalid_tool";
        }
        FieldToolAssembly assembly = ModularToolItem.assembly(tool);
        if (!assembly.isValidFor(toolItem.family())) {
            return "rngtech.tool_bench.failure.invalid_combination";
        }
        int wear = ModularToolItem.damageWear(tool);
        inventory.setStackInSlot(SLOT_TOOL, ItemStack.EMPTY);
        giveOrDrop(player, componentWithStoredWear(assembly.head(), wear));
        giveOrDrop(player, componentWithStoredWear(assembly.rod(), wear));
        giveOrDrop(player, assembly.batteryCell());
        setChanged();
        return "rngtech.tool_bench.success.disassembled";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.tool_bench");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ToolBenchMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    public void dropInventory(Level level) {
        suppressAnvilStateSync = true;
        try {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                    inventory.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        } finally {
            suppressAnvilStateSync = false;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag inventoryTag = tag.getCompound("Inventory");
        if (inventoryTag.getInt("Size") < SLOT_COUNT) {
            inventoryTag = inventoryTag.copy();
            inventoryTag.putInt("Size", SLOT_COUNT);
        }
        inventory.deserializeNBT(registries, inventoryTag);
        syncAnvilBlockState();
        invalidateEnergyCapability();
    }

    private void syncAnvilBlockState() {
        if (suppressAnvilStateSync || level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (!state.hasProperty(ToolBenchBlock.HAS_TINY_ANVIL)) {
            return;
        }
        boolean hasTinyAnvil = hasTinyAnvil();
        if (state.getValue(ToolBenchBlock.HAS_TINY_ANVIL) != hasTinyAnvil) {
            level.setBlock(worldPosition, state.setValue(ToolBenchBlock.HAS_TINY_ANVIL, hasTinyAnvil), Block.UPDATE_ALL);
        }
    }

    private void invalidateEnergyCapability() {
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    private String assembleFresh(Player player) {
        ItemStack head = inventory.getStackInSlot(SLOT_HEAD);
        ItemStack rod = inventory.getStackInSlot(SLOT_ROD);
        ItemStack batteryCell = inventory.getStackInSlot(SLOT_BATTERY_CELL);
        ItemStack repairInput = inventory.getStackInSlot(SLOT_REPAIR_MATERIAL);
        boolean diamondTip = isDiamondTip(repairInput);
        if (!(head.getItem() instanceof ToolHeadItem headItem) || !(rod.getItem() instanceof ToolRodItem)) {
            if (diamondTip && head.getItem() instanceof ToolHeadItem) {
                return applyDiamondTipToLooseHead();
            }
            return "rngtech.tool_bench.failure.requires_head_and_rod";
        }
        if (requiresTinyAnvil(headItem.family()) && !hasTinyAnvil()) {
            return "rngtech.tool_bench.failure.requires_tiny_anvil";
        }

        ItemStack nextHead = head;
        if (diamondTip) {
            if (!ToolHeadItem.canAcceptDiamondTip(head)) {
                return diamondTipFailure();
            }
            nextHead = head.copy();
            ToolHeadItem.applyDiamondTip(nextHead);
        }

        ItemStack result = newToolStack(headItem.family(), nextHead, rod, batteryCell);
        if (result.isEmpty()) {
            return "rngtech.tool_bench.failure.battery_support";
        }
        if (diamondTip) {
            inventory.extractItem(SLOT_REPAIR_MATERIAL, 1, false);
        }
        inventory.setStackInSlot(SLOT_TOOL, result);
        inventory.setStackInSlot(SLOT_HEAD, ItemStack.EMPTY);
        inventory.setStackInSlot(SLOT_ROD, ItemStack.EMPTY);
        inventory.setStackInSlot(SLOT_BATTERY_CELL, ItemStack.EMPTY);
        setChanged();
        return "rngtech.tool_bench.success.assembled";
    }

    private String swapParts(Player player, ItemStack tool) {
        if (!(tool.getItem() instanceof ModularToolItem toolItem)) {
            return "rngtech.tool_bench.failure.invalid_tool";
        }
        FieldToolAssembly current = ModularToolItem.assembly(tool);
        if (!current.isValidFor(toolItem.family())) {
            return "rngtech.tool_bench.failure.invalid_combination";
        }

        ItemStack headInput = inventory.getStackInSlot(SLOT_HEAD);
        ItemStack rodInput = inventory.getStackInSlot(SLOT_ROD);
        ItemStack cellInput = inventory.getStackInSlot(SLOT_BATTERY_CELL);
        ItemStack repairInput = inventory.getStackInSlot(SLOT_REPAIR_MATERIAL);
        boolean replaceHead = !headInput.isEmpty();
        boolean replaceRod = !rodInput.isEmpty();
        boolean replaceCell = !cellInput.isEmpty();
        boolean diamondTip = isDiamondTip(repairInput);
        boolean repair = !repairInput.isEmpty() && !diamondTip;
        int removedPartWear = ModularToolItem.damageWear(tool);
        if (!replaceHead && !replaceRod && !replaceCell && !repair && !diamondTip) {
            return "rngtech.tool_bench.failure.no_changes";
        }

        ItemStack nextHead = replaceHead ? headInput.copy() : current.head().copy();
        ItemStack nextRod = replaceRod ? rodInput : current.rod();
        ItemStack nextCell = replaceCell ? cellInput : current.batteryCell();
        if (!(nextHead.getItem() instanceof ToolHeadItem nextHeadItem) || !(nextRod.getItem() instanceof ToolRodItem)) {
            return "rngtech.tool_bench.failure.requires_head_and_rod";
        }
        if (replaceHead && requiresTinyAnvil(nextHeadItem.family()) && !hasTinyAnvil()) {
            return "rngtech.tool_bench.failure.requires_tiny_anvil";
        }
        if (diamondTip) {
            if (!ToolHeadItem.canAcceptDiamondTip(nextHead)) {
                return diamondTipFailure();
            }
            ToolHeadItem.applyDiamondTip(nextHead);
        }

        boolean replacePart = replaceHead || replaceRod || replaceCell;
        ItemStack result;
        if (replacePart) {
            result = rebuiltToolStack(tool, nextHeadItem.family(), nextHead, nextRod, nextCell);
            if (result.isEmpty()) {
                return "rngtech.tool_bench.failure.battery_support";
            }
            preserveDamageRatio(tool, result, ModularToolItem.damageWear(result));
        } else {
            result = tool.copy();
            if (diamondTip) {
                result.set(ModDataComponents.FIELD_TOOL_ASSEMBLY.get(), new FieldToolAssembly(nextHead, nextRod, nextCell));
            }
            ModularToolItem.refreshMaxDamage(result);
        }
        boolean repaired = false;
        if (repair) {
            if (!ModularToolItem.isMatchingInstalledHeadRepairItem(result, repairInput)) {
                return "rngtech.tool_bench.failure.invalid_repair_material";
            }
            if (result.getDamageValue() <= 0) {
                if (!replacePart) {
                    return "rngtech.tool_bench.failure.not_damaged";
                }
            } else {
                repairTool(result);
                inventory.extractItem(SLOT_REPAIR_MATERIAL, 1, false);
                repaired = true;
            }
        }
        if (diamondTip) {
            inventory.extractItem(SLOT_REPAIR_MATERIAL, 1, false);
        }
        inventory.setStackInSlot(SLOT_TOOL, result);
        if (replaceHead) {
            inventory.setStackInSlot(SLOT_HEAD, ItemStack.EMPTY);
            giveOrDrop(player, componentWithStoredWear(current.head(), removedPartWear));
        }
        if (replaceRod) {
            inventory.setStackInSlot(SLOT_ROD, ItemStack.EMPTY);
            giveOrDrop(player, componentWithStoredWear(current.rod(), removedPartWear));
        }
        if (replaceCell) {
            inventory.setStackInSlot(SLOT_BATTERY_CELL, ItemStack.EMPTY);
            giveOrDrop(player, current.batteryCell());
        }
        setChanged();
        return replacePart
                ? "rngtech.tool_bench.success.swapped"
                : diamondTip ? "rngtech.tool_bench.success.diamond_tipped"
                : repaired ? "rngtech.tool_bench.success.repaired" : "rngtech.tool_bench.failure.no_changes";
    }

    private String applyDiamondTipToLooseHead() {
        ItemStack head = inventory.getStackInSlot(SLOT_HEAD);
        if (!ToolHeadItem.canAcceptDiamondTip(head)) {
            return diamondTipFailure();
        }
        ItemStack tippedHead = head.copy();
        ToolHeadItem.applyDiamondTip(tippedHead);
        inventory.setStackInSlot(SLOT_HEAD, tippedHead);
        inventory.extractItem(SLOT_REPAIR_MATERIAL, 1, false);
        setChanged();
        return "rngtech.tool_bench.success.diamond_tipped";
    }

    private ItemStack newToolStack(
            ToolHeadFamily family,
            ItemStack head,
            ItemStack rod,
            ItemStack batteryCell
    ) {
        return configureToolStack(new ItemStack(ModItems.modularTool(family).get()), family, head, rod, batteryCell);
    }

    private ItemStack rebuiltToolStack(
            ItemStack existingTool,
            ToolHeadFamily family,
            ItemStack head,
            ItemStack rod,
            ItemStack batteryCell
    ) {
        ItemStack result = existingTool.is(ModItems.modularTool(family).get())
                ? existingTool.copyWithCount(1)
                : existingTool.transmuteCopy(ModItems.modularTool(family).get(), 1);
        return configureToolStack(result, family, head, rod, batteryCell);
    }

    private ItemStack configureToolStack(
            ItemStack result,
            ToolHeadFamily family,
            ItemStack head,
            ItemStack rod,
            ItemStack batteryCell
    ) {
        int storedWear = Math.max(ModularToolItem.storedWear(head), ModularToolItem.storedWear(rod));
        ItemStack storedHead = head.copy();
        ItemStack storedRod = rod.copy();
        ModularToolItem.clearStoredWear(storedHead);
        ModularToolItem.clearStoredWear(storedRod);
        FieldToolAssembly assembly = new FieldToolAssembly(storedHead, storedRod, batteryCell);
        if (!assembly.isValidFor(family)) {
            return ItemStack.EMPTY;
        }
        result.set(ModDataComponents.FIELD_TOOL_ASSEMBLY.get(), assembly);
        result.remove(ModDataComponents.MACHINE_TRAITS.get());
        FieldToolEnchantments.migrateOuterEnchantments(result);
        ModularToolItem.refreshMaxDamage(result);
        ModularToolItem.applyDamageWear(result, storedWear);
        if (!batteryCell.isEmpty() && !ModularToolItem.isBatteryAccepted(result, batteryCell)) {
            return ItemStack.EMPTY;
        }
        return result;
    }

    private boolean setRefinementTarget(boolean headTarget, ItemStack refinedTarget) {
        ItemStack tool = inventory.getStackInSlot(SLOT_TOOL);
        if (tool.getItem() instanceof ModularToolItem toolItem) {
            FieldToolAssembly assembly = ModularToolItem.assembly(tool);
            if (assembly.isValidFor(toolItem.family())) {
                FieldToolAssembly refinedAssembly = headTarget
                        ? new FieldToolAssembly(refinedTarget, assembly.rod(), assembly.batteryCell())
                        : new FieldToolAssembly(assembly.head(), refinedTarget, assembly.batteryCell());
                ItemStack refinedTool = tool.copy();
                refinedTool.set(ModDataComponents.FIELD_TOOL_ASSEMBLY.get(), refinedAssembly);
                FieldToolEnchantments.migrateOuterEnchantments(refinedTool);
                preserveDamageRatio(tool, refinedTool);
                inventory.setStackInSlot(SLOT_TOOL, refinedTool);
                return true;
            }
        }

        int slot = headTarget ? SLOT_HEAD : SLOT_ROD;
        if (!RefinementTargets.canRefine(inventory.getStackInSlot(slot))) {
            return false;
        }
        inventory.setStackInSlot(slot, refinedTarget);
        return true;
    }

    private static boolean isRefinementOperationSupported(RefinementOperation operation) {
        return operation == RefinementOperation.ADD_MODIFIER
                || operation == RefinementOperation.ASCENSION_CATALYST
                || operation == RefinementOperation.ASCEND_RARITY;
    }

    private boolean hasTinyAnvil() {
        return inventory.getStackInSlot(SLOT_TINY_ANVIL).is(ModItems.TINY_ANVIL.get());
    }

    private boolean hasEnergyConnector() {
        return inventory.getStackInSlot(SLOT_ENERGY_CONNECTOR).getItem() instanceof EnergyConnectorItem;
    }

    private int connectorTransferRate() {
        ItemStack stack = inventory.getStackInSlot(SLOT_ENERGY_CONNECTOR);
        if (stack.getItem() instanceof EnergyConnectorItem connector) {
            return connector.tier().transferRate();
        }
        return 0;
    }

    private IEnergyStorage toolEnergyStorage() {
        ItemStack tool = inventory.getStackInSlot(SLOT_TOOL);
        return tool.isEmpty() ? null : tool.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private static boolean isDiamondTip(ItemStack stack) {
        return stack.is(ModItems.DIAMOND_TIP.get());
    }

    private static String diamondTipFailure() {
        return "rngtech.tool_bench.failure.invalid_diamond_tip_target";
    }

    private boolean requiresTinyAnvil(ToolHeadFamily family) {
        return family.requiresTinyAnvil();
    }

    private void preserveDamageRatio(ItemStack oldStack, ItemStack newStack) {
        preserveDamageRatio(oldStack, newStack, 0);
    }

    private void preserveDamageRatio(ItemStack oldStack, ItemStack newStack, int minimumWear) {
        ModularToolItem.applyDamageWear(newStack, Math.max(ModularToolItem.damageWear(oldStack), minimumWear));
    }

    private ItemStack componentWithStoredWear(ItemStack stack, int wear) {
        ItemStack copy = stack.copy();
        ModularToolItem.setStoredWear(copy, wear);
        return copy;
    }

    private void repairTool(ItemStack tool) {
        ModularToolItem.refreshMaxDamage(tool);
        int repairAmount = Math.max(1, tool.getMaxDamage() / 4);
        ModularToolItem.repairWithoutBreaking(tool, repairAmount);
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) {
            Containers.dropItemStack(player.level(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), copy);
        }
    }

    private void showSuccess(Player player, RefinementResult result) {
        player.displayClientMessage(Component.translatable(result.messageKey(), result.consumedPotential()), true);
    }

    private void showFailure(Player player, String messageKey) {
        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    private final class ToolBenchEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) {
                return 0;
            }
            IEnergyStorage toolStorage = toolEnergyStorage();
            int transferRate = connectorTransferRate();
            if (toolStorage == null || transferRate <= 0 || !toolStorage.canReceive()) {
                return 0;
            }
            int received = toolStorage.receiveEnergy(Math.min(toReceive, transferRate), simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            IEnergyStorage toolStorage = toolEnergyStorage();
            return toolStorage == null ? 0 : toolStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            IEnergyStorage toolStorage = toolEnergyStorage();
            return toolStorage == null ? 0 : toolStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            IEnergyStorage toolStorage = toolEnergyStorage();
            return connectorTransferRate() > 0 && toolStorage != null && toolStorage.canReceive();
        }
    }
}
