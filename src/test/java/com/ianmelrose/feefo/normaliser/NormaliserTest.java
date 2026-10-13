package com.ianmelrose.feefo.normaliser;

import com.ianmelrose.feefo.normaliser.exception.NoMatchException;
import com.ianmelrose.feefo.normaliser.scoring.JaccardScorer;
import com.ianmelrose.feefo.normaliser.scoring.SimilarityScorer;
import com.ianmelrose.feefo.normaliser.tokenise.SimpleTokeniser;
import com.ianmelrose.feefo.normaliser.tokenise.Tokeniser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.ianmelrose.feefo.normaliser.Normaliser.DEFAULT_THRESHOLD;
import static com.ianmelrose.feefo.normaliser.Normaliser.DEFAULT_TITLES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormaliserTest {

    private static final Tokeniser TOKENISER = new SimpleTokeniser();
    private static final SimilarityScorer SCORER = new JaccardScorer();

    private final Normaliser normaliser = new Normaliser();

    private static Normaliser withThreshold(double threshold) {
        return new Normaliser(DEFAULT_TITLES, TOKENISER, SCORER, threshold);
    }

    // Examples from the brief

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

    static Stream<Arguments> scoredExamples() {
        return Stream.of(
                Arguments.of("Java engineer", "Software engineer", 1.0 / 3),
                Arguments.of("C# engineer", "Software engineer", 1.0 / 3),
                Arguments.of("Accountant", "Accountant", 1.0),
                Arguments.of("Chief Accountant", "Accountant", 0.5)
        );
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\" at {2}")
    @MethodSource("scoredExamples")
    void findBestMatchExposesScore(String input, String expectedTitle, double expectedScore) {
        assertThat(normaliser.findBestMatch(input)).hasValueSatisfying(result -> {
            assertThat(result.normalisedTitle()).isEqualTo(expectedTitle);
            assertThat(result.score()).isEqualTo(expectedScore);
        });
    }

    // Input validation

    @ParameterizedTest(name = "[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void rejectsNullOrBlankInput(String input) {
        assertThatThrownBy(() -> normaliser.normalise(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Construction validation

    static Stream<Arguments> invalidTitleLists() {
        return Stream.of(
                Arguments.of("null list", null),
                Arguments.of("empty list", List.of()),
                Arguments.of("null entry", Arrays.asList("Architect", null)),
                Arguments.of("blank entry", List.of("Architect", " ")),
                Arguments.of("entry with no tokens", List.of("Architect", "###")),
                Arguments.of("exact duplicate", List.of("Architect", "Architect")),
                Arguments.of("duplicate differing only by case", List.of("Architect", "ARCHITECT")),
                Arguments.of("duplicate differing only by punctuation",
                        List.of("Software engineer", "software-engineer"))
        );
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
        assertThatThrownBy(() -> new Normaliser(DEFAULT_TITLES, null, SCORER, DEFAULT_THRESHOLD))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullScorer() {
        assertThatThrownBy(() -> new Normaliser(DEFAULT_TITLES, TOKENISER, null, DEFAULT_THRESHOLD))
                .isInstanceOf(NullPointerException.class);
    }

    // Tie breaking

    static Stream<Arguments> tieCases() {
        return Stream.of(
                Arguments.of(List.of("Engineer", "Developer"), "Engineer"),
                Arguments.of(List.of("Developer", "Engineer"), "Developer")
        );
    }

    @ParameterizedTest(name = "titles {0} -> \"{1}\"")
    @MethodSource("tieCases")
    void earliestSuppliedTitleWinsOnEqualScore(List<String> titles, String expected) {
        // "Engineer developer" scores 0.5 against both titles
        assertThat(new Normaliser(titles).normalise("Engineer developer")).isEqualTo(expected);
    }

    // No match

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"Plumber", "#!?", "C#"})
    void inputSharingNoTokensIsNotAMatch(String input) {
        assertThat(normaliser.findBestMatch(input)).isEmpty();
        assertThatThrownBy(() -> normaliser.normalise(input))
                .isInstanceOf(NoMatchException.class)
                .hasMessageContaining(input);
    }

    @Test
    void noMatchExceptionCarriesInputAndThreshold() {
        assertThatThrownBy(() -> withThreshold(0.5).normalise("Java engineer"))
                .isInstanceOfSatisfying(NoMatchException.class, e -> {
                    assertThat(e.getInput()).isEqualTo("Java engineer");
                    assertThat(e.getThreshold()).isEqualTo(0.5);
                });
    }

    // Threshold

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

    // Collaborators

    @Test
    void usesInjectedScorer() {
        Normaliser custom = new Normaliser(DEFAULT_TITLES, TOKENISER, new FixedScorer("accountant"), DEFAULT_THRESHOLD);

        assertThat(custom.normalise("Java engineer")).isEqualTo("Accountant");
    }

    @Test
    void usesInjectedTokeniser() {
        // Tokenise on whitespace only, so "C#" survives as a token and "c#" can be a title.
        Tokeniser whitespaceOnly = text -> Set.of(text.toLowerCase().split("\\s+"));
        Normaliser custom = new Normaliser(List.of("c#", "java"), whitespaceOnly, SCORER, DEFAULT_THRESHOLD);

        assertThat(custom.normalise("C# developer")).isEqualTo("c#");
    }

    private record FixedScorer(String favouredToken) implements SimilarityScorer {

        @Override
        public double score(Set<String> input, Set<String> candidate) {
            return candidate.contains(favouredToken) ? 1.0 : 0.0;
        }
    }
}
