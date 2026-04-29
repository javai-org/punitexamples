package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.TestIntent;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.examples.typed.PaymentGatewayUseCase;
import org.javai.punit.examples.typed.PaymentGatewayUseCase.Charge;
import org.javai.punit.examples.typed.PaymentGatewayUseCase.Tier;
import org.javai.punit.junit5.Punit;

/**
 * Probabilistic tests for payment-gateway reliability against a
 * contractual SLA threshold, demonstrating how {@link TestIntent}
 * interacts with sample sizing and threshold targets.
 *
 * <h2>Intent modes</h2>
 *
 * <ul>
 *   <li>{@link TestIntent#VERIFICATION} — sample size is sufficient
 *       for the target pass rate, so the framework can provide
 *       statistical evidence that the SUT meets the threshold.
 *       This is the default; if the configuration is undersized for
 *       the target, the framework rejects it pre-flight (the
 *       feasibility gate, see CV-feasibility / punit#77).</li>
 *   <li>{@link TestIntent#SMOKE} — sample size is intentionally
 *       small relative to the target. Acts as a sentinel: catches
 *       catastrophic regressions quickly but does not claim
 *       statistical verification. The verdict notes the sizing gap
 *       so reviewers can see the caveat.</li>
 * </ul>
 *
 * <h2>Provenance</h2>
 *
 * <p>The {@link ThresholdOrigin} on the contractual criterion records
 * <em>where</em> the threshold came from — SLA, SLO, POLICY — for
 * audit traceability. Doesn't affect verdict logic; surfaces in
 * verdict reports and the YAML/XML output.
 */
public class PaymentGatewaySlaTest {

    private static final List<Charge> CHARGES = List.of(
            new Charge("tok_visa_4242", 1999L),
            new Charge("tok_mastercard_5555", 2499L),
            new Charge("tok_amex_3782", 3499L),
            new Charge("tok_discover_6011", 999L),
            new Charge("tok_visa_4000", 5999L));

    @ProbabilisticTest
    void verifiesAgainstInternalSlo() {
        // 268 samples is the minimum that supports a verification-
        // grade claim against a 99% target at default 95% confidence
        // — the framework's pre-flight feasibility gate is satisfied.
        // The default intent (VERIFICATION) is implicit.
        Punit.testing(PaymentGatewayUseCase.sampling(CHARGES, 268), Tier.DEFAULT)
                .criterion(BernoulliPassRate.meeting(0.99, ThresholdOrigin.SLO))
                .assertPasses();
    }

    @ProbabilisticTest
    void smokeTestsAgainstSla() {
        // 50 samples is too few to verify a 99.99% SLA target. The
        // explicit SMOKE intent tells the framework "I know it's
        // undersized; treat this as a sentinel, not a verification
        // claim." The framework records the sizing gap on the
        // verdict (the pre-flight gate is bypassed in SMOKE mode).
        Punit.testing(PaymentGatewayUseCase.sampling(CHARGES, 50), Tier.DEFAULT)
                .intent(TestIntent.SMOKE)
                .criterion(BernoulliPassRate.meeting(0.9999, ThresholdOrigin.SLA))
                .assertPasses();
    }
}
