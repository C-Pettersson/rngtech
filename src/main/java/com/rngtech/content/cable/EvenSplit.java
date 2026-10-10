package com.rngtech.content.cable;

import java.util.function.IntPredicate;

/**
 * Splits one shipment as evenly as possible across the endpoints that can take it. Endpoints are visited in
 * rotation order from the cursor; the units that do not divide evenly go to the first endpoints in that order, and
 * the returned cursor moves past the last of them so the next shipment's odd units land elsewhere.
 */
public final class EvenSplit {
    private EvenSplit() {
    }

    @FunctionalInterface
    public interface Receiver {
        /** Returns how much of {@code amount} the endpoint at {@code index} accepts. */
        int accept(int index, int amount, boolean simulate);
    }

    public record Result(int moved, int nextCursor) {
    }

    public static Result distribute(
            int size,
            int cursor,
            IntPredicate skip,
            int amount,
            Receiver receiver,
            boolean simulate
    ) {
        if (size <= 0 || amount <= 0) {
            return new Result(0, cursor);
        }

        int start = Math.floorMod(cursor, size);
        int[] order = new int[size];
        int[] capacity = new int[size];
        int count = 0;
        for (int offset = 0; offset < size; offset++) {
            int index = (start + offset) % size;
            if (skip.test(index)) {
                continue;
            }
            int accepted = Math.min(amount, receiver.accept(index, amount, true));
            if (accepted > 0) {
                order[count] = index;
                capacity[count] = accepted;
                count++;
            }
        }
        if (count == 0) {
            return new Result(0, start);
        }

        int extra = amount % count;
        int nextCursor = extra > 0 ? (order[extra - 1] + 1) % size : start;
        int[] given = new int[count];
        int moved = 0;
        while (moved < amount) {
            int open = 0;
            for (int k = 0; k < count; k++) {
                if (given[k] < capacity[k]) {
                    open++;
                }
            }
            if (open == 0) {
                break;
            }

            int left = amount - moved;
            int share = left / open;
            int roundExtra = left % open;
            int roundMoved = 0;
            int slot = 0;
            for (int k = 0; k < count; k++) {
                if (given[k] >= capacity[k]) {
                    continue;
                }
                int request = Math.min(share + (slot < roundExtra ? 1 : 0), capacity[k] - given[k]);
                slot++;
                if (request <= 0) {
                    continue;
                }
                int accepted = receiver.accept(order[k], request, simulate);
                if (accepted < request) {
                    capacity[k] = given[k] + accepted;
                }
                given[k] += accepted;
                roundMoved += accepted;
            }
            if (roundMoved == 0) {
                break;
            }
            moved += roundMoved;
        }

        return new Result(moved, nextCursor);
    }
}
