# PUnit Examples — User Guide

This project contains worked examples of [PUnit](https://github.com/mavai-org/punit) experiments and probabilistic tests. It is not a replacement for [PUnit's own user guide](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md) — refer to that for full documentation of the framework's capabilities, in particular [Part 1: The Service Contract](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md#part-1-the-service-contract--the-shared-correctness-target), which documents the contract-first authoring style every example here uses (`ServiceContract` interface; `invoke(I, TokenTracker)` for the service call; `criteria()` — a `meeting()` chain with `satisfies(...)` clauses — for the acceptance criteria). This guide describes the example application, explains how to run the experiments and tests, and covers LLM configuration.

## The example application

The examples exercise a simulated shopping application comprising two services, each representing a different flavour of non-determinism.

### Shopping basket (LLM-powered)

A user issues natural language instructions like _"Add 2 apples"_ or _"Clear the basket"_. An LLM translates each instruction into a structured JSON action that a shopping basket API can execute:

```json
{
  "actions": [
    {
      "context": "SHOP",
      "name": "add",
      "parameters": [
        {"name": "item", "value": "apples"},
        {"name": "quantity", "value": "2"}
      ]
    }
  ]
}
```

Valid actions for the `SHOP` context are `add`, `remove`, and `clear`. A translation is considered successful when the LLM returns valid JSON that deserialises into valid actions for the given context.

Because the LLM is inherently non-deterministic — it may hallucinate field names, produce malformed JSON, or invent actions that don't exist — success rates are probabilistic. This makes the shopping basket a natural fit for PUnit's **empirical approach**, where acceptable thresholds are derived from measured baselines.

### Payment gateway (SLA-driven)

The payment gateway simulates an external service with a contractual SLA:

- **Availability:** 99.99% (Section 4.1 of _Payment Provider SLA v2.3_)
- **Latency:** Transactions complete within 1 second (Section 4.2)

The mock gateway intentionally underperforms its SLA slightly (~99.97% actual availability) so that PUnit's statistical machinery has something to detect. Unlike the shopping basket, the thresholds are known upfront from the contract — this is PUnit's **contractual approach**.

### Architecture

```
experiments / tests
      │
      ▼
  usecases          ← PUnit service contract adapters
      │
      ▼
    app             ← application code (no PUnit dependency)
```

The `app` package contains the application code: LLM clients, the payment gateway, and the shopping domain model. The `usecases` package wraps these in PUnit service contract adapters. The `experiments` and `probabilistictests` packages exercise the service contracts through PUnit.

## Prerequisites

- **Java 21** or later
- **Gradle** (the wrapper is included — use `./gradlew`)

## Running experiments

Experiments gather empirical data about how the system behaves. PUnit provides three experiment types, and this project includes examples of each.

### Explore — compare configurations

Before committing to a model or temperature, explore how different configurations perform:

```bash
./gradlew exp -Prun=ShoppingBasketExplore
```

Results are written to `build/punit/explorations/`.

### Measure — establish a baseline

Once you've chosen a configuration, run a measurement experiment to establish a statistical baseline:

```bash
./gradlew exp -Prun=ShoppingBasketMeasure
```

This runs 1000 samples (by default) and writes a baseline file (called a "spec" in earlier docs) to `src/test/resources/punit/baselines/`. Probabilistic tests derive their pass/fail thresholds from this baseline.

### Optimize — tune parameters

The optimization experiments iteratively refine parameters:

```bash
# Optimize LLM temperature for structured output reliability
./gradlew exp -Prun=ShoppingBasketOptimizeTemperature

# Optimize the system prompt through iterative refinement
./gradlew exp -Prun=ShoppingBasketOptimizePrompt
```

Results are written to `build/punit/optimizations/`.

### Running a specific experiment method

To run a single method within an experiment class:

```bash
./gradlew exp -Prun=ShoppingBasketExplore.compareModels
```

## Running tests

Probabilistic tests verify that the system's observed behaviour meets expectations. The shopping basket tests require a baseline (run `ShoppingBasketMeasure` first). The payment gateway tests use inline thresholds from the SLA.

```bash
# Run a specific test class
./gradlew test --tests "ShoppingBasketTest"

# Run all tests
./gradlew test
```

Individual sample failures are expected — that's the nature of probabilistic testing. PUnit aggregates the results and applies statistical analysis to determine the verdict.

## Generating reports

PUnit's Gradle plugin (`org.mavai.punit`, applied by this project) turns the raw experiment and test outputs into browser-friendly HTML reports. Each report is a single self-contained page — all CSS embedded, no JavaScript, no external requests — so it opens straight from disk with no server. Running a report over data that was never produced states that none was found rather than failing the build, so it is always safe to run.

### Test report

Aggregates every verdict from the most recent `./gradlew test` run:

```bash
./gradlew test punitReport
```

Output: `build/reports/punit/html/index.html`. Per test it shows the verdict (PASS / FAIL / INCONCLUSIVE) and the contract reference, the criteria evaluated, the per-postcondition failure histogram, and the latency profile.

### Exploration comparison

Compares the variants of an EXPLORE run — overall and per criterion — so you can see which configuration performed best without reading the raw YAML. Run an explore experiment first, then the report:

```bash
./gradlew exp -Prun=ShoppingBasketExplore
./gradlew explorationReport
```

Reads `build/punit/explorations/` and writes `build/reports/punit-explorations/html/index.html`.

### Optimization comparison

Summarises one OPTIMIZE run's iterations — listed in run order, with the chosen best highlighted and each row expanding to reveal the factor bundle that produced it. Run an optimize experiment first, then the report:

```bash
./gradlew exp -Prun=ShoppingBasketOptimizePrompt
./gradlew optimizationReport
```

Reads `build/punit/optimizations/` and writes `build/reports/punit-optimizations/html/index.html`.

For the full description of what each report shows — the per-criterion matrices, latency strips, the score trajectory, and the *too close to call* markers — see [Part 11: Reports](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md#part-11-reports) in the PUnit user guide.

## LLM configuration

### Mock mode (default)

By default, all LLM calls use a built-in mock that requires no API keys, no network access, and costs nothing. The mock simulates realistic LLM behaviour including:

- Temperature-sensitive reliability (lower temperature = more reliable structured output)
- Realistic failure modes (malformed JSON, hallucinated fields, invalid values)
- Approximate token counting

This means you can run every experiment and test in this project out of the box.

### Real mode

To call a real provider, set the mode and let punit-lm find the credential the way a services file would:

```bash
export PUNIT_LLM_MODE=real
export OPENAI_API_KEY=sk-...        # or ANTHROPIC_API_KEY=sk-ant-... for claude-* models
```

Or as system properties:

```bash
./gradlew test -Dpunit.llm.mode=real -Dmavai.llm.api-key=sk-...
```

Every language-model call goes through punit-lm's public API (`LanguageModels.configure`), so the examples carry no HTTP client, no per-provider key and no base-URL setting of their own. The provider follows the model name:

| Model pattern | Provider  |
|---------------|-----------|
| `claude-*`    | Anthropic |
| anything else | OpenAI    |

Credentials resolve in punit-lm's tier: `mavai.llm.api-key` / `MAVAI_LLM_API_KEY` first, then the vendor's conventional variable (`OPENAI_API_KEY`, `ANTHROPIC_API_KEY`). A missing credential is refused at configure time, before any request.

**Real mode will incur costs on your OpenAI and/or Anthropic accounts.** The measurement experiment, for example, runs 1000 samples by default. Be aware of your provider's rate limits and pricing before running large experiments.

### Configuration

| Setting  | System property  | Environment variable | Default |
|----------|------------------|----------------------|---------|
| LLM mode | `punit.llm.mode` | `PUNIT_LLM_MODE`     | `mock`  |
| API key  | `mavai.llm.api-key` | `MAVAI_LLM_API_KEY` (else `OPENAI_API_KEY` / `ANTHROPIC_API_KEY`) | — |

System properties take precedence over environment variables. Everything else about the model (the request deadline, capabilities, structured output) is punit-lm's, documented in the PUnit User Guide.

## Typical workflow

A typical workflow for the shopping basket service contract:

1. **Explore** — Compare models and temperatures to find the best configuration:
   ```bash
   ./gradlew exp -Prun=ShoppingBasketExplore
   ```

2. **Measure** — Establish a baseline with your chosen configuration:
   ```bash
   ./gradlew exp -Prun=ShoppingBasketMeasure
   ```

3. **Test** — Run probabilistic tests against the baseline:
   ```bash
   ./gradlew test --tests "ShoppingBasketTest"
   ```

For the payment gateway, no baseline is needed — the SLA threshold is specified directly in the test:

```bash
./gradlew test --tests "PaymentGatewaySlaTest"
```

## Further reading

- [PUnit User Guide](https://github.com/mavai-org/punit/blob/main/docs/USER-GUIDE.md) — full framework documentation
- [Verdict Catalog](VERDICT-CATALOG.md) — examples of every PUnit verdict archetype
