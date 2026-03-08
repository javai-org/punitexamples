package org.javai.punit.examples.sentinals;

import org.javai.punit.api.Latency;
import org.javai.punit.api.MeasureExperiment;
import org.javai.punit.api.OutcomeCaptor;
import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.Sentinel;
import org.javai.punit.api.TestIntent;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase;
import org.javai.punit.usecase.UseCaseFactory;

/**
 * Reliability specification for PaymentGatewayUseCase — demonstrating the
 * <b>two-dimension stochasticity model</b> with both functional and latency assertions.
 *
 * <p>This class is a {@link Sentinel @Sentinel}-annotated reliability specification:
 * pure Java, no JUnit dependencies. It uses {@link UseCaseFactory} directly and
 * contains {@code @MeasureExperiment} and {@code @ProbabilisticTest} methods.
 *
 * <p>The payment gateway use case has a {@link org.javai.punit.contract.ServiceContract}
 * with both a functional postcondition ("Transaction succeeded") and a
 * {@link org.javai.punit.contract.DurationConstraint} ("SLA: under 1 second").
 * This enables three dimension-scoped assertion styles:
 * <ul>
 *   <li>{@code assertContract()} — functional correctness only</li>
 *   <li>{@code assertLatency()} — timing constraint only</li>
 *   <li>{@code assertAll()} — both dimensions (adaptive)</li>
 * </ul>
 *
 * <h2>Workflow</h2>
 * <pre>{@code
 * # Phase 1: Establish baseline (produces both functional and latency specs)
 * ./gradlew exp -Prun=PaymentGatewayReliabilityTest.measureBaseline
 *
 * # Phase 2: Verify against baseline (run in CI)
 * ./gradlew test --tests "PaymentGatewayReliabilityTest"
 * }</pre>
 *
 * @see PaymentGatewayUseCase
 */
@Sentinel
public class PaymentGatewayReliability {

	UseCaseFactory factory = new UseCaseFactory();

	{
		factory.register(PaymentGatewayUseCase.class, PaymentGatewayUseCase::new);
	}

	// =========================================================================
	// MEASURE — establish baseline (run once / periodically)
	// =========================================================================

	/**
	 * Establishes the production baseline for payment gateway reliability.
	 *
	 * <p>With 200 samples, this experiment captures both the functional success rate
	 * and latency distribution. The framework produces two spec files:
	 * <ul>
	 *   <li>{@code PaymentGatewayUseCase.yaml} — functional baseline (pass rate)</li>
	 *   <li>{@code PaymentGatewayUseCase.latency.yaml} — latency baseline (percentiles)</li>
	 * </ul>
	 *
	 * @param useCase the use case instance (injected by PUnit)
	 * @param captor records outcomes for aggregation
	 */
	@MeasureExperiment(useCase = PaymentGatewayUseCase.class, samples = 200,
			experimentId = "baseline-v1")
	void measureBaseline(PaymentGatewayUseCase useCase, OutcomeCaptor captor) {
		captor.record(useCase.chargeCard("tok_visa_4242", 1999L));
	}

	// =========================================================================
	// TEST — verify against baseline (run in CI)
	// =========================================================================

	/**
	 * Tests functional correctness only — postconditions are evaluated,
	 * but the latency constraint is not.
	 *
	 * <p>This test demonstrates {@code assertContract()} which isolates the
	 * functional dimension. The verdict reports only functional pass/fail
	 * statistics; latency data is absent.
	 *
	 * <p><b>Latency mechanisms:</b> None. {@code assertContract()} bypasses
	 * the service contract's duration constraint. No {@code @Latency} annotation
	 * is present, and any baseline-derived latency thresholds are not evaluated
	 * because the latency dimension is not exercised.
	 *
	 * @param useCase the use case instance (injected by PUnit)
	 */
	@ProbabilisticTest(
			useCase = PaymentGatewayUseCase.class,
			samples = 50,
			minPassRate = 0.99,
			intent = TestIntent.SMOKE,
			thresholdOrigin = ThresholdOrigin.SLA,
			contractRef = "Payment Provider SLA v2.3, Section 4.1"
	)
	void testFunctionalCorrectness(PaymentGatewayUseCase useCase) {
		useCase.chargeCard("tok_visa_4242", 1999L).assertContract();
	}

	/**
	 * Tests latency constraint only — the duration must be within the SLA limit,
	 * but functional postconditions are not evaluated.
	 *
	 * <p>This test demonstrates {@code assertLatency()} which isolates the
	 * latency dimension. The verdict reports only latency pass/fail statistics;
	 * functional postcondition data is absent.
	 *
	 * <p><b>Latency mechanisms:</b> Service contract duration constraint only.
	 * {@code assertLatency()} evaluates the {@code ensureDurationBelow("SLA",
	 * Duration.ofSeconds(5))} constraint per sample. No {@code @Latency}
	 * annotation is present, so no aggregate percentile thresholds are applied.
	 * If a baseline spec with latency data exists, PUnit will additionally derive
	 * aggregate percentile thresholds automatically (advisory by default).
	 *
	 * @param useCase the use case instance (injected by PUnit)
	 */
	@ProbabilisticTest(
			useCase = PaymentGatewayUseCase.class,
			samples = 50,
			minPassRate = 0.99,
			intent = TestIntent.SMOKE,
			thresholdOrigin = ThresholdOrigin.SLA,
			contractRef = "Payment Provider SLA v2.3, Section 4.2"
	)
	void testLatency(PaymentGatewayUseCase useCase) {
		useCase.chargeCard("tok_visa_4242", 1999L).assertLatency();
	}

	/**
	 * Tests functional correctness with explicit latency thresholds from the SLA.
	 *
	 * <p>This test demonstrates the {@code latency} attribute of
	 * {@code @ProbabilisticTest}, which declares contractual percentile
	 * thresholds directly on the annotation. The SLA requires that 95% of
	 * transactions complete within 500ms and 99% within 1000ms.
	 *
	 * <p>PUnit measures the wall-clock time of each successful sample, computes
	 * the observed percentile distribution, and compares it against the declared
	 * thresholds. Both the pass-rate and latency dimensions must pass for the
	 * test to pass.
	 *
	 * <p><b>Latency mechanisms:</b> Two mechanisms are active. (1) The service
	 * contract's duration constraint ({@code ensureDurationBelow}) is evaluated
	 * per sample via {@code assertAll()}. (2) The explicit {@code @Latency}
	 * annotation provides aggregate percentile thresholds (p95, p99) evaluated
	 * after all samples complete. Note: explicit {@code @Latency} thresholds are
	 * mutually exclusive with baseline-derived thresholds — if a baseline spec
	 * with latency data exists, PUnit raises a configuration error.
	 *
	 * @param useCase the use case instance (injected by PUnit)
	 */
	@ProbabilisticTest(
			useCase = PaymentGatewayUseCase.class,
			samples = 200,
			minPassRate = 0.99,
			intent = TestIntent.SMOKE,
			thresholdOrigin = ThresholdOrigin.SLA,
			contractRef = "Payment Provider SLA v2.3, Sections 4.1 & 4.2",
			latency = @Latency(
					p95Ms = 500 // not more than 5% of invocations should have latency of > 500ms
					, p99Ms = 1000 // not more than 1% of invocations should have latency of > 1s
			)
	)
	void testReliabilityWithLatencySla(PaymentGatewayUseCase useCase) {
		useCase.chargeCard("tok_visa_4242", 1999L).assertAll();
	}

	/**
	 * Tests both dimensions — functional postconditions <b>and</b> latency constraint
	 * are evaluated together. A sample passes only if both dimensions pass.
	 *
	 * <p>This test demonstrates {@code assertAll()} which adaptively asserts
	 * whichever dimensions are configured on the service contract. Since
	 * {@link PaymentGatewayUseCase} has both a functional postcondition and a
	 * {@link org.javai.punit.contract.DurationConstraint}, both are evaluated.
	 * The verdict includes per-dimension breakdown.
	 *
	 * <p><b>Latency mechanisms:</b> The service contract's duration constraint
	 * is evaluated per sample via {@code assertAll()}. No explicit
	 * {@code @Latency} annotation is present, but if a baseline spec with
	 * latency data exists (from {@code measureBaseline}), PUnit automatically
	 * derives aggregate percentile thresholds — advisory by default, enforced
	 * when {@code -Dpunit.latency.enforce=true}.
	 *
	 * @param useCase the use case instance (injected by PUnit)
	 */
	@ProbabilisticTest(
			useCase = PaymentGatewayUseCase.class,
			samples = 50,
			minPassRate = 0.99,
			intent = TestIntent.SMOKE,
			thresholdOrigin = ThresholdOrigin.SLA,
			contractRef = "Payment Provider SLA v2.3, Sections 4.1 & 4.2"
	)
	void testCombinedReliability(PaymentGatewayUseCase useCase) {
		useCase.chargeCard("tok_visa_4242", 1999L).assertAll();
	}
}
