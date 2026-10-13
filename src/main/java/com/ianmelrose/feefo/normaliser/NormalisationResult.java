package com.ianmelrose.feefo.normaliser;

import java.util.Objects;

public record NormalisationResult(String normalisedTitle, double score) {
    public NormalisationResult {
        Objects.requireNonNull(normalisedTitle, "normalisedTitle cannot be null");
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0, was " + score);
        }
    }
}