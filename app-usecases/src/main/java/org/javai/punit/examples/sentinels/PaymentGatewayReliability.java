package org.javai.punit.examples.sentinels;

import org.javai.punit.api.Latency;
import org.javai.punit.api.legacy.MeasureExperiment;
import org.javai.punit.api.OutcomeCaptor;
import org.javai.punit.api.legacy.ProbabilisticTest;
import org.javai.punit.api.Sentinel;
import org.javai.punit.api.TestIntent;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase;
import org.javai.punit.usecase.UseCaseFactory;

/**
 * Reliability specification for {@link PaymentGatewayUseCase}, a
 * {@link Sentinel @Sentinel}-annotated pure-Java spec containing a
 * {@code @MeasureExperiment} and several {@code @ProbabilisticTest}
 * methods.
 *
 * <p>The payment gateway use case has a service contract with both
 * a functional postcondition and a duration constraint, so this
 * spec exercises the three dimension-scoped assertion styles:
 * <ul>
 *   <li>{@code assertContract()} — functional correctness only.</li>
 *   <li>{@code assertLatency()} — timing constraint only.</li>
 *   <li>{@code assertAll()} — both dimensions.</li>
 * </ul>
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * # 1. Establish baseline.
 * ./gradlew experiment -Prun=PaymentGatewayReliabilityTest.measureBaseline
 *
 * # 2. Verify against baseline.
 * ./gradlew test --tests "PaymentGatewayReliabilityTest"
 * }</pre>
 */
@Sentinel
public class PaymentGatewayReliability {

	UseCaseFactory factory = new UseCaseFactory();

	{
		factory.register(PaymentGatewayUseCase.class, PaymentGatewayUseCase::new);
	}

	/**
	 * Establishes the baseline for payment gateway reliability.
	 * Captures both the functional success rate and the latency
	 * distribution; the framework produces both
	 * {@code PaymentGatewayUseCase.yaml} (pass rate) and
	 * {@code PaymentGatewayUseCase.latency.yaml} (percentiles).
	 */
	@MeasureExperiment(useCase = PaymentGatewayUseCase.class, samples = 200,
			experimentId = "baseline-v1")
	void measureBaseline(PaymentGatewayUseCase useCase, OutcomeCaptor captor) {
		captor.record(useCase.chargeCard("tok_visa_4242", 1999L));
	}

	/**
	 * Tests functional correctness only via {@code assertContract()},
	 * which evaluates postconditions but bypasses the service
	 * contract's duration constraint. The verdict reports only
	 * functional pass/fail statistics.
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
	 * Tests the latency constraint only via {@code assertLatency()},
	 * which evaluates the service contract's duration constraint
	 * per sample but skips functional postconditions. If a baseline
	 * spec with latency data exists, aggregate percentile thresholds
	 * are also derived automatically (advisory by default).
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
	 * Tests functional correctness with explicit aggregate latency
	 * thresholds declared via the {@code latency = @Latency(...)}
	 * attribute. PUnit measures wall-clock time per successful
	 * sample, computes the observed percentile distribution, and
	 * compares against the declared p95 and p99 thresholds. Both
	 * pass-rate and latency dimensions must pass.
	 *
	 * <p>Explicit {@code @Latency} thresholds are mutually exclusive
	 * with baseline-derived thresholds — PUnit raises a configuration
	 * error if a baseline spec with latency data also exists.
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
	 * Tests both dimensions — functional postconditions and the
	 * latency constraint — via {@code assertAll()}, which adaptively
	 * asserts whichever dimensions are configured on the service
	 * contract. The verdict includes a per-dimension breakdown. If
	 * a baseline spec with latency data exists, aggregate percentile
	 * thresholds are also derived automatically (advisory by default,
	 * enforced under {@code -Dpunit.latency.enforce=true}).
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
