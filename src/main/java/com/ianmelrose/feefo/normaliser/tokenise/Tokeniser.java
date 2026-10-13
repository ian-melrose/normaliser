package com.ianmelrose.feefo.normaliser.tokenise;

import java.util.Set;

public interface Tokeniser {
    Set<String> tokenise(String text);
}