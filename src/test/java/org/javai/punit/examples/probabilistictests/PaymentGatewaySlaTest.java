package org.javai.punit.examples.probabilistictests;

import static org.javai.punit.examples.servicecontracts.PaymentGatewaySampleSizes.CONTRACTUAL_SLA_SMOKE_SAMPLE_SIZE;
import static org.javai.punit.examples.servicecontracts.PaymentGatewaySampleSizes.INTERNAL_SLO_VERIFICATION_FLOOR_SAMPLE_SIZE;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.TestIntent;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.examples.servicecontracts.PaymentGatewayServiceContract;
import org.javai.punit.runtime.PUnit;
import org.junit.jupiter.api.Disabled;

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

    // Both test methods exercise test-side override of the contract's
    // declared SLA posture (the contract bakes in
    // ThresholdOrigin.SLA at 0.99). The override mechanism is the
    // subject of a follow-on directive
    // (DIR-CRITERIA-OVERRIDE-punit); these tests are re-enabled when
    // it lands.

    @Disabled("awaiting DIR-CRITERIA-OVERRIDE-punit: test-side override of contract posture")
    @ProbabilisticTest
    void verifiesAgainstInternalSlo() {
        // The pre-flight feasibility gate accepts this configuration
        // because the sample count meets the framework's minimum for
        // a 99% target at default 95% confidence. The default intent
        // (VERIFICATION) is implicit.
        PUnit.testing(PaymentGatewayServiceContract.sampling(INTERNAL_SLO_VERIFICATION_FLOOR_SAMPLE_SIZE))
                .assertPasses();
    }

    @Disabled("awaiting DIR-CRITERIA-OVERRIDE-punit: test-side override of contract posture")
    @ProbabilisticTest
    void smokeTestsAgainstSla() {
        // The sample count is intentionally too few to verify a
        // 99.99% SLA target. The explicit SMOKE intent tells the
        // framework "I know it's undersized; treat this as a
        // sentinel, not a verification claim." The framework records
        // the sizing gap on the verdict (the pre-flight gate is
        // bypassed in SMOKE mode).
        PUnit.testing(PaymentGatewayServiceContract.sampling(CONTRACTUAL_SLA_SMOKE_SAMPLE_SIZE))
                .intent(TestIntent.SMOKE)
                .assertPasses();
    }
}
