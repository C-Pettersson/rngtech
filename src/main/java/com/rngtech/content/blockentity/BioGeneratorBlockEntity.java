package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.energy.BiogasDigestion;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.BiogasMembraneItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.BioGeneratorMenu;
import com.rngtech.content.menu.GenerationMenuSupport;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModTags;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;

public class BioGeneratorBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider, PurgeableFluidStorage {
    private final EnergyExportTargets exportTargets = new EnergyExportTargets(this);
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_BIO_CHAMBER = 1;
    /** Two flexible gas-line slots: each takes a Fluid Pump or a Biogas Membrane. */
    public static final int SLOT_GAS_LINE_A = 2;
    public static final int SLOT_GAS_LINE_B = 3;
    public static final int GEAR_SLOT_COUNT = 4;

    public static final int GAS_LINE_EMPTY = 0;
    public static final int GAS_LINE_PUMP = 1;
    public static final int GAS_LINE_MEMBRANE = 2;

    public static final int PURGE_METHANE_TANK = 0;
    public static final int PURGE_EXHAUST_TANK = 1;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_FUEL = 1;
    public static final int STATUS_INVALID_FUEL = 2;
    public static final int STATUS_ENERGY_FULL = 3;
    public static final int STATUS_PRIMING = 4;
    public static final int STATUS_TAKE_OFF = 5;
    public static final int STATUS_METHANE_FULL = 6;
    public static final int STATUS_NEEDS_PUMP = 7;

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
    private static final int DATA_GAS = 27;
    private static final int DATA_GAS_CAPACITY = 28;
    private static final int DATA_RETENTION = 29;
    private static final int DATA_ENGINE_RATE = 30;
    private static final int DATA_ENGINE_RUNNING = 31;
    private static final int DATA_FEED_ENERGY = 32;
    private static final int DATA_TAKE_OFF = 33;
    private static final int DATA_METHANE = 34;
    private static final int DATA_EXHAUST = 35;
    private static final int DATA_TAKE_OFF_CAPACITY = 36;
    private static final int DATA_RECOVERY = 37;
    private static final int DATA_CAPTURE = 38;
    private static final int DATA_METHANE_RATE = 39;
    private static final int DATA_VENT_RATE = 40;
    private static final int DATA_PACK_GENERATION_MULTIPLIER = 41;
    /** Recovery is synced in tenths of a percent, rates in hundredths of a mB per tick. */
    private static final int RECOVERY_SCALE = 1000;
    private static final int RATE_SCALE = 100;
    private static final int STAT_SCALE = 100;
    private static final int VISUAL_SYNC_INTERVAL = 2;
    private static final double EPSILON = 1.0E-6;
    /** Top of the exhaust stack in model pixels, matching tools/blockmodels/bio_generator.py. */
    private static final double EXHAUST_X = 2.25;
    private static final double EXHAUST_Y = 15.2;
    private static final double EXHAUST_Z = 2.25;

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
                case SLOT_GAS_LINE_A, SLOT_GAS_LINE_B -> isGasLinePart(stack);
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
            pumpStats = null;
            clampInternalEnergy();
            updateTakeOffCapacity();
            setChanged();
        }
    };
    private final FluidTank methaneTank = takeOffTank(ModFluids.METHANE_SOURCE.get());
    private final FluidTank exhaustTank = takeOffTank(ModFluids.CARBON_EXHAUST_SOURCE.get());
    private final IFluidHandler takeOffHandler = new TakeOffFluidHandler();
    private final IItemHandler fuelHandler = new FuelItemHandler();
    private final IEnergyStorage energyStorage = new GeneratorEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final InstalledBatteryCell batteryCell = new InstalledBatteryCell(this::batteryCellStack);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_BURN_TIME -> remainingDigestionTicks(stats);
                case DATA_TOTAL_BURN_TIME -> totalDigestionTicks(stats);
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
                case DATA_GAS -> (int) Math.floor(gas + EPSILON);
                case DATA_GAS_CAPACITY -> BiogasDigestion.HOLDER_CAPACITY;
                case DATA_RETENTION -> retentionDays;
                case DATA_ENGINE_RATE -> engineRate(stats);
                case DATA_ENGINE_RUNNING -> engineRunning ? 1 : 0;
                case DATA_FEED_ENERGY -> (int) Math.round(currentFeedEnergy(stats));
                case DATA_TAKE_OFF -> takeOffMode() ? 1 : 0;
                case DATA_METHANE -> methaneTank.getFluidAmount();
                case DATA_EXHAUST -> exhaustTank.getFluidAmount();
                case DATA_TAKE_OFF_CAPACITY -> methaneTank.getCapacity();
                case DATA_RECOVERY -> (int) Math.round(methaneRecovery() * RECOVERY_SCALE);
                case DATA_CAPTURE -> capturesCarbon() ? 1 : 0;
                case DATA_METHANE_RATE -> (int) Math.round(lastMethane * RATE_SCALE);
                case DATA_VENT_RATE -> (int) Math.round(lastVented * RATE_SCALE);
                case DATA_PACK_GENERATION_MULTIPLIER -> GenerationMenuSupport.packMultiplierData(refinementComponentStage());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_PACK_GENERATION_MULTIPLIER + 1;
        }
    };

    private int internalEnergy;
    private int retentionDays = BiogasDigestion.RATED_RETENTION_DAYS;
    /** Rated FE of the item in the digester, and how much of it has not digested yet. */
    private double feedTotal;
    private double feedRemaining;
    /** Biogas in the gas holder, in FE. */
    private double gas;
    private boolean engineRunning;
    private double generationCarry;
    private double lastDigestion;
    private double lastGeneration;
    /** Take-off fractions below 1 mB, carried to the next tick, and the last tick's output for display. */
    private double methaneCarry;
    private double exhaustCarry;
    private double lastMethane;
    private double lastVented;
    /** Whether the feed and holder gas are valued for the take-off, which leaves Energy Generation out of gas per item. */
    private boolean takeOffValued;
    private MachineStatAccumulator pumpStats;
    private BioGeneratorVisualState visualState = BioGeneratorVisualState.IDLE;
    private BioGeneratorVisualState.Key lastSyncedVisual;
    private final BioGeneratorVisualState.Animation animation = new BioGeneratorVisualState.Animation();

    public BioGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BIO_GENERATOR.get(), pos, blockState, MachineType.BIO_GENERATOR, SLOT_FUEL, SLOT_FUEL, SLOT_FUEL);
        trackStatSlots(gearInventory);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BioGeneratorBlockEntity generator) {
        generator.revalueForMode();
        boolean digested = generator.tickDigester();
        boolean generated = generator.takeOffMode() ? generator.tickTakeOff() : generator.tickEngine();
        boolean exported = generator.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, generator.isWorking());
        if (digested || generated || exported) {
            generator.setChanged();
        }
        generator.syncVisualState(level);
    }

    /** Engine exhaust: puffs leave the stack faster and heavier as the engine works harder. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, BioGeneratorBlockEntity generator) {
        generator.animation.clientTicks++;
        // The stack puffs for the engine's exhaust, or for the CO2 a take-off vents.
        float engine = Math.max(generator.visualState.engine(), generator.visualState.vent());
        if (engine <= 0.05F) {
            return;
        }
        int interval = Math.max(2, Math.round(5.0F / engine));
        if (generator.animation.clientTicks % interval != 0) {
            return;
        }
        double x = EXHAUST_X / 16.0 - 0.5;
        double z = EXHAUST_Z / 16.0 - 0.5;
        double angle = Math.toRadians(180.0 - state.getValue(BaseMachineBlock.FACING).toYRot());
        double worldX = x * Math.cos(angle) + z * Math.sin(angle);
        double worldZ = -x * Math.sin(angle) + z * Math.cos(angle);
        RandomSource random = level.getRandom();
        level.addParticle(
                engine > 1.2F && random.nextInt(3) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                pos.getX() + 0.5 + worldX + (random.nextDouble() - 0.5) * 0.04,
                pos.getY() + EXHAUST_Y / 16.0,
                pos.getZ() + 0.5 + worldZ + (random.nextDouble() - 0.5) * 0.04,
                0.0,
                0.03 + 0.02 * engine,
                0.0
        );
    }

    public int retentionDays() {
        return retentionDays;
    }

    /** Sets the digester retention time in days; it applies to the feed already digesting. */
    public void setRetentionDays(int days) {
        int clamped = BiogasDigestion.clampRetention(days);
        if (clamped != retentionDays) {
            retentionDays = clamped;
            setChanged();
        }
    }

    public double gasStored() {
        return gas;
    }

    public boolean engineRunning() {
        return engineRunning;
    }

    /** Client copy of the state the renderer draws; synced from the server in the block entity update tag. */
    public BioGeneratorVisualState visualState() {
        return visualState;
    }

    public BioGeneratorVisualState.Animation animation() {
        return animation;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return currentVisualState().save();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        visualState = BioGeneratorVisualState.load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        visualState = BioGeneratorVisualState.load(packet.getTag());
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        BioGeneratorVisualState.Key key = currentVisualState().key();
        if (!key.equals(lastSyncedVisual)) {
            lastSyncedVisual = key;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private BioGeneratorVisualState currentVisualState() {
        double rated = Math.max(1.0, ratedGeneration(effectiveStats()));
        return new BioGeneratorVisualState(
                (float) Mth.clamp(gas / BiogasDigestion.HOLDER_CAPACITY, 0.0, 1.0),
                feedTotal <= EPSILON ? 0.0F : (float) Mth.clamp(feedRemaining / feedTotal, 0.0, 1.0),
                (float) Mth.clamp(lastDigestion / rated, 0.0, 2.0),
                (float) Mth.clamp(lastGeneration / rated, 0.0, 2.0),
                (float) Mth.clamp(lastVented / (rated * BiogasDigestion.CARBON_EXHAUST_PER_FE), 0.0, 2.0),
                isBioChamber(bioChamberStack()),
                gasLineKind(gearInventory.getStackInSlot(SLOT_GAS_LINE_A)),
                gasLineKind(gearInventory.getStackInSlot(SLOT_GAS_LINE_B)),
                fuelInventory.getStackInSlot(SLOT_FUEL).getItem()
        );
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

    /** FE export works on every face; the back port only marks where a connector looks best. */
    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedEnergyStorage;
    }

    /** Take-off output on every face: pipes drain Methane and Carbon Exhaust, and nothing fills the block. */
    public IFluidHandler getFluidHandler(Direction side) {
        return side == null ? null : takeOffHandler;
    }

    public FluidTank methaneTank() {
        return methaneTank;
    }

    public FluidTank exhaustTank() {
        return exhaustTank;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(
                FluidPurgeTarget.of(
                        PURGE_METHANE_TANK,
                        Component.translatable("rngtech.purge.target.methane_tank"),
                        FluidPurgeRole.OUTPUT,
                        methaneTank::getFluid,
                        methaneTank::drain,
                        this::setChanged
                ).withCapacity(methaneTank::getCapacity),
                FluidPurgeTarget.of(
                        PURGE_EXHAUST_TANK,
                        Component.translatable("rngtech.purge.target.exhaust_tank"),
                        FluidPurgeRole.OUTPUT,
                        exhaustTank::getFluid,
                        exhaustTank::drain,
                        this::setChanged
                ).withCapacity(exhaustTank::getCapacity)
        );
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
                .fuel(remainingDigestionTicks(stats), totalDigestionTicks(stats))
                .output(switch (status) {
                    case STATUS_ENERGY_FULL -> MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
                    case STATUS_METHANE_FULL -> MachineInfoSnapshot.OutputSummary.OUTPUT_FULL;
                    default -> MachineInfoSnapshot.OutputSummary.NONE;
                })
                .refinement(machineTraits())
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_NO_FUEL -> "no_fuel";
            case STATUS_INVALID_FUEL -> "invalid_fuel";
            case STATUS_ENERGY_FULL -> "energy_full";
            case STATUS_PRIMING -> "priming";
            case STATUS_TAKE_OFF -> "take_off";
            case STATUS_METHANE_FULL -> "methane_full";
            case STATUS_NEEDS_PUMP -> "needs_pump";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.bio_generator.status." + name;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        return fuelHandler;
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

    public static boolean isFluidPump(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.FLUID_PUMP);
    }

    public static boolean isBiogasMembrane(ItemStack stack) {
        return stack.getItem() instanceof BiogasMembraneItem;
    }

    public static boolean isGasLinePart(ItemStack stack) {
        return isFluidPump(stack) || isBiogasMembrane(stack);
    }

    /** Any Fluid Pump stops the engine and sends the gas out; membranes alone change nothing. */
    public boolean takeOffMode() {
        return gasLineCount(GAS_LINE_PUMP) > 0;
    }

    /** Share of the biogas Methane the take-off recovers: the best installed membrane, or the bare pump's. */
    public double methaneRecovery() {
        double recovery = BiogasDigestion.PUMP_RECOVERY;
        for (int slot = SLOT_GAS_LINE_A; slot <= SLOT_GAS_LINE_B; slot++) {
            ItemStack stack = gearInventory.getStackInSlot(slot);
            if (isBiogasMembrane(stack)) {
                recovery = Math.max(recovery, BiogasMembraneItem.recovery(stack));
            }
        }
        return recovery;
    }

    /** A membrane captures the CO2 as Carbon Exhaust; a bare pump vents it. */
    public boolean capturesCarbon() {
        return takeOffMode() && gasLineCount(GAS_LINE_MEMBRANE) > 0;
    }

    private int gasLineCount(int kind) {
        int count = 0;
        for (int slot = SLOT_GAS_LINE_A; slot <= SLOT_GAS_LINE_B; slot++) {
            if (gasLineKind(gearInventory.getStackInSlot(slot)) == kind) {
                count++;
            }
        }
        return count;
    }

    private static int gasLineKind(ItemStack stack) {
        if (isFluidPump(stack)) {
            return GAS_LINE_PUMP;
        }
        return isBiogasMembrane(stack) ? GAS_LINE_MEMBRANE : GAS_LINE_EMPTY;
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(0L);
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.bioGenerator();
        stats.apply(effectiveTraits());
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
        tag.putInt("Energy", internalEnergy);
        tag.putInt("RetentionDays", retentionDays);
        tag.putDouble("FeedTotal", feedTotal);
        tag.putDouble("FeedRemaining", feedRemaining);
        tag.putDouble("Gas", gas);
        tag.putBoolean("EngineRunning", engineRunning);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.put("MethaneTank", methaneTank.writeToNBT(registries, new CompoundTag()));
        tag.put("ExhaustTank", exhaustTank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("MethaneCarry", methaneCarry);
        tag.putDouble("ExhaustCarry", exhaustCarry);
        tag.putBoolean("TakeOffValued", takeOffValued);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuelInventory.deserializeNBT(registries, tag.getCompound("FuelInventory"));
        loadGearInventory(tag.getCompound("GearInventory"), registries);
        pumpStats = null;
        updateTakeOffCapacity();
        methaneTank.readFromNBT(registries, tag.getCompound("MethaneTank"));
        exhaustTank.readFromNBT(registries, tag.getCompound("ExhaustTank"));
        methaneCarry = Math.max(0.0, tag.getDouble("MethaneCarry"));
        exhaustCarry = Math.max(0.0, tag.getDouble("ExhaustCarry"));
        takeOffValued = tag.getBoolean("TakeOffValued");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        retentionDays = tag.contains("RetentionDays")
                ? BiogasDigestion.clampRetention(tag.getInt("RetentionDays"))
                : BiogasDigestion.RATED_RETENTION_DAYS;
        if (tag.contains("FeedRemaining")) {
            feedTotal = Math.max(0.0, tag.getDouble("FeedTotal"));
            feedRemaining = Math.max(0.0, tag.getDouble("FeedRemaining"));
        } else if (tag.getInt("BurnTime") > 0) {
            // Saves from before the digester: the burning item's unburned FE becomes undigested feed.
            feedTotal = Math.max(0.0, tag.getDouble("ActiveTotalEnergy"));
            feedRemaining = Math.max(0.0, tag.getDouble("RemainingEnergy"));
        } else {
            clearFeed();
        }
        gas = Mth.clamp(tag.getDouble("Gas"), 0.0, BiogasDigestion.HOLDER_CAPACITY);
        engineRunning = tag.getBoolean("EngineRunning") && gas > EPSILON;
        generationCarry = tag.getDouble("GenerationCarry");
        if (feedRemaining <= EPSILON) {
            clearFeed();
        }
        clampInternalEnergy();
    }

    /** Digests the current feed into the gas holder at the retention's pace, loading the next item when it is spent. */
    private boolean tickDigester() {
        lastDigestion = 0.0;
        MachineStatAccumulator stats = effectiveStats();
        if (!isDigesting() && !tryLoadFeed(stats)) {
            return false;
        }
        double space = BiogasDigestion.HOLDER_CAPACITY - gas;
        if (space <= EPSILON) {
            return false;
        }
        double yield = BiogasDigestion.gasYield(retentionDays);
        double produced = Math.min(Math.min(engineRate(stats), feedRemaining * yield), space);
        gas += produced;
        feedRemaining = Math.max(0.0, feedRemaining - produced / yield);
        lastDigestion = produced;
        if (feedRemaining <= EPSILON) {
            clearFeed();
        }
        return produced > 0.0;
    }

    /**
     * Burns biogas into FE. The engine starts once the holder reaches its start pressure, or on whatever gas is left
     * when no more feed is coming, and runs until the holder is empty. It idles while the FE buffer is full.
     */
    private boolean tickEngine() {
        lastGeneration = 0.0;
        MachineStatAccumulator stats = effectiveStats();
        int room = energyCapacity(stats) - energyStored();
        if (!engineRunning) {
            boolean lastGas = gas > EPSILON && !moreFeedComing();
            if (room <= 0 || (gas < BiogasDigestion.ENGINE_START_GAS && !lastGas)) {
                return false;
            }
            engineRunning = true;
        }
        double limit = engineRate(stats);
        if (gas < BiogasDigestion.ENGINE_START_GAS && moreFeedComing()) {
            // Below start pressure with feed still coming, burn only what the digester makes. Otherwise the short last
            // tick of each item drains the holder a little, until the engine stalls and has to prime again.
            limit = Math.min(limit, lastDigestion);
        }
        double burn = Math.min(Math.min(limit, gas), room - generationCarry);
        if (burn <= EPSILON) {
            return false;
        }
        gas -= burn;
        generationCarry += burn;
        int whole = (int) Math.floor(generationCarry + EPSILON);
        if (whole > 0) {
            generationCarry = Math.max(0.0, generationCarry - storeGeneratedEnergy(whole, stats));
        }
        lastGeneration = burn;
        if (gas <= EPSILON) {
            gas = 0.0;
            engineRunning = false;
        }
        return true;
    }

    /**
     * Separates the holder's biogas instead of burning it. Each FE of gas holds a fixed amount of Methane and CO2; the
     * recovered Methane fills its tank, and the slip and any CO2 the membrane does not capture are vented. Gas waits in
     * the holder while the Methane tank is full, so no fuel is lost.
     */
    private boolean tickTakeOff() {
        lastGeneration = 0.0;
        lastMethane = 0.0;
        lastVented = 0.0;
        engineRunning = false;
        double methanePerFe = BiogasDigestion.METHANE_PER_FE * methaneRecovery();
        double room = methaneTank.getSpace() - methaneCarry;
        double separated = Math.min(gas, room / methanePerFe);
        if (separated <= EPSILON) {
            return false;
        }
        gas = Math.max(0.0, gas - separated);
        lastMethane = separated * methanePerFe;
        methaneCarry = fillWhole(methaneTank, ModFluids.METHANE_SOURCE.get(), methaneCarry + lastMethane);

        double carbon = separated * BiogasDigestion.CARBON_EXHAUST_PER_FE;
        double captured = capturesCarbon() ? Math.max(0.0, Math.min(carbon, exhaustTank.getSpace() - exhaustCarry)) : 0.0;
        if (captured > 0.0) {
            exhaustCarry = fillWhole(exhaustTank, ModFluids.CARBON_EXHAUST_SOURCE.get(), exhaustCarry + captured);
        }
        lastVented = carbon - captured;
        return true;
    }

    /** Fills the whole millibuckets of {@code amount} and returns the fraction left over. */
    private static double fillWhole(FluidTank tank, Fluid fluid, double amount) {
        int whole = (int) Math.floor(amount + EPSILON);
        if (whole <= 0) {
            return amount;
        }
        int filled = tank.fill(new FluidStack(fluid, whole), IFluidHandler.FluidAction.EXECUTE);
        return Math.max(0.0, amount - filled);
    }

    private FluidTank takeOffTank(Fluid fluid) {
        return new FluidTank(BiogasDigestion.TAKE_OFF_TANK_CAPACITY, stack -> stack.is(fluid)) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    /** Pump Fluid Capacity rolls enlarge both output tanks; fluid above a smaller capacity stays until it drains. */
    private void updateTakeOffCapacity() {
        int capacity = Math.max(BiogasDigestion.TAKE_OFF_TANK_CAPACITY, (int) Math.round(pumpStats().value(MachineStat.FLUID_CAPACITY)));
        if (methaneTank.getCapacity() != capacity) {
            methaneTank.setCapacity(capacity);
            exhaustTank.setCapacity(capacity);
        }
    }

    /** The installed pumps' own fluid stats, added together; they never change the generator's stats. */
    private MachineStatAccumulator pumpStats() {
        if (pumpStats == null) {
            MachineStatAccumulator stats = MachineStatAccumulator.componentBase(
                    Map.of(MachineStat.FLUID_CAPACITY, (double) BiogasDigestion.TAKE_OFF_TANK_CAPACITY)
            );
            for (int slot = SLOT_GAS_LINE_A; slot <= SLOT_GAS_LINE_B; slot++) {
                ItemStack stack = gearInventory.getStackInSlot(slot);
                if (isFluidPump(stack)) {
                    ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
                }
            }
            pumpStats = stats;
        }
        return pumpStats;
    }

    /** mB a pipe pulls per operation: the pumps' transfer, or unlimited for fluid left after the pumps came out. */
    public int fluidTransfer() {
        return takeOffMode() ? Math.max(1, pumpStats().intValue(MachineStat.FLUID_TRANSFER)) : Integer.MAX_VALUE;
    }

    /** Older saves have a two-slot Gear inventory without the gas-line slots. */
    private void loadGearInventory(CompoundTag gearTag, HolderLookup.Provider registries) {
        int savedSize = gearTag.getInt("Size");
        if (savedSize == GEAR_SLOT_COUNT || !gearTag.contains("Size")) {
            gearInventory.deserializeNBT(registries, gearTag);
            return;
        }
        ItemStackHandler legacyGear = new ItemStackHandler(Math.max(0, savedSize));
        legacyGear.deserializeNBT(registries, gearTag);
        gearInventory.setSize(GEAR_SLOT_COUNT);
        for (int slot = 0; slot < Math.min(legacyGear.getSlots(), GEAR_SLOT_COUNT); slot++) {
            gearInventory.setStackInSlot(slot, legacyGear.getStackInSlot(slot));
        }
    }

    private boolean tryLoadFeed(MachineStatAccumulator stats) {
        if (gas >= BiogasDigestion.HOLDER_CAPACITY - EPSILON) {
            return false;
        }
        ItemStack fuel = fuelInventory.getStackInSlot(SLOT_FUEL);
        if (!isAcceptedFuel(fuel)) {
            return false;
        }
        double energy = feedEnergy(fuel, stats);
        if (energy <= 0.0) {
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

        feedTotal = energy;
        feedRemaining = energy;
        setChanged();
        return true;
    }

    /** FE an item gives at rated retention: its digestion ticks at the rated generation. */
    private double ratedFuelEnergy(ItemStack fuel, MachineStatAccumulator stats) {
        return effectiveFuelDurationTicks(fuel, stats) * (double) effectiveGenerationRate(stats);
    }

    /**
     * Gas an item gives at rated retention. The engine values it like the old burner, Energy Generation included. The
     * take-off values it at the base generation, so Energy Generation only digests faster, as on the Solid Fuel Burner,
     * and Methane per item depends on the fuel alone.
     */
    private double feedEnergy(ItemStack fuel, MachineStatAccumulator stats) {
        return takeOffMode() ? effectiveFuelDurationTicks(fuel, stats) * (double) BASE_GENERATION_RATE : ratedFuelEnergy(fuel, stats);
    }

    /**
     * Rescales the undigested feed and the holder gas when a pump goes in or comes out, so gas digested for the engine
     * cannot be sent to the take-off at the engine's higher value. Gas above the holder's capacity goes back to the feed.
     */
    private void revalueForMode() {
        boolean takeOff = takeOffMode();
        if (takeOff == takeOffValued) {
            return;
        }
        takeOffValued = takeOff;
        double toTakeOff = BASE_GENERATION_RATE / (double) effectiveGenerationRate(effectiveStats());
        double scale = takeOff ? toTakeOff : 1.0 / toTakeOff;
        feedTotal *= scale;
        feedRemaining *= scale;
        gas *= scale;
        if (gas > BiogasDigestion.HOLDER_CAPACITY) {
            double excess = (gas - BiogasDigestion.HOLDER_CAPACITY) / BiogasDigestion.gasYield(retentionDays);
            gas = BiogasDigestion.HOLDER_CAPACITY;
            feedTotal += excess;
            feedRemaining += excess;
        }
        setChanged();
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
        return Math.max(1, Mth.ceil(ratedGeneration(stats)));
    }

    /** Generation at rated retention, before rounding. */
    private double ratedGeneration(MachineStatAccumulator stats) {
        double multiplier = Math.max(0.1, stats.effectiveEnergyGenerationMultiplier());
        double flatBonus = Math.max(0.0, stats.effectiveFlatEnergyGenerationBonus());
        return RNGTechConfig.generatedEnergy(BASE_GENERATION_RATE * multiplier + flatBonus, refinementComponentStage());
    }

    /** Digestion and engine FE/t at the selected retention. */
    private int engineRate(MachineStatAccumulator stats) {
        return BiogasDigestion.engineRate(ratedGeneration(stats), retentionDays);
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

    /** How many times an energy export looked a neighbour up instead of reusing its cached capability. */
    public int exportTargetLookups() {
        return exportTargets.lookups();
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        MachineStatAccumulator stats = effectiveStats();
        if (energyStored() <= 0) {
            return false;
        }

        int remainingOutput = energyStored();
        boolean exported = false;
        for (Direction direction : Direction.values()) {
            if (remainingOutput <= 0 || energyStored() <= 0) {
                return exported;
            }

            IEnergyStorage target = exportTargets.get(level, pos, direction);
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
        return lastGeneration <= EPSILON ? 0 : (int) Math.ceil(lastGeneration - EPSILON);
    }

    /** FE the current item, or the next one, gives at the selected retention. */
    private int currentRecipeEnergy(MachineStatAccumulator stats) {
        return (int) Math.round(currentFeedEnergy(stats) * BiogasDigestion.gasYield(retentionDays));
    }

    /** Rated FE of the item digesting, or of the next item when the digester is empty. */
    private double currentFeedEnergy(MachineStatAccumulator stats) {
        if (isDigesting()) {
            return feedTotal;
        }
        ItemStack fuel = fuelInventory.getStackInSlot(SLOT_FUEL);
        return isAcceptedFuel(fuel) ? feedEnergy(fuel, stats) : 0.0;
    }

    private int remainingDigestionTicks(MachineStatAccumulator stats) {
        return isDigesting() ? digestionTicks(feedRemaining, stats) : 0;
    }

    private int totalDigestionTicks(MachineStatAccumulator stats) {
        return isDigesting() ? digestionTicks(feedTotal, stats) : 0;
    }

    private int digestionTicks(double feed, MachineStatAccumulator stats) {
        return Math.max(0, Mth.ceil(feed * BiogasDigestion.gasYield(retentionDays) / engineRate(stats) - EPSILON));
    }

    private int statusCode(MachineStatAccumulator stats) {
        if (takeOffMode()) {
            return takeOffStatus();
        }
        if (energyStored() >= energyCapacity(stats)) {
            return STATUS_ENERGY_FULL;
        }
        if (engineRunning) {
            return gasLineCount(GAS_LINE_MEMBRANE) > 0 ? STATUS_NEEDS_PUMP : STATUS_READY;
        }
        if (isDigesting() || gas > EPSILON) {
            return STATUS_PRIMING;
        }
        ItemStack fuel = fuelInventory.getStackInSlot(SLOT_FUEL);
        if (fuel.isEmpty()) {
            return STATUS_NO_FUEL;
        }
        return isAcceptedFuel(fuel) ? STATUS_READY : STATUS_INVALID_FUEL;
    }

    private int takeOffStatus() {
        if (lastMethane > EPSILON) {
            return STATUS_TAKE_OFF;
        }
        if (gas > EPSILON && methaneTank.getSpace() <= 0) {
            return STATUS_METHANE_FULL;
        }
        if (isDigesting() || gas > EPSILON) {
            return STATUS_TAKE_OFF;
        }
        ItemStack fuel = fuelInventory.getStackInSlot(SLOT_FUEL);
        if (fuel.isEmpty()) {
            return STATUS_NO_FUEL;
        }
        return isAcceptedFuel(fuel) ? STATUS_TAKE_OFF : STATUS_INVALID_FUEL;
    }

    private boolean moreFeedComing() {
        return isDigesting() || isAcceptedFuel(fuelInventory.getStackInSlot(SLOT_FUEL));
    }

    private boolean isDigesting() {
        return feedRemaining > EPSILON;
    }

    private boolean isWorking() {
        return isDigesting() || lastGeneration > EPSILON || lastMethane > EPSILON;
    }

    private void clearFeed() {
        feedTotal = 0.0;
        feedRemaining = 0.0;
    }

    private int energyStored() {
        return internalEnergyStored(effectiveStats()) + batteryCell.energyStored();
    }

    private int energyCapacity(MachineStatAccumulator stats) {
        return internalEnergyCapacity(stats) + batteryCell.capacity();
    }

    private int internalEnergyCapacity(MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.round(stats.value(MachineStat.ENERGY_CAPACITY)));
    }

    private int internalEnergyStored(MachineStatAccumulator stats) {
        return Mth.clamp(internalEnergy, 0, internalEnergyCapacity(stats));
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        return batteryCell.storage();
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

    private final class TakeOffFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            for (FluidTank tank : List.of(methaneTank, exhaustTank)) {
                if (FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) {
                    return tank.drain(Math.min(resource.getAmount(), fluidTransfer()), action);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidTank tank = methaneTank.isEmpty() ? exhaustTank : methaneTank;
            return tank.drain(Math.min(maxDrain, fluidTransfer()), action);
        }

        private FluidTank tank(int tank) {
            return tank == 0 ? methaneTank : exhaustTank;
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
