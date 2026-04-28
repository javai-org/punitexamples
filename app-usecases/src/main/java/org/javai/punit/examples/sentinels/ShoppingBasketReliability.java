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
 * Reliability specification for ShoppingBasketUseCase — both baseline measurement
 * and probabilistic verification live in a single class.
 *
 * <p>This class demonstrates the <b>Sentinel authoring model</b>: a pure-Java reliability
 * specification with no JUnit dependencies. It uses {@link UseCaseFactory} directly and
 * contains {@code @MeasureExperiment} and {@code @ProbabilisticTest} methods with shared
 * input sources.
 *
 * <p>JUnit test classes derive from this class via inheritance (one-line adapter),
 * while the Sentinel engine can consume it directly for automated monitoring.
 *
 * <h2>Workflow</h2>
 * <pre>{@code
 * # Phase 1: Establish baseline (run once / periodically)
 * ./gradlew exp -Prun=ShoppingBasketReliabilityTest.measureBaseline
 *
 * # Phase 2: Verify against baseline (run in CI)
 * ./gradlew test --tests "ShoppingBasketReliabilityTest.testInstructionTranslation"
 * }</pre>
 *
 * @see ShoppingBasketUseCase
 */
@Sentinel
public class ShoppingBasketReliability {

    UseCaseFactory factory = new UseCaseFactory();
    { factory.register(ShoppingBasketUseCase.class, ShoppingBasketUseCase::new); }

    // ═══════════════════════════════════════════════════════════════════════════
    // MEASURE — establish baseline (run once / periodically)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Establishes the production baseline for shopping basket instruction translation.
     *
     * <p>Cycles through representative instructions, measuring how reliably the LLM
     * translates each type. With 1000 samples and 10 instructions, each instruction
     * is tested exactly 100 times.
     *
     * @param useCase the use case instance (injected by PUnit)
     * @param instruction the instruction (cycles through variations)
     * @param captor records outcomes for aggregation
     */
    @MeasureExperiment(useCase = ShoppingBasketUseCase.class, experimentId = "baseline-v1")
    @InputSource("instructions")
    void measureBaseline(ShoppingBasketUseCase useCase, String instruction, OutcomeCaptor captor) {
        captor.record(useCase.translateInstruction(instruction));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST — verify against baseline (run in CI)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tests shopping basket instruction translation with spec-derived threshold.
     *
     * <p>Runs 100 samples with varied instructions, using a threshold derived from
     * the baseline measurement. Passes if the observed success rate meets statistical
     * expectations.
     *
     * @param useCase the use case instance (injected by PUnit)
     * @param instruction the instruction to translate
     */
    @ProbabilisticTest(useCase = ShoppingBasketUseCase.class, samples = 100)
    @InputSource("instructions")
    void testInstructionTranslation(ShoppingBasketUseCase useCase, String instruction) {
        useCase.translateInstruction(instruction).assertContract();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHARED INPUT SOURCE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Representative basket instructions covering the main operation types.
     *
     * <p>Shared by both the measure experiment and the probabilistic test, ensuring
     * the test exercises the same input distribution as the baseline.
     *
     * @return stream of instruction strings
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
