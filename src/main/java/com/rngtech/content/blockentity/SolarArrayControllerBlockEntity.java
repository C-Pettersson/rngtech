package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.SolarArrayControllerMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SolarArrayControllerBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_SOLAR_ARRAY_EXTENDER = 1;
    public static final int GEAR_SLOT_COUNT = 2;
    private static final int LEGACY_SLOT_ENERGY_CONNECTOR = 1;

    public static final int STATUS_CLEAR = 0;
    public static final int STATUS_WEATHER = 1;
    public static final int STATUS_NIGHT = 2;
    public static final int STATUS_BAD_DIMENSION = 3;
    public static final int STATUS_NO_PANELS = 4;
    public static final int STATUS_FULL = 5;
    public static final int STATUS_BLOCKED = 6;

    private static final int MAX_RANGE = 2;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int DATA_ENERGY = 0;
    private static final int DATA_ENERGY_CAPACITY = 1;
    private static final int DATA_ENERGY_PER_TICK = 2;
    private static final int DATA_MAX_OUTPUT = 3;
    private static final int DATA_ACTIVE_PANELS = 4;
    private static final int DATA_BLOCKED_PANELS = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_ENERGY_GENERATION = 7;
    private static final int DATA_ENERGY_CAPACITY_STAT = 8;
    private static final int DATA_EFFICIENCY = 9;
    private static final int DATA_STABILITY = 10;
    private static final int DATA_REFINEMENT_POTENTIAL = 11;
    private static final int DATA_PANEL_LIMIT = 12;
    private static final int DATA_MOONLIGHT_CONVERSION = 13;
    private static final int DATA_WEATHER_RECOVERY = 14;
    private static final int DATA_PANEL_SYNCHRONIZATION = 15;
    private static final int DATA_OVERFLOW_SHUNTING = 16;
    private static final int DATA_CLEAR_SKY_AMPLIFICATION = 17;
    private static final int DATA_LUNAR_INVERSION = 18;
    private static final int DATA_PREVIEW_RANGE = 19;
    private static final int DATA_PANEL_RANGE = 20;
    private static final int DATA_FLAT_ENERGY_GENERATION = 21;
    private static final int DATA_BASE_ENERGY_GENERATION = 22;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_SOLAR_ARRAY_EXTENDER -> isSolarArrayExtender(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
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
            clampInternalEnergy();
            setChanged();
            syncPreviewToClient();
        }
    };
    private final IItemHandler emptyItemHandler = new EmptyItemHandler();
    private final IEnergyStorage energyStorage = new ControllerEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity(stats);
                case DATA_ENERGY_PER_TICK -> lastEnergyPerTick;
                case DATA_MAX_OUTPUT -> energyFlow.lastOutput();
                case DATA_ACTIVE_PANELS -> lastActivePanelCount;
                case DATA_BLOCKED_PANELS -> lastBlockedPanelCount;
                case DATA_STATUS -> lastStatus;
                case DATA_ENERGY_GENERATION -> (int) Math.round(stats.effectiveEnergyGenerationMultiplier() * STAT_SCALE);
                case DATA_FLAT_ENERGY_GENERATION -> (int) Math.round(stats.effectiveFlatEnergyGenerationBonus() * STAT_SCALE);
                case DATA_BASE_ENERGY_GENERATION -> (int) Math.round(baseEnergyPerTick() * STAT_SCALE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_PANEL_LIMIT -> panelLimit(stats);
                case DATA_MOONLIGHT_CONVERSION -> scaledStat(stats, MachineStat.MOONLIGHT_CONVERSION);
                case DATA_WEATHER_RECOVERY -> scaledStat(stats, MachineStat.WEATHER_RECOVERY);
                case DATA_PANEL_SYNCHRONIZATION -> scaledStat(stats, MachineStat.SOLAR_PANEL_SYNCHRONIZATION);
                case DATA_OVERFLOW_SHUNTING -> scaledStat(stats, MachineStat.OVERFLOW_SHUNTING);
                case DATA_CLEAR_SKY_AMPLIFICATION -> scaledStat(stats, MachineStat.CLEAR_SKY_AMPLIFICATION);
                case DATA_LUNAR_INVERSION -> scaledStat(stats, MachineStat.LUNAR_INVERSION);
                case DATA_PREVIEW_RANGE -> previewRange ? 1 : 0;
                case DATA_PANEL_RANGE -> panelRange(stats);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_BASE_ENERGY_GENERATION + 1;
        }
    };

    private final List<BlockPos> panelPositions = new ArrayList<>();
    private int internalEnergy;
    private double generationCarry;
    private int scanCooldown;
    private int overflowPanelCount;
    private int lastEnergyPerTick;
    private int lastActivePanelCount;
    private int lastBlockedPanelCount;
    private int lastStatus = STATUS_NO_PANELS;
    private boolean previewRange;

    public SolarArrayControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SOLAR_ARRAY_CONTROLLER.get(), pos, blockState, MachineType.SOLAR_ARRAY_CONTROLLER, 0, 0, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarArrayControllerBlockEntity controller) {
        controller.dropRemovedGear(level);
        boolean generated = controller.generateFromPanels(level);
        boolean exported = controller.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, controller.lastEnergyPerTick > 0 && controller.lastStatus != STATUS_FULL);
        if (generated || exported) {
            controller.setChanged();
        }
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedEnergyStorage;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        return side == null ? gearInventory : emptyItemHandler;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return gearInventory;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech.solar_array_controller")
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SolarArrayControllerMenu(containerId, playerInventory, this, menuData);
    }

    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public void dropInventory(Level level) {
        dropSlot(level, gearInventory, SLOT_BATTERY_CELL);
        dropSlot(level, gearInventory, SLOT_SOLAR_ARRAY_EXTENDER);
        dropRemovedGear(level);
        dropRefinementInventory(level);
    }

    public boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public boolean isSolarArrayExtender(ItemStack stack) {
        return stack.getItem() instanceof MachinePartItem part
                && part.machineType() == MachineType.SOLAR_ARRAY_CONTROLLER
                && part.partType() == MachinePartType.SOLAR_ARRAY_EXTENDER;
    }

    public void togglePreviewRange() {
        previewRange = !previewRange;
        setChanged();
        syncPreviewToClient();
    }

    public boolean shouldPreviewRange() {
        return previewRange;
    }

    public int panelRange() {
        return panelRange(effectiveStats());
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.solarArrayController();
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        ItemStack extender = gearInventory.getStackInSlot(SLOT_SOLAR_ARRAY_EXTENDER);
        if (isSolarArrayExtender(extender)) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, extender);
        }
        return stats;
    }

    @Override
    public void setMachineTraits(MachineTraits traits) {
        super.setMachineTraits(traits);
        syncPreviewToClient();
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        int status = lastStatus;
        boolean active = getBlockState().getValue(BaseMachineBlock.ACTIVE);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        return MachineInfoSnapshot.builder("solar_array_controller")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_CLEAR,
                                active,
                                status == STATUS_WEATHER || status == STATUS_NIGHT || status == STATUS_FULL
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .energy(energyStored(stats), energyCapacity(stats), active ? lastEnergyPerTick : 0)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.outputBottleneck(connector, active ? lastEnergyPerTick : 0)
                )
                .gear(BatteryCellItem.isBatteryCell(batteryCellStack())
                        ? MachineInfoSnapshot.GearSummary.BATTERY_CELL_INSTALLED
                        : MachineInfoSnapshot.GearSummary.NONE)
                .output(status == STATUS_FULL
                        ? MachineInfoSnapshot.OutputSummary.ENERGY_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_WEATHER -> "weather";
            case STATUS_NIGHT -> "night";
            case STATUS_BAD_DIMENSION -> "dimension";
            case STATUS_NO_PANELS -> "no_panels";
            case STATUS_FULL -> "full";
            case STATUS_BLOCKED -> "blocked";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.solar.status." + name;
    }

    @Override
    public int refinementComponentStage() {
        return 4;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("Energy", internalEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.putInt("ScanCooldown", scanCooldown);
        tag.putInt("LastEnergyPerTick", lastEnergyPerTick);
        tag.putInt("LastActivePanelCount", lastActivePanelCount);
        tag.putInt("LastBlockedPanelCount", lastBlockedPanelCount);
        tag.putInt("LastStatus", lastStatus);
        tag.putBoolean("PreviewRange", previewRange);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadGearInventory(tag.getCompound("GearInventory"), registries);
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        generationCarry = tag.getDouble("GenerationCarry");
        scanCooldown = Math.max(0, tag.getInt("ScanCooldown"));
        lastEnergyPerTick = tag.getInt("LastEnergyPerTick");
        lastActivePanelCount = tag.getInt("LastActivePanelCount");
        lastBlockedPanelCount = tag.getInt("LastBlockedPanelCount");
        lastStatus = tag.getInt("LastStatus");
        previewRange = tag.getBoolean("PreviewRange");
        clampInternalEnergy();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private boolean generateFromPanels(Level level) {
        MachineStatAccumulator stats = effectiveStats();
        refreshPanelCacheIfNeeded(level, stats);

        if (panelPositions.isEmpty()) {
            generationCarry = 0.0;
            lastEnergyPerTick = 0;
            lastActivePanelCount = 0;
            lastBlockedPanelCount = overflowPanelCount;
            lastStatus = SolarPanelBlockEntity.isDimensionEnabled(level) ? STATUS_NO_PANELS : STATUS_BAD_DIMENSION;
            return false;
        }

        int active = 0;
        int blocked = overflowPanelCount;
        double generatedThisTick = 0.0;
        SolarPanelMaterialState materialState = SolarPanelMaterialState.empty();
        boolean setBonus = hasCompleteSetBonus(level, stats);
        for (BlockPos panelPos : panelPositions) {
            if (!(level.getBlockEntity(panelPos) instanceof SolarPanelBlockEntity panel)) {
                blocked++;
                materialState.markMixed();
                continue;
            }

            panel.markControlled(level.getGameTime());
            materialState.accept(panel);
            double generation = panelGeneration(level, panel, stats) / controllerClaims(level, panelPos);
            if (generation > 0.0) {
                active++;
                generatedThisTick += generation;
            } else {
                blocked++;
            }
        }

        if (generatedThisTick > 0.0) {
            double controllerEfficiency = Math.max(0.01, stats.value(MachineStat.EFFICIENCY));
            generatedThisTick = Math.max(0.0, stats.generatedEnergyTotal(generatedThisTick, 1)) * controllerEfficiency;
        }
        if (active > 1 && materialState.singleMaterial() && stats.value(MachineStat.SOLAR_PANEL_SYNCHRONIZATION) > 0.0) {
            generatedThisTick *= 1.0 + stats.value(MachineStat.SOLAR_PANEL_SYNCHRONIZATION) / 100.0;
        }
        if (setBonus) {
            generatedThisTick *= 1.20;
        }

        lastActivePanelCount = active;
        lastBlockedPanelCount = blocked;
        lastEnergyPerTick = Math.max(0, (int) Math.floor(generatedThisTick));
        lastStatus = statusFor(level, active, blocked, stats);
        if (generatedThisTick <= 0.0 || energyStored(stats) >= energyCapacity(stats)) {
            generationCarry = 0.0;
            return false;
        }

        generationCarry += generatedThisTick;
        int wholeEnergy = (int) Math.floor(generationCarry);
        if (wholeEnergy <= 0) {
            return true;
        }

        int stored = storeGeneratedEnergy(wholeEnergy, stats);
        generationCarry = stored == wholeEnergy ? generationCarry - wholeEnergy : 0.0;
        return stored > 0;
    }

    /** One panel's output before the controller's generation stats, which apply once to the whole array. */
    private double panelGeneration(Level level, SolarPanelBlockEntity panel, MachineStatAccumulator controllerStats) {
        int status = panel.sunlightStatus(level, panel.getBlockPos());
        if (status != SolarPanelBlockEntity.STATUS_CLEAR
                && status != SolarPanelBlockEntity.STATUS_WEATHER
                && status != SolarPanelBlockEntity.STATUS_NIGHT) {
            return 0.0;
        }

        MachineStatAccumulator panelStats = panel.effectiveStats();
        double panelScale = panelStats.value(MachineStat.ENERGY_GENERATION)
                / Math.max(0.0001, panelStats.baseValue(MachineStat.ENERGY_GENERATION));
        double panelEfficiency = Math.max(0.01, panelStats.value(MachineStat.EFFICIENCY));
        double clearGeneration = panel.material().clearGeneration() * panelScale * panelEfficiency;
        double generation = clearGeneration;
        if (status == SolarPanelBlockEntity.STATUS_WEATHER) {
            double weatherGeneration = panel.material().rainGeneration() * panelScale * panelEfficiency;
            double weatherRecovery = Mth.clamp((controllerStats.value(MachineStat.STABILITY) - 1.0) * 0.25, 0.0, 0.5);
            weatherRecovery = Mth.clamp(weatherRecovery + controllerStats.value(MachineStat.WEATHER_RECOVERY) / 100.0, 0.0, 0.85);
            generation = weatherGeneration + (generation - weatherGeneration) * weatherRecovery;
        } else if (status == SolarPanelBlockEntity.STATUS_NIGHT) {
            double nightPercent = Math.max(
                    controllerStats.value(MachineStat.MOONLIGHT_CONVERSION),
                    controllerStats.value(MachineStat.LUNAR_INVERSION)
            );
            generation = nightPercent <= 0.0 ? 0.0 : clearGeneration * nightPercent / 100.0;
        } else if (controllerStats.value(MachineStat.CLEAR_SKY_AMPLIFICATION) > 0.0) {
            generation *= 1.0 + controllerStats.value(MachineStat.CLEAR_SKY_AMPLIFICATION) / 100.0;
        }

        if (status == SolarPanelBlockEntity.STATUS_CLEAR && controllerStats.value(MachineStat.LUNAR_INVERSION) > 0.0) {
            generation *= 0.85;
        }
        generation = SolarPanelBlockEntity.applyPeakSolarBonus(level, status, panelStats, generation);

        return Math.max(0.0, generation);
    }

    private int statusFor(Level level, int active, int blocked, MachineStatAccumulator stats) {
        if (!SolarPanelBlockEntity.isDimensionEnabled(level)) {
            return STATUS_BAD_DIMENSION;
        }
        if (energyStored(stats) >= energyCapacity(stats)) {
            return STATUS_FULL;
        }
        if (!level.isDay()) {
            return STATUS_NIGHT;
        }
        if (active > 0) {
            return level.isThundering() || level.isRaining() ? STATUS_WEATHER : STATUS_CLEAR;
        }
        return blocked > 0 ? STATUS_BLOCKED : STATUS_NO_PANELS;
    }

    private void refreshPanelCacheIfNeeded(Level level, MachineStatAccumulator stats) {
        if (scanCooldown > 0) {
            scanCooldown--;
            return;
        }
        scanCooldown = SCAN_INTERVAL_TICKS;
        int range = panelRange(stats);
        List<BlockPos> found = connectedPanels(level, range);

        if (stats.value(MachineStat.SOLAR_PANEL_ARBITRATION) > 0.0) {
            found.sort(Comparator
                    .comparingDouble((BlockPos panelPos) -> arbitrationGenerationSortValue(level, panelPos))
                    .reversed()
                    .thenComparingLong(this::distanceSortKey));
        } else {
            found.sort(Comparator.comparingLong(this::distanceSortKey));
        }
        panelPositions.clear();
        panelPositions.addAll(found);
        overflowPanelCount = Math.max(0, expectedPanelSlots(stats) - found.size());
    }

    private double arbitrationGenerationSortValue(Level level, BlockPos panelPos) {
        if (!(level.getBlockEntity(panelPos) instanceof SolarPanelBlockEntity panel)) {
            return 0.0;
        }
        MachineStatAccumulator panelStats = panel.effectiveStats();
        double panelScale = panelStats.value(MachineStat.ENERGY_GENERATION)
                / Math.max(0.0001, panelStats.baseValue(MachineStat.ENERGY_GENERATION));
        double generation = panel.material().clearGeneration() * panelScale * Math.max(0.01, panelStats.value(MachineStat.EFFICIENCY));
        return SolarPanelBlockEntity.applyPeakSolarBonus(
                level,
                SolarPanelBlockEntity.STATUS_CLEAR,
                panelStats,
                generation
        );
    }

    private long distanceSortKey(BlockPos pos) {
        long dx = pos.getX() - worldPosition.getX();
        long dz = pos.getZ() - worldPosition.getZ();
        return dx * dx + dz * dz;
    }

    private int storeGeneratedEnergy(int energy, MachineStatAccumulator stats) {
        int remaining = energy;
        int stored = receiveInternalEnergy(remaining, stats, false);
        remaining -= stored;

        IEnergyStorage cell = batteryCellEnergyStorage();
        if (cell != null && cell.canReceive() && remaining > 0) {
            int cellStored = cell.receiveEnergy(remaining, false);
            if (cellStored > 0) {
                stored += cellStored;
                setChanged();
            }
        }
        return stored;
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        MachineStatAccumulator stats = effectiveStats();
        if (energyStored(stats) <= 0) {
            return false;
        }

        int remainingOutput = energyStored(stats);
        boolean exported = false;
        for (Direction direction : Direction.values()) {
            if (remainingOutput <= 0 || energyStored(stats) <= 0) {
                return exported;
            }
            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    pos.relative(direction),
                    direction.getOpposite()
            );
            if (target == null || !target.canReceive()) {
                continue;
            }

            int offered = extractEnergyInternal(remainingOutput, stats, true);
            int received = target.receiveEnergy(offered, false);
            int delivered = extractEnergyInternal(received, stats, false);
            energyFlow.recordOutput(delivered);
            remainingOutput -= delivered;
            exported |= delivered > 0;
        }
        return exported;
    }

    private int receiveInternalEnergy(int toReceive, MachineStatAccumulator stats, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }
        int received = Math.min(toReceive, internalEnergyCapacity(stats) - internalEnergyStored(stats));
        if (!simulate && received > 0) {
            internalEnergy = internalEnergyStored(stats) + received;
            setChanged();
        }
        return received;
    }

    private int extractEnergyInternal(int toExtract, MachineStatAccumulator stats, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }

        int remaining = toExtract;
        int extracted = Math.min(internalEnergyStored(stats), remaining);
        if (!simulate && extracted > 0) {
            internalEnergy = internalEnergyStored(stats) - extracted;
            setChanged();
        }
        remaining -= extracted;

        IEnergyStorage cell = batteryCellEnergyStorage();
        if (cell != null && cell.canExtract() && remaining > 0) {
            int cellExtracted = cell.extractEnergy(remaining, simulate);
            extracted += cellExtracted;
            if (!simulate && cellExtracted > 0) {
                setChanged();
            }
        }
        return extracted;
    }

    private int energyStored() {
        return energyStored(effectiveStats());
    }

    private int energyStored(MachineStatAccumulator stats) {
        return internalEnergyStored(stats) + BatteryCellItem.energyStored(batteryCellStack());
    }

    private int energyCapacity(MachineStatAccumulator stats) {
        return internalEnergyCapacity(stats) + BatteryCellItem.energyCapacity(batteryCellStack());
    }

    private int internalEnergyCapacity(MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.round(stats.value(MachineStat.ENERGY_CAPACITY)));
    }

    private int internalEnergyStored(MachineStatAccumulator stats) {
        return Mth.clamp(internalEnergy, 0, internalEnergyCapacity(stats));
    }

    private int panelLimit(MachineStatAccumulator stats) {
        return expectedPanelSlots(stats);
    }

    private int panelRange(MachineStatAccumulator stats) {
        return Math.max(1, Math.min(MAX_RANGE, stats.intValue(MachineStat.SOLAR_PANEL_LIMIT)));
    }

    private int expectedPanelSlots(MachineStatAccumulator stats) {
        int width = panelRange(stats) * 2 + 1;
        return width * width - 1;
    }

    private void syncPreviewToClient() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private int controllerClaims(Level level, BlockPos panelPos) {
        int claims = 1;
        int maxSearch = MAX_RANGE;
        for (int x = -maxSearch; x <= maxSearch; x++) {
            for (int z = -maxSearch; z <= maxSearch; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                BlockPos candidate = panelPos.offset(x, 0, z);
                if (candidate.equals(worldPosition)) {
                    continue;
                }
                if (level.getBlockEntity(candidate) instanceof SolarArrayControllerBlockEntity controller
                        && controller.controlsPanel(level, panelPos, controller.effectiveStats())) {
                    claims++;
                }
            }
        }
        return Math.max(1, claims);
    }

    private boolean controlsPanel(Level level, BlockPos panelPos, MachineStatAccumulator stats) {
        int range = panelRange(stats);
        return coversPanel(panelPos, range) && connectedPanels(level, range).contains(panelPos.immutable());
    }

    private boolean coversPanel(BlockPos panelPos, int range) {
        if (panelPos.equals(worldPosition)) {
            return false;
        }
        if (panelPos.getY() != worldPosition.getY()) {
            return false;
        }
        int dx = Math.abs(panelPos.getX() - worldPosition.getX());
        int dz = Math.abs(panelPos.getZ() - worldPosition.getZ());
        return dx <= range && dz <= range;
    }

    private List<BlockPos> connectedPanels(Level level, int range) {
        List<BlockPos> connected = new ArrayList<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                enqueuePanel(level, worldPosition.offset(x, 0, z), range, visited, frontier);
            }
        }

        while (!frontier.isEmpty()) {
            BlockPos panelPos = frontier.removeFirst();
            connected.add(panelPos);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    enqueuePanel(level, panelPos.offset(x, 0, z), range, visited, frontier);
                }
            }
        }
        return connected;
    }

    private void enqueuePanel(Level level, BlockPos candidate, int range, Set<BlockPos> visited, ArrayDeque<BlockPos> frontier) {
        BlockPos immutable = candidate.immutable();
        if (!coversPanel(immutable, range) || visited.contains(immutable)) {
            return;
        }
        visited.add(immutable);
        if (level.getBlockEntity(immutable) instanceof SolarPanelBlockEntity) {
            frontier.add(immutable);
        }
    }

    private boolean hasCompleteSetBonus(Level level, MachineStatAccumulator stats) {
        int range = panelRange(stats);
        SolarPanelMaterial material = null;
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                BlockPos candidate = worldPosition.offset(x, 0, z);
                if (!(level.getBlockEntity(candidate) instanceof SolarPanelBlockEntity panel)) {
                    return false;
                }
                if (material == null) {
                    material = panel.material();
                } else if (material.stage() != panel.material().stage()) {
                    return false;
                }
            }
        }
        return material != null;
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack stack = batteryCellStack();
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored(effectiveStats());
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private void loadGearInventory(CompoundTag gearTag, HolderLookup.Provider registries) {
        loadGearWithoutSlot(gearInventory, gearTag, registries, LEGACY_SLOT_ENERGY_CONNECTOR);
    }

    private final class ControllerEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return extractEnergyInternal(toExtract, effectiveStats(), simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity(effectiveStats());
        }

        @Override
        public boolean canExtract() {
            return energyStored() > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    private static final class EmptyItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private static final class SolarPanelMaterialState {
        private SolarPanelMaterial material;
        private boolean mixed;

        private static SolarPanelMaterialState empty() {
            return new SolarPanelMaterialState();
        }

        private void accept(SolarPanelBlockEntity panel) {
            if (material == null) {
                material = panel.material();
            } else if (material != panel.material()) {
                mixed = true;
            }
        }

        private void markMixed() {
            mixed = true;
        }

        private boolean singleMaterial() {
            return material != null && !mixed;
        }
    }

    /** Solar output has no fixed base, so the Stats tab breakdown omits it. */
    private double baseEnergyPerTick() {
        return 0.0;
    }
}
