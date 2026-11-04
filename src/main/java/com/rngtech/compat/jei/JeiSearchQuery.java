package com.rngtech.compat.jei;

import com.rngtech.RNGTech;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Quotes RNGTech's multi-word search terms in a JEI query. JEI splits an unquoted {@code stage 3} into two tokens that each match
 * any tooltip word, so {@code 3} also finds {@code +30%}. A quoted phrase stays one token and can only match the alias.
 */
public final class JeiSearchQuery {
    private static volatile Set<String> phrases = Set.of();

    private JeiSearchQuery() {
    }

    public static void setPhrases(Collection<String> terms) {
        phrases = terms.stream().filter(term -> term.contains(" ")).collect(Collectors.toUnmodifiableSet());
    }

    public static String quotePhrases(String query) {
        return quotePhrases(query, phrases);
    }

    public static String quotePhrases(String query, Set<String> phrases) {
        if (phrases.isEmpty() || query.indexOf('"') >= 0 || !scopedToRngtech(query)) {
            return query;
        }
        return phrasePattern(phrases).matcher(query).replaceAll(match -> Matcher.quoteReplacement(
                match.group(1) + '"' + match.group(2).replaceAll("\\s+", " ") + '"'
        ));
    }

    /** Other mods' tooltips can only match unquoted, so a query outside this mod is left as JEI reads it. */
    private static boolean scopedToRngtech(String query) {
        for (String token : query.trim().split("\\s+")) {
            if (token.length() > 1 && token.charAt(0) == '@' && RNGTech.MOD_ID.contains(token.substring(1))) {
                return true;
            }
        }
        return false;
    }

    private static Pattern phrasePattern(Set<String> phrases) {
        String alternatives = phrases.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(phrase -> Pattern.quote(phrase).replace(" ", "\\E\\s+\\Q"))
                .collect(Collectors.joining("|"));
        return Pattern.compile("(?<!\\S)(-?)(" + alternatives + ")(?!\\S)");
    }
}
