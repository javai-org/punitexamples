package org.javai.punit.examples.usecases;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import org.javai.outcome.Outcome;
import org.javai.punit.api.Covariate;
import org.javai.punit.api.CovariateCategory;
import org.javai.punit.api.CovariateSource;
import org.javai.punit.api.DayGroup;
import org.javai.punit.api.UseCase;
import org.javai.punit.contract.ServiceContract;
import org.javai.punit.contract.UseCaseOutcome;
import org.javai.punit.contract.match.JsonMatcher;
import org.javai.punit.examples.app.llm.ChatLlm;
import org.javai.punit.examples.app.llm.ChatLlmException;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.app.llm.ChatResponse;
import org.javai.punit.examples.app.shopping.ShoppingAction;
import org.javai.punit.examples.app.shopping.ShoppingActionValidator;
import org.jspecify.annotations.NonNull;

/**
 * Use case for translating natural-language shopping instructions
 * into structured actions via an LLM. Used by the empirical-baseline
 * probabilistic-testing examples.
 *
 * <h2>Domain</h2>
 * <p>A user provides natural language instructions like "Add 2 apples" or "Clear the basket",
 * and an LLM translates these into structured {@link ShoppingAction} objects that can be
 * executed against a shopping basket API.
 *
 * <h2>JSON Format</h2>
 * <pre>{@code
 * {
 *   "context": "SHOP",
 *   "name": "add",
 *   "parameters": [
 *     {"name": "item", "value": "apple"},
 *     {"name": "quantity", "value": "2"}
 *   ]
 * }
 * }</pre>
 *
 * <h2>Success Criteria</h2>
 * <ol>
 *   <li>Valid JSON (parseable)</li>
 *   <li>Deserializes to a valid {@link ShoppingAction}</li>
 *   <li>Action name is valid for the given context</li>
 * </ol>
 *
 * <h2>Covariates</h2>
 * <p>This use case tracks covariates that may affect LLM behavior:
 * <ul>
 *   <li>{@code day_of_week} - Day-of-week partitioning (weekend vs weekday)</li>
 *   <li>{@code time_of_day} - Time-of-day partitioning</li>
 *   <li>{@code llm_model} - Which model is being used (CONFIGURATION)</li>
 *   <li>{@code temperature} - Temperature setting (CONFIGURATION)</li>
 * </ul>
 *
 * @see ShoppingAction
 * @see ShoppingActionValidator
 */
@UseCase(
        description = "Translate natural language shopping instructions to structured actions",
        covariateDayOfWeek = {@DayGroup({SATURDAY, SUNDAY})},
        covariateTimeOfDay = {"08:00/4h", "16:00/4h"},
        covariates = {
                @Covariate(key = "llm_model", category = CovariateCategory.EXTERNAL_DEPENDENCY),
                @Covariate(key = "temperature", category = CovariateCategory.EXTERNAL_DEPENDENCY)
        }
)
public class ShoppingBasketUseCase {

    /**
     * Input parameters for the translation service.
     */
    private record ServiceInput(String systemPrompt, String instruction, String model, double temperature) {}

    /**
     * The service contract defining postconditions for translation results.
     */
    private static final ServiceContract<ServiceInput, ChatResponse> CONTRACT =
            ServiceContract.<ServiceInput, ChatResponse>define()
                    .ensure("Response has content", ShoppingBasketUseCase::hasContent)
                    .derive("Valid shopping action", ShoppingActionValidator::validate)
                    .ensure("Contains valid actions", ShoppingBasketUseCase::hasValidAction)
                    .build();

    private static @NonNull Outcome<Void> hasContent(ChatResponse response) {
        return response.content() != null && !response.content().isBlank()
                ? Outcome.ok()
                : Outcome.fail("check", "content was null or blank");
    }

    private static @NonNull Outcome<Void> hasValidAction(ShoppingActionValidator.BasketTranslation result) {
        if (result.actions().isEmpty()) {
            return Outcome.fail("check", "No actions in result");
        }
        for (ShoppingAction action : result.actions()) {
            if (!action.context().isValidAction(action.name())) {
                return Outcome.fail("check",
                        "Invalid action '%s' for context %s"
                                .formatted(action.name(), action.context()));
            }
        }
        return Outcome.ok();
    }

    /**
     * Default system prompt for shopping basket instruction translation.
     */
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

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final double DEFAULT_TEMPERATURE = 0.3;

    private final ChatLlm llm;
    private final String model;
    private final double temperature;
    private final String systemPrompt;

    /**
     * Creates a use case with the LLM resolved from configuration and default settings.
     *
     * @see ChatLlmProvider#resolve()
     */
    public ShoppingBasketUseCase() {
        this(ChatLlmProvider.resolve());
    }

    /**
     * Creates a use case with a specific LLM implementation and default settings.
     *
     * @param llm the chat LLM to use
     */
    public ShoppingBasketUseCase(ChatLlm llm) {
        this(llm, DEFAULT_MODEL, DEFAULT_TEMPERATURE, DEFAULT_SYSTEM_PROMPT);
    }

    /**
     * Creates a fully configured use case.
     *
     * @param llm the chat LLM to use
     * @param model the LLM model identifier
     * @param temperature the sampling temperature
     * @param systemPrompt the system prompt for the LLM
     */
    public ShoppingBasketUseCase(ChatLlm llm, String model, double temperature, String systemPrompt) {
        this.llm = llm;
        this.model = model;
        this.temperature = temperature;
        this.systemPrompt = systemPrompt;
    }

    // ===============================================================================
    // ACCESSORS AND COVARIATE SOURCES
    // ===============================================================================

    @CovariateSource("llm_model")
    public String getModel() {
        return model;
    }

    @CovariateSource
    public double getTemperature() {
        return temperature;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Returns the cumulative tokens used by the underlying LLM since the last reset.
     *
     * @return total tokens across all calls
     */
    public long getTotalTokensUsed() {
        return llm.getTotalTokensUsed();
    }

    /**
     * Resets the cumulative token counter in the underlying LLM.
     */
    public void resetTokenCount() {
        llm.resetTokenCount();
    }

    // ===============================================================================
    // USE CASE METHODS
    // ===============================================================================

    /**
     * Translates a natural language shopping instruction to a structured action.
     *
     * @param instruction the natural language instruction (e.g., "Add 2 apples")
     * @return outcome containing the result and postcondition evaluations
     */
    public UseCaseOutcome<ChatResponse> translateInstruction(String instruction) {
        return translateInstructionCore(instruction, null);
    }

    /**
     * Translates a natural language shopping instruction to a structured action,
     * with instance conformance checking against an expected JSON result.
     *
     * <p>This method extends {@link #translateInstruction(String)} by also comparing
     * the actual LLM response against the expected JSON. The comparison is semantic,
     * meaning JSON property order and whitespace differences are ignored.
     *
     * <p>Use {@link UseCaseOutcome#fullySatisfied()} to check both behavioral conformance
     * (postconditions) and instance conformance (expected value match).
     *
     * @param instruction the natural language instruction (e.g., "Add 2 apples")
     * @param expectedJson the expected JSON response for instance conformance checking
     * @return outcome containing the result, postcondition evaluations, and match result
     */
    public UseCaseOutcome<ChatResponse> translateInstruction(String instruction, String expectedJson) {
        return translateInstructionCore(instruction, expectedJson);
    }

    private UseCaseOutcome<ChatResponse> translateInstructionCore(String instruction, String expectedJson) {
        var builder = UseCaseOutcome
                .withContract(CONTRACT)
                .input(new ServiceInput(systemPrompt, instruction, model, temperature))
                .execute(this::executeTranslation)
                .withResult((response, meta) -> meta
                        .meta("tokensUsed", response.totalTokens())
                        .meta("promptTokens", response.promptTokens())
                        .meta("completionTokens", response.completionTokens()));

        if (expectedJson != null) {
            builder.expecting(expectedJson, ChatResponse::content, JsonMatcher.create());
        }

        return builder
                .meta("instruction", instruction)
                .meta("model", model)
                .meta("temperature", temperature)
                .build();
    }

    private ChatResponse executeTranslation(ServiceInput input) {
        try {
            return llm.chatWithMetadata(
                    input.systemPrompt(),
                    input.instruction(),
                    input.model(),
                    input.temperature()
            );
        } catch (ChatLlmException e) {
            // Legacy ServiceContract.execute(...) takes a non-throwing
            // Function<I, R>, so this boundary cannot propagate a checked
            // exception. Rewrap with the cause attached; the legacy
            // framework's exception-handling treats this as a sample
            // failure. The typed-API path (typed.ShoppingBasketUseCase)
            // catches ChatLlmException directly and returns Outcome.fail.
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }
}
