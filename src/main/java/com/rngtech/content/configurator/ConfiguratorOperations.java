package com.rngtech.content.configurator;

import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyDistributionMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.ItemConnectorMode;
import com.rngtech.content.menu.UniversalConnectorAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ConfiguratorOperations {
    private ConfiguratorOperations() {
    }

    public static boolean isValid(ServerPlayer player, ConfiguratorTarget target) {
        return target != null
                && player.distanceToSqr(
                        target.pos().getX() + 0.5D,
                        target.pos().getY() + 0.5D,
                        target.pos().getZ() + 0.5D
                ) <= 64.0D
                && connectorTarget(player.level(), target) != null;
    }

    public static ConfiguratorPreset copy(
            ServerPlayer player,
            ConfiguratorTarget target,
            ConfiguratorPreset current,
            ConfiguratorPresetCategory category
    ) {
        ConnectorTarget connector = connectorTarget(player.level(), target);
        if (connector == null) {
            return current;
        }
        ConfiguratorPreset preset = current == null ? ConfiguratorPreset.EMPTY : current;
        if (category == ConfiguratorPresetCategory.ALL || category == ConfiguratorPresetCategory.ENERGY) {
            ConnectorSetting energy = connector.energySetting();
            if (energy != null) {
                preset = preset.withEnergy(energy);
            }
        }
        if (connector.universalAccess() != null
                && (category == ConfiguratorPresetCategory.ALL || category == ConfiguratorPresetCategory.FLUID)) {
            preset = preset.withFluidModules(connector.fluidSettings());
        }
        if (connector.universalAccess() != null
                && (category == ConfiguratorPresetCategory.ALL || category == ConfiguratorPresetCategory.ITEM)) {
            preset = preset.withItemModules(connector.itemSettings());
        }
        return preset;
    }

    public static ConfiguratorOperationResult paste(ServerPlayer player, ConfiguratorTarget target, ConfiguratorPreset preset) {
        ConnectorTarget connector = connectorTarget(player.level(), target);
        if (connector == null) {
            return ConfiguratorOperationResult.fail("rngtech.configurator.message.invalid_target");
        }
        if (preset == null || !preset.hasAnySettings()) {
            return ConfiguratorOperationResult.fail("rngtech.configurator.message.empty_preset");
        }
        boolean changed = false;
        if (preset.pasteModules() && connector.universalAccess() != null) {
            List<ModuleChange> moduleChanges = moduleChanges(connector.universalAccess(), preset);
            ConfiguratorOperationResult moduleResult = pasteModules(player, connector.universalAccess(), moduleChanges);
            if (!moduleResult.success()) {
                return moduleResult;
            }
            changed = !moduleChanges.isEmpty();
        }

        if (preset.energy().present()) {
            changed |= connector.applyEnergy(preset.energy());
        }
        if (connector.universalAccess() != null) {
            changed |= connector.applyFluid(preset.fluidModules());
            changed |= connector.applyItem(preset.itemModules());
        }
        return changed
                ? ConfiguratorOperationResult.success("rngtech.configurator.message.pasted")
                : ConfiguratorOperationResult.success("rngtech.configurator.message.no_change");
    }

    public static ConfiguratorOperationResult installGearHelper(ServerPlayer player, BlockPos pos) {
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        ItemStackHandler gearInventory = gearInventory(blockEntity);
        if (gearInventory == null && blockEntity instanceof CrusherBlockEntity crusher) {
            gearInventory = new ItemStackHandler(CrusherBlockEntity.SLOT_COUNT) {
                @Override
                public int getSlots() {
                    return 2;
                }

                @Override
                public ItemStack getStackInSlot(int slot) {
                    return crusher.getInventory().getStackInSlot(slot == 0 ? CrusherBlockEntity.SLOT_FUEL : CrusherBlockEntity.SLOT_CRUSH_HEAD);
                }

                @Override
                public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                    return crusher.getInventory().insertItem(
                            slot == 0 ? CrusherBlockEntity.SLOT_FUEL : CrusherBlockEntity.SLOT_CRUSH_HEAD,
                            stack,
                            simulate
                    );
                }
            };
        }
        if (gearInventory == null) {
            return ConfiguratorOperationResult.fail("rngtech.configurator.message.no_gear_target");
        }

        int installed = 0;
        Inventory inventory = player.getInventory();
        for (int gearSlot = 0; gearSlot < gearInventory.getSlots(); gearSlot++) {
            if (!gearInventory.getStackInSlot(gearSlot).isEmpty()) {
                continue;
            }
            for (int playerSlot = 0; playerSlot < inventory.getContainerSize(); playerSlot++) {
                ItemStack candidate = inventory.getItem(playerSlot);
                if (candidate.isEmpty()) {
                    continue;
                }
                ItemStack remainder = gearInventory.insertItem(gearSlot, candidate, true);
                if (remainder.getCount() == candidate.getCount()) {
                    continue;
                }
                ItemStack before = candidate.copy();
                ItemStack leftover = gearInventory.insertItem(gearSlot, candidate, false);
                inventory.setItem(playerSlot, leftover);
                if (leftover.getCount() < before.getCount()) {
                    installed++;
                }
                break;
            }
        }
        if (installed <= 0) {
            return ConfiguratorOperationResult.fail("rngtech.configurator.message.no_gear_installed");
        }
        return ConfiguratorOperationResult.success("rngtech.configurator.message.gear_installed", installed);
    }

    private static ConfiguratorOperationResult pasteModules(
            ServerPlayer player,
            UniversalConnectorAccess access,
            List<ModuleChange> changes
    ) {
        if (changes.isEmpty()) {
            return ConfiguratorOperationResult.success("rngtech.configurator.message.no_module_change");
        }
        if (!player.getAbilities().instabuild && !hasRequiredItems(player.getInventory(), changes)) {
            return ConfiguratorOperationResult.fail("rngtech.configurator.message.missing_modules");
        }
        if (!canStoreReplacedModules(player.getInventory(), changes, player.getAbilities().instabuild)) {
            return ConfiguratorOperationResult.fail("rngtech.configurator.message.no_inventory_space");
        }

        ItemStackHandler connectorInventory = access.getInventory();
        if (!player.getAbilities().instabuild) {
            for (ModuleChange change : changes) {
                consumeOne(player.getInventory(), change.desired());
            }
        }
        for (ModuleChange change : changes) {
            ItemStack removed = connectorInventory.getStackInSlot(change.slot()).copy();
            connectorInventory.setStackInSlot(change.slot(), change.desired().copyWithCount(1));
            if (!removed.isEmpty()) {
                if (!player.getInventory().add(removed)) {
                    player.drop(removed, false);
                }
            }
        }
        return ConfiguratorOperationResult.success("rngtech.configurator.message.modules_pasted");
    }

    private static List<ModuleChange> moduleChanges(UniversalConnectorAccess access, ConfiguratorPreset preset) {
        List<ModuleChange> changes = new ArrayList<>();
        addModuleChange(changes, access, UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR, preset.energy());
        for (int index = 0; index < UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT; index++) {
            addModuleChange(
                    changes,
                    access,
                    UniversalConnectorBlockEntity.fluidConnectorSlot(index),
                    preset.fluidModules().get(index)
            );
        }
        for (int index = 0; index < UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT; index++) {
            addModuleChange(
                    changes,
                    access,
                    UniversalConnectorBlockEntity.itemConnectorSlot(index),
                    preset.itemModules().get(index)
            );
        }
        return changes;
    }

    private static void addModuleChange(
            List<ModuleChange> changes,
            UniversalConnectorAccess access,
            int slot,
            ConnectorSetting setting
    ) {
        if (setting == null || !setting.present() || setting.module().isEmpty()) {
            return;
        }
        ItemStack desired = setting.module().copyWithCount(1);
        ItemStackHandler inventory = access.getInventory();
        ItemStack current = inventory.getStackInSlot(slot);
        if (ItemStack.isSameItemSameComponents(current, desired)) {
            return;
        }
        if (!inventory.isItemValid(slot, desired)) {
            return;
        }
        changes.add(new ModuleChange(slot, desired, current.copy()));
    }

    private static boolean hasRequiredItems(Inventory inventory, List<ModuleChange> changes) {
        List<ItemStack> required = requiredStacks(changes);
        for (ItemStack requiredStack : required) {
            int remaining = requiredStack.getCount();
            for (ItemStack stack : inventory.items) {
                if (remaining <= 0) {
                    break;
                }
                if (ItemStack.isSameItemSameComponents(stack, requiredStack)) {
                    remaining -= stack.getCount();
                }
            }
            for (ItemStack stack : inventory.offhand) {
                if (remaining <= 0) {
                    break;
                }
                if (ItemStack.isSameItemSameComponents(stack, requiredStack)) {
                    remaining -= stack.getCount();
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean canStoreReplacedModules(Inventory inventory, List<ModuleChange> changes, boolean creative) {
        List<ItemStack> simulatedMain = new ArrayList<>(inventory.items.size());
        for (ItemStack stack : inventory.items) {
            simulatedMain.add(stack.copy());
        }
        ItemStack simulatedOffhand = inventory.offhand.getFirst().copy();
        if (!creative) {
            for (ItemStack required : requiredStacks(changes)) {
                simulatedOffhand = removeSimulated(simulatedMain, simulatedOffhand, required);
            }
        }
        for (ModuleChange change : changes) {
            ItemStack removed = change.current().copy();
            if (!removed.isEmpty() && !addSimulated(simulatedMain, simulatedOffhand, inventory.selected, removed)) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> requiredStacks(List<ModuleChange> changes) {
        List<ItemStack> required = new ArrayList<>();
        for (ModuleChange change : changes) {
            boolean merged = false;
            for (ItemStack stack : required) {
                if (ItemStack.isSameItemSameComponents(stack, change.desired())) {
                    stack.grow(1);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                required.add(change.desired().copyWithCount(1));
            }
        }
        return required;
    }

    private static ItemStack removeSimulated(List<ItemStack> mainSlots, ItemStack offhand, ItemStack required) {
        int remaining = required.getCount();
        for (ItemStack slot : mainSlots) {
            if (remaining <= 0) {
                return offhand;
            }
            if (ItemStack.isSameItemSameComponents(slot, required)) {
                int removed = Math.min(remaining, slot.getCount());
                slot.shrink(removed);
                remaining -= removed;
            }
        }
        if (remaining > 0 && ItemStack.isSameItemSameComponents(offhand, required)) {
            int removed = Math.min(remaining, offhand.getCount());
            ItemStack updatedOffhand = offhand.copy();
            updatedOffhand.shrink(removed);
            return updatedOffhand;
        }
        return offhand;
    }

    private static boolean addSimulated(List<ItemStack> mainSlots, ItemStack offhand, int selected, ItemStack stack) {
        ItemStack remaining = stack.copy();
        if (selected >= 0 && selected < mainSlots.size()) {
            mergeIntoSimulatedSlot(mainSlots.get(selected), remaining);
        }
        mergeIntoSimulatedSlot(offhand, remaining);
        for (ItemStack slot : mainSlots) {
            mergeIntoSimulatedSlot(slot, remaining);
        }
        for (int index = 0; index < mainSlots.size() && !remaining.isEmpty(); index++) {
            if (mainSlots.get(index).isEmpty()) {
                ItemStack moved = remaining.copyWithCount(Math.min(remaining.getCount(), remaining.getMaxStackSize()));
                mainSlots.set(index, moved);
                remaining.shrink(moved.getCount());
            }
        }
        return remaining.isEmpty();
    }

    private static void mergeIntoSimulatedSlot(ItemStack slot, ItemStack remaining) {
        if (remaining.isEmpty()
                || slot.isEmpty()
                || !ItemStack.isSameItemSameComponents(slot, remaining)
                || !slot.isStackable()
                || slot.getCount() >= slot.getMaxStackSize()) {
            return;
        }
        int moved = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
        slot.grow(moved);
        remaining.shrink(moved);
    }

    private static boolean consumeOne(Inventory inventory, ItemStack required) {
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (ItemStack.isSameItemSameComponents(stack, required)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.items.set(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        for (int slot = 0; slot < inventory.offhand.size(); slot++) {
            ItemStack stack = inventory.offhand.get(slot);
            if (ItemStack.isSameItemSameComponents(stack, required)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.offhand.set(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    private static ConnectorTarget connectorTarget(Level level, ConfiguratorTarget target) {
        if (target == null) {
            return null;
        }
        BlockState state = level.getBlockState(target.pos());
        BlockEntity blockEntity = level.getBlockEntity(target.pos());
        return switch (target.type()) {
            case UNIVERSAL_CONNECTOR -> state.getBlock() instanceof UniversalConnectorBlock
                            && blockEntity instanceof UniversalConnectorBlockEntity connector
                    ? ConnectorTarget.universal(connector)
                    : null;
            case CABLE_UNIVERSAL_CONNECTOR -> blockEntity instanceof CableBlockEntity cable
                            && target.side() != null
                            && cable.hasUniversalConnector(target.side())
                    ? ConnectorTarget.cableUniversal(cable, target.side())
                    : null;
            case CABLE_ENERGY_CONNECTOR -> blockEntity instanceof CableBlockEntity cable
                            && target.side() != null
                            && cable.hasConnector(target.side())
                    ? ConnectorTarget.cableEnergy(cable, target.side())
                    : null;
        };
    }

    private static ItemStackHandler gearInventory(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        try {
            Method method = blockEntity.getClass().getMethod("getGearInventory");
            Object result = method.invoke(blockEntity);
            return result instanceof ItemStackHandler handler ? handler : null;
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException ignored) {
            return null;
        }
    }

    private record ModuleChange(int slot, ItemStack desired, ItemStack current) {
    }

    private record ConnectorTarget(
            UniversalConnectorAccess universalAccess,
            CableBlockEntity cable,
            Direction cableSide
    ) {
        private static ConnectorTarget universal(UniversalConnectorBlockEntity connector) {
            return new ConnectorTarget(connector, null, null);
        }

        private static ConnectorTarget cableUniversal(CableBlockEntity cable, Direction side) {
            return new ConnectorTarget(cable.universalConnectorAccess(side), cable, side);
        }

        private static ConnectorTarget cableEnergy(CableBlockEntity cable, Direction side) {
            return new ConnectorTarget(null, cable, side);
        }

        private ConnectorSetting energySetting() {
            if (universalAccess != null) {
                ItemStackHandler inventory = universalAccess.getInventory();
                return ConnectorSetting.of(
                        universalAccess.menuData().get(UniversalConnectorBlockEntity.dataChannelIndex()),
                        universalAccess.menuData().get(UniversalConnectorBlockEntity.dataModeIndex()),
                        universalAccess.menuData().get(UniversalConnectorBlockEntity.dataDistributionIndex()),
                        direction(universalAccess.menuData().get(UniversalConnectorBlockEntity.dataAttachAsIndex())),
                        universalAccess.defaultAttachAs(),
                        inventory.getStackInSlot(UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR)
                );
            }
            CableBlockEntity.ConnectorData connector = cable.connector(cableSide);
            if (connector == null) {
                return null;
            }
            return ConnectorSetting.of(
                    connector.channel(),
                    connector.mode().ordinal(),
                    connector.distributionMode().ordinal(),
                    connector.attachAs(),
                    cableSide.getOpposite(),
                    ItemStack.EMPTY
            );
        }

        private List<ConnectorSetting> fluidSettings() {
            List<ConnectorSetting> settings = new ArrayList<>(UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT);
            ItemStackHandler inventory = universalAccess.getInventory();
            for (int index = 0; index < UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT; index++) {
                settings.add(ConnectorSetting.of(
                        universalAccess.menuData().get(UniversalConnectorBlockEntity.dataFluidChannelIndex(index)),
                        universalAccess.menuData().get(UniversalConnectorBlockEntity.dataFluidModeIndex(index)),
                        0,
                        directionOrNull(universalAccess.menuData().get(UniversalConnectorBlockEntity.dataFluidAttachAsIndex(index))),
                        universalAccess.defaultAttachAs(),
                        inventory.getStackInSlot(UniversalConnectorBlockEntity.fluidConnectorSlot(index)),
                        filters(universalAccess, true, index)
                ));
            }
            return settings;
        }

        private List<ConnectorSetting> itemSettings() {
            List<ConnectorSetting> settings = new ArrayList<>(UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT);
            ItemStackHandler inventory = universalAccess.getInventory();
            for (int index = 0; index < UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT; index++) {
                settings.add(ConnectorSetting.of(
                        universalAccess.menuData().get(UniversalConnectorBlockEntity.dataItemChannelIndex(index)),
                        universalAccess.menuData().get(UniversalConnectorBlockEntity.dataItemModeIndex(index)),
                        0,
                        directionOrNull(universalAccess.menuData().get(UniversalConnectorBlockEntity.dataItemAttachAsIndex(index))),
                        universalAccess.defaultAttachAs(),
                        inventory.getStackInSlot(UniversalConnectorBlockEntity.itemConnectorSlot(index)),
                        filters(universalAccess, false, index)
                ));
            }
            return settings;
        }

        private boolean applyEnergy(ConnectorSetting setting) {
            if (!setting.present()) {
                return false;
            }
            boolean changed = false;
            Direction attachAs = setting.attachAs().resolve(defaultEnergyAttachAs());
            CableConnectorMode mode = cableMode(setting.modeOrdinal());
            EnergyDistributionMode distributionMode = energyDistributionMode(setting.distributionOrdinal());
            if (universalAccess != null) {
                changed |= universalAccess.setChannel(setting.channel());
                changed |= universalAccess.setAttachAs(attachAs == null ? universalAccess.defaultAttachAs() : attachAs);
                changed |= universalAccess.setMode(mode);
                changed |= universalAccess.setEnergyDistributionMode(distributionMode);
            } else {
                changed |= cable.setChannel(cableSide, setting.channel());
                changed |= cable.setAttachAs(cableSide, attachAs == null ? cableSide.getOpposite() : attachAs);
                changed |= cable.setMode(cableSide, mode);
                changed |= cable.setDistributionMode(cableSide, distributionMode);
            }
            return changed;
        }

        private boolean applyFluid(List<ConnectorSetting> settings) {
            boolean changed = false;
            for (int index = 0; index < Math.min(settings.size(), UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT); index++) {
                ConnectorSetting setting = settings.get(index);
                if (!setting.present()) {
                    continue;
                }
                changed |= universalAccess.setFluidChannel(index, setting.channel());
                changed |= universalAccess.setFluidAttachAs(index, setting.attachAs().resolve(universalAccess.defaultAttachAs()));
                changed |= universalAccess.setFluidMode(index, fluidMode(setting.modeOrdinal()));
                for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
                    changed |= universalAccess.setFluidFilter(index, filterIndex, setting.filter(filterIndex));
                }
            }
            return changed;
        }

        private boolean applyItem(List<ConnectorSetting> settings) {
            boolean changed = false;
            for (int index = 0; index < Math.min(settings.size(), UniversalConnectorBlockEntity.ITEM_MODULE_SLOT_COUNT); index++) {
                ConnectorSetting setting = settings.get(index);
                if (!setting.present()) {
                    continue;
                }
                changed |= universalAccess.setItemChannel(index, setting.channel());
                changed |= universalAccess.setItemAttachAs(index, setting.attachAs().resolve(universalAccess.defaultAttachAs()));
                changed |= universalAccess.setItemMode(index, itemMode(setting.modeOrdinal()));
                for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
                    changed |= universalAccess.setItemFilter(index, filterIndex, setting.filter(filterIndex));
                }
            }
            return changed;
        }

        private static List<ItemStack> filters(UniversalConnectorAccess access, boolean fluid, int moduleIndex) {
            List<ItemStack> filters = new ArrayList<>(UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE);
            ItemStackHandler filterInventory = access.getFilterInventory();
            for (int filterIndex = 0; filterIndex < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; filterIndex++) {
                int slot = fluid
                        ? UniversalConnectorBlockEntity.fluidFilterSlot(moduleIndex, filterIndex)
                        : UniversalConnectorBlockEntity.itemFilterSlot(moduleIndex, filterIndex);
                filters.add(filterInventory.getStackInSlot(slot));
            }
            return List.copyOf(filters);
        }

        private Direction defaultEnergyAttachAs() {
            return universalAccess == null ? cableSide.getOpposite() : universalAccess.defaultAttachAs();
        }

        private static Direction direction(int ordinal) {
            Direction[] values = Direction.values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Direction.NORTH;
        }

        private static Direction directionOrNull(int ordinal) {
            Direction[] values = Direction.values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
        }

        private static CableConnectorMode cableMode(int ordinal) {
            CableConnectorMode[] values = CableConnectorMode.values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CableConnectorMode.BOTH;
        }

        private static EnergyDistributionMode energyDistributionMode(int ordinal) {
            EnergyDistributionMode[] values = EnergyDistributionMode.values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : EnergyDistributionMode.ROUND_ROBIN;
        }

        private static FluidConnectorMode fluidMode(int ordinal) {
            FluidConnectorMode[] values = FluidConnectorMode.values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FluidConnectorMode.INPUT;
        }

        private static ItemConnectorMode itemMode(int ordinal) {
            ItemConnectorMode[] values = ItemConnectorMode.values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ItemConnectorMode.INPUT;
        }
    }
}
