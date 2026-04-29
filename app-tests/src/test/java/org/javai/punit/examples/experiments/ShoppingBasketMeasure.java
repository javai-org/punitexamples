package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.Punit;

/**
 * MEASURE experiment establishing a baseline for
 * {@link ShoppingBasketUseCase}.
 *
 * <p>The experiment runs the configured number of samples through
 * the LLM and records the observed pass rate (plus latency
 * percentiles) as a baseline file under {@code punit.baseline.dir}.
 * Probabilistic tests in the same use case + factors space resolve
 * the baseline at evaluate time and test against its recorded
 * numbers.
 *
 * <p>With 1000 samples cycling through 10 instructions, each
 * instruction is exercised exactly 100 times, providing reliable
 * per-instruction statistics in the aggregated baseline.
 *
 * <h2>Pairing</h2>
 *
 * <p>The same input list and same {@link LlmTuning} that this
 * measure uses must drive the paired probabilistic test —
 * otherwise the framework's pairing-integrity check (inputs
 * identity + factors fingerprint) rejects the baseline at test
 * time. {@link ShoppingBasketUseCase#sampling sampling} and
 * {@link LlmTuning#DEFAULT} together form the canonical pair used
 * by {@code ShoppingBasketTest}.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketMeasure.measureBaseline \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines"
 * }</pre>
 *
 * <h2>Migration note</h2>
 *
 * <p>The legacy file had a second method
 * {@code measureBaselineWithGolden} that loaded inputs from a JSON
 * fixture and used per-input expected values for instance-
 * conformance checking via a {@code JsonMatcher}. That variant
 * isn't migrated here — it depends on Jackson being on the
 * test-module classpath (which the legacy framework arranged via
 * {@code @InputSource(file = ...)}; the typed pipeline asks
 * authors to load fixtures themselves) <em>and</em> on the typed
 * pipeline's matcher / expectedOutputs surface, which is wired but
 * not yet demonstrated in the worked-example layer. The fixture-
 * loaded measure pattern lands when one of the upcoming examples
 * exercises {@code ValueMatcher} explicitly.
 */
public class ShoppingBasketMeasure {

    private static final List<String> BASKET_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Add 5 tomatoes and remove the cheese",
            "Clear the basket",
            "Clear everything",
            "Remove 2 eggs from the basket",
            "Add a dozen eggs",
            "I'd like to remove all the vegetables");

    @Experiment
    void measureBaseline() {
        Punit.measuring(ShoppingBasketUseCase.sampling(BASKET_INSTRUCTIONS, 1000), LlmTuning.DEFAULT)
                .experimentId("baseline-v1")
                .run();
    }
}
