package org.javai.punit.examples.sentinels;

import java.util.stream.Stream;
import org.javai.punit.api.InputSource;
import org.javai.punit.api.legacy.MeasureExperiment;
import org.javai.punit.api.OutcomeCaptor;
import org.javai.punit.api.legacy.ProbabilisticTest;
import org.javai.punit.api.Sentinel;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.javai.punit.usecase.UseCaseFactory;

/**
 * {@link Sentinel @Sentinel}-annotated reliability specification
 * for {@link ShoppingBasketUseCase}: a pure-Java spec containing
 * {@code @MeasureExperiment} and {@code @ProbabilisticTest} methods
 * sharing one input source. JUnit test classes derive from this
 * class via inheritance; the Sentinel engine can also consume it
 * directly for automated monitoring.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * # 1. Establish baseline.
 * ./gradlew experiment -Prun=ShoppingBasketReliabilityTest.measureBaseline
 *
 * # 2. Verify against baseline.
 * ./gradlew test --tests "ShoppingBasketReliabilityTest.testInstructionTranslation"
 * }</pre>
 */
@Sentinel
public class ShoppingBasketReliability {

    UseCaseFactory factory = new UseCaseFactory();
    { factory.register(ShoppingBasketUseCase.class, ShoppingBasketUseCase::new); }

    /**
     * Establishes the baseline by cycling through the shared
     * {@link #instructions()} stream and recording each outcome.
     */
    @MeasureExperiment(useCase = ShoppingBasketUseCase.class, experimentId = "baseline-v1")
    @InputSource("instructions")
    void measureBaseline(ShoppingBasketUseCase useCase, String instruction, OutcomeCaptor captor) {
        captor.record(useCase.translateInstruction(instruction));
    }

    /**
     * Verifies instruction translation against a spec-derived
     * threshold. 100 samples cycle through the shared
     * {@link #instructions()} stream.
     */
    @ProbabilisticTest(useCase = ShoppingBasketUseCase.class, samples = 100)
    @InputSource("instructions")
    void testInstructionTranslation(ShoppingBasketUseCase useCase, String instruction) {
        useCase.translateInstruction(instruction).assertContract();
    }

    /**
     * Shared by the measure experiment and the probabilistic test
     * so the test exercises the same input distribution as the
     * baseline.
     */
    static Stream<String> instructions() {
        return Stream.of(
                "Add 2 apples",
                "Remove the milk",
                "Add 1 loaf of bread",
                "Add 3 oranges and 2 bananas",
                "Add 5 tomatoes and remove the cheese",
                "Clear the basket",
                "Clear everything",
                "Remove 2 eggs from the basket",
                "Add a dozen eggs",
                "I'd like to remove all the vegetables"
        );
    }
}
