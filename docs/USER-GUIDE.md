# PUnit Examples — User Guide

This project contains worked examples of [PUnit](https://github.com/javai-org/punit) experiments and probabilistic tests. It is not a replacement for PUnit's own user guide — refer to that for full documentation of the framework's capabilities. This guide describes the example application, explains how to run the experiments and tests, and covers LLM configuration.

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
  usecases          ← PUnit use case adapters
      │
      ▼
    app             ← application code (no PUnit dependency)
```

The `app` package contains the application code: LLM clients, the payment gateway, and the shopping domain model. The `usecases` package wraps these in PUnit use case adapters. The `experiments` and `probabilistictests` packages exercise the use cases through PUnit.

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

Results are written to `src/test/resources/punit/explorations/`.

### Measure — establish a baseline

Once you've chosen a configuration, run a measurement experiment to establish a statistical baseline:

```bash
./gradlew exp -Prun=ShoppingBasketMeasure
```

This runs 1000 samples (by default) and writes a spec file to `src/test/resources/punit/specs/`. Probabilistic tests derive their pass/fail thresholds from this baseline.

### Optimize — tune parameters

The optimization experiments iteratively refine parameters:

```bash
# Optimize LLM temperature for structured output reliability
./gradlew exp -Prun=ShoppingBasketOptimizeTemperature

# Optimize the system prompt through iterative refinement
./gradlew exp -Prun=ShoppingBasketOptimizePrompt
```

Results are written to `src/test/resources/punit/optimizations/`.

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

## LLM configuration

### Mock mode (default)

By default, all LLM calls use a built-in mock that requires no API keys, no network access, and costs nothing. The mock simulates realistic LLM behaviour including:

- Temperature-sensitive reliability (lower temperature = more reliable structured output)
- Realistic failure modes (malformed JSON, hallucinated fields, invalid values)
- Approximate token counting

This means you can run every experiment and test in this project out of the box.

### Real mode

To call real LLM providers, set the mode and provide API keys:

```bash
export PUNIT_LLM_MODE=real
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...
```

Alternatively, use system properties:

```bash
./gradlew test -Dpunit.llm.mode=real \
               -Dpunit.llm.openai.key=sk-... \
               -Dpunit.llm.anthropic.key=sk-ant-...
```

In real mode, the `RoutingChatLlm` routes each call to the appropriate provider based on the model name:

| Model pattern                                 | Provider  |
|-----------------------------------------------|-----------|
| `gpt-*`, `o1-*`, `o3-*`, `text-*`, `davinci*` | OpenAI    |
| `claude-*`                                    | Anthropic |

Providers are initialised lazily — if an experiment only uses OpenAI models, no Anthropic API key is required (and vice versa).

**Real mode will incur costs on your OpenAI and/or Anthropic accounts.** The measurement experiment, for example, runs 1000 samples by default. Be aware of your provider's rate limits and pricing before running large experiments.

### Additional configuration

| Setting              | System property               | Environment variable | Default                        |
|----------------------|-------------------------------|----------------------|--------------------------------|
| LLM mode             | `punit.llm.mode`              | `PUNIT_LLM_MODE`     | `mock`                         |
| OpenAI API key       | `punit.llm.openai.key`        | `OPENAI_API_KEY`     | —                              |
| OpenAI base URL      | `punit.llm.openai.baseUrl`    | `OPENAI_BASE_URL`    | `https://api.openai.com/v1`    |
| Anthropic API key    | `punit.llm.anthropic.key`     | `ANTHROPIC_API_KEY`  | —                              |
| Anthropic base URL   | `punit.llm.anthropic.baseUrl` | `ANTHROPIC_BASE_URL` | `https://api.anthropic.com/v1` |
| Request timeout (ms) | `punit.llm.timeout`           | `PUNIT_LLM_TIMEOUT`  | `30000`                        |

For all settings, system properties take precedence over environment variables.

## Typical workflow

A typical workflow for the shopping basket use case:

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

- [PUnit User Guide](https://github.com/javai-org/punit) — full framework documentation
- [Verdict Catalog](VERDICT-CATALOG.md) — examples of every PUnit verdict archetype
