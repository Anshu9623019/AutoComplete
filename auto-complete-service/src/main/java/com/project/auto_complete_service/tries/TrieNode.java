package com.project.auto_complete_service.tries;

import com.project.auto_complete_service.model.SuggestionEntry;
import lombok.Data;

import java.util.*;

@Data
public class TrieNode {
    private Map<Character, TrieNode> children = new HashMap<>();
    private List<SuggestionEntry> topK = new ArrayList<>();
    private boolean isEndOfWord;
}