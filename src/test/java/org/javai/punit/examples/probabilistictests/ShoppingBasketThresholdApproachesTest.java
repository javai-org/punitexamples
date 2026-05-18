package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.MEASURE_EXPERIMENT_SAMPLE_SIZE;
import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.PROBABILISTIC_TEST_SAMPLE_SIZE;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.spec.Experiment;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.javai.punit.runtime.PUnit;
import org.javai.punit.internal.engine.baseline.PowerAnalysis;
import org.junit.jupiter.api.Disabled;

/**
 * Demonstrates the three operational approaches for choosing a
 * threshold + sample size in probabilistic testing.
 *
 * <h2>Sample-size-first</h2>
 *
 * <p>"I have budget for 100 samples. Give me the threshold the
 * baseline supports at 95% confidence."
 *
 * <p>Use when compute or token budget is the binding constraint
 * and you want the most rigorous threshold within that budget. The
 * framework's {@code PassRate.empirical()} criterion
 * does this natively — the threshold is the resolved baseline's
 * observed pass rate, and the verdict comes from the Wilson-score
 * lower bound on the test's observed rate at the configured
 * confidence (default 0.95).
 *
 * <h2>Confidence-first</h2>
 *
 * <p>"I need to detect a 5% degradation with 95% confidence and
 * 80% power. Tell me how many samples that requires."
 *
 * <p>Use when statistical power is the binding constraint —
 * typically SLA monitoring where you must reliably detect
 * regressions of a specific size. The framework's
 * {@link PowerAnalysis#sampleSize(java.util.function.Supplier, double, double)
 * PowerAnalysis.sampleSize} computes the required sample count
 * from the baseline rate plus the (MDE, power) pair; the test
 * then runs at that count.
 *
 * <h2>Threshold-first</h2>
 *
 * <p>"I know the pass rate must be at least 90%. Run 100 samples
 * to verify."
 *
 * <p>Use when the threshold is dictated externally — an SLA, a
 * regulatory requirement, a policy commitment — and the test's job
 * is to verify conformance. The framework's
 * {@code PassRate.meeting(threshold, origin)} factory is
 * the contractual path: a deterministic
 * {@code observed >= threshold} comparison, with the threshold's
 * provenance ({@link ThresholdOrigin#SLA SLA},
 * {@link ThresholdOrigin#SLO SLO}, {@link ThresholdOrigin#POLICY POLICY})
 * recorded for audit.
 */
public class ShoppingBasketThresholdApproachesTest {

    /**
     * The baseline measure-experiment the empirical and
     * confidence-first tests below resolve through. A single
     * baseline definition shared by both tests guarantees they
     * select the same baseline file at test time.
     */
    private Experiment baseline() {
        return PUnit.measuring(ShoppingBasketServiceContract.sampling(MEASURE_EXPERIMENT_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .experimentId("baseline-v1")
                .build();
    }

    @ProbabilisticTest
    void sampleSizeFirst() {
        // Fixed sample budget. Confidence stays at the empirical
        // criterion's default (0.95). The threshold the verdict
        // tests against is the resolved baseline's observed rate.
        PUnit.testing(this::baseline)
                .samples(PROBABILISTIC_TEST_SAMPLE_SIZE)
                .assertPasses();
    }

    @ProbabilisticTest
    void confidenceFirst() {
        // PowerAnalysis computes the minimum sample count required
        // to:
        //   - detect a degradation of at least 5 percentage points
        //     against the baseline pass rate (the minimum
        //     detectable effect, MDE = 0.05), and
        //   - detect it with probability 0.80 when it really is
        //     present (statistical power = 0.80, the conventional
        //     "I am willing to miss a true 5-point regression at
        //     most 1 run in 5"),
        // at 95% confidence (criterion default — controls the
        // false-positive rate, i.e. how often a healthy run is
        // mis-flagged as regressed).
        //
        // MDE 0.05 + power 0.80 are workable defaults for SLA
        // monitoring; tighten either to require larger samples.
        int n = PowerAnalysis.sampleSize(this::baseline, 0.05, 0.80);

        PUnit.testing(this::baseline)
                .samples(n)
                .assertPasses();
    }

    @Disabled("awaiting DIR-CRITERIA-OVERRIDE-punit: test-side override of contract posture")
    @ProbabilisticTest
    void thresholdFirst() {
        // Externally-dictated threshold (SLA). No baseline involved
        // — the verdict is the deterministic observed >= threshold
        // comparison; the threshold's provenance is stamped onto
        // the result for audit.
        //
        // This test overrides the contract's empirical posture with
        // an SLA-driven one; the override mechanism is the subject
        // of a follow-on directive.
        int samples = 25;
        PUnit.testing(ShoppingBasketServiceContract.sampling(samples), LlmTuning.DEFAULT)
                .assertPasses();
    }
}
