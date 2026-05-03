package com.project.auto_complete_service.scheduler;

import com.project.auto_complete_service.model.QueryFrequency;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import com.project.auto_complete_service.tries.AutocompleteTrie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrieRebuildJob {

    private final QueryFrequencyRepository repo;
    private final AutocompleteTrie liveTrie;

    @Value("${autocomplete.trie.top-k:5}")
    private int topK;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void rebuild() {
        log.info("Trie rebuild started...");
        long start = System.currentTimeMillis();

        AutocompleteTrie tempTrie = new AutocompleteTrie();
        tempTrie.setK(topK);

        // ✅ getWord() and getFrequency() from @Getter on your QueryFrequency
        repo.streamAllByOrderByFrequencyDesc().forEach(record ->
                tempTrie.insert(record.getWord(), record.getFrequency())
        );

        liveTrie.swap(tempTrie.getRoot());

        log.info("Trie rebuild complete in {}ms", System.currentTimeMillis() - start);
    }
}