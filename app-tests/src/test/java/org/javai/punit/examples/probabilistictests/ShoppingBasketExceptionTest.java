package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.typed.spec.ExceptionPolicy;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.ShoppingBasketUseCase;
import org.javai.punit.examples.typed.ShoppingBasketUseCase.LlmTuning;
import org.javai.punit.junit5.Punit;

/**
 * Demonstrates exception-handling policies in probabilistic
 * testing.
 *
 * <p>The typed pipeline reserves <em>thrown exceptions</em> from
 * {@code UseCase.apply()} for genuine defects — programming
 * mistakes, misconfiguration, catastrophe. Anticipated failures
 * (contract violations, validation errors, service-returned error
 * codes) travel through {@code UseCaseOutcome.fail(...)} as data,
 * never as exceptions. See
 * {@link org.javai.punit.api.typed.UseCaseOutcome}.
 *
 * <p>That said, real-world use cases occasionally throw despite
 * the convention — flaky network, third-party libraries that
 * surface failures as exceptions. The {@link ExceptionPolicy} knob
 * controls what the engine does:
 *
 * <ul>
 *   <li>{@link ExceptionPolicy#ABORT_TEST} (default) — the defect
 *       propagates, the run dies. Use when any exception indicates
 *       a serious problem and you want fast feedback.</li>
 *   <li>{@link ExceptionPolicy#FAIL_SAMPLE} — synthesise a failing
 *       sample, continue the run, count the exception toward the
 *       observed pass rate. Use when exceptions are part of the
 *       expected failure-mode space being measured.</li>
 * </ul>
 *
 * <p>{@code Error} subtypes (OOM, StackOverflow, LinkageError)
 * always propagate regardless of policy — they are never caught.
 *
 * <p>The {@code .maxExampleFailures(int)} knob caps how many full
 * failure outcomes are retained for diagnostic display. Latency
 * statistics still see every sample; only the retained-for-display
 * detail is capped.
 *
 * <h2>Default change vs the legacy</h2>
 *
 * <p>The legacy default was {@code FAIL_SAMPLE} — exceptions were
 * silently counted as failures unless the author opted into
 * {@code ABORT_TEST}. The typed default is {@code ABORT_TEST}
 * because silently converting a defect into a counted-failure
 * sample hides bugs. Authors who want the count-as-failure
 * behaviour opt in explicitly.
 */
public class ShoppingBasketExceptionTest {

    private static final List<String> STANDARD_INSTRUCTIONS = List.of(
            "Add 2 apples",
            "Remove the milk",
            "Add 1 loaf of bread",
            "Add 3 oranges and 2 bananas",
            "Clear the basket");

    @ProbabilisticTest
    void abortTestPolicyStopsOnFirstDefect() {
        // Default policy. Any thrown exception from apply() bubbles
        // out of the engine and aborts the run. The engine never
        // gets a chance to render a verdict.
        Punit.testing(
                ShoppingBasketUseCase.samplingBuilder(STANDARD_INSTRUCTIONS, 100)
                        .onException(ExceptionPolicy.ABORT_TEST)
                        .build(),
                LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void failSamplePolicyCountsExceptionAsFailedSample() {
        // FAIL_SAMPLE: exceptions become synthetic failed outcomes.
        // The run completes; the verdict reflects the proportion of
        // exception-throwing samples among the total. Useful when
        // intermittent infrastructure failures are part of the
        // "reliability" you're measuring rather than a signal that
        // the test setup is broken.
        Punit.testing(
                ShoppingBasketUseCase.samplingBuilder(STANDARD_INSTRUCTIONS, 100)
                        .onException(ExceptionPolicy.FAIL_SAMPLE)
                        .build(),
                LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }

    @ProbabilisticTest
    void failSampleWithFailureRetentionCap() {
        // .maxExampleFailures(int) caps the number of full failure
        // outcomes retained for diagnostic display. The engine still
        // counts every failure for the verdict and computes latency
        // over every sample; only the per-failure detail kept for
        // post-run inspection is bounded. Default is 10.
        Punit.testing(
                ShoppingBasketUseCase.samplingBuilder(STANDARD_INSTRUCTIONS, 100)
                        .onException(ExceptionPolicy.FAIL_SAMPLE)
                        .maxExampleFailures(3)
                        .build(),
                LlmTuning.DEFAULT)
                .criterion(BernoulliPassRate.empirical())
                .assertPasses();
    }
}
