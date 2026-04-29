package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.PUnit;

/**
 * EXPLORE experiment comparing LLM model configurations across the
 * same input set.
 *
 * <p>Before establishing a production baseline, you decide which
 * model is the right one for the task. This experiment answers:
 * "Which model handles these instructions most reliably?" — by
 * cycling the same input list through each model in turn and
 * comparing the resulting pass rates.
 *
 * <h2>Typical workflow</h2>
 *
 * <ol>
 *   <li><b>Explore</b> — this experiment compares models.</li>
 *   <li><b>Choose</b> — pick the configuration with the best
 *       pass rate / latency tradeoff for your domain.</li>
 *   <li><b>Measure</b> — {@link ShoppingBasketMeasure} establishes
 *       a baseline for that chosen configuration.</li>
 *   <li><b>Test</b> — {@code ShoppingBasketTest} verifies the
 *       LLM still meets the recorded baseline in CI.</li>
 * </ol>
 *
 * <h2>Grid as factor variants</h2>
 *
 * <p>Each grid entry is a {@link LlmTuning} value — the typed
 * factor record. The framework iterates the grid, constructs one
 * use case per entry via the sampling's factory closure, and runs
 * {@code samplesPerConfig} samples through each. The four entries
 * here all hold temperature at 0.1 (low randomness, more
 * deterministic output for translation tasks); the {@code model}
 * is what varies.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketExplore.compareModels
 * }</pre>
 */
public class ShoppingBasketExplore {

    private static final List<String> BASKET_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Add 5 tomatoes and remove the cheese",
            "Clear the basket",
            "Clear everything",
            "Remove 2 eggs from the basket",
            "Add a dozen eggs",
            "I'd like to remove all the vegetables");

    private static final LlmTuning LOW_TEMPERATURE = LlmTuning.DEFAULT.temperature(0.1);

    @Experiment
    void compareModels() {
        PUnit.exploring(ShoppingBasketUseCase.sampling(BASKET_INSTRUCTIONS, 20))
                .experimentId("model-comparison-v1")
                .grid(
                        LOW_TEMPERATURE.model("gpt-4o-mini"),
                        LOW_TEMPERATURE.model("gpt-4o"),
                        LOW_TEMPERATURE.model("claude-haiku-4-5-20251001"),
                        LOW_TEMPERATURE.model("claude-sonnet-4-5-20250929"))
                .run();
    }
}
