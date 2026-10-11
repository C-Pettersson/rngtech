package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.BioGeneratorMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModTags;
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
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BioGeneratorBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_BIO_CHAMBER = 1;
    public static final int GEAR_SLOT_COUNT = 2;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_FUEL = 1;
    public static final int STATUS_INVALID_FUEL = 2;
    public static final int STATUS_ENERGY_FULL = 3;

    private static final int BASE_GENERATION_RATE = 8;
    private static final int POTATO_FE = 120;
    private static final int POISONOUS_POTATO_FE = 200;
    private static final int CARROT_FE = 120;
    private static final int BREAD_FE = 240;
    private static final int SAPLING_FE = 80;
    private static final int SEED_FE = 40;
    private static final int PLANT_BIOMASS_FE = 80;
    private static final int ORGANIC_REAGENT_FE = 546;
    private static final int COMPOSTED_BIOMASS_FE = 1600;
    private static final int RICH_BIOMASS_FE = 3200;
    private static final int ALGAE_BIOMASS_FE = 400;
    private static final int DENSE_ALGAE_BIOMASS_FE = 2400;

    private static final int DATA_BURN_TIME = 0;
    private static final int DATA_TOTAL_BURN_TIME = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_MAX_OUTPUT = 5;
    private static final int DATA_RECIPE_ENERGY = 6;
    private static final int DATA_STATUS = 7;
    private static final int DATA_ENERGY_GENERATION = 8;
    private static final int DATA_ENERGY_CAPACITY_STAT = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_EFFICIENCY = 11;
    private static final int DATA_FUEL_EFFICIENCY = 12;
    private static final int DATA_POTATO_POWER = 13;
    private static final int DATA_CARROT_POWER = 14;
    private static final int DATA_BREAD_POWER = 15;
    private static final int DATA_SAPLING_POWER = 16;
    private static final int DATA_SEED_POWER = 17;
    private static final int DATA_PLANT_POWER = 18;
    private static final int DATA_ORGANIC_REAGENT_POWER = 19;
    private static final int DATA_COMPOSTED_BIOMASS_POWER = 20;
    private static final int DATA_ALGAE_POWER = 21;
    private static final int DATA_RICH_BIOMASS_POWER = 22;
    private static final int DATA_FUEL_DURATION = 23;
    private static final int DATA_REFINEMENT_POTENTIAL = 24;
    private static final int DATA_FLAT_ENERGY_GENERATION = 25;
    private static final int DATA_BASE_ENERGY_GENERATION = 26;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler fuelInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_FUEL && isAcceptedFuel(stack);
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
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_BIO_CHAMBER -> isBioChamber(stack);
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
        }
    };
    private final IItemHandler fuelHandler = new FuelItemHandler();
    private final IItemHandler emptyItemHandler = new EmptyItemHandler();
    private final IEnergyStorage energyStorage = new GeneratorEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_BURN_TIME -> burnTime;
                case DATA_TOTAL_BURN_TIME -> totalBurnTime;
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity(stats);
                case DATA_ENERGY_PER_TICK -> currentEnergyPerTick();
                case DATA_MAX_OUTPUT -> energyFlow.lastOutput();
                case DATA_RECIPE_ENERGY -> currentRecipeEnergy(stats);
                case DATA_STATUS -> statusCode(stats);
                case DATA_ENERGY_GENERATION -> (int) Math.round(stats.effectiveEnergyGenerationMultiplier() * STAT_SCALE);
                case DATA_FLAT_ENERGY_GENERATION -> (int) Math.round(stats.effectiveFlatEnergyGenerationBonus() * STAT_SCALE);
                case DATA_BASE_ENERGY_GENERATION -> (int) Math.round(baseEnergyPerTick() * STAT_SCALE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_FUEL_EFFICIENCY -> scaledStat(stats, MachineStat.FUEL_EFFICIENCY);
                case DATA_POTATO_POWER -> scaledStat(stats, MachineStat.POTATO_POWER);
                case DATA_CARROT_POWER -> scaledStat(stats, MachineStat.CARROT_POWER);
                case DATA_BREAD_POWER -> scaledStat(stats, MachineStat.BREAD_POWER);
                case DATA_SAPLING_POWER -> scaledStat(stats, MachineStat.SAPLING_POWER);
                case DATA_SEED_POWER -> scaledStat(stats, MachineStat.SEED_POWER);
                case DATA_PLANT_POWER -> scaledStat(stats, MachineStat.PLANT_POWER);
                case DATA_ORGANIC_REAGENT_POWER -> scaledStat(stats, MachineStat.ORGANIC_REAGENT_POWER);
                case DATA_COMPOSTED_BIOMASS_POWER -> scaledStat(stats, MachineStat.COMPOSTED_BIOMASS_POWER);
                case DATA_ALGAE_POWER -> scaledStat(stats, MachineStat.ALGAE_POWER);
                case DATA_RICH_BIOMASS_POWER -> scaledStat(stats, MachineStat.RICH_BIOMASS_POWER);
                case DATA_FUEL_DURATION -> scaledStat(stats, MachineStat.FUEL_DURATION);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
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

    private int burnTime;
    private int totalBurnTime;
    private int activeEnergyPerTick;
    private int internalEnergy;
    private double activeTotalEnergy;
    private double remainingEnergy;
    private double generationCarry;

    public BioGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BIO_GENERATOR.get(), pos, blockState, MachineType.BIO_GENERATOR, SLOT_FUEL, SLOT_FUEL, SLOT_FUEL);
        trackStatSlots(gearInventory);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BioGeneratorBlockEntity generator) {
        boolean generated = generator.tickFuel();
        boolean exported = generator.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, generator.hasBurnTime());
        if (generated || exported) {
            generator.setChanged();
        }
    }

    public ItemStackHandler getFuelInventory() {
        return fuelInventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == Direction.UP || side == Direction.DOWN ? null : trackedEnergyStorage;
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        int status = statusCode(stats);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        return MachineInfoSnapshot.builder("bio_generator")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_FUEL
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .energy(energyStored(), energyCapacity(stats), currentEnergyPerTick())
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.outputBottleneck(connector, currentEnergyPerTick())
                )
                .fuel(burnTime, totalBurnTime)
                .output(status == STATUS_ENERGY_FULL
                        ? MachineInfoSnapshot.OutputSummary.ENERGY_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_NO_FUEL -> "no_fuel";
            case STATUS_INVALID_FUEL -> "invalid_fuel";
            case STATUS_ENERGY_FULL -> "energy_full";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.bio_generator.status." + name;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        return side == Direction.UP || side == null ? fuelHandler : emptyItemHandler;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return fuelInventory;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(machineTraits(), Component.translatable("container.rngtech.bio_generator"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BioGeneratorMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public void dropInventory(Level level) {
        dropSlot(level, fuelInventory, SLOT_FUEL);
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            dropSlot(level, gearInventory, slot);
        }
        dropRefinementInventory(level);
    }

    public boolean isAcceptedFuel(ItemStack stack) {
        return baseFuelValue(stack) > 0;
    }

    public boolean isBatteryCell(ItemStack stack) {
        if (!(stack.getItem() instanceof BatteryCellItem cell)) {
            return false;
        }
        int stage = cell.material().stage();
        return stage >= 0 && stage <= 3;
    }

    public boolean isBioChamber(ItemStack stack) {
        return stack.getItem() instanceof MachinePartItem part
                && part.partType() == MachinePartType.BIO_CHAMBER
                && part.machineType() == MachineType.BIO_GENERATOR;
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(0L);
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.bioGenerator();
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        ItemStack chamber = bioChamberStack();
        if (isBioChamber(chamber)) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, chamber);
        }
        return stats;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FuelInventory", fuelInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalBurnTime", totalBurnTime);
        tag.putInt("ActiveEnergyPerTick", activeEnergyPerTick);
        tag.putInt("Energy", internalEnergy);
        tag.putDouble("ActiveTotalEnergy", activeTotalEnergy);
        tag.putDouble("RemainingEnergy", remainingEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuelInventory.deserializeNBT(registries, tag.getCompound("FuelInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        burnTime = tag.getInt("BurnTime");
        totalBurnTime = tag.getInt("TotalBurnTime");
        activeEnergyPerTick = Math.max(0, tag.getInt("ActiveEnergyPerTick"));
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        activeTotalEnergy = tag.getDouble("ActiveTotalEnergy");
        remainingEnergy = tag.getDouble("RemainingEnergy");
        generationCarry = tag.getDouble("GenerationCarry");
        if (hasBurnTime() && activeEnergyPerTick <= 0) {
            activeEnergyPerTick = BASE_GENERATION_RATE;
        }
        clampInternalEnergy();
    }

    private boolean tickFuel() {
        MachineStatAccumulator stats = effectiveStats();
        if (!hasBurnTime() && !tryStartFuel(stats)) {
            return false;
        }
        if (!hasBurnTime() || energyStored() >= energyCapacity(stats)) {
            return false;
        }

        int generation = currentEnergyPerTick();
        if (generation <= 0) {
            finishFuelIfSpent();
            return false;
        }

        generationCarry += Math.min(generation, remainingEnergy);
        int wholeEnergy = (int) Math.floor(generationCarry);
        if (wholeEnergy <= 0) {
            burnTime--;
            finishFuelIfSpent();
            return true;
        }

        int stored = storeGeneratedEnergy(wholeEnergy, stats);
        if (stored <= 0) {
            return false;
        }
        generationCarry -= stored;
        remainingEnergy = Math.max(0.0, remainingEnergy - stored);
        burnTime--;
        finishFuelIfSpent();
        return true;
    }

    private boolean tryStartFuel(MachineStatAccumulator stats) {
        if (energyStored() >= energyCapacity(stats)) {
            return false;
        }

        ItemStack fuel = fuelInventory.getStackInSlot(SLOT_FUEL);
        if (!isAcceptedFuel(fuel)) {
            clearActiveFuel();
            return false;
        }

        totalBurnTime = effectiveFuelDurationTicks(fuel, stats);
        activeEnergyPerTick = effectiveGenerationRate(stats);
        activeTotalEnergy = effectiveFuelEnergy(fuel, stats);
        if (activeTotalEnergy <= 0.0 || totalBurnTime <= 0 || activeEnergyPerTick <= 0) {
            clearActiveFuel();
            return false;
        }

        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (!remainder.isEmpty()) {
            if (fuel.isEmpty()) {
                fuelInventory.setStackInSlot(SLOT_FUEL, remainder);
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }

        remainingEnergy = activeTotalEnergy;
        burnTime = totalBurnTime;
        generationCarry = 0.0;
        setChanged();
        return true;
    }

    private double effectiveFuelEnergy(ItemStack fuel, MachineStatAccumulator stats) {
        return effectiveFuelDurationTicks(fuel, stats) * (double) effectiveGenerationRate(stats);
    }

    private int effectiveFuelDurationTicks(ItemStack fuel, MachineStatAccumulator stats) {
        double ingredientPower = ingredientPower(fuel, stats);
        double fuelEfficiency = Math.max(0.1, stats.value(MachineStat.FUEL_EFFICIENCY));
        double efficiency = Math.max(0.1, stats.value(MachineStat.EFFICIENCY));
        double fuelDuration = Math.max(0.1, stats.value(MachineStat.FUEL_DURATION));
        double baseEnergy = baseFuelValue(fuel) * ingredientPower * fuelEfficiency * efficiency;
        int baseTicks = Math.max(1, Mth.ceil(baseEnergy / BASE_GENERATION_RATE));
        return Math.max(1, Mth.ceil(baseTicks * fuelDuration));
    }

    private int effectiveGenerationRate(MachineStatAccumulator stats) {
        double multiplier = Math.max(0.1, stats.effectiveEnergyGenerationMultiplier());
        double flatBonus = Math.max(0.0, stats.effectiveFlatEnergyGenerationBonus());
        return Math.max(1, Mth.ceil(BASE_GENERATION_RATE * multiplier + flatBonus));
    }

    private double ingredientPower(ItemStack fuel, MachineStatAccumulator stats) {
        MachineStat stat = ingredientPowerStat(fuel);
        return stat == null ? 1.0 : Math.max(0.1, stats.value(stat));
    }

    private MachineStat ingredientPowerStat(ItemStack fuel) {
        if (fuel.is(ModTags.Items.BIO_GENERATOR_RICH_BIOMASS)) {
            return MachineStat.RICH_BIOMASS_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_DENSE_ALGAE_BIOMASS)
                || fuel.is(ModTags.Items.BIO_GENERATOR_ALGAE_BIOMASS)) {
            return MachineStat.ALGAE_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_COMPOSTED_BIOMASS)) {
            return MachineStat.COMPOSTED_BIOMASS_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_ORGANIC_REAGENTS)) {
            return MachineStat.ORGANIC_REAGENT_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_POTATO_FUELS)) {
            return MachineStat.POTATO_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_CARROT_FUELS)) {
            return MachineStat.CARROT_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_BREAD_FUELS)) {
            return MachineStat.BREAD_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_SAPLING_BIOMASS)) {
            return MachineStat.SAPLING_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_SEED_BIOMASS)) {
            return MachineStat.SEED_POWER;
        }
        if (fuel.is(ModTags.Items.BIO_GENERATOR_PLANT_BIOMASS)) {
            return MachineStat.PLANT_POWER;
        }
        return null;
    }

    private int baseFuelValue(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModTags.Items.BIO_GENERATOR_EXCLUDED_FOODS)) {
            return 0;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_RICH_BIOMASS)) {
            return RICH_BIOMASS_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_DENSE_ALGAE_BIOMASS)) {
            return DENSE_ALGAE_BIOMASS_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_ALGAE_BIOMASS)) {
            return ALGAE_BIOMASS_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_COMPOSTED_BIOMASS)) {
            return COMPOSTED_BIOMASS_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_ORGANIC_REAGENTS)) {
            return ORGANIC_REAGENT_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_BREAD_FUELS)) {
            return BREAD_FE;
        }
        if (stack.is(Items.POISONOUS_POTATO)) {
            return POISONOUS_POTATO_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_POTATO_FUELS)) {
            return POTATO_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_CARROT_FUELS)) {
            return CARROT_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_SAPLING_BIOMASS)) {
            return SAPLING_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_SEED_BIOMASS)) {
            return SEED_FE;
        }
        if (stack.is(ModTags.Items.BIO_GENERATOR_PLANT_BIOMASS)) {
            return PLANT_BIOMASS_FE;
        }
        return 0;
    }

    private int storeGeneratedEnergy(int energy, MachineStatAccumulator stats) {
        int remaining = Math.min(energy, energyCapacity(stats) - energyStored());
        if (remaining <= 0) {
            return 0;
        }

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
        if (energyStored() <= 0) {
            return false;
        }

        int remainingOutput = energyStored();
        boolean exported = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (remainingOutput <= 0 || energyStored() <= 0) {
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

    private int currentEnergyPerTick() {
        if (!hasBurnTime()) {
            return 0;
        }
        int generation = activeEnergyPerTick > 0 ? activeEnergyPerTick : BASE_GENERATION_RATE;
        return Math.max(0, (int) Math.ceil(Math.min(generation, remainingEnergy)));
    }

    private int currentRecipeEnergy(MachineStatAccumulator stats) {
        if (hasBurnTime()) {
            return Math.max(0, (int) Math.round(activeTotalEnergy));
        }
        ItemStack fuel = fuelInventory.getStackInSlot(SLOT_FUEL);
        return isAcceptedFuel(fuel) ? Math.max(1, (int) Math.round(effectiveFuelEnergy(fuel, stats))) : 0;
    }

    private int statusCode(MachineStatAccumulator stats) {
        if (energyStored() >= energyCapacity(stats)) {
            return STATUS_ENERGY_FULL;
        }
        if (hasBurnTime()) {
            return STATUS_READY;
        }
        ItemStack fuel = fuelInventory.getStackInSlot(SLOT_FUEL);
        if (fuel.isEmpty()) {
            return STATUS_NO_FUEL;
        }
        return isAcceptedFuel(fuel) ? STATUS_READY : STATUS_INVALID_FUEL;
    }

    private boolean hasBurnTime() {
        return burnTime > 0 && remainingEnergy > 0.0001;
    }

    private void finishFuelIfSpent() {
        if (burnTime <= 0 || remainingEnergy <= 0.0001) {
            clearActiveFuel();
        }
    }

    private void clearActiveFuel() {
        burnTime = 0;
        totalBurnTime = 0;
        activeEnergyPerTick = 0;
        activeTotalEnergy = 0.0;
        remainingEnergy = 0.0;
        generationCarry = 0.0;
    }

    private int energyStored() {
        return internalEnergyStored(effectiveStats()) + BatteryCellItem.energyStored(batteryCellStack());
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

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack stack = batteryCellStack();
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private ItemStack bioChamberStack() {
        return gearInventory.getStackInSlot(SLOT_BIO_CHAMBER);
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private void clampInternalEnergy() {
        MachineStatAccumulator stats = effectiveStats();
        internalEnergy = internalEnergyStored(stats);
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class FuelItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return fuelInventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return fuelInventory.insertItem(mappedSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return fuelInventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return fuelInventory.isItemValid(mappedSlot(slot), stack);
        }

        private int mappedSlot(int slot) {
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
            return SLOT_FUEL;
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

    private final class GeneratorEnergyStorage implements IEnergyStorage {
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

    /** Generation before machine and part stats, for the Stats tab breakdown; 0 when unknown. */
    private double baseEnergyPerTick() {
        return BASE_GENERATION_RATE;
    }
}
