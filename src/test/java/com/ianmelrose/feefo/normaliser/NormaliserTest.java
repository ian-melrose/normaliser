package com.ianmelrose.feefo.normaliser;

import com.ianmelrose.feefo.normaliser.exception.NoMatchException;
import com.ianmelrose.feefo.normaliser.scoring.JaccardScorer;
import com.ianmelrose.feefo.normaliser.scoring.SimilarityScorer;
import com.ianmelrose.feefo.normaliser.tokenise.SimpleTokeniser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormaliserTest {

    private final Normaliser normaliser = new Normaliser();

    private static Normaliser withThreshold(double threshold) {
        return new Normaliser(Normaliser.DEFAULT_TITLES, new SimpleTokeniser(), new JaccardScorer(), threshold);
    }

    static Stream<Arguments> scoredExamples() {
        return Stream.of(
                Arguments.of("Java engineer", "Software engineer", 1.0 / 3),
                Arguments.of("C# engineer", "Software engineer", 1.0 / 3),
                Arguments.of("Accountant", "Accountant", 1.0),
                Arguments.of("Chief Accountant", "Accountant", 0.5)
        );
    }

    static Stream<Arguments> invalidTitleLists() {
        return Stream.of(
                Arguments.of("null list", null),
                Arguments.of("empty list", List.of()),
                Arguments.of("null entry", Arrays.asList("Architect", null)),
                Arguments.of("blank entry", List.of("Architect", " ")),
                Arguments.of("duplicate entry", List.of("Architect", "Architect"))
        );
    }

    static Stream<Arguments> tieCases() {
        return Stream.of(
                Arguments.of(List.of("Engineer", "Developer"), "Engineer"),
                Arguments.of(List.of("Developer", "Engineer"), "Developer")
        );
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "Java engineer,     Software engineer",
            "C# engineer,       Software engineer",
            "Accountant,        Accountant",
            "Chief Accountant,  Accountant",
            "JAVA ENGINEER,     Software engineer",
            "chief accountant,  Accountant"
    })
    void normalisesToExpectedTitle(String input, String expected) {
        assertThat(normaliser.normalise(input)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\" at {2}")
    @MethodSource("scoredExamples")
    void findBestMatchExposesScore(String input, String expectedTitle, double expectedScore) {
        assertThat(normaliser.findBestMatch(input)).hasValueSatisfying(result -> {
            assertThat(result.normalisedTitle()).isEqualTo(expectedTitle);
            assertThat(result.score()).isEqualTo(expectedScore);
        });
    }

    @ParameterizedTest(name = "[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void rejectsNullOrBlankInput(String input) {
        assertThatThrownBy(() -> normaliser.normalise(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTitleLists")
    void rejectsInvalidTitles(String description, List<String> titles) {
        assertThatThrownBy(() -> new Normaliser(titles))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "threshold {0}")
    @ValueSource(doubles = {-0.1, 1.1, Double.NaN})
    void rejectsThresholdOutsideRange(double threshold) {
        assertThatThrownBy(() -> withThreshold(threshold))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullTokeniser() {
        assertThatThrownBy(() -> new Normaliser(Normaliser.DEFAULT_TITLES, null, new JaccardScorer(), 0.0))
                .isInstanceOf(NullPointerException.class);
    }

    // Tie breaking

    @Test
    void rejectsNullScorer() {
        assertThatThrownBy(() -> new Normaliser(Normaliser.DEFAULT_TITLES, new SimpleTokeniser(), null, 0.0))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "titles {0} -> \"{1}\"")
    @MethodSource("tieCases")
    void earliestSuppliedTitleWinsOnEqualScore(List<String> titles, String expected) {
        // "Engineer developer" scores 0.5 against both titles
        assertThat(new Normaliser(titles).normalise("Engineer developer")).isEqualTo(expected);
    }

    // Threshold

    @Test
    void defaultThresholdAlwaysReturnsSomething() {
        assertThat(normaliser.findBestMatch("Plumber"))
                .hasValueSatisfying(result -> assertThat(result.score()).isEqualTo(0.0));
    }

    @ParameterizedTest(name = "\"{0}\" at threshold {1}")
    @CsvSource({
            "Plumber,          0.3",
            "Java engineer,    0.5",
            "Chief Accountant, 0.51"
    })
    void findBestMatchReturnsEmptyBelowThreshold(String input, double threshold) {
        assertThat(withThreshold(threshold).findBestMatch(input)).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\" at threshold {1}")
    @CsvSource({
            "Plumber,       0.3",
            "Java engineer, 0.5"
    })
    void normaliseThrowsBelowThreshold(String input, double threshold) {
        assertThatThrownBy(() -> withThreshold(threshold).normalise(input))
                .isInstanceOf(NoMatchException.class)
                .hasMessageContaining(input);
    }

    @ParameterizedTest(name = "\"{0}\" at threshold {1} -> \"{2}\"")
    @CsvSource({
            "Chief Accountant, 0.5, Accountant",
            "Accountant,       1.0, Accountant"
    })
    void thresholdIsInclusive(String input, double threshold, String expected) {
        assertThat(withThreshold(threshold).normalise(input)).isEqualTo(expected);
    }

    @Test
    void usesInjectedScorer() {
        Normaliser custom = new Normaliser(Normaliser.DEFAULT_TITLES, new SimpleTokeniser(), new FixedScorer("accountant"), 0.0);

        assertThat(custom.normalise("Java engineer")).isEqualTo("Accountant");
    }

    @ParameterizedTest(name = "\"{0}\" -> score {1}")
    @CsvSource({
            "Accountant,    1.0",
            "Java engineer, 0.0"
    })
    void usesInjectedTokeniser(String input, double expectedScore) {
        Normaliser custom = new Normaliser(Normaliser.DEFAULT_TITLES, Set::of, new JaccardScorer(), 0.0);

        assertThat(custom.findBestMatch(input))
                .hasValueSatisfying(result -> assertThat(result.score()).isEqualTo(expectedScore));
    }

    private record FixedScorer(String favouredToken) implements SimilarityScorer {

        @Override
            public double score(Set<String> input, Set<String> candidate) {
                if (candidate.contains(favouredToken)) {
                    return 1.0;
                }

                return 0.0;
            }
        }
}