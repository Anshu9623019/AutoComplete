package com.project.auto_complete_service.scheduler;

import com.project.auto_complete_service.bktree.BKTree;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BKTreeRebuildJob {

    private final QueryFrequencyRepository repo;
    private final BKTree liveBKTree;

    // Runs right after TrieRebuildJob (2:05 AM)
    @Scheduled(fixedDelay = 90_000)
    @Transactional(readOnly = true)
    public void rebuild() {
        log.info("BK-Tree rebuild started...");
        long start = System.currentTimeMillis();

        // Build new BK-Tree from all known words
        BKTree newTree = new BKTree();

        repo.streamAllByOrderByFrequencyDesc()
                .forEach(record -> newTree.insert(record.getWord()));

        // Atomic swap — zero downtime
        liveBKTree.swap(newTree);

        log.info("BK-Tree rebuild complete in {}ms — {} words",
                System.currentTimeMillis() - start,
                newTree.size());
    }
}