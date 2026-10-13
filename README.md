# Job title normaliser

Maps a free-text job title onto the closest entry in a fixed list of normalised titles.

```java
Normaliser n = new Normaliser();
n.normalise("Java engineer");     // "Software engineer"
n.normalise("C# engineer");       // "Software engineer"
n.normalise("Accountant");        // "Accountant"
n.normalise("Chief Accountant");  // "Accountant"
```

## Requirements

Java 21. The build declares a Java 21 toolchain and uses the Foojay resolver, so Gradle will
download a JDK if it can't find one. The Gradle wrapper is included.

## Running

```bash
# unit tests, one line per case
./gradlew test                                          
# the four examples from the brief
./gradlew run         
# your own inputs                                  
./gradlew run --args="'Senior software engineer' Plumber"   
```

The HTML test report ends up in `build/reports/tests/test/index.html`. The GitHub Actions
workflow in `.github/workflows/ci.yml` runs the same tests on every push and uploads that report.

## How it works

`SimpleTokeniser` lower-cases the text and splits it on anything that isn't a letter or digit,
so `"C# engineer"` becomes `{c, engineer}`. Each normalised title is tokenised once when the
`Normaliser` is built.

`JaccardScorer` compares the input's tokens with each title's tokens: shared tokens divided by
total distinct tokens. That is the quality score `q` from the brief, with `0.0 <= q <= 1.0`.

The highest score wins. On a tie the title that came first in the list wins, so list order is
priority order.

| Input              | Tokens                | Best title          | Shared / union | Score |
|--------------------|-----------------------|---------------------|----------------|-------|
| `Java engineer`    | `{java, engineer}`    | `Software engineer` | 1 / 3          | 0.33  |
| `C# engineer`      | `{c, engineer}`       | `Software engineer` | 1 / 3          | 0.33  |
| `Accountant`       | `{accountant}`        | `Accountant`        | 1 / 1          | 1.00  |
| `Chief Accountant` | `{chief, accountant}` | `Accountant`        | 1 / 2          | 0.50  |

### No match

A score of `0.0` never counts as a match. If the input shares no tokens with any title, as with
`"Plumber"`, `normalise` throws `NoMatchException` and `findBestMatch` returns an empty
`Optional`. The alternative, handing back whichever title happens to be first in the list, would
be wrong data that looks right.

A threshold can raise that minimum. It defaults to `0.0` and is inclusive, so at `0.5` the input
`"Chief Accountant"` still matches `Accountant`. Use `findBestMatch` if you want the score as
well as the title, or would rather check an `Optional` than catch an exception.

## Design notes

`Tokeniser` and `SimilarityScorer` are single-method interfaces passed into `Normaliser`, so the
tokenisation rules or the similarity measure can be replaced without touching the matching code.
The tests do exactly that with a whitespace-only tokeniser and a fixed scorer.

`Normaliser` is final, every field is final, and the title list is copied on the way in. Titles
are tokenised once in the constructor and reused on every call, and instances can be shared
between threads.

Bad configuration fails in the constructor rather than later. A null or empty title list, null
or blank entries, entries with no tokens (`"###"` could never match anything), and entries that
tokenise the same as an earlier one (`"Architect"` and `"ARCHITECT"`, where the second could
never be chosen) all throw `IllegalArgumentException`. Thresholds outside `0.0` to `1.0`, or
`NaN`, are rejected too, and `NormalisationResult` checks its own score so a broken scorer can't
produce an invalid result.

`normalise` keeps the `String` signature from the brief and throws `NoMatchException`, which
carries the input and the threshold, when nothing qualifies. `findBestMatch` returns
`Optional<NormalisationResult>` for callers that want the score.

## Tests

One test class per production class, all JUnit 5 with AssertJ. `NormaliserTest` covers the
brief's examples and their scores, case-insensitivity, tie-breaking, thresholds, the no-match
rule, constructor validation and the injected collaborators. The scorer and tokeniser have their
own tests for edge cases such as empty sets and punctuation-only input. `MainTest` checks the
demo's output.

## Limitations

The matching is simple on purpose. Things it doesn't do, and where each would go if it were needed:

- Plurals and stemming: `"Software engineers"` doesn't share a token with `"Software engineer"`.
  This belongs in a `Tokeniser`.
- Synonyms: `"Developer"` scores `0.0` against `"Software engineer"`. Also a `Tokeniser` concern,
  expanding terms from a synonym map.
- Typos: `"Acountant"` scores `0.0`. A `SimilarityScorer` using edit distance between tokens
  would handle this.
- Short tokens: `"C#"` becomes `"c"`, harmless with four titles but a source of false overlaps with
  a bigger list. A minimum token length in the `Tokeniser` would fix it.
- Weighting: every token counts the same, so `"chief"` and `"accountant"` carry equal weight.
  A `SimilarityScorer` with TF-IDF weighting would favour the distinctive tokens.

None of these need changes to `Normaliser` itself.

## Project layout

```
src/main/java/com/ianmelrose/feefo/normaliser/
  Normaliser.java              matching and validation
  NormalisationResult.java     title + score
  Main.java                    command-line demo
  exception/NoMatchException.java
  scoring/SimilarityScorer.java, JaccardScorer.java
  tokenise/Tokeniser.java, SimpleTokeniser.java
src/test/java/...              one test class per production class
```
