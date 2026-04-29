package org.javai.punit.examples.probabilistictests;

import java.time.Duration;
import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.typed.spec.BudgetExhaustionPolicy;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.PUnit;

/**
 * Demonstrates budget management features in probabilistic testing.
 *
 * <p>Budget management controls resource consumption (wall-clock
 * time, LLM tokens) per test run. Essential when testing against
 * paid APIs or when tests need to complete within a time-box.
 *
 * <h2>What this demonstrates</h2>
 *
 * <ul>
 *   <li>{@code .timeBudget(Duration)} — wall-clock cap on the run.</li>
 *   <li>{@code .tokenBudget(long)} — token cap on the run. The use
 *       case stamps actual tokens via
 *       {@code UseCaseOutcome.withTokens(...)} per sample, so the
 *       engine totals real usage rather than relying on estimates.</li>
 *   <li>{@code .onBudgetExhausted(BudgetExhaustionPolicy)} — what to
 *       do when a budget is exceeded:
 *       <ul>
 *         <li>{@link BudgetExhaustionPolicy#FAIL} — fail the run,
 *             refuse to emit a verdict on incomplete data</li>
 *         <li>{@link BudgetExhaustionPolicy#PASS_INCOMPLETE} —
 *             synthesise a verdict from samples completed so far,
 *             attach a warning</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>What's <em>not</em> in the typed migration</h2>
 *
 * <p>The legacy test featured a class-level {@code @ProbabilisticTestBudget}
 * shared across methods, plus a static-charge {@code tokenCharge}
 * per-sample variant and a dynamic {@code TokenChargeRecorder}
 * parameter. The typed pipeline doesn't have a class-level shared
 * budget concept — budgets attach to a {@link Sampling} instance,
 * one per test. Class-level budget enforcement would be a future
 * framework addition; nothing prevents authors from declaring the
 * same budget across multiple tests in the meantime.
 *
 * <p>The typed pipeline also folds static and dynamic token
 * accounting into one path: the use case stamps actual tokens on
 * each {@link org.javai.punit.api.typed.UseCaseOutcome}, the engine
 * totals them, no recorder parameter required. This is strictly more
 * accurate than the legacy static-charge estimate and removes a step
 * from the test author's mental model.
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
        // 10K-token cap. The typed ShoppingBasketUseCase stamps
        // response.totalTokens() onto each UseCaseOutcome via
        // .withTokens(...), so the engine sees real usage per sample.
        // No recorder parameter; no estimate; the use case is the
        // source of truth.
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
