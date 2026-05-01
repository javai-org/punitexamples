package org.javai.punit.examples.probabilistictests;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.examples.usecases.CoinTossUseCase;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.usecases.CoinTossUseCase;
import org.javai.punit.runtime.PUnit;

/**
 * Worked example for compositional authoring under JUnit against
 * {@link CoinTossUseCase}, a deterministic configurable use case.
 * Demonstrates three authoring shapes:
 *
 * <ol>
 *   <li>{@code @Experiment} measure — produces a baseline file at
 *       the configured baseline directory.</li>
 *   <li>{@code @ProbabilisticTest} contractual — asserts an
 *       SLA-style threshold the observed pass rate must clear.</li>
 *   <li>{@code @ProbabilisticTest} empirical — derives factors and
 *       sampling from the baseline by reference; asserts the
 *       Wilson-score lower bound clears the recorded baseline rate.</li>
 * </ol>
 *
 * <h2>Setup</h2>
 *
 * <p>Both phases need the same {@code punit.baseline.dir} system
 * property pointing at a shared baseline directory.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * # 1. Establish the baseline.
 * ./gradlew experiment -Prun=CoinTossReliabilityExamples.measureBaseline \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines"
 *
 * # 2. Verify against the baseline.
 * ./gradlew test --tests "CoinTossReliabilityExamples.shouldMeetSla" \
 *                --tests "CoinTossReliabilityExamples.shouldMatchBaseline" \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines"
 * }</pre>
 *
 * <p>Note on the empirical pair: Wilson-score's one-sided lower
 * bound on the observed rate is always strictly below the observed
 * value, by a margin that shrinks as sample size grows. So when
 * test and baseline observe the same rate, the test's lower bound
 * dips below the baseline rate and the criterion FAILs. The
 * example uses a smaller sample window in the test ({@code n=50}
 * over inputs 1..50, all passing under {@code threshold=94}) so
 * the Wilson lower bound at observed=1.0 just clears the recorded
 * 0.94 baseline.
 */
public class CoinTossReliabilityExamples {

    private static final CoinTossUseCase.Bias BIAS_94 = new CoinTossUseCase.Bias(94);

    /**
     * Inputs cycling 1..100. With {@link CoinTossUseCase.Bias#threshold}
     * set to 94, exactly 94 of every 100 inputs satisfy the contract.
     */
    private static final java.util.List<Integer> CYCLE_1_TO_100 =
            IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toUnmodifiableList());

    /**
     * Baseline supplier consumed by the empirical test below via
     * {@link PUnit#testing(java.util.function.Supplier)}. Same
     * sampling and factors as {@link #measureBaseline()}.
     */
    private org.javai.punit.api.spec.Experiment baseline() {
        return PUnit.measuring(CoinTossUseCase.sampling(CYCLE_1_TO_100, 1000), BIAS_94)
                .experimentId("baseline-v1")
                .build();
    }

    // ── Phase 1: @Experiment measure ───────────────────────────────

    @Experiment
    void measureBaseline() {
        // Same Sampling and factors as baseline() above; running this
        // method writes the baseline YAML to the configured directory.
        // Cycling 1..100 ten times produces an exact 0.94 observed
        // pass rate (94 passes per 100 inputs × 10 cycles).
        PUnit.measuring(CoinTossUseCase.sampling(CYCLE_1_TO_100, 1000), BIAS_94)
                .experimentId("baseline-v1")
                .run();
    }

    // ── Phase 2: @ProbabilisticTest contractual ────────────────────

    @ProbabilisticTest
    void shouldMeetSla() {
        // Contractual: observed 0.94 should comfortably clear a 0.90 SLA.
        // No baseline involved — the threshold is declared explicitly,
        // and the contractual path uses observed >= threshold (no Wilson
        // wrap), so 0.94 ≥ 0.90 → PASS straightforwardly.
        PUnit.testing(CoinTossUseCase.sampling(CYCLE_1_TO_100, 200), BIAS_94)
                .criterion(BernoulliPassRate.meeting(0.90, ThresholdOrigin.SLA))
                .assertPasses();
    }

    // ── Phase 2: @ProbabilisticTest empirical (paired) ─────────────

    @ProbabilisticTest
    void shouldMatchBaseline() {
        // Empirical: derive factors and sampling from the baseline,
        // override only the sample count. Test runs through the first
        // 50 inputs (1..50) of the baseline's cycle — all pass under
        // threshold 94 — observed = 1.0. Wilson-score lower bound at
        // p=1.0, n=50, c=0.95 ≈ 0.949, just clearing the baseline's
        // recorded 0.94. The Wilson margin is the integrity check —
        // the test cannot claim "matches baseline" without statistical
        // backing.
        PUnit.testing(this::baseline)
                .samples(50)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }
}
