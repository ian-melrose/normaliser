package com.ianmelrose.feefo.normaliser.scoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JaccardScorerTest {

    private final JaccardScorer scorer = new JaccardScorer();

    @ParameterizedTest(name = "{0} vs {1} -> {2}")
    @MethodSource("scoreCases")
    void scoresIntersectionOverUnion(Set<String> a, Set<String> b, double expected) {
        assertThat(scorer.score(a, b)).isEqualTo(expected);
    }

    static Stream<Arguments> scoreCases() {
        return Stream.of(
                Arguments.of(Set.of("a", "b"), Set.of("a", "b"), 1.0),
                Arguments.of(Set.of("a"), Set.of("b"), 0.0),
                Arguments.of(Set.of("java", "engineer"), Set.of("software", "engineer"), 1.0 / 3.0),
                Arguments.of(Set.of("chief", "accountant"), Set.of("accountant"), 0.5),
                Arguments.of(Set.of(), Set.of("a"), 0.0),
                Arguments.of(Set.of("a"), Set.of(), 0.0),
                Arguments.of(Set.of(), Set.of(), 0.0)
        );
    }

    @ParameterizedTest(name = "{0} vs {1}")
    @MethodSource("asymmetricPairs")
    void isSymmetric(Set<String> a, Set<String> b) {
        assertThat(scorer.score(a, b)).isEqualTo(scorer.score(b, a));
    }

    static Stream<Arguments> asymmetricPairs() {
        return Stream.of(
                Arguments.of(Set.of("chief", "accountant"), Set.of("accountant")),
                Arguments.of(Set.of("java", "engineer"), Set.of("software", "engineer")),
                Arguments.of(Set.of(), Set.of("a"))
        );
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> scorer.score(null, Set.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> scorer.score(Set.of(), null)).isInstanceOf(NullPointerException.class);
    }
}