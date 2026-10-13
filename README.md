# Job title normaliser

A small Java library that maps a free-text job title onto the closest entry in a fixed list of
normalised titles.

```java
Normaliser n = new Normaliser();
n.normalise("Java engineer");     // "Software engineer"
n.normalise("C# engineer");       // "Software engineer"
n.normalise("Accountant");        // "Accountant"
n.normalise("Chief Accountant");  // "Accountant"
```

## Requirements

- Java 21. The build declares a Java 21 toolchain and uses the Foojay resolver, so Gradle will
  download a matching JDK if one is not already installed.
- Nothing else. The Gradle wrapper is included.

## Running

```bash
./gradlew test            # run the unit tests (74 tests, results printed per test)
./gradlew run             # normalise the four examples from the brief
./gradlew run --args="'Senior Java developer' Plumber"   # normalise your own inputs
```

The test report is written to `build/reports/tests/test/index.html`. The GitHub Actions workflow in
`.github/workflows/ci.yml` runs the same test task on every push and uploads that report.

## How it works

1. **Tokenise.** `SimpleTokeniser` lower-cases the text and splits it on any run of characters
   that is not a letter or digit. Punctuation is dropped, so `"C# engineer"` becomes
   `{c, engineer}`. Each normalised title is tokenised once when the `Normaliser` is built.
2. **Score.** `JaccardScorer` compares the input's token set with each candidate's token set:
   the size of the intersection divided by the size of the union. This gives the quality score
   `q` suggested in the brief, where `0.0 <= q <= 1.0`.
3. **Pick.** The candidate with the highest score wins. On an exact tie the title supplied
   earliest in the list wins, so the order of the title list expresses priority.

Worked examples against the default titles `Architect`, `Software engineer`,
`Quantity surveyor`, `Accountant`:

| Input              | Tokens                | Best candidate      | Shared / union | Score |
|--------------------|-----------------------|---------------------|----------------|-------|
| `Java engineer`    | `{java, engineer}`    | `Software engineer` | 1 / 3          | 0.33  |
| `C# engineer`      | `{c, engineer}`       | `Software engineer` | 1 / 3          | 0.33  |
| `Accountant`       | `{accountant}`        | `Accountant`        | 1 / 1          | 1.00  |
| `Chief Accountant` | `{chief, accountant}` | `Accountant`        | 1 / 2          | 0.50  |

### When there is no match

A score of `0.0` is never treated as a match. An input that shares no tokens with any title,
such as `"Plumber"`, has no meaningful "best" match, so `normalise` throws `NoMatchException`
and `findBestMatch` returns an empty `Optional`. Returning an arbitrary title in that case
would silently produce wrong data.

On top of that rule an optional **threshold** (default `0.0`) sets the minimum acceptable score.
It is inclusive, so with a threshold of `0.5` the input `"Chief Accountant"` still matches
`Accountant`. Callers that want to inspect the score rather than catch an exception should use
`findBestMatch`, which returns a `NormalisationResult` holding the title and its score.

## Design notes

- **Small, single-purpose types.** `Tokeniser` and `SimilarityScorer` are one-method interfaces
  injected into `Normaliser`, so the tokenisation rules or the similarity measure can be swapped
  without touching the matching logic. The tests exercise this by injecting a whitespace-only
  tokeniser and a fixed scorer.
- **Immutable and thread-safe.** `Normaliser` is `final`, holds only final fields, and copies the
  title list defensively. Tokenised titles are computed once and reused for every call.
- **Fail fast on bad configuration.** The constructor rejects a null or empty title list, null or
  blank entries, entries that produce no tokens (for example `"###"`, which could never match),
  and entries that tokenise identically (for example `"Architect"` and `"ARCHITECT"`), because
  the later one could never be chosen. Thresholds outside `0.0` to `1.0` or `NaN` are rejected,
  and `NormalisationResult` guards its own score range so a misbehaving scorer cannot produce an
  invalid result.
- **Explicit outcomes.** `normalise` keeps the simple `String` signature from the brief and
  throws `NoMatchException` (carrying the input and threshold) when nothing qualifies.
  `findBestMatch` offers the richer `Optional<NormalisationResult>` for callers that want the
  score or prefer not to handle exceptions.

## Tests

| Test class                | Covers                                                                         |
|---------------------------|--------------------------------------------------------------------------------|
| `NormaliserTest`          | Brief examples, scores, case-insensitivity, tie-breaking, thresholds, no-match handling, constructor validation, injected collaborators |
| `JaccardScorerTest`       | Intersection-over-union values, symmetry, empty sets, null arguments           |
| `SimpleTokeniserTest`     | Case folding, punctuation and whitespace handling, duplicate collapse, empty input, null |
| `NormalisationResultTest` | Score range and NaN guards, null title                                         |
| `MainTest`                | Demo output for the brief examples and for an unmatched input                  |

## Limitations and possible extensions

The matching is deliberately simple and transparent. Known gaps, all addressable through the
`Tokeniser` and `SimilarityScorer` extension points without changing `Normaliser`:

- **No stemming or plurals.** `"Software engineers"` does not share a token with
  `"Software engineer"`. A stemming tokeniser would fix this.
- **No synonyms.** `"Developer"` or `"Programmer"` score `0.0` against `"Software engineer"`.
  A tokeniser backed by a synonym map could expand such terms.
- **No typo tolerance.** `"Acountant"` scores `0.0`. A scorer using Jaro-Winkler or edit
  distance between tokens would allow fuzzy matches.
- **Short tokens are noise.** `"C#"` tokenises to `"c"`, which is harmless here but could
  cause spurious overlaps with a larger title list. A stop-word or minimum-length filter in the
  tokeniser would address this.
- **Equal weighting.** Every token counts the same, so `"Chief"` and `"Accountant"` contribute
  equally. TF-IDF style weighting would favour the distinctive tokens.

## Project layout

```
src/main/java/com/ianmelrose/feefo/normaliser/
  Normaliser.java              matching logic and validation
  NormalisationResult.java     title + score value object
  Main.java                    command-line demo
  exception/NoMatchException.java
  scoring/SimilarityScorer.java, JaccardScorer.java
  tokenise/Tokeniser.java, SimpleTokeniser.java
src/test/java/...              one test class per production class
```
