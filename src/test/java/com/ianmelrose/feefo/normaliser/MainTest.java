package com.ianmelrose.feefo.normaliser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void printsBriefExamplesWithScores() {
        String output = run(Main.BRIEF_EXAMPLES);

        assertThat(output.lines()).containsExactly(
                "\"Java engineer\"      -> \"Software engineer\"  (score 0.33)",
                "\"C# engineer\"        -> \"Software engineer\"  (score 0.33)",
                "\"Accountant\"         -> \"Accountant\"         (score 1.00)",
                "\"Chief Accountant\"   -> \"Accountant\"         (score 0.50)"
        );
    }

    @Test
    void reportsUnmatchedInput() {
        assertThat(run(List.of("Plumber")).lines()).containsExactly("\"Plumber\"            -> no match");
    }

    private static String run(List<String> inputs) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Main.run(inputs, new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
