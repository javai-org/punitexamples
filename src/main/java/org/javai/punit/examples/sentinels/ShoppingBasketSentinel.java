package org.javai.punit.examples.sentinels;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.MEASURE_EXPERIMENT_SAMPLE_SIZE;
import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.PROBABILISTIC_TEST_SAMPLE_SIZE;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
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
 *       using the empirical posture declared on the contract. Run this on a
 *       schedule via the Sentinel binary to detect regression.</li>
 * </ul>
 *
 * <p>Both methods reference {@link ShoppingBasketServiceContract}, whose
 * {@code id()} anchors the baseline filename and covariate
 * fingerprint. The sentinel itself carries no identity concerns:
 * the experiment and the test share a single service contract definition,
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
// javai-ref: JVI-VQQ8SP1 — do not remove (resolves in javai-orchestrator)
public class ShoppingBasketSentinel {

    /**
     * The asymmetry between the baseline and verification sample
     * counts is intentional. The baseline is captured once with
     * high statistical power, so the recorded pass rate is a tight
     * estimate; the verification test then runs cheaply and
     * frequently against that baseline. Equal sample counts on both
     * sides would flatten this distinction and burn budget that
     * calibration deserves more than routine verification does.
     *
     * <p>The two values are read from
     * {@link org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes},
     * the single sample-size policy shared across the service contract's
     * dev experiments, dev tests, and this sentinel — so a baseline
     * captured here is commensurate with whatever the dev tests
     * recorded, and vice versa.
     */
    @Experiment
    void shoppingBaseline() {
        PUnit.measuring(ShoppingBasketServiceContract.sampling(MEASURE_EXPERIMENT_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .run();
    }

    @ProbabilisticTest
    void shoppingMeetsBaseline() {
        PUnit.testing(ShoppingBasketServiceContract.sampling(PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .assertPasses();
    }
}
