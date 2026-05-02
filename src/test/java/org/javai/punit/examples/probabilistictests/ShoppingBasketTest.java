package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.engine.criteria.PassRate;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * Core probabilistic test for {@link ShoppingBasketUseCase},
 * demonstrating the empirical-pair pattern with a real LLM-backed
 * use case: a measure run records
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

    private static final List<String> STANDARD_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    private static final List<String> SINGLE_INSTRUCTION =
            List.of("Add 2 apples and remove the bread");

    @ProbabilisticTest
    void testInstructionTranslation() {
        PUnit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 100), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void testControlledInstruction() {
        PUnit.testing(ShoppingBasketUseCase.sampling(SINGLE_INSTRUCTION, 100), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }
}
