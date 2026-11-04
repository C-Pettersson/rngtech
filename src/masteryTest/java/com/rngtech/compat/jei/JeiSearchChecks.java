package com.rngtech.compat.jei;

import java.util.Set;

/** Which JEI queries get RNGTech's multi-word search terms quoted, so each term stays one exact token. */
public final class JeiSearchChecks {
    private static final Set<String> PHRASES = Set.of(
            "stage 3",
            "stage 0",
            "affix forge",
            "gear crusher",
            "gear furnace",
            "gear alloy furnace"
    );
    private static int checks;

    private JeiSearchChecks() {
    }

    public static int run() {
        checks = 0;
        quotesKnownTerms();
        quotesCombinedAndReorderedTerms();
        keepsOtherWordsOutsideTheQuotes();
        keepsExclusion();
        leavesOtherQueriesAlone();
        return checks;
    }

    private static void quotesKnownTerms() {
        expect("@rngtech stage 3", "@rngtech \"stage 3\"");
        expect("@rngtech stage 0", "@rngtech \"stage 0\"");
        expect("@rngtech affix forge", "@rngtech \"affix forge\"");
        expect("@rngtech gear alloy furnace", "@rngtech \"gear alloy furnace\"");
        expect("@rng stage 3", "@rng \"stage 3\"");
        expect("@rngtech   stage   3", "@rngtech   \"stage 3\"");
    }

    private static void quotesCombinedAndReorderedTerms() {
        expect("stage 3 @rngtech gear crusher", "\"stage 3\" @rngtech \"gear crusher\"");
        expect("@rngtech gear furnace stage 3", "@rngtech \"gear furnace\" \"stage 3\"");
    }

    private static void keepsOtherWordsOutsideTheQuotes() {
        expect("@rngtech gear crusher head", "@rngtech \"gear crusher\" head");
        expect("@rngtech steel stage 3", "@rngtech steel \"stage 3\"");
    }

    private static void keepsExclusion() {
        expect("@rngtech -stage 3", "@rngtech -\"stage 3\"");
    }

    private static void leavesOtherQueriesAlone() {
        unchanged("stage 3");
        unchanged("@jei stage 3");
        unchanged("-@rngtech stage 3");
        unchanged("@ stage 3");
        unchanged("@rngtech stage 30");
        unchanged("@rngtech stage 9");
        unchanged("@rngtech gear foo");
        unchanged("@rngtech xstage 3");
        unchanged("@rngtech \"stage 3\"");
        unchanged("@rngtech");
        unchanged("");
        require(JeiSearchQuery.quotePhrases("@rngtech stage 3", Set.of()).equals("@rngtech stage 3"), "no phrases means no change");
    }

    private static void expect(String query, String expected) {
        String actual = JeiSearchQuery.quotePhrases(query, PHRASES);
        require(actual.equals(expected), "'" + query + "' should become '" + expected + "', was '" + actual + "'");
    }

    private static void unchanged(String query) {
        expect(query, query);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
