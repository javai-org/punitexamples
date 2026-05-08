package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.examples.usecases.PaymentGatewaySampleSizes.CONTRACTUAL_SLA_PASS_RATE;
import static org.javai.punit.examples.usecases.PaymentGatewaySampleSizes.CONTRACTUAL_SLA_SMOKE_SAMPLE_SIZE;
import static org.javai.punit.examples.usecases.PaymentGatewaySampleSizes.INTERNAL_SLO_PASS_RATE;
import static org.javai.punit.examples.usecases.PaymentGatewaySampleSizes.INTERNAL_SLO_VERIFICATION_FLOOR_SAMPLE_SIZE;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.TestIntent;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.PassRate;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase;
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

    @ProbabilisticTest
    void verifiesAgainstInternalSlo() {
        // The pre-flight feasibility gate accepts this configuration
        // because the sample count meets the framework's minimum for
        // a 99% target at default 95% confidence. The default intent
        // (VERIFICATION) is implicit.
        PUnit.testing(PaymentGatewayUseCase.sampling(INTERNAL_SLO_VERIFICATION_FLOOR_SAMPLE_SIZE))
                .criterion(PassRate.meeting(INTERNAL_SLO_PASS_RATE, ThresholdOrigin.SLO))
                .assertPasses();
    }

    @ProbabilisticTest
    void smokeTestsAgainstSla() {
        // The sample count is intentionally too few to verify a
        // 99.99% SLA target. The explicit SMOKE intent tells the
        // framework "I know it's undersized; treat this as a
        // sentinel, not a verification claim." The framework records
        // the sizing gap on the verdict (the pre-flight gate is
        // bypassed in SMOKE mode).
        PUnit.testing(PaymentGatewayUseCase.sampling(CONTRACTUAL_SLA_SMOKE_SAMPLE_SIZE))
                .intent(TestIntent.SMOKE)
                .criterion(PassRate.meeting(CONTRACTUAL_SLA_PASS_RATE, ThresholdOrigin.SLA))
                .assertPasses();
    }
}
