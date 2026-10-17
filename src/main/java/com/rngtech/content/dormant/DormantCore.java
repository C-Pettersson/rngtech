package com.rngtech.content.dormant;

import com.rngtech.RNGTech;
import com.rngtech.RNGTechConfig;
import com.rngtech.content.blockentity.MachineInfoSnapshot;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.rpg.dormant.DormantSite;
import com.rngtech.rpg.dormant.DormantSoul;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Everything a Dormant Machine keeps, shared by the block and the derelict Forestry Companion: one rolled Mastery soul,
 * the wake charge filled from FE and up to nine Battery Cells, and the menu data the screen draws.
 */
public final class DormantCore {
    public static final int CELL_SLOTS = 9;

    public static final int DATA_CHARGE = 0;
    public static final int DATA_WAKE_FE = 1;
    public static final int DATA_LAST_INPUT = 2;
    public static final int DATA_WAKE_INPUT = 3;
    public static final int DATA_PHASE = 4;
    public static final int DATA_LEVEL = 5;
    public static final int DATA_RELEASE_STAGE = 6;
    public static final int DATA_FAMILY = 7;
    public static final int DATA_FAILED_ABSORB = 8;
    public static final int DATA_COUNT = 9;

    /** The block or entity a core lives in. */
    public interface Host {
        Level hostLevel();

        /** The site's position: the block, or where the derelict cart was placed. */
        BlockPos sitePos();

        DormantPhase phase();

        void setPhase(DormantPhase phase);

        void changed();

        boolean entitySite();
    }

    private final Host host;
    private final ItemStackHandler cells = new ItemStackHandler(CELL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return BatteryCellItem.isBatteryCell(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            host.changed();
        }
    };
    private final IItemHandler automationCells = new AutomationCells(false);
    private final IItemHandler bottomCells = new AutomationCells(true);
    private final IEnergyStorage energy = new WakeEnergy();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_CHARGE -> clampInt(charge);
                case DATA_WAKE_FE -> soul == null ? 0 : clampInt(soul.wakeFe());
                case DATA_LAST_INPUT -> lastTickInput;
                case DATA_WAKE_INPUT -> soul == null ? 0 : soul.wakeInput();
                case DATA_PHASE -> host.phase().ordinal();
                case DATA_LEVEL -> soul == null ? 0 : soul.level();
                case DATA_RELEASE_STAGE -> soul == null ? 0 : soul.releaseStage();
                case DATA_FAMILY -> soul == null ? -1 : soul.family().ordinal();
                case DATA_FAILED_ABSORB -> RNGTechConfig.DORMANT_FAILED_ABSORB.get().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private ResourceLocation site = DormantSiteCatalog.DEFAULT_SITE;
    private DormantSoul soul;
    private long charge;
    private boolean checkedIndex;
    private long inputTick = Long.MIN_VALUE;
    private int inputThisTick;
    private int lastTickInput;

    public DormantCore(Host host) {
        this.host = host;
    }

    public void tick(ServerLevel level) {
        if (!checkedIndex) {
            checkedIndex = true;
            DormantSiteIndex index = DormantSiteIndex.get(level.getServer());
            DormantSiteIndex.Entry entry = index.register(key(level), site, indexState(), host.entitySite());
            if (entry.pending()) {
                reroll(entry);
                index.applied(entry.pos());
            } else if (soul == null && host.phase() != DormantPhase.SPENT) {
                rollSoul(entry.salt());
            }
        }
        rollInputWindow(level.getGameTime());
        if (host.phase() == DormantPhase.ASLEEP && soul != null) {
            drainCells();
            if (charge >= soul.wakeFe()) { wake(); }
        }
    }

    /** Rolls a new soul from the site and {@code entry}'s salt, empties the wake charge, and puts the machine back to sleep. */
    public void reroll(DormantSiteIndex.Entry entry) {
        site = entry.site();
        charge = 0;
        soul = null;
        host.setPhase(DormantPhase.ASLEEP);
        rollSoul(entry.salt());
        host.changed();
    }

    private void rollSoul(long salt) {
        DormantSite definition = DormantSiteCatalog.get(site);
        if (definition == null || !(host.hostLevel() instanceof ServerLevel server)) {
            RNGTech.LOGGER.warn("Dormant Machine at {} names unknown site {}", host.sitePos(), site);
            return;
        }
        soul = DormantSoul.roll(definition, seed(server.getSeed(), host.sitePos(), site, salt));
        host.changed();
    }

    /** The world seed, position, site, and salt together pick the soul, so the same salt always rolls the same soul. */
    public static long seed(long worldSeed, BlockPos pos, ResourceLocation site, long salt) {
        long seed = worldSeed ^ pos.asLong() * 0x9E3779B97F4A7C15L ^ salt * 0xC2B2AE3D27D4EB4FL ^ site.hashCode() * 0x165667B19E3779F9L;
        return seed ^ seed >>> 31;
    }

    private void drainCells() {
        for (int slot = 0; slot < CELL_SLOTS && remainingInput() > 0; slot++) {
            ItemStack cell = cells.getStackInSlot(slot);
            IEnergyStorage storage = cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage == null) { continue; }
            int extracted = storage.extractEnergy(remainingInput(), false);
            if (extracted > 0) { addCharge(extracted); }
        }
    }

    private int remainingInput() {
        if (soul == null || host.phase() != DormantPhase.ASLEEP) { return 0; }
        long untilAwake = soul.wakeFe() - charge;
        return (int) Math.max(0, Math.min(untilAwake, soul.wakeInput() - (long) inputThisTick));
    }

    private void addCharge(int amount) {
        boolean first = charge == 0;
        charge += amount;
        inputThisTick += amount;
        if (first && host.hostLevel() instanceof ServerLevel server) {
            DormantSiteIndex.get(server.getServer()).mark(key(server), DormantSiteIndex.State.CHARGED);
        }
        host.changed();
    }

    private void rollInputWindow(long gameTime) {
        if (inputTick != gameTime) {
            lastTickInput = inputTick == gameTime - 1 ? inputThisTick : 0;
            inputTick = gameTime;
            inputThisTick = 0;
        }
    }

    private void wake() {
        host.setPhase(DormantPhase.AWAKE);
        host.hostLevel().playSound(null, host.sitePos(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Takes the soul away for good: the machine becomes a spent husk. */
    public void spend() {
        soul = null;
        charge = 0;
        host.setPhase(DormantPhase.SPENT);
        if (host.hostLevel() instanceof ServerLevel server) {
            DormantSiteIndex.get(server.getServer()).mark(key(server), DormantSiteIndex.State.SPENT);
        }
        host.changed();
    }

    /** Names the soul table a machine rolls from; a structure sets it before the first roll. */
    public void setSite(ResourceLocation site) {
        if (soul == null && site != null) {
            this.site = site;
            host.changed();
        }
    }

    public void setSoulState(MachineProgressionState state) {
        if (soul != null) {
            soul = soul.withState(state);
            host.changed();
        }
    }

    private DormantSiteIndex.State indexState() {
        return switch (host.phase()) {
            case SPENT -> DormantSiteIndex.State.SPENT;
            case AWAKE -> DormantSiteIndex.State.CHARGED;
            case ASLEEP -> charge > 0 ? DormantSiteIndex.State.CHARGED : DormantSiteIndex.State.UNTOUCHED;
        };
    }

    private net.minecraft.core.GlobalPos key(Level level) {
        return DormantSiteIndex.key(level, host.sitePos());
    }

    public DormantSoul soul() {
        return soul;
    }

    public long charge() {
        return charge;
    }

    public ResourceLocation site() {
        return site;
    }

    public DormantPhase phase() {
        return host.phase();
    }

    public BlockPos sitePos() {
        return host.sitePos();
    }

    public Level level() {
        return host.hostLevel();
    }

    public ItemStackHandler cells() {
        return cells;
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public IItemHandler itemHandler(Direction side) {
        return side == Direction.DOWN ? bottomCells : automationCells;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public MachineInfoSnapshot machineInfo() {
        MachineInfoSnapshot.Builder builder = MachineInfoSnapshot.builder("dormant_machine");
        if (soul == null) {
            return builder.status("rngtech.dormant_machine.status.spent").build();
        }
        builder.stage(soul.releaseStage()).energy(charge, soul.wakeFe(), lastTickInput);
        return switch (host.phase()) {
            case AWAKE -> builder.status("rngtech.dormant_machine.status.awake", soul.level()).build();
            case SPENT -> builder.status("rngtech.dormant_machine.status.spent").build();
            case ASLEEP -> builder.progress(clampInt(charge * 1000 / soul.wakeFe()), 1000)
                    .status("rngtech.dormant_machine.status.asleep", soul.level()).build();
        };
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Site", site.toString());
        tag.putLong("Charge", charge);
        tag.put("Cells", cells.serializeNBT(registries));
        if (soul != null) {
            DormantSoul.CODEC.encodeStart(NbtOps.INSTANCE, soul).result().ifPresent(encoded -> tag.put("Soul", encoded));
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Site")) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("Site"));
            site = parsed == null ? DormantSiteCatalog.DEFAULT_SITE : parsed;
        }
        charge = Math.max(0L, tag.getLong("Charge"));
        if (tag.contains("Cells")) { cells.deserializeNBT(registries, tag.getCompound("Cells")); }
        soul = tag.contains("Soul") ? DormantSoul.CODEC.parse(NbtOps.INSTANCE, tag.get("Soul")).result().orElse(null) : null;
    }

    private static int clampInt(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }

    private final class WakeEnergy implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            Level level = host.hostLevel();
            if (level == null) { return 0; }
            rollInputWindow(level.getGameTime());
            int accepted = Math.min(Math.max(0, toReceive), remainingInput());
            if (!simulate && accepted > 0) { addCharge(accepted); }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return clampInt(charge);
        }

        @Override
        public int getMaxEnergyStored() {
            return soul == null ? clampInt(charge) : clampInt(soul.wakeFe());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return soul != null && host.phase() == DormantPhase.ASLEEP;
        }
    }

    /** Cells go in from the top and sides; only empty cells come out, and only from below. */
    private final class AutomationCells implements IItemHandler {
        private final boolean bottom;

        private AutomationCells(boolean bottom) {
            this.bottom = bottom;
        }

        @Override
        public int getSlots() {
            return CELL_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return cells.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return bottom ? stack : cells.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!bottom) { return ItemStack.EMPTY; }
            ItemStack cell = cells.getStackInSlot(slot);
            IEnergyStorage storage = cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage != null && storage.getEnergyStored() > 0) { return ItemStack.EMPTY; }
            return cells.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !bottom && cells.isItemValid(slot, stack);
        }
    }
}
