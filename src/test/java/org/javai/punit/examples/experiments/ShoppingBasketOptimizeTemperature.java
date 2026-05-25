package org.javai.punit.examples.experiments;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.OPTIMIZE_PER_ITERATION_SAMPLE_SIZE;

import java.util.List;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.spec.FactorsStepper;
import org.javai.punit.api.spec.NextFactor;
import org.javai.punit.api.spec.Scorer;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * OPTIMIZE experiment demonstrating temperature's effect on
 * structured-output reliability. The stepper walks temperature
 * down from 1.0 to 0.0 in 0.1 steps; the run's score should climb
 * as temperature falls and the JSON output becomes more
 * deterministic.
 *
 * <h2>Setup</h2>
 *
 * <p>This experiment makes real LLM calls. Configure the
 * {@code ChatLlm} provider via {@code OPENAI_API_KEY} (or the
 * equivalent for your provider). The example is sized small to
 * keep the run cheap — 11 iterations at
 * {@link org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes#OPTIMIZE_PER_ITERATION_SAMPLE_SIZE}
 * samples per iteration. A realistic optimisation run uses 30+
 * samples per iteration so the per-iteration score has enough
 * signal to drive the stepper.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * ./gradlew experiment -Prun=ShoppingBasketOptimizeTemperature.optimizeTemperature
 * }</pre>
 */
// javai-ref: JVI-K47ZVJU — do not remove (resolves in javai-orchestrator)
public class ShoppingBasketOptimizeTemperature {

    private static final double TEMPERATURE_FLOOR = 0.0;
    private static final double STEP = 0.1;

    private static final List<String> SINGLE_INSTRUCTION =
            List.of("Add 2 apples and remove the bread");

    private static final Scorer PASS_RATE_SCORE = summary -> summary.total() == 0
            ? 0.0
            : (double) summary.successes() / (double) summary.total();

    private static final FactorsStepper<LlmTuning> COOL_DOWN = (current, history) -> {
        double next = current.temperature() - STEP;
        if (next < TEMPERATURE_FLOOR) {
            return NextFactor.stop();
        }
        // Round to one decimal place — IEEE-754 subtraction would
        // otherwise leave the experiment id with values like
        // 0.30000000000000004 in baselines and reports.
        double rounded = Math.round(next * 10.0) / 10.0;
        return NextFactor.next(current.temperature(rounded));
    };

    @Experiment
    void optimizeTemperature() {
        PUnit.optimizing(ShoppingBasketServiceContract.sampling(SINGLE_INSTRUCTION, OPTIMIZE_PER_ITERATION_SAMPLE_SIZE))
                .initialFactors(LlmTuning.DEFAULT.temperature(1.0))
                .stepper(COOL_DOWN)
                .maximize(PASS_RATE_SCORE)
                .maxIterations(11)         // covers 1.0, 0.9, …, 0.0
                .disableEarlyTermination()
                .experimentId("temperature-optimization-v1")
                .run();
    }
}
