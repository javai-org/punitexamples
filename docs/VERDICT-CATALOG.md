# Verdict Catalog

A curated collection of archetypal PUnit verdicts, organized from simplest to most complex.

PUnit verdicts are available at two detail levels:

- **Summary** (default) — compact verdict with pass rate comparison, termination reason, and caveats
- **Verbose** (`-Dpunit.stats.detail=VERBOSE`) — full statistical analysis including hypothesis test formulation, confidence intervals, and inference workings

The numerical values shown below come from actual test runs and will vary between executions due to the probabilistic nature of the tests.

---

## 1. Pass

The simplest passing verdict. PUnit detects that the required number of successes has been reached and terminates early, skipping the remaining samples.

### Summary

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  servicePassesComfortably
  
  Mode:                EXPLICIT THRESHOLD
  Intent:              VERIFICATION
  Use Case:            ShoppingBasketUseCase
  Threshold:           0.5000
  Samples:             50

```

```
═ VERDICT: PASS (VERIFICATION) ═══════════════════════════════════════ PUnit ═

  servicePassesComfortably(ShoppingBasketUseCase, String)
  
  Observed pass rate:  0.5682 (25/44) >= required: 0.5000
  Termination:         Required pass rate already achieved
  Details:             After 44 samples with 25 successes (0.5682), required min pass rate (25 successes) already met. Skipping 6 remaining samples.
  Elapsed:             2322ms

```

### Verbose

```
═ STATISTICAL ANALYSIS FOR: servicePassesComfortablyTransparent(ShoppingB... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.5000 (success rate meets threshold)
    H₁ (alternative):      True success rate π < 0.5000 (success rate below threshold)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       32
    Successes (k):         25
    Observed rate (p̂):    0.7813
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.5000 (Threshold specified directly in @ProbabilisticTest annotation)
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.78 × 0.22 / 32) = 0.0731
    Confidence interval:   95% [0.612, 0.890]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.78 - 0.50) / √(0.50 × 0.50 / 32)
                           z = 3.18
  
    p-value:               P(Z > 3.18) = 0.001
  
  VERDICT
    Result:                PASS
    Interpretation:        The observed success rate of 0.7813 meets the
                           required threshold of 0.5000. The test passes.
  
    Caveat:                With n=32 samples, subtle performance changes may
                           not be detectable. For higher sensitivity, consider
                           increasing sample size.
    Caveat:                Using inline threshold (no baseline spec). For
                           statistically-derived thresholds with confidence
                           intervals, run a MEASURE experiment first.

```

---

## 2. Fail with early termination

When PUnit determines that the threshold can no longer be reached — even if every remaining sample succeeds — it terminates early and reports the impossibility analysis.

### Summary

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  failsEarlyWhenThresholdUnreachable
  
  Mode:                EXPLICIT THRESHOLD
  Intent:              SMOKE
  Use Case:            ShoppingBasketUseCase
  Threshold:           0.9500
  Samples:             30

```

```
═ VERDICT: FAIL (SMOKE) ══════════════════════════════════════════════ PUnit ═

  failsEarlyWhenThresholdUnreachable(ShoppingBasketUseCase, String)
  
  Observed pass rate:  0.0000 (0/2) < required: 0.9500
  Termination:         Cannot reach required pass rate
  Details:             After 2 samples with 0 successes, maximum possible successes (0 + 28 = 28) is less than required (29)
  Analysis:            Needed 29 successes, maximum possible is 28
  Elapsed:             164ms

```

### Verbose

```
═ STATISTICAL ANALYSIS FOR: failsEarlyWhenThresholdUnreachableTransparent... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.9500 (observed rate meets threshold)
    H₁ (alternative):      True success rate π < 0.9500 (observed rate below threshold)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       2
    Successes (k):         0
    Observed rate (p̂):    0.0000
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.9500 (Threshold specified directly in @ProbabilisticTest annotation)
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.00 × 1.00 / 2) = 0.0000
    Confidence interval:   95% [0.000, 0.658]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.00 - 0.95) / √(0.95 × 0.05 / 2)
                           z = -6.16
  
    p-value:               P(Z > -6.16) = 1.000
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.0000 falls below the
                           required threshold of 0.9500. The observed rate does
                           not meet the threshold.
  
    Caveat:                Small sample size (n=2). Statistical conclusions
                           should be interpreted with caution. Consider
                           increasing sample size for more reliable results.
    Caveat:                Zero success rate observed. This indicates a
                           fundamental failure that may warrant investigation
                           before further testing.
    Caveat:                Using inline threshold (no baseline spec). For
                           statistically-derived thresholds with confidence
                           intervals, run a MEASURE experiment first.

```

---

## 3. Fail

A failing verdict. The verbose variant adds the HYPOTHESIS TEST and STATISTICAL INFERENCE workings.

### Summary

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  serviceFailsNarrowlyTransparent
  
  Mode:                EXPLICIT THRESHOLD
  Intent:              SMOKE
  Use Case:            ShoppingBasketUseCase
  Threshold:           0.9500
  Samples:             50

```

```
═ STATISTICAL ANALYSIS FOR: serviceFailsNarrowlyTransparent(ShoppingBaske... ═

  OBSERVED DATA
    Sample size (n):       6
    Successes (k):         3
    Observed rate (p̂):    0.5000
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.9500 (Threshold specified directly in @ProbabilisticTest annotation)
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.5000 falls below the
                           required threshold of 0.9500. The observed rate does
                           not meet the threshold.
  
    Caveat:                Small sample size (n=6). Statistical conclusions
                           should be interpreted with caution. Consider
                           increasing sample size for more reliable results.
    Caveat:                Using inline threshold (no baseline spec). For
                           statistically-derived thresholds with confidence
                           intervals, run a MEASURE experiment first.

```

### Verbose

```
═ STATISTICAL ANALYSIS FOR: serviceFailsNarrowlyTransparent(ShoppingBaske... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.9500 (observed rate meets threshold)
    H₁ (alternative):      True success rate π < 0.9500 (observed rate below threshold)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       4
    Successes (k):         1
    Observed rate (p̂):    0.2500
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.9500 (Threshold specified directly in @ProbabilisticTest annotation)
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.25 × 0.75 / 4) = 0.2165
    Confidence interval:   95% [0.046, 0.699]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.25 - 0.95) / √(0.95 × 0.05 / 4)
                           z = -6.42
  
    p-value:               P(Z > -6.42) = 1.000
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.2500 falls below the
                           required threshold of 0.9500. The observed rate does
                           not meet the threshold.
  
    Caveat:                Small sample size (n=4). Statistical conclusions
                           should be interpreted with caution. Consider
                           increasing sample size for more reliable results.
    Caveat:                Using inline threshold (no baseline spec). For
                           statistically-derived thresholds with confidence
                           intervals, run a MEASURE experiment first.

```

---

## 4. Budget exhaustion

When a cost budget (time or tokens) runs out before all samples complete, PUnit reports how many samples were executed and the pass rate at termination. The default budget-exhaustion behaviour is FAIL — the test fails regardless of the partial results.

### Summary

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  failsWhenBudgetRunsOut
  
  Mode:                EXPLICIT THRESHOLD
  Intent:              VERIFICATION
  Use Case:            ShoppingBasketUseCase
  Threshold:           0.5000
  Samples:             50

```

```
═ VERDICT: FAIL (VERIFICATION) ═══════════════════════════════════════ PUnit ═

  failsWhenBudgetRunsOut(ShoppingBasketUseCase, String)
  
  Samples executed:    5 of 50 (budget exhausted)
  Pass rate:           0.8000 (4/5), required: 0.5000
  Termination:         Method token budget exhausted
  Details:             Method token budget exhausted: 1000 tokens >= 1000 budget
  Elapsed:             328ms

```

An alternative budget behaviour, `EVALUATE_PARTIAL`, evaluates the partial results against the threshold as if the test had completed normally. This can produce either a pass or fail depending on the partial results.

```
═ VERDICT: PASS (VERIFICATION) ═══════════════════════════════════════ PUnit ═

  evaluatesPartialResultsOnBudgetPass(ShoppingBasketUseCase, String)
  
  Observed pass rate:  1.0000 (5/5) >= required: 0.5000
  Termination:         Method token budget exhausted
  Details:             Method token budget exhausted: 1000 tokens >= 1000 budget
  Elapsed:             352ms

```

### Verbose

```
═ STATISTICAL ANALYSIS FOR: failsWhenBudgetRunsOutTransparent(ShoppingBas... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.5000 (success rate meets threshold)
    H₁ (alternative):      True success rate π < 0.5000 (success rate below threshold)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       5
    Successes (k):         3
    Observed rate (p̂):    0.6000
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.5000 (Threshold specified directly in @ProbabilisticTest annotation)
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.60 × 0.40 / 5) = 0.2191
    Confidence interval:   95% [0.231, 0.882]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.60 - 0.50) / √(0.50 × 0.50 / 5)
                           z = 0.45
  
    p-value:               P(Z > 0.45) = 0.327
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.6000 falls below the
                           required threshold of 0.5000. This suggests the
                           system is not meeting its expected performance
                           level.
  
    Caveat:                Small sample size (n=5). Statistical conclusions
                           should be interpreted with caution. Consider
                           increasing sample size for more reliable results.
    Caveat:                Using inline threshold (no baseline spec). For
                           statistically-derived thresholds with confidence
                           intervals, run a MEASURE experiment first.

```

---

## 5. SLA compliance with contract provenance

When a threshold originates from an SLA, SLO, or policy, PUnit frames the hypothesis test accordingly and includes a THRESHOLD PROVENANCE section tracing the threshold back to its source. The hypothesis text adapts to the threshold origin (e.g. "system meets SLA requirement" vs "system meets SLO target" vs "system meets policy requirement").

### Summary

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  slaPassShowsComplianceHypothesis
  
  Mode:                SLA-DRIVEN
  Intent:              VERIFICATION
  Use Case:            ShoppingBasketUseCase
  Threshold:           0.5000 (SLA)
  Contract:            Acme Payment SLA v3.2 §4.1
  Samples:             50

```

```
═ STATISTICAL ANALYSIS FOR: slaPassShowsComplianceHypothesis(ShoppingBask... ═

  OBSERVED DATA
    Sample size (n):       42
    Successes (k):         25
    Observed rate (p̂):    0.5952
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.5000 (Threshold specified directly in @ProbabilisticTest annotation)
  
  VERDICT
    Result:                PASS
    Interpretation:        The observed success rate of 0.5952 meets the
                           required threshold of 0.5000. The system meets its
                           SLA requirement.
  
    Caveat:                With n=42 samples, subtle performance changes may
                           not be detectable. For higher sensitivity, consider
                           increasing sample size.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA
    Contract:              Acme Payment SLA v3.2 §4.1

```

### Verbose

```
═ STATISTICAL ANALYSIS FOR: slaPassShowsComplianceHypothesis(ShoppingBask... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.5000 (system meets SLA requirement)
    H₁ (alternative):      True success rate π < 0.5000 (system violates SLA)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       31
    Successes (k):         25
    Observed rate (p̂):    0.8065
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.5000 (Threshold specified directly in @ProbabilisticTest annotation)
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.81 × 0.19 / 31) = 0.0710
    Confidence interval:   95% [0.637, 0.908]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.81 - 0.50) / √(0.50 × 0.50 / 31)
                           z = 3.41
  
    p-value:               P(Z > 3.41) = 0.000
  
  VERDICT
    Result:                PASS
    Interpretation:        The observed success rate of 0.8065 meets the
                           required threshold of 0.5000. The system meets its
                           SLA requirement.
  
    Caveat:                With n=31 samples, subtle performance changes may
                           not be detectable. For higher sensitivity, consider
                           increasing sample size.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA
    Contract:              Acme Payment SLA v3.2 §4.1

```

---

## 6. Compliance undersized

When the sample size is too small to provide meaningful statistical evidence of compliance with a high-reliability SLA target, PUnit warns that a passing result is only a smoke-test-level observation. A failing result remains a reliable indication of non-conformance.

### Summary

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  complianceUndersizedSmokeTestOnly
  
  Mode:                SLA-DRIVEN
  Intent:              SMOKE
  Use Case:            ShoppingBasketUseCase
  Threshold:           0.9999 (SLA)
  Contract:            Acme Payment SLA v3.2 §4.1
  Samples:             50

```

```
═ STATISTICAL ANALYSIS FOR: complianceUndersizedSmokeTestOnly(ShoppingBas... ═

  OBSERVED DATA
    Sample size (n):       2
    Successes (k):         1
    Observed rate (p̂):    0.5000
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.9999 (Threshold specified directly in @ProbabilisticTest annotation)
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.5000 falls below the
                           required threshold of 0.9999. The observed rate is
                           inconsistent with the target.
  
    Caveat:                Small sample size (n=2). Statistical conclusions
                           should be interpreted with caution. Consider
                           increasing sample size for more reliable results.
    Caveat:                Warning: sample not sized for compliance
                           verification. With n=2 and target of 0.9999, even
                           zero failures would not provide sufficient
                           statistical evidence of compliance (α=0.050). A PASS
                           at this sample size is a smoke-test-level
                           observation, not a compliance determination. Note: a
                           FAIL verdict remains a reliable indication of
                           non-conformance.
    Caveat:                Sample not sized for verification (N=2, need 27053).
                           This is a smoke-test-level observation, not a
                           compliance determination.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA
    Contract:              Acme Payment SLA v3.2 §4.1

```

### Verbose

```
═ STATISTICAL ANALYSIS FOR: complianceUndersizedSmokeTestOnly(ShoppingBas... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.9999 (observed rate consistent with target)
    H₁ (alternative):      True success rate π < 0.9999 (observed rate inconsistent with target)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       6
    Successes (k):         5
    Observed rate (p̂):    0.8333
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.9999 (Threshold specified directly in @ProbabilisticTest annotation)
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.83 × 0.17 / 6) = 0.1521
    Confidence interval:   95% [0.436, 0.970]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.83 - 1.00) / √(1.00 × 0.00 / 6)
                           z = -40.80
  
    p-value:               P(Z > -40.80) = 1.000
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.8333 falls below the
                           required threshold of 0.9999. The observed rate is
                           inconsistent with the target.
  
    Caveat:                Small sample size (n=6). Statistical conclusions
                           should be interpreted with caution. Consider
                           increasing sample size for more reliable results.
    Caveat:                Warning: sample not sized for compliance
                           verification. With n=6 and target of 0.9999, even
                           zero failures would not provide sufficient
                           statistical evidence of compliance (α=0.050). A PASS
                           at this sample size is a smoke-test-level
                           observation, not a compliance determination. Note: a
                           FAIL verdict remains a reliable indication of
                           non-conformance.
    Caveat:                Sample not sized for verification (N=6, need 27053).
                           This is a smoke-test-level observation, not a
                           compliance determination.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA
    Contract:              Acme Payment SLA v3.2 §4.1

```

---

## 7. Covariate misalignment

When the test runs under conditions that differ from the baseline (e.g. different time of day, weekday vs weekend), PUnit emits a BASELINE FOUND banner listing the misaligned covariates before the test runs. This also appears as a caveat in the verdict.

### Summary

```
═ BASELINE FOUND FOR USE CASE: ShoppingBasketUseCase ═════════════════ PUnit ═

  Baseline file:       ShoppingBasketUseCase-8e72-980b-d43e-5bad-a769.yaml
  
  Please note, the following covariates do not match the baseline:
    - day_of_week: baseline=WEEKEND, test=WEEKDAY
    - llm_model: baseline=gpt-4o-mini, test=mock-llm
  
  Statistical comparison may be less reliable.
  Consider running a new MEASURE experiment under current conditions.

```

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  temporalMismatchShowsCaveatTransparent
  
  Mode:                SPEC-DRIVEN
  Intent:              VERIFICATION
  Spec:                ShoppingBasketUseCase
  Threshold:           0.4063 (derived from baseline)
  Samples:             50

```

```
═ STATISTICAL ANALYSIS FOR: temporalMismatchShowsCaveatTransparent(Shoppi... ═

  OBSERVED DATA
    Sample size (n):       46
    Successes (k):         16
    Observed rate (p̂):    0.3478
  
  BASELINE REFERENCE
    Source:                ShoppingBasketUseCase-8e72-980b-d43e-5bad-a769.yaml (generated 2026-03-01)
    Empirical basis:       1000 samples, 437 successes (0.4370)
    Threshold derivation:  Lower bound of 95% CI = 0.4114, min pass rate = 0.4063
  
  LATENCY ANALYSIS
    Population:            Successful samples only (n=16 of 46)
    Observed distribution:
    p50:                   53ms
    p90:                   96ms
    p95:                   98ms
    p99:                   98ms
    max:                   98ms
  
    Percentile thresholds (from baseline):
    p50:                   53ms <= 55ms                                     PASS
    p90:                   96ms > 95ms                                      FAIL
    p95:                   98ms <= 99ms                        PASS (indicative)
    p99:                   98ms <= 104ms                       PASS (indicative)
  
    Caveat:                Sample size is small for some percentiles — results
                           are indicative, not evidential.
  
    Baseline reference:    ShoppingBasketUseCase-8e72-980b-d43e-5bad-a769.yaml
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.3478 falls below the
                           required threshold of 0.4063. This suggests the
                           system is not meeting its expected performance
                           level.
  
    Caveat:                Covariate misalignment detected: the test conditions
                           differ from the baseline. Misaligned covariates:
                           day_of_week (baseline=WEEKEND, test=WEEKDAY),
                           llm_model (baseline=gpt-4o-mini, test=mock-llm).
                           Statistical comparison may be less reliable.
    Caveat:                With n=46 samples, subtle performance changes may
                           not be detectable. For higher sensitivity, consider
                           increasing sample size.

```

### Verbose

```
═ STATISTICAL ANALYSIS FOR: temporalMismatchShowsCaveatTransparent(Shoppi... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.4063 (success rate meets threshold)
    H₁ (alternative):      True success rate π < 0.4063 (success rate below threshold)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       49
    Successes (k):         19
    Observed rate (p̂):    0.3878
  
  BASELINE REFERENCE
    Source:                ShoppingBasketUseCase-8e72-980b-d43e-5bad-a769.yaml (generated 2026-03-01)
    Empirical basis:       1000 samples, 437 successes (0.4370)
    Threshold derivation:  Lower bound of 95% CI = 0.4114, min pass rate = 0.4063
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.39 × 0.61 / 49) = 0.0696
    Confidence interval:   95% [0.264, 0.528]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.39 - 0.41) / √(0.41 × 0.59 / 49)
                           z = -0.26
  
    p-value:               P(Z > -0.26) = 0.604
  
  LATENCY ANALYSIS
    Population:            Successful samples only (n=19 of 49)
    Observed distribution:
    p50:                   66ms
    p90:                   101ms
    p95:                   102ms
    p99:                   102ms
    max:                   102ms
  
    Percentile thresholds (from baseline):
    p50:                   66ms > 55ms                                      FAIL
    p90:                   101ms > 95ms                                     FAIL
    p95:                   102ms > 99ms                                     FAIL
    p99:                   102ms <= 104ms                      PASS (indicative)
  
    Caveat:                Sample size is small for some percentiles — results
                           are indicative, not evidential.
  
    Baseline reference:    ShoppingBasketUseCase-8e72-980b-d43e-5bad-a769.yaml
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.3878 falls below the
                           required threshold of 0.4063. This suggests the
                           system is not meeting its expected performance
                           level.
  
    Caveat:                Covariate misalignment detected: the test conditions
                           differ from the baseline. Misaligned covariates:
                           day_of_week (baseline=WEEKEND, test=WEEKDAY),
                           llm_model (baseline=gpt-4o-mini, test=mock-llm).
                           Statistical comparison may be less reliable.
    Caveat:                With n=49 samples, subtle performance changes may
                           not be detectable. For higher sensitivity, consider
                           increasing sample size.

```

---

## 8. Intent-governed verdicts (VERIFICATION vs SMOKE)

PUnit supports two test intents: **VERIFICATION** (evidential — enforces a statistical feasibility gate before any samples execute) and **SMOKE** (sentinel — permits undersized configurations and uses softened language). The intent appears in the verdict banner and influences hypothesis framing and caveats.

A VERIFICATION test with insufficient samples will fail at configuration time (not shown here — see the user guide for the infeasibility message). The examples below show how intent affects the verdict for tests that *do* execute.

### Summary

### Verification (sized, normative)

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  verificationPassSized
  
  Mode:                SLA-DRIVEN
  Intent:              VERIFICATION
  Use Case:            ShoppingBasketUseCase
  Threshold:           0.5000 (SLA)
  Contract:            Acme Payment SLA v3.2 §4.1
  Samples:             50

```

```
═ STATISTICAL ANALYSIS FOR: verificationPassSized(ShoppingBasketUseCase, ... ═

  OBSERVED DATA
    Sample size (n):       36
    Successes (k):         25
    Observed rate (p̂):    0.6944
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.5000 (Threshold specified directly in @ProbabilisticTest annotation)
  
  VERDICT
    Result:                PASS
    Interpretation:        The observed success rate of 0.6944 meets the
                           required threshold of 0.5000. The system meets its
                           SLA requirement.
  
    Caveat:                With n=36 samples, subtle performance changes may
                           not be detectable. For higher sensitivity, consider
                           increasing sample size.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA
    Contract:              Acme Payment SLA v3.2 §4.1

```

When a test is configured with `intent = SMOKE` and a normative threshold origin (SLA, SLO, or POLICY), PUnit uses softened language — "consistent with the target" rather than "meets SLA requirement". If the sample is undersized for verification, a sizing note appears.

### Smoke undersized (normative)

```
═ TEST CONFIGURATION ═════════════════════════════════════════════════ PUnit ═

  smokeUndersizedNormative
  
  Mode:                SLA-DRIVEN
  Intent:              SMOKE
  Use Case:            ShoppingBasketUseCase
  Threshold:           0.9500 (SLA)
  Samples:             30

```

```
═ STATISTICAL ANALYSIS FOR: smokeUndersizedNormative(ShoppingBasketUseCas... ═

  OBSERVED DATA
    Sample size (n):       4
    Successes (k):         2
    Observed rate (p̂):    0.5000
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.9500 (Threshold specified directly in @ProbabilisticTest annotation)
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.5000 falls below the
                           required threshold of 0.9500. The observed rate is
                           inconsistent with the target.
  
    Caveat:                Small sample size (n=4). Statistical conclusions
                           should be interpreted with caution. Consider
                           increasing sample size for more reliable results.
    Caveat:                Warning: sample not sized for compliance
                           verification. With n=4 and target of 0.9500, even
                           zero failures would not provide sufficient
                           statistical evidence of compliance (α=0.050). A PASS
                           at this sample size is a smoke-test-level
                           observation, not a compliance determination. Note: a
                           FAIL verdict remains a reliable indication of
                           non-conformance.
    Caveat:                Sample not sized for verification (N=4, need 52).
                           This is a smoke-test-level observation, not a
                           compliance determination.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA

```

If the SMOKE sample is already large enough for verification, PUnit suggests upgrading to `intent = VERIFICATION` for evidential strength.

### Smoke sized with hint (normative)

```
═ STATISTICAL ANALYSIS FOR: smokeSizedNormativeHint(ShoppingBasketUseCase... ═

  OBSERVED DATA
    Sample size (n):       35
    Successes (k):         25
    Observed rate (p̂):    0.7143
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.5000 (Threshold specified directly in @ProbabilisticTest annotation)
  
  VERDICT
    Result:                PASS
    Interpretation:        The observed success rate of 0.7143 meets the
                           required threshold of 0.5000. The observed rate is
                           consistent with the target.
  
    Caveat:                With n=35 samples, subtle performance changes may
                           not be detectable. For higher sensitivity, consider
                           increasing sample size.
    Caveat:                Sample is sized for verification. Consider setting
                           intent = VERIFICATION for stronger statistical
                           guarantees.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA

```

### Verbose

```
═ STATISTICAL ANALYSIS FOR: verificationPassSized(ShoppingBasketUseCase, ... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.5000 (system meets SLA requirement)
    H₁ (alternative):      True success rate π < 0.5000 (system violates SLA)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       36
    Successes (k):         25
    Observed rate (p̂):    0.6944
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.5000 (Threshold specified directly in @ProbabilisticTest annotation)
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.69 × 0.31 / 36) = 0.0768
    Confidence interval:   95% [0.531, 0.820]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.69 - 0.50) / √(0.50 × 0.50 / 36)
                           z = 2.33
  
    p-value:               P(Z > 2.33) = 0.010
  
  VERDICT
    Result:                PASS
    Interpretation:        The observed success rate of 0.6944 meets the
                           required threshold of 0.5000. The system meets its
                           SLA requirement.
  
    Caveat:                With n=36 samples, subtle performance changes may
                           not be detectable. For higher sensitivity, consider
                           increasing sample size.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA
    Contract:              Acme Payment SLA v3.2 §4.1

```

```
═ STATISTICAL ANALYSIS FOR: smokeUndersizedNormative(ShoppingBasketUseCas... ═

  HYPOTHESIS TEST
    H₀ (null):             True success rate π ≥ 0.9500 (observed rate consistent with target)
    H₁ (alternative):      True success rate π < 0.9500 (observed rate inconsistent with target)
    Test type:             One-sided binomial proportion test
  
  OBSERVED DATA
    Sample size (n):       3
    Successes (k):         1
    Observed rate (p̂):    0.3333
  
  BASELINE REFERENCE
    Source:                (inline threshold)
    Threshold:             0.9500 (Threshold specified directly in @ProbabilisticTest annotation)
  
  STATISTICAL INFERENCE
    Standard error:        SE = √(p̂(1-p̂)/n) = √(0.33 × 0.67 / 3) = 0.2722
    Confidence interval:   95% [0.061, 0.792]
  
    Test statistic:        z = (p̂ - π₀) / √(π₀(1-π₀)/n)
                           z = (0.33 - 0.95) / √(0.95 × 0.05 / 3)
                           z = -4.90
  
    p-value:               P(Z > -4.90) = 1.000
  
  VERDICT
    Result:                FAIL
    Interpretation:        The observed success rate of 0.3333 falls below the
                           required threshold of 0.9500. The observed rate is
                           inconsistent with the target.
  
    Caveat:                Small sample size (n=3). Statistical conclusions
                           should be interpreted with caution. Consider
                           increasing sample size for more reliable results.
    Caveat:                Warning: sample not sized for compliance
                           verification. With n=3 and target of 0.9500, even
                           zero failures would not provide sufficient
                           statistical evidence of compliance (α=0.050). A PASS
                           at this sample size is a smoke-test-level
                           observation, not a compliance determination. Note: a
                           FAIL verdict remains a reliable indication of
                           non-conformance.
    Caveat:                Sample not sized for verification (N=3, need 52).
                           This is a smoke-test-level observation, not a
                           compliance determination.
  
  THRESHOLD PROVENANCE
    Threshold origin:      SLA

```

