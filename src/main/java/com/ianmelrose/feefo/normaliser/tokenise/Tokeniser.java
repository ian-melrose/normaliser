package com.ianmelrose.feefo.normaliser.tokenise;

import java.util.Set;

/**
 * Splits text into the set of tokens used for similarity scoring.
 *
 * <p>Implementations decide what counts as a token and how it is normalised (for example case
 * folding). They must return an empty set, never null, when the text contains no tokens, reject
 * null text, and be safe to call from multiple threads.
 */
@FunctionalInterface
public interface Tokeniser {

    Set<String> tokenise(String text);
}
