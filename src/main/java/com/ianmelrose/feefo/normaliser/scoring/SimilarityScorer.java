package com.ianmelrose.feefo.normaliser.scoring;

import java.util.Set;

/**
 * Scores how similar two token sets are.
 *
 * <p>Implementations must return a value in the range {@code 0.0} (nothing in common) to
 * {@code 1.0} (identical), never NaN, and must be safe to call from multiple threads.
 */
@FunctionalInterface
public interface SimilarityScorer {

    double MIN_SCORE = 0.0;
    double MAX_SCORE = 1.0;

    double score(Set<String> inputTokens, Set<String> candidateTokens);

    /**
     * @return {@code value} if it is a valid score
     * @throws IllegalArgumentException if {@code value} is NaN or outside {@link #MIN_SCORE} to
     *                                  {@link #MAX_SCORE}
     */
    static double requireValidScore(double value, String name) {
        if (Double.isNaN(value) || value < MIN_SCORE || value > MAX_SCORE) {
            throw new IllegalArgumentException(
                    name + " must be between " + MIN_SCORE + " and " + MAX_SCORE + ", was " + value);
        }
        return value;
    }
}
