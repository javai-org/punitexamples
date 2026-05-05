package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.spec.Scorer;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * OPTIMIZE experiment that uses a meta-LLM as a prompt engineer to
 * refine the system prompt iteratively. The stepper logic — meta-LLM
 * configuration, message formatting, failure-breakdown rendering —
 * lives in {@link PromptEngineerStepper}; this class wires it into a
 * PUnit optimize run. Early termination is disabled so all
 * {@code maxIterations} run regardless of whether a given step
 * improves the score — useful pedagogically, since a flat or
 * dipping intermediate iteration is informative about how the
 * meta-LLM responds to feedback.
 *
 * <h2>Setup</h2>
 *
 * <p>This experiment makes real LLM calls. Configure the
 * {@code ChatLlm} provider via {@code OPENAI_API_KEY} (or the
 * equivalent for your provider). With {@code maxIterations=5} and
 * 20 samples per iteration, expect ~100 sample calls plus 5
 * meta-prompt calls.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketOptimizePrompt.optimizeSystemPrompt
 * }</pre>
 */
public class ShoppingBasketOptimizePrompt {

    private static final List<String> BASKET_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    // Schema-shape-correct seed but missing the rules: no action
    // enum, no quantity constraint, no "clear" special case, no
    // worked examples. Iter 0 produces parseable JSON often enough
    // that postcondition error messages give the meta-LLM directed
    // signal to tighten — rather than asking it to invent the
    // schema from a vague two-line prompt.
    private static final String INITIAL_PROMPT = """
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
            """;

    private static final Scorer PASS_RATE_SCORE = summary -> summary.total() == 0
            ? 0.0
            : (double) summary.successes() / (double) summary.total();

    @Experiment
    void optimizeSystemPrompt() {
        if (ChatLlmProvider.isMockMode()) {
            // The mock LLM ignores prompt content and emits canned
            // shopping-basket JSON, which makes meta-prompt
            // engineering impossible. Set PUNIT_LLM_MODE=real (with
            // OPENAI_API_KEY) before running this experiment.
            throw new IllegalStateException(
                    "ShoppingBasketOptimizePrompt requires a real meta-LLM. "
                            + "Set PUNIT_LLM_MODE=real and provide OPENAI_API_KEY.");
        }
        PUnit.optimizing(ShoppingBasketUseCase.sampling(BASKET_INSTRUCTIONS, 20))
                .initialFactors(LlmTuning.DEFAULT.systemPrompt(INITIAL_PROMPT))
                .stepper(PromptEngineerStepper.create())
                .maximize(PASS_RATE_SCORE)
                .maxIterations(5)
                .disableEarlyTermination()
                .experimentId("prompt-optimization-v1")
                .run();
    }
}
