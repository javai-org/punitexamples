package org.javai.punit.examples.typed;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.javai.outcome.Outcome;
import org.javai.punit.api.CovariateCategory;
import org.javai.punit.api.typed.Sampling;
import org.javai.punit.api.typed.UseCase;
import org.javai.punit.api.typed.UseCaseOutcome;
import org.javai.punit.api.typed.covariate.Covariate;
import org.javai.punit.examples.app.llm.ChatLlm;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.app.llm.ChatResponse;
import org.javai.punit.examples.app.shopping.ShoppingAction;
import org.javai.punit.examples.app.shopping.ShoppingActionValidator;
import org.javai.punit.examples.app.shopping.ShoppingActionValidator.ValidationResult;

/**
 * Typed-API translation of natural-language shopping instructions
 * into structured actions via an LLM.
 *
 * <p>The factor record {@link Config} carries the LLM model,
 * sampling temperature, and system prompt — the parameters an
 * experiment varies. The input type is the natural-language
 * instruction. The success-output type is the validated
 * {@link ValidationResult} carrying the parsed actions; a sample
 * fails when the LLM response is unparseable, deserialises to
 * unknown actions, or carries an action whose name is invalid for
 * its declared context.
 *
 * <h2>Covariates</h2>
 *
 * <p>{@code llm_model} and {@code temperature} are declared as
 * {@link CovariateCategory#CONFIGURATION}: deliberate setup choices
 * that determine which baseline a probabilistic test should match.
 * The framework hard-gates CONFIGURATION mismatches — a baseline
 * recorded under {@code gpt-4o-mini @ 0.3} cannot silently match a
 * test running under {@code gpt-4-turbo @ 0.1}, since the two
 * configurations produce statistically distinct populations.
 *
 * <p>The legacy use case used {@link CovariateCategory#EXTERNAL_DEPENDENCY}
 * for {@code temperature} (soft match). The typed migration tightens
 * this to CONFIGURATION because temperature is an authored choice,
 * not a third-party variable.
 *
 * <h2>LLM construction</h2>
 *
 * <p>The use case takes a {@link ChatLlm} in its constructor — the
 * factory closure (see worked examples) is responsible for resolving
 * one via {@link ChatLlmProvider#resolve()}. Tests that need a
 * different LLM (e.g. for offline / mocked runs) supply their own
 * factory.
 */
public final class ShoppingBasketUseCase
        implements UseCase<ShoppingBasketUseCase.Config, String, ValidationResult> {

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
     * different {@code Config} instance to {@code Punit.testing(...)}
     * or {@code Punit.measuring(...)}; baselines are partitioned by
     * the resulting factors fingerprint.
     */
    public record Config(String model, double temperature, String systemPrompt) {

        public static final Config DEFAULT = new Config(
                DEFAULT_MODEL, DEFAULT_TEMPERATURE, DEFAULT_SYSTEM_PROMPT);

        public Config withModel(String model) {
            return new Config(model, this.temperature, this.systemPrompt);
        }

        public Config withTemperature(double temperature) {
            return new Config(this.model, temperature, this.systemPrompt);
        }
    }

    private final ChatLlm llm;
    private final Config config;

    public ShoppingBasketUseCase(ChatLlm llm, Config config) {
        this.llm = llm;
        this.config = config;
    }

    /**
     * Builds a {@link Sampling} configured to construct this use case
     * via {@link ChatLlmProvider#resolve()}. The triple
     * {@code <Config, String, ValidationResult>} is baked in here so
     * test methods don't need to spell it out.
     *
     * <p>For tests that need to inject a custom {@link ChatLlm}
     * (mocked LLMs in offline runs, alternative providers), use
     * {@link #samplingWith(ChatLlm, List, int)} instead.
     */
    public static Sampling<Config, String, ValidationResult> sampling(
            List<String> inputs, int samples) {
        return samplingWith(ChatLlmProvider.resolve(), inputs, samples);
    }

    public static Sampling<Config, String, ValidationResult> samplingWith(
            ChatLlm llm, List<String> inputs, int samples) {
        return Sampling.of(
                cfg -> new ShoppingBasketUseCase(llm, cfg),
                samples, inputs);
    }

    @Override
    public String id() {
        return "shopping-basket";
    }

    @Override
    public List<Covariate> covariates() {
        return List.of(
                Covariate.custom("llm_model", CovariateCategory.CONFIGURATION),
                Covariate.custom("temperature", CovariateCategory.CONFIGURATION));
    }

    @Override
    public Map<String, Supplier<String>> customCovariateResolvers() {
        return Map.of(
                "llm_model", () -> config.model(),
                "temperature", () -> Double.toString(config.temperature()));
    }

    @Override
    public UseCaseOutcome<ValidationResult> apply(String instruction) {
        ChatResponse response;
        try {
            response = llm.chatWithMetadata(
                    config.systemPrompt(), instruction,
                    config.model(), config.temperature());
        } catch (RuntimeException e) {
            // Network / API failures are surface-level errors that
            // belong on the Outcome channel — they're an expected
            // class of LLM behaviour, not a defect in the test.
            return UseCaseOutcome.<ValidationResult>fail(
                    "llm-error", e.getMessage())
                    .withTokens(0);
        }
        if (response.content() == null || response.content().isBlank()) {
            return UseCaseOutcome.<ValidationResult>fail(
                    "empty-response", "LLM returned no content")
                    .withTokens(response.totalTokens());
        }
        Outcome<ValidationResult> validated = ShoppingActionValidator.validate(response);
        if (validated instanceof Outcome.Fail<ValidationResult> failure) {
            return UseCaseOutcome.<ValidationResult>fail(
                    failure.failure().id().name(),
                    failure.failure().message())
                    .withTokens(response.totalTokens());
        }
        ValidationResult result = validated.getOrThrow();
        for (ShoppingAction action : result.actions()) {
            if (!action.context().isValidAction(action.name())) {
                return UseCaseOutcome.<ValidationResult>fail(
                        "invalid-action",
                        "Invalid action '%s' for context %s"
                                .formatted(action.name(), action.context()))
                        .withTokens(response.totalTokens());
            }
        }
        return UseCaseOutcome.ok(result).withTokens(response.totalTokens());
    }
}
