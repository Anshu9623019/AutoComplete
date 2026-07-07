package com.project.auto_complete_service.bktree;

import java.util.HashMap;
import java.util.Map;

public class BKTreeNode {

    final String word;

    // Key = edit distance from this node to child
    final Map<Integer, BKTreeNode> children = new HashMap<>();

    public BKTreeNode(String word) {
        this.word = word;
    }
}