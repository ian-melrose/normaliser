package com.ianmelrose.feefo.normaliser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormalisationResultTest {

    private static final String ANY_TITLE = "Architect";

    @ParameterizedTest(name = "score {0}")
    @ValueSource(doubles = {0.0, 0.5, 1.0})
    void acceptsScoresWithinRange(double score) {
        assertThat(new NormalisationResult(ANY_TITLE, score).score()).isEqualTo(score);
    }

    @ParameterizedTest(name = "score {0}")
    @ValueSource(doubles = {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsScoresOutsideRange(double score) {
        assertThatThrownBy(() -> new NormalisationResult(ANY_TITLE, score))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullTitle() {
        assertThatThrownBy(() -> new NormalisationResult(null, 1.0))
                .isInstanceOf(NullPointerException.class);
    }
}
