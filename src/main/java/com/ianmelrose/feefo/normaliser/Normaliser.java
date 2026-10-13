package com.ianmelrose.feefo.normaliser;

import com.ianmelrose.feefo.normaliser.exception.NoMatchException;
import com.ianmelrose.feefo.normaliser.scoring.JaccardScorer;
import com.ianmelrose.feefo.normaliser.scoring.SimilarityScorer;
import com.ianmelrose.feefo.normaliser.tokenise.SimpleTokeniser;
import com.ianmelrose.feefo.normaliser.tokenise.Tokeniser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Maps a free-text job title onto the closest entry in a fixed list of normalised titles.
 *
 * <p>Each title is tokenised once at construction time. On every call the input is tokenised the
 * same way and scored against every candidate with a {@link SimilarityScorer}. The candidate with
 * the highest score wins; on an exact tie the title supplied earliest in the list wins.
 *
 * <p>A score of {@code 0.0} is never treated as a match, regardless of the configured threshold:
 * an input that shares nothing with a candidate has no "best" match. The threshold then raises
 * the bar further and is inclusive, so a score equal to the threshold is accepted.
 *
 * <p>Instances are immutable and safe to share between threads.
 */
public final class Normaliser {

    public static final List<String> DEFAULT_TITLES =
            List.of("Architect", "Software engineer", "Quantity surveyor", "Accountant");

    public static final double DEFAULT_THRESHOLD = 0.0;

    private static final Comparator<ScoredCandidate> BEST_SCORE_THEN_EARLIEST =
            Comparator.comparingDouble((ScoredCandidate scored) -> scored.result().score())
                    .thenComparing(ScoredCandidate::index, Comparator.reverseOrder());

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

    /**
     * @param titles    the normalised titles to match against, in priority order for tie-breaking
     * @param tokeniser splits titles and inputs into comparable tokens
     * @param scorer    scores an input's tokens against a candidate's tokens in the range 0.0 to 1.0
     * @param threshold the minimum (inclusive) score for a candidate to count as a match
     * @throws IllegalArgumentException if {@code threshold} is NaN or outside 0.0 to 1.0, or if
     *                                  {@code titles} is null, empty, contains null or blank entries,
     *                                  contains an entry with no tokens, or contains two entries
     *                                  that tokenise identically
     */
    public Normaliser(List<String> titles, Tokeniser tokeniser, SimilarityScorer scorer, double threshold) {
        this.tokeniser = Objects.requireNonNull(tokeniser, "tokeniser cannot be null");
        this.scorer = Objects.requireNonNull(scorer, "scorer cannot be null");
        this.threshold = SimilarityScorer.requireValidScore(threshold, "threshold");
        this.candidates = validateAndTokenise(titles);
    }

    /**
     * @return the normalised title that best matches {@code input}
     * @throws IllegalArgumentException if {@code input} is null or blank
     * @throws NoMatchException         if no candidate scores above zero and at or above the threshold
     */
    public String normalise(String input) {
        return findBestMatch(input)
                .map(NormalisationResult::normalisedTitle)
                .orElseThrow(() -> new NoMatchException(input, threshold));
    }

    /**
     * @return the best match for {@code input} with its score, or empty when no candidate scores
     *         above zero and at or above the threshold
     * @throws IllegalArgumentException if {@code input} is null or blank
     */
    public Optional<NormalisationResult> findBestMatch(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input must not be null or blank");
        }

        Set<String> inputTokens = tokeniser.tokenise(input);

        return IntStream.range(0, candidates.size())
                .mapToObj(index -> score(index, inputTokens))
                .max(BEST_SCORE_THEN_EARLIEST)
                .map(ScoredCandidate::result)
                .filter(this::isMatch);
    }

    private boolean isMatch(NormalisationResult result) {
        return result.score() > 0.0 && result.score() >= threshold;
    }

    private List<Candidate> validateAndTokenise(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            throw new IllegalArgumentException("titles must not be null or empty");
        }

        List<Candidate> tokenised = new ArrayList<>(titles.size());
        Set<Set<String>> seenTokens = new HashSet<>();

        for (String title : titles) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("titles must not contain null or blank entries");
            }

            Set<String> tokens = tokeniser.tokenise(title);
            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("title '" + title + "' contains no matchable tokens");
            }

            if (!seenTokens.add(tokens)) {
                throw new IllegalArgumentException(
                        "title '" + title + "' duplicates an earlier title once tokenised");
            }

            tokenised.add(new Candidate(title, tokens));
        }

        return List.copyOf(tokenised);
    }

    private ScoredCandidate score(int index, Set<String> inputTokens) {
        Candidate candidate = candidates.get(index);
        double score = scorer.score(inputTokens, candidate.tokens());

        return new ScoredCandidate(index, new NormalisationResult(candidate.title(), score));
    }

    private record Candidate(String title, Set<String> tokens) {
    }

    private record ScoredCandidate(int index, NormalisationResult result) {
    }
}
