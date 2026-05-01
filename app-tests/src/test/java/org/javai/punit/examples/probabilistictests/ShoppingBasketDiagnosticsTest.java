package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * Demonstrates the diagnostic output the framework produces when a
 * probabilistic test runs. Three configurations exercise distinct
 * criterion paths.
 *
 * <p>Every {@code CriterionResult} carries a human-readable
 * explanation string and a structured {@code detail()} map. For
 * empirical {@link BernoulliPassRate} runs the explanation reads
 * for example
 * <pre>{@code observed=0.94 (Wilson-95% lower=0.93) vs threshold=0.85 (origin=EMPIRICAL) over 100 samples}</pre>
 * — the figures that drove the verdict. When no baseline matches
 * the run's covariate profile, the verdict's warnings list each
 * rejected candidate and the category mismatch that rejected it.
 */
public class ShoppingBasketDiagnosticsTest {

    private static final List<String> STANDARD_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    @ProbabilisticTest
    void empiricalAtModerateSampleCount() {
        // Empirical criterion: threshold derived from the resolved
        // baseline; verdict driven by the Wilson-score lower bound
        // on the observed rate clearing the baseline rate.
        PUnit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 100), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void empiricalAtHigherSampleCount() {
        // Larger sample count tightens the Wilson-score margin
        // around the observed rate. A run that's borderline at
        // n=100 can be definitively PASS or FAIL at n=200 — same
        // criterion explanation shape, tighter numbers.
        PUnit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 200), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void contractualAtExplicitThreshold() {
        // Contractual criterion: threshold is an external SLA, not
        // derived from a baseline. The verdict path is the simple
        // observed >= threshold (no Wilson wrap), and the
        // diagnostic message reports the observed rate, the SLA
        // threshold, and the sample count.
        PUnit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 100), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.meeting(0.85, ThresholdOrigin.SLA))
                .assertPasses();
    }
}
