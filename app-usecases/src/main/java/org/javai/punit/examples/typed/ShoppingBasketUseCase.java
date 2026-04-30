package org.javai.punit.examples.typed;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.javai.outcome.Outcome;
import org.javai.punit.api.CovariateCategory;
import org.javai.punit.api.typed.ContractBuilder;
import org.javai.punit.api.typed.Pacing;
import org.javai.punit.api.typed.Sampling;
import org.javai.punit.api.typed.TokenTracker;
import org.javai.punit.api.typed.UseCase;
import org.javai.punit.api.typed.covariate.Covariate;
import org.javai.punit.examples.app.llm.ChatLlm;
import org.javai.punit.examples.app.llm.ChatLlmException;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.app.llm.ChatResponse;
import org.javai.punit.examples.app.shopping.ShoppingAction;
import org.javai.punit.examples.app.shopping.ShoppingActionValidator;
import org.javai.punit.examples.app.shopping.ShoppingActionValidator.BasketTranslation;

/**
 * Typed-API translation of natural-language shopping instructions
 * into structured actions via an LLM.
 *
 * <p>The factor record {@link LlmTuning} carries the LLM model,
 * sampling temperature, and system prompt. The input type is the
 * natural-language instruction; the output type is the LLM's raw
 * response string. The contract — declared via
 * {@link #postconditions(ContractBuilder) postconditions} — judges
 * the response: a non-empty body clause, then a {@code deriving}
 * step that parses the JSON into a {@link BasketTranslation} and
 * (on successful parse) checks every action's name is valid for
 * its declared context. The histogram on
 * {@code SampleSummary.failuresByPostcondition()} surfaces each
 * failure mode separately, so the optimize meta-prompt can see
 * "Valid JSON failed 8 times" vs "All actions valid failed 3 times"
 * rather than collapsing both to "the use case failed."
 *
 * <p>{@code llm_model} and {@code temperature} are declared as
 * {@link CovariateCategory#CONFIGURATION} covariates. The framework
 * hard-gates CONFIGURATION mismatches — a baseline recorded under
 * {@code gpt-4o-mini @ 0.3} cannot silently match a test running
 * under {@code gpt-4-turbo @ 0.1}.
 *
 * <p>The use case takes a {@link ChatLlm} in its constructor; the
 * factory closure resolves one via {@link ChatLlmProvider#resolve()}
 * by default. Tests that need a different LLM supply their own
 * factory through {@link #samplingWith(ChatLlm, List, int)}.
 */
public final class ShoppingBasketUseCase
        implements UseCase<ShoppingBasketUseCase.LlmTuning, String, String> {

    public static final String DEFAULT_MODEL = "gpt-4o-mini";
    public static final double DEFAULT_TEMPERATURE = 0.3;
    public static final String DEFAULT_SYSTEM_PROMPT = """
            You are a shopping assistant that converts natural language instructions into JSON actions.

            Respond with JSON only — no explanation or commentary. The JSON must contain an "actions" array, even for single operations.

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

            Valid actions for SHOP context: "add", "remove", "clear"
            For "clear" actions, parameters may be empty.

            Examples:
            - "Add 2 apples" -> {"actions": [{"context": "SHOP", "name": "add", "parameters": [{"name": "item", "value": "apples"}, {"name": "quantity", "value": "2"}]}]}
            - "Add apples and remove milk" -> {"actions": [{"context": "SHOP", "name": "add", "parameters": [{"name": "item", "value": "apples"}, {"name": "quantity", "value": "1"}]}, {"context": "SHOP", "name": "remove", "parameters": [{"name": "item", "value": "milk"}]}]}
            - "Clear the basket" -> {"actions": [{"context": "SHOP", "name": "clear", "parameters": []}]}
            """;

    /**
     * The factor record. Tests vary configuration by passing a
     * different {@code LlmTuning} instance to {@code PUnit.testing(...)}
     * or {@code PUnit.measuring(...)}.
     */
    public record LlmTuning(String model, double temperature, String systemPrompt) {

        public static final LlmTuning DEFAULT = new LlmTuning(
                DEFAULT_MODEL, DEFAULT_TEMPERATURE, DEFAULT_SYSTEM_PROMPT);

        public LlmTuning model(String model) {
            return new LlmTuning(model, this.temperature, this.systemPrompt);
        }

        public LlmTuning temperature(double temperature) {
            return new LlmTuning(this.model, temperature, this.systemPrompt);
        }

        public LlmTuning systemPrompt(String systemPrompt) {
            return new LlmTuning(this.model, this.temperature, systemPrompt);
        }
    }

    private final ChatLlm llm;
    private final LlmTuning tuning;
    private final Pacing pacing;

    public ShoppingBasketUseCase(ChatLlm llm, LlmTuning tuning) {
        this(llm, tuning, Pacing.unlimited());
    }

    public ShoppingBasketUseCase(ChatLlm llm, LlmTuning tuning, Pacing pacing) {
        this.llm = llm;
        this.tuning = tuning;
        this.pacing = pacing;
    }

    /**
     * Surfaces the constructor-injected pacing so different test
     * setups can exercise the same use case under different
     * rate-limit and concurrency shapes.
     */
    @Override
    public Pacing pacing() {
        return pacing;
    }

    /**
     * Builds a {@link Sampling} configured to construct this use case
     * with a {@link ChatLlm} resolved via
     * {@link ChatLlmProvider#resolve()}. For tests that need to
     * inject a different {@link ChatLlm}, use
     * {@link #samplingWith(ChatLlm, List, int)} instead.
     */
    public static Sampling<LlmTuning, String, String> sampling(
            List<String> inputs, int samples) {
        return samplingWith(ChatLlmProvider.resolve(), inputs, samples);
    }

    public static Sampling<LlmTuning, String, String> samplingWith(
            ChatLlm llm, List<String> inputs, int samples) {
        return Sampling.of(
                tuning -> new ShoppingBasketUseCase(llm, tuning),
                samples, inputs);
    }

    /**
     * Sampling whose constructed use case respects the supplied
     * {@link Pacing}.
     */
    public static Sampling<LlmTuning, String, String> samplingPaced(
            Pacing pacing, List<String> inputs, int samples) {
        return Sampling.of(
                tuning -> new ShoppingBasketUseCase(
                        ChatLlmProvider.resolve(), tuning, pacing),
                samples, inputs);
    }

    /**
     * Builder form for tests that need to configure budgets, exception
     * policy, or other Sampling knobs not exposed on the simpler
     * {@link #sampling(List, int)} factory. Returns a partially-built
     * Sampling.Builder ready for {@code .timeBudget(...)},
     * {@code .tokenBudget(...)}, etc., terminated with {@code .build()}.
     */
    public static Sampling.Builder<LlmTuning, String, String> samplingBuilder(
            List<String> inputs, int samples) {
        return Sampling.<LlmTuning, String, String>builder()
                .useCaseFactory(tuning -> new ShoppingBasketUseCase(
                        ChatLlmProvider.resolve(), tuning))
                .inputs(inputs)
                .samples(samples);
    }

    /**
     * Stable identifier used in baseline filenames and reports.
     * The default would already produce {@code "shopping-basket"};
     * pinning it explicitly insulates the baseline-file identity
     * from any future class-name refactor.
     */
    @Override
    public String id() {
        return "shopping-basket";
    }

    /**
     * Declares the contract — what counts as a valid LLM response.
     * Two clauses: a leaf check that the response has content, and
     * a {@code deriving} step that parses the JSON and runs a
     * nested clause against the parsed value. The framework
     * evaluates each clause per sample and surfaces the per-clause
     * failures in {@code SampleSummary.failuresByPostcondition()}.
     */
    @Override
    public void postconditions(ContractBuilder<String> b) {
        b.ensure("Response not empty", ShoppingBasketUseCase::checkResponseNotEmpty);
        b.deriving("Valid JSON",
                ShoppingActionValidator::parse,
                sub -> sub.ensure("All actions valid for context",
                        ShoppingBasketUseCase::checkActionsValidForContext));
    }

    private static Outcome<Void> checkResponseNotEmpty(String response) {
        return (response == null || response.isBlank())
                ? Outcome.fail("empty-response", "LLM returned no content")
                : Outcome.ok();
    }

    private static Outcome<Void> checkActionsValidForContext(BasketTranslation translation) {
        for (ShoppingAction action : translation.actions()) {
            if (!action.context().isValidAction(action.name())) {
                return Outcome.fail(
                        "invalid-action",
                        "Invalid action '%s' for context %s"
                                .formatted(action.name(), action.context()));
            }
        }
        return Outcome.ok();
    }

    /**
     * Declares the factors that influence outcomes — here the LLM
     * model and the sampling temperature. Resolved values stamp the
     * baseline's identity so a test under one configuration never
     * silently matches a baseline measured under another.
     */
    @Override
    public List<Covariate> covariates() {
        return List.of(
                Covariate.custom("llm_model", CovariateCategory.CONFIGURATION),
                Covariate.custom("temperature", CovariateCategory.CONFIGURATION));
    }

    /**
     * Resolves each custom covariate at run time by reading from
     * the use case's tuning. Called once per run; the resolved
     * value flows into the baseline's identity.
     */
    @Override
    public Map<String, Supplier<String>> customCovariateResolvers() {
        return Map.of(
                "llm_model", () -> tuning.model(),
                "temperature", () -> Double.toString(tuning.temperature()));
    }

    /**
     * The service call. Hits the LLM, records token cost via the
     * tracker, returns the raw response wrapped in {@link Outcome#ok}.
     * The catch clause is narrow: {@link ChatLlmException} models the
     * LLM client's anticipated transport-level failures (HTTP errors,
     * timeouts, malformed responses) — those are translated to
     * {@link Outcome#fail} under the symbolic name {@code "llm-error"}
     * so the engine counts them as sample failures. Anything else the
     * client might throw (an unchecked exception from a logic bug,
     * misconfiguration) is left to bubble — that is a defect, and the
     * run should abort so the author can fix it. The contract —
     * declared in {@link #postconditions(ContractBuilder) postconditions} —
     * judges the returned response shape.
     */
    @Override
    public Outcome<String> invoke(String instruction, TokenTracker tracker) {
        try {
            ChatResponse response = llm.chatWithMetadata(
                    tuning.systemPrompt(), instruction,
                    tuning.model(), tuning.temperature());
            tracker.recordTokens(response.totalTokens());
            return Outcome.ok(response.content());
        } catch (ChatLlmException e) {
            return Outcome.fail("llm-error", e.getMessage());
        }
    }
}
