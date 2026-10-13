package com.ianmelrose.feefo.normaliser.tokenise;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lower-cases the text and splits it on any run of characters that is not a letter or digit.
 *
 * <p>Punctuation is discarded, so {@code "C#"} becomes {@code "c"} and {@code "Front-end"} becomes
 * {@code "front"} and {@code "end"}. Duplicate tokens collapse because the result is a set.
 */
public class SimpleTokeniser implements Tokeniser {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

    @Override
    public Set<String> tokenise(String text) {
        Objects.requireNonNull(text, "text to tokenise cannot be null");

        return Arrays.stream(NON_ALPHANUMERIC.split(text.toLowerCase(Locale.ROOT)))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
