package com.rngtech.rpg.workorder;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The Work Orders loaded into one machine: up to {@value #SLOTS}, and the index of the active one, or -1 when none is
 * active. A finished chip stays in its bay until the player takes it out, and is never active. Every change returns a new
 * value; an invalid change returns this one.
 */
public record MachineWorkOrders(List<LoadedWorkOrder> orders, int active) {
    public static final int SLOTS = 3;
    public static final int NONE = -1;
    public static final MachineWorkOrders EMPTY = new MachineWorkOrders(List.of(), NONE);
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Decodes each order on its own, so one order that no longer decodes is dropped and the others stay. */
    private static final Codec<List<LoadedWorkOrder>> ORDERS_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<List<LoadedWorkOrder>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getStream(input).map(stream -> {
                List<LoadedWorkOrder> loaded = new ArrayList<>();
                stream.forEach(value -> LoadedWorkOrder.CODEC.parse(ops, value)
                        .resultOrPartial(error -> LOGGER.warn("Dropping a loaded Work Order that no longer decodes: {}", error))
                        .ifPresent(loaded::add));
                return Pair.of(loaded, input);
            });
        }

        @Override
        public <T> DataResult<T> encode(List<LoadedWorkOrder> input, DynamicOps<T> ops, T prefix) {
            return LoadedWorkOrder.CODEC.listOf().encode(input, ops, prefix);
        }
    };

    public static final Codec<MachineWorkOrders> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ORDERS_CODEC.optionalFieldOf("orders", List.of()).forGetter(MachineWorkOrders::orders),
            Codec.INT.optionalFieldOf("active", NONE).forGetter(MachineWorkOrders::active)
    ).apply(instance, MachineWorkOrders::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineWorkOrders> STREAM_CODEC = StreamCodec.composite(
            LoadedWorkOrder.STREAM_CODEC.apply(ByteBufCodecs.list(SLOTS)), MachineWorkOrders::orders,
            ByteBufCodecs.VAR_INT, MachineWorkOrders::active,
            MachineWorkOrders::new);

    public MachineWorkOrders {
        orders = List.copyOf(orders.size() > SLOTS ? orders.subList(0, SLOTS) : orders);
        active = active >= 0 && active < orders.size() && !orders.get(active).fulfilled() ? active : NONE;
    }

    public boolean isEmpty() { return orders.isEmpty(); }
    public boolean full() { return orders.size() >= SLOTS; }

    /** The active order, or null. */
    public LoadedWorkOrder activeOrder() {
        return active == NONE ? null : orders.get(active);
    }

    /** Loads {@code order} into the next free slot. The first order loaded into a machine with none active becomes active. */
    public MachineWorkOrders withLoaded(LoadedWorkOrder order) {
        if (full()) { return this; }
        List<LoadedWorkOrder> next = new ArrayList<>(orders);
        next.add(order);
        return new MachineWorkOrders(next, active == NONE ? next.size() - 1 : active);
    }

    /** Makes slot {@code index} active, or none with {@link #NONE}. Every order keeps its progress. A finished chip stays inactive. */
    public MachineWorkOrders withActive(int index) {
        if (index != NONE && (index < 0 || index >= orders.size() || orders.get(index).fulfilled())) { return this; }
        return new MachineWorkOrders(orders, index);
    }

    /** Removes slot {@code index}. Removing the active order leaves none active. */
    public MachineWorkOrders without(int index) {
        if (index < 0 || index >= orders.size()) { return this; }
        List<LoadedWorkOrder> next = new ArrayList<>(orders);
        next.remove(index);
        int nextActive = active == index ? NONE : active > index ? active - 1 : active;
        return new MachineWorkOrders(next, nextActive);
    }

    /** Without the Rush chips whose deadline passed at {@code gameTime}: they are lost, and nothing is returned. */
    public MachineWorkOrders withoutExpired(long gameTime) {
        MachineWorkOrders next = this;
        for (int index = orders.size() - 1; index >= 0; index--) {
            if (orders.get(index).expired(gameTime)) { next = next.without(index); }
        }
        return next;
    }

    /** Replaces slot {@code index} and keeps which order is active. */
    public MachineWorkOrders withReplaced(int index, LoadedWorkOrder order) {
        if (index < 0 || index >= orders.size()) { return this; }
        List<LoadedWorkOrder> next = new ArrayList<>(orders);
        next.set(index, order);
        return new MachineWorkOrders(next, active);
    }
}
