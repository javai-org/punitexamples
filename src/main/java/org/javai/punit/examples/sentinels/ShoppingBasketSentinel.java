package org.javai.punit.examples.sentinels;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.engine.criteria.PassRate;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * Sentinel-deployable reliability checks for the shopping-basket use
 * case.
 *
 * <p>Two methods, both runnable under JUnit (as part of the test
 * suite) and under the Sentinel binary (as a production-monitoring
 * artefact):
 *
 * <ul>
 *   <li>{@link #shoppingBaseline()} — an {@code @Experiment} that
 *       records the LLM's observed pass rate as a covariate-keyed
 *       baseline. Run this in a controlled environment (typically a
 *       development workstation or a dedicated measure pipeline)
 *       <em>before</em> the verification test runs in production.</li>
 *   <li>{@link #shoppingMeetsBaseline()} — a {@code @ProbabilisticTest}
 *       that compares a fresh sample against the recorded baseline
 *       using {@link PassRate#empirical()}. Run this on a
 *       schedule via the Sentinel binary to detect regression.</li>
 * </ul>
 *
 * <h2>Sentinel deployment</h2>
 *
 * <p>To package this class as a Sentinel binary, apply the punit
 * Gradle plugin and run the {@code createSentinel} task. The plugin
 * scans compiled classes for any class declaring a
 * {@code @ProbabilisticTest} or {@code @Experiment} method (no
 * class-level marker required) and writes the FQNs into the JAR's
 * {@code META-INF/punit/sentinel-classes} manifest:
 *
 * <pre>{@code
 * plugins {
 *     id("org.javai.punit")
 * }
 *
 * // ./gradlew createSentinel
 * // produces build/libs/<project>-sentinel.jar
 * }</pre>
 *
 * <p>The resulting JAR is a self-contained executable. Run it as a
 * scheduled job, container health-check, or CI pipeline step:
 *
 * <pre>{@code
 * java -jar build/libs/myapp-sentinel.jar test
 * }</pre>
 *
 * <h2>Dual consumption</h2>
 *
 * <p>The same class is also picked up by JUnit during ordinary
 * development runs ({@code ./gradlew test}) — the
 * {@code @ProbabilisticTest} and {@code @Experiment} annotations are
 * meta-annotated {@code @Test} so JUnit Jupiter discovers them
 * directly. No second annotation, no JUnit-flavoured variant, no
 * separate test wrapper.
 */
public class ShoppingBasketSentinel {

    private static final List<String> INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    @Experiment
    void shoppingBaseline() {
        PUnit.measuring(ShoppingBasketUseCase.sampling(INSTRUCTIONS, 100), LlmTuning.DEFAULT)
                .run();
    }

    @ProbabilisticTest
    void shoppingMeetsBaseline() {
        PUnit.testing(ShoppingBasketUseCase.sampling(INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }
}
