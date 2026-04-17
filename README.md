# PUnit Examples

Example applications and probabilistic tests demonstrating the
[PUnit](https://github.com/javai-org/punit) framework. The project models
realistic use cases — an LLM-powered shopping assistant and a payment gateway
with SLA requirements — and shows how to apply statistical rigour at every
stage of the testing lifecycle.

For framework concepts, annotations, and configuration details see the
[PUnit User Guide](https://github.com/javai-org/punit/blob/main/docs/USER-GUIDE.md).

## Project structure

The project is organised into three modules that mirror how a real application
would separate concerns:

```
app/              Domain classes — shopping actions, LLM integrations, payment gateway.
                  No testing dependencies.

app-usecases/     Use case definitions and @Sentinel reliability specs.
                  Depends on punit-core but not JUnit.

app-tests/        Probabilistic tests, experiments, and integration tests.
                  Depends on punit-junit5.
```

This separation exists because sentinel specs (`app-usecases`) need to be
deployable without pulling in a test framework. The same reliability spec that
runs in CI can be consumed by an automation agent in production.

## Use cases

### Shopping basket

An LLM translates natural-language instructions ("add 2 apples") into
structured JSON shopping actions. The service contract validates that the
response is non-blank, parses as valid JSON, and contains only actions
appropriate to a shopping context.

This use case demonstrates covariates (model, temperature, time of day),
input sources (method-based and file-based), budget management (token tracking),
exception handling modes, and pacing constraints.

### Payment gateway

A mock gateway processes card payments with configurable latency and failure
rates. The service contract asserts both functional correctness (transaction
succeeds) and temporal compliance (completes within the SLA threshold).

This use case demonstrates latency testing (p50, p90, p95, p99 percentiles),
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

| Stage | Gradle task | What it does |
|-------|-------------|--------------|
| Explore | `./gradlew flowExplore` | Compares configurations (models, prompts) side by side |
| Optimize | `./gradlew flowOptimize` | Auto-tunes parameters (temperature, prompt text) |
| Measure | `./gradlew flowMeasure` | Establishes an empirical baseline from 1000+ samples |
| Test | `./gradlew flowTest` | Runs a probabilistic test against the baseline |

To run the full flow end to end:

```bash
./gradlew operationalFlowTest
```

This executes explore → optimize → measure → verify → test in sequence and
validates the artifacts produced at each stage.

## Specs and baselines

Measure experiments produce YAML spec files containing empirical baselines
(observed pass rate, confidence interval, latency distribution). In a real
project these specs are committed and consumed by probabilistic tests in CI.

In this project the generated specs under `app-tests/src/test/resources/punit/specs/`
are gitignored because they regenerate frequently during development. Committed
reference copies are in `specs-reference/` — see the
[README](app-tests/src/test/resources/punit/specs/README.md) in that directory
for details.

## PUnit dependency

The project uses Gradle composite builds (`settings.gradle.kts`) to
automatically substitute the local `../punit` source when available. The
declared version in `build.gradle.kts` is only used when the local checkout
is absent. This means you can develop punit and punitexamples side by side
without publishing intermediate artifacts.

## Documentation

The **[PUnit User Guide](https://github.com/javai-org/punit/blob/main/docs/USER-GUIDE.md)** is the comprehensive reference for the framework. It covers the full experimentation-to-testing workflow, the use case pattern, latency assertions, budget and pacing control, the statistical core, the Sentinel runtime, and the HTML report.

The **[Statistical Companion](https://github.com/javai-org/punit/blob/main/docs/STATISTICAL-COMPANION.md)** covers the mathematical foundations for readers who want to understand the inference machinery.

## Requirements

- Java 21+
- JUnit Jupiter 5.13+
- PUnit 0.6.0+

## License

[Attribution Required License (ARL-1.0)](LICENSE)

## Contributing

Contributions are welcome. Please open an issue or pull request on [GitHub](https://github.com/javai-org/punitexamples).
