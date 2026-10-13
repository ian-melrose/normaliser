package com.ianmelrose.feefo.normaliser.tokenise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleTokeniserTest {

    private final SimpleTokeniser tokeniser = new SimpleTokeniser();

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @MethodSource("tokenisationCases")
    void tokenises(String input, Set<String> expected) {
        assertThat(tokeniser.tokenise(input)).containsExactlyInAnyOrderElementsOf(expected);
    }

    static Stream<Arguments> tokenisationCases() {
        return Stream.of(
                Arguments.of("Software Engineer", Set.of("software", "engineer")),
                Arguments.of("C# engineer", Set.of("c", "engineer")),
                Arguments.of("Front-end developer", Set.of("front", "end", "developer")),
                Arguments.of("  Chief   Accountant  ", Set.of("chief", "accountant")),
                Arguments.of("engineer engineer", Set.of("engineer")));
    }

    @ParameterizedTest(name = "\"{0}\" -> empty")
    @ValueSource(strings = {"", "   ", "#!?", " - "})
    void returnsEmptySetWhenNoTokens(String input) {
        assertThat(tokeniser.tokenise(input)).isEmpty();
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> tokeniser.tokenise(null)).isInstanceOf(NullPointerException.class);
    }
}