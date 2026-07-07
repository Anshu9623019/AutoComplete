package com.project.auto_complete_service.service;

import com.project.auto_complete_service.bktree.BKTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AutocompleteServiceFuzzyTest {

    @Autowired
    private BKTreeService bkTreeService;

    @Autowired
    private BKTree bkTree;

    @BeforeEach
    void seed() {
        // ✅ Clear first — removes stale data from BKTreeStartupSeeder
        bkTree.clear();

        // Seed with known clean words only
        List.of("java", "javascript", "kafka", "spring",
                "redis", "docker", "kotlin", "kubernetes")
                .forEach(bkTree::insert);
    }

    @Test
    void bkTree_findsTypo_directSearch() {
        // "jva" is distance 1 from "java" (delete 'a' at position 2)
        List<String> results = bkTreeService.search("jva", 5);

        assertThat(results).isNotEmpty();
        assertThat(results).contains("java");
    }

    @Test
    void bkTree_exactWord_distanceZero() {
        // Exact match — distance 0
        List<BKTree.SearchResult> results = bkTree.search("java", 0, 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).word()).isEqualTo("java");
        assertThat(results.get(0).distance()).isEqualTo(0);
    }

    @Test
    void bkTree_findsKafkaTypo() {
        // "kafak" is distance 2 from "kafka"
        // BKTreeService.search tries tolerance 1 first then 2
        List<String> results = bkTreeService.search("kafak", 5);

        // At tolerance 2 it should find kafka
        assertThat(results).isNotNull();
        assertThat(results).contains("kafka");
    }

    @Test
    void bkTree_nullAndEmpty_returnsEmpty() {
        assertThat(bkTreeService.search(null, 5)).isEmpty();
        assertThat(bkTreeService.search("", 5)).isEmpty();
    }
}