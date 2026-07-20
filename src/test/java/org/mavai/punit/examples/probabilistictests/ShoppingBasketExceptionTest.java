package org.mavai.punit.examples.probabilistictests;

import static org.mavai.punit.examples.servicecontracts.ShoppingBasketSampleSizes.PROBABILISTIC_TEST_SAMPLE_SIZE;

import org.mavai.punit.api.ProbabilisticTest;
import org.mavai.punit.api.spec.ExceptionPolicy;
import org.mavai.punit.examples.servicecontracts.ShoppingBasketServiceContract;
import org.mavai.punit.examples.servicecontracts.ShoppingBasketServiceContract.LlmTuning;
import org.mavai.punit.runtime.PUnit;

/**
 * Demonstrates exception-handling policies in probabilistic
 * testing.
 *
 * <p>The framework reserves <em>thrown exceptions</em> from
 * {@code ServiceContract.invoke(...)} for genuine defects — programming
 * mistakes, misconfiguration, catastrophe. Anticipated failures
 * (contract violations, validation errors, service-returned error
 * codes) travel through {@code Outcome.fail(...)} as data,
 * never as exceptions. See
 * {@link org.mavai.outcome.Outcome}.
 *
 * <p>That said, real-world service contracts occasionally throw despite
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
 */
// mavai-ref: JVI-AKXG921 — do not remove (resolves in mavai-orchestrator)
public class ShoppingBasketExceptionTest {

    @ProbabilisticTest
    void abortTestPolicyStopsOnFirstDefect() {
        // Default policy. Any thrown exception from invoke() bubbles
        // out of the engine and aborts the run. The engine never
        // gets a chance to render a verdict.
        PUnit.testing(
                ShoppingBasketServiceContract.samplingBuilder(PROBABILISTIC_TEST_SAMPLE_SIZE)
                        .onException(ExceptionPolicy.ABORT_TEST)
                        .build(),
                LlmTuning.DEFAULT)
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
        PUnit.testing(
                ShoppingBasketServiceContract.samplingBuilder(PROBABILISTIC_TEST_SAMPLE_SIZE)
                        .onException(ExceptionPolicy.FAIL_SAMPLE)
                        .build(),
                LlmTuning.DEFAULT)
                .assertPasses();
    }

    @ProbabilisticTest
    void failSampleWithFailureRetentionCap() {
        // .maxExampleFailures(int) caps the number of full failure
        // outcomes retained for diagnostic display. The engine still
        // counts every failure for the verdict and computes latency
        // over every sample; only the per-failure detail kept for
        // post-run inspection is bounded. Default is 10.
        PUnit.testing(
                ShoppingBasketServiceContract.samplingBuilder(PROBABILISTIC_TEST_SAMPLE_SIZE)
                        .onException(ExceptionPolicy.FAIL_SAMPLE)
                        .maxExampleFailures(3)
                        .build(),
                LlmTuning.DEFAULT)
                .assertPasses();
    }
}
