package com.project.auto_complete_service.tries;

import com.project.auto_complete_service.model.SuggestionEntry;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class AutocompleteTrie {

    private TrieNode root = new TrieNode();
    private final int K = 5;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void insert(String word, int frequency) {
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
            node.setTopK(node.getTopK().subList(0, K));
        }
    }

    public List<SuggestionEntry> search(String prefix) {
        lock.readLock().lock();
        try {
            TrieNode curr = root;

            for (char c : prefix.toCharArray()) {
                if (!curr.getChildren().containsKey(c)) return List.of();
                curr = curr.getChildren().get(c);
            }
            return curr.getTopK();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Atomic swap
    public void swap(AutocompleteTrie newTrie) {
        lock.writeLock().lock();
        try {
            this.root = newTrie.root;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
