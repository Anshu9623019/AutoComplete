package com.project.auto_complete_service.scheduler;

import com.project.auto_complete_service.respository.QueryFrequencyRepository;
import com.project.auto_complete_service.tries.AutocompleteTrie;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrieRebuildJob {

    private final QueryFrequencyRepository repo;
    private final AutocompleteTrie trie;

    @Scheduled(cron = "0 0 2 * * *")
    public void rebuild() {

        AutocompleteTrie newTrie = new AutocompleteTrie();

        repo.findAll().forEach(q ->
                newTrie.insert(q.getWord(), q.getFrequency())
        );

        trie.swap(newTrie);
    }
}
