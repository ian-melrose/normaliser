package com.ianmelrose.feefo.normaliser.exception;

public class NoMatchException extends RuntimeException {
    public NoMatchException(String input, double threshold) {
        super("No normalised title matched '%s' at or above threshold %.2f".formatted(input, threshold));
    }
}