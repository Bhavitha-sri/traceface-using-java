package com.traceface.model;

import java.util.ArrayList;
import java.util.List;

/** Parsed Google Lens response. */
public class SearchResults {
    private final List<SearchItem> exactMatches = new ArrayList<>();
    private final List<SearchItem> visualMatches = new ArrayList<>();
    private final List<String[]> relatedContent = new ArrayList<>(); // {query, link, thumbnail}

    public List<SearchItem> getExactMatches()  { return exactMatches; }
    public List<SearchItem> getVisualMatches() { return visualMatches; }
    public List<String[]> getRelatedContent()  { return relatedContent; }

    public int getTotalImageResults() {
        return exactMatches.size() + visualMatches.size();
    }
}
