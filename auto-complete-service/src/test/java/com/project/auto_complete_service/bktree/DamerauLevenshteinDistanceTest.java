package com.project.auto_complete_service.bktree;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class DamerauLevenshteinDistanceTest {


    @Test
    void shouldHandleTransposition() {

        assertThat(
                DamerauLevenshteinDistance.compute(
                        "kafak",
                        "kafka"
                )
        ).isEqualTo(1);
    }


    @Test
    void shouldHandleInsertion() {

        assertThat(
                DamerauLevenshteinDistance.compute(
                        "jva",
                        "java"
                )
        ).isEqualTo(1);
    }


    @Test
    void shouldHandleDeletion() {

        assertThat(
                DamerauLevenshteinDistance.compute(
                        "spriing",
                        "spring"
                )
        ).isEqualTo(1);
    }


    @Test
    void shouldHandleExactMatch() {

        assertThat(
                DamerauLevenshteinDistance.compute(
                        "redis",
                        "redis"
                )
        ).isZero();
    }
}