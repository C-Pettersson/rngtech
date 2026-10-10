package com.rngtech.content.cable;

import java.util.Arrays;

/** Executable checks for how cable networks split item and fluid shipments across receivers. */
public final class CableRoutingChecks {
    private static int checks;

    private CableRoutingChecks() {
    }

    public static void run() {
        singleItemShipmentsRotate();
        largeShipmentsSplitEvenly();
        oddUnitsRotateAcrossShipments();
        fullReceiversSpillToTheRest();
        simulationMatchesExecution();
        nothingToReceiveMovesNothing();
        System.out.println("Cable routing: " + checks + " checks passed");
    }

    /** One crusher (index 1) feeding three furnaces one item at a time: every furnace gets a turn. */
    private static void singleItemShipmentsRotate() {
        Network network = new Network(64, 64, 64, 64);
        int cursor = 0;
        for (int shipment = 0; shipment < 30; shipment++) {
            cursor = network.ship(cursor, 1, 1, false).nextCursor();
        }
        require(Arrays.equals(network.stored, new int[] {10, 0, 10, 10}), "single items rotate across every furnace");
    }

    private static void largeShipmentsSplitEvenly() {
        Network network = new Network(64, 64, 64, 64);
        EvenSplit.Result result = network.ship(0, 1, 16, false);
        require(result.moved() == 16, "the whole shipment is delivered");
        require(Arrays.equals(network.stored, new int[] {6, 0, 5, 5}), "16 items split 6/5/5, never to the source");
    }

    private static void oddUnitsRotateAcrossShipments() {
        Network network = new Network(640, 640, 640);
        int cursor = 0;
        for (int shipment = 0; shipment < 30; shipment++) {
            cursor = network.ship(cursor, -1, 16, false).nextCursor();
        }
        int max = Arrays.stream(network.stored).max().orElseThrow();
        int min = Arrays.stream(network.stored).min().orElseThrow();
        require(max - min <= 1, "the leftover unit does not always land on the same receiver");
    }

    private static void fullReceiversSpillToTheRest() {
        Network network = new Network(2, 64, 64);
        EvenSplit.Result result = network.ship(0, -1, 16, false);
        require(result.moved() == 16, "a nearly full receiver does not strand items");
        require(Arrays.equals(network.stored, new int[] {2, 7, 7}), "what a full receiver cannot take goes to the rest");
    }

    private static void simulationMatchesExecution() {
        Network network = new Network(3, 64, 0, 64);
        EvenSplit.Result simulated = network.ship(2, -1, 20, true);
        require(Arrays.equals(network.stored, new int[4]), "simulation stores nothing");
        EvenSplit.Result executed = network.ship(2, -1, 20, false);
        require(simulated.equals(executed), "simulation predicts the executed split and cursor");
        require(Arrays.equals(network.stored, new int[] {3, 8, 0, 9}), "a receiver with no room is left out of the split");
    }

    private static void nothingToReceiveMovesNothing() {
        Network network = new Network(0, 0);
        EvenSplit.Result result = network.ship(1, -1, 8, false);
        require(result.moved() == 0, "full receivers take nothing");
        require(result.nextCursor() == 1, "the cursor stays put when nothing moves");
        require(EvenSplit.distribute(0, 3, index -> false, 8, (index, amount, simulate) -> amount, false).moved() == 0,
                "an empty network takes nothing");
    }

    private static final class Network {
        private final int[] capacity;
        private final int[] stored;

        private Network(int... capacity) {
            this.capacity = capacity;
            this.stored = new int[capacity.length];
        }

        private EvenSplit.Result ship(int cursor, int source, int amount, boolean simulate) {
            return EvenSplit.distribute(capacity.length, cursor, index -> index == source, amount, this::accept, simulate);
        }

        private int accept(int index, int amount, boolean simulate) {
            int accepted = Math.min(amount, capacity[index] - stored[index]);
            if (!simulate) {
                stored[index] += accepted;
            }
            return accepted;
        }
    }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
