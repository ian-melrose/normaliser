package com.ianmelrose.feefo.normaliser.exception;

import java.util.Locale;

/**
 * Thrown by {@code Normaliser.normalise} when no normalised title qualifies as a match for the input.
 */
public class NoMatchException extends RuntimeException {

    private final String input;
    private final double threshold;

    public NoMatchException(String input, double threshold) {
        super(String.format(Locale.ROOT,
                "No normalised title matched '%s' at or above threshold %.2f", input, threshold));
        this.input = input;
        this.threshold = threshold;
    }

    public String getInput() {
        return input;
    }

    public double getThreshold() {
        return threshold;
    }
}
