package org.javai.punit.examples.typed;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.api.typed.Sampling;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.CoinTossUseCase;
import org.javai.punit.junit5.Punit;

/**
 * Worked example — typed-compositional authoring under JUnit.
 *
 * <p>Demonstrates the three canonical authoring shapes that ship in
 * punit 1.0:
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
 * <p>The use case under test is {@link CoinTossUseCase} —
 * deterministic, configurable threshold, no external dependencies.
 * Real-world examples would substitute an LLM- or service-backed
 * use case in the same shape.
 *
 * <h2>Running</h2>
 *
 * <p>The {@code @Experiment}-tagged tests are excluded from default
 * test runs; the punit Gradle plugin's {@code experiment} task
 * runs only them. {@code @ProbabilisticTest}-tagged tests run with
 * the standard {@code test} task. Both phases need the same
 * {@code punit.baseline.dir} system property pointing at a shared
 * baseline directory.
 *
 * <pre>{@code
 * # Phase 1 — establish the baseline.
 * ./gradlew experiment -Prun=CoinTossReliabilityExamples.measureBaseline \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines"
 *
 * # Phase 2 — verify against the baseline.
 * ./gradlew test --tests "CoinTossReliabilityExamples.shouldMeetSla" \
 *                --tests "CoinTossReliabilityExamples.shouldMatchBaseline" \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines"
 * }</pre>
 *
 * <p><b>Why the two phases need separate sample shapes for the
 * empirical pair to PASS:</b> Wilson-score's one-sided lower bound
 * on the test's observed rate at confidence 95% is always strictly
 * below the test's observed value (by a margin that shrinks as
 * sample size grows). So when test and baseline observe the same
 * rate, the test's lower bound dips below the baseline's recorded
 * rate and the criterion FAILs — correctly enforcing statistical
 * rigour ("we observed 95% but cannot confidently claim the true
 * rate is ≥ 95%"). The example below uses a deterministic use case
 * whose observed rate at the test's smaller sample window
 * ({@code n=50}) is above the baseline's recorded rate (computed
 * across {@code n=1000} cycling through 100 inputs), giving the
 * Wilson check enough margin to clear.
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
     * Sampling shape — same use-case factory, same inputs at measure
     * and at test, varying only the sample count. The integrity
     * guarantee for the empirical pair.
     */
    private static Sampling<CoinTossUseCase.Bias, Integer, String> sampling(int samples) {
        return Sampling.<CoinTossUseCase.Bias, Integer, String>builder()
                .useCaseFactory(CoinTossUseCase::new)
                .inputs(CYCLE_1_TO_100)
                .samples(samples)
                .build();
    }

    /**
     * The baseline supplier — a non-test method that returns a
     * built {@link org.javai.punit.api.typed.spec.Experiment} value.
     * Consumed by the empirical test below via
     * {@link Punit#testing(java.util.function.Supplier)}.
     */
    private org.javai.punit.api.typed.spec.Experiment baseline() {
        return Punit.measuring(sampling(1000), BIAS_94)
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
        Punit.measuring(sampling(1000), BIAS_94)
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
        Punit.testing(sampling(200), BIAS_94)
                .criterion(BernoulliPassRate.<String>meeting(0.90, ThresholdOrigin.SLA))
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
        Punit.testing(this::baseline)
                .samples(50)
                .criterion(BernoulliPassRate.<String>empirical())
                .assertPasses();
    }
}
