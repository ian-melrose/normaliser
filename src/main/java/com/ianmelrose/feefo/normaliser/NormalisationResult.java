package com.ianmelrose.feefo.normaliser;

import com.ianmelrose.feefo.normaliser.scoring.SimilarityScorer;

import java.util.Objects;

/**
 * The outcome of a successful normalisation: the chosen title and how well it matched.
 *
 * @param normalisedTitle the matched title, exactly as supplied to the {@link Normaliser}
 * @param score           the quality of the match, where {@code 0.0} is no overlap and
 *                        {@code 1.0} is identical
 */
public record NormalisationResult(String normalisedTitle, double score) {

    public NormalisationResult {
        Objects.requireNonNull(normalisedTitle, "normalisedTitle cannot be null");
        SimilarityScorer.requireValidScore(score, "score");
    }
}
