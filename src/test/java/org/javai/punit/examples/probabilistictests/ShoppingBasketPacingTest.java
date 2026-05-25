package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.PROBABILISTIC_TEST_SAMPLE_SIZE;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.Pacing;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.javai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.javai.punit.runtime.PUnit;

/**
 * Demonstrates rate-limiting via {@link Pacing}, useful when testing
 * against rate-limited APIs (LLMs typically).
 *
 * <ul>
 *   <li>{@link Pacing.Builder#maxRequestsPerSecond(double)
 *       maxRequestsPerSecond} — burst-style RPS cap. Inserts
 *       {@code 1000 / rps} ms between samples.</li>
 *   <li>{@link Pacing.Builder#maxRequestsPerMinute(double)
 *       maxRequestsPerMinute} — sustained RPM cap. Inserts
 *       {@code 60_000 / rpm} ms between samples.</li>
 *   <li>{@link Pacing.Builder#minMillisPerSample(long)
 *       minMillisPerSample} — explicit floor on the inter-sample
 *       gap.</li>
 *   <li>When multiple knobs combine, the most restrictive wins.</li>
 * </ul>
 *
 * <p>Pacing is a property of the service contract (it belongs to the
 * service under test, not to a particular test). The
 * {@link ShoppingBasketServiceContract#samplingPaced(Pacing, int)
 * samplingPaced} factory threads the pacing through for the
 * demonstrations below.
 */
// javai-ref: JVI-MGTJRSZ — do not remove (resolves in javai-orchestrator)
public class ShoppingBasketPacingTest {

    @ProbabilisticTest
    void runsAtRequestsPerSecondLimit() {
        // 5 RPS → ~200ms between samples. Use when the LLM API
        // documents a per-second burst limit.
        Pacing pacing = Pacing.builder().maxRequestsPerSecond(5).build();

        PUnit.testing(ShoppingBasketServiceContract.samplingPaced(pacing, PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .assertPasses();
    }

    @ProbabilisticTest
    void runsAtRequestsPerMinuteLimit() {
        // 60 RPM → 1000ms between samples. Use when the LLM API
        // documents a per-minute sustained limit (typical for OpenAI
        // and Anthropic free / lower tiers).
        Pacing pacing = Pacing.builder().maxRequestsPerMinute(60).build();

        PUnit.testing(ShoppingBasketServiceContract.samplingPaced(pacing, PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .assertPasses();
    }

    @ProbabilisticTest
    void runsAtExplicitMillisPerSample() {
        // Direct floor on the inter-sample gap. Use when the API's
        // rate limit isn't well-defined (e.g. self-hosted LLM with
        // unknown throughput) and you want guaranteed breathing
        // room between requests.
        Pacing pacing = Pacing.builder().minMillisPerSample(200).build();

        PUnit.testing(ShoppingBasketServiceContract.samplingPaced(pacing, PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .assertPasses();
    }

    @ProbabilisticTest
    void runsWithCombinedConstraints() {
        // Burst + sustained, the realistic LLM-API pacing setup.
        // The most restrictive constraint wins per sample, so this
        // pacing handles short-burst capacity (10 RPS) without
        // exceeding the longer-window quota (120 RPM).
        Pacing pacing = Pacing.builder()
                .maxRequestsPerSecond(10)
                .maxRequestsPerMinute(120)
                .build();

        PUnit.testing(ShoppingBasketServiceContract.samplingPaced(pacing, PROBABILISTIC_TEST_SAMPLE_SIZE), LlmTuning.DEFAULT)
                .assertPasses();
    }
}
