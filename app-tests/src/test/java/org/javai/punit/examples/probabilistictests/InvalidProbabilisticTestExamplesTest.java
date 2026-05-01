package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.TestIntent;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;
import org.junit.jupiter.api.Disabled;

/**
 * Quick-fail examples: invalid probabilistic-test configurations.
 *
 * <p>Each method below is <em>intentionally misconfigured</em>. PUnit must
 * reject it <strong>before any verdict can be emitted</strong> — at
 * criterion construction, builder build, pre-flight feasibility, or as an
 * evaluate-time INCONCLUSIVE.
 *
 * <h2>Glossary</h2>
 * <ul>
 *   <li><b>criterion</b> — a claim the test asserts must hold; added with
 *       {@code .criterion(...)}.</li>
 *   <li><b>contractual criterion</b> — explicit threshold, deterministic
 *       comparison; e.g.
 *       {@code BernoulliPassRate.meeting(0.95, ThresholdOrigin.SLA)}.</li>
 *   <li><b>empirical criterion</b> — derives the threshold from a baseline
 *       at evaluate time; e.g. {@code BernoulliPassRate.empirical()} or
 *       {@code .empiricalFrom(supplier)}.</li>
 *   <li><b>{@link TestIntent}</b> — VERIFICATION (default) requires a
 *       feasible configuration; SMOKE tolerates undersizing with a
 *       warning.</li>
 *   <li><b>baseline</b> — pass-rate statistics recorded by a prior measure
 *       run, written to the baseline directory and resolved by use-case
 *       identity, factors, and covariate profile.</li>
 * </ul>
 *
 * <h2>Pedagogical ordering</h2>
 * Start with the missing-claim case ("you forgot to assert anything"), then
 * move through wrong-entry-point misuse, infeasibility, origin mismatches,
 * baseline-required-but-missing, and finally pure range hygiene.
 */
@Disabled("Intentionally invalid configurations — documentation and manual sanity-checking only")
class InvalidProbabilisticTestExamplesTest {

    private static final List<String> INSTRUCTIONS = List.of(
            "Add 2 apples", "Remove the milk", "Add 3 oranges");

    // ═══════════════════════════════════════════════════════════════════════════
    // 1) NO CRITERION (the most common newcomer mistake)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * <b>Error:</b> No criterion supplied.
     *
     * <p><b>What it says:</b> "Run 50 samples and assert it passes."</p>
     * <p><b>Why invalid:</b> A probabilistic test needs a criterion to
     * make any claim. {@code .assertPasses()} without
     * {@code .criterion(...)} leaves the engine with nothing to evaluate;
     * the verdict carries no gating criterion result.</p>
     *
     * <p><b>Fix:</b> Add at least one gating criterion, e.g.
     * {@code .criterion(BernoulliPassRate.meeting(0.95, ThresholdOrigin.SLA))}
     * or {@code .criterion(BernoulliPassRate.empirical())}.</p>
     */
    @ProbabilisticTest
    void noCriterion_emptyClaim() {
        PUnit.testing(ShoppingBasketUseCase.sampling(INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .assertPasses();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2) WRONG ENTRY POINT — empirical / contractual mismatch
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * <b>Error:</b> Contractual criterion passed to the empirical entry
     * point.
     *
     * <p><b>What it says:</b> "Test against an empirical baseline, but
     * gate on an SLA-style explicit threshold."</p>
     * <p><b>Why invalid:</b> {@code PUnit.testing(supplier)} pulls
     * sampling, factors, and the threshold from the supplied baseline; a
     * contractual criterion (constructed via {@code .meeting(threshold,
     * origin)}) makes its own threshold claim independent of any
     * baseline. The two are incompatible at this entry point.</p>
     *
     * <p><b>Caught by:</b> {@code EmpiricalTestBuilder.criterion(...)} →
     * {@code IllegalArgumentException}: "PUnit.testing(supplier) only
     * accepts empirical criteria…".</p>
     * <p><b>Fix:</b> For an SLA-style claim, use the contractual entry
     * point {@code PUnit.testing(sampling, factors)} and pass the
     * contractual criterion there. To compare against a baseline,
     * switch to {@code BernoulliPassRate.empirical()} or
     * {@code .empiricalFrom(supplier)}.</p>
     */
    @ProbabilisticTest
    void empiricalEntryPoint_rejectsContractualCriterion() {
        PUnit.testing(() -> null) // baseline supplier — never invoked here
                .samples(50)
                .criterion(BernoulliPassRate.meeting(0.95, ThresholdOrigin.SLA));
    }

    /**
     * <b>Error:</b> Required builder field {@code samples} omitted.
     *
     * <p><b>What it says:</b> Empirical entry point with a criterion but
     * no sample count.</p>
     * <p><b>Why invalid:</b> The empirical entry point doesn't infer
     * sample count — the author specifies it explicitly so a verification
     * run is sized intentionally rather than reusing the (typically much
     * larger) baseline's sample count.</p>
     *
     * <p><b>Caught by:</b> {@code EmpiricalTestBuilder.build()} →
     * {@code IllegalStateException}: "samples is required — call
     * .samples(n) before .build() / .assertPasses()".</p>
     * <p><b>Fix:</b> Add {@code .samples(n)} before
     * {@code .assertPasses()}.</p>
     */
    @ProbabilisticTest
    void empiricalBuilder_missingSamples() {
        PUnit.testing(() -> null)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    /**
     * <b>Error:</b> Required builder field {@code criterion} omitted.
     *
     * <p><b>What it says:</b> Empirical entry point with a sample count
     * but no criterion.</p>
     * <p><b>Why invalid:</b> The empirical entry point doesn't infer
     * the criterion — the author chooses (e.g.) Bernoulli pass rate vs.
     * latency-percentile vs. some custom score. No default makes
     * sense.</p>
     *
     * <p><b>Caught by:</b> {@code EmpiricalTestBuilder.build()} →
     * {@code IllegalStateException}: "criterion is required — call
     * .criterion(BernoulliPassRate.empirical()) or similar before
     * .build() / .assertPasses()".</p>
     * <p><b>Fix:</b> Add {@code .criterion(...)} before
     * {@code .assertPasses()}.</p>
     */
    @ProbabilisticTest
    void empiricalBuilder_missingCriterion() {
        PUnit.testing(() -> null)
                .samples(50)
                .assertPasses();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3) INFEASIBILITY — sample size cannot support a confidence-backed claim
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * <b>Error:</b> VERIFICATION intent with infeasibly few samples for
     * the resolved baseline rate.
     *
     * <p><b>What it says:</b> "Verify against the recorded baseline with
     * 5 samples."</p>
     * <p><b>Why invalid:</b> Empirical evaluation wraps the observed
     * pass rate in a Wilson-score lower bound at the criterion's
     * confidence (default 0.95) and PASSes only if that bound clears
     * the baseline-derived threshold. With baseline rates near 1.0
     * (typical of stable services), 5 samples can never produce a
     * lower bound high enough to support a PASS — the configuration
     * is misconfigured before any sample executes.</p>
     *
     * <p><b>Caught by:</b> {@code Feasibility.check} pre-flight →
     * {@code IllegalStateException} (only when a baseline exists for
     * this identity; without a baseline, feasibility silently defers
     * and the criterion produces INCONCLUSIVE at evaluate time — see §5
     * below).</p>
     * <p><b>Fix:</b> Increase {@code samples} to the minimum the
     * feasibility evaluator requires for the resolved baseline rate, or
     * switch to {@code .intent(TestIntent.SMOKE)} (next example) if the
     * test is non-evidential by design.</p>
     */
    @ProbabilisticTest
    void infeasibleVerification_empirical() {
        PUnit.testing(ShoppingBasketUseCase.sampling(INSTRUCTIONS, 5), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    /**
     * <b>Note:</b> SMOKE intent <em>tolerates</em> the same configuration.
     *
     * <p><b>What it says:</b> Same 5-sample empirical configuration, but
     * marked as a smoke test rather than a verification claim.</p>
     * <p><b>Behaviour:</b> {@code Feasibility.check} returns a warning
     * (printed to stderr) instead of throwing; the run proceeds and the
     * verdict carries the smoke caveat. Useful as a quick-and-cheap
     * pre-merge probe; not suitable for compliance evidence.</p>
     *
     * <p>Included here for contrast with the VERIFICATION case above —
     * the difference between "rejected as misconfigured" and "tolerated
     * with a caveat" is one builder call.</p>
     */
    @ProbabilisticTest
    void smokeIntent_tolerantOfUndersizing() {
        PUnit.testing(ShoppingBasketUseCase.sampling(INSTRUCTIONS, 5), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .intent(TestIntent.SMOKE)
                .assertPasses();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4) ORIGIN MISMATCH
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * <b>Error:</b> {@code ThresholdOrigin.EMPIRICAL} on a contractual
     * criterion factory.
     *
     * <p><b>What it says:</b> "Use an explicit threshold of 0.95, but
     * label its origin as empirical."</p>
     * <p><b>Why invalid:</b> {@code .meeting(threshold, origin)} is the
     * contractual factory — its threshold is supplied by the author and
     * deterministic. The {@code EMPIRICAL} origin is reserved for the
     * empirical factories ({@code .empirical()}, {@code .empiricalFrom(...)})
     * which derive the threshold from a baseline at evaluate time.</p>
     *
     * <p><b>Caught by:</b> {@code BernoulliPassRate.meeting(...)} →
     * {@code IllegalArgumentException}: "ThresholdOrigin.EMPIRICAL is
     * reserved for the empirical factories…".</p>
     * <p><b>Fix:</b> Either pick a non-empirical origin
     * ({@code SLA}, {@code SLO}, {@code POLICY}, {@code NONE}), or
     * switch to {@code BernoulliPassRate.empirical()} and let the
     * threshold come from the baseline.</p>
     */
    @ProbabilisticTest
    void contractualCriterion_rejectsEmpiricalOrigin() {
        BernoulliPassRate.meeting(0.95, ThresholdOrigin.EMPIRICAL);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5) EMPIRICAL CRITERION WITHOUT A RESOLVABLE BASELINE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * <b>Error:</b> Empirical criterion when no baseline file exists for
     * the test's identity.
     *
     * <p><b>What it says:</b> "Compare against the baseline" — but no
     * matching baseline has been recorded yet.</p>
     * <p><b>Why invalid (sort of):</b> This is the canonical
     * <em>workflow</em> mistake — attempting verification before the
     * measure step has produced a baseline. Unlike the resolver in the
     * pre-typed pipeline (which rejected baseline-required configurations
     * at config time), the typed pipeline doesn't reject pre-flight: it
     * runs to completion and the criterion reports INCONCLUSIVE at
     * evaluate time with the message "no matching baseline was resolvable
     * for empirical threshold."</p>
     *
     * <p><b>Caught by:</b> {@code BernoulliPassRate.evaluate} returns a
     * {@code Verdict.INCONCLUSIVE} {@code CriterionResult}; the test
     * surfaces as a JUnit <em>aborted</em> result, not a failure.</p>
     * <p><b>Fix:</b> Run the measure phase first
     * ({@code PUnit.measuring(sampling, factors).run()}) so a baseline
     * exists, then run the verification test. See the empirical-pair
     * pattern in {@link CoinTossReliabilityExamples} for the canonical
     * workflow.</p>
     */
    @ProbabilisticTest
    void empiricalCriterion_noBaselineProducesInconclusive() {
        PUnit.testing(ShoppingBasketUseCase.sampling(INSTRUCTIONS, 100), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6) RANGE VIOLATIONS — pure hygiene
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * <b>Error:</b> Threshold above {@code 1.0}.
     *
     * <p><b>Why invalid:</b> A pass rate is a probability.</p>
     * <p><b>Caught by:</b> {@code BernoulliPassRate.meeting(...)} →
     * {@code IllegalArgumentException}: "threshold must be in [0, 1]…".</p>
     * <p><b>Fix:</b> Use a value in {@code [0, 1]}.</p>
     */
    @ProbabilisticTest
    void thresholdAboveOne() {
        BernoulliPassRate.meeting(1.5, ThresholdOrigin.SLA);
    }

    /**
     * <b>Error:</b> Threshold below {@code 0.0}.
     *
     * <p><b>Why invalid:</b> A pass rate is a probability.</p>
     * <p><b>Caught by:</b> {@code BernoulliPassRate.meeting(...)} →
     * {@code IllegalArgumentException}: "threshold must be in [0, 1]…".</p>
     */
    @ProbabilisticTest
    void thresholdBelowZero() {
        BernoulliPassRate.meeting(-0.1, ThresholdOrigin.SLA);
    }

    /**
     * <b>Error:</b> Confidence at the upper boundary {@code 1.0}.
     *
     * <p><b>Why invalid:</b> {@code α = 0} implies absolute certainty,
     * which cannot be obtained from finite samples. If the system under
     * test is fully deterministic, use a regular JUnit {@code @Test} —
     * not a probabilistic one.</p>
     * <p><b>Caught by:</b> {@code BernoulliPassRate.atConfidence(...)} →
     * {@code IllegalArgumentException}: "confidence must be in (0, 1)…".</p>
     * <p><b>Fix:</b> Use a value strictly between 0 and 1
     * (e.g. 0.95, 0.99).</p>
     */
    @ProbabilisticTest
    void confidenceAtUpperBoundary() {
        BernoulliPassRate.empirical().atConfidence(1.0);
    }

    /**
     * <b>Error:</b> Confidence at the lower boundary {@code 0.0}.
     *
     * <p><b>Why invalid:</b> {@code α = 1} is meaningless — accepting
     * any observation as evidence at no confidence level.</p>
     * <p><b>Caught by:</b> {@code BernoulliPassRate.atConfidence(...)} →
     * {@code IllegalArgumentException}: "confidence must be in (0, 1)…".</p>
     */
    @ProbabilisticTest
    void confidenceAtLowerBoundary() {
        BernoulliPassRate.empirical().atConfidence(0.0);
    }

    /**
     * <b>Error:</b> Sample count of zero (or negative).
     *
     * <p><b>Why invalid:</b> A test must take at least one sample to
     * produce evidence.</p>
     * <p><b>Caught by:</b> {@code EmpiricalTestBuilder.samples(...)} →
     * {@code IllegalArgumentException}: "samples must be >= 1…".</p>
     */
    @ProbabilisticTest
    void zeroSamples() {
        PUnit.testing(() -> null).samples(0);
    }
}
