package com.rngtech.content.item;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.blockentity.DormantMachineBlockEntity;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.dormant.DormantCore;
import com.rngtech.content.dormant.DormantPhase;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.dormant.DormantSoul;
import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.SoulTransfer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.OptionalInt;

/**
 * Absorbs one machine's whole Mastery, then releases it into another machine of the same family. The absorb can fail
 * by the recorded calibration stability; the release always succeeds and uses the item up.
 */
public class SoulTransferItem extends Item {
    public SoulTransferItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) { return InteractionResult.PASS; }
        var blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof MachineMasteryHost) && !(blockEntity instanceof DormantMachineBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (!player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack)) { return InteractionResult.FAIL; }
        if (blockEntity instanceof DormantMachineBlockEntity dormant) { return useOnDormant(player, stack, dormant.core()); }
        return useOnHost(player, stack, (MachineMasteryHost) blockEntity, context.getClickedPos());
    }

    /**
     * Absorbs an awake Dormant Machine's soul. The charge records the site's release stage. A failed absorb always uses
     * the Soul Transfer up; what it does to the machine is the pack's {@code dormantMachines.failedAbsorb} choice.
     */
    public static InteractionResult useOnDormant(Player player, ItemStack stack, DormantCore dormant) {
        if (player.level().isClientSide) { return InteractionResult.SUCCESS; }
        if (player.isSpectator()) { return InteractionResult.FAIL; }
        BlockPos pos = dormant.sitePos();
        DormantSoul soul = dormant.soul();
        if (stack.has(ModDataComponents.SOUL_CHARGE.get())) {
            message(player, Component.translatable("rngtech.soul_transfer.message.dormant_full"));
            return InteractionResult.FAIL;
        }
        if (soul == null || dormant.phase() == DormantPhase.SPENT) {
            message(player, Component.translatable("rngtech.soul_transfer.message.dormant_spent"));
            return InteractionResult.FAIL;
        }
        if (dormant.phase() != DormantPhase.AWAKE) {
            message(player, Component.translatable("rngtech.soul_transfer.message.dormant_asleep"));
            return InteractionResult.FAIL;
        }
        if (player.getRandom().nextInt(100) >= successChance(stack)) {
            stack.consume(1, player);
            playSound(player, pos, SoundEvents.ITEM_BREAK);
            switch (RNGTechConfig.DORMANT_FAILED_ABSORB.get()) {
                case NOTHING -> {
                    MachineProgressionState state = soul.state();
                    MachineProgressionState damaged = SoulTransfer.afterFailure(state, RNGTechConfig.SOUL_TRANSFER_FAILURE_XP_LOSS.get(),
                            RNGTechConfig.SOUL_TRANSFER_FAILURE_LEVEL_LOSS.get(), candidate -> true);
                    dormant.setSoulState(damaged);
                    message(player, damaged.level() == state.level() ? Component.translatable("rngtech.soul_transfer.message.failed")
                            : Component.translatable("rngtech.soul_transfer.message.failed_loss", state.level(), damaged.level()));
                }
                case HUSK -> {
                    dormant.spend();
                    message(player, Component.translatable("rngtech.soul_transfer.message.dormant_lost"));
                }
                case EXPLODE -> {
                    dormant.spend();
                    player.level().explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4.0F, Level.ExplosionInteraction.BLOCK);
                    message(player, Component.translatable("rngtech.soul_transfer.message.dormant_exploded"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        stack.set(ModDataComponents.SOUL_CHARGE.get(), new SoulTransfer.Charge(soul.family(), soul.state(), soul.releaseStage()));
        dormant.spend();
        playSound(player, pos, SoundEvents.SOUL_ESCAPE.value());
        message(player, Component.translatable("rngtech.soul_transfer.message.absorbed", soul.level(), familyName(soul.family())));
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult useOnHost(Player player, ItemStack stack, MachineMasteryHost host, BlockPos pos) {
        if (player.level().isClientSide) { return InteractionResult.SUCCESS; }
        if (player.isSpectator()) { return InteractionResult.FAIL; }
        SoulTransfer.Charge charge = stack.get(ModDataComponents.SOUL_CHARGE.get());
        return charge == null ? absorb(player, stack, host, pos) : release(player, stack, host, pos, charge);
    }

    public static int successChance(ItemStack stack) {
        CalibrationState calibration = stack.get(ModDataComponents.SOURCE_CALIBRATION.get());
        return SoulTransfer.successChance(calibration == null ? OptionalInt.empty() : OptionalInt.of(calibration.stability()),
                RNGTechConfig.SOUL_TRANSFER_ALWAYS_SUCCEEDS.get());
    }

    private static InteractionResult absorb(Player player, ItemStack stack, MachineMasteryHost host, BlockPos pos) {
        MachineProgressionState state = host.masteryState();
        Component family = familyName(host.masteryFamily());
        if (!SoulTransfer.hasSoul(state)) {
            message(player, Component.translatable("rngtech.soul_transfer.message.empty", family));
            return InteractionResult.FAIL;
        }
        if (player.getRandom().nextInt(100) >= successChance(stack)) {
            MachineProgressionState damaged = SoulTransfer.afterFailure(state, RNGTechConfig.SOUL_TRANSFER_FAILURE_XP_LOSS.get(),
                    RNGTechConfig.SOUL_TRANSFER_FAILURE_LEVEL_LOSS.get(), host::masteryGearAllows);
            if (!damaged.equals(state)) {
                host.setMachineProgression(damaged);
                host.masteryChanged();
            }
            stack.consume(1, player);
            playSound(player, pos, SoundEvents.ITEM_BREAK);
            message(player, damaged.equals(state) ? Component.translatable("rngtech.soul_transfer.message.failed")
                    : damaged.level() == state.level() ? Component.translatable("rngtech.soul_transfer.message.failed_xp", state.xp() - damaged.xp())
                    : Component.translatable("rngtech.soul_transfer.message.failed_loss", state.level(), damaged.level()));
            return InteractionResult.SUCCESS;
        }
        stack.set(ModDataComponents.SOUL_CHARGE.get(), new SoulTransfer.Charge(host.masteryFamily(), state));
        host.setMachineProgression(MachineProgressionState.EMPTY.forFamily(host.masteryFamily()));
        host.masteryChanged();
        playSound(player, pos, SoundEvents.SOUL_ESCAPE.value());
        message(player, Component.translatable("rngtech.soul_transfer.message.absorbed", state.level(), family));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult release(Player player, ItemStack stack, MachineMasteryHost host, BlockPos pos, SoulTransfer.Charge charge) {
        MachineProgressionState target = host.masteryState();
        switch (SoulTransfer.release(charge, host.masteryFamily(), host.ascendancyEntryStage(), target, player.isShiftKeyDown())) {
            case WRONG_FAMILY -> {
                message(player, Component.translatable("rngtech.soul_transfer.message.wrong_family", familyName(charge.family())));
                return InteractionResult.FAIL;
            }
            case ENTRY_STAGE -> {
                message(player, Component.translatable("rngtech.soul_transfer.message.entry_stage",
                        AscendancyCatalog.ENTRY_STAGE));
                return InteractionResult.FAIL;
            }
            case RELEASE_STAGE -> {
                message(player, Component.translatable("rngtech.soul_transfer.message.release_stage", charge.effectiveReleaseStage()));
                return InteractionResult.FAIL;
            }
            case CONFIRM_OVERWRITE -> {
                message(player, Component.translatable("rngtech.soul_transfer.message.overwrite", target.level())
                        .withStyle(ChatFormatting.GOLD));
                return InteractionResult.FAIL;
            }
            case RELEASE -> { }
        }
        // Always used up, also in Creative, so a soul never lands in two machines.
        stack.shrink(1);
        host.applyMasteryState(charge.state());
        playSound(player, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE);
        message(player, SoulTransfer.hasSoul(target)
                ? Component.translatable("rngtech.soul_transfer.message.replaced", charge.state().level(), target.level())
                : Component.translatable("rngtech.soul_transfer.message.released", charge.state().level()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.SOUL_CHARGE.get()) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        SoulTransfer.Charge charge = stack.get(ModDataComponents.SOUL_CHARGE.get());
        if (charge == null) {
            tooltipComponents.add(Component.translatable("rngtech.soul_transfer.tooltip.chance", successChance(stack)).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("rngtech.soul_transfer.tooltip.absorb").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        Component family = familyName(charge.family());
        tooltipComponents.add(Component.translatable("rngtech.soul_transfer.tooltip.holds", charge.state().level(), family)
                .withStyle(ChatFormatting.AQUA));
        if (charge.effectiveReleaseStage() > 0) {
            tooltipComponents.add(Component.translatable("rngtech.soul_transfer.tooltip.release_stage", charge.effectiveReleaseStage())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (charge.state().sealTiers() > 0) {
            tooltipComponents.add(Component.translatable("rngtech.soul_transfer.tooltip.seals", charge.state().sealTiers()).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("rngtech.soul_transfer.tooltip.release", family).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component familyName(MachineMasteryFamily family) {
        return Component.translatable(family.translationKey());
    }

    private static void message(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private static void playSound(Player player, BlockPos pos, SoundEvent sound) {
        player.level().playSound(null, pos, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
