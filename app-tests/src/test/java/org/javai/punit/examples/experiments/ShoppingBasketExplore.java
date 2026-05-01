package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * EXPLORE experiment comparing LLM model configurations across the
 * same input set. The grid cycles through four models at a fixed
 * low temperature; each model receives {@code samplesPerConfig}
 * samples and the framework reports the pass rate per configuration.
 *
 * <h2>Setup</h2>
 *
 * <p>This experiment makes real LLM calls. Configure the
 * {@code ChatLlm} provider via {@code OPENAI_API_KEY} and
 * {@code ANTHROPIC_API_KEY} for the models in the grid.
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
