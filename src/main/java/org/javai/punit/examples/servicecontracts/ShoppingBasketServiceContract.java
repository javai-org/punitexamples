package org.javai.punit.examples.servicecontracts;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.javai.outcome.Outcome;
import org.javai.punit.api.ContractBuilder;
import org.javai.punit.api.Pacing;
import org.javai.punit.api.Sampling;
import org.javai.punit.api.ServiceContract;
import org.javai.punit.api.TokenTracker;
import org.javai.punit.api.covariate.Covariate;
import org.javai.punit.api.covariate.CovariateCategory;
import org.javai.punit.api.criterion.CriteriaBuilder;
import org.javai.punit.api.criterion.Criteria;
import org.javai.punit.examples.app.llm.ChatLlm;
import org.javai.punit.examples.app.llm.ChatLlmException;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.app.llm.ChatResponse;
import org.javai.punit.examples.app.shopping.ShoppingAction;
import org.javai.punit.examples.app.shopping.ShoppingActionParameter;
import org.javai.punit.examples.app.shopping.ShoppingActionValidator;
import org.javai.punit.examples.app.shopping.ShoppingActionValidator.BasketTranslation;

/**
 * Translation of natural-language shopping instructions into
 * structured actions via an LLM.
 *
 * <p>The factor record {@link LlmTuning} carries the LLM model,
 * sampling temperature, and system prompt. The input type is the
 * natural-language instruction; the output type is the LLM's raw
 * response string. The contract — declared via
 * {@link #criteria(CriteriaBuilder) criteria} — judges the response
 * through two criteria: a direct {@code response-not-empty} criterion
 * that checks the LLM returned content, and a transforming
 * {@code valid-json} criterion that parses the response into a
 * {@link BasketTranslation} and (on successful parse) checks every
 * action's name is valid for its declared context and that any
 * {@code quantity} parameter is a positive integer. A parse failure
 * classifies the sample INCONCLUSIVE for the JSON criterion (the
 * postcondition chain is not evaluated); the engine surfaces the
 * parse failure on the per-sample record with its symbolic name and
 * message preserved.
 *
 * <p>{@code llm_model} and {@code temperature} are declared as
 * {@link CovariateCategory#CONFIGURATION} covariates. The framework
 * hard-gates CONFIGURATION mismatches — a baseline recorded under
 * {@code gpt-4o-mini @ 0.3} cannot silently match a test running
 * under {@code gpt-4-turbo @ 0.1}.
 *
 * <p>The service contract takes a {@link ChatLlm} in its constructor; the
 * factory closure resolves one via {@link ChatLlmProvider#resolve()}
 * by default. Tests that need a different LLM supply their own
 * factory through {@link #samplingWith(ChatLlm, List, int)}.
 */
public final class ShoppingBasketServiceContract
		implements ServiceContract<ShoppingBasketServiceContract.LlmTuning, String, String> {

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
			Quantities must be positive integers (>= 1).
			
			Examples:
			- "Add 2 apples" -> {"actions": [{"context": "SHOP", "name": "add", "parameters": [{"name": "item", "value": "apples"}, {"name": "quantity", "value": "2"}]}]}
			- "Add apples and remove milk" -> {"actions": [{"context": "SHOP", "name": "add", "parameters": [{"name": "item", "value": "apples"}, {"name": "quantity", "value": "1"}]}, {"context": "SHOP", "name": "remove", "parameters": [{"name": "item", "value": "milk"}]}]}
			- "Clear the basket" -> {"actions": [{"context": "SHOP", "name": "clear", "parameters": []}]}
			""";
	/**
	 * Canonical input list shared by the measure experiment and every
	 * probabilistic test for this service contract. Keeping the list here —
	 * not in any single test or experiment class — closes the drift
	 * vector that produces silent INCONCLUSIVE / SKIPPED verdicts when
	 * the measure-side and test-side {@code inputsIdentity} hashes
	 * don't match.
	 *
	 * <p>EXPLORE and OPTIMIZE experiments may legitimately need their
	 * own input lists (sweeping over different scenarios is the point);
	 * those use the list-taking factory overloads
	 * ({@link #sampling(List, int)} etc.) directly.
	 */
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
	private final ChatLlm llm;
	private final LlmTuning tuning;
	private final Pacing pacing;
	public ShoppingBasketServiceContract(ChatLlm llm, LlmTuning tuning) {
		this(llm, tuning, Pacing.unlimited());
	}

	public ShoppingBasketServiceContract(ChatLlm llm, LlmTuning tuning, Pacing pacing) {
		this.llm = llm;
		this.tuning = tuning;
		this.pacing = pacing;
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

	private static Outcome<Void> checkQuantitiesArePositiveIntegers(BasketTranslation translation) {
		for (ShoppingAction action : translation.actions()) {
			for (ShoppingActionParameter param : action.parameters()) {
				if (!"quantity".equals(param.name())) {
					continue;
				}
				int quantity;
				try {
					quantity = param.valueAsInt();
				} catch (NumberFormatException e) {
					return Outcome.fail(
							"non-integer-quantity",
							"Quantity '%s' is not an integer".formatted(param.value()));
				}
				if (quantity < 1) {
					return Outcome.fail(
							"non-positive-quantity",
							"Quantity %d is not a positive integer".formatted(quantity));
				}
			}
		}
		return Outcome.ok();
	}

	/**
	 * Canonical-inputs factory: builds a {@link Sampling} over
	 * {@link #BASKET_INSTRUCTIONS} with the {@link ChatLlm} resolved
	 * via {@link ChatLlmProvider#resolve()}. This is the factory
	 * MEASURE experiments and probabilistic tests should use — sharing
	 * one input source on both sides makes the empirical-pair's
	 * inputs-identity match structural rather than coincidental.
	 */
	public static Sampling<LlmTuning, String, String> sampling(int samples) {
		return sampling(BASKET_INSTRUCTIONS, samples);
	}

	/**
	 * Custom-inputs overload for EXPLORE and OPTIMIZE experiments that
	 * legitimately sweep over different input lists. Probabilistic
	 * tests should not call this overload; use {@link #sampling(int)}.
	 */
	public static Sampling<LlmTuning, String, String> sampling(
			List<String> inputs, int samples) {
		return samplingWith(ChatLlmProvider.resolve(), inputs, samples);
	}

	/** Canonical-inputs variant of {@link #samplingWith(ChatLlm, List, int)}. */
	public static Sampling<LlmTuning, String, String> samplingWith(
			ChatLlm llm, int samples) {
		return samplingWith(llm, BASKET_INSTRUCTIONS, samples);
	}

	public static Sampling<LlmTuning, String, String> samplingWith(
			ChatLlm llm, List<String> inputs, int samples) {
		return Sampling.of(
				tuning -> new ShoppingBasketServiceContract(llm, tuning),
				samples, inputs);
	}

	/** Canonical-inputs variant of {@link #samplingPaced(Pacing, List, int)}. */
	public static Sampling<LlmTuning, String, String> samplingPaced(
			Pacing pacing, int samples) {
		return samplingPaced(pacing, BASKET_INSTRUCTIONS, samples);
	}

	/**
	 * Sampling whose constructed service contract respects the supplied
	 * {@link Pacing}.
	 */
	public static Sampling<LlmTuning, String, String> samplingPaced(
			Pacing pacing, List<String> inputs, int samples) {
		return Sampling.of(
				tuning -> new ShoppingBasketServiceContract(
						ChatLlmProvider.resolve(), tuning, pacing),
				samples, inputs);
	}

	/** Canonical-inputs variant of {@link #samplingBuilder(List, int)}. */
	public static Sampling.Builder<LlmTuning, String, String> samplingBuilder(int samples) {
		return samplingBuilder(BASKET_INSTRUCTIONS, samples);
	}

	/**
	 * Builder form for tests that need to configure budgets, exception
	 * policy, or other Sampling knobs not exposed on the simpler
	 * {@link #sampling(int)} factory. Returns a partially-built
	 * Sampling.Builder ready for {@code .timeBudget(...)},
	 * {@code .tokenBudget(...)}, etc., terminated with {@code .build()}.
	 */
	public static Sampling.Builder<LlmTuning, String, String> samplingBuilder(
			List<String> inputs, int samples) {
		return Sampling.<LlmTuning, String, String>builder()
				.serviceContractFactory(tuning -> new ShoppingBasketServiceContract(
						ChatLlmProvider.resolve(), tuning))
				.inputs(inputs)
				.samples(samples);
	}

	/**
	 * No contract clauses live at the postcondition level — the
	 * contract's two clauses are declared as criteria via
	 * {@link #criteria(CriteriaBuilder)}. The framework's coexistence
	 * rule rejects a contract that overrides both surfaces with
	 * non-empty content, so this body must remain empty.
	 */
	@Override
	public void postconditions(ContractBuilder<String> b) {
		// Intentionally empty — see criteria(CriteriaBuilder) below.
	}

	/**
	 * Declares the contract's two criteria.
	 *
	 * <p>{@code response-not-empty} is a direct criterion: it
	 * evaluates against the LLM's raw response string. Its only
	 * postcondition checks the response is non-blank.
	 *
	 * <p>{@code valid-json} is a transforming criterion: it parses
	 * the response into a {@link BasketTranslation} via
	 * {@link ShoppingActionValidator#parse} and, on a successful
	 * parse, evaluates two postconditions over the translation — that
	 * every action's name is valid for its declared context, and that
	 * any {@code quantity} parameter is a positive integer. A parse
	 * failure classifies the sample INCONCLUSIVE for this criterion;
	 * the engine surfaces the parse Failure on the per-sample record.
	 */
	@Override
	public void criteria(CriteriaBuilder<String> b) {
		b.add(Criteria.direct("response-not-empty",
				cb -> cb.ensure("Response not empty",
						ShoppingBasketServiceContract::checkResponseNotEmpty)));
		b.add(Criteria.transforming("valid-json",
				ShoppingActionValidator::parse,
				cb -> cb.ensure("All actions valid for context",
								ShoppingBasketServiceContract::checkActionsValidForContext)
						.ensure("Quantities are positive integers",
								ShoppingBasketServiceContract::checkQuantitiesArePositiveIntegers)));
	}

	/**
	 * Declares the factors that influence outcomes — here the LLM
	 * model and the sampling temperature. Resolved values stamp the
	 * baseline's identity, so a test under one configuration never
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
	 * the service contract's tuning. Called once per run; the resolved
	 * value flows into the baseline's identity.
	 */
	@Override
	public Map<String, Supplier<String>> customCovariateResolvers() {
		return Map.of(
				"llm_model", tuning::model,
				"temperature", () -> Double.toString(tuning.temperature()));
	}

	/**
	 * Surfaces the constructor-injected pacing so different test
	 * setups can exercise the same service contract under different
	 * rate-limit and concurrency shapes.
	 */
	@Override
	public Pacing pacing() {
		return pacing;
	}

	/**
	 * Stable identifier used in baseline filenames and reports.
	 * The default would otherwise be this usecase's class name.
	 * Pinning it explicitly insulates the baseline-file identity
	 * from any future class-name refactor.
	 */
	@Override
	public String id() {
		return "shopping-basket";
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
					tuning.model(), tuning.temperature()
			);
			tracker.recordTokens(response.totalTokens());
			return Outcome.ok(response.content());
		} catch (ChatLlmException e) {
			return Outcome.fail("llm-error", e.getMessage());
		}
	}

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
}
