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
 *       baseline. Run this in the same target environment as the
 *       verification, or in a deliberately equivalent calibration
 *       environment, <em>before</em> the verification test runs.
 *       Capturing the baseline anywhere else risks measuring noise
 *       that the production system will not exhibit — defeating the
 *       purpose of the empirical comparison.</li>
 *   <li>{@link #shoppingMeetsBaseline()} — a {@code @ProbabilisticTest}
 *       that compares a fresh sample against the recorded baseline
 *       using {@link PassRate#empirical()}. Run this on a
 *       schedule via the Sentinel binary to detect regression.</li>
 * </ul>
 *
 * <p>Both methods reference {@link ShoppingBasketUseCase}, whose
 * {@code id()} anchors the baseline filename and covariate
 * fingerprint. The sentinel itself carries no identity concerns:
 * the experiment and the test share a single use case definition,
 * so they cannot drift onto different baseline artefacts.
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

    /**
     * The asymmetry is intentional. The baseline is captured once
     * with high statistical power, so the recorded pass rate is a
     * tight estimate; the verification test then runs cheaply and
     * frequently against that baseline. Equal sample counts on
     * both sides would flatten this distinction and burn budget
     * that calibration deserves more than routine verification does.
     */
    private static final int BASELINE_SAMPLES = 1000;
    private static final int VERIFICATION_SAMPLES = 50;

    private static final List<String> INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    @Experiment
    void shoppingBaseline() {
        PUnit.measuring(ShoppingBasketUseCase.sampling(INSTRUCTIONS, BASELINE_SAMPLES), LlmTuning.DEFAULT)
                .run();
    }

    @ProbabilisticTest
    void shoppingMeetsBaseline() {
        PUnit.testing(ShoppingBasketUseCase.sampling(INSTRUCTIONS, VERIFICATION_SAMPLES), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }
}
