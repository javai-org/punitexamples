package org.javai.punit.examples.experiments;

import java.util.stream.Stream;
import org.javai.punit.api.ConfigSource;
import org.javai.punit.api.legacy.ExploreExperiment;
import org.javai.punit.api.Input;
import org.javai.punit.api.InputSource;
import org.javai.punit.api.NamedConfig;
import org.javai.punit.api.OutcomeCaptor;
import org.javai.punit.api.UseCaseProvider;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * EXPLORE experiment comparing LLM model configurations across varied inputs.
 *
 * <p>Before establishing a production baseline, you need to decide which LLM model
 * to use. This experiment compares models by running each as a named, immutable
 * use case instance — the instance <em>is</em> the factor specification — across
 * a curated set of instructions to understand how each model performs on different
 * instruction types. Inputs are cycled via round-robin within each configuration.
 *
 * <h2>Typical Workflow</h2>
 * <ol>
 *   <li><b>Explore</b> - Run this experiment to compare models across inputs</li>
 *   <li><b>Choose</b> - Select the best configuration based on results</li>
 *   <li><b>Measure</b> - Run {@link ShoppingBasketMeasure} to establish baseline</li>
 *   <li><b>Test</b> - Use the baseline in probabilistic regression tests</li>
 * </ol>
 *
 * <h2>Running</h2>
 * <pre>{@code
 * ./gradlew exp -Prun=ShoppingBasketExplore
 * }</pre>
 *
 * @see ShoppingBasketUseCase
 * @see ShoppingBasketMeasure
 */
// Run with ./gradlew exp -Prun=ShoppingBasketExplore
public class ShoppingBasketExplore {

    @RegisterExtension
    UseCaseProvider provider = new UseCaseProvider();

    /**
     * Compares different LLM models across varied inputs.
     *
     * <p>This experiment answers: "Which model handles different instruction types
     * most reliably?" Each configuration is a fully-constructed, immutable use case
     * instance, and each is tested against a curated set of instructions from the
     * input source.
     *
     * @param useCase the use case instance (provided by the named config)
     * @param inputData the input data containing instruction and expected output
     * @param captor records outcomes for comparison
     */
    @ExploreExperiment(
            useCase = ShoppingBasketUseCase.class,
            samplesPerConfig = 20,
            experimentId = "model-comparison-v1",
            skipWarmup = true
    )
    @ConfigSource("modelConfigurations")
    @InputSource(file = "fixtures/shopping-instructions.json")
    void compareModels(
            ShoppingBasketUseCase useCase,
            @Input ShoppingInstructionInput inputData,
            OutcomeCaptor captor
    ) {
        captor.record(useCase.translateInstruction(inputData.instruction()));
    }

    /**
     * Input data for the experiment.
     *
     * @param instruction the natural language instruction
     * @param expected the expected JSON response
     */
    public record ShoppingInstructionInput(String instruction, String expected) {}

    /**
     * Models to compare.
     *
     * <p>Each configuration is a fully-constructed use case instance. The instance
     * carries its own model, temperature, and system prompt — no separate factor
     * map is needed.
     */
    static Stream<NamedConfig<ShoppingBasketUseCase>> modelConfigurations() {
        var llm = ChatLlmProvider.resolve();
        return Stream.of(
                NamedConfig.of("gpt-4o-mini",
                        new ShoppingBasketUseCase(llm, "gpt-4o-mini", 0.1, ShoppingBasketUseCase.DEFAULT_SYSTEM_PROMPT)),
                NamedConfig.of("gpt-4o",
                        new ShoppingBasketUseCase(llm, "gpt-4o", 0.1, ShoppingBasketUseCase.DEFAULT_SYSTEM_PROMPT)),
                NamedConfig.of("claude-haiku",
                        new ShoppingBasketUseCase(llm, "claude-haiku-4-5-20251001", 0.1, ShoppingBasketUseCase.DEFAULT_SYSTEM_PROMPT)),
                NamedConfig.of("claude-sonnet",
                        new ShoppingBasketUseCase(llm, "claude-sonnet-4-5-20250929", 0.1, ShoppingBasketUseCase.DEFAULT_SYSTEM_PROMPT))
        );
    }
}
