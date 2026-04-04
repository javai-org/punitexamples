package org.javai.punit.examples.experiments;

import java.util.stream.Stream;
import org.javai.punit.api.ConfigSource;
import org.javai.punit.api.ExploreExperiment;
import org.javai.punit.api.NamedConfig;
import org.javai.punit.api.OutcomeCaptor;
import org.javai.punit.api.UseCaseProvider;
import org.javai.punit.examples.app.llm.ChatLlmProvider;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * EXPLORE experiment comparing LLM model configurations.
 *
 * <p>Before establishing a production baseline, you need to decide which LLM model
 * to use. This experiment compares models by running each as a named, immutable
 * use case instance — the instance <em>is</em> the factor specification.
 *
 * <h2>Typical Workflow</h2>
 * <ol>
 *   <li><b>Explore</b> - Run this experiment to compare models</li>
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
 * @see ShoppingBasketExploreInputs
 * @see ShoppingBasketUseCase
 * @see ShoppingBasketMeasure
 */
// Run with ./gradlew exp -Prun=ShoppingBasketExplore
public class ShoppingBasketExplore {

    @RegisterExtension
    UseCaseProvider provider = new UseCaseProvider();

    private static final String TEST_INSTRUCTION = "Add some apples";

    /**
     * Compares different LLM models.
     *
     * <p>This experiment answers: "Which model handles this task most reliably?"
     * Each configuration is a fully-constructed, immutable use case instance —
     * the use case <em>is</em> the factor specification.
     *
     * @param useCase the use case instance (provided by the named config)
     * @param captor records outcomes for comparison
     */
    @ExploreExperiment(
            useCase = ShoppingBasketUseCase.class,
            samplesPerConfig = 20,
            experimentId = "model-comparison-v1",
            skipWarmup = true
    )
    @ConfigSource("modelConfigurations")
    void compareModels(ShoppingBasketUseCase useCase, OutcomeCaptor captor) {
        captor.record(useCase.translateInstruction(TEST_INSTRUCTION));
    }

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
