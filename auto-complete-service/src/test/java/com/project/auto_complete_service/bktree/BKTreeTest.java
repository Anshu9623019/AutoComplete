package com.project.auto_complete_service.bktree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BKTreeTest {

    private BKTree tree;

    @BeforeEach
    void setUp() {
        tree = new BKTree();
        // Seed with common tech terms
        List.of("java", "javascript", "kafka", "spring",
                "redis", "docker", "kotlin", "kubernetes")
            .forEach(tree::insert);
    }

    @Test
    void exactMatch_distanceZero() {
        List<BKTree.SearchResult> results = tree.search("java", 0, 5);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).word()).isEqualTo("java");
        assertThat(results.get(0).distance()).isEqualTo(0);
    }

    @Test
    void oneCharTypo_foundWithTolerance1() {
        // "jva" is distance 1 from "java"
        List<BKTree.SearchResult> results = tree.search("jva", 1, 5);
        assertThat(results).isNotEmpty();
        assertThat(results.stream().map(BKTree.SearchResult::word))
                .contains("java");
    }

    @Test
    void twoCharTypo_foundWithTolerance2() {
        // "kafak" is distance 1 but test tolerance 2
        List<BKTree.SearchResult> results = tree.search("kafak", 2, 5);
        assertThat(results.stream().map(BKTree.SearchResult::word))
                .contains("kafka");
    }

    @Test
    void noMatch_returnsEmpty() {
        List<BKTree.SearchResult> results = tree.search("xyz", 1, 5);
        assertThat(results).isEmpty();
    }

    @Test
    void results_sortedByDistanceAscending() {
        tree.insert("jav");   // distance 1 from "java"
        tree.insert("ja");    // distance 2 from "java"

        List<BKTree.SearchResult> results = tree.search("java", 2, 10);

        // Closest matches should come first
        for (int i = 1; i < results.size(); i++) {
            assertThat(results.get(i).distance())
                    .isGreaterThanOrEqualTo(results.get(i - 1).distance());
        }
    }

    @Test
    void limit_respected() {
        // Insert many words close to "java"
        List.of("jave", "jiva", "jala", "jawa").forEach(tree::insert);

        List<BKTree.SearchResult> results = tree.search("java", 1, 2);
        assertThat(results.size()).isLessThanOrEqualTo(2);
    }

    @Test
    void duplicateInsert_doesNotDuplicate() {
        int sizeBefore = tree.size();
        tree.insert("java"); // already inserted
        assertThat(tree.size()).isEqualTo(sizeBefore);
    }

    @Test
    void nullInput_returnsEmpty() {
        assertThat(tree.search(null, 1, 5)).isEmpty();
        assertThat(tree.search("",   1, 5)).isEmpty();
    }

    @Test
    void realWorldTypos() {
        assertThat(tree.search("kafak",   1, 5)
                .stream().map(BKTree.SearchResult::word))
                .contains("kafka");

        assertThat(tree.search("sprng",   1, 5)
                .stream().map(BKTree.SearchResult::word))
                .contains("spring");

        assertThat(tree.search("reddis",  1, 5)
                .stream().map(BKTree.SearchResult::word))
                .contains("redis");

        assertThat(tree.search("doker",   1, 5)
                .stream().map(BKTree.SearchResult::word))
                .contains("docker");

        assertThat(tree.search("kotln",   1, 5)
                .stream().map(BKTree.SearchResult::word))
                .contains("kotlin");
    }
}