package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.api.ThresholdOrigin.SLA;
import java.util.List;
import org.javai.outcome.Outcome;
import org.javai.punit.api.Contract;
import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.TestIntent;
import org.javai.punit.examples.app.payment.MockPaymentGateway;
import org.javai.punit.examples.app.payment.PaymentGateway;
import org.javai.punit.examples.app.payment.PaymentResult;
import org.javai.punit.runtime.PUnit;
import org.jspecify.annotations.NonNull;

/**
 * The smallest shape a probabilistic test takes: an <em>inline</em>
 * contract. "Inline" means the contract — the service call plus its
 * acceptance criteria — is written directly here at the test call
 * site, rather than as its own named {@code ServiceContract} class.
 *
 * <p>A payment gateway is a genuinely stochastic dependency — it
 * succeeds <em>most</em> of the time — which is exactly why it warrants
 * probabilistic testing rather than a single pass/fail call. Its
 * commitments are <em>normative</em>: a contractual pass rate drawn
 * from an SLA document, not from a measured baseline. Normative
 * criteria need no baseline, and so no stable identity, so the whole
 * contract is authored inline at the call site.
 *
 * <p>The chain reads contract-first: open an {@link Contract#inline()},
 * declare the service call and the criteria, then {@code .sampling(...)}
 * says how to exercise it — sample {@code n} times over these inputs.
 * 99% of charges must succeed (Acme Payment SLA v3.2 §4.1); the
 * {@code .satisfies(...)} clause names the failure mode so a breach is
 * attributed to "transaction succeeds" in the verdict rather than to an
 * undifferentiated count.
 *
 * <p>The inline builder offers no empirical path by design. The moment
 * a criterion needs an <em>empirical</em> baseline — a threshold
 * derived from a measured reference rather than asserted from a
 * document — graduate to a named {@code ServiceContract} (see
 * {@code PaymentGatewayServiceContract}, whose body is this same shape
 * lifted into a class with an {@code id()}). A baseline needs identity
 * an anonymous contract cannot carry.
 */
public class PaymentGatewayInlineSlaTest {

	private static final List<Charge> CHARGES = List.of(
			new Charge("tok_visa_4242", 1999L),
			new Charge("tok_mastercard_5555", 2499L),
			new Charge("tok_amex_3782", 3499L),
			new Charge("tok_discover_6011", 999L),
			new Charge("tok_visa_4000", 5999L));
	/**
	 * A SMOKE-sized check — too few samples to <em>verify</em> the SLA,
	 * but enough to catch a catastrophic regression cheaply. A
	 * verification-grade run derives its sample count from the target
	 * via {@code PowerAnalysis.sampleSize(...)}; for a 99% target that
	 * runs to the hundreds, more round-trips than an example should pay.
	 */
	private static final int SMOKE_SAMPLES = 50;

	private static PaymentResult getPaymentResult(Charge charge) {
		PaymentGateway gateway = MockPaymentGateway.instance();
		return gateway.charge(charge.cardToken(), charge.amountCents());
	}

	private static @NonNull Outcome<?> paymentOutcome(PaymentResult r) {
		return r.paymentSucceeded()
				? Outcome.ok()
				: Outcome.fail("transaction-failed", "errorCode=" + r.errorCode());
	}

	@ProbabilisticTest
	void paymentGatewayMeetsItsSla() {
		// a Sampling — the contract plus how to exercise it — ready for PUnit.testing(...)
		var paymentSampling =
				Contract.<Charge, PaymentResult>inline()
						.returning(PaymentGatewayInlineSlaTest::getPaymentResult)
						.passRate(0.99)
						.contractRef(SLA, "Acme Payment SLA v3.2 §4.1")
						.satisfies("transaction succeeds", PaymentGatewayInlineSlaTest::paymentOutcome)
						.sampling(SMOKE_SAMPLES, CHARGES);
		PUnit.testing(paymentSampling)
				.intent(TestIntent.SMOKE)
				.assertPasses();
	}

	/** The per-sample input: a card token plus amount in cents. */
	private record Charge(String cardToken, long amountCents) {
	}
}
