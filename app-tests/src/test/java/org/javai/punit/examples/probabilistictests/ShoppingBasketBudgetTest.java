package org.javai.punit.examples.probabilistictests;

import java.time.Duration;
import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.typed.spec.BudgetExhaustionPolicy;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * Demonstrates budget management — controlling wall-clock time and
 * LLM token consumption per test run, essential when testing against
 * paid APIs or under a CI time-box.
 *
 * <ul>
 *   <li>{@code .timeBudget(Duration)} — wall-clock cap on the run.</li>
 *   <li>{@code .tokenBudget(long)} — token cap on the run. The use
 *       case stamps actual tokens via
 *       {@code UseCaseOutcome.withTokens(...)} per sample.</li>
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

    private static final List<String> STANDARD_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    @ProbabilisticTest
    void timeBudgetFailsRunOnExhaustion() {
        // 10-second cap; budget exhaustion fails the run rather than
        // emitting a verdict on incomplete data. Use this when the
        // statistical claim requires the full sample set.
        PUnit.testing(
                ShoppingBasketUseCase.samplingBuilder(STANDARD_INSTRUCTIONS, 100)
                        .timeBudget(Duration.ofSeconds(10))
                        .onBudgetExhausted(BudgetExhaustionPolicy.FAIL)
                        .build(),
                LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void timeBudgetEvaluatesPartialOnExhaustion() {
        // Same time cap, but on exhaustion the engine synthesises a
        // verdict from completed samples and attaches a warning. Use
        // this when partial information is better than none — but be
        // aware the verdict may not be statistically significant.
        PUnit.testing(
                ShoppingBasketUseCase.samplingBuilder(STANDARD_INSTRUCTIONS, 100)
                        .timeBudget(Duration.ofSeconds(10))
                        .onBudgetExhausted(BudgetExhaustionPolicy.PASS_INCOMPLETE)
                        .build(),
                LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void tokenBudgetWithDynamicTracking() {
        // 10K-token cap. The use case stamps response.totalTokens()
        // onto each UseCaseOutcome via .withTokens(...), so the engine
        // sees real usage per sample.
        PUnit.testing(
                ShoppingBasketUseCase.samplingBuilder(STANDARD_INSTRUCTIONS, 100)
                        .tokenBudget(10_000L)
                        .onBudgetExhausted(BudgetExhaustionPolicy.PASS_INCOMPLETE)
                        .build(),
                LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }
}
