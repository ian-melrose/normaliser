package com.ianmelrose.feefo.normaliser;

import com.ianmelrose.feefo.normaliser.exception.NoMatchException;
import com.ianmelrose.feefo.normaliser.scoring.JaccardScorer;
import com.ianmelrose.feefo.normaliser.scoring.SimilarityScorer;
import com.ianmelrose.feefo.normaliser.tokenise.SimpleTokeniser;
import com.ianmelrose.feefo.normaliser.tokenise.Tokeniser;

import java.util.*;
import java.util.stream.IntStream;

public final class Normaliser {
    public static final List<String> DEFAULT_TITLES =
            List.of("Architect", "Software engineer", "Quantity surveyor", "Accountant");
    public static final double DEFAULT_THRESHOLD = 0.0;

    private final List<Candidate> candidates;
    private final Tokeniser tokeniser;
    private final SimilarityScorer scorer;
    private final double threshold;

    public Normaliser() {
        this(DEFAULT_TITLES);
    }

    public Normaliser(List<String> titles) {
        this(titles, new SimpleTokeniser(), new JaccardScorer(), DEFAULT_THRESHOLD);
    }

    public Normaliser(List<String> titles, Tokeniser tokeniser, SimilarityScorer scorer, double threshold) {
        this.tokeniser = Objects.requireNonNull(tokeniser, "tokeniser cannot be null");
        this.scorer = Objects.requireNonNull(scorer, "scorer cannot be null");
        if (Double.isNaN(threshold) || threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be between 0.0 and 1.0");
        }
        this.threshold = threshold;
        this.candidates = validateAndTokenise(titles);
    }

    public String normalise(String input) {
        return findBestMatch(input)
                .map(NormalisationResult::normalisedTitle)
                .orElseThrow(() -> new NoMatchException(input, threshold));
    }

    public Optional<NormalisationResult> findBestMatch(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input must not be null or blank");
        }

        Set<String> inputTokens = tokeniser.tokenise(input);

        return IntStream.range(0, candidates.size())
                .mapToObj(index -> score(index, inputTokens))
                .max(BEST_SCORE_THEN_EARLIEST)
                .map(ScoredCandidate::result)
                .filter(result -> result.score() >= threshold);
    }

    private List<Candidate> validateAndTokenise(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            throw new IllegalArgumentException("titles must not be null or empty");
        }

        if (titles.stream().anyMatch(title -> title == null || title.isBlank())) {
            throw new IllegalArgumentException("titles must not contain null or blank entries");
        }

        if (titles.stream().distinct().count() != titles.size()) {
            throw new IllegalArgumentException("titles must not contain duplicates");
        }

        return titles.stream()
                .map(title -> new Candidate(title, tokeniser.tokenise(title)))
                .toList();
    }

    private ScoredCandidate score(int index, Set<String> inputTokens) {
        Candidate candidate = candidates.get(index);
        double score = scorer.score(inputTokens, candidate.tokens());

        return new ScoredCandidate(index, new NormalisationResult(candidate.title(), score));
    }

    private static final Comparator<ScoredCandidate> BEST_SCORE_THEN_EARLIEST =
            Comparator.comparingDouble((ScoredCandidate scored) -> scored.result().score())
                    .thenComparing(ScoredCandidate::index, Comparator.reverseOrder());

    private record Candidate(String title, Set<String> tokens) {
    }

    private record ScoredCandidate(int index, NormalisationResult result) {
    }
}
