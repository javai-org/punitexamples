package org.javai.punit.examples.experiments;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.MEASURE_EXPERIMENT_SAMPLE_SIZE;

import org.javai.punit.api.Experiment;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * MEASURE experiment establishing a baseline for
 * {@link ShoppingBasketServiceContract}. Runs samples through the LLM and
 * records the observed pass rate plus latency percentiles as a
 * baseline file under {@code punit.baseline.dir}. Probabilistic
 * tests with the same service contract and factors then test against the
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

    @Experiment
    void measureBaseline() {
        PUnit.measuring(ShoppingBasketServiceContract.sampling(MEASURE_EXPERIMENT_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .experimentId("baseline-v1")
                .expiresInDays(30)
                .run();
    }
}
