package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.typed.spec.FactorsStepper;
import org.javai.punit.api.typed.spec.Scorer;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.Punit;

/**
 * OPTIMIZE experiment demonstrating temperature's effect on
 * structured-output reliability.
 *
 * <p>A developer might naively set temperature 1.0 thinking "more
 * creativity is better." For structured JSON output that's wrong —
 * higher temperatures produce format deviations, hallucinated
 * field names, and invalid values. This experiment walks the
 * temperature down from 1.0 to 0.0 in 0.1 steps and shows the
 * pass rate climb.
 *
 * <h2>Expected progression</h2>
 *
 * <pre>
 * Iteration  0:  temp=1.0  →  ~50% success (high hallucination)
 * Iteration  1:  temp=0.9  →  ~55% success
 * ...
 * Iteration 10:  temp=0.0  → ~100% success (deterministic)
 * </pre>
 *
 * <p>Demonstrates a numeric stepper — simpler than the prompt
 * stepper since the search space is one continuous dimension and
 * the strategy is just linear decrease. Authors writing real
 * gradient-descent / hill-climbing search write the same
 * {@link FactorsStepper}-shaped lambda; the framework only cares
 * about (current, history) → next.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketOptimizeTemperature.optimizeTemperature
 * }</pre>
 */
public class ShoppingBasketOptimizeTemperature {

    private static final double TEMPERATURE_FLOOR = 0.0;
    private static final double STEP = 0.1;

    private static final List<String> SINGLE_INSTRUCTION =
            List.of("Add 2 apples and remove the bread");

    private static final Scorer SUCCESS_RATE = summary -> summary.total() == 0
            ? 0.0
            : (double) summary.successes() / (double) summary.total();

    /**
     * Linear search: decrease temperature by {@value #STEP} each
     * iteration; stop when the floor (0.0) would be passed.
     */
    private static final FactorsStepper<LlmTuning> COOL_DOWN = (current, history) -> {
        double next = current.temperature() - STEP;
        if (next < TEMPERATURE_FLOOR) {
            return null;
        }
        // Round to one decimal place — IEEE-754 subtraction would
        // otherwise leave the experiment id with values like
        // 0.30000000000000004 in baselines and reports.
        double rounded = Math.round(next * 10.0) / 10.0;
        return current.temperature(rounded);
    };

    @Experiment
    void optimizeTemperature() {
        Punit.optimizing(ShoppingBasketUseCase.sampling(SINGLE_INSTRUCTION, 20))
                .initialFactors(LlmTuning.DEFAULT.temperature(1.0))
                .stepper(COOL_DOWN)
                .maximize(SUCCESS_RATE)
                .maxIterations(11)         // covers 1.0, 0.9, …, 0.0
                .noImprovementWindow(20)   // disable early termination
                .experimentId("temperature-optimization-v1")
                .run();
    }
}
