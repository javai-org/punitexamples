package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.PUnit;

/**
 * Demonstrates the diagnostic output the framework produces when a
 * probabilistic test runs.
 *
 * <h2>Diagnostic features in punit</h2>
 *
 * <p><b>Per-criterion explanation and detail.</b> Every
 * {@code CriterionResult} carries a human-readable explanation
 * string and a structured {@code detail()} map. For empirical
 * {@link BernoulliPassRate} runs the explanation reads e.g.
 * <pre>{@code observed=0.94 (Wilson-95% lower=0.93) vs threshold=0.85 (origin=EMPIRICAL) over 100 samples}</pre>
 * — the figures that drove the verdict. The typed pipeline surfaces
 * these on FAIL and INCONCLUSIVE verdicts via the JUnit assertion
 * message.
 *
 * <p><b>Misalignment notes.</b> When no baseline matches the run's
 * covariate profile, the verdict's warnings list each rejected
 * candidate and the category mismatch that rejected it (CV-4).
 *
 * <p><b>Verbose statistical reporting</b> (legacy
 * {@code transparentStats = true}). The framework can render a full
 * hypothesis-test breakdown — null hypothesis, z-statistic, p-value,
 * statistical inference — on every verdict including PASS. This is
 * essential for audit / compliance documentation where the
 * statistical reasoning behind a passing run also has to be shown.
 * The verbose console-rendering path is currently surfaced through
 * the legacy {@code @ProbabilisticTest(transparentStats = true)}
 * annotation; the typed pipeline owes a builder-side equivalent
 * (e.g. {@code PUnit.testing(...).transparentStats()}) before
 * this feature is fully exposed in the typed authoring surface.
 *
 * <p><b>Early-termination visibility</b>
 * (impossibility / success-guaranteed). The engine can stop a run
 * early when the verdict is mathematically determined ahead of the
 * configured sample count. The legacy annotation surfaces this
 * naturally; the typed pipeline reaches into the same evaluator
 * but its diagnostic carriage in the verdict still needs to land.
 *
 * <p>Both verbose reporting and early-termination diagnostics are
 * <em>tracked typed-pipeline work</em>, not legacy-only features —
 * they're recorded as features of punit and need their typed
 * authoring surfaces wired before this file can demonstrate them.
 *
 * <h2>The tests below</h2>
 *
 * <p>Three configurations exercising distinct criterion paths.
 * Once verbose reporting and early-termination diagnostics have
 * typed-builder surfaces, this file's tests will opt into them
 * explicitly and the diagnostic-feature demonstration becomes
 * concrete.
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
        // criterion explanation shape, tighter numbers. Once
        // early-termination diagnostics have a typed-builder
        // surface, the larger sample count is also where
        // impossibility / success-guaranteed signals become most
        // visible.
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
