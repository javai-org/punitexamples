package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.typed.Pacing;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.PUnit;

/**
 * Demonstrates rate-limiting via {@link Pacing}.
 *
 * <p>When testing against rate-limited APIs (LLMs typically), the
 * test loop has to throttle requests below the API's documented
 * limits. The framework's {@link Pacing} record captures the
 * available knobs:
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
 *       gap, regardless of rate-derived calculation.</li>
 *   <li>When multiple knobs combine, the most restrictive wins.</li>
 * </ul>
 *
 * <h2>Where pacing lives</h2>
 *
 * <p>The typed pipeline puts pacing on the use case via
 * {@link ShoppingBasketUseCase#pacing()}. Per the framework's
 * design, "pacing belongs to the service under test, not to a
 * specific experiment or probabilistic test exercising it" — every
 * test of the same service should respect the same rate limit.
 *
 * <p>For pedagogic demonstration of multiple pacing options we
 * thread the choice through the factory closure via
 * {@link ShoppingBasketUseCase#samplingPaced(Pacing, List, int)
 * samplingPaced}; in real usage authors would pick one pacing for
 * their use case and not vary it per test.
 *
 * <h2>Migration shape</h2>
 *
 * <p>The legacy file used {@code @Pacing} on each test method to
 * declare per-test pacing. The typed file packages pacing into the
 * use case construction so authors who want one pacing for all
 * tests of a use case write it once on the use-case implementation;
 * the {@code samplingPaced(...)} factory exists only to demonstrate
 * the API surface here.
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
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void runsAtRequestsPerMinuteLimit() {
        // 60 RPM → 1000ms between samples. Use when the LLM API
        // documents a per-minute sustained limit (typical for OpenAI
        // and Anthropic free / lower tiers).
        Pacing pacing = Pacing.builder().maxRequestsPerMinute(60).build();

        PUnit.testing(ShoppingBasketUseCase.samplingPaced(pacing, STANDARD_INSTRUCTIONS, 50), LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
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
                .criterion(BernoulliPassRate.empirical())
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
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }
}
