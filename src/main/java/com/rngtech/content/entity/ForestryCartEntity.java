package com.rngtech.content.entity;

import com.rngtech.content.blockentity.ForestryCartStationBlockEntity;
import com.rngtech.content.blockentity.LedgerNbt;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.ConfiguratorItem;
import com.rngtech.content.item.FluidPumpItem;
import com.rngtech.content.item.ForestryCartItem;
import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.item.PruningShearsItem;
import com.rngtech.content.menu.ForestryCartMenu;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModEntityTypes;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModSounds;
import com.rngtech.content.registry.ModTags;
import com.rngtech.content.tool.ToolBaseStatCatalog;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.OutputLedger;
import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.AscendancyFormulas;
import com.rngtech.rpg.progression.ForestryCompanionPassiveTree;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ticket.AABBTicket;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class ForestryCartEntity extends AbstractMinecart implements MenuProvider, MachineMasteryHost {
    public static final int SAPLING_SLOT_START = 0;
    public static final int SAPLING_SLOT_COUNT = 2;
    public static final int OUTPUT_SLOT_START = SAPLING_SLOT_START + SAPLING_SLOT_COUNT;
    public static final int OUTPUT_SLOT_COUNT = 6;
    public static final int CARGO_SLOT_COUNT = OUTPUT_SLOT_START + OUTPUT_SLOT_COUNT;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_TOOL = 1;
    public static final int SLOT_SHEARS = 2;
    public static final int SLOT_PUMP = 3;
    public static final int GEAR_SLOT_COUNT = SLOT_PUMP + 1;
    public static final int WATER_CAPACITY = 8000;
    public static final int GEAR_CONDITION_MISSING = 0;
    public static final int GEAR_CONDITION_INVALID = 1;
    public static final int GEAR_CONDITION_BROKEN = 2;
    public static final int GEAR_CONDITION_READY = 3;

    private static final String TAG_MACHINE_TRAITS = "MachineTraits";
    private static final String TAG_MACHINE_PROGRESSION = "MachineProgression";
    private static final String TAG_MANUAL_SPEED_ENABLED = "ManualSpeedEnabled";
    private static final int AXE_CONNECTED_LOG_LIMIT = 8;
    private static final int BASE_MAX_TREE_HEIGHT = 16;
    private static final int CUT_BASE_FE = 80;
    private static final int MOVEMENT_FE = 1;
    private static final int SCAN_FE = 2;
    private static final int PLANT_FE = 40;
    private static final int PULSE_FE = 30;
    private static final int PULSE_INTERVAL_TICKS = 100;
    private static final int PULSE_STALLED_INTERVAL_TICKS = 1200;
    private static final int GIANT_TREE_SAPLINGS = 4;
    private static final double DOCK_SPRINT_PERCENT = 50.0;
    private static final double LOG_LEDGER_LEAF_SLOWDOWN = 0.25;
    private static final double GROWTH_PULSE_MOVEMENT_SURCHARGE = 0.25;
    private static final double MAX_CART_SPEED = 0.34D;
    public static final int FERTILIZER_CAPACITY = 256;
    private static final int UNSHEARED_LEAF_ENERGY_MULTIPLIER = 12;
    private static final int PLANT_MACHINE_XP = 4;
    private static final int LOG_HARVEST_MACHINE_XP = 8;
    private static final int LEAF_HARVEST_MACHINE_XP = 1;
    private static final int MANUAL_SPEED_FE_MULTIPLIER = 4;
    private static final int VANILLA_CART_SHEARS_LEAF_BUDGET = 8;
    private static final int SHEAR_WORK_COOLDOWN = 1;
    private static final int LOG_SETTLE_TICKS = 4;
    private static final int CROP_HARVEST_FE = 20;
    private static final int CROP_HARVEST_MACHINE_XP = 2;
    private static final int SPRINKLER_WATER_MB = 50;
    /** Sprinkled farmland counts as near water this long, like a block beside a water source; Irrigation adds half. */
    private static final int HYDRATION_TICKS = 1200;
    private static final double IRRIGATION_HYDRATION_MULTIPLIER = 1.5;
    private static final int HYDRATION_CHECK_INTERVAL_TICKS = 20;
    /** Watered cells still get a spray as the cart passes, without spending water. */
    private static final int COSMETIC_SPRAY_INTERVAL_TICKS = 10;
    private static final int BREAK_SOUND_INTERVAL_TICKS = 4;
    private static final int MAX_CRACK_OVERLAYS = 64;
    private static final float REFERENCE_LOG_HARDNESS = 2.0F;
    private static final int BASE_MAX_MANAGED_CELLS = 96;
    private static final int SCAN_BOUND_HORIZONTAL = 8;
    private static final int STATION_SAPLING_IDLE_RELEASE_TICKS = 20;
    private static final int MOVE_SOUND_INTERVAL_TICKS = 18;
    private static final int TRANSFER_SEEKING_MOVE_SOUND_INTERVAL_TICKS = 12;
    private static final int SCAN_SOUND_COOLDOWN_TICKS = 12;
    private static final int WORK_SOUND_COOLDOWN_TICKS = 4;
    private static final int TREEFELLER_SOUND_COOLDOWN_TICKS = 10;
    private static final int TRANSFER_SOUND_COOLDOWN_TICKS = 16;
    private static final int ALERT_SOUND_COOLDOWN_TICKS = 32;
    private static final int LEAF_SCAN_RADIUS = 4;
    private static final int MAX_HARVEST_SNAPSHOT_LOGS = 512;
    private static final int MAX_HARVEST_SNAPSHOT_HEIGHT = 64;
    private static final int MAX_CONNECTED_LEAF_BLOCKS = 192;
    private static final double MAGNET_RADIUS = 3.0D;
    private static final double POWERED_CART_SPEED = 0.055D;
    private static final double TRANSFER_SEEKING_CART_SPEED = 0.08D;
    private static final double UNPOWERED_CART_SPEED = 0.03D;
    private static final double MANUAL_POWERED_CART_SPEED = 0.095D;
    private static final double MANUAL_TRANSFER_SEEKING_CART_SPEED = 0.12D;
    private static final double COASTING_CART_SPEED = 0.06D;
    private static final EntityDataAccessor<Integer> DATA_ROUTE_DIRECTION =
            SynchedEntityData.defineId(ForestryCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_STATUS =
            SynchedEntityData.defineId(ForestryCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VISUAL_ACTION =
            SynchedEntityData.defineId(ForestryCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SCAN_DEBUG_VISIBLE =
            SynchedEntityData.defineId(ForestryCartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DEBUG_MAX_CONNECTED_LOGS =
            SynchedEntityData.defineId(ForestryCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DEBUG_MAX_TREE_HEIGHT =
            SynchedEntityData.defineId(ForestryCartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DEBUG_CAN_HARVEST_LEAVES =
            SynchedEntityData.defineId(ForestryCartEntity.class, EntityDataSerializers.BOOLEAN);

    private final ItemStackHandler cargoInventory = new ItemStackHandler(CARGO_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= SAPLING_SLOT_START && slot < OUTPUT_SLOT_START) {
                return isPlantableStack(stack) || isFertilizerStack(stack) && acceptsFertilizer();
            }
            return slot >= OUTPUT_SLOT_START && slot < CARGO_SLOT_COUNT;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= SAPLING_SLOT_START && slot < OUTPUT_SLOT_START && isFertilizerStack(stack)) {
                return stack.copyWithCount(stack.getCount() - storeFertilizer(stack.getCount(), simulate));
            }
            return super.insertItem(slot, stack, simulate);
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_TOOL -> isToolCandidate(stack);
                case SLOT_SHEARS -> isShearsCandidate(stack);
                case SLOT_PUMP -> isPumpCandidate(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
    };
    private final FluidTank waterTank = new FluidTank(WATER_CAPACITY, stack -> stack.getFluid().isSame(Fluids.WATER));
    private final Map<BlockPos, ManagedCell> managedCells = new LinkedHashMap<>();
    private final Map<BlockPos, Long> hydratedUntil = new HashMap<>();
    private final Map<BlockPos, AABBTicket> hydrationTickets = new HashMap<>();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= ForestryCartMenu.DATA_MACHINE_PROGRESSION_START
                    && index < ForestryCartMenu.DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT) {
                return MasteryMenuSupport.get(ForestryCartEntity.this, index - ForestryCartMenu.DATA_MACHINE_PROGRESSION_START, ForestryCartEntity.this::effectiveStats);
            }
            return switch (index) {
                case ForestryCartMenu.DATA_STATUS -> status;
                case ForestryCartMenu.DATA_CURRENT_ACTION -> currentAction;
                case ForestryCartMenu.DATA_MANAGED_CELLS -> managedCells.size();
                case ForestryCartMenu.DATA_ACTIVE_CELLS -> activeManagedCells();
                case ForestryCartMenu.DATA_MAX_CONNECTED_LOGS -> maxSnapshotLogs();
                case ForestryCartMenu.DATA_MAX_TREE_HEIGHT -> maxTreeHeight();
                case ForestryCartMenu.DATA_MOVEMENT_FE -> movementEnergyCost();
                case ForestryCartMenu.DATA_SCAN_FE -> scanEnergyCost();
                case ForestryCartMenu.DATA_PLANT_FE -> plantEnergyCost();
                case ForestryCartMenu.DATA_CUT_FE -> cutActionEnergyCost();
                case ForestryCartMenu.DATA_WORK_COOLDOWN -> workCooldown;
                case ForestryCartMenu.DATA_CART_ENERGY -> energyStored();
                case ForestryCartMenu.DATA_CART_ENERGY_CAPACITY -> energyCapacity();
                case ForestryCartMenu.DATA_SCAN_DEBUG_VISIBLE -> scanDebugVisible() ? 1 : 0;
                case ForestryCartMenu.DATA_WORKFLOW_STATE -> workflowState.ordinal();
                case ForestryCartMenu.DATA_MAX_MANAGED_CELLS -> maxManagedCells();
                case ForestryCartMenu.DATA_LOGS_PER_ACTION -> maxLogsPerAction();
                case ForestryCartMenu.DATA_SAPLING_CARGO -> saplingCargoCount();
                case ForestryCartMenu.DATA_SAPLING_CARGO_CAPACITY -> saplingCargoCapacity();
                case ForestryCartMenu.DATA_OUTPUT_CARGO -> outputCargoCount();
                case ForestryCartMenu.DATA_OUTPUT_CARGO_CAPACITY -> outputCargoCapacity();
                case ForestryCartMenu.DATA_TOOL_CONDITION -> toolCondition();
                case ForestryCartMenu.DATA_TOOL_DURABILITY -> toolDurabilityRemaining();
                case ForestryCartMenu.DATA_TOOL_MAX_DURABILITY -> toolMaxDurability();
                case ForestryCartMenu.DATA_LEAF_COLLECTION_ENABLED -> leafCollectionEnabled() ? 1 : 0;
                case ForestryCartMenu.DATA_SHEARS_CONDITION -> shearsCondition();
                case ForestryCartMenu.DATA_SHEARS_DURABILITY -> shearsDurabilityRemaining();
                case ForestryCartMenu.DATA_SHEARS_MAX_DURABILITY -> shearsMaxDurability();
                case ForestryCartMenu.DATA_SHEARS_LEAF_BUDGET -> shearsLeafBudget();
                case ForestryCartMenu.DATA_WORK_INTERVAL -> workIntervalTicks();
                case ForestryCartMenu.DATA_SHEAR_INTERVAL -> shearIntervalTicks();
                case ForestryCartMenu.DATA_POWERED_SPEED_MILLI -> speedMilli(poweredCartSpeed());
                case ForestryCartMenu.DATA_TRANSFER_SEEKING_SPEED_MILLI -> speedMilli(transferSeekingCartSpeed());
                case ForestryCartMenu.DATA_UNPOWERED_SPEED_MILLI -> speedMilli(unpoweredCartSpeed());
                case ForestryCartMenu.DATA_UNSHEARED_LEAF_FE -> unshearedLeafEnergyCost();
                case ForestryCartMenu.DATA_MANUAL_SPEED_ENABLED -> manualSpeedEnabled() ? 1 : 0;
                case ForestryCartMenu.DATA_MANUAL_SPEED_UNLOCKED -> manualSpeedUnlocked() ? 1 : 0;
                case ForestryCartMenu.DATA_CORE_CONTROL -> coreStatPoints(MachineStat.CONTROL);
                case ForestryCartMenu.DATA_CORE_DRIVE -> coreStatPoints(MachineStat.DRIVE);
                case ForestryCartMenu.DATA_CORE_RESERVE -> coreStatPoints(MachineStat.RESERVE);
                case ForestryCartMenu.DATA_FERTILIZER -> fertilizer;
                case ForestryCartMenu.DATA_FERTILIZER_CAPACITY -> acceptsFertilizer() ? FERTILIZER_CAPACITY : 0;
                case ForestryCartMenu.DATA_WATER -> waterStored();
                case ForestryCartMenu.DATA_WATER_CAPACITY -> hasMasteryBehavior("SPRINKLER") ? waterTank.getCapacity() : 0;
                case ForestryCartMenu.DATA_WORK_RANGE -> workRange();
                case ForestryCartMenu.DATA_IDLE_SPEED_ACTIVE -> idleSpeedActive() ? 1 : 0;
                case ForestryCartMenu.DATA_RESERVED_PLANTABLES -> reservedPlantables();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return ForestryCartMenu.DATA_COUNT;
        }
    };

    private Direction routeDirection = Direction.NORTH;
    private MachineTraits machineTraits = MachineTraits.EMPTY;
    private MachineProgressionState machineProgression = MachineProgressionState.EMPTY;
    private boolean manualSpeedEnabled;
    private WorkflowState workflowState = WorkflowState.SETUP_BLOCKED;
    private HarvestSnapshot activeHarvestSnapshot;
    private ActiveBreak activeBreak;
    private UUID ownerUuid;
    private String ownerName = "";
    private int status = ForestryCartStationBlockEntity.STATUS_MISSING_BATTERY_CELL;
    private int currentAction = ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED;
    private int workCooldown;
    private int ticksSinceWork = AscendancyFormulas.IDLE_CART_TICKS;
    private int managementViewers;
    private BlockPos dockSaplingTransferStation;
    private int dockSaplingTransferIdleTicks;
    private int pendingEnergyRequirement;
    private int nextMoveSoundTick;
    private int nextScanSoundTick;
    private int nextWorkSoundTick;
    private int nextTransferSoundTick;
    private int nextAlertSoundTick;
    private final OutputLedger<Item> logLedger = new OutputLedger<>();
    private int fertilizer;
    private double leafDelayCarry;
    private double movementSurchargeCarry;
    private final Map<BlockPos, List<BlockPos>> rolledRails = new LinkedHashMap<>();

    public ForestryCartEntity(EntityType<? extends ForestryCartEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ForestryCartEntity(Level level, double x, double y, double z) {
        super(ModEntityTypes.FORESTRY_CART.get(), level, x, y, z);
    }

    public void setOwner(Player player) {
        ownerUuid = player.getUUID();
        ownerName = player.getGameProfile().getName();
    }

    public Direction routeDirection() {
        Direction synced = Direction.from2DDataValue(entityData.get(DATA_ROUTE_DIRECTION));
        return synced.getAxis().isHorizontal() ? synced : routeDirection;
    }

    public ItemStackHandler cargoInventory() {
        return cargoInventory;
    }

    public ItemStackHandler gearInventory() {
        return gearInventory;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public int statusCode() {
        return status;
    }

    public int visualStatusCode() {
        return entityData.get(DATA_VISUAL_STATUS);
    }

    public int visualActionCode() {
        return entityData.get(DATA_VISUAL_ACTION);
    }

    public int currentAction() {
        return currentAction;
    }

    public MachineTraits machineTraits() {
        return machineTraits;
    }

    public void setMachineTraits(MachineTraits machineTraits) {
        this.machineTraits = machineTraits == null ? MachineTraits.EMPTY : machineTraits;
    }

    public MachineProgressionState machineProgression() {
        return machineProgression.forFamily(masteryFamily());
    }

    public void setMachineProgression(MachineProgressionState machineProgression) {
        this.machineProgression = (machineProgression == null ? MachineProgressionState.EMPTY : machineProgression).forFamily(masteryFamily());
        if (!manualSpeedUnlocked()) {
            manualSpeedEnabled = false;
        }
    }

    public boolean unlockPassiveNode(MegaPassiveNode node) {
        return allocateMastery(node);
    }

    /** Work XP marks the cart as busy, which pauses Idle Cart Speed for the next 4 seconds. */
    private void addMachineXp(int amount) {
        ticksSinceWork = 0;
        if (amount > 0) {
            int band = Math.min(MachineProgressionState.MAX_LEVEL - 2, 12 + (int) Math.sqrt(Math.max(0, effectiveStats().value(MachineStat.TREE_FELL_LIMIT))) * 10);
            grantMasteryXp(MachineProgressionState.workXp(amount, band), MachineProgressionState.xpQuarters(machineProgression().level(), band));
        }
    }

    public MachineStatAccumulator effectiveStats() {
        return ForestryCartItem.effectiveStats(machineTraits, machineProgression);
    }

    private int coreStatPoints(MachineStat stat) {
        return Math.max(0, (int) Math.floor(effectiveStats().value(stat)));
    }

    public int movementEnergyCost() {
        int cost = effectiveStats().adjustedEnergyCost(MOVEMENT_FE);
        return manualSpeedEnabled() ? cost * MANUAL_SPEED_FE_MULTIPLIER : cost;
    }

    public int scanEnergyCost() {
        return effectiveStats().adjustedEnergyCost(SCAN_FE * workRange());
    }

    /** Rows worked on each side of the rail. */
    public int workRange() {
        return ForestryCartRules.workRange(effectiveStats().value(MachineStat.WORK_RANGE));
    }

    /** Verdant Surge and Rolling Harvest reach one row past the worked rows. */
    private int workReach() {
        return workRange() + 1;
    }

    public int plantEnergyCost() {
        return effectiveStats().adjustedEnergyCost(PLANT_FE);
    }

    public int workCooldown() {
        return workCooldown;
    }

    public int energyStored() {
        IEnergyStorage storage = batteryEnergyStorage();
        return storage == null ? 0 : storage.getEnergyStored();
    }

    public int energyCapacity() {
        IEnergyStorage storage = batteryEnergyStorage();
        return storage == null ? 0 : storage.getMaxEnergyStored();
    }

    public int maxManagedCells() {
        return BASE_MAX_MANAGED_CELLS + ForestryCompanionPassiveTree.managedCellBonus(machineProgression)
                + (MegaPassiveTree.has(machineProgression, "NO_INHERENT_ATTRIBUTES") ? 0 : coreStatPoints(MachineStat.RESERVE) / 4);
    }

    public boolean manualSpeedUnlocked() {
        return ForestryCompanionPassiveTree.enablesManualThrottle(machineProgression);
    }

    public boolean manualSpeedEnabled() {
        return manualSpeedEnabled && manualSpeedUnlocked();
    }

    public boolean toggleManualSpeedEnabled() {
        if (!manualSpeedUnlocked()) {
            manualSpeedEnabled = false;
            return false;
        }
        manualSpeedEnabled = !manualSpeedEnabled;
        return true;
    }

    public double poweredCartSpeed() {
        return cartSpeed(manualSpeedEnabled() ? MANUAL_POWERED_CART_SPEED : POWERED_CART_SPEED, 0.0);
    }

    public double transferSeekingCartSpeed() {
        double sprint = hasMasteryBehavior("DOCK_SPRINT") ? DOCK_SPRINT_PERCENT : 0.0;
        return cartSpeed(manualSpeedEnabled() ? MANUAL_TRANSFER_SEEKING_CART_SPEED : TRANSFER_SEEKING_CART_SPEED, sprint);
    }

    /** Whether Idle Cart Speed currently applies. */
    public boolean idleSpeedActive() {
        return AscendancyFormulas.idleCartSpeedPercent(effectiveStats(), ticksSinceWork) > 0.0;
    }

    private double cartSpeed(double baseSpeed, double extraPercent) {
        MachineStatAccumulator stats = effectiveStats();
        double percent = extraPercent + AscendancyFormulas.idleCartSpeedPercent(stats, ticksSinceWork);
        return Math.min(MAX_CART_SPEED, baseSpeed * Math.max(0.1, stats.valueWithIncreased(MachineStat.CART_SPEED, percent)));
    }

    public double unpoweredCartSpeed() {
        return ForestryCompanionPassiveTree.enablesCoastingClutch(machineProgression) ? COASTING_CART_SPEED : UNPOWERED_CART_SPEED;
    }

    public int receiveEnergy(int amount, boolean simulate) {
        IEnergyStorage storage = batteryEnergyStorage();
        if (storage == null || amount <= 0) {
            return 0;
        }
        return storage.receiveEnergy(amount, simulate);
    }

    public boolean consumeEnergy(int amount, boolean simulate) {
        if (amount <= 0) {
            return true;
        }
        IEnergyStorage storage = batteryEnergyStorage();
        if (storage == null || !storage.canExtract() || storage.getEnergyStored() < amount) {
            return false;
        }
        if (!simulate) {
            int remaining = amount;
            while (remaining > 0) {
                int extracted = storage.extractEnergy(remaining, false);
                if (extracted <= 0) {
                    return false;
                }
                remaining -= extracted;
            }
        }
        return true;
    }

    public void setRouteDirection(Direction routeDirection) {
        if (routeDirection.getAxis().isHorizontal()) {
            this.routeDirection = routeDirection;
            entityData.set(DATA_ROUTE_DIRECTION, routeDirection.get2DDataValue());
        }
    }

    public boolean scanDebugVisible() {
        return entityData.get(DATA_SCAN_DEBUG_VISIBLE);
    }

    public int debugMaxConnectedLogs() {
        return entityData.get(DATA_DEBUG_MAX_CONNECTED_LOGS);
    }

    public int debugMaxTreeHeight() {
        return entityData.get(DATA_DEBUG_MAX_TREE_HEIGHT);
    }

    public boolean debugCanHarvestLeaves() {
        return entityData.get(DATA_DEBUG_CAN_HARVEST_LEAVES);
    }

    public void toggleScanDebugVisible() {
        setScanDebugVisible(!scanDebugVisible());
    }

    public void setScanDebugVisible(boolean visible) {
        entityData.set(DATA_SCAN_DEBUG_VISIBLE, visible);
    }

    public List<BlockPos> plantingScanRoots(BlockPos railPos) {
        if (!isForestryWorkRail(railPos)) {
            return List.of();
        }
        return ForestryCartRules.scanRoots(railPos, scanDirection(railPos), workRange());
    }

    public static List<BlockPos> plantingScanRoots(BlockPos railPos, Direction routeDirection) {
        return ForestryCartRules.scanRoots(railPos, routeDirection, ForestryCartRules.MIN_WORK_RANGE);
    }

    public BlockPos railPosition() {
        BlockPos pos = blockPosition();
        if (isRail(pos)) {
            return pos;
        }
        BlockPos below = pos.below();
        return isRail(below) ? below : null;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            stopBeforeMinecartPhysics();
        }
        super.tick();
        if (!level().isClientSide) {
            updateScanDebugData();
        }
        if (level().isClientSide) {
            return;
        }

        if (ticksSinceWork < AscendancyFormulas.IDLE_CART_TICKS) {
            ticksSinceWork++;
        }
        tryMagnetNearbyItems();
        absorbSuppliedFertilizer();
        evictUnplantableSupply();
        if (tickCount % HYDRATION_CHECK_INTERVAL_TICKS == 0) {
            maintainHydration();
            updateWaterCapacity();
        }

        if (managementViewers > 0) {
            setWorkflowState(WorkflowState.MANAGED, ForestryCartStationBlockEntity.STATUS_READY, ForestryCartStationBlockEntity.ACTION_MANAGED);
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        BlockPos railPos = railPosition();
        if (railPos == null) {
            setWorkflowState(
                    WorkflowState.OFF_RAIL,
                    ForestryCartStationBlockEntity.STATUS_CART_OFF_RAIL,
                    ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED
            );
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        refreshServiceStatuses();
        ForestryCartStationBlockEntity station = ForestryCartStationBlockEntity.stationAtDock(level(), railPos);
        if (station != null && station.holdCartAtStation()) {
            setWorkflowState(
                    WorkflowState.DOCKED_HELD,
                    ForestryCartStationBlockEntity.STATUS_HOLDING_CART,
                    ForestryCartStationBlockEntity.ACTION_HOLDING
            );
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        int setupStatus = setupStatus();
        if (setupStatus != ForestryCartStationBlockEntity.STATUS_READY) {
            setWorkflowState(WorkflowState.SETUP_BLOCKED, setupStatus, ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED);
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        int transferStatus = transferStatus();
        if (station != null) {
            if (needsStationRecharge()) {
                setWorkflowState(
                        WorkflowState.DOCKED_TRANSFER,
                        ForestryCartStationBlockEntity.STATUS_READY,
                        ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER
                );
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            if (shouldWaitForStationSaplings(station, transferStatus, true)) {
                setWorkflowState(WorkflowState.DOCKED_TRANSFER, transferStatus, ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            if (isBlockingDockTransfer(transferStatus)) {
                setWorkflowState(WorkflowState.DOCKED_TRANSFER, transferStatus, ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
        } else {
            clearDockSaplingTransferWait();
        }

        if (transferStatus != ForestryCartStationBlockEntity.STATUS_READY) {
            setWorkflowState(WorkflowState.SEEKING_TRANSFER, transferStatus, transferAction(transferStatus));
            driveOnRails(railPos, true);
            return;
        }

        if (!isForestryWorkRail(railPos)) {
            setWorkflowState(WorkflowState.MOVING, ForestryCartStationBlockEntity.STATUS_READY, ForestryCartStationBlockEntity.ACTION_MOVING);
            driveOnRails(railPos, false);
            return;
        }

        rememberRolledRail(railPos);
        if (workCooldown > 0) {
            workCooldown--;
            setWorkflowState(
                    WorkflowState.WORK_COOLDOWN,
                    ForestryCartStationBlockEntity.STATUS_READY,
                    ForestryCartStationBlockEntity.ACTION_WAITING_COOLDOWN
            );
            if (rollingHarvest() && canRollPast(railPos)) {
                driveOnRails(railPos, false);
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        List<BlockPos> workRoots = workRoots(railPos);
        if (activeHarvestSnapshot != null && !worksTrees()) {
            clearActiveHarvestSnapshot();
        }
        if (activeHarvestSnapshot != null) {
            if (!harvestInReach(railPos, workRoots, activeHarvestSnapshot.logBase())) {
                clearActiveHarvestSnapshot();
                setWorkflowState(
                        WorkflowState.MOVING,
                        ForestryCartStationBlockEntity.STATUS_READY,
                        ForestryCartStationBlockEntity.ACTION_SNAPSHOT_OUT_OF_RANGE
                );
                driveOnRails(railPos, false);
                return;
            }
            WorkResult result = processActiveHarvestSnapshot();
            if (handleWorkResult(result, railPos)) {
                return;
            }
        }

        int scanCost = scanEnergyCost();
        if (!consumeEnergy(scanCost, true)) {
            markNoPower(scanCost);
            driveOnRails(railPos, true);
            return;
        }
        consumeEnergy(scanCost, false);
        playScanSound();
        pruneManagedCells(railPos);
        pulseSaplings(railPos, workRoots);
        sprinkle(railPos, workRoots);

        setWorkflowState(
                WorkflowState.SCANNING,
                ForestryCartStationBlockEntity.STATUS_READY,
                ForestryCartStationBlockEntity.ACTION_SCANNING_LOG_BASES
        );
        boolean treeTooLarge = false;
        for (BlockPos root : workRoots) {
            updateManagedSaplingState(root);
            if (tendsCrops() && isMatureCrop(root)) {
                if (handleWorkResult(harvestCrop(root, rolledRail(root, railPos)), railPos)) {
                    return;
                }
                continue;
            }
            if (!worksTrees() || !isLogBase(root)) {
                continue;
            }
            SnapshotCreationResult result = createHarvestSnapshot(root);
            if (result == SnapshotCreationResult.TOO_LARGE) {
                treeTooLarge = true;
                continue;
            }
            if (result == SnapshotCreationResult.CREATED) {
                if (handleWorkResult(processActiveHarvestSnapshot(), railPos)) {
                    return;
                }
            }
        }

        boolean plantingBlocked = false;
        for (BlockPos root : workRoots) {
            WorkResult result = tryPlant(root, rolledRail(root, railPos));
            if (result == WorkResult.BLOCKED_STOP) {
                plantingBlocked = true;
                continue;
            }
            if (handleWorkResult(result, railPos)) {
                return;
            }
        }

        if (plantingBlocked) {
            setWorkflowState(WorkflowState.MOVING, status, ForestryCartStationBlockEntity.ACTION_PLANTING_BLOCKED);
            driveOnRails(railPos, false);
            return;
        }

        if (treeTooLarge) {
            setWorkflowState(
                    WorkflowState.MOVING,
                    ForestryCartStationBlockEntity.STATUS_TREE_TOO_LARGE,
                    ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED
            );
            driveOnRails(railPos, false);
            return;
        }

        if (plantsSaplings() && !hasSaplings()) {
            setWorkflowState(WorkflowState.MOVING, ForestryCartStationBlockEntity.STATUS_NO_SAPLINGS, ForestryCartStationBlockEntity.ACTION_NO_SAPLINGS);
            driveOnRails(railPos, false);
            return;
        }

        setWorkflowState(WorkflowState.MOVING, ForestryCartStationBlockEntity.STATUS_READY, ForestryCartStationBlockEntity.ACTION_MOVING);
        driveOnRails(railPos, false);
    }

    private void tryMagnetNearbyItems() {
        if (!ForestryCompanionPassiveTree.enablesMagnetMode(machineProgression)) {
            return;
        }
        List<ItemEntity> items = level().getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(MAGNET_RADIUS),
                item -> item.isAlive() && !item.getItem().isEmpty()
        );
        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            ItemStack remaining = ForestryCompanionPassiveTree.routesMagnetSaplings(machineProgression)
                    ? routeCargo(cargoInventory, stack)
                    : insertIntoRange(cargoInventory, OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT, stack);
            if (remaining.getCount() >= stack.getCount()) {
                continue;
            }
            if (remaining.isEmpty()) {
                item.discard();
            } else {
                item.setItem(remaining);
            }
        }
    }

    private void stopBeforeMinecartPhysics() {
        if (managementViewers > 0) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        BlockPos railPos = railPosition();
        if (railPos == null) {
            return;
        }
        refreshServiceStatuses();
        if (setupStatus() != ForestryCartStationBlockEntity.STATUS_READY) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        ForestryCartStationBlockEntity station = ForestryCartStationBlockEntity.stationAtDock(level(), railPos);
        int transferStatus = transferStatus();
        if (station != null
                && (station.holdCartAtStation()
                        || needsStationRecharge()
                        || shouldWaitForStationSaplings(station, transferStatus, false)
                        || isBlockingDockTransfer(transferStatus))) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (station == null) {
            clearDockSaplingTransferWait();
        }
        alignVelocityToRail(railPos);
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.RIDEABLE;
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ConfiguratorItem
                && held.getOrDefault(ModDataComponents.MASTERY_CONFIGURATOR_MODE.get(), false)) {
            return ConfiguratorItem.useMastery(player, held, this);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this, buffer -> buffer.writeInt(getId()));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.forestry_cart");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        beginManagement();
        return new ForestryCartMenu(containerId, playerInventory, this, menuData);
    }

    public void closeManagementMenu() {
        managementViewers = Math.max(0, managementViewers - 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROUTE_DIRECTION, Direction.NORTH.get2DDataValue());
        builder.define(DATA_VISUAL_STATUS, ForestryCartStationBlockEntity.STATUS_MISSING_BATTERY_CELL);
        builder.define(DATA_VISUAL_ACTION, ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED);
        builder.define(DATA_SCAN_DEBUG_VISIBLE, false);
        builder.define(DATA_DEBUG_MAX_CONNECTED_LOGS, 0);
        builder.define(DATA_DEBUG_MAX_TREE_HEIGHT, BASE_MAX_TREE_HEIGHT);
        builder.define(DATA_DEBUG_CAN_HARVEST_LEAVES, true);
    }

    @Override
    public ItemStack getPickResult() {
        return cartItemStack();
    }

    @Override
    protected Item getDropItem() {
        return ModItems.FORESTRY_CART.get();
    }

    private ItemStack cartItemStack() {
        ItemStack stack = new ItemStack(ModItems.FORESTRY_CART.get());
        if (!machineTraits.isEmpty()) {
            stack.set(ModDataComponents.MACHINE_TRAITS.get(), machineTraits);
        }
        if (!machineProgression.equals(MachineProgressionState.EMPTY)) {
            stack.set(ModDataComponents.MACHINE_PROGRESSION.get(), machineProgression);
        }
        if (hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, getCustomName());
        }
        return stack;
    }

    @Override
    public void remove(RemovalReason reason) {
        clearBreakProgress();
        hydrationTickets.values().forEach(AABBTicket::invalidate);
        hydrationTickets.clear();
        super.remove(reason);
    }

    @Override
    public void destroy(DamageSource source) {
        kill();
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        Containers.dropItemStack(level(), getX(), getY(), getZ(), cartItemStack());
        dropFertilizer();
        for (ManagedCell cell : managedCells.values()) {
            ItemStack reserve = reserveStack(cell);
            cell.setReserved(0);
            if (!reserve.isEmpty()) {
                Containers.dropItemStack(level(), getX(), getY(), getZ(), reserve);
            }
        }
        for (int slot = 0; slot < cargoInventory.getSlots(); slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level(), getX(), getY(), getZ(), stack.copy());
                cargoInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            ItemStack stack = gearInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level(), getX(), getY(), getZ(), stack.copy());
                gearInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RouteDirection", routeDirection().get2DDataValue());
        tag.putBoolean("ScanDebugVisible", scanDebugVisible());
        if (!machineTraits.isEmpty()) {
            MachineTraits.CODEC.encodeStart(NbtOps.INSTANCE, machineTraits)
                    .result()
                    .ifPresent(traitsTag -> tag.put(TAG_MACHINE_TRAITS, traitsTag));
        }
        if (!machineProgression.equals(MachineProgressionState.EMPTY)) {
            MachineProgressionState.CODEC.encodeStart(NbtOps.INSTANCE, machineProgression)
                    .result()
                    .ifPresent(progressionTag -> tag.put(TAG_MACHINE_PROGRESSION, progressionTag));
        }
        tag.put("CargoInventory", cargoInventory.serializeNBT(level().registryAccess()));
        tag.put("GearInventory", gearInventory.serializeNBT(level().registryAccess()));
        tag.putString("WorkflowState", workflowState.name());
        tag.putInt("Status", status);
        tag.putInt("CurrentAction", currentAction);
        tag.putInt("WorkCooldown", workCooldown);
        tag.putInt("TicksSinceWork", ticksSinceWork);
        tag.put("WaterTank", waterTank.writeToNBT(level().registryAccess(), new CompoundTag()));
        ListTag hydrated = new ListTag();
        hydratedUntil.forEach((pos, until) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", pos.getX());
            entry.putInt("Y", pos.getY());
            entry.putInt("Z", pos.getZ());
            entry.putLong("Until", until);
            hydrated.add(entry);
        });
        tag.put("Hydrated", hydrated);
        tag.putInt("PendingEnergyRequirement", pendingEnergyRequirement);
        tag.putBoolean(TAG_MANUAL_SPEED_ENABLED, manualSpeedEnabled());
        if (ownerUuid != null) {
            tag.putUUID("OwnerUuid", ownerUuid);
            tag.putString("OwnerName", ownerName == null ? "" : ownerName);
        }
        ListTag cells = new ListTag();
        for (ManagedCell cell : managedCells.values()) {
            cells.add(cell.save());
        }
        tag.put("ManagedCells", cells);
        tag.putInt("Fertilizer", fertilizer);
        LedgerNbt.save(tag, "LogLedger", logLedger);
        if (activeHarvestSnapshot != null) {
            tag.put("ActiveHarvestSnapshot", activeHarvestSnapshot.save());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRouteDirection(Direction.from2DDataValue(tag.getInt("RouteDirection")));
        setScanDebugVisible(tag.getBoolean("ScanDebugVisible"));
        machineTraits = tag.contains(TAG_MACHINE_TRAITS)
                ? MachineTraits.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_MACHINE_TRAITS)).result().orElse(MachineTraits.EMPTY)
                : MachineTraits.EMPTY;
        machineProgression = tag.contains(TAG_MACHINE_PROGRESSION)
                ? MachineProgressionState.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_MACHINE_PROGRESSION))
                        .result()
                        .orElse(MachineProgressionState.EMPTY)
                : MachineProgressionState.EMPTY;
        if (tag.contains("CargoInventory", Tag.TAG_COMPOUND)) {
            cargoInventory.deserializeNBT(level().registryAccess(), tag.getCompound("CargoInventory"));
        }
        if (tag.contains("GearInventory", Tag.TAG_COMPOUND)) {
            gearInventory.deserializeNBT(level().registryAccess(), expandedGearInventoryTag(tag.getCompound("GearInventory")));
        }
        workflowState = workflowState(tag.getString("WorkflowState"));
        setStatus(tag.contains("Status") ? tag.getInt("Status") : ForestryCartStationBlockEntity.STATUS_READY);
        setCurrentAction(tag.contains("CurrentAction") ? tag.getInt("CurrentAction") : ForestryCartStationBlockEntity.ACTION_IDLE);
        workCooldown = Math.max(0, tag.getInt("WorkCooldown"));
        ticksSinceWork = tag.contains("TicksSinceWork") ? Math.max(0, tag.getInt("TicksSinceWork")) : AscendancyFormulas.IDLE_CART_TICKS;
        waterTank.setFluid(FluidStack.EMPTY);
        updateWaterCapacity();
        if (tag.contains("WaterTank", Tag.TAG_COMPOUND)) {
            waterTank.readFromNBT(level().registryAccess(), tag.getCompound("WaterTank"));
        }
        hydratedUntil.clear();
        ListTag hydrated = tag.getList("Hydrated", Tag.TAG_COMPOUND);
        for (int index = 0; index < hydrated.size(); index++) {
            CompoundTag entry = hydrated.getCompound(index);
            hydratedUntil.put(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")), entry.getLong("Until"));
        }
        pendingEnergyRequirement = Math.max(0, tag.getInt("PendingEnergyRequirement"));
        manualSpeedEnabled = tag.getBoolean(TAG_MANUAL_SPEED_ENABLED) && manualSpeedUnlocked();
        if (status != ForestryCartStationBlockEntity.STATUS_NO_POWER) {
            pendingEnergyRequirement = 0;
        }
        ownerUuid = tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : null;
        ownerName = tag.getString("OwnerName");
        managedCells.clear();
        ListTag cells = tag.getList("ManagedCells", Tag.TAG_COMPOUND);
        for (int index = 0; index < cells.size(); index++) {
            ManagedCell cell = ManagedCell.load(cells.getCompound(index));
            managedCells.put(cell.root(), cell);
        }
        fertilizer = Math.max(0, Math.min(FERTILIZER_CAPACITY, tag.getInt("Fertilizer")));
        LedgerNbt.load(tag, "LogLedger", logLedger);
        activeHarvestSnapshot = tag.contains("ActiveHarvestSnapshot", Tag.TAG_COMPOUND)
                ? HarvestSnapshot.load(tag.getCompound("ActiveHarvestSnapshot"))
                : null;
    }

    public boolean hasSaplings() {
        return !saplingStack().isEmpty();
    }

    public ItemStack saplingStack() {
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START; slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            if (isPlantableStack(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack extractSapling() {
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START; slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            if (isPlantableStack(stack)) {
                return cargoInventory.extractItem(slot, 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack saplingStack(Predicate<ItemStack> filter) {
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START; slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            if (isPlantableStack(stack) && filter.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack extractSapling(Item item) {
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START; slot++) {
            if (cargoInventory.getStackInSlot(slot).is(item)) {
                return cargoInventory.extractItem(slot, 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    private int saplingCount(Item item) {
        int count = 0;
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START; slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            count += stack.is(item) ? stack.getCount() : 0;
        }
        return count;
    }

    public ItemStack insertSaplings(ItemStack stack) {
        return insertSaplings(stack, false);
    }

    public boolean canAcceptSaplings(ItemStack stack) {
        return plantsSaplings() && isPlantableStack(stack) && insertSaplings(stack, true).getCount() < stack.getCount();
    }

    public void recordStationSaplingTransfer(BlockPos stationPos) {
        dockSaplingTransferStation = stationPos.immutable();
        dockSaplingTransferIdleTicks = 0;
    }

    public boolean isWaitingForStationSaplings(BlockPos stationPos) {
        return dockSaplingTransferStation != null && dockSaplingTransferStation.equals(stationPos);
    }

    private ItemStack insertSaplings(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START && !remaining.isEmpty(); slot++) {
            remaining = cargoInventory.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    public int saplingCargoCount() {
        return cargoCount(SAPLING_SLOT_START, SAPLING_SLOT_COUNT);
    }

    public int saplingCargoCapacity() {
        return cargoCapacity(SAPLING_SLOT_START, SAPLING_SLOT_COUNT);
    }

    public boolean hasOutputCargo() {
        for (int slot = OUTPUT_SLOT_START; slot < CARGO_SLOT_COUNT; slot++) {
            if (!cargoInventory.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public int outputCargoCount() {
        return cargoCount(OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT);
    }

    public int outputCargoCapacity() {
        return cargoCapacity(OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT);
    }

    public boolean canAcceptOutputs(List<ItemStack> stacks) {
        ItemStackHandler copy = new ItemStackHandler(CARGO_SLOT_COUNT);
        for (int slot = 0; slot < CARGO_SLOT_COUNT; slot++) {
            copy.setStackInSlot(slot, cargoInventory.getStackInSlot(slot).copy());
        }
        for (ItemStack stack : stacks) {
            if (!routeCargo(copy, stack).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void insertOutputs(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack remaining = routeCargo(cargoInventory, stack);
            if (!remaining.isEmpty()) {
                Containers.dropItemStack(level(), getX(), getY(), getZ(), remaining);
            }
        }
    }

    public static boolean isSaplingStack(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof SaplingBlock;
    }

    /** Crop items a Field Hand cart plants: tagged {@code rngtech:cart_crops} and placing a crop block. */
    public static boolean isCropStack(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(ModTags.Items.CART_CROPS)
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof CropBlock;
    }

    /** What this cart's supply slots hold: crop items for a Field Hand cart, saplings for every other cart. */
    public boolean isPlantableStack(ItemStack stack) {
        return tendsCrops() ? isCropStack(stack) : isSaplingStack(stack);
    }

    public static boolean isPumpCandidate(ItemStack stack) {
        return stack.getItem() instanceof FluidPumpItem;
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public static boolean isToolCandidate(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularToolItem tool)) {
            return false;
        }
        return tool.family() == ToolHeadFamily.AXE || tool.family() == ToolHeadFamily.TREEFELLER;
    }

    public static boolean isShearsCandidate(ItemStack stack) {
        return !stack.isEmpty() && stack.canPerformAction(ItemAbilities.SHEARS_DIG);
    }

    public static boolean isUsableShearsCandidate(ItemStack stack) {
        return isShearsCandidate(stack) && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage());
    }

    public boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    public boolean hasUsableTool() {
        ItemStack tool = toolStack();
        return isToolCandidate(tool) && ModularToolItem.hasValidAssembly(tool) && !ModularToolItem.isBroken(tool);
    }

    public boolean hasTreefellerTool() {
        ItemStack tool = toolStack();
        return tool.getItem() instanceof ModularToolItem modularTool
                && modularTool.family() == ToolHeadFamily.TREEFELLER
                && ModularToolItem.hasValidAssembly(tool)
                && !ModularToolItem.isBroken(tool);
    }

    public boolean hasUsableShears() {
        return isUsableShearsCandidate(shearsStack());
    }

    public ItemStack toolStack() {
        return gearInventory.getStackInSlot(SLOT_TOOL);
    }

    public void setToolStack(ItemStack stack) {
        gearInventory.setStackInSlot(SLOT_TOOL, stack);
    }

    public ItemStack shearsStack() {
        return gearInventory.getStackInSlot(SLOT_SHEARS);
    }

    public void setShearsStack(ItemStack stack) {
        gearInventory.setStackInSlot(SLOT_SHEARS, stack);
    }

    public int maxConnectedLogs() {
        ItemStack tool = toolStack();
        if (!(tool.getItem() instanceof ModularToolItem modularTool) || !ModularToolItem.hasValidAssembly(tool)) {
            return 0;
        }
        int masteryBonus = effectiveStats().intValue(MachineStat.TREE_FELL_LIMIT);
        if (modularTool.family() == ToolHeadFamily.TREEFELLER) {
            return Math.max(1, ToolBaseStatCatalog.treeFellLimit(tool) + masteryBonus);
        }
        return AXE_CONNECTED_LOG_LIMIT + masteryBonus;
    }

    public int maxSnapshotLogs() {
        return maxConnectedLogs() > 0 ? MAX_HARVEST_SNAPSHOT_LOGS : 0;
    }

    public int maxLogsPerAction() {
        return hasTreefellerTool() ? maxConnectedLogs() : hasUsableTool() ? 1 : 0;
    }

    public int maxTreeHeight() {
        ItemStack tool = toolStack();
        int miningLevel = ModularToolItem.hasValidAssembly(tool) ? ToolBaseStatCatalog.miningLevel(tool) : 0;
        return Math.max(MAX_HARVEST_SNAPSHOT_HEIGHT, BASE_MAX_TREE_HEIGHT + Math.max(0, miningLevel) * 2);
    }

    public int cutEnergyCost() {
        return effectiveStats().adjustedEnergyCost(CUT_BASE_FE);
    }

    public int cutActionEnergyCost() {
        return hasTreefellerTool() ? cutEnergyCost() * maxConnectedLogs() : cutEnergyCost();
    }

    public int unshearedLeafEnergyCost() {
        return cutEnergyCost() * UNSHEARED_LEAF_ENERGY_MULTIPLIER;
    }

    private int leafHarvestEnergyCost(boolean leaves, boolean shearsReady) {
        if (leaves && !shearsReady && processesLeavesWithoutShears()) {
            return unshearedLeafEnergyCost();
        }
        return cutEnergyCost();
    }

    private boolean processesLeavesWithoutShears() {
        return ForestryCompanionPassiveTree.processesLeavesWithoutShears(machineProgression);
    }

    /** Ticks the installed tool takes to break an ordinary log; shown on the cart's Stats tab. */
    public int workIntervalTicks() {
        ItemStack tool = toolStack();
        float speed = ModularToolItem.hasValidAssembly(tool) ? ToolBaseStatCatalog.miningSpeed(tool) + efficiencyBonus(tool) : 1.0F;
        return ForestryCartRules.breakTicks(REFERENCE_LOG_HARDNESS, speed, effectiveStats().value(MachineStat.PROCESSING_SPEED));
    }

    public int plantIntervalTicks() {
        return ForestryCartRules.plantIntervalTicks(effectiveStats().value(MachineStat.PROCESSING_SPEED));
    }

    /** Ticks the installed tool takes to break this block, as a player would with the same tool and Efficiency. */
    private int breakTicks(BlockPos pos, BlockState state) {
        ItemStack tool = toolStack();
        float speed = tool.getDestroySpeed(state);
        if (speed > 1.0F) {
            speed += efficiencyBonus(tool);
        }
        return ForestryCartRules.breakTicks(state.getDestroySpeed(level(), pos), speed, effectiveStats().value(MachineStat.PROCESSING_SPEED));
    }

    private float efficiencyBonus(ItemStack tool) {
        int efficiency = EnchantmentHelper.getItemEnchantmentLevel(
                level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), tool);
        return efficiency > 0 ? efficiency * efficiency + 1 : 0.0F;
    }

    public int shearIntervalTicks() {
        return SHEAR_WORK_COOLDOWN;
    }

    public int toolCondition() {
        ItemStack tool = toolStack();
        if (tool.isEmpty()) {
            return GEAR_CONDITION_MISSING;
        }
        if (!isToolCandidate(tool) || !ModularToolItem.hasValidAssembly(tool)) {
            return GEAR_CONDITION_INVALID;
        }
        if (ModularToolItem.isBroken(tool)) {
            return GEAR_CONDITION_BROKEN;
        }
        return GEAR_CONDITION_READY;
    }

    public int toolDurabilityRemaining() {
        ItemStack tool = toolStack();
        if (!isToolCandidate(tool) || !ModularToolItem.hasValidAssembly(tool)) {
            return 0;
        }
        ModularToolItem.refreshMaxDamage(tool);
        return Math.max(0, tool.getMaxDamage() - tool.getDamageValue());
    }

    public int toolMaxDurability() {
        ItemStack tool = toolStack();
        if (!isToolCandidate(tool) || !ModularToolItem.hasValidAssembly(tool)) {
            return 0;
        }
        ModularToolItem.refreshMaxDamage(tool);
        return tool.getMaxDamage();
    }

    public boolean leafCollectionEnabled() {
        return hasUsableShears() || processesLeavesWithoutShears();
    }

    public int shearsCondition() {
        ItemStack shears = shearsStack();
        if (shears.isEmpty()) {
            return GEAR_CONDITION_MISSING;
        }
        if (!isShearsCandidate(shears)) {
            return GEAR_CONDITION_INVALID;
        }
        if (shears.isDamageableItem() && shears.getDamageValue() >= shears.getMaxDamage()) {
            return GEAR_CONDITION_BROKEN;
        }
        return GEAR_CONDITION_READY;
    }

    public int shearsDurabilityRemaining() {
        ItemStack shears = shearsStack();
        if (!isShearsCandidate(shears) || !shears.isDamageableItem()) {
            return 0;
        }
        return Math.max(0, shears.getMaxDamage() - shears.getDamageValue());
    }

    public int shearsMaxDurability() {
        ItemStack shears = shearsStack();
        if (!isShearsCandidate(shears) || !shears.isDamageableItem()) {
            return 0;
        }
        return shears.getMaxDamage();
    }

    public int shearsLeafBudget() {
        ItemStack shears = shearsStack();
        return isShearsCandidate(shears) ? cartShearsLeafBudget(shears) : 0;
    }

    private void beginManagement() {
        managementViewers++;
        setWorkflowState(WorkflowState.MANAGED, ForestryCartStationBlockEntity.STATUS_READY, ForestryCartStationBlockEntity.ACTION_MANAGED);
        setDeltaMovement(Vec3.ZERO);
    }

    private boolean handleWorkResult(WorkResult result, BlockPos railPos) {
        return switch (result) {
            case WORKED, BLOCKED_STOP -> {
                if (result == WorkResult.WORKED && rollingHarvest() && canRollPast(railPos)) {
                    driveOnRails(railPos, false);
                } else {
                    setDeltaMovement(Vec3.ZERO);
                }
                yield true;
            }
            case NEEDS_TRANSFER -> {
                ForestryCartStationBlockEntity station = ForestryCartStationBlockEntity.stationAtDock(level(), railPos);
                if (station != null) {
                    setWorkflowState(WorkflowState.DOCKED_TRANSFER, transferStatus(), ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER);
                    setDeltaMovement(Vec3.ZERO);
                } else {
                    setWorkflowState(WorkflowState.SEEKING_TRANSFER, transferStatus(), transferAction());
                    driveOnRails(railPos, true);
                }
                yield true;
            }
            case NO_WORK -> false;
        };
    }

    private void refreshServiceStatuses() {
        if (status == ForestryCartStationBlockEntity.STATUS_OUTPUT_FULL && !hasOutputCargo()) {
            setStatus(ForestryCartStationBlockEntity.STATUS_READY);
        }
        if (status == ForestryCartStationBlockEntity.STATUS_NO_SAPLINGS && hasSaplings()) {
            setStatus(ForestryCartStationBlockEntity.STATUS_READY);
        }
        if (status == ForestryCartStationBlockEntity.STATUS_NO_POWER && consumeEnergy(transferEnergyRequirement(), true)) {
            setStatus(ForestryCartStationBlockEntity.STATUS_READY);
        }
    }

    private int transferStatus() {
        if (status == ForestryCartStationBlockEntity.STATUS_OUTPUT_FULL || isOutputCargoFull()) {
            return ForestryCartStationBlockEntity.STATUS_OUTPUT_FULL;
        }
        if (status == ForestryCartStationBlockEntity.STATUS_NO_POWER || !consumeEnergy(transferEnergyRequirement(), true)) {
            return ForestryCartStationBlockEntity.STATUS_NO_POWER;
        }
        return ForestryCartStationBlockEntity.STATUS_READY;
    }

    private int transferEnergyRequirement() {
        return pendingEnergyRequirement > 0 ? pendingEnergyRequirement : scanEnergyCost();
    }

    private boolean needsStationRecharge() {
        int capacity = energyCapacity();
        return capacity > 0 && energyStored() < capacity;
    }

    private int transferAction() {
        return transferAction(transferStatus());
    }

    private int transferAction(int transferStatus) {
        if (transferStatus == ForestryCartStationBlockEntity.STATUS_OUTPUT_FULL) {
            return ForestryCartStationBlockEntity.ACTION_OUTPUT_FULL;
        }
        if (transferStatus == ForestryCartStationBlockEntity.STATUS_NO_POWER) {
            return ForestryCartStationBlockEntity.ACTION_NO_POWER;
        }
        if (transferStatus == ForestryCartStationBlockEntity.STATUS_NO_SAPLINGS) {
            return ForestryCartStationBlockEntity.ACTION_NO_SAPLINGS;
        }
        return ForestryCartStationBlockEntity.ACTION_SEEKING_TRANSFER;
    }

    private boolean isBlockingDockTransfer(int transferStatus) {
        return transferStatus != ForestryCartStationBlockEntity.STATUS_READY
                && transferStatus != ForestryCartStationBlockEntity.STATUS_NO_SAPLINGS;
    }

    private boolean shouldWaitForStationSaplings(
            ForestryCartStationBlockEntity station,
            int transferStatus,
            boolean advanceIdle
    ) {
        if (!plantsSaplings()) {
            clearDockSaplingTransferWait();
            return false;
        }
        boolean sameStation = dockSaplingTransferStation != null && dockSaplingTransferStation.equals(station.getBlockPos());
        boolean canLoadSaplings = !hasSaplings() && hasSaplingCargoSpace() && station.canLoadQueuedSaplingsInto(this);
        if (transferStatus != ForestryCartStationBlockEntity.STATUS_NO_SAPLINGS && !sameStation && !canLoadSaplings) {
            clearDockSaplingTransferWait();
            return false;
        }
        if (station.hasSaplingsQueued()) {
            if (!station.canLoadQueuedSaplingsInto(this)) {
                clearDockSaplingTransferWait();
                return false;
            }
            if (advanceIdle) {
                recordStationSaplingTransfer(station.getBlockPos());
            }
            return true;
        }
        if (!hasSaplingCargoSpace()) {
            clearDockSaplingTransferWait();
            return false;
        }
        if (!sameStation) {
            if (advanceIdle) {
                recordStationSaplingTransfer(station.getBlockPos());
            }
            return true;
        }
        if (dockSaplingTransferIdleTicks < STATION_SAPLING_IDLE_RELEASE_TICKS) {
            if (advanceIdle) {
                dockSaplingTransferIdleTicks++;
            }
            return true;
        }
        if (advanceIdle) {
            clearDockSaplingTransferWait();
        }
        return false;
    }

    private boolean hasSaplingCargoSpace() {
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START; slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxStackSize(), cargoInventory.getSlotLimit(slot))) {
                return true;
            }
        }
        return false;
    }

    private void clearDockSaplingTransferWait() {
        dockSaplingTransferStation = null;
        dockSaplingTransferIdleTicks = 0;
    }

    private int setupStatus() {
        if (!hasBatteryCell()) {
            return ForestryCartStationBlockEntity.STATUS_MISSING_BATTERY_CELL;
        }
        if (!worksTrees()) {
            return ForestryCartStationBlockEntity.STATUS_READY;
        }
        ItemStack tool = toolStack();
        if (tool.isEmpty()) {
            return ForestryCartStationBlockEntity.STATUS_MISSING_TOOL;
        }
        if (!isToolCandidate(tool) || !ModularToolItem.hasValidAssembly(tool)) {
            return ForestryCartStationBlockEntity.STATUS_INVALID_TOOL;
        }
        if (ModularToolItem.isBroken(tool)) {
            return ForestryCartStationBlockEntity.STATUS_BROKEN_TOOL;
        }
        return ForestryCartStationBlockEntity.STATUS_READY;
    }

    private SnapshotCreationResult createHarvestSnapshot(BlockPos logBase) {
        setWorkflowState(
                WorkflowState.CREATING_SNAPSHOT,
                ForestryCartStationBlockEntity.STATUS_READY,
                ForestryCartStationBlockEntity.ACTION_CREATING_SNAPSHOT
        );
        HarvestScan scan = scanHarvestTree(logBase, maxTreeHeight());
        if (scan.tooLarge()) {
            setCellState(managedCells.get(logBase), ManagedCell.STATE_BLOCKED);
            return SnapshotCreationResult.TOO_LARGE;
        }
        if (scan.logs().isEmpty()) {
            return SnapshotCreationResult.NO_TREE;
        }

        activeHarvestSnapshot = new HarvestSnapshot(
                logBase,
                harvestOrder(logBase, scan.leaves()),
                harvestOrder(logBase, scan.logs()),
                scan.logs()
        );
        setCellState(managedCell(logBase), ManagedCell.STATE_HARVESTING);
        setStatus(ForestryCartStationBlockEntity.STATUS_READY);
        return SnapshotCreationResult.CREATED;
    }

    private WorkResult processActiveHarvestSnapshot() {
        if (!(level() instanceof ServerLevel) || activeHarvestSnapshot == null) {
            return WorkResult.NO_WORK;
        }

        HarvestSnapshot snapshot = activeHarvestSnapshot;
        ManagedCell cell = managedCell(snapshot.logBase());
        pruneInvalidSnapshotTargets(snapshot);

        if (!snapshot.leaves().isEmpty()) {
            BlockPos harvestPos = snapshot.leaves().get(0);
            WorkResult result = harvestTreeBlock(cell, harvestPos);
            removeIfNoLonger(snapshot.leaves(), harvestPos, BlockTags.LEAVES);
            if (snapshot.isEmpty()) {
                clearActiveHarvestSnapshot();
            }
            return result;
        }

        if (refreshRemainingSnapshotLeaves(snapshot)) {
            return processActiveHarvestSnapshot();
        }

        if (!snapshot.logs().isEmpty()) {
            WorkResult result;
            boolean treefeller = hasTreefellerTool();
            List<BlockPos> targets = treefeller
                    ? new ArrayList<>(snapshot.logs().subList(0, Math.min(Math.max(1, maxLogsPerAction()), snapshot.logs().size())))
                    : List.of(snapshot.logs().get(0));
            if (!advanceBreak(targets, treefeller)) {
                return WorkResult.WORKED;
            }
            if (treefeller) {
                result = harvestTreeBlocks(cell, targets);
                removeInvalidSnapshotTargets(snapshot.logs(), BlockTags.LOGS);
            } else {
                BlockPos harvestPos = targets.get(0);
                result = harvestTreeBlock(cell, harvestPos);
                removeIfNoLonger(snapshot.logs(), harvestPos, BlockTags.LOGS);
            }
            clearBreakProgress();
            if (snapshot.isEmpty()) {
                clearActiveHarvestSnapshot();
            }
            return result;
        }

        clearActiveHarvestSnapshot();
        return WorkResult.NO_WORK;
    }

    private boolean refreshRemainingSnapshotLeaves(HarvestSnapshot snapshot) {
        if (!snapshot.leaves().isEmpty() || snapshot.leafAnchors().isEmpty()) {
            return false;
        }
        List<BlockPos> leaves = scanRemainingSnapshotLeaves(snapshot.logBase(), snapshot.leafAnchors(), maxTreeHeight());
        if (leaves.isEmpty()) {
            return false;
        }
        snapshot.leaves().addAll(harvestOrder(snapshot.logBase(), leaves));
        return true;
    }

    private WorkResult tryPlant(BlockPos root, BlockPos railPos) {
        if (!plantsSaplings() || managedCells.size() >= maxManagedCells() && !managedCells.containsKey(root)) {
            return WorkResult.NO_WORK;
        }

        BlockState current = level().getBlockState(root);
        if (isPlantedBlock(current) || isLogBase(root)) {
            return WorkResult.NO_WORK;
        }
        ItemStack sapling = plantingSapling(root).copy();
        if (sapling.isEmpty()) {
            return WorkResult.NO_WORK;
        }
        if (!(sapling.getItem() instanceof BlockItem blockItem) || !isPlantableBlock(blockItem.getBlock())) {
            return WorkResult.NO_WORK;
        }
        BlockState plantedState = blockItem.getBlock().defaultBlockState();
        if (ForestryCartRules.rowDistance(railPos, root) > 1 && (!current.canBeReplaced() || !plantedState.canSurvive(level(), root))) {
            return WorkResult.NO_WORK;
        }
        setWorkflowState(
                WorkflowState.PLANTING,
                ForestryCartStationBlockEntity.STATUS_READY,
                ForestryCartStationBlockEntity.ACTION_PLANTING
        );
        if (!current.canBeReplaced()) {
            setStatus(ForestryCartStationBlockEntity.STATUS_PLANTING_BLOCKED);
            setCurrentAction(ForestryCartStationBlockEntity.ACTION_PLANTING_BLOCKED);
            return WorkResult.BLOCKED_STOP;
        }
        if (!plantedState.canSurvive(level(), root)) {
            setStatus(ForestryCartStationBlockEntity.STATUS_INVALID_SOIL);
            setCurrentAction(ForestryCartStationBlockEntity.ACTION_PLANTING_BLOCKED);
            return WorkResult.BLOCKED_STOP;
        }
        List<BlockPos> plot = plantingPlot(root, railPos, sapling, plantedState);
        int plantCost = plantEnergyCost() * plot.size();
        if (!consumeEnergy(plantCost, true)) {
            markNoPower(plantCost);
            return WorkResult.NEEDS_TRANSFER;
        }

        int planted = 0;
        for (BlockPos pos : plot) {
            ItemStack consumed = extractPlantable(sapling.getItem(), managedCells.get(root));
            if (consumed.isEmpty()) {
                break;
            }
            if (!level().setBlock(pos, plantedState, Block.UPDATE_ALL)) {
                insertSaplings(consumed);
                break;
            }
            planted++;
        }
        if (planted == 0) {
            setStatus(ForestryCartStationBlockEntity.STATUS_PLANTING_BLOCKED);
            setCurrentAction(ForestryCartStationBlockEntity.ACTION_PLANTING_BLOCKED);
            return WorkResult.BLOCKED_STOP;
        }
        ResourceLocation saplingId = BuiltInRegistries.ITEM.getKey(sapling.getItem());
        consumeEnergy(plantEnergyCost() * planted, false);
        managedCells.put(root, new ManagedCell(root, saplingId.toString(), ManagedCell.STATE_PLANTED));
        addMachineXp(PLANT_MACHINE_XP);
        workCooldown = plantIntervalTicks();
        playWorkSound(ModSounds.FORESTRY_COMPANION_PLANT.get(), 0.55F, 1.0F, WORK_SOUND_COOLDOWN_TICKS);
        setWorkflowState(WorkflowState.PLANTING, ForestryCartStationBlockEntity.STATUS_READY, ForestryCartStationBlockEntity.ACTION_PLANTING);
        return WorkResult.WORKED;
    }

    private WorkResult harvestTreeBlock(ManagedCell cell, BlockPos harvestPos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return WorkResult.NO_WORK;
        }
        BlockState state = serverLevel.getBlockState(harvestPos);
        boolean leaves = state.is(BlockTags.LEAVES);
        setWorkflowState(
                leaves ? WorkflowState.HARVESTING_LEAVES : WorkflowState.HARVESTING_LOGS,
                ForestryCartStationBlockEntity.STATUS_READY,
                leaves ? ForestryCartStationBlockEntity.ACTION_HARVESTING_LEAVES : ForestryCartStationBlockEntity.ACTION_HARVESTING_LOG
        );
        boolean shearsReady = hasUsableShears();
        boolean collectDrops = !leaves || shearsReady || processesLeavesWithoutShears();
        ItemStack tool = leaves ? (shearsReady ? shearsStack() : ItemStack.EMPTY) : toolStack();
        if (!canHarvestTreeBlock(state, tool, harvestPos, leaves, collectDrops)) {
            setCellState(cell, ManagedCell.STATE_BLOCKED);
            setStatus(ForestryCartStationBlockEntity.STATUS_HARVEST_BLOCKED);
            setCurrentAction(ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED);
            return WorkResult.BLOCKED_STOP;
        }

        int cost = leafHarvestEnergyCost(leaves, shearsReady);
        if (!consumeEnergy(cost, true)) {
            markNoPower(cost);
            return WorkResult.NEEDS_TRANSFER;
        }

        FakePlayer fakePlayer = fakePlayer(serverLevel);
        fakePlayer.moveTo(harvestPos.getX() + 0.5D, harvestPos.getY(), harvestPos.getZ() + 0.5D, 0.0F, 0.0F);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, leaves ? ItemStack.EMPTY : tool);
        BlockEntity blockEntity = serverLevel.getBlockEntity(harvestPos);
        ItemStack lootTool = leaves ? ItemStack.EMPTY : tool;
        List<ItemStack> expectedDrops = collectDrops
                ? Block.getDrops(state, serverLevel, harvestPos, blockEntity, fakePlayer, lootTool)
                : List.of();
        if (collectDrops && !canAcceptOutputs(leaves ? afterReserve(cell, expectedDrops) : expectedDrops)) {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            setWorkflowState(WorkflowState.SEEKING_TRANSFER, ForestryCartStationBlockEntity.STATUS_OUTPUT_FULL, ForestryCartStationBlockEntity.ACTION_OUTPUT_FULL);
            return WorkResult.NEEDS_TRANSFER;
        }

        boolean destroyed = harvestBlockIntoCart(serverLevel, fakePlayer, harvestPos, state);
        if (!leaves) {
            setToolStack(fakePlayer.getMainHandItem());
        }
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        if (!destroyed) {
            setCellState(cell, ManagedCell.STATE_BLOCKED);
            setStatus(ForestryCartStationBlockEntity.STATUS_HARVEST_BLOCKED);
            setCurrentAction(ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED);
            return WorkResult.BLOCKED_STOP;
        }

        consumeEnergy(cost, false);
        if (leaves && shearsReady) {
            applyCartShearsWear();
        }
        if (collectDrops) {
            insertOutputs(leaves ? reserveFromDrops(cell, expectedDrops) : expectedDrops);
            if (!leaves) {
                feedLogLedger(expectedDrops, false);
            }
        }
        addMachineXp(leaves ? LEAF_HARVEST_MACHINE_XP : LOG_HARVEST_MACHINE_XP);
        setCellState(cell, ManagedCell.STATE_HARVESTING);
        workCooldown = leaves ? leafWorkCooldown() : LOG_SETTLE_TICKS;
        setStatus(ForestryCartStationBlockEntity.STATUS_READY);
        playWorkSound(
                leaves ? ModSounds.FORESTRY_COMPANION_LEAF_CUT.get() : ModSounds.FORESTRY_COMPANION_LOG_CUT.get(),
                leaves ? 0.42F : 0.55F,
                leaves ? 1.05F : 0.92F,
                WORK_SOUND_COOLDOWN_TICKS
        );
        return WorkResult.WORKED;
    }

    private WorkResult harvestTreeBlocks(ManagedCell cell, List<BlockPos> harvestPositions) {
        setWorkflowState(
                WorkflowState.HARVESTING_LOGS,
                ForestryCartStationBlockEntity.STATUS_READY,
                ForestryCartStationBlockEntity.ACTION_TREEFELLER_BATCH
        );
        if (!(level() instanceof ServerLevel serverLevel) || harvestPositions.isEmpty()) {
            return WorkResult.NO_WORK;
        }
        ItemStack tool = toolStack();
        FakePlayer fakePlayer = fakePlayer(serverLevel);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);

        List<ItemStack> expectedDrops = new ArrayList<>();
        List<List<ItemStack>> expectedDropsByBlock = new ArrayList<>();
        for (BlockPos harvestPos : harvestPositions) {
            BlockState state = serverLevel.getBlockState(harvestPos);
            if (!canHarvestTreeBlock(state, tool, harvestPos, false, true)) {
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                setCellState(cell, ManagedCell.STATE_BLOCKED);
                setStatus(ForestryCartStationBlockEntity.STATUS_HARVEST_BLOCKED);
                setCurrentAction(ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED);
                return WorkResult.BLOCKED_STOP;
            }
            fakePlayer.moveTo(harvestPos.getX() + 0.5D, harvestPos.getY(), harvestPos.getZ() + 0.5D, 0.0F, 0.0F);
            BlockEntity blockEntity = serverLevel.getBlockEntity(harvestPos);
            List<ItemStack> drops = Block.getDrops(state, serverLevel, harvestPos, blockEntity, fakePlayer, tool);
            expectedDropsByBlock.add(drops);
            expectedDrops.addAll(drops);
        }

        int cost = hasMasteryBehavior("CLEAN_FELL") ? cutEnergyCost() * harvestPositions.size() : cutActionEnergyCost();
        if (!consumeEnergy(cost, true)) {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            markNoPower(cost);
            return WorkResult.NEEDS_TRANSFER;
        }
        if (!canAcceptOutputs(expectedDrops)) {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            setWorkflowState(WorkflowState.SEEKING_TRANSFER, ForestryCartStationBlockEntity.STATUS_OUTPUT_FULL, ForestryCartStationBlockEntity.ACTION_OUTPUT_FULL);
            return WorkResult.NEEDS_TRANSFER;
        }
        if (!canBreakHarvestBlocks(serverLevel, fakePlayer, harvestPositions)) {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            setCellState(cell, ManagedCell.STATE_BLOCKED);
            setStatus(ForestryCartStationBlockEntity.STATUS_HARVEST_BLOCKED);
            setCurrentAction(ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED);
            return WorkResult.BLOCKED_STOP;
        }

        int brokenCount = 0;
        for (int index = 0; index < harvestPositions.size(); index++) {
            BlockPos harvestPos = harvestPositions.get(index);
            BlockState state = serverLevel.getBlockState(harvestPos);
            fakePlayer.moveTo(harvestPos.getX() + 0.5D, harvestPos.getY(), harvestPos.getZ() + 0.5D, 0.0F, 0.0F);
            if (!canHarvestTreeBlock(state, fakePlayer.getMainHandItem(), harvestPos, false, true)) {
                setToolStack(fakePlayer.getMainHandItem());
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                if (brokenCount > 0) {
                    consumeEnergy(cost, false);
                }
                setCellState(cell, ManagedCell.STATE_BLOCKED);
                setStatus(ForestryCartStationBlockEntity.STATUS_HARVEST_BLOCKED);
                setCurrentAction(ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED);
                return WorkResult.BLOCKED_STOP;
            }
            if (!destroyHarvestBlock(serverLevel, fakePlayer, harvestPos, state)) {
                setToolStack(fakePlayer.getMainHandItem());
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                if (brokenCount > 0) {
                    consumeEnergy(cost, false);
                }
                setCellState(cell, ManagedCell.STATE_BLOCKED);
                setStatus(ForestryCartStationBlockEntity.STATUS_HARVEST_BLOCKED);
                setCurrentAction(ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED);
                return WorkResult.BLOCKED_STOP;
            }
            insertOutputs(expectedDropsByBlock.get(index));
            brokenCount++;
        }

        setToolStack(fakePlayer.getMainHandItem());
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        consumeEnergy(cost, false);
        feedLogLedger(expectedDrops, true);
        setCellState(cell, ManagedCell.STATE_HARVESTING);
        addMachineXp(LOG_HARVEST_MACHINE_XP * brokenCount);
        workCooldown = LOG_SETTLE_TICKS;
        setStatus(ForestryCartStationBlockEntity.STATUS_READY);
        playWorkSound(
                ModSounds.FORESTRY_COMPANION_TREEFELLER.get(),
                0.65F,
                Math.min(1.18F, 0.82F + brokenCount * 0.02F),
                TREEFELLER_SOUND_COOLDOWN_TICKS
        );
        return WorkResult.WORKED;
    }

    private boolean canHarvestTreeBlock(BlockState state, ItemStack tool, BlockPos pos, boolean leaves, boolean collectDrops) {
        if (state.getDestroySpeed(level(), pos) < 0.0F) {
            return false;
        }
        if (leaves) {
            return state.is(BlockTags.LEAVES)
                    && (!collectDrops || isUsableShearsCandidate(tool) || processesLeavesWithoutShears());
        }
        return state.is(BlockTags.LOGS) && tool.isCorrectToolForDrops(state);
    }

    private void applyCartShearsWear() {
        ItemStack stack = shearsStack();
        if (!isShearsCandidate(stack) || !stack.isDamageableItem()) {
            return;
        }

        int budget = cartShearsLeafBudget(stack);
        int progress = Math.max(0, stack.getOrDefault(ModDataComponents.CART_SHEARS_WEAR_PROGRESS.get(), 0)) + 1;
        if (progress < budget) {
            stack.set(ModDataComponents.CART_SHEARS_WEAR_PROGRESS.get(), progress);
            setShearsStack(stack);
            return;
        }

        stack.remove(ModDataComponents.CART_SHEARS_WEAR_PROGRESS.get());
        int nextDamage = stack.getDamageValue() + 1;
        if (nextDamage >= stack.getMaxDamage()) {
            setShearsStack(ItemStack.EMPTY);
            return;
        }
        stack.setDamageValue(nextDamage);
        setShearsStack(stack);
    }

    private int cartShearsLeafBudget(ItemStack stack) {
        int budget = stack.getItem() instanceof PruningShearsItem pruningShears
                ? pruningShears.material().cartLeafWearBudget()
                : VANILLA_CART_SHEARS_LEAF_BUDGET;
        return hasMasteryBehavior("CANOPY_CARE") ? budget * 2 : budget;
    }

    private boolean harvestBlockIntoCart(ServerLevel serverLevel, FakePlayer fakePlayer, BlockPos harvestPos, BlockState state) {
        var event = CommonHooks.fireBlockBreak(serverLevel, fakePlayer.gameMode.getGameModeForPlayer(), fakePlayer, harvestPos, state);
        if (event.isCanceled()) {
            return false;
        }
        if (fakePlayer.blockActionRestricted(serverLevel, harvestPos, fakePlayer.gameMode.getGameModeForPlayer())) {
            return false;
        }
        return destroyHarvestBlock(serverLevel, fakePlayer, harvestPos, state);
    }

    private boolean canBreakHarvestBlocks(ServerLevel serverLevel, FakePlayer fakePlayer, List<BlockPos> harvestPositions) {
        for (BlockPos harvestPos : harvestPositions) {
            BlockState state = serverLevel.getBlockState(harvestPos);
            fakePlayer.moveTo(harvestPos.getX() + 0.5D, harvestPos.getY(), harvestPos.getZ() + 0.5D, 0.0F, 0.0F);
            var event = CommonHooks.fireBlockBreak(serverLevel, fakePlayer.gameMode.getGameModeForPlayer(), fakePlayer, harvestPos, state);
            if (event.isCanceled()
                    || fakePlayer.blockActionRestricted(serverLevel, harvestPos, fakePlayer.gameMode.getGameModeForPlayer())) {
                return false;
            }
        }
        return true;
    }

    private boolean destroyHarvestBlock(ServerLevel serverLevel, FakePlayer fakePlayer, BlockPos harvestPos, BlockState state) {
        BlockState breakState = state.getBlock().playerWillDestroy(serverLevel, harvestPos, state, fakePlayer);
        ItemStack tool = fakePlayer.getMainHandItem();
        ModularToolItem.runWithoutExtraBreaks(() -> {
            tool.mineBlock(serverLevel, breakState, harvestPos, fakePlayer);
            return true;
        });
        boolean removed = breakState.onDestroyedByPlayer(serverLevel, harvestPos, fakePlayer, true, serverLevel.getFluidState(harvestPos));
        if (removed) {
            breakState.getBlock().destroy(serverLevel, harvestPos, breakState);
        }
        return removed;
    }

    private FakePlayer fakePlayer(ServerLevel serverLevel) {
        if (ownerUuid == null) {
            return FakePlayerFactory.getMinecraft(serverLevel);
        }
        String profileName = ownerName == null || ownerName.isBlank() ? "RNGTechForestryCart" : ownerName;
        return FakePlayerFactory.get(serverLevel, new GameProfile(ownerUuid, profileName));
    }

    private void pruneInvalidSnapshotTargets(HarvestSnapshot snapshot) {
        removeInvalidSnapshotTargets(snapshot.leaves(), BlockTags.LEAVES);
        removeInvalidSnapshotTargets(snapshot.logs(), BlockTags.LOGS);
    }

    private boolean removeInvalidSnapshotTargets(List<BlockPos> positions, net.minecraft.tags.TagKey<Block> expectedTag) {
        return positions.removeIf(pos -> !level().getBlockState(pos).is(expectedTag));
    }

    private void removeIfNoLonger(List<BlockPos> positions, BlockPos pos, net.minecraft.tags.TagKey<Block> expectedTag) {
        if (!level().getBlockState(pos).is(expectedTag)) {
            positions.remove(pos);
        }
    }

    private void clearActiveHarvestSnapshot() {
        activeHarvestSnapshot = null;
        clearBreakProgress();
    }

    /**
     * Cracks the next logs over time before they are cut, as a player would. Returns true once the break is complete;
     * a changed target list restarts it. A cart that cannot pay for the cut skips the wait so the cut reports why.
     */
    private boolean advanceBreak(List<BlockPos> targets, boolean batch) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        if (activeBreak == null || !activeBreak.targets().equals(targets)) {
            clearBreakProgress();
            int cost = batch ? cutActionEnergyCost() : cutEnergyCost();
            if (!consumeEnergy(cost, true)) {
                return true;
            }
            int single = 0;
            for (BlockPos pos : targets) {
                single = Math.max(single, breakTicks(pos, serverLevel.getBlockState(pos)));
            }
            activeBreak = new ActiveBreak(List.copyOf(targets), batch ? ForestryCartRules.batchBreakTicks(single, targets.size()) : single);
        }
        activeBreak.tick();
        if (activeBreak.done()) {
            return true;
        }
        setWorkflowState(
                WorkflowState.HARVESTING_LOGS,
                ForestryCartStationBlockEntity.STATUS_READY,
                batch ? ForestryCartStationBlockEntity.ACTION_TREEFELLER_BATCH : ForestryCartStationBlockEntity.ACTION_HARVESTING_LOG
        );
        int stage = ForestryCartRules.crackStage(activeBreak.elapsed(), activeBreak.total());
        List<BlockPos> shown = activeBreak.targets();
        for (int index = 0; index < Math.min(MAX_CRACK_OVERLAYS, shown.size()); index++) {
            serverLevel.destroyBlockProgress(crackId(index), shown.get(index), stage);
        }
        if (activeBreak.elapsed() % BREAK_SOUND_INTERVAL_TICKS == 1) {
            BlockPos pos = shown.get(0);
            SoundType sound = serverLevel.getBlockState(pos).getSoundType(serverLevel, pos, this);
            serverLevel.playSound(null, pos, sound.getHitSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 8.0F, sound.getPitch() * 0.5F);
        }
        return false;
    }

    private void clearBreakProgress() {
        if (activeBreak == null) {
            return;
        }
        if (level() instanceof ServerLevel serverLevel) {
            List<BlockPos> shown = activeBreak.targets();
            for (int index = 0; index < Math.min(MAX_CRACK_OVERLAYS, shown.size()); index++) {
                serverLevel.destroyBlockProgress(crackId(index), shown.get(index), -1);
            }
        }
        activeBreak = null;
    }

    /** Crack overlays need one breaker id per block; negative ids never collide with real entities. */
    private int crackId(int index) {
        return -(getId() * MAX_CRACK_OVERLAYS + index) - 1;
    }

    private ManagedCell managedCell(BlockPos root) {
        ManagedCell cell = managedCells.get(root);
        if (cell == null && managedCells.size() < maxManagedCells()) {
            cell = new ManagedCell(root, "", ManagedCell.STATE_HARVESTING);
            managedCells.put(root, cell);
        }
        return cell;
    }

    private void setCellState(ManagedCell cell, int state) {
        if (cell != null) {
            cell.setState(state);
        }
    }

    /**
     * A cell remembers whatever is growing in it, so Seed Library replants what was there, even when a player planted
     * it. Plants this cart can tend are adopted into the managed cells while the cap allows.
     */
    private void updateManagedSaplingState(BlockPos root) {
        BlockState state = level().getBlockState(root);
        ManagedCell cell = managedCells.get(root);
        if (!isPlantedBlock(state)) {
            return;
        }
        if (isPlantableBlock(state.getBlock()) && state.getBlock().asItem() != Items.AIR) {
            String plantedId = BuiltInRegistries.ITEM.getKey(state.getBlock().asItem()).toString();
            if (cell == null && managedCells.size() < maxManagedCells()) {
                cell = new ManagedCell(root.immutable(), plantedId, ManagedCell.STATE_PLANTED);
                managedCells.put(cell.root(), cell);
            } else if (cell != null && !cell.saplingId().equals(plantedId)) {
                releaseReserve(cell);
                cell.setSaplingId(plantedId);
            }
        }
        if (cell != null) {
            cell.setState(ManagedCell.STATE_PLANTED);
        }
    }

    private boolean isLogBase(BlockPos root) {
        return level().getBlockState(root).is(BlockTags.LOGS);
    }

    private HarvestScan scanHarvestTree(BlockPos logBase, int maxHeight) {
        ForestryTreeScan.TreeBlocks blocks = ForestryTreeScan.TreeBlocks.of(level());
        ForestryTreeScan.Bounds bounds = treeBounds(logBase, maxHeight);
        ForestryTreeScan.OwnedLogs owned = ForestryTreeScan.ownedLogs(blocks, bounds, logBase, maxSnapshotLogs());
        if (owned.tooLarge()) {
            return HarvestScan.TOO_LARGE;
        }
        if (owned.logs().isEmpty()) {
            return HarvestScan.EMPTY;
        }
        List<BlockPos> leaves = ForestryTreeScan.leavesAround(blocks, bounds, owned.logs(), LEAF_SCAN_RADIUS, MAX_CONNECTED_LEAF_BLOCKS);
        return new HarvestScan(owned.logs(), leaves, false);
    }

    private List<BlockPos> scanRemainingSnapshotLeaves(BlockPos root, List<BlockPos> leafAnchors, int maxHeight) {
        Set<BlockPos> anchorSet = new HashSet<>(leafAnchors);
        BlockPos start = findRemainingSnapshotLeaf(root, leafAnchors, anchorSet, maxHeight);
        if (start == null) {
            return List.of();
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && leaves.size() < MAX_CONNECTED_LEAF_BLOCKS) {
            BlockPos current = queue.remove();
            if (!isSnapshotLeaf(root, current, leafAnchors, anchorSet, maxHeight)) {
                continue;
            }
            leaves.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = current.offset(dx, dy, dz);
                        if (!seen.add(next) || !isSnapshotLeaf(root, next, leafAnchors, anchorSet, maxHeight)) {
                            continue;
                        }
                        queue.add(next);
                    }
                }
            }
        }
        return List.copyOf(leaves);
    }

    private BlockPos findRemainingSnapshotLeaf(BlockPos root, List<BlockPos> leafAnchors, Set<BlockPos> anchorSet, int maxHeight) {
        for (int y = maxHeight; y >= 0; y--) {
            for (int radius = 0; radius <= SCAN_BOUND_HORIZONTAL; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos candidate = root.offset(dx, y, dz);
                        if (isSnapshotLeaf(root, candidate, leafAnchors, anchorSet, maxHeight)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isSnapshotLeaf(BlockPos root, BlockPos candidate, List<BlockPos> leafAnchors, Set<BlockPos> anchorSet, int maxHeight) {
        return treeBounds(root, maxHeight).contains(candidate)
                && level().getBlockState(candidate).is(BlockTags.LEAVES)
                && ForestryTreeScan.withinLeafReach(leafAnchors, candidate, LEAF_SCAN_RADIUS)
                && !ForestryTreeScan.touchesForeignLog(ForestryTreeScan.TreeBlocks.of(level()), candidate, anchorSet);
    }

    private List<BlockPos> harvestOrder(BlockPos root, List<BlockPos> blocks) {
        List<BlockPos> ordered = new ArrayList<>(blocks);
        ordered.sort((left, right) -> {
            if (left.equals(right)) {
                return 0;
            }
            return harvestsBefore(root, left, right) ? -1 : 1;
        });
        return ordered;
    }

    private boolean harvestsBefore(BlockPos root, BlockPos candidate, BlockPos selected) {
        if (candidate.getY() != selected.getY()) {
            return candidate.getY() > selected.getY();
        }
        int candidateDistance = horizontalDistanceSquared(root, candidate);
        int selectedDistance = horizontalDistanceSquared(root, selected);
        if (candidateDistance != selectedDistance) {
            return candidateDistance < selectedDistance;
        }
        if (candidate.getX() != selected.getX()) {
            return candidate.getX() < selected.getX();
        }
        return candidate.getZ() < selected.getZ();
    }

    private int horizontalDistanceSquared(BlockPos root, BlockPos candidate) {
        int dx = candidate.getX() - root.getX();
        int dz = candidate.getZ() - root.getZ();
        return dx * dx + dz * dz;
    }

    private ForestryTreeScan.Bounds treeBounds(BlockPos root, int maxHeight) {
        return new ForestryTreeScan.Bounds(root, maxHeight, SCAN_BOUND_HORIZONTAL);
    }

    private boolean isSaplingBlock(BlockState state) {
        return state.getBlock() instanceof SaplingBlock;
    }

    private boolean isPlantedBlock(BlockState state) {
        return isSaplingBlock(state) || state.getBlock() instanceof CropBlock;
    }

    private boolean isPlantableBlock(Block block) {
        return tendsCrops() ? block instanceof CropBlock : block instanceof SaplingBlock;
    }

    private boolean isMatureCrop(BlockPos root) {
        BlockState state = level().getBlockState(root);
        return managedCells.containsKey(root) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private int activeManagedCells() {
        int active = 0;
        for (ManagedCell cell : managedCells.values()) {
            if (cell.state() == ManagedCell.STATE_PLANTED || cell.state() == ManagedCell.STATE_HARVESTING) {
                active++;
            }
        }
        return active;
    }

    private boolean isOutputCargoFull() {
        for (int slot = OUTPUT_SLOT_START; slot < CARGO_SLOT_COUNT; slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxStackSize(), cargoInventory.getSlotLimit(slot))) {
                return false;
            }
        }
        return true;
    }

    private int cargoCount(int startSlot, int slotCount) {
        int count = 0;
        for (int slot = startSlot; slot < startSlot + slotCount; slot++) {
            count += cargoInventory.getStackInSlot(slot).getCount();
        }
        return count;
    }

    private int cargoCapacity(int startSlot, int slotCount) {
        int capacity = 0;
        for (int slot = startSlot; slot < startSlot + slotCount; slot++) {
            capacity += cargoInventory.getSlotLimit(slot);
        }
        return capacity;
    }

    private ItemStack routeCargo(ItemStackHandler inventory, ItemStack stack) {
        return plantsSaplings() ? routeOutput(inventory, stack) : insertIntoRange(inventory, OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT, stack);
    }

    private ItemStack routeOutput(ItemStackHandler inventory, ItemStack stack) {
        ItemStack remaining = stack.copy();
        if (isPlantableStack(remaining)) {
            remaining = insertIntoRange(inventory, SAPLING_SLOT_START, SAPLING_SLOT_COUNT, remaining);
        }
        return insertIntoRange(inventory, OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT, remaining);
    }

    private static ItemStack insertIntoRange(ItemStackHandler inventory, int startSlot, int slotCount, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = startSlot; slot < startSlot + slotCount && !remaining.isEmpty(); slot++) {
            remaining = inventory.insertItem(slot, remaining, false);
        }
        return remaining;
    }

    private static CompoundTag expandedGearInventoryTag(CompoundTag gearTag) {
        CompoundTag expandedTag = gearTag.copy();
        if (expandedTag.getInt("Size") < GEAR_SLOT_COUNT) {
            expandedTag.putInt("Size", GEAR_SLOT_COUNT);
        }
        return expandedTag;
    }

    private static WorkflowState workflowState(String name) {
        for (WorkflowState state : WorkflowState.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }
        return WorkflowState.SETUP_BLOCKED;
    }

    private void updateScanDebugData() {
        entityData.set(DATA_DEBUG_MAX_CONNECTED_LOGS, maxSnapshotLogs());
        entityData.set(DATA_DEBUG_MAX_TREE_HEIGHT, maxTreeHeight());
        entityData.set(DATA_DEBUG_CAN_HARVEST_LEAVES, true);
    }

    private boolean hasMasteryBehavior(String behavior) {
        return MegaPassiveTree.has(machineProgression, behavior);
    }

    private boolean plantsSaplings() {
        return !hasMasteryBehavior("CLEARCUT_CHARTER");
    }

    public static boolean isFertilizerStack(ItemStack stack) {
        return stack.is(Items.BONE_MEAL);
    }

    /** Only a Grove Warden stores bone meal. */
    public boolean acceptsFertilizer() {
        return hasMasteryBehavior("FERTILIZER_PULSE");
    }

    public int fertilizer() {
        return fertilizer;
    }

    /** Takes up to {@code count} bone meal from a station and returns how much was stored. */
    public int acceptFertilizer(int count) {
        return storeFertilizer(count, false);
    }

    private int storeFertilizer(int count, boolean simulate) {
        if (count <= 0 || !acceptsFertilizer()) {
            return 0;
        }
        int accepted = Math.min(count, FERTILIZER_CAPACITY - fertilizer);
        if (!simulate) {
            fertilizer += accepted;
        }
        return accepted;
    }

    /** Bone meal a player placed in a supply slot moves into the fertilizer store. */
    private void absorbSuppliedFertilizer() {
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START; slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            if (isFertilizerStack(stack)) {
                int accepted = storeFertilizer(stack.getCount(), false);
                if (accepted > 0) {
                    cargoInventory.extractItem(slot, accepted, false);
                }
            }
        }
    }

    /** After a change of ascendancy, supply items this cart can no longer plant move to output cargo. */
    private void evictUnplantableSupply() {
        if (!keepsReserves()) {
            managedCells.values().forEach(this::releaseReserve);
        } else {
            for (ManagedCell cell : managedCells.values()) {
                if (cell.reserved() > 0 && !isPlantableStack(reserveStack(cell))) {
                    releaseReserve(cell);
                }
            }
        }
        for (int slot = SAPLING_SLOT_START; slot < OUTPUT_SLOT_START; slot++) {
            ItemStack stack = cargoInventory.getStackInSlot(slot);
            if (stack.isEmpty() || isPlantableStack(stack) || isFertilizerStack(stack)) {
                continue;
            }
            ItemStack rest = insertIntoRange(cargoInventory, OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT, stack);
            cargoInventory.setStackInSlot(slot, rest);
        }
    }

    /** Seed Library carts set aside each cell's replant from that cell's own harvest. */
    private boolean keepsReserves() {
        return plantsSaplings() && hasMasteryBehavior("SEED_LIBRARY");
    }

    /** How many of {@code item} the cell holds for its replant. */
    private int reservedFor(ManagedCell cell, Item item) {
        if (cell == null || cell.reserved() <= 0 || item == Items.AIR) {
            return 0;
        }
        return BuiltInRegistries.ITEM.getKey(item).toString().equals(cell.saplingId()) ? cell.reserved() : 0;
    }

    /** One replant, or four for an Ancient Grove plot of a giant sapling. */
    private int reserveTarget(Item item) {
        return hasMasteryBehavior("ANCIENT_GROVE") && new ItemStack(item).is(ModTags.Items.GIANT_SAPLINGS) ? GIANT_TREE_SAPLINGS : 1;
    }

    /** The species and count this harvest would set aside for the cell, or null when nothing is reserved. */
    private ItemStack plannedReserve(ManagedCell cell, List<ItemStack> drops) {
        if (cell == null || !keepsReserves()) {
            return ItemStack.EMPTY;
        }
        Item item = cell.saplingId().isEmpty() ? Items.AIR : BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(cell.saplingId()));
        if (item == Items.AIR) {
            item = drops.stream().filter(this::isPlantableStack).map(ItemStack::getItem).findFirst().orElse(Items.AIR);
        }
        if (item == Items.AIR || !isPlantableStack(new ItemStack(item))) {
            return ItemStack.EMPTY;
        }
        int need = reserveTarget(item) - reservedFor(cell, item);
        Item species = item;
        int available = drops.stream().filter(drop -> drop.is(species)).mapToInt(ItemStack::getCount).sum();
        int take = Math.min(need, available);
        return take > 0 ? new ItemStack(item, take) : ItemStack.EMPTY;
    }

    /** The drops left to route after the cell's reserve is set aside. */
    private List<ItemStack> afterReserve(ManagedCell cell, List<ItemStack> drops) {
        ItemStack reserve = plannedReserve(cell, drops);
        if (reserve.isEmpty()) {
            return drops;
        }
        int remaining = reserve.getCount();
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack copy = drop.copy();
            if (remaining > 0 && copy.is(reserve.getItem())) {
                int taken = Math.min(remaining, copy.getCount());
                copy.shrink(taken);
                remaining -= taken;
            }
            if (!copy.isEmpty()) {
                rest.add(copy);
            }
        }
        return rest;
    }

    /** Sets the cell's replant aside from its own harvest and returns the drops left to route. */
    private List<ItemStack> reserveFromDrops(ManagedCell cell, List<ItemStack> drops) {
        ItemStack reserve = plannedReserve(cell, drops);
        if (reserve.isEmpty()) {
            return drops;
        }
        List<ItemStack> rest = afterReserve(cell, drops);
        String id = BuiltInRegistries.ITEM.getKey(reserve.getItem()).toString();
        if (!id.equals(cell.saplingId())) {
            releaseReserve(cell);
            cell.setSaplingId(id);
        }
        cell.setReserved(cell.reserved() + reserve.getCount());
        return rest;
    }

    private ItemStack reserveStack(ManagedCell cell) {
        if (cell == null || cell.reserved() <= 0 || cell.saplingId().isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(cell.saplingId()));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, cell.reserved());
    }

    /** Returns a cell's reserve to cargo, or drops it when cargo is full. */
    private void releaseReserve(ManagedCell cell) {
        ItemStack reserve = reserveStack(cell);
        if (cell != null) {
            cell.setReserved(0);
        }
        if (!reserve.isEmpty()) {
            insertOutputs(List.of(reserve));
        }
    }

    /** Takes one plantable for a cell, from its reserve first and then from supply cargo. */
    private ItemStack extractPlantable(Item item, ManagedCell cell) {
        if (reservedFor(cell, item) > 0) {
            cell.setReserved(cell.reserved() - 1);
            return new ItemStack(item);
        }
        return extractSapling(item);
    }

    /** Plantables set aside in managed cells for their replants. */
    public int reservedPlantables() {
        return managedCells.values().stream().mapToInt(ManagedCell::reserved).sum();
    }

    /**
     * Species a Seed Library cart is waiting for: how many empty managed cells remember each one and have no reserve.
     * A docked station loads these first.
     */
    public Map<Item, Integer> plantDemand() {
        Map<Item, Integer> demand = new LinkedHashMap<>();
        if (!keepsReserves()) {
            return demand;
        }
        for (ManagedCell cell : managedCells.values()) {
            if (cell.saplingId().isEmpty() || cell.reserved() > 0) {
                continue;
            }
            BlockState state = level().getBlockState(cell.root());
            if (isPlantedBlock(state) || state.is(BlockTags.LOGS)) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(cell.saplingId()));
            if (item != Items.AIR && isPlantableStack(new ItemStack(item))) {
                demand.merge(item, 1, Integer::sum);
            }
        }
        return demand;
    }

    /** Supply cargo of one item. */
    public int supplyCount(Item item) {
        return saplingCount(item);
    }

    private void dropFertilizer() {
        while (fertilizer > 0) {
            int count = Math.min(fertilizer, Items.BONE_MEAL.getDefaultMaxStackSize());
            Containers.dropItemStack(level(), getX(), getY(), getZ(), new ItemStack(Items.BONE_MEAL, count));
            fertilizer -= count;
        }
    }

    /**
     * Grove Warden: each managed sapling in reach takes a growth pulse at most once per interval. A pulse spends one bone
     * meal and FE and applies bone meal Growth Pulse times, so FE alone never grows wood.
     */
    private void pulseSaplings(BlockPos railPos, List<BlockPos> workRoots) {
        if (fertilizer <= 0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        MachineStatAccumulator stats = effectiveStats();
        if (stats.value(MachineStat.GROWTH_PULSE) <= 0.0) {
            return;
        }
        long now = serverLevel.getGameTime();
        int cost = stats.adjustedEnergyCost(PULSE_FE);
        for (ManagedCell cell : pulseTargets(railPos, workRoots)) {
            BlockPos pos = cell.root();
            BlockState state = serverLevel.getBlockState(pos);
            if (fertilizer <= 0) {
                return;
            }
            if (cell.nextPulseTick() > now || !(state.getBlock() instanceof SaplingBlock sapling)
                    || !sapling.isValidBonemealTarget(serverLevel, pos, state)) {
                continue;
            }
            if (!consumeEnergy(cost, false)) {
                return;
            }
            fertilizer--;
            cell.setNextPulseTick(now + PULSE_INTERVAL_TICKS);
            int attempts = AscendancyFormulas.pulseAttempts(stats, random.nextDouble());
            for (int attempt = 0; attempt < attempts; attempt++) {
                BlockState target = serverLevel.getBlockState(pos);
                if (!(target.getBlock() instanceof SaplingBlock grower) || !grower.isValidBonemealTarget(serverLevel, pos, target)) {
                    break;
                }
                if (grower.isBonemealSuccess(serverLevel, serverLevel.random, pos, target)) {
                    boolean ready = target.getValue(SaplingBlock.STAGE) > 0;
                    grower.performBonemeal(serverLevel, serverLevel.random, pos, target);
                    if (ready && serverLevel.getBlockState(pos).getBlock() instanceof SaplingBlock) {
                        // A ready sapling that did not grow lacks room or a single-tree form; stop spending bone meal on it for a while.
                        cell.setNextPulseTick(now + PULSE_STALLED_INTERVAL_TICKS);
                        break;
                    }
                }
            }
            serverLevel.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
        }
    }

    /** The cells beside the rail, or with Verdant Surge every managed cell within reach of it. */
    private List<ManagedCell> pulseTargets(BlockPos railPos, List<BlockPos> workRoots) {
        if (hasMasteryBehavior("VERDANT_SURGE")) {
            return managedCells.values().stream().filter(cell -> withinReach(railPos, cell.root(), workReach())).toList();
        }
        return workRoots.stream().map(managedCells::get).filter(java.util.Objects::nonNull).toList();
    }

    /** Movement costs a quarter more FE while Growth Pulse is on, paid as whole FE as the fraction adds up. */
    private int movementCharge() {
        int cost = movementEnergyCost();
        if (!acceptsFertilizer()) {
            return cost;
        }
        movementSurchargeCarry += cost * GROWTH_PULSE_MOVEMENT_SURCHARGE;
        int surcharge = (int) movementSurchargeCarry;
        movementSurchargeCarry -= surcharge;
        return cost + surcharge;
    }

    /** Timber Baron: harvested logs bank Ledger Rate of themselves and pay whole logs into cargo; Heartwood feeds batches twice. */
    private void feedLogLedger(List<ItemStack> drops, boolean batch) {
        if (!hasMasteryBehavior("LOG_LEDGER")) {
            return;
        }
        MachineStatAccumulator stats = effectiveStats();
        boolean doubled = batch && hasMasteryBehavior("HEARTWOOD");
        for (ItemStack drop : drops) {
            if (drop.is(ItemTags.LOGS)) {
                logLedger.add(drop.getItem(), AscendancyFormulas.ledgerShare(stats, drop.getCount(), doubled));
            }
        }
        for (Item log : logLedger.entries().keySet()) {
            int payable = logLedger.payable(log);
            if (payable > 0) {
                ItemStack rest = insertIntoRange(cargoInventory, OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT, new ItemStack(log, payable));
                logLedger.pay(log, payable - rest.getCount());
            }
        }
    }

    /** Each leaf waits the shear interval; the Log Ledger makes leaf cleanup a quarter slower. */
    private int leafWorkCooldown() {
        if (!hasMasteryBehavior("LOG_LEDGER")) {
            return shearIntervalTicks();
        }
        leafDelayCarry += (1 + shearIntervalTicks()) * LOG_LEDGER_LEAF_SLOWDOWN;
        int extra = (int) leafDelayCarry;
        leafDelayCarry -= extra;
        return shearIntervalTicks() + extra;
    }

    private boolean rollingHarvest() {
        return hasMasteryBehavior("ROLLING_HARVEST") || hasMasteryBehavior("ROLLING_REAP");
    }

    /** Field Hand carts plant and harvest crops. */
    public boolean tendsCrops() {
        return hasMasteryBehavior("CROP_TENDING");
    }

    /** Field Hand carts never plant saplings or cut trees. */
    public boolean worksTrees() {
        return !hasMasteryBehavior("NO_TREE_WORK");
    }

    /** Harvests a fully grown managed crop; Reap and Sow replants the cell in the same action. */
    private WorkResult harvestCrop(BlockPos root, BlockPos railPos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return WorkResult.NO_WORK;
        }
        BlockState state = serverLevel.getBlockState(root);
        int cost = effectiveStats().adjustedEnergyCost(CROP_HARVEST_FE);
        if (!consumeEnergy(cost, true)) {
            markNoPower(cost);
            return WorkResult.NEEDS_TRANSFER;
        }
        FakePlayer fakePlayer = fakePlayer(serverLevel);
        fakePlayer.moveTo(root.getX() + 0.5D, root.getY(), root.getZ() + 0.5D, 0.0F, 0.0F);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        List<ItemStack> drops = Block.getDrops(state, serverLevel, root, null, fakePlayer, ItemStack.EMPTY);
        ManagedCell cell = managedCells.get(root);
        if (!canAcceptOutputs(afterReserve(cell, drops))) {
            setWorkflowState(WorkflowState.SEEKING_TRANSFER, ForestryCartStationBlockEntity.STATUS_OUTPUT_FULL, ForestryCartStationBlockEntity.ACTION_OUTPUT_FULL);
            return WorkResult.NEEDS_TRANSFER;
        }
        if (!harvestBlockIntoCart(serverLevel, fakePlayer, root, state)) {
            setCellState(managedCells.get(root), ManagedCell.STATE_BLOCKED);
            setStatus(ForestryCartStationBlockEntity.STATUS_HARVEST_BLOCKED);
            setCurrentAction(ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED);
            return WorkResult.BLOCKED_STOP;
        }
        consumeEnergy(cost, false);
        insertOutputs(reserveFromDrops(cell, drops));
        addMachineXp(CROP_HARVEST_MACHINE_XP);
        workCooldown = plantIntervalTicks();
        setWorkflowState(WorkflowState.HARVESTING_LEAVES, ForestryCartStationBlockEntity.STATUS_READY, ForestryCartStationBlockEntity.ACTION_HARVESTING_CROP);
        playWorkSound(ModSounds.FORESTRY_COMPANION_LEAF_CUT.get(), 0.42F, 1.15F, WORK_SOUND_COOLDOWN_TICKS);
        if (hasMasteryBehavior("REAP_AND_SOW")) {
            tryPlant(root, railPos);
        }
        return WorkResult.WORKED;
    }

    /**
     * The sprinkler tills dirt and grass under empty crop cells and keeps farmland fully moist, spending water from the
     * cart's tank; it needs a Fluid Pump installed. Irrigation reaches one row further at half the water.
     */
    private void sprinkle(BlockPos railPos, List<BlockPos> workRoots) {
        if (!hasMasteryBehavior("SPRINKLER") || !hasPump() || !hasSaplings() && reservedPlantables() == 0) {
            return;
        }
        boolean irrigation = hasMasteryBehavior("IRRIGATION");
        int cost = irrigation ? SPRINKLER_WATER_MB / 2 : SPRINKLER_WATER_MB;
        List<BlockPos> cells = new ArrayList<>(workRoots);
        if (irrigation && isForestryWorkRail(railPos)) {
            for (BlockPos root : ForestryCartRules.scanRoots(railPos, scanDirection(railPos), workRange() + 1)) {
                if (!cells.contains(root)) {
                    cells.add(root);
                }
            }
        }
        long now = level().getGameTime();
        int duration = irrigation ? (int) (HYDRATION_TICKS * IRRIGATION_HYDRATION_MULTIPLIER) : HYDRATION_TICKS;
        boolean sprayed = false;
        for (BlockPos root : cells) {
            if (waterTank.getFluidAmount() < cost) {
                return;
            }
            BlockPos soil = root.below();
            BlockState soilState = level().getBlockState(soil);
            BlockState wet = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
            boolean changed = false;
            if (soilState.is(Blocks.FARMLAND)) {
                Long until = hydratedUntil.get(soil);
                if (until != null && until - now > duration / 2 && soilState.getValue(FarmBlock.MOISTURE) == FarmBlock.MAX_MOISTURE) {
                    if (tickCount % COSMETIC_SPRAY_INTERVAL_TICKS == 0) {
                        sprayWater(soil);
                    }
                    continue;
                }
                changed = soilState.getValue(FarmBlock.MOISTURE) == FarmBlock.MAX_MOISTURE
                        || level().setBlock(soil, soilState.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
            } else if (isTillable(soilState) && level().getBlockState(root).isAir() && level().getBlockState(soil.above()).isAir()) {
                changed = level().setBlock(soil, wet, Block.UPDATE_ALL);
            }
            if (changed) {
                hydrate(soil, now + duration);
                waterTank.drain(cost, IFluidHandler.FluidAction.EXECUTE);
                sprayWater(soil);
                sprayed = true;
            }
        }
        if (sprayed) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.BLOCKS, 0.35F, 1.4F + random.nextFloat() * 0.2F);
        }
    }

    /** A spray arcs from the cart and splashes down on the watered block. */
    private void sprayWater(BlockPos soil) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = soil.getX() + 0.5D;
        double y = soil.getY() + 1.0D;
        double z = soil.getZ() + 0.5D;
        serverLevel.sendParticles(ParticleTypes.SPLASH, x, y + 0.05D, z, 10, 0.3D, 0.05D, 0.3D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.FALLING_WATER, x, y + 0.9D, z, 4, 0.25D, 0.1D, 0.25D, 0.0D);
        double dx = x - getX();
        double dz = z - getZ();
        for (int step = 1; step <= 3; step++) {
            double t = step / 4.0D;
            double arc = Math.sin(t * Math.PI) * 0.8D;
            serverLevel.sendParticles(ParticleTypes.SPLASH, getX() + dx * t, getY() + 0.9D + arc, getZ() + dz * t, 2, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    /** Sprinkled farmland holds a water ticket, so it stays moist until the ticket runs out or the cart refreshes it. */
    private void hydrate(BlockPos soil, long until) {
        BlockPos pos = soil.immutable();
        hydratedUntil.put(pos, until);
        AABBTicket ticket = hydrationTickets.get(pos);
        if (ticket == null || !ticket.isValid() || !FarmlandWaterManager.hasBlockWaterTicket(level(), pos)) {
            if (ticket != null) {
                ticket.invalidate();
            }
            ticket = FarmlandWaterManager.addAABBTicket(level(), new AABB(pos));
            ticket.validate();
            hydrationTickets.put(pos, ticket);
        }
    }

    /**
     * Ends expired water tickets and restores live ones after a reload or chunk unload, which drop tickets. Ends early
     * where the farmland is gone.
     */
    private void maintainHydration() {
        if (hydratedUntil.isEmpty()) {
            return;
        }
        long now = level().getGameTime();
        var entries = hydratedUntil.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            BlockPos pos = entry.getKey();
            if (!level().isLoaded(pos)) {
                continue;
            }
            if (entry.getValue() <= now || !level().getBlockState(pos).is(Blocks.FARMLAND)) {
                AABBTicket ticket = hydrationTickets.remove(pos);
                if (ticket != null) {
                    ticket.invalidate();
                }
                entries.remove();
            } else if (!FarmlandWaterManager.hasBlockWaterTicket(level(), pos)) {
                AABBTicket ticket = FarmlandWaterManager.addAABBTicket(level(), new AABB(pos));
                ticket.validate();
                hydrationTickets.put(pos, ticket);
            }
        }
    }

    private static boolean isTillable(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH);
    }

    public boolean hasPump() {
        return isPumpCandidate(gearInventory.getStackInSlot(SLOT_PUMP));
    }

    public ItemStack pumpStack() {
        return gearInventory.getStackInSlot(SLOT_PUMP);
    }

    public int waterStored() {
        return waterTank.getFluidAmount();
    }

    /** Fluid Capacity sizes the water tank; water above a smaller capacity stays until the sprinkler uses it. */
    private void updateWaterCapacity() {
        int capacity = Math.max(WATER_CAPACITY, (int) Math.round(pumpStats().value(MachineStat.FLUID_CAPACITY)));
        if (waterTank.getCapacity() != capacity) {
            waterTank.setCapacity(capacity);
        }
    }

    /** Water a docked station may load per tick: the installed pump's rolled transfer rate, or nothing without one. */
    public int waterTransferRate() {
        return hasPump() ? (int) Math.round(pumpStats().value(MachineStat.FLUID_TRANSFER)) : 0;
    }

    /** Cart stats with the installed Fluid Pump's contribution, whose Fluid Capacity affixes size the water tank. */
    private MachineStatAccumulator pumpStats() {
        MachineStatAccumulator stats = effectiveStats();
        if (hasPump()) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, pumpStack());
        }
        return stats;
    }

    /** Fills the cart's water tank from a station; returns the millibuckets accepted. */
    public int acceptWater(int amount, boolean simulate) {
        int limited = Math.min(amount, waterTransferRate());
        if (limited <= 0 || !hasMasteryBehavior("SPRINKLER")) {
            return 0;
        }
        return waterTank.fill(new FluidStack(Fluids.WATER, limited), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * Rolling Harvest keeps moving unless the tree being cut, or a tree or empty cell still waiting for work, would leave
     * reach at the next rail block.
     */
    private boolean canRollPast(BlockPos railPos) {
        Direction next = nextRailDirection(railPos);
        if (next == null) {
            return false;
        }
        BlockPos nextRail = railPos.relative(next);
        if (activeHarvestSnapshot != null && !withinReach(nextRail, activeHarvestSnapshot.logBase(), workReach())) {
            return false;
        }
        int reach = workReach();
        for (BlockPos root : workRoots(railPos)) {
            if (!withinReach(nextRail, root, reach) && hasPendingWork(root)) {
                return false;
            }
        }
        return true;
    }

    /** The side cells to work: the current rail block's, plus under Rolling Harvest the cells it rolled past within reach. */
    private List<BlockPos> workRoots(BlockPos railPos) {
        if (!rollingHarvest()) {
            return plantingScanRoots(railPos);
        }
        List<BlockPos> roots = new ArrayList<>();
        int reach = workReach();
        for (List<BlockPos> railRoots : rolledRails.values()) {
            for (BlockPos root : railRoots) {
                if (withinReach(railPos, root, reach) && !roots.contains(root)) {
                    roots.add(root);
                }
            }
        }
        for (BlockPos root : plantingScanRoots(railPos)) {
            if (!roots.contains(root)) {
                roots.add(root);
            }
        }
        return roots;
    }

    private void rememberRolledRail(BlockPos railPos) {
        if (!rollingHarvest()) {
            rolledRails.clear();
            return;
        }
        List<BlockPos> roots = plantingScanRoots(railPos);
        if (roots.isEmpty()) {
            return;
        }
        rolledRails.remove(railPos);
        rolledRails.put(railPos.immutable(), roots);
        int reach = workReach();
        rolledRails.keySet().removeIf(rail -> !withinReach(railPos, rail, reach + 1));
    }

    private BlockPos rolledRail(BlockPos root, BlockPos railPos) {
        for (Map.Entry<BlockPos, List<BlockPos>> entry : rolledRails.entrySet()) {
            if (entry.getValue().contains(root)) {
                return entry.getKey();
            }
        }
        return railPos;
    }

    /** Drops managed cells beyond the current Work Range that no longer hold anything, so they stop using the cap. */
    private void pruneManagedCells(BlockPos railPos) {
        if (managedCells.isEmpty()) {
            return;
        }
        for (BlockPos pos : ForestryCartRules.pruneRoots(railPos, scanDirection(railPos), workRange())) {
            if (managedCells.containsKey(pos) && !holdsManagedGrowth(pos)) {
                releaseReserve(managedCells.remove(pos));
            }
        }
    }

    private boolean holdsManagedGrowth(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        return isSaplingBlock(state) || state.getBlock() instanceof CropBlock || state.is(BlockTags.LOGS);
    }

    private boolean hasPendingWork(BlockPos root) {
        return worksTrees() && isLogBase(root) || tendsCrops() && isMatureCrop(root) || canPlantAt(root);
    }

    private boolean canPlantAt(BlockPos root) {
        if (!plantsSaplings() || managedCells.size() >= maxManagedCells() && !managedCells.containsKey(root)) {
            return false;
        }
        BlockState current = level().getBlockState(root);
        if (isPlantedBlock(current) || !current.canBeReplaced()) {
            return false;
        }
        ItemStack sapling = plantingSapling(root);
        return sapling.getItem() instanceof BlockItem blockItem
                && isPlantableBlock(blockItem.getBlock())
                && blockItem.getBlock().defaultBlockState().canSurvive(level(), root);
    }

    private boolean harvestInReach(BlockPos railPos, List<BlockPos> workRoots, BlockPos logBase) {
        return workRoots.contains(logBase) || rollingHarvest() && withinReach(railPos, logBase, workReach());
    }

    private static boolean withinReach(BlockPos railPos, BlockPos target, int reach) {
        return Math.abs(railPos.getX() - target.getX()) <= reach
                && Math.abs(railPos.getZ() - target.getZ()) <= reach
                && Math.abs(railPos.getY() - target.getY()) <= 1;
    }

    /** Seed Library replants a cell's remembered species, and leaves the cell empty until that species is in cargo. */
    private ItemStack plantingSapling(BlockPos root) {
        ManagedCell cell = managedCells.get(root);
        Item reservedItem = cell == null ? Items.AIR : BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(cell.saplingId()));
        if (reservedFor(cell, reservedItem) > 0) {
            return new ItemStack(reservedItem, reservedFor(cell, reservedItem));
        }
        if (cell == null || cell.saplingId().isEmpty() || !hasMasteryBehavior("SEED_LIBRARY")) {
            ItemStack survives = saplingStack(stack -> stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock().defaultBlockState().canSurvive(level(), root));
            return survives.isEmpty() ? saplingStack() : survives;
        }
        return saplingStack(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(cell.saplingId()));
    }

    /** Ancient Grove plants a clear 2x2 plot away from the rail when four matching giant saplings are in cargo. */
    private List<BlockPos> plantingPlot(BlockPos root, BlockPos railPos, ItemStack sapling, BlockState plantedState) {
        if (!hasMasteryBehavior("ANCIENT_GROVE") || !sapling.is(ModTags.Items.GIANT_SAPLINGS)
                || saplingCount(sapling.getItem()) + reservedFor(managedCells.get(root), sapling.getItem()) < GIANT_TREE_SAPLINGS) {
            return List.of(root);
        }
        Direction outward = ForestryCartRules.outwardDirection(railPos, root);
        Direction forward = scanDirection(railPos);
        if (outward == null || outward.getAxis() == forward.getAxis()) {
            return List.of(root);
        }
        List<BlockPos> plot = List.of(root, root.relative(outward), root.relative(forward), root.relative(forward).relative(outward));
        for (BlockPos pos : plot.subList(1, plot.size())) {
            BlockState state = level().getBlockState(pos);
            if (!state.canBeReplaced() || isSaplingBlock(state) || isRail(pos) || !plantedState.canSurvive(level(), pos)) {
                return List.of(root);
            }
        }
        return plot;
    }

    private boolean driveOnRails(BlockPos railPos, boolean seekingTransfer) {
        Direction nextDirection = nextRailDirection(railPos);
        if (nextDirection == null) {
            setWorkflowState(
                    WorkflowState.ROUTE_BLOCKED,
                    ForestryCartStationBlockEntity.STATUS_ROUTE_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_PATH_BLOCKED
            );
            setDeltaMovement(Vec3.ZERO);
            return false;
        }
        PathBlockage blockage = pathBlockage(railPos, nextDirection);
        if (blockage != null) {
            if (blockage.player()) {
                setWorkflowState(
                        WorkflowState.PATH_BLOCKED,
                        ForestryCartStationBlockEntity.STATUS_PATH_BLOCKED,
                        ForestryCartStationBlockEntity.ACTION_PLAYER_BLOCKING_PATH
                );
            } else if (blockage.state().is(BlockTags.LEAVES)) {
                tryClearPathLeaves(blockage);
            } else {
                setWorkflowState(
                        WorkflowState.PATH_BLOCKED,
                        ForestryCartStationBlockEntity.STATUS_PATH_BLOCKED,
                        ForestryCartStationBlockEntity.ACTION_PATH_BLOCKED
                );
            }
            setDeltaMovement(Vec3.ZERO);
            return false;
        }
        setRouteDirection(nextDirection);

        boolean powered = consumeEnergy(movementCharge(), false);
        double speed = powered ? (seekingTransfer ? transferSeekingCartSpeed() : poweredCartSpeed()) : unpoweredCartSpeed();
        Vec3 railVelocity = railVelocity(railPos, nextDirection, speed);
        setYRot(nextDirection.toYRot());
        setDeltaMovement(new Vec3(railVelocity.x, getDeltaMovement().y, railVelocity.z));
        hasImpulse = true;
        playMoveSound(powered, seekingTransfer);
        return true;
    }

    private Direction scanDirection(BlockPos railPos) {
        Direction nextDirection = nextRailDirection(railPos);
        return nextDirection == null ? routeDirection() : nextDirection;
    }

    private Direction nextRailDirection(BlockPos railPos) {
        RailShape shape = railShape(railPos);
        Direction routeDirection = routeDirection();
        if (shape != null) {
            Direction[] exits = railExits(shape);
            Direction incoming = routeDirection.getOpposite();
            if (contains(exits, routeDirection) && hasConnectedRailNeighbor(railPos, routeDirection)) {
                return routeDirection;
            }
            for (Direction exit : exits) {
                if (exit != incoming && hasConnectedRailNeighbor(railPos, exit)) {
                    return exit;
                }
            }
            for (Direction exit : exits) {
                if (hasConnectedRailNeighbor(railPos, exit)) {
                    return exit;
                }
            }
            return null;
        }

        if (hasConnectedRailNeighbor(railPos, routeDirection)) {
            return routeDirection;
        }
        Direction reversed = routeDirection.getOpposite();
        return hasConnectedRailNeighbor(railPos, reversed) ? reversed : null;
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private IEnergyStorage batteryEnergyStorage() {
        ItemStack stack = batteryCellStack();
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private RailShape railShape(BlockPos railPos) {
        BlockState state = level().getBlockState(railPos);
        if (state.getBlock() instanceof BaseRailBlock railBlock) {
            return railBlock.getRailDirection(state, level(), railPos, this);
        }
        return null;
    }

    private boolean isForestryWorkRail(BlockPos railPos) {
        RailShape shape = railShape(railPos);
        return shape != RailShape.SOUTH_EAST
                && shape != RailShape.SOUTH_WEST
                && shape != RailShape.NORTH_WEST
                && shape != RailShape.NORTH_EAST
                && shape != RailShape.ASCENDING_NORTH
                && shape != RailShape.ASCENDING_SOUTH
                && shape != RailShape.ASCENDING_EAST
                && shape != RailShape.ASCENDING_WEST;
    }

    private void alignVelocityToRail(BlockPos railPos) {
        Vec3 movement = getDeltaMovement();
        double speed = movement.horizontalDistance();
        if (speed <= 1.0E-5D) {
            return;
        }
        Direction nextDirection = nextRailDirection(railPos);
        if (nextDirection == null || pathBlockage(railPos, nextDirection) != null) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 railVelocity = railVelocity(railPos, nextDirection, speed);
        if (railVelocity.horizontalDistanceSqr() <= 1.0E-8D) {
            return;
        }
        setRouteDirection(nextDirection);
        setDeltaMovement(new Vec3(railVelocity.x, movement.y, railVelocity.z));
    }

    private Vec3 railVelocity(BlockPos railPos, Direction nextDirection, double speed) {
        RailShape shape = railShape(railPos);
        if (shape == null) {
            return new Vec3(nextDirection.getStepX() * speed, 0.0D, nextDirection.getStepZ() * speed);
        }
        Direction[] exits = railExits(shape);
        if (!contains(exits, nextDirection)) {
            return new Vec3(nextDirection.getStepX() * speed, 0.0D, nextDirection.getStepZ() * speed);
        }
        Direction entry = exits[0] == nextDirection ? exits[1] : exits[0];
        double x = nextDirection.getStepX() - entry.getStepX();
        double z = nextDirection.getStepZ() - entry.getStepZ();
        double length = Math.sqrt(x * x + z * z);
        if (length <= 0.0D) {
            return Vec3.ZERO;
        }
        return new Vec3(x / length * speed, 0.0D, z / length * speed);
    }

    private boolean hasConnectedRailNeighbor(BlockPos railPos, Direction direction) {
        return connectedRailNeighbor(railPos, direction) != null;
    }

    private BlockPos connectedRailNeighbor(BlockPos railPos, Direction direction) {
        BlockPos next = railPos.relative(direction);
        if (railConnectsBack(next, direction)) {
            return next;
        }
        BlockPos above = next.above();
        if (railConnectsBack(above, direction)) {
            return above;
        }
        BlockPos below = next.below();
        return railConnectsBack(below, direction) ? below : null;
    }

    private boolean railConnectsBack(BlockPos railPos, Direction direction) {
        RailShape shape = railShape(railPos);
        return shape != null && contains(railExits(shape), direction.getOpposite());
    }

    private PathBlockage pathBlockage(BlockPos railPos, Direction direction) {
        PathBlockage current = pathBlockageAt(railPos.above(), railPos);
        if (current != null) {
            return current;
        }
        BlockPos nextRail = connectedRailNeighbor(railPos, direction);
        return nextRail == null ? null : pathBlockageAt(nextRail.above(), nextRail);
    }

    private PathBlockage pathBlockageAt(BlockPos pos, BlockPos railPos) {
        PathBlockage playerBlockage = playerPathBlockageAt(railPos);
        if (playerBlockage != null) {
            return playerBlockage;
        }
        BlockState state = level().getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        VoxelShape collisionShape = state.getCollisionShape(level(), pos);
        if ((state.is(BlockTags.LEAVES) || !collisionShape.isEmpty()) && intersectsCartClearance(collisionShape, pos, railPos)) {
            return new PathBlockage(pos.immutable(), state, false);
        }
        return null;
    }

    private PathBlockage playerPathBlockageAt(BlockPos railPos) {
        AABB clearance = cartClearanceAt(railPos);
        List<Player> blockers = level().getEntitiesOfClass(Player.class, clearance, player -> player.isAlive() && !player.isSpectator());
        return blockers.isEmpty() ? null : new PathBlockage(railPos.above().immutable(), level().getBlockState(railPos.above()), true);
    }

    private boolean intersectsCartClearance(VoxelShape collisionShape, BlockPos pos, BlockPos railPos) {
        if (collisionShape.isEmpty()) {
            return false;
        }
        AABB clearance = cartClearanceAt(railPos);
        for (AABB box : collisionShape.toAabbs()) {
            if (box.move(pos).intersects(clearance)) {
                return true;
            }
        }
        return false;
    }

    private AABB cartClearanceAt(BlockPos railPos) {
        double halfWidth = getBbWidth() * 0.5D;
        double baseY = railPos.getY() + 0.1D;
        RailShape shape = railShape(railPos);
        double slopeRise = shape != null && shape.isAscending() ? 1.0D : 0.0D;
        return new AABB(
                railPos.getX() + 0.5D - halfWidth,
                baseY,
                railPos.getZ() + 0.5D - halfWidth,
                railPos.getX() + 0.5D + halfWidth,
                baseY + slopeRise + getBbHeight(),
                railPos.getZ() + 0.5D + halfWidth
        );
    }

    private boolean tryClearPathLeaves(PathBlockage blockage) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos pos = blockage.pos();
        BlockState state = serverLevel.getBlockState(pos);
        if (!state.is(BlockTags.LEAVES) || state.getDestroySpeed(serverLevel, pos) < 0.0F) {
            setWorkflowState(
                    WorkflowState.PATH_BLOCKED,
                    ForestryCartStationBlockEntity.STATUS_PATH_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_PATH_BLOCKED
            );
            return false;
        }

        int cost = cutEnergyCost();
        if (!consumeEnergy(cost, true)) {
            setWorkflowState(
                    WorkflowState.PATH_BLOCKED,
                    ForestryCartStationBlockEntity.STATUS_PATH_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_NO_POWER
            );
            pendingEnergyRequirement = Math.max(scanEnergyCost(), cost);
            return false;
        }

        FakePlayer fakePlayer = fakePlayer(serverLevel);
        fakePlayer.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        boolean cleared = harvestBlockIntoCart(serverLevel, fakePlayer, pos, state);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (!cleared) {
            setWorkflowState(
                    WorkflowState.PATH_BLOCKED,
                    ForestryCartStationBlockEntity.STATUS_PATH_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_PATH_BLOCKED
            );
            return false;
        }

        consumeEnergy(cost, false);
        setWorkflowState(
                WorkflowState.PATH_BLOCKED,
                ForestryCartStationBlockEntity.STATUS_PATH_BLOCKED,
                ForestryCartStationBlockEntity.ACTION_CLEARING_PATH
        );
        playWorkSound(ModSounds.FORESTRY_COMPANION_LEAF_CUT.get(), 0.42F, 0.96F, WORK_SOUND_COOLDOWN_TICKS);
        return true;
    }

    private static Direction[] railExits(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Direction[] { Direction.NORTH, Direction.SOUTH };
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Direction[] { Direction.WEST, Direction.EAST };
            case SOUTH_EAST -> new Direction[] { Direction.SOUTH, Direction.EAST };
            case SOUTH_WEST -> new Direction[] { Direction.SOUTH, Direction.WEST };
            case NORTH_WEST -> new Direction[] { Direction.NORTH, Direction.WEST };
            case NORTH_EAST -> new Direction[] { Direction.NORTH, Direction.EAST };
        };
    }

    private static boolean contains(Direction[] directions, Direction candidate) {
        for (Direction direction : directions) {
            if (direction == candidate) {
                return true;
            }
        }
        return false;
    }

    private static int speedMilli(double speed) {
        return Math.max(0, (int) Math.round(speed * 1000.0D));
    }

    private boolean isRail(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        return state.is(BlockTags.RAILS);
    }

    private void setWorkflowState(WorkflowState workflowState, int status, int currentAction) {
        this.workflowState = workflowState;
        setStatus(status);
        setCurrentAction(currentAction);
    }

    private void markNoPower(int energyRequirement) {
        pendingEnergyRequirement = Math.max(scanEnergyCost(), energyRequirement);
        setWorkflowState(
                WorkflowState.SEEKING_TRANSFER,
                ForestryCartStationBlockEntity.STATUS_NO_POWER,
                ForestryCartStationBlockEntity.ACTION_NO_POWER
        );
    }

    private void setStatus(int status) {
        this.status = status;
        entityData.set(DATA_VISUAL_STATUS, status);
        if (status != ForestryCartStationBlockEntity.STATUS_NO_POWER) {
            pendingEnergyRequirement = 0;
        }
    }

    private void setCurrentAction(int currentAction) {
        boolean changed = this.currentAction != currentAction;
        this.currentAction = currentAction;
        entityData.set(DATA_VISUAL_ACTION, currentAction);
        if (changed) {
            playActionTransitionSound(currentAction);
        }
    }

    private void playActionTransitionSound(int currentAction) {
        switch (currentAction) {
            case ForestryCartStationBlockEntity.ACTION_DOCK_TRANSFER -> playTransferSound();
            case ForestryCartStationBlockEntity.ACTION_SETUP_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_PATH_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_NO_POWER,
                    ForestryCartStationBlockEntity.ACTION_OUTPUT_FULL,
                    ForestryCartStationBlockEntity.ACTION_HARVEST_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_PLANTING_BLOCKED,
                    ForestryCartStationBlockEntity.ACTION_PLAYER_BLOCKING_PATH ->
                    playAlertSound(ModSounds.FORESTRY_COMPANION_BLOCKED.get(), 0.45F, 0.98F);
            default -> {
            }
        }
    }

    private void playMoveSound(boolean powered, boolean seekingTransfer) {
        int interval = seekingTransfer ? TRANSFER_SEEKING_MOVE_SOUND_INTERVAL_TICKS : MOVE_SOUND_INTERVAL_TICKS;
        if (!readyForSound(nextMoveSoundTick)) {
            return;
        }
        nextMoveSoundTick = tickCount + interval;
        float volume = powered ? 0.28F : 0.18F;
        float pitch = seekingTransfer ? 1.12F : powered ? 1.0F : 0.76F;
        playCartSound(ModSounds.FORESTRY_COMPANION_MOVE.get(), volume, pitch);
    }

    private void playScanSound() {
        if (!readyForSound(nextScanSoundTick)) {
            return;
        }
        nextScanSoundTick = tickCount + SCAN_SOUND_COOLDOWN_TICKS;
        playCartSound(ModSounds.FORESTRY_COMPANION_SCAN.get(), 0.36F, 1.0F);
    }

    private void playWorkSound(SoundEvent sound, float volume, float pitch, int cooldownTicks) {
        if (!readyForSound(nextWorkSoundTick)) {
            return;
        }
        nextWorkSoundTick = tickCount + cooldownTicks;
        playCartSound(sound, volume, pitch);
    }

    private void playTransferSound() {
        if (!readyForSound(nextTransferSoundTick)) {
            return;
        }
        nextTransferSoundTick = tickCount + TRANSFER_SOUND_COOLDOWN_TICKS;
        playCartSound(ModSounds.FORESTRY_COMPANION_TRANSFER.get(), 0.38F, 1.0F);
    }

    private void playAlertSound(SoundEvent sound, float volume, float pitch) {
        if (!readyForSound(nextAlertSoundTick)) {
            return;
        }
        nextAlertSoundTick = tickCount + ALERT_SOUND_COOLDOWN_TICKS;
        playCartSound(sound, volume, pitch);
    }

    private boolean readyForSound(int nextAllowedTick) {
        return !level().isClientSide && tickCount > 0 && tickCount >= nextAllowedTick;
    }

    private void playCartSound(SoundEvent sound, float volume, float pitch) {
        if (ForestryCompanionPassiveTree.mutesMachineSound(machineProgression)) {
            return;
        }
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
    }

    private enum WorkflowState {
        MANAGED,
        SETUP_BLOCKED,
        MOVING,
        SCANNING,
        CREATING_SNAPSHOT,
        HARVESTING_LEAVES,
        HARVESTING_LOGS,
        PLANTING,
        WORK_COOLDOWN,
        SEEKING_TRANSFER,
        DOCKED_TRANSFER,
        DOCKED_HELD,
        ROUTE_BLOCKED,
        PATH_BLOCKED,
        OFF_RAIL
    }

    private enum SnapshotCreationResult {
        CREATED,
        NO_TREE,
        TOO_LARGE
    }

    private enum WorkResult {
        WORKED,
        NEEDS_TRANSFER,
        BLOCKED_STOP,
        NO_WORK
    }

    /** Logs being cracked before a cut; not saved, so a reloaded cart restarts the break. */
    private static final class ActiveBreak {
        private final List<BlockPos> targets;
        private final int total;
        private int elapsed;

        private ActiveBreak(List<BlockPos> targets, int total) {
            this.targets = targets;
            this.total = Math.max(1, total);
        }

        private List<BlockPos> targets() {
            return targets;
        }

        private int total() {
            return total;
        }

        private int elapsed() {
            return elapsed;
        }

        private void tick() {
            elapsed++;
        }

        private boolean done() {
            return elapsed >= total;
        }
    }

    private static final class HarvestSnapshot {
        private final BlockPos logBase;
        private final List<BlockPos> leaves;
        private final List<BlockPos> logs;
        private final List<BlockPos> leafAnchors;

        private HarvestSnapshot(BlockPos logBase, List<BlockPos> leaves, List<BlockPos> logs, List<BlockPos> leafAnchors) {
            this.logBase = logBase.immutable();
            this.leaves = new ArrayList<>(leaves);
            this.logs = new ArrayList<>(logs);
            this.leafAnchors = new ArrayList<>(leafAnchors);
        }

        private static HarvestSnapshot load(CompoundTag tag) {
            List<BlockPos> logs = readPositions(tag.getList("Logs", Tag.TAG_COMPOUND));
            return new HarvestSnapshot(
                    readPos(tag.getCompound("LogBase")),
                    readPositions(tag.getList("Leaves", Tag.TAG_COMPOUND)),
                    logs,
                    tag.contains("LeafAnchors", Tag.TAG_LIST)
                            ? readPositions(tag.getList("LeafAnchors", Tag.TAG_COMPOUND))
                            : logs
            );
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.put("LogBase", writePos(logBase));
            tag.put("Leaves", writePositions(leaves));
            tag.put("Logs", writePositions(logs));
            tag.put("LeafAnchors", writePositions(leafAnchors));
            return tag;
        }

        private BlockPos logBase() {
            return logBase;
        }

        private List<BlockPos> leaves() {
            return leaves;
        }

        private List<BlockPos> logs() {
            return logs;
        }

        private List<BlockPos> leafAnchors() {
            return leafAnchors;
        }

        private boolean isEmpty() {
            return leaves.isEmpty() && logs.isEmpty();
        }

        private static CompoundTag writePos(BlockPos pos) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
            return tag;
        }

        private static BlockPos readPos(CompoundTag tag) {
            return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        }

        private static ListTag writePositions(List<BlockPos> positions) {
            ListTag list = new ListTag();
            for (BlockPos pos : positions) {
                list.add(writePos(pos));
            }
            return list;
        }

        private static List<BlockPos> readPositions(ListTag list) {
            List<BlockPos> positions = new ArrayList<>();
            for (int index = 0; index < list.size(); index++) {
                positions.add(readPos(list.getCompound(index)));
            }
            return positions;
        }
    }

    private record HarvestScan(List<BlockPos> logs, List<BlockPos> leaves, boolean tooLarge) {
        private static final HarvestScan EMPTY = new HarvestScan(List.of(), List.of(), false);
        private static final HarvestScan TOO_LARGE = new HarvestScan(List.of(), List.of(), true);
    }

    private record PathBlockage(BlockPos pos, BlockState state, boolean player) {
    }

    private static final class ManagedCell {
        private static final int STATE_PLANTED = 1;
        private static final int STATE_HARVESTING = 2;
        private static final int STATE_BLOCKED = 3;
        private final BlockPos root;
        private String saplingId;
        private int state;
        private long nextPulseTick;
        /** Plantables of {@link #saplingId} set aside from this cell's own harvest for its replant. */
        private int reserved;

        private ManagedCell(BlockPos root, String saplingId, int state) {
            this.root = root.immutable();
            this.saplingId = saplingId;
            this.state = state;
        }

        private static ManagedCell load(CompoundTag tag) {
            ManagedCell cell = new ManagedCell(
                    new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                    tag.getString("Sapling"),
                    tag.getInt("State")
            );
            cell.reserved = Math.max(0, tag.getInt("Reserved"));
            return cell;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", root.getX());
            tag.putInt("Y", root.getY());
            tag.putInt("Z", root.getZ());
            tag.putString("Sapling", saplingId);
            tag.putInt("State", state);
            if (reserved > 0) {
                tag.putInt("Reserved", reserved);
            }
            return tag;
        }

        private BlockPos root() {
            return root;
        }

        private int state() {
            return state;
        }

        private void setState(int state) {
            this.state = state;
        }

        private String saplingId() {
            return saplingId;
        }

        private void setSaplingId(String saplingId) {
            this.saplingId = saplingId;
        }

        private int reserved() {
            return reserved;
        }

        private void setReserved(int reserved) {
            this.reserved = Math.max(0, reserved);
        }

        private long nextPulseTick() {
            return nextPulseTick;
        }

        private void setNextPulseTick(long nextPulseTick) {
            this.nextPulseTick = nextPulseTick;
        }
    }
    @Override public MachineMasteryFamily masteryFamily() { return MachineMasteryFamily.FORESTRY; }
    /** The cart has no chassis stage and needs no particular Gear to ascend; crafting Seal I is its gate. */
    @Override public int ascendancyEntryStage() { return AscendancyCatalog.ENTRY_STAGE; }

}
