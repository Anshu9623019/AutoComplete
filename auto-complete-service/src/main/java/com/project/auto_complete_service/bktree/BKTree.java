package com.project.auto_complete_service.bktree;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class BKTree {

    private BKTreeNode root;
    private int size;

    // Thread-safe — same pattern as AutocompleteTrie
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // ── Insert a word into the tree ───────────────────────────────
    public void insert(String word) {
        if (word == null || word.isBlank())
            return;

        lock.writeLock().lock();
        try {
            if (root == null) {
                root = new BKTreeNode(word);
                size++;
                return;
            }

            BKTreeNode current = root;

            while (true) {
                int dist = LevenshteinDistance.compute(
                        word,
                        current.word);

                if (dist == 0)
                    return; // word already exists

                BKTreeNode child = current.children.get(dist);

                if (child == null) {
                    // No child at this distance — insert here
                    current.children.put(dist, new BKTreeNode(word));
                    size++;
                    return;
                }

                // Move down the tree
                current = child;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── Search for words within edit distance tolerance ───────────
    // In BKTree.search() — verify this is the order:
    public List<SearchResult> search(String query, int tolerance, int limit) {
        if (query == null || query.isBlank() || root == null)
            return List.of();

        lock.readLock().lock();
        try {
            List<SearchResult> results = new ArrayList<>(); // ← mutable
            searchRecursive(root, query, tolerance, results, limit);

            results.sort((a, b) -> // ← sort the mutable list
            Integer.compare(a.distance(), b.distance()));

            return results.stream().limit(limit).toList(); // ← then return unmodifiable
        } finally {
            lock.readLock().unlock();
        }
    }

    // ── Recursive BK-Tree traversal ───────────────────────────────
    private void searchRecursive(
            BKTreeNode node,
            String query,
            int tolerance,
            List<SearchResult> results,
            int limit) {

        if (node == null || results.size() >= limit)
            return;

        int dist = LevenshteinDistance.compute(query, node.word, tolerance + 1);
        // ↑ Pass tolerance+1 here so early exit doesn't
        // incorrectly cap the distance used for child pruning

        if (dist <= tolerance) {
            results.add(new SearchResult(node.word, dist));
        }

        // Visit children whose edge weight falls in [dist-tolerance, dist+tolerance]
        int low = Math.max(1, dist - tolerance); // ← Math.max(1,...) prevents visiting 0
        int high = dist + tolerance;

        for (Map.Entry<Integer, BKTreeNode> entry : node.children.entrySet()) {
            int edgeWeight = entry.getKey();
            if (edgeWeight >= low && edgeWeight <= high) {
                searchRecursive(entry.getValue(), query, tolerance, results, limit);
            }
        }
    }

    // ── Atomic swap (same as Trie — for nightly rebuild) ─────────
    public void swap(BKTree newTree) {
        lock.writeLock().lock();
        try {
            this.root = newTree.root;
            this.size = newTree.size;
            log.info("BK-Tree swapped — new size: {}", this.size);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Add to BKTree.java
    public void clear() {
        lock.writeLock().lock();
        try {
            this.root = null;
            this.size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        return size;
    }

    public record SearchResult(String word, int distance) {
    }

}