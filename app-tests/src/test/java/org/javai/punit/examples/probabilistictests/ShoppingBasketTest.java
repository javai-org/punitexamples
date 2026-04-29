package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.Config;
import org.javai.punit.junit5.Punit;

/**
 * Core probabilistic test for the typed
 * {@link ShoppingBasketUseCase}.
 *
 * <p>Demonstrates the empirical-pair pattern with a real LLM-backed
 * use case: a measure run records the LLM's observed pass rate
 * under a configuration, and this test verifies a future run under
 * the same configuration still meets the recorded baseline.
 *
 * <h2>What this demonstrates</h2>
 *
 * <ul>
 *   <li>Typed-API authoring for an LLM-backed use case (the use case
 *       ships its own {@code sampling(...)} helper, so the test
 *       method body has no type-parameter ceremony).</li>
 *   <li>Empirical {@link BernoulliPassRate} criterion — the test
 *       passes when the Wilson-score lower bound on observed
 *       success rate clears the recorded baseline rate.</li>
 *   <li>Two test variants — one over varied instructions, one
 *       over a controlled single instruction. The single-instruction
 *       form isolates the test from input variance, making it
 *       easier to detect drift in LLM behaviour.</li>
 * </ul>
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * # Phase 1 — establish the baseline:
 * ./gradlew experiment -Prun=ShoppingBasketMeasure
 *
 * # Phase 2 — verify against the baseline:
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
        Punit.testing(ShoppingBasketUseCase.sampling(STANDARD_INSTRUCTIONS, 100), Config.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void testControlledInstruction() {
        Punit.testing(ShoppingBasketUseCase.sampling(SINGLE_INSTRUCTION, 100), Config.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }
}
