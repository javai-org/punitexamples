package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.Punit;

/**
 * Demonstrates the diagnostic output the typed pipeline produces
 * when a probabilistic test does not pass.
 *
 * <h2>What's available today</h2>
 *
 * <ul>
 *   <li><b>Per-criterion explanation</b> — every {@code CriterionResult}
 *       carries a human-readable explanation. For
 *       {@link BernoulliPassRate#empirical() empirical} runs this is
 *       e.g.
 *       <pre>{@code observed=0.94 (Wilson-95% lower=0.93) vs threshold=0.85 (origin=EMPIRICAL) over 100 samples}</pre>
 *       which surfaces the observed rate, the Wilson-score lower
 *       bound, the threshold and its origin, and the sample count
 *       — the figures that drive the verdict.</li>
 *   <li><b>Detail map</b> — the same numbers are available
 *       structurally on {@code CriterionResult.detail()} for tooling
 *       that wants to render them differently.</li>
 *   <li><b>Misalignment notes</b> — when no baseline matches, the
 *       verdict's warnings list each rejected candidate and the
 *       category mismatch that rejected it (CV-4).</li>
 *   <li><b>FAIL / INCONCLUSIVE diagnostics</b> — surfaced through
 *       the JUnit assertion message; visible in the IDE's test
 *       output, the surefire report, etc.</li>
 * </ul>
 *
 * <h2>What's deferred (legacy {@code transparentStats=true} feature)</h2>
 *
 * <p>The legacy pipeline supports {@code transparentStats = true},
 * which renders a full hypothesis-test breakdown — null hypothesis,
 * z-statistic, p-value — on <em>every</em> verdict, including PASS.
 * This is useful for audit / compliance documentation where the
 * statistical reasoning behind a passing run also needs to be
 * shown. The typed pipeline doesn't yet emit this verbose per-PASS
 * output; criterion-level diagnostics are only surfaced on FAIL /
 * INCONCLUSIVE today. Wiring transparentStats into the typed
 * pipeline is on the post-1.0 roadmap.
 *
 * <p>Early-termination visibility (impossibility / success-guaranteed)
 * is similarly legacy-only at the moment.
 *
 * <h2>The tests below</h2>
 *
 * <p>Three configurations, each sharing the use case but exercising
 * a different criterion path. The migrated file is currently
 * pedagogically equivalent to {@link ShoppingBasketTest}; its
 * distinct identity returns when transparentStats lands in the
 * typed pipeline.
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
        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 100), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void empiricalAtHigherSampleCount() {
        // Larger sample count tightens the Wilson-score margin
        // around the observed rate. A run that's borderline at n=100
        // can be definitively PASS or FAIL at n=200 — diagnostics
        // are the same in shape, just with tighter numbers.
        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 200), LlmTuning.DEFAULT)
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
        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 100), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.meeting(0.85, ThresholdOrigin.SLA))
                .assertPasses();
    }
}
