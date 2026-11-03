package com.rngtech.content.affixsorter;

/** A menu whose screen edits an Affix Sorter rule list. */
public interface SorterRulesHost {
    SorterRules sorterRules();

    /** Replaces the rule list on the server. Rules past the sorter's Rule Capacity are stored disabled. */
    void applySorterRules(SorterRules rules);
}
