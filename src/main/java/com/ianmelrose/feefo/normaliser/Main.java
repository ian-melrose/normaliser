package com.ianmelrose.feefo.normaliser;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

/**
 * Command-line demonstration. With no arguments it runs the examples from the assessment brief;
 * otherwise each argument is normalised in turn.
 */
public final class Main {

    static final List<String> BRIEF_EXAMPLES =
            List.of("Java engineer", "C# engineer", "Accountant", "Chief Accountant");

    private Main() {
    }

    public static void main(String[] args) {
        run(args.length == 0 ? BRIEF_EXAMPLES : List.of(args), System.out);
    }

    static void run(List<String> inputs, PrintStream out) {
        Normaliser normaliser = new Normaliser();

        for (String input : inputs) {
            normaliser.findBestMatch(input).ifPresentOrElse(
                    result -> out.printf(Locale.ROOT, "%-20s -> %-20s (score %.2f)%n",
                            quote(input), quote(result.normalisedTitle()), result.score()),
                    () -> out.printf(Locale.ROOT, "%-20s -> no match%n", quote(input)));
        }
    }

    private static String quote(String text) {
        return "\"" + text + "\"";
    }
}
