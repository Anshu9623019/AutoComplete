package com.project.auto_complete_service.config;

import com.project.auto_complete_service.bktree.BKTree;
import com.project.auto_complete_service.repository.QueryFrequencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BKTreeStartupSeeder implements ApplicationRunner {

    private final BKTree bkTree;
    private final QueryFrequencyRepository repo;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        log.info("Seeding BK-Tree from database...");
        long start = System.currentTimeMillis();

        repo.streamAllByOrderByFrequencyDesc()
                .forEach(q -> bkTree.insert(q.getWord()));

        log.info("BK-Tree seeded in {}ms — {} words",
                System.currentTimeMillis() - start,
                bkTree.size());
    }
}