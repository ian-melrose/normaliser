package com.ianmelrose.feefo.normaliser.scoring;

import java.util.Objects;
import java.util.Set;

public class JaccardScorer implements SimilarityScorer {

    @Override
    public double score(Set<String> inputTokens, Set<String> candidateTokens) {
        Objects.requireNonNull(inputTokens, "inputTokens cannot be null");
        Objects.requireNonNull(candidateTokens, "candidateTokens cannot be null");

        if (inputTokens.isEmpty() && candidateTokens.isEmpty()) {
            return 0.0;
        }

        long intersection = inputTokens.stream()
                .filter(candidateTokens::contains)
                .count();
        long union = inputTokens.size() + candidateTokens.size() - intersection;

        return (double) intersection / union;
    }
}
