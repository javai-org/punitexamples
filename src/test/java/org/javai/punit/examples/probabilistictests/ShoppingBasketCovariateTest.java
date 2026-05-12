package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.PROBABILISTIC_TEST_SAMPLE_SIZE;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.internal.engine.criteria.PassRate;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * Demonstrates covariate-aware baseline matching across LLM
 * configurations. {@link ShoppingBasketServiceContract} declares
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

    @ProbabilisticTest
    void runsUnderDefaultConfiguration() {
        // Default LlmTuning: gpt-4o-mini @ 0.3 with the service contract's
        // shipping system prompt. The framework records llm_model
        // and temperature as covariates on the resolved profile, so
        // the baseline this test consults is the one measured under
        // the same configuration.
        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderExplicitModel() {
        // Switching to gpt-4-turbo. The covariate hash on the baseline
        // filename changes, so this test resolves a different baseline
        // file than the default-configuration test above.
        LlmTuning gpt4Turbo = LlmTuning.DEFAULT.model("gpt-4-turbo");

        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE), gpt4Turbo)
                .criterion(PassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderLowTemperature() {
        // Lower temperature means the LLM is more deterministic. A
        // separate baseline captures whatever pass rate that produces
        // — typically higher than the default-temperature baseline.
        LlmTuning lowTemp = LlmTuning.DEFAULT.temperature(0.1);

        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE), lowTemp)
                .criterion(PassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsUnderHighTemperature() {
        // Higher temperature increases output variance. The baseline
        // for this configuration captures the looser pass rate.
        LlmTuning highTemp = LlmTuning.DEFAULT.temperature(0.7);

        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE), highTemp)
                .criterion(PassRate.empirical())
                .assertPasses();
    }
}
