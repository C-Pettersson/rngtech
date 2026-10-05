package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.SolarPanelBlock;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SolarPanelBlockEntity extends BaseMachineBlockEntity implements MachineInfoProvider {
    public static final int STATUS_CLEAR = 0;
    public static final int STATUS_WEATHER = 1;
    public static final int STATUS_NIGHT = 2;
    public static final int STATUS_NO_SKY = 3;
    public static final int STATUS_BAD_DIMENSION = 4;
    public static final int STATUS_FULL = 5;

    private static final long DAY_TICKS = 24000L;
    private static final long ZENITH_TICK = 6000L;
    private static final long PEAK_SOLAR_WINDOW_TICKS = 2000L;
    private static final int CONTROL_SUPPRESSION_TICKS = 2;

    private final SolarPanelBlock block;
    private final ItemStackHandler emptyInventory = new ItemStackHandler(0);
    private final IItemHandler emptyItemHandler = new EmptyItemHandler();
    private final IEnergyStorage energyStorage = new PanelEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private int internalEnergy;
    private double generationCarry;
    private long lastControlledTick = Long.MIN_VALUE;

    public SolarPanelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, blockState, MachineType.SOLAR_PANEL, 0, 0, 0);
        if (!(blockState.getBlock() instanceof SolarPanelBlock panelBlock)) {
            throw new IllegalStateException("Solar panel block entity created for non-panel block: " + blockState);
        }
        block = panelBlock;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        boolean controlled = panel.isControlled(level);
        boolean generated = !controlled && panel.generateFromSunlight();
        boolean exported = !controlled && panel.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, !controlled && panel.currentGeneration() > 0 && panel.energyStored() < panel.energyCapacity());
        if (generated || exported) {
            panel.setChanged();
        }
    }

    public SolarPanelMaterial material() {
        return block.material();
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedEnergyStorage;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        return emptyItemHandler;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return emptyInventory;
    }

    public void dropInventory(Level level) {
        dropRefinementInventory(level);
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.solarPanel(block.material());
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        return stats;
    }

    public void markControlled(long gameTime) {
        lastControlledTick = gameTime;
    }

    public int currentGeneration() {
        return currentGeneration(effectiveStats());
    }

    public int currentGeneration(MachineStatAccumulator stats) {
        if (level == null) {
            return 0;
        }
        int status = sunlightStatus(level, worldPosition);
        if (status != STATUS_CLEAR && status != STATUS_WEATHER) {
            return 0;
        }

        int environmentalBase = status == STATUS_WEATHER ? block.material().rainGeneration() : block.material().clearGeneration();
        double generationScale = stats.value(MachineStat.ENERGY_GENERATION)
                / Math.max(0.0001, stats.baseValue(MachineStat.ENERGY_GENERATION));
        double efficiency = Math.max(0.01, stats.value(MachineStat.EFFICIENCY));
        double generation = environmentalBase * generationScale * efficiency;
        generation = applyPeakSolarBonus(level, status, stats, generation);
        return Math.max(0, (int) Math.floor(generation));
    }

    public static double applyPeakSolarBonus(
            Level level,
            int sunlightStatus,
            MachineStatAccumulator stats,
            double generation
    ) {
        if (generation <= 0.0 || !isDaylightStatus(sunlightStatus) || !isPeakSolarWindow(level)) {
            return generation;
        }
        return generation * Math.max(1.0, stats.value(MachineStat.PEAK_SOLAR_GENERATION));
    }

    public static boolean isPeakSolarWindow(Level level) {
        long dayTime = Math.floorMod(level.getDayTime(), DAY_TICKS);
        return Math.abs(dayTime - ZENITH_TICK) <= PEAK_SOLAR_WINDOW_TICKS;
    }

    public int sunlightStatus(Level level, BlockPos pos) {
        if (!isDimensionEnabled(level)) {
            return STATUS_BAD_DIMENSION;
        }
        if (!level.canSeeSky(pos.above())) {
            return STATUS_NO_SKY;
        }
        if (!level.isDay()) {
            return STATUS_NIGHT;
        }
        if (level.isThundering() || level.isRainingAt(pos.above())) {
            return STATUS_WEATHER;
        }
        return STATUS_CLEAR;
    }

    private static boolean isDaylightStatus(int sunlightStatus) {
        return sunlightStatus == STATUS_CLEAR || sunlightStatus == STATUS_WEATHER;
    }

    public static boolean isDimensionEnabled(Level level) {
        if (level.dimension() == Level.OVERWORLD) {
            return RNGTechConfig.SOLAR_GENERATES_IN_OVERWORLD.get();
        }
        if (level.dimension() == Level.NETHER) {
            return RNGTechConfig.SOLAR_GENERATES_IN_NETHER.get();
        }
        if (level.dimension() == Level.END) {
            return RNGTechConfig.SOLAR_GENERATES_IN_END.get();
        }
        return RNGTechConfig.SOLAR_GENERATES_IN_OTHER_DIMENSIONS.get();
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        int status = infoStatus(stats);
        boolean controlled = level != null && isControlled(level);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        return MachineInfoSnapshot.builder("solar_panel")
                .stage(material().stage())
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_CLEAR,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_WEATHER || status == STATUS_NIGHT || status == STATUS_FULL
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .energy(
                        internalEnergyStored(stats),
                        energyCapacity(stats),
                        controlled || status == STATUS_FULL ? 0 : currentGeneration(stats)
                )
                .energyTelemetry(energyFlow.lastInput(), energyFlow.lastOutput(), connector.transferRate(), MachineInfoSnapshot.EnergyBottleneck.NONE)
                .output(status == STATUS_FULL
                        ? MachineInfoSnapshot.OutputSummary.ENERGY_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    private int infoStatus(MachineStatAccumulator stats) {
        if (level == null) {
            return STATUS_NO_SKY;
        }
        int status = sunlightStatus(level, worldPosition);
        if (isDaylightStatus(status) && internalEnergyStored(stats) >= energyCapacity(stats)) {
            return STATUS_FULL;
        }
        return status;
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_WEATHER -> "weather";
            case STATUS_NIGHT -> "night";
            case STATUS_NO_SKY -> "blocked";
            case STATUS_BAD_DIMENSION -> "dimension";
            case STATUS_FULL -> "full";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.solar.status." + name;
    }

    public int energyStored() {
        return internalEnergyStored(effectiveStats());
    }

    public int energyCapacity() {
        return energyCapacity(effectiveStats());
    }

    public int effectiveOutputRate() {
        return effectiveOutputRate(effectiveStats());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", internalEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.putLong("LastControlledTick", lastControlledTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        generationCarry = tag.getDouble("GenerationCarry");
        lastControlledTick = tag.getLong("LastControlledTick");
        clampInternalEnergy();
    }

    private boolean generateFromSunlight() {
        MachineStatAccumulator stats = effectiveStats();
        int generation = currentGeneration(stats);
        if (generation <= 0 || internalEnergyStored(stats) >= energyCapacity(stats)) {
            generationCarry = 0.0;
            return false;
        }

        generationCarry += generation;
        int wholeEnergy = (int) Math.floor(generationCarry);
        if (wholeEnergy <= 0) {
            return true;
        }

        int stored = receiveInternalEnergy(wholeEnergy, stats, false);
        generationCarry = stored == wholeEnergy ? generationCarry - wholeEnergy : 0.0;
        return stored > 0;
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        MachineStatAccumulator stats = effectiveStats();
        if (internalEnergyStored(stats) <= 0 || effectiveOutputRate(stats) <= 0) {
            return false;
        }

        int remainingOutput = Math.min(effectiveOutputRate(stats), internalEnergyStored(stats));
        boolean exported = false;
        for (Direction direction : Direction.values()) {
            if (remainingOutput <= 0 || internalEnergyStored(stats) <= 0) {
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
        int received = Math.min(toReceive, energyCapacity(stats) - internalEnergyStored(stats));
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
        int extracted = Math.min(toExtract, Math.min(effectiveOutputRate(stats), internalEnergyStored(stats)));
        if (!simulate && extracted > 0) {
            internalEnergy = internalEnergyStored(stats) - extracted;
            setChanged();
        }
        return extracted;
    }

    private int energyCapacity(MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.round(stats.value(MachineStat.ENERGY_CAPACITY)));
    }

    private int internalEnergyStored(MachineStatAccumulator stats) {
        return Mth.clamp(internalEnergy, 0, energyCapacity(stats));
    }

    private int effectiveOutputRate(MachineStatAccumulator stats) {
        return Math.max(0, (int) Math.round(stats.value(MachineStat.ENERGY_TRANSFER)));
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored(effectiveStats());
    }

    private boolean isControlled(Level level) {
        long gameTime = level.getGameTime();
        return lastControlledTick != Long.MIN_VALUE && gameTime - lastControlledTick <= CONTROL_SUPPRESSION_TICKS;
    }

    private final class PanelEnergyStorage implements IEnergyStorage {
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
            return energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return energyStored() > 0 && effectiveOutputRate() > 0;
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
        public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        @Override
        public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return false;
        }
    }
}
