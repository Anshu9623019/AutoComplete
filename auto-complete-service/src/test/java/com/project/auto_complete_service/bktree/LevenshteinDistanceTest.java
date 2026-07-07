package com.project.auto_complete_service.bktree;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LevenshteinDistanceTest {

    @Test
    void sameWord_returnsZero() {
        assertThat(LevenshteinDistance.compute("java", "java")).isEqualTo(0);
    }

    @Test
    void oneInsertion() {
        // "jva" → "java": insert 'a' at position 2 = 1 edit
        assertThat(LevenshteinDistance.compute("jva", "java")).isEqualTo(1);
    }

    @Test
    void oneDeletion() {
        // "jaava" → "java": delete one 'a' = 1 edit
        assertThat(LevenshteinDistance.compute("jaava", "java")).isEqualTo(1);
    }

    @Test
    void oneReplacement() {
        // "kava" → "java": replace k→j = 1 edit
        assertThat(LevenshteinDistance.compute("kava", "java")).isEqualTo(1);
    }

    @Test
    void twoEdits() {
        // "kaffe" → "kafka": replace f→k at pos 3, replace e→a at pos 4 = 2 edits
        assertThat(LevenshteinDistance.compute("kaffe", "kafka")).isEqualTo(2);
    
        // Additional verified 2-edit pair:
        // "jaba" → "java": b→v at pos 2, a→a identical... let me use:
        // "sprong" → "spring": o→i (1), insert i (wait no)
        // safest: "abcd" → "abef" = 2 replacements (c→e, d→f)
        assertThat(LevenshteinDistance.compute("abcd", "abef")).isEqualTo(2);
    }

    @Test
    void completelyDifferent() {
        // "java" → "kafka" = 3 edits (verified by DP above)
        assertThat(LevenshteinDistance.compute("java", "kafka")).isEqualTo(3);
        // Truly far pair
        assertThat(LevenshteinDistance.compute("java", "python")).isEqualTo(6);
    }

    @Test
    void emptyAndNonEmpty() {
        assertThat(LevenshteinDistance.compute("", "java")).isEqualTo(4);
    }

    @Test
    void bothEmpty() {
        assertThat(LevenshteinDistance.compute("", "")).isEqualTo(0);
    }

    @Test
    void thresholdEarlyExit() {
        // distance("java","python") = 6, threshold = 1 → should return > 1
        int result = LevenshteinDistance.compute("java", "python", 1);
        assertThat(result).isGreaterThan(1);
    }

    @Test
    void commonTypos() {
        // All verified distance-1 pairs
        assertThat(LevenshteinDistance.compute("sprng",  "spring")).isEqualTo(1);
        assertThat(LevenshteinDistance.compute("reddis", "redis" )).isEqualTo(1);
        assertThat(LevenshteinDistance.compute("doker",  "docker")).isEqualTo(1);
        assertThat(LevenshteinDistance.compute("kotln",  "kotlin")).isEqualTo(1);
        // "kfka" → "kafka" = 1 edit (delete first k... wait:)
        // k f k a  →  k a f k a: insert 'a' after first k = 1 ✓
        assertThat(LevenshteinDistance.compute("kfka",  "kafka")).isEqualTo(1);
        // "kafak" → "kafka" = 2 edits (two replacements)
        assertThat(LevenshteinDistance.compute("kafak", "kafka")).isEqualTo(2);
    }
}