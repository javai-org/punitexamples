package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.PUnit;

/**
 * Demonstrates covariate-aware baseline matching across LLM
 * configurations. {@link ShoppingBasketUseCase} declares
 * {@code llm_model} and {@code temperature} as {@code CONFIGURATION}
 * covariates, and each configuration partitions into its own
 * baseline file. A test running under a configuration with no
 * matching baseline produces INCONCLUSIVE rather than silently
 * using a different configuration's baseline.
 *
 * <h2>Setup</h2>
 *
 * <p>Each configuration needs its own baseline measurement before
 * the matching test will succeed. See {@code ShoppingBasketMeasure}
 * for the measure phase.
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
        PUnit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderExplicitModel() {
        // Switching to gpt-4-turbo. The covariate hash on the baseline
        // filename changes, so this test resolves a different baseline
        // file than the default-configuration test above.
        LlmTuning gpt4Turbo = LlmTuning.DEFAULT.model("gpt-4-turbo");

        PUnit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 50), gpt4Turbo)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderLowTemperature() {
        // Lower temperature means the LLM is more deterministic. A
        // separate baseline captures whatever pass rate that produces
        // — typically higher than the default-temperature baseline.
        LlmTuning lowTemp = LlmTuning.DEFAULT.temperature(0.1);

        PUnit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 50), lowTemp)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderHighTemperature() {
        // Higher temperature increases output variance. The baseline
        // for this configuration captures the looser pass rate.
        LlmTuning highTemp = LlmTuning.DEFAULT.temperature(0.7);

        PUnit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 50), highTemp)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }
}
