package com.rngtech.client.screen;

import com.rngtech.content.network.StatBreakdownRequestPayload;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.StatBreakdown;
import com.rngtech.rpg.StatBreakdownText;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The open menu's stat breakdowns from the server, refreshed about once a second while a player reads them. */
public final class StatBreakdownCache {
    private static final long REFRESH_MILLIS = 1000L;

    private static int containerId = -1;
    private static long requestedAt = Long.MIN_VALUE;
    private static Map<MachineStat, StatBreakdown> breakdowns;

    private StatBreakdownCache() {
    }

    static List<Component> tooltip(MachineStat stat) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return List.of();
        }
        int openContainerId = minecraft.player.containerMenu.containerId;
        if (openContainerId != containerId) {
            containerId = openContainerId;
            breakdowns = null;
            requestedAt = Long.MIN_VALUE;
        }
        long now = Util.getMillis();
        if (now - requestedAt >= REFRESH_MILLIS) {
            requestedAt = now;
            PacketDistributor.sendToServer(new StatBreakdownRequestPayload(openContainerId));
        }
        if (breakdowns == null) {
            return List.of(Component.translatable("rngtech.stat.breakdown.pending").withStyle(ChatFormatting.GRAY));
        }
        StatBreakdown breakdown = breakdowns.get(MachineStatAccumulator.accumulationStat(stat));
        if (breakdown == null) {
            return List.of(
                    Component.translatable(stat.translationKey()),
                    Component.translatable("rngtech.stat.breakdown.none").withStyle(ChatFormatting.GRAY)
            );
        }
        return StatBreakdownText.lines(breakdown);
    }

    public static void accept(int payloadContainerId, List<StatBreakdown> payloadBreakdowns) {
        if (payloadContainerId != containerId) {
            return;
        }
        Map<MachineStat, StatBreakdown> received = new EnumMap<>(MachineStat.class);
        for (StatBreakdown breakdown : payloadBreakdowns) {
            received.put(breakdown.stat(), breakdown);
        }
        breakdowns = received;
    }
}
