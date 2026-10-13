package com.ianmelrose.feefo.normaliser.scoring;

import java.util.Set;

public interface SimilarityScorer {
    double score(Set<String> inputTokens, Set<String> candidateTokens);
}
