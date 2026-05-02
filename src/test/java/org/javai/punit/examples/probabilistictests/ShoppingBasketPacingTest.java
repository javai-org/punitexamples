package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.Pacing;
import org.javai.punit.engine.criteria.PassRate;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase;
import org.javai.punit.examples.usecases.ShoppingBasketUseCase.LlmTuning;
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
 * <p>Pacing is a property of the use case (it belongs to the
 * service under test, not to a particular test). The
 * {@link ShoppingBasketUseCase#samplingPaced(Pacing, List, int)
 * samplingPaced} factory threads the pacing through for the
 * demonstrations below.
 */
public class ShoppingBasketPacingTest {

    private static final List<String> STANDARD_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    @ProbabilisticTest
    void runsAtRequestsPerSecondLimit() {
        // 5 RPS → ~200ms between samples. Use when the LLM API
        // documents a per-second burst limit.
        Pacing pacing = Pacing.builder().maxRequestsPerSecond(5).build();

        PUnit.testing(ShoppingBasketUseCase.samplingPaced(pacing, STANDARD_INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsAtRequestsPerMinuteLimit() {
        // 60 RPM → 1000ms between samples. Use when the LLM API
        // documents a per-minute sustained limit (typical for OpenAI
        // and Anthropic free / lower tiers).
        Pacing pacing = Pacing.builder().maxRequestsPerMinute(60).build();

        PUnit.testing(ShoppingBasketUseCase.samplingPaced(pacing, STANDARD_INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsAtExplicitMillisPerSample() {
        // Direct floor on the inter-sample gap. Use when the API's
        // rate limit isn't well-defined (e.g. self-hosted LLM with
        // unknown throughput) and you want guaranteed breathing
        // room between requests.
        Pacing pacing = Pacing.builder().minMillisPerSample(200).build();

        PUnit.testing(ShoppingBasketUseCase.samplingPaced(pacing, STANDARD_INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
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

        PUnit.testing(ShoppingBasketUseCase.samplingPaced(pacing, STANDARD_INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .criterion(PassRate.empirical())
                .assertPasses();
    }
}
