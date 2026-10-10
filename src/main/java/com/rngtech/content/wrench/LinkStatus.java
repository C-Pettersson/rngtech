package com.rngtech.content.wrench;

import java.util.Locale;

/** Why a connector or module is, or is not, moving anything on its channel. */
public enum LinkStatus {
    OK,
    NO_PARTNER,
    NO_RECEIVER,
    NO_SOURCE,
    TARGET_BLOCKED,
    NOT_ATTACHED;

    public static LinkStatus byOrdinal(int ordinal) {
        LinkStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : OK;
    }

    public boolean isProblem() {
        return this != OK;
    }

    public String translationKey() {
        return "rngtech.link_status." + name().toLowerCase(Locale.ROOT);
    }

    /**
     * Checks one endpoint against the endpoints on its channel.
     *
     * @param sources every source on the channel, this endpoint included when {@code countedSource}
     * @param sinks every sink on the channel, this endpoint included when {@code countedSink}
     * @param roleSource whether the endpoint's mode pulls into the network
     * @param roleSink whether the endpoint's mode pushes out of the network
     */
    public static LinkStatus evaluate(
            int sources,
            int sinks,
            boolean roleSource,
            boolean roleSink,
            boolean countedSource,
            boolean countedSink
    ) {
        int otherSources = sources - (countedSource ? 1 : 0);
        int otherSinks = sinks - (countedSink ? 1 : 0);
        if ((roleSource && otherSinks > 0) || (roleSink && otherSources > 0)) {
            return OK;
        }
        if (otherSources + otherSinks <= 0) {
            return NO_PARTNER;
        }
        if (roleSource && !roleSink) {
            return NO_RECEIVER;
        }
        if (roleSink && !roleSource) {
            return NO_SOURCE;
        }
        return NO_PARTNER;
    }
}
