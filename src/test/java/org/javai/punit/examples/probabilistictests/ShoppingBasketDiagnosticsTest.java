package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.PROBABILISTIC_TEST_SAMPLE_SIZE;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.javai.punit.runtime.PUnit;
import org.junit.jupiter.api.Disabled;

/**
 * Demonstrates the diagnostic output the framework produces when a
 * probabilistic test runs. Three configurations exercise distinct
 * criterion paths.
 *
 * <p>Every {@code CriterionResult} carries a human-readable
 * explanation string and a structured {@code detail()} map. For
 * empirical {@link PassRate} runs the explanation reads
 * for example
 * <pre>{@code observed=0.94 (Wilson-95% lower=0.93) vs threshold=0.85 (origin=EMPIRICAL) over n samples}</pre>
 * — the figures that drove the verdict. When no baseline matches
 * the run's covariate profile, the verdict's warnings list each
 * rejected candidate and the category mismatch that rejected it.
 */
public class ShoppingBasketDiagnosticsTest {

    @ProbabilisticTest
    void empiricalAtModerateSampleCount() {
        // Empirical criterion: threshold derived from the resolved
        // baseline; verdict driven by the Wilson-score lower bound
        // on the observed rate clearing the baseline rate.
        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .assertPasses();
    }

    @ProbabilisticTest
    void empiricalAtHigherSampleCount() {
        // Larger sample count tightens the Wilson-score margin
        // around the observed rate. A run that's borderline at the
        // smaller n can be definitively PASS or FAIL at a larger n
        // — same criterion explanation shape, tighter numbers.
        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE * 2), LlmTuning.DEFAULT)
                .assertPasses();
    }

    @Disabled("awaiting DIR-CRITERIA-OVERRIDE-punit: test-side override of contract posture")
    @ProbabilisticTest
    void contractualAtExplicitThreshold() {
        // Contractual criterion: threshold is an external SLA, not
        // derived from a baseline. The verdict path is the simple
        // observed >= threshold (no Wilson wrap), and the
        // diagnostic message reports the observed rate, the SLA
        // threshold, and the sample count. This test overrides the
        // contract's empirical posture with an SLA-driven one — the
        // override mechanism is the subject of a follow-on directive.
        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .assertPasses();
    }
}
