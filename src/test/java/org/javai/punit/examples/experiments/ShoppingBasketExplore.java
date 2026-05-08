package org.javai.punit.examples.experiments;

import static org.javai.punit.examples.usecases.ShoppingBasketSampleSizes.EXPLORE_PER_CONFIG_SAMPLE_SIZE;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * EXPLORE experiment comparing LLM model configurations across the
 * same input set. The grid cycles through four models at a fixed
 * low temperature; each model receives {@code samplesPerConfig}
 * samples and the framework reports the pass rate per configuration.
 *
 * <p>The seed prompt is realistic-but-imperfect (schema and action
 * enum specified, no worked examples) and the input set mixes easy
 * structured cases with ambiguity-heavy ones (substitution,
 * decimal/unbounded quantities, conversational phrasing). With
 * 2026's frontier models the per-row pass rates often converge —
 * but a model comparison is more than pass-rate. The generated
 * YAML carries per-sample {@code executionTimeMs} alongside the
 * functional verdict, and once latency percentile aggregation
 * (LT01) lands in the EXPLORE writer the comparison along the
 * latency dimension becomes the headline differentiator: same
 * functional behaviour, different cost-per-call profiles.
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
            // Easy — straightforward schema-fidelity test.
            "Add 2 apples",
            "Remove the milk",
            "Clear the basket",
            // Multi-action — exercises the actions[] array.
            "Add 3 oranges and 2 bananas",
            "Add 5 tomatoes and remove the cheese",
            // Quantity normalisation — model must convert to integer.
            "Add a dozen eggs",
            "Add half a kilo of apples",
            // Hard interpretation — substitution, bulk over an
            // unspecified set, conversational phrasing with implicit
            // quantity. These are where frontier models actually
            // differ; the easier cases above just anchor a baseline.
            "Swap the milk for almond milk",
            "I'd like to remove all the vegetables",
            "Could you please add some pears? Maybe two.");

    private static final String WEAK_INITIAL_PROMPT = """
            You are a shopping assistant that converts natural language
            instructions into JSON actions.

            Respond with JSON only. The JSON must contain an "actions"
            array, even for single operations.

            Format:
            {
              "actions": [
                {
                  "context": "SHOP",
                  "name": "<action>",
                  "parameters": [
                    {"name": "item", "value": "<item_name>"},
                    {"name": "quantity", "value": "<number>"}
                  ]
                }
              ]
            }

            Valid actions for SHOP context: "add", "remove", "clear".
            """;

    private static final LlmTuning WEAK_BASE = LlmTuning.DEFAULT
            .temperature(0.1)
            .systemPrompt(WEAK_INITIAL_PROMPT);

    @Experiment
    void compareModels() {
        PUnit.exploring(ShoppingBasketUseCase.sampling(BASKET_INSTRUCTIONS, EXPLORE_PER_CONFIG_SAMPLE_SIZE))
                .experimentId("model-comparison-v1")
                .grid(
                        WEAK_BASE.model("gpt-4o-mini"),
                        WEAK_BASE.model("gpt-4o"),
                        WEAK_BASE.model("claude-haiku-4-5-20251001"),
                        WEAK_BASE.model("claude-sonnet-4-5-20250929"))
                .run();
    }
}
