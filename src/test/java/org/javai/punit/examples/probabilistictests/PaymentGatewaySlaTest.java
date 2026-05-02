package org.javai.punit.examples.probabilistictests;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.TestIntent;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.PassRate;
import org.javai.punit.examples.app.payment.PaymentResult;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase.Charge;
import org.javai.punit.runtime.PUnit;

/**
 * Probabilistic tests for payment-gateway reliability against a
 * contractual SLA threshold, demonstrating how {@link TestIntent}
 * interacts with sample sizing and threshold targets.
 *
 * <h2>Intent modes</h2>
 *
 * <ul>
 *   <li>{@link TestIntent#VERIFICATION} (default) — sample size is
 *       sufficient for the target pass rate, so the framework can
 *       provide statistical evidence that the SUT meets the
 *       threshold. Configurations undersized for the target are
 *       rejected pre-flight.</li>
 *   <li>{@link TestIntent#SMOKE} — sample size is intentionally
 *       small relative to the target. Catches catastrophic
 *       regressions quickly but does not claim statistical
 *       verification; the verdict records the sizing gap.</li>
 * </ul>
 *
 * <p>The {@link ThresholdOrigin} on the contractual criterion records
 * where the threshold came from — SLA, SLO, POLICY — for audit
 * traceability and surfaces in the verdict reports.
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
        PUnit.<Void, Charge, PaymentResult>testing(
                        PaymentGatewayUseCase.sampling(CHARGES, 268), null)
                .criterion(PassRate.meeting(0.99, ThresholdOrigin.SLO))
                .assertPasses();
    }

    @ProbabilisticTest
    void smokeTestsAgainstSla() {
        // 50 samples is too few to verify a 99.99% SLA target. The
        // explicit SMOKE intent tells the framework "I know it's
        // undersized; treat this as a sentinel, not a verification
        // claim." The framework records the sizing gap on the
        // verdict (the pre-flight gate is bypassed in SMOKE mode).
        PUnit.<Void, Charge, PaymentResult>testing(
                        PaymentGatewayUseCase.sampling(CHARGES, 50), null)
                .intent(TestIntent.SMOKE)
                .criterion(PassRate.meeting(0.9999, ThresholdOrigin.SLA))
                .assertPasses();
    }
}
