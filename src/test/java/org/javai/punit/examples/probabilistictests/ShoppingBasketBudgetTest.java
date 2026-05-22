package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.PROBABILISTIC_TEST_SAMPLE_SIZE;

import java.time.Duration;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.spec.BudgetExhaustionPolicy;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * Demonstrates budget management — controlling wall-clock time and
 * LLM token consumption per test run, essential when testing against
 * paid APIs or under a CI time-box.
 *
 * <ul>
 *   <li>{@code .timeBudget(Duration)} — wall-clock cap on the run.</li>
 *   <li>{@code .tokenBudget(long)} — token cap on the run. The
 *       service contract records actual tokens via
 *       {@code tracker.recordTokens(...)} inside {@code invoke}.</li>
 *   <li>{@code .onBudgetExhausted(BudgetExhaustionPolicy)} — what to
 *       do when a budget is exceeded:
 *       <ul>
 *         <li>{@link BudgetExhaustionPolicy#FAIL} — fail the run,
 *             refuse to emit a verdict on incomplete data.</li>
 *         <li>{@link BudgetExhaustionPolicy#PASS_INCOMPLETE} —
 *             synthesise a verdict from samples completed so far,
 *             attach a warning.</li>
 *       </ul>
 *   </li>
 * </ul>
 */
public class ShoppingBasketBudgetTest {

    @ProbabilisticTest
    void timeBudgetFailsRunOnExhaustion() {
        // 10-second cap; budget exhaustion fails the run rather than
        // emitting a verdict on incomplete data. Use this when the
        // statistical claim requires the full sample set.
        PUnit.testing(
                ShoppingBasketServiceContract.samplingBuilder(PROBABILISTIC_TEST_SAMPLE_SIZE)
                        .timeBudget(Duration.ofSeconds(10))
                        .onBudgetExhausted(BudgetExhaustionPolicy.FAIL)
                        .build(),
                LlmTuning.DEFAULT)
                .assertPasses();
    }

    @ProbabilisticTest
    void timeBudgetEvaluatesPartialOnExhaustion() {
        // Same time cap, but on exhaustion the engine synthesises a
        // verdict from completed samples and attaches a warning. Use
        // this when partial information is better than none — but be
        // aware the verdict may not be statistically significant.
        PUnit.testing(
                ShoppingBasketServiceContract.samplingBuilder(PROBABILISTIC_TEST_SAMPLE_SIZE)
                        .timeBudget(Duration.ofSeconds(10))
                        .onBudgetExhausted(BudgetExhaustionPolicy.PASS_INCOMPLETE)
                        .build(),
                LlmTuning.DEFAULT)
                .assertPasses();
    }

    @ProbabilisticTest
    void tokenBudgetWithDynamicTracking() {
        // 10K-token cap. The service contract records
        // response.totalTokens() via tracker.recordTokens(...) inside
        // invoke, so the engine sees real usage per sample.
        PUnit.testing(
                ShoppingBasketServiceContract.samplingBuilder(PROBABILISTIC_TEST_SAMPLE_SIZE)
                        .tokenBudget(10_000L)
                        .onBudgetExhausted(BudgetExhaustionPolicy.PASS_INCOMPLETE)
                        .build(),
                LlmTuning.DEFAULT)
                .assertPasses();
    }
}
