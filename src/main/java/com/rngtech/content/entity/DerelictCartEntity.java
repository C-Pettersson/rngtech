package com.rngtech.content.entity;

import com.rngtech.content.dormant.DormantCore;
import com.rngtech.content.dormant.DormantPhase;
import com.rngtech.content.item.SoulTransferItem;
import com.rngtech.content.menu.DormantMachineMenu;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

/**
 * The Dormant Machine at an Abandoned Cart Siding: a derelict Forestry Companion that holds a soul. It never moves,
 * takes power only from the Battery Cells on its screen, and breaks into Scrap once spent.
 */
public class DerelictCartEntity extends Entity implements MenuProvider, DormantCore.Host {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DerelictCartEntity.class, EntityDataSerializers.INT);

    private final DormantCore core = new DormantCore(this);
    private BlockPos anchor;

    public DerelictCartEntity(EntityType<? extends DerelictCartEntity> type, Level level) {
        super(type, level);
    }

    public DormantCore core() {
        return core;
    }

    /** Fixes the site position the soul's seed and the site index use. */
    public void anchor(BlockPos pos) {
        this.anchor = pos.immutable();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, DormantPhase.ASLEEP.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (anchor == null) { anchor = blockPosition(); }
            core.tick(server);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof SoulTransferItem) {
            return SoulTransferItem.useOnDormant(player, held, core);
        }
        if (phase() == DormantPhase.SPENT) { return InteractionResult.PASS; }
        if (!level().isClientSide) {
            player.openMenu(this, buffer -> {
                buffer.writeBoolean(true);
                buffer.writeVarInt(getId());
            });
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** It cannot be broken while it holds a soul, except in Creative; a spent cart breaks into Scrap and drops its cells. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) { return false; }
        boolean creative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
        if (phase() != DormantPhase.SPENT && !creative) { return false; }
        for (int slot = 0; slot < DormantCore.CELL_SLOTS; slot++) {
            spawnAtLocation(core.cells().getStackInSlot(slot).copy());
        }
        if (phase() == DormantPhase.SPENT && !creative) { spawnAtLocation(new ItemStack(ModItems.materialItem("scrap").get())); }
        discard();
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public Level hostLevel() {
        return level();
    }

    @Override
    public BlockPos sitePos() {
        return anchor == null ? blockPosition() : anchor;
    }

    @Override
    public DormantPhase phase() {
        int ordinal = entityData.get(PHASE);
        return DormantPhase.values()[Math.clamp(ordinal, 0, DormantPhase.values().length - 1)];
    }

    @Override
    public void setPhase(DormantPhase phase) {
        entityData.set(PHASE, phase.ordinal());
    }

    @Override
    public void changed() {
    }

    @Override
    public boolean entitySite() {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return DormantMachineMenu.siteName(core.site());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DormantMachineMenu(containerId, inventory, core, core.menuData(),
                viewer -> isAlive() && phase() != DormantPhase.SPENT && viewer.canInteractWithEntity(this, 4.0D));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        core.load(tag, registryAccess());
        anchor = NbtUtils.readBlockPos(tag, "Anchor").orElse(null);
        if (tag.contains("Phase")) { setPhase(DormantPhase.values()[Math.clamp(tag.getInt("Phase"), 0, DormantPhase.values().length - 1)]); }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        core.save(tag, registryAccess());
        if (anchor != null) { tag.put("Anchor", NbtUtils.writeBlockPos(anchor)); }
        tag.putInt("Phase", phase().ordinal());
    }
}
