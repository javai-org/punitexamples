package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.PROBABILISTIC_TEST_SAMPLE_SIZE;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.internal.engine.criteria.PassRate;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * Core probabilistic test for {@link ShoppingBasketServiceContract},
 * demonstrating the empirical-pair pattern with a real LLM-backed
 * service contract: a measure run records
 * the LLM's observed pass rate under a configuration, and this
 * test verifies a future run under the same configuration still
 * meets the recorded baseline. The empirical
 * {@link PassRate} criterion passes when the Wilson-score
 * lower bound on observed success rate clears the recorded baseline.
 *
 * <h2>Setup</h2>
 *
 * <p>This test reads a baseline produced by a prior measure run.
 * Run the measure phase first.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * # 1. Establish the baseline:
 * ./gradlew experiment -Prun=ShoppingBasketMeasure
 *
 * # 2. Verify against the baseline:
 * ./gradlew test --tests "ShoppingBasketTest"
 * }</pre>
 */
public class ShoppingBasketTest {

    @ProbabilisticTest
    void testInstructionTranslation() {
        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }
}
