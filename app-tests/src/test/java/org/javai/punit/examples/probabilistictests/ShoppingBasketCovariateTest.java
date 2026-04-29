package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.Punit;

/**
 * Demonstrates covariate-aware baseline matching with different LLM
 * configurations.
 *
 * <p>{@link ShoppingBasketUseCase} declares {@code llm_model} and
 * {@code temperature} as {@code CONFIGURATION}-category covariates.
 * The framework's hard-gating rule means a baseline measured under
 * one configuration ({@code gpt-4o-mini @ 0.3}) cannot silently
 * match a test running under another ({@code gpt-4-turbo @ 0.1}) —
 * each configuration partitions into its own baseline file.
 *
 * <h2>What this demonstrates</h2>
 *
 * <ul>
 *   <li><b>Automatic covariate resolution</b> — the use case's
 *       {@link ShoppingBasketUseCase#customCovariateResolvers()
 *       customCovariateResolvers} reads model/temperature from the
 *       resolved {@link LlmTuning} factor. No manual
 *       {@code @CovariateSource} methods.</li>
 *   <li><b>Automatic baseline partitioning</b> — measure runs under
 *       different LlmTuning values produce separate baselines, each
 *       stamped with its covariate hash per EX09.</li>
 *   <li><b>Hard CONFIGURATION gating</b> — a test running under a
 *       configuration with no matching baseline produces
 *       INCONCLUSIVE rather than silently using a baseline from a
 *       different configuration. The verdict's misalignment notes
 *       explain which baseline was rejected and why.</li>
 * </ul>
 *
 * <h2>Migration note</h2>
 *
 * <p>The legacy test used four nested {@code @Nested} classes
 * (DefaultConfiguration, ExplicitModelConfiguration,
 * LowTemperatureConfiguration, HighTemperatureConfiguration) — each
 * with its own {@code @BeforeEach} registering a different
 * {@code ShoppingBasketUseCase} variant via {@code UseCaseProvider}.
 * The typed pipeline collapses this to four flat tests: configuration
 * is a value (the {@link LlmTuning} factor) passed to
 * {@code Punit.testing}, not a setup step. The use case's covariate
 * declarations make the framework's selection automatic.
 *
 * <h2>Running</h2>
 *
 * <p>Each configuration needs its own baseline measurement. See
 * {@code ShoppingBasketMeasure} for the measure phase (configurations
 * mirror the test variants here).
 */
public class ShoppingBasketCovariateTest {

    private static final List<String> STANDARD_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    @ProbabilisticTest
    void runsUnderDefaultConfiguration() {
        // Default LlmTuning: gpt-4o-mini @ 0.3 with the use case's
        // shipping system prompt. The framework records llm_model
        // and temperature as covariates on the resolved profile, so
        // the baseline this test consults is the one measured under
        // the same configuration.
        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderExplicitModel() {
        // Switching to gpt-4-turbo. The covariate hash on the baseline
        // filename changes, so this test resolves a different baseline
        // file than the default-configuration test above.
        LlmTuning gpt4Turbo = LlmTuning.DEFAULT.model("gpt-4-turbo");

        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 50), gpt4Turbo)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderLowTemperature() {
        // Lower temperature means the LLM is more deterministic. A
        // separate baseline captures whatever pass rate that produces
        // — typically higher than the default-temperature baseline.
        LlmTuning lowTemp = LlmTuning.DEFAULT.temperature(0.1);

        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 50), lowTemp)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderHighTemperature() {
        // Higher temperature increases output variance. The baseline
        // for this configuration captures the looser pass rate.
        LlmTuning highTemp = LlmTuning.DEFAULT.temperature(0.7);

        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 50), highTemp)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }
}
