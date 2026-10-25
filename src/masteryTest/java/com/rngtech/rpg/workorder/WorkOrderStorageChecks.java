package com.rngtech.rpg.workorder;

import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Storage checks for Work Order item data and the orders loaded into a machine, run from {@code MasteryChecks}. */
public final class WorkOrderStorageChecks {
    private static int checks;

    private WorkOrderStorageChecks() { }

    public static int run() {
        checks = 0;
        clamps();
        orderSaves();
        machineSaves();
        slots();
        masteryKeepsNoOrder();
        return checks;
    }

    private static void clamps() {
        var order = new WorkOrder(null, 9, objective(0), -3, -1, -10, -1);
        require(order.family().isEmpty() && order.tier() == WorkOrder.MAX_TIER && order.objective().totalTarget() == 1,
                "family, tier, and target stay in range");
        require(order.minStage() == 0 && order.minMasteryLevel() == 0 && order.bounty() == 0 && order.prestige() == 0,
                "negative requirements, Bounty, and Prestige clamp to zero");
        var loaded = new LoadedWorkOrder(order(1), null, List.of(400L), 3, -5);
        require(loaded.totalProgress() == 256 && loaded.fulfilled() && loaded.bestStreak() == 256, "progress never passes the target");
        require(loaded.traits().equals(MachineTraits.EMPTY) && !loaded.rushed(), "missing traits read as none, and no deadline means no Rush");
        var rushed = new LoadedWorkOrder(order(1), MachineTraits.EMPTY, List.of(), 0, 1_000);
        require(rushed.rushed() && !rushed.expired(999) && rushed.expired(1_000), "a Rush order expires at its deadline");
        require(WorkOrder.tierStage(1) == 4 && WorkOrder.tierStage(5) == 8, "the tier stages run 4 to 8");
    }

    private static void orderSaves() {
        WorkOrder order = new WorkOrder("crusher", 3, objective(320), 6, 60, 135, 1);
        roundTrip(WorkOrder.CODEC, WorkOrder.STREAM_CODEC, order, "an order");
        JsonObject minimal = new JsonObject();
        minimal.addProperty("family", "furnace");
        minimal.addProperty("tier", 2);
        minimal.add("objective", WorkObjective.CODEC.encodeStart(JsonOps.INSTANCE, objective(64)).getOrThrow());
        WorkOrder loaded = WorkOrder.CODEC.parse(JsonOps.INSTANCE, minimal).getOrThrow();
        require(loaded.minStage() == 0 && loaded.minMasteryLevel() == 0 && loaded.bounty() == WorkOrder.BASE_BOUNTY && loaded.prestige() == 0,
                "an order with only family, tier, and objective loads with base values");
    }

    private static void machineSaves() {
        roundTrip(MachineWorkOrders.CODEC, MachineWorkOrders.STREAM_CODEC, MachineWorkOrders.EMPTY, "a machine with no orders");
        MachineWorkOrders full = MachineWorkOrders.EMPTY
                .withLoaded(new LoadedWorkOrder(order(1), magic(), List.of(17L), 40, 0))
                .withLoaded(new LoadedWorkOrder(order(2), MachineTraits.EMPTY, List.of(3L), 3, 9_000))
                .withLoaded(new LoadedWorkOrder(order(3), MachineTraits.EMPTY))
                .withActive(2);
        roundTrip(MachineWorkOrders.CODEC, MachineWorkOrders.STREAM_CODEC, full, "a machine with three orders");

        JsonObject json = MachineWorkOrders.CODEC.encodeStart(JsonOps.INSTANCE, full).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("orders").get(1).getAsJsonObject().remove("order");
        MachineWorkOrders salvaged = MachineWorkOrders.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        require(salvaged.orders().size() == 2 && salvaged.orders().get(0).equals(full.orders().get(0))
                && salvaged.orders().get(1).equals(full.orders().get(2)), "an order that no longer decodes is dropped and the others stay");

        json.addProperty("active", 7);
        require(MachineWorkOrders.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().active() == MachineWorkOrders.NONE,
                "an active index past the loaded orders means none is active");
        List<LoadedWorkOrder> four = new ArrayList<>(full.orders());
        four.add(new LoadedWorkOrder(order(4), MachineTraits.EMPTY));
        require(new MachineWorkOrders(four, 0).orders().size() == MachineWorkOrders.SLOTS, "a machine never holds more than three orders");
    }

    private static void slots() {
        var first = new LoadedWorkOrder(order(1), MachineTraits.EMPTY);
        var second = new LoadedWorkOrder(order(2), MachineTraits.EMPTY);
        var third = new LoadedWorkOrder(order(3), MachineTraits.EMPTY);
        var one = MachineWorkOrders.EMPTY.withLoaded(first);
        require(one.active() == 0 && one.activeOrder().equals(first), "the first order loaded becomes active");
        var two = one.withLoaded(second);
        require(two.active() == 0 && two.orders().size() == 2, "a later order loads inactive");
        var full = two.withLoaded(third);
        require(full.full() && full.withLoaded(first) == full, "a full machine refuses a fourth order");

        var progressed = full.withReplaced(0, first.withPartProgress(0, 30)).withActive(1).withReplaced(1, second.withPartProgress(0, 5)).withActive(0);
        require(progressed.orders().get(0).totalProgress() == 30 && progressed.orders().get(1).totalProgress() == 5 && progressed.active() == 0,
                "switching the active order keeps every order's progress");
        require(progressed.withActive(MachineWorkOrders.NONE).activeOrder() == null && progressed.withActive(5) == progressed,
                "a machine can have no active order, and a missing slot cannot be active");

        var dropActive = progressed.without(0);
        require(dropActive.active() == MachineWorkOrders.NONE && dropActive.orders().size() == 2, "removing the active order leaves none active");
        var dropBefore = progressed.withActive(2).without(0);
        require(dropBefore.active() == 1 && dropBefore.activeOrder().equals(third), "removing an earlier order keeps the same order active");
        require(progressed.without(9) == progressed && progressed.withReplaced(9, first) == progressed, "a missing slot changes nothing");
    }

    /** A Mastery save from the earlier design, with an order inside it, still loads; the order is ignored. */
    private static void masteryKeepsNoOrder() {
        var state = MachineProgressionState.EMPTY.forFamily(MachineMasteryFamily.CRUSHER).withAddedXp(MachineProgressionState.xpForLevel(40));
        JsonObject json = MachineProgressionState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow().getAsJsonObject();
        JsonObject old = new JsonObject();
        old.addProperty("tier", 1);
        json.add("work_order", old);
        require(MachineProgressionState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(state),
                "a Mastery save from the earlier Work Order design loads unchanged");
    }

    private static <T> void roundTrip(Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> stream, T value, String label) {
        require(codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow()).getOrThrow().equals(value),
                label + " survives a JSON save");
        require(codec.parse(NbtOps.INSTANCE, codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow()).getOrThrow().equals(value),
                label + " survives an NBT save");
        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            stream.encode(wire, value);
            require(value.equals(stream.decode(wire)) && wire.readableBytes() == 0, label + " survives the network codec");
        } finally {
            wire.release();
        }
    }

    private static MachineTraits magic() {
        var condition = new MachineModifier(ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.LESS, 20);
        return new MachineTraits(Rarity.MAGIC, 3, List.of(condition));
    }

    private static WorkOrder order(int tier) {
        return new WorkOrder("crusher", tier, objective(256), WorkOrder.tierStage(tier), 0, WorkOrder.BASE_BOUNTY, 0);
    }

    private static WorkObjective objective(long target) {
        return new WorkObjective(ResourceLocation.fromNamespaceAndPath("rngtech", "crusher_stage"),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.STAGE, 6, "", target));
    }

    private static void require(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
}
