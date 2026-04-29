package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.api.typed.spec.Experiment;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.Punit;
import org.javai.punit.power.PowerAnalysis;

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
 * typed pipeline's {@code BernoulliPassRate.empirical()} criterion
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
 * regressions of a specific size. The typed pipeline's
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
 * is to verify conformance. The typed pipeline's
 * {@code BernoulliPassRate.meeting(threshold, origin)} factory is
 * the contractual path: a deterministic
 * {@code observed >= threshold} comparison, with the threshold's
 * provenance ({@link ThresholdOrigin#SLA SLA},
 * {@link ThresholdOrigin#SLO SLO}, {@link ThresholdOrigin#POLICY POLICY})
 * recorded for audit.
 */
public class ShoppingBasketThresholdApproachesTest {

    private static final List<String> STANDARD_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    /**
     * The baseline measure-experiment the empirical and
     * confidence-first tests below resolve through. A single
     * baseline definition shared by both tests guarantees they
     * select the same baseline file at test time.
     */
    private Experiment baseline() {
        return Punit.measuring(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 1000), LlmTuning.DEFAULT)
                .experimentId("baseline-v1")
                .build();
    }

    @ProbabilisticTest
    void sampleSizeFirst() {
        // Fixed sample budget of 100. Confidence stays at the
        // empirical criterion's default (0.95). The threshold the
        // verdict tests against is the resolved baseline's
        // observed rate.
        Punit.testing(this::baseline)
                .samples(100)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void confidenceFirst() {
        // PowerAnalysis computes the minimum sample count required
        // to detect a 5%-point degradation against the baseline
        // rate at 95% confidence and 80% power. The test then runs
        // at that count.
        int n = PowerAnalysis.sampleSize(this::baseline, 0.05, 0.80);

        Punit.testing(this::baseline)
                .samples(n)
                .criterion(BernoulliPassRate.empirical().atConfidence(0.95))
                .assertPasses();
    }

    @ProbabilisticTest
    void thresholdFirst() {
        // Externally-dictated threshold (SLA). No baseline involved
        // — the verdict is the deterministic observed >= threshold
        // comparison; the threshold's provenance is stamped onto
        // the result for audit.
        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 100), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.meeting(0.90, ThresholdOrigin.SLA))
                .assertPasses();
    }
}
