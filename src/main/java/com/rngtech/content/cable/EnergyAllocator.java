package com.rngtech.content.cable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plans one tick of FE movement on one cable channel from what every connector can give and take, before anything
 * moves. The plan only depends on the ports and their order, never on which block entity ticked first.
 *
 * <p>FE moves in four steps, each from what is left after the one before:</p>
 * <ol>
 *     <li>Plain sources (generators, or anything not a storage block on a Both connector) feed plain sinks.</li>
 *     <li>Their spare FE charges storage blocks.</li>
 *     <li>Storage blocks cover what plain sinks still need.</li>
 *     <li>Storage blocks even out with each other, fullest first, only from fuller to emptier.</li>
 * </ol>
 *
 * <p>In steps 1 to 3 every source gives the same fraction of what it offers, so generators share the load instead of
 * the first one doing all the work. Each source splits its share between the sinks by its own
 * {@link EnergyDistributionMode}. Ports with the same {@link Port#target()} never trade with each other.</p>
 */
public final class EnergyAllocator {
    private static final double STORAGE_EQUALIZATION_DEADBAND = 0.01;

    private EnergyAllocator() {
    }

    /**
     * What one connector can do this tick.
     *
     * @param offer    FE the attached block can give through this connector now, within the connector's tier
     * @param demand   FE the attached block can take through this connector now, within the connector's tier
     * @param storage  a block that can both take and give FE, behind a Both connector
     * @param stored   the storage block's FE, used only when {@code storage}
     * @param capacity the storage block's capacity, used only when {@code storage}
     * @param mode     how this port splits what it gives between sinks
     * @param target   identifies the attached block; ports on the same block never trade
     */
    public record Port(
            int offer,
            int demand,
            boolean storage,
            long stored,
            long capacity,
            EnergyDistributionMode mode,
            int target
    ) {
        public static Port source(int offer, EnergyDistributionMode mode, int target) {
            return new Port(offer, 0, false, 0L, 0L, mode, target);
        }

        public static Port sink(int demand, int target) {
            return new Port(0, demand, false, 0L, 0L, EnergyDistributionMode.ROUND_ROBIN, target);
        }

        public static Port storage(int offer, int demand, long stored, long capacity, EnergyDistributionMode mode, int target) {
            return new Port(offer, demand, true, stored, capacity, mode, target);
        }
    }

    /** Extra pairs of ports that never trade, on top of ports sharing a target. */
    @FunctionalInterface
    public interface Exclusion {
        Exclusion NONE = (source, sink) -> false;

        boolean excluded(int source, int sink);
    }

    /** One planned hand-over from a source port to a sink port. */
    public record Flow(int source, int sink, int amount) {
    }

    /** FE each port gives and takes, and every hand-over between them; the give and take totals are always equal. */
    public record Plan(int[] give, int[] take, List<Flow> flows, int nextCursor) {
        public long total() {
            long total = 0L;
            for (int amount : give) {
                total += amount;
            }
            return total;
        }
    }

    public static Plan allocate(List<Port> ports, Exclusion exclusion, int cursor) {
        int size = ports.size();
        int[] give = new int[size];
        int[] take = new int[size];
        List<Flow> flows = new ArrayList<>();
        if (size == 0) {
            return new Plan(give, take, flows, 0);
        }
        Allocation allocation = new Allocation(
                ports,
                exclusion == null ? Exclusion.NONE : exclusion,
                cursor,
                give,
                take,
                flows
        );

        List<Integer> plainSources = new ArrayList<>();
        List<Integer> storageSources = new ArrayList<>();
        List<Integer> plainSinks = new ArrayList<>();
        List<Integer> storageSinks = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            Port port = ports.get(index);
            if (port.offer() > 0) {
                (port.storage() ? storageSources : plainSources).add(index);
            }
            if (port.demand() > 0) {
                (port.storage() ? storageSinks : plainSinks).add(index);
            }
        }

        allocation.shareProportionally(plainSources, plainSinks, false);
        allocation.shareProportionally(plainSources, storageSinks, false);
        allocation.shareProportionally(storageSources, plainSinks, false);
        storageSources.sort((first, second) -> Double.compare(fill(ports.get(second)), fill(ports.get(first))));
        for (int source : storageSources) {
            allocation.place(source, allocation.remainingOffer(source), storageSinks, true);
        }
        return new Plan(give, take, flows, allocation.cursor);
    }

    /**
     * FE a fuller storage block may send to an emptier one so both end at the same fill fraction. Below a 1% fill gap
     * it sends nothing, so charge and discharge losses cannot make the two trade small amounts back and forth.
     */
    public static int storageEqualizationLimit(long sourceStored, long sourceCapacity, long targetStored, long targetCapacity) {
        if (sourceCapacity <= 0 || targetCapacity <= 0 || sourceStored <= 0) {
            return 0;
        }
        double gap = (double) sourceStored / sourceCapacity - (double) targetStored / targetCapacity;
        if (gap <= STORAGE_EQUALIZATION_DEADBAND) {
            return 0;
        }
        long excess = sourceStored * targetCapacity - targetStored * sourceCapacity;
        long limit = excess / (sourceCapacity + targetCapacity);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, limit));
    }

    private static double fill(Port port) {
        return port.capacity() <= 0 ? 0.0 : (double) port.stored() / port.capacity();
    }

    private static final class Allocation {
        private final List<Port> ports;
        private final Exclusion exclusion;
        private final int[] give;
        private final int[] take;
        private final List<Flow> flows;
        private int cursor;

        private Allocation(List<Port> ports, Exclusion exclusion, int cursor, int[] give, int[] take, List<Flow> flows) {
            this.ports = ports;
            this.exclusion = exclusion;
            this.cursor = Math.floorMod(cursor, ports.size());
            this.give = give;
            this.take = take;
            this.flows = flows;
        }

        private int remainingOffer(int source) {
            return ports.get(source).offer() - give[source];
        }

        private int remainingDemand(int sink) {
            return ports.get(sink).demand() - take[sink];
        }

        /**
         * Gives every source the same fraction of its remaining offer, capped by what the sinks still need, then lets
         * sources that still have FE fill demand that exclusions kept the others from reaching.
         */
        private void shareProportionally(List<Integer> sources, List<Integer> sinks, boolean equalize) {
            if (sources.isEmpty() || sinks.isEmpty()) {
                return;
            }
            long offered = 0L;
            for (int source : sources) {
                offered += remainingOffer(source);
            }
            long needed = 0L;
            for (int sink : sinks) {
                needed += remainingDemand(sink);
            }
            if (offered <= 0L || needed <= 0L) {
                return;
            }

            int[] quota = quotas(sources, offered, needed);
            for (int index = 0; index < sources.size(); index++) {
                place(sources.get(index), quota[index], sinks, equalize);
            }
            for (int source : sources) {
                place(source, remainingOffer(source), sinks, equalize);
            }
        }

        /** Splits {@code min(offered, needed)} across sources by offer; whole units left over go to the largest remainders. */
        private int[] quotas(List<Integer> sources, long offered, long needed) {
            int[] quota = new int[sources.size()];
            if (offered <= needed) {
                for (int index = 0; index < sources.size(); index++) {
                    quota[index] = remainingOffer(sources.get(index));
                }
                return quota;
            }
            // needed < offered here, so when offered fits an int the product fits a long and the split is exact.
            boolean exact = offered <= Integer.MAX_VALUE;
            double[] remainders = new double[sources.size()];
            long assigned = 0L;
            for (int index = 0; index < sources.size(); index++) {
                long offer = remainingOffer(sources.get(index));
                if (exact) {
                    long scaled = offer * needed;
                    quota[index] = (int) (scaled / offered);
                    remainders[index] = scaled % offered;
                } else {
                    double scaled = (double) offer * needed / offered;
                    quota[index] = (int) Math.min(offer, Math.floor(scaled));
                    remainders[index] = scaled - quota[index];
                }
                assigned += quota[index];
            }
            for (int index = sources.size() - 1; index >= 0 && assigned > needed; index--) {
                long trim = Math.min(quota[index], assigned - needed);
                quota[index] -= (int) trim;
                assigned -= trim;
            }
            long left = needed - assigned;
            while (left > 0) {
                int best = -1;
                for (int index = 0; index < sources.size(); index++) {
                    if (quota[index] < remainingOffer(sources.get(index))
                            && (best < 0 || remainders[index] > remainders[best])) {
                        best = index;
                    }
                }
                if (best < 0) {
                    break;
                }
                quota[best]++;
                remainders[best] = -1.0;
                left--;
            }
            return quota;
        }

        private void place(int source, int amount, List<Integer> sinks, boolean equalize) {
            amount = Math.min(amount, remainingOffer(source));
            if (amount <= 0) {
                return;
            }
            if (equalize) {
                // Each hand-over changes both fill levels, so the next sink's even-fill limit must see the new ones.
                int left = amount;
                for (int sink : orderedSinks(source, sinks)) {
                    int moved = Math.min(left, room(source, sink, true));
                    transfer(source, sink, moved);
                    left -= Math.max(0, moved);
                    if (left <= 0) {
                        break;
                    }
                }
                return;
            }
            int[] candidates = new int[sinks.size()];
            int[] capacity = new int[sinks.size()];
            int count = 0;
            for (int sink : orderedSinks(source, sinks)) {
                int room = room(source, sink, equalize);
                if (room > 0) {
                    candidates[count] = sink;
                    capacity[count] = room;
                    count++;
                }
            }
            if (count == 0) {
                return;
            }

            EnergyDistributionMode mode = ports.get(source).mode();
            if (mode == EnergyDistributionMode.EVEN) {
                int[] given = new int[count];
                EvenSplit.distribute(count, 0, index -> false, amount, (index, request, simulate) -> {
                    int accepted = Math.min(request, capacity[index] - given[index]);
                    if (!simulate) {
                        given[index] += accepted;
                    }
                    return accepted;
                }, false);
                for (int index = 0; index < count; index++) {
                    transfer(source, candidates[index], given[index]);
                }
                return;
            }

            int left = amount;
            int lastReceiver = -1;
            for (int index = 0; index < count && left > 0; index++) {
                int moved = Math.min(left, capacity[index]);
                transfer(source, candidates[index], moved);
                left -= moved;
                lastReceiver = candidates[index];
            }
            if (mode == EnergyDistributionMode.ROUND_ROBIN && lastReceiver >= 0) {
                cursor = (lastReceiver + 1) % ports.size();
            }
        }

        /** Round Robin starts at the shared cursor; the other modes always start with the first sink. */
        private List<Integer> orderedSinks(int source, List<Integer> sinks) {
            if (ports.get(source).mode() != EnergyDistributionMode.ROUND_ROBIN) {
                return sinks;
            }
            int start = 0;
            while (start < sinks.size() && sinks.get(start) < cursor) {
                start++;
            }
            if (start == 0 || start == sinks.size()) {
                return sinks;
            }
            List<Integer> rotated = new ArrayList<>(sinks.size());
            rotated.addAll(sinks.subList(start, sinks.size()));
            rotated.addAll(sinks.subList(0, start));
            return rotated;
        }

        private int room(int source, int sink, boolean equalize) {
            Port from = ports.get(source);
            Port to = ports.get(sink);
            if (source == sink || from.target() == to.target() || exclusion.excluded(source, sink)) {
                return 0;
            }
            int room = remainingDemand(sink);
            if (equalize) {
                room = Math.min(room, storageEqualizationLimit(
                        Math.max(0L, from.stored() - give[source] + take[source]),
                        from.capacity(),
                        Math.max(0L, to.stored() + take[sink] - give[sink]),
                        to.capacity()
                ));
            }
            return Math.max(0, room);
        }

        private void transfer(int source, int sink, int amount) {
            if (amount > 0) {
                give[source] += amount;
                take[sink] += amount;
                flows.add(new Flow(source, sink, amount));
            }
        }
    }

    /** What actually moved when a plan ran: storages may accept or give less than they simulated. */
    public record Outcome(int[] extracted, int[] delivered, int returned, int lost) {
        public long totalExtracted() {
            return Arrays.stream(extracted).asLongStream().sum();
        }

        public long totalDelivered() {
            return Arrays.stream(delivered).asLongStream().sum();
        }
    }

    /** One port's attached FE storage, as the executor sees it. */
    public interface Storage {
        int extract(int amount);

        int receive(int amount);
    }

    /**
     * Runs a plan: takes what each source gives, hands it to the sinks, and puts anything a sink refused back into
     * the sources that gave it. FE no source can take back is reported as lost. A {@code null} storage moves nothing.
     */
    public static Outcome execute(Storage[] storages, Plan plan) {
        int size = storages.length;
        int[] extracted = new int[size];
        int[] delivered = new int[size];
        long pool = 0L;
        for (int index = 0; index < size; index++) {
            if (plan.give()[index] > 0 && storages[index] != null) {
                extracted[index] = clamp(storages[index].extract(plan.give()[index]), plan.give()[index]);
                pool += extracted[index];
            }
        }
        for (int index = 0; index < size && pool > 0L; index++) {
            int request = (int) Math.min(plan.take()[index], pool);
            if (request > 0 && storages[index] != null) {
                delivered[index] = clamp(storages[index].receive(request), request);
                pool -= delivered[index];
            }
        }
        int returned = 0;
        for (int index = size - 1; index >= 0 && pool > 0L; index--) {
            int request = (int) Math.min(extracted[index], pool);
            if (request > 0) {
                int back = clamp(storages[index].receive(request), request);
                extracted[index] -= back;
                returned += back;
                pool -= back;
            }
        }
        return new Outcome(extracted, delivered, returned, (int) pool);
    }

    private static int clamp(int amount, int max) {
        return Math.max(0, Math.min(amount, max));
    }
}
