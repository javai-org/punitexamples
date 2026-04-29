package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.typed.spec.FactorsStepper;
import org.javai.punit.api.typed.spec.Scorer;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.Punit;

/**
 * OPTIMIZE experiment iteratively refining the system prompt.
 *
 * <p>Demonstrates how the typed-pipeline optimize loop walks the
 * factor space toward higher pass rates. Starts from a deliberately
 * weak prompt and applies a scripted progression that addresses
 * common failure modes one at a time.
 *
 * <h2>Expected progression</h2>
 *
 * <pre>
 * Iteration 0 (initial weak prompt):           ~30% success
 * Iteration 1 (+ JSON-only format):            ~50% success
 * Iteration 2 (+ explicit schema):             ~65% success
 * Iteration 3 (+ required fields):             ~80% success
 * Iteration 4 (+ valid action enumeration):    ~90% success
 * Iteration 5 (+ quantity constraints):        ~95% success
 * </pre>
 *
 * <h2>Authoring shape</h2>
 *
 * <p>The typed optimize builder reads cleanly:
 *
 * <pre>{@code
 * Punit.optimizing(sampling(...))
 *         .initialFactors(LlmTuning.DEFAULT.systemPrompt(WEAK_PROMPT))
 *         .stepper((current, history) -> nextPromptVariant(current, history))
 *         .maximize(SUCCESS_RATE)
 *         .maxIterations(10)
 *         .noImprovementWindow(3)
 *         .experimentId(...)
 *         .run();
 * }</pre>
 *
 * <p>The legacy form pulled scorer and mutator from separate
 * dedicated classes ({@code ShoppingBasketSuccessRateScorer},
 * {@code ShoppingBasketPromptMutator}, plus a strategy hierarchy
 * underneath); the typed {@link Scorer} and {@link FactorsStepper}
 * interfaces are functional, so an inlined lambda or method
 * reference is sufficient. The {@code optimize} subpackage that
 * housed those classes is removed in this PR.
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

    /**
     * Deliberately weak starting prompt — plausible but missing
     * the JSON schema, valid-action enumeration, and quantity
     * constraints the LLM needs to produce reliable structured
     * output.
     */
    private static final String WEAK_PROMPT = """
            You are a shopping assistant. Convert the user's request into
            a JSON list of shopping basket operations.
            """;

    /**
     * Scripted prompt progression. Each entry addresses one common
     * failure mode left over from the previous prompt. The stepper
     * consumes this list iteratively; reaching the end signals
     * "no more candidates" and the optimiser stops.
     */
    private static final List<String> PROMPT_PROGRESSION = List.of(
            // 1. Add JSON-only format requirement
            """
            You are a shopping assistant. Convert the user's request into
            a JSON list of shopping basket operations. Respond with JSON only.
            """,
            // 2. Add explicit schema
            """
            You are a shopping assistant. Convert the user's request into
            JSON of the form {"actions": [...]}. Respond with JSON only.
            """,
            // 3. Add required fields
            """
            You are a shopping assistant. Convert the user's request into
            JSON of the form {"actions": [{"name": <action>, "item": <item>,
            "quantity": <count>}]}. Respond with JSON only.
            """,
            // 4. Add valid action enumeration
            """
            You are a shopping assistant. Convert the user's request into
            JSON of the form {"actions": [{"name": <action>, "item": <item>,
            "quantity": <count>}]}. Valid actions: add, remove, clear.
            Respond with JSON only.
            """,
            // 5. Add quantity constraints
            """
            You are a shopping assistant. Convert the user's request into
            JSON of the form {"actions": [{"name": <action>, "item": <item>,
            "quantity": <positive integer>}]}. Valid actions: add, remove, clear.
            For "clear", omit item and quantity. Respond with JSON only.
            """);

    private static final Scorer SUCCESS_RATE = summary -> summary.total() == 0
            ? 0.0
            : (double) summary.successes() / (double) summary.total();

    private static final FactorsStepper<LlmTuning> NEXT_PROMPT_VARIANT =
            (current, history) -> {
                int iterationsCompleted = history.size();
                if (iterationsCompleted >= PROMPT_PROGRESSION.size()) {
                    // Exhausted the scripted progression — signal stop.
                    return null;
                }
                return current.systemPrompt(PROMPT_PROGRESSION.get(iterationsCompleted));
            };

    @Experiment
    void optimizeSystemPrompt() {
        Punit.optimizing(ShoppingBasketUseCase.sampling(BASKET_INSTRUCTIONS, 20))
                .initialFactors(LlmTuning.DEFAULT.systemPrompt(WEAK_PROMPT))
                .stepper(NEXT_PROMPT_VARIANT)
                .maximize(SUCCESS_RATE)
                .maxIterations(10)
                .noImprovementWindow(3)
                .experimentId("prompt-optimization-v1")
                .run();
    }
}
