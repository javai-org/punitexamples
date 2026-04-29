package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.typed.spec.FactorsStepper;
import org.javai.punit.api.typed.spec.Scorer;
import org.javai.punit.examples.app.llm.ChatLlm;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.Punit;

/**
 * OPTIMIZE experiment that uses an LLM acting as a prompt engineer
 * to refine the system prompt iteratively.
 *
 * <p>This is what real prompt optimization looks like. There is no
 * scripted progression of pre-written prompts — at each iteration,
 * a meta-LLM with a "prompt engineer" persona reads the current
 * prompt and the success rate it achieved, then proposes an
 * improved version. The framework feeds that improved prompt back
 * into the next iteration's runs and the loop continues until
 * {@code maxIterations} or {@code noImprovementWindow} terminates
 * it.
 *
 * <h2>Why the meta-LLM, not a scripted list?</h2>
 *
 * <p>A hard-coded sequence of "iteration 1: add JSON-only format,
 * iteration 2: add valid action enumeration, …" is tempting in a
 * worked example because it's deterministic and easy to read. It
 * is also profoundly unrealistic: prompt engineering in practice
 * is an iterative dialogue with an LLM, not a predetermined
 * playlist. The optimizer only earns its value when the next
 * candidate is genuinely informed by the previous one's outcome.
 *
 * <p>So this example uses the LLM to drive that dialogue. The
 * stepper formats the previous prompt + score as a user message
 * and asks a meta-LLM to suggest something better. The meta-LLM's
 * output becomes the next iteration's system prompt.
 *
 * <h2>Stepper interface</h2>
 *
 * <p>The framework's {@link FactorsStepper} contract is just
 * {@code (current, history) → next}. The stepper sees per-iteration
 * scores; it does <em>not</em> currently see per-sample failure
 * detail (which inputs failed, which postconditions tripped). A
 * fully-informed prompt engineer would want failure exemplars in
 * the meta-prompt — that's a tracked typed-pipeline enhancement.
 * The score-only feedback loop still converges, just less
 * efficiently than one with failure detail would.
 *
 * <h2>API key required</h2>
 *
 * <p>Like every other LLM-backed example in punitexamples, this
 * experiment makes real LLM calls and needs the {@code ChatLlm}
 * provider to be configured ({@code OPENAI_API_KEY} env var, or
 * the equivalent for whichever provider the punitexamples app
 * resolves at runtime). Running without one fails fast at the
 * first sample. CI does not run LLM-backed examples by default;
 * they are run manually.
 *
 * <p>There's a real cost trade-off: the meta-LLM call happens
 * once per iteration, and each iteration also runs the per-sample
 * LLM calls. With {@code maxIterations=5} and
 * {@code samplesPerConfig=20}, expect ~100 LLM calls plus 5
 * meta-prompt calls.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketOptimizePrompt.optimizeSystemPrompt
 * }</pre>
 *
 * @see ShoppingBasketOptimizeTemperature ShoppingBasketOptimizeTemperature for
 *      a numeric-search optimize example (linear cool-down)
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
                // Should not happen — the framework runs the initial
                // factors first, so the stepper is only called once
                // there's at least one iteration's history. Defensive
                // null returns "no more candidates" which is fine.
                return null;
            }
            FactorsStepper.IterationResult<LlmTuning> last =
                    history.get(history.size() - 1);
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
        Punit.optimizing(ShoppingBasketUseCase.sampling(BASKET_INSTRUCTIONS, 20))
                .initialFactors(LlmTuning.DEFAULT.systemPrompt(INITIAL_PROMPT))
                .stepper(promptEngineerStepper())
                .maximize(SUCCESS_RATE)
                .maxIterations(5)
                .noImprovementWindow(2)
                .experimentId("prompt-optimization-v1")
                .run();
    }
}
