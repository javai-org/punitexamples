package org.javai.punit.examples.experiments;

import org.javai.punit.api.ExploreExperiment;
import org.javai.punit.api.Input;
import org.javai.punit.api.InputSource;
import org.javai.punit.api.OutcomeCaptor;
import org.javai.punit.api.UseCaseProvider;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * EXPLORE experiment exploring performance across varied inputs.
 *
 * <p>This experiment uses a curated set of instructions to understand how the
 * LLM performs across different instruction types. Inputs are cycled via round-robin
 * across all samples, producing a single aggregated exploration spec.
 *
 * <h2>Running</h2>
 * <pre>{@code
 * ./gradlew exp -Prun=ShoppingBasketExploreInputs
 * }</pre>
 *
 * @see ShoppingBasketExplore
 * @see ShoppingBasketUseCase
 */
// Run with ./gradlew exp -Prun=ShoppingBasketExploreInputs
public class ShoppingBasketExploreInputs {

    @RegisterExtension
    UseCaseProvider provider = new UseCaseProvider();

    @BeforeEach
    void setUp() {
        provider.register(ShoppingBasketUseCase.class, ShoppingBasketUseCase::new);
    }

    /**
     * Explores performance across varied inputs.
     *
     * <p>The {@code @Input} annotation explicitly marks which parameter receives
     * the input value, distinguishing it from the use case parameter.
     *
     * @param useCase the use case instance
     * @param inputData the input data containing instruction and expected output
     * @param captor records outcomes for comparison
     */
    @ExploreExperiment(
            useCase = ShoppingBasketUseCase.class,
            samplesPerConfig = 10,
            experimentId = "input-exploration-v1",
            skipWarmup = false
    )
    @InputSource(file = "fixtures/shopping-instructions.json")
    void exploreInputVariations(
            ShoppingBasketUseCase useCase,
            @Input ShoppingInstructionInput inputData,
            OutcomeCaptor captor
    ) {
        captor.record(useCase.translateInstruction(inputData.instruction()));
    }

    /**
     * Input data for the experiment.
     *
     * <p>Deserialized from JSON via {@code @InputSource}. The fields can be
     * whatever the experiment needs — the framework is agnostic about the shape.
     *
     * @param instruction the natural language instruction
     * @param expected the expected JSON response
     */
    public record ShoppingInstructionInput(String instruction, String expected) {}
}
