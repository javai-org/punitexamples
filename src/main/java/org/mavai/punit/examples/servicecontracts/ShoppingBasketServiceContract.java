package org.mavai.punit.examples.servicecontracts;

import static org.mavai.punit.api.criterion.Criteria.empirical;
import static org.mavai.punit.api.criterion.Criteria.of;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.mavai.outcome.Outcome;
import org.mavai.punit.api.Pacing;
import org.mavai.punit.api.Sampling;
import org.mavai.punit.api.ServiceContract;
import org.mavai.punit.api.TokenTracker;
import org.mavai.punit.api.covariate.Covariate;
import org.mavai.punit.api.covariate.CovariateCategory;
import org.mavai.punit.api.criterion.Criteria;
import org.mavai.punit.examples.app.shopping.ShoppingAction;
import org.mavai.punit.examples.app.shopping.ShoppingActionParameter;
import org.mavai.punit.examples.app.shopping.ShoppingActionValidator;
import org.mavai.punit.examples.app.shopping.ShoppingActionValidator.BasketTranslation;
import org.mavai.punit.examples.lm.LanguageModelMode;
import org.mavai.punit.lm.api.LanguageModel;

/**
 * Translation of natural-language shopping instructions into
 * structured actions via an LLM.
 *
 * <p>The factor record {@link LlmTuning} carries the LLM model,
 * sampling temperature, and system prompt. The input type is the
 * natural-language instruction; the output type is the LLM's raw
 * response string. The contract — declared via
 * {@link #criteria()} — judges the response
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
// mavai-ref: JVI-6P9FH4F — do not remove (resolves in mavai-orchestrator)
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
	private final LanguageModel llm;
	private final LlmTuning tuning;
	private final Pacing pacing;

	/** A contract over the model the resolved mode gives this tuning (mock by default). */
	public ShoppingBasketServiceContract(LlmTuning tuning) {
		this(modelFor(tuning), tuning, Pacing.unlimited());
	}

	public ShoppingBasketServiceContract(LanguageModel llm, LlmTuning tuning) {
		this(llm, tuning, Pacing.unlimited());
	}

	public ShoppingBasketServiceContract(LanguageModel llm, LlmTuning tuning, Pacing pacing) {
		this.llm = llm;
		this.tuning = tuning;
		this.pacing = pacing;
	}

	/** The model the resolved mode gives a tuning: the examples' mock, or punit-lm over a real provider. */
	public static LanguageModel modelFor(LlmTuning tuning) {
		return LanguageModelMode.resolve(tuning.model(), tuning.temperature(), tuning.systemPrompt());
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
	 * {@link #BASKET_INSTRUCTIONS} with the {@link LanguageModel}
	 * resolved per factor bundle via {@link LanguageModelMode}. This is
	 * the factory MEASURE experiments and probabilistic tests should use
	 * — sharing one input source on both sides makes the empirical-pair's
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
		return samplingWith(ShoppingBasketServiceContract::modelFor, inputs, samples);
	}

	/** Canonical-inputs variant of {@link #samplingWith(Function, List, int)}. */
	public static Sampling<LlmTuning, String, String> samplingWith(
			Function<LlmTuning, LanguageModel> models, int samples) {
		return samplingWith(models, BASKET_INSTRUCTIONS, samples);
	}

	/**
	 * Sampling over a caller-supplied model source — one
	 * {@link LanguageModel} per factor bundle, since a configured model
	 * carries its model name, temperature and system prompt.
	 */
	public static Sampling<LlmTuning, String, String> samplingWith(
			Function<LlmTuning, LanguageModel> models, List<String> inputs, int samples) {
		return Sampling.of(
				tuning -> new ShoppingBasketServiceContract(models.apply(tuning), tuning),
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
						modelFor(tuning), tuning, pacing),
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
				.serviceContractFactory(ShoppingBasketServiceContract::new)
				.inputs(inputs)
				.samples(samples);
	}

	/**
	 * Declares the contract's two criteria.
	 *
	 * <p>{@code response-not-empty} is a direct criterion: its single
	 * postcondition checks the LLM's raw response string is non-blank.
	 *
	 * <p>{@code valid-json} is a transforming criterion: it parses the
	 * response into a {@link BasketTranslation} via
	 * {@link ShoppingActionValidator#parse} and, on a successful parse,
	 * evaluates two postconditions over the translation — that every
	 * action's name is valid for its declared context, and that any
	 * {@code quantity} parameter is a positive integer. A parse failure
	 * classifies the sample INCONCLUSIVE for this criterion; the engine
	 * surfaces the parse Failure on the per-sample record.
	 */
	@Override
	public Criteria<String> criteria() {
		return of(
				empirical().<String>passRate()
						.name("response-not-empty")
						.satisfies("Response not empty",
								ShoppingBasketServiceContract::checkResponseNotEmpty),
				empirical().<String>passRate()
						.transforming(ShoppingActionValidator::parse)
						.satisfies("All actions valid for context",
								ShoppingBasketServiceContract::checkActionsValidForContext)
						.satisfies("Quantities are positive integers",
								ShoppingBasketServiceContract::checkQuantitiesArePositiveIntegers)
						.name("valid-json"));
	}

	/**
	 * Declares the factors that influence outcomes: every configuration
	 * value the language model states about itself (provider, model,
	 * temperature, system prompt, …), exactly as a declarative
	 * {@code type: language-model} service would. Resolved values stamp
	 * the baseline's identity, so a test under one configuration never
	 * silently matches a baseline measured under another — and a mock
	 * run never matches a real one.
	 */
	@Override
	public List<Covariate> covariates() {
		List<Covariate> covariates = new ArrayList<>();
		for (String key : llm.configurationCovariates().keySet()) {
			covariates.add(Covariate.custom(key, CovariateCategory.CONFIGURATION));
		}
		return List.copyOf(covariates);
	}

	/**
	 * Resolves each custom covariate at run time from the model's own
	 * statement of its configuration. Called once per run; the resolved
	 * value flows into the baseline's identity.
	 */
	@Override
	public Map<String, Supplier<String>> customCovariateResolvers() {
		Map<String, Supplier<String>> resolvers = new LinkedHashMap<>();
		llm.configurationCovariates().forEach((key, value) -> resolvers.put(key, () -> value));
		return resolvers;
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
	 * The service call. Invokes the model once, records the token usage
	 * the reply states via the tracker, and returns the reply's text
	 * wrapped in {@link Outcome#ok}. A failed delivery (an unreachable
	 * endpoint, a server error, a client deadline, an off-shape reply)
	 * comes back from punit-lm as an {@link Outcome} failure carrying its
	 * delivery cause, and passes through unchanged so the engine counts
	 * it as a sample failure under the family's own vocabulary. A
	 * provider <em>rejection</em> (a bad credential, an unknown model)
	 * throws instead — that is a defect, and the run should abort so the
	 * author can fix it. The contract's criteria — declared in
	 * {@link #criteria()} — judge the returned response shape.
	 */
	@Override
	public Outcome<String> invoke(String instruction, TokenTracker tracker) {
		return llm.invoke(instruction).map(reply -> {
			reply.usage().ifPresent(usage -> tracker.recordTokens(usage.totalTokens()));
			return reply.text();
		});
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
