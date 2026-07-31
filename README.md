# PUnit Examples

Example applications and probabilistic tests demonstrating the
[PUnit](https://github.com/mavai-org/punit) framework. The project models
realistic service contracts — an LLM-powered shopping assistant and a payment gateway
with SLA requirements — and shows how to apply statistical rigour at every
stage of the testing lifecycle.

For framework concepts and configuration details see the
[PUnit User Guide](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md).
The service contracts here are written in the contract-first style documented in
[Part 1: The Service Contract](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md#part-1-the-service-contract--the-shared-correctness-target).

## The declarative path — start here

The fastest way in is a YAML contract plus a one-line test — no builder
vocabulary on first contact. The worked example lives in
`src/test/java/org/mavai/punit/examples/declarative/`:

- **`shopping-basket.yaml`** (test resources, same package) — the claim:
  the service, the inputs, one thresholded criterion over a `json` view.
- **`MavaiBindings.java`** — the whole code surface: one `@Binding`
  method making the service call.
- **`ShoppingBasketDeclarativeTest.java`** — the test:
  `PUnit.declared().samples(100).assertPasses();`

```bash
./gradlew mavaiCheck                                        # validate every contract, zero samples
./gradlew test --tests ShoppingBasketDeclarativeTest        # run the declarative example
```

**The pure-services variant** goes further: `shopping-basket-service.yaml`
names a service declared entirely in `mavai-services.yaml` — a
`language-model` type with a temperature exploration and a
`prompt-engineer` optimization — **no bindings class at all**. A bundled
stub endpoint (`StubLanguageModel`) stands in for the model, so
everything runs offline through the real punit-lm wire path, token
usage included:

```bash
./gradlew test --tests ShoppingBasketServiceTest        # the one-line test
./gradlew exp -Prun=ShoppingBasketServiceExperiments    # explore + optimize
```

Artefacts land under `build/punit/explorations/` and
`build/punit/optimizations/` with `totalTokens`/`avgTokensPerSample` in
their cost blocks — render them with the shared `mavai` tool and the
cost cells read "ms · tok".

When a claim outgrows the file, graduate: `./gradlew mavaiMaterialise`
emits the equivalent `ServiceContract` class under
`build/punit/materialised/` — the same criteria the file instantiated,
as Java source that is now yours. The rest of this repository shows
that full-API style; see the user guide's
[declarative part](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md#the-declarative-surface--contracts-as-files)
for the format.

## Project structure

A standard single-module Gradle / Maven layout — no special wiring is required to use PUnit:

```
src/main/java/org/mavai/punit/examples/
  app/         Domain classes — shopping actions, LLM integrations, payment gateway.
  servicecontracts/    Service contract definitions (the contract-first authoring surface).
  sentinels/   Sentinel-deployable reliability classes.

src/test/java/org/mavai/punit/examples/
  app/                     Unit tests for the domain code.
  experiments/             EXPLORE, MEASURE, OPTIMIZE experiments.
  probabilistictests/      Probabilistic tests of the service contracts.
  integration/             Operational-flow integration tests.
  architecture/            ArchUnit rules.

src/test/resources/        Test fixtures + PUnit baseline files (see below).
```

The service contracts and sentinels live in `src/main/` rather than `src/test/` because the same classes must be deployable as a sentinel JAR (see [Part 10: Sentinels](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md#part-10-sentinels--production-time-execution) in the user guide). The test stack (`punit-report`, JUnit, AssertJ, ArchUnit) is `testImplementation`, so it stays out of the sentinel JAR's runtime classpath.

## Service contracts

### Shopping basket

An LLM translates natural-language instructions ("add 2 apples") into
structured JSON shopping actions. The service contract validates that the
response is non-blank, parses as valid JSON, and contains only actions
appropriate to a shopping context.

This service contract demonstrates covariates (model, temperature, time of day),
input cycling, budget management (token tracking), exception handling modes,
and pacing constraints.

### Payment gateway

A mock gateway processes card payments with configurable latency and failure
rates. The service contract asserts both functional correctness (transaction
succeeds) and temporal compliance (completes within the SLA threshold).

This service contract demonstrates latency testing (p50, p90, p95, p99 percentiles),
warmup, test intent (smoke vs verification), and threshold origin documentation.

## Running

```bash
# Compile
./gradlew compileJava compileTestJava

# Run tests (some failures are expected — see note below)
./gradlew test

# Generate the PUnit HTML report
./gradlew punitReport
```

Many tests are **expected to fail at the sample level**. PUnit determines
pass/fail from the aggregate pass rate, not from individual samples. A test
run with sample-level failures does not indicate a broken build. The key
compatibility indicator is successful compilation.

## Experiments

PUnit's experiment types map to stages in a testing workflow:

| Stage    | Gradle task              | What it does                                           |
|----------|--------------------------|--------------------------------------------------------|
| Explore  | `./gradlew flowExplore`  | Compares configurations (models, prompts) side by side |
| Optimize | `./gradlew flowOptimize` | Auto-tunes parameters (temperature, prompt text)       |
| Measure  | `./gradlew flowMeasure`  | Establishes an empirical baseline from 1000+ samples   |
| Test     | `./gradlew flowTest`     | Runs a probabilistic test against the baseline         |

To run the full flow end to end:

```bash
./gradlew operationalFlowTest
```

This executes explore → optimize → measure → verify → test in sequence and
validates the artifacts produced at each stage.

## Baselines

Measure experiments produce YAML **baseline files** — the empirical record of observed behaviour (pass rate, sample count, latency distribution) from which probabilistic tests derive their thresholds. Earlier docs called these files "specs" or "baseline specs"; the terms name the same artefact, and *baseline file* is the current one. In a real project baseline files are committed and consumed by probabilistic tests in CI.

In this project the generated baseline files under `src/test/resources/punit/baselines/` are gitignored because they regenerate frequently during development. Committed reference copies are in `src/test/resources/punit/specs-reference/` — see the [README](src/test/resources/punit/baselines/README.md) in the baselines directory for details.

## PUnit dependency

The project uses Gradle composite builds (`settings.gradle.kts`) to
automatically substitute the local `../punit` source when available. The
declared version in `build.gradle.kts` is only used when the local checkout
is absent. This means you can develop punit and punitexamples side by side
without publishing intermediate artifacts.

## Documentation

The **[PUnit User Guide](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md)** is the comprehensive reference for the framework. It covers the full experimentation-to-testing workflow, the service contract pattern, latency assertions, budget and pacing control, the statistical core, the Sentinel runtime, and the HTML report.

The **[Statistical Companion](https://github.com/mavai-org/punit/blob/main/docs/STATISTICAL-COMPANION.md)** covers the mathematical foundations for readers who want to understand the inference machinery.

## Requirements

- Java 21+
- JUnit Jupiter 5.13+
- PUnit 0.6.0+

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).

## Contributing

Contributions are welcome. All contributions are accepted under Apache 2.0 and
require a [Developer Certificate of Origin](dco.txt) sign-off (`git commit -s`).
See [CONTRIBUTING.md](CONTRIBUTING.md) for details. Please open an issue or pull
request on [GitHub](https://github.com/mavai-org/punitexamples).
