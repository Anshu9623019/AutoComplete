package com.project.auto_complete_service.model;

public record SuggestionEntry(String word, int frequency)
        implements Comparable<SuggestionEntry> {

    @Override
    public int compareTo(SuggestionEntry o) {
        return Integer.compare(o.frequency(), this.frequency());
    }
}
