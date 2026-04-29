package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.typed.spec.FactorsStepper;
import org.javai.punit.api.typed.spec.Scorer;
import org.javai.punit.examples.app.llm.ChatLlm;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.PUnit;

/**
 * OPTIMIZE experiment that uses a meta-LLM as a prompt engineer to
 * refine the system prompt iteratively. At each iteration the
 * stepper formats the previous prompt and its success rate as a
 * user message, asks the meta-LLM to suggest something better, and
 * the suggestion becomes the next iteration's system prompt. The
 * loop runs until {@code maxIterations} or
 * {@code noImprovementWindow} terminates it.
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

    private static final String META_LLM_MODEL = "gpt-4o";
    private static final double META_LLM_TEMPERATURE = 0.5;

    private static final String META_SYSTEM_PROMPT = """
            You are a prompt engineer. The user gives you a system prompt
            currently being used with an LLM-backed shopping-basket
            translation service, plus the success rate that prompt
            achieved on a JSON-translation task. Your job: propose an
            improved version of the prompt that addresses common LLM
            failure modes for structured-output tasks — vague schema,
            hallucinated action names, missing required fields, free-form
            commentary mixed in with JSON. Output only the new system
            prompt. No commentary, no preamble, no surrounding quotes.
            """;

    private static final List<String> BASKET_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    private static final String INITIAL_PROMPT = """
            You are a shopping assistant. Convert the user's request into
            a JSON list of shopping basket operations.
            """;

    private static final Scorer SUCCESS_RATE = summary -> summary.total() == 0
            ? 0.0
            : (double) summary.successes() / (double) summary.total();

    private static FactorsStepper<LlmTuning> promptEngineerStepper() {
        ChatLlm metaLlm = ChatLlmProvider.resolve();
        return (current, history) -> {
            if (history.isEmpty()) {
                return null;
            }
            FactorsStepper.IterationResult<LlmTuning> last = history.getLast();
            String userMessage = """
                    Current system prompt:
                    %s

                    Success rate on the translation task: %.2f

                    Suggest an improved version.
                    """.formatted(last.factors().systemPrompt(), last.score());
            String suggested = metaLlm.chat(
                    META_SYSTEM_PROMPT,
                    userMessage,
                    META_LLM_MODEL,
                    META_LLM_TEMPERATURE);
            return current.systemPrompt(suggested);
        };
    }

    @Experiment
    void optimizeSystemPrompt() {
        PUnit.optimizing(ShoppingBasketUseCase.sampling(BASKET_INSTRUCTIONS, 20))
                .initialFactors(LlmTuning.DEFAULT.systemPrompt(INITIAL_PROMPT))
                .stepper(promptEngineerStepper())
                .maximize(SUCCESS_RATE)
                .maxIterations(5)
                .noImprovementWindow(2)
                .experimentId("prompt-optimization-v1")
                .run();
    }
}
