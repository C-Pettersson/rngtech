package com.rngtech.content.cable;

import com.rngtech.content.cable.EnergyAllocator.Flow;
import com.rngtech.content.cable.EnergyAllocator.Plan;
import com.rngtech.content.cable.EnergyAllocator.Port;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Executable checks for the once-per-tick network energy pass: fair shares, priorities, and FE conservation. */
public final class EnergyAllocatorChecks {
    private static final EnergyDistributionMode RR = EnergyDistributionMode.ROUND_ROBIN;
    private static final EnergyDistributionMode EVEN = EnergyDistributionMode.EVEN;
    private static final EnergyDistributionMode FIRST = EnergyDistributionMode.FIRST_AVAILABLE;
    private static int checks;

    private EnergyAllocatorChecks() {
    }

    public static void run() {
        generatorsShareTheLoad();
        sharesFollowOffers();
        sourceOrderDoesNotChangeShares();
        machinesComeBeforeStorage();
        storageCoversWhatGeneratorsCannot();
        storageEvensOutFromFullerToEmptier();
        portsOnOneBlockNeverTrade();
        excludedPairsNeverTrade();
        roundRobinRotatesSingleUnits();
        firstAvailableFillsInOrder();
        evenSplitsAcrossSinks();
        randomPlansConserveEnergy();
        executionConservesEnergy();
        refusedEnergyGoesBack();
        pushedEnergyStaysWithTheMachine();
        System.out.println("Energy allocation: " + checks + " checks passed");
    }

    private static void generatorsShareTheLoad() {
        Plan plan = allocate(
                Port.source(100, RR, 0),
                Port.source(100, RR, 1),
                Port.source(100, RR, 2),
                Port.sink(150, 3)
        );
        require(Arrays.equals(plan.give(), new int[] {50, 50, 50, 0}), "three equal generators each give a third");
        require(plan.take()[3] == 150, "the machine gets all it asked for");
    }

    private static void sharesFollowOffers() {
        Plan plan = allocate(Port.source(200, RR, 0), Port.source(100, RR, 1), Port.sink(150, 2));
        require(plan.give()[0] == 100 && plan.give()[1] == 50, "a generator offering twice as much gives twice as much");

        Plan odd = allocate(Port.source(1, RR, 0), Port.source(1, RR, 1), Port.source(1, RR, 2), Port.sink(2, 3));
        require(odd.total() == 2, "whole units left over by the split still move");
    }

    private static void sourceOrderDoesNotChangeShares() {
        Plan forward = allocate(Port.source(90, FIRST, 0), Port.source(30, EVEN, 1), Port.sink(60, 2), Port.sink(20, 3));
        Plan backward = allocate(Port.sink(60, 2), Port.sink(20, 3), Port.source(30, EVEN, 1), Port.source(90, FIRST, 0));
        require(forward.give()[0] == backward.give()[3] && forward.give()[1] == backward.give()[2],
                "each generator's share depends on its offer, not on its place in the order");
        require(forward.give()[0] == 60 && forward.give()[1] == 20, "shares are 3 to 1 like the offers");
    }

    private static void machinesComeBeforeStorage() {
        Plan plan = allocate(
                Port.source(100, RR, 0),
                Port.storage(0, 100, 0, 1_000, RR, 1),
                Port.sink(60, 2)
        );
        require(plan.take()[2] == 60, "the machine is served first");
        require(plan.take()[1] == 40, "only the spare FE charges storage");
    }

    private static void storageCoversWhatGeneratorsCannot() {
        Plan plan = allocate(
                Port.source(50, RR, 0),
                Port.storage(100, 100, 500, 1_000, RR, 1),
                Port.sink(80, 2)
        );
        require(plan.give()[0] == 50, "the generator gives everything first");
        require(plan.give()[1] == 30, "storage covers only the rest");
        require(plan.take()[1] == 0, "storage does not charge while it discharges into a machine");
    }

    private static void storageEvensOutFromFullerToEmptier() {
        Plan plan = allocate(
                Port.storage(1_000, 1_000, 600, 1_000, RR, 0),
                Port.storage(1_000, 1_000, 400, 1_000, RR, 1)
        );
        require(plan.give()[0] == 100 && plan.take()[1] == 100, "the fuller bank sends half the gap");
        require(plan.give()[1] == 0, "the emptier bank sends nothing back");

        Plan level = allocate(
                Port.storage(1_000, 1_000, 505, 1_000, RR, 0),
                Port.storage(1_000, 1_000, 500, 1_000, RR, 1)
        );
        require(level.total() == 0, "gaps under 1% move nothing");
    }

    private static void portsOnOneBlockNeverTrade() {
        Plan plan = allocate(Port.storage(100, 100, 900, 1_000, RR, 7), Port.storage(100, 100, 100, 1_000, RR, 7));
        require(plan.total() == 0, "two connectors on the same storage block never trade");
    }

    private static void excludedPairsNeverTrade() {
        List<Port> ports = List.of(Port.source(100, RR, 0), Port.sink(100, 1), Port.sink(100, 2));
        Plan plan = EnergyAllocator.allocate(ports, (source, sink) -> sink == 1, 0);
        require(plan.take()[1] == 0 && plan.take()[2] == 100, "an excluded sink gets nothing and the rest gets it all");
    }

    private static void roundRobinRotatesSingleUnits() {
        int[] received = new int[4];
        int cursor = 0;
        for (int tick = 0; tick < 30; tick++) {
            Plan plan = EnergyAllocator.allocate(
                    List.of(Port.source(1, RR, 0), Port.sink(64, 1), Port.sink(64, 2), Port.sink(64, 3)),
                    EnergyAllocator.Exclusion.NONE,
                    cursor
            );
            for (int index = 0; index < received.length; index++) {
                received[index] += plan.take()[index];
            }
            cursor = plan.nextCursor();
        }
        require(Arrays.equals(received, new int[] {0, 10, 10, 10}), "single FE rotates across every machine");
    }

    private static void firstAvailableFillsInOrder() {
        Plan plan = allocate(Port.source(100, FIRST, 0), Port.sink(70, 1), Port.sink(70, 2));
        require(plan.take()[1] == 70 && plan.take()[2] == 30, "First Available fills the first machine first");
    }

    private static void evenSplitsAcrossSinks() {
        Plan plan = allocate(Port.source(100, EVEN, 0), Port.sink(20, 1), Port.sink(70, 2), Port.sink(70, 3));
        require(plan.take()[1] == 20 && plan.take()[2] == 40 && plan.take()[3] == 40,
                "Even splits evenly and spills what a full machine cannot take");
    }

    /** Random networks: the plan never invents or loses FE and never breaks a rule. */
    private static void randomPlansConserveEnergy() {
        Random random = new Random(67L);
        EnergyDistributionMode[] modes = EnergyDistributionMode.values();
        for (int trial = 0; trial < 2_000; trial++) {
            int size = 1 + random.nextInt(12);
            List<Port> ports = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                int target = random.nextInt(Math.max(1, size - 2));
                EnergyDistributionMode mode = modes[random.nextInt(modes.length)];
                int offer = random.nextInt(3) == 0 ? 0 : random.nextInt(2_000);
                int demand = random.nextInt(3) == 0 ? 0 : random.nextInt(2_000);
                if (random.nextBoolean()) {
                    long capacity = 1 + random.nextInt(10_000);
                    ports.add(Port.storage(offer, demand, random.nextLong(capacity + 1), capacity, mode, target));
                } else {
                    ports.add(new Port(offer, demand, false, 0L, 0L, mode, target));
                }
            }
            long seed = random.nextLong();
            EnergyAllocator.Exclusion exclusion = (source, sink) -> ((source * 31L + sink) ^ seed) % 7 == 0;
            Plan plan = EnergyAllocator.allocate(ports, exclusion, random.nextInt(size));

            long given = 0L;
            long taken = 0L;
            for (int index = 0; index < size; index++) {
                given += plan.give()[index];
                taken += plan.take()[index];
                if (plan.give()[index] > ports.get(index).offer() || plan.take()[index] > ports.get(index).demand()) {
                    throw new IllegalStateException("trial " + trial + ": a port moved more than it offered or asked for");
                }
            }
            if (given != taken) {
                throw new IllegalStateException("trial " + trial + ": gave " + given + " but took " + taken);
            }
            long flowed = 0L;
            long[] level = new long[size];
            for (int index = 0; index < size; index++) {
                level[index] = ports.get(index).stored();
            }
            for (Flow flow : plan.flows()) {
                flowed += flow.amount();
                Port source = ports.get(flow.source());
                Port sink = ports.get(flow.sink());
                if (source.target() == sink.target() || exclusion.excluded(flow.source(), flow.sink())) {
                    throw new IllegalStateException("trial " + trial + ": FE moved between ports that never trade");
                }
                boolean betweenStorage = source.storage() && sink.storage();
                if (betweenStorage && fill(level[flow.source()], source) <= fill(level[flow.sink()], sink)) {
                    throw new IllegalStateException("trial " + trial + ": storage fed storage that was at least as full");
                }
                level[flow.source()] -= flow.amount();
                level[flow.sink()] += flow.amount();
                if (betweenStorage && fill(level[flow.source()], source) + 1e-9 < fill(level[flow.sink()], sink)) {
                    throw new IllegalStateException("trial " + trial + ": storage overshot an even fill");
                }
            }
            if (flowed != given) {
                throw new IllegalStateException("trial " + trial + ": flows do not add up to the plan");
            }
            Plan again = EnergyAllocator.allocate(ports, exclusion, cursorOf(plan, size));
            Plan repeat = EnergyAllocator.allocate(ports, exclusion, cursorOf(plan, size));
            if (!Arrays.equals(again.give(), repeat.give()) || !Arrays.equals(again.take(), repeat.take())) {
                throw new IllegalStateException("trial " + trial + ": the same ports gave a different plan");
            }
        }
        checks += 2_000;
    }

    private static void executionConservesEnergy() {
        Plan plan = allocate(Port.source(100, RR, 0), Port.source(100, RR, 1), Port.sink(80, 2), Port.sink(80, 3));
        FakeStorage first = new FakeStorage(100, 0);
        FakeStorage second = new FakeStorage(100, 0);
        FakeStorage machine = new FakeStorage(0, 80);
        FakeStorage other = new FakeStorage(0, 80);
        EnergyAllocator.Outcome outcome = EnergyAllocator.execute(
                new EnergyAllocator.Storage[] {first, second, machine, other},
                plan
        );
        require(outcome.totalExtracted() == 160 && outcome.totalDelivered() == 160, "what leaves the generators arrives");
        require(first.energy + second.energy + machine.energy + other.energy == 200, "no FE is created or lost");
        require(first.energy == second.energy, "both generators gave the same");
    }

    private static void refusedEnergyGoesBack() {
        Plan plan = allocate(Port.storage(100, 0, 1_000, 1_000, RR, 0), Port.sink(100, 1));
        FakeStorage bank = new FakeStorage(1_000, 1_000);
        FakeStorage stubborn = new FakeStorage(0, 40);
        EnergyAllocator.Outcome outcome = EnergyAllocator.execute(new EnergyAllocator.Storage[] {bank, stubborn}, plan);
        require(outcome.delivered()[1] == 40, "a machine that takes less than it simulated gets what it takes");
        require(outcome.returned() == 60 && outcome.lost() == 0, "the rest goes back into the bank");
        require(bank.energy + stubborn.energy == 1_000, "the bank and machine still hold all the FE");
        require(outcome.extracted()[0] == 40, "the bank is only charged for what moved");

        FakeStorage generator = new FakeStorage(100, 0);
        generator.refuses = true;
        EnergyAllocator.Outcome lost = EnergyAllocator.execute(
                new EnergyAllocator.Storage[] {generator, new FakeStorage(0, 40)},
                allocate(Port.source(100, RR, 0), Port.sink(100, 1))
        );
        require(lost.lost() == 60, "FE nothing can take back is reported, never hidden");
    }

    private static void pushedEnergyStaysWithTheMachine() {
        Plan plan = allocate(Port.sink(30, 0), Port.source(50, RR, 1));
        EnergyAllocator.Storage pushed = new EnergyAllocator.Storage() {
            @Override
            public int extract(int amount) {
                return amount;
            }

            @Override
            public int receive(int amount) {
                return amount;
            }
        };
        FakeStorage half = new FakeStorage(0, 10);
        EnergyAllocator.Outcome outcome = EnergyAllocator.execute(new EnergyAllocator.Storage[] {half, pushed}, plan);
        require(outcome.extracted()[1] == 10, "a pushing machine only hands over what was taken");
    }

    private static int cursorOf(Plan plan, int size) {
        return plan.nextCursor() % size;
    }

    private static double fill(long stored, Port port) {
        return (double) stored / port.capacity();
    }

    private static Plan allocate(Port... ports) {
        return EnergyAllocator.allocate(List.of(ports), EnergyAllocator.Exclusion.NONE, 0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
        checks++;
    }

    /** An FE store that can give its energy and take up to {@code room} more. */
    private static final class FakeStorage implements EnergyAllocator.Storage {
        private int energy;
        private int room;
        private boolean refuses;

        private FakeStorage(int energy, int room) {
            this.energy = energy;
            this.room = room;
        }

        @Override
        public int extract(int amount) {
            int moved = Math.min(amount, energy);
            energy -= moved;
            room += moved;
            return moved;
        }

        @Override
        public int receive(int amount) {
            if (refuses) {
                return 0;
            }
            int moved = Math.min(amount, room);
            energy += moved;
            room -= moved;
            return moved;
        }
    }
}
