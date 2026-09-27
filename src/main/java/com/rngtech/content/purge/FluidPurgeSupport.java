package com.rngtech.content.purge;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidType;

public final class FluidPurgeSupport {
    public static final int PURGE_AMOUNT = FluidType.BUCKET_VOLUME;
    public static final int BUTTON_PURGE_BASE = 9000;
    private static final int BUTTON_PURGE_LIMIT = 128;

    private FluidPurgeSupport() {
    }

    public static int buttonId(int targetId) {
        return BUTTON_PURGE_BASE + targetId;
    }

    public static boolean isPurgeButton(int id) {
        int targetId = id - BUTTON_PURGE_BASE;
        return targetId >= 0 && targetId < BUTTON_PURGE_LIMIT;
    }

    public static int targetIdFromButton(int id) {
        return id - BUTTON_PURGE_BASE;
    }

    public static boolean handleMenuButton(Player player, PurgeableFluidStorage storage, int id) {
        if (!isPurgeButton(id)) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        showFeedback(player, storage.purgeFluid(targetIdFromButton(id), PURGE_AMOUNT));
        return true;
    }

    public static void purgeFromItem(Player player, PurgeableFluidStorage storage, boolean preferOutputs) {
        showFeedback(player, storage.purgeFirst(preferOutputs, PURGE_AMOUNT));
    }

    public static void showUnsupported(Player player) {
        player.displayClientMessage(Component.translatable("rngtech.purge.unsupported"), true);
    }

    public static void showFeedback(Player player, FluidPurgeResult result) {
        if (!result.supported()) {
            showUnsupported(player);
            return;
        }
        if (!result.success()) {
            player.displayClientMessage(Component.translatable("rngtech.purge.empty"), true);
            return;
        }
        player.displayClientMessage(
                Component.translatable(
                        "rngtech.purge.success",
                        result.amount(),
                        result.fluid().getHoverName(),
                        result.targetName()
                ),
                true
        );
    }
}
