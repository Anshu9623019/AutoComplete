package com.project.auto_complete_service.tries;

import com.project.auto_complete_service.model.SuggestionEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Component
public class AutocompleteTrie {

    private volatile TrieNode root = new TrieNode();

    @Value("${autocomplete.trie.top-k:5}")
    private int K;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void insert(String word, int frequency) {
        if (word == null || word.isBlank()) return;

        lock.writeLock().lock();
        try {
            TrieNode curr = root;

            for (char c : word.toCharArray()) {
                curr.getChildren().putIfAbsent(c, new TrieNode());
                curr = curr.getChildren().get(c);
                updateTopK(curr, new SuggestionEntry(word, frequency));
            }
            curr.setEndOfWord(true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void updateTopK(TrieNode node, SuggestionEntry entry) {
        node.getTopK().removeIf(e -> e.word().equals(entry.word()));
        node.getTopK().add(entry);
        Collections.sort(node.getTopK());

        if (node.getTopK().size() > K) {
            node.setTopK(new ArrayList<>(node.getTopK().subList(0, K)));
        }
    }


    public List<String> search(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return List.of();

        lock.readLock().lock();
        try {
            TrieNode curr = root;

            for (char c : prefix.toCharArray()) {
                if (!curr.getChildren().containsKey(c)) return List.of();
                curr = curr.getChildren().get(c);
            }

            return curr.getTopK().stream()
                    .limit(limit)
                    .map(SuggestionEntry::word)
                    .toList();

        } finally {
            lock.readLock().unlock();
        }
    }

    public void swap(TrieNode newRoot) {
        lock.writeLock().lock();
        try {
            this.root = newRoot;
            log.info("Trie root swapped successfully");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public TrieNode getRoot() {
        return root;
    }

    public void setK(int k) {
        this.K = k;
    }
}