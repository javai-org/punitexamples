package org.javai.punit.examples.experiments;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * MEASURE experiment establishing a baseline for
 * {@link ShoppingBasketUseCase}. Runs samples through the LLM and
 * records the observed pass rate plus latency percentiles as a
 * baseline file under {@code punit.baseline.dir}. Probabilistic
 * tests with the same use case and factors then test against the
 * recorded numbers.
 *
 * <h2>Setup</h2>
 *
 * <p>This experiment makes real LLM calls. Configure the
 * {@code ChatLlm} provider via {@code OPENAI_API_KEY}. The same
 * input list and {@link LlmTuning} value used here must drive any
 * paired probabilistic test — the pairing-integrity check rejects
 * mismatched inputs or factors at test time.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketMeasure.measureBaseline \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines"
 * }</pre>
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
        PUnit.measuring(ShoppingBasketUseCase.sampling(BASKET_INSTRUCTIONS, 1000), LlmTuning.DEFAULT)
                .experimentId("baseline-v1")
                .run();
    }
}
