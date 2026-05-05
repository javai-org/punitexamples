package org.javai.punit.examples.sentinels;

import java.util.List;

import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.PassRate;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase;
import org.javai.punit.examples.usecases.PaymentGatewayUseCase.Charge;
import org.javai.punit.runtime.PUnit;

/**
 * Sentinel-deployable reliability check for the payment-gateway use
 * case.
 *
 * <p>The threshold here is contractual (an SLA) rather than
 * empirical, so there is no measure / baseline step — the test
 * asserts directly against the SLA threshold.
 *
 * <h2>Sentinel deployment</h2>
 *
 * <p>See {@link ShoppingBasketSentinel} for the {@code createSentinel}
 * Gradle workflow. The same fat JAR can bundle both classes; each is
 * registered automatically by virtue of declaring annotated methods.
 *
 * <h2>Why no @Experiment method here</h2>
 *
 * <p>An SLA-driven probabilistic test gates on a threshold expressed
 * in the contract — there is no "baseline" to measure or pair against.
 * The {@code PassRate.meeting(threshold, ThresholdOrigin.SLA)}
 * criterion compares observed pass rate against the SLA target
 * directly. Empirical thresholds (with an accompanying
 * {@code @Experiment} measure step) are demonstrated in
 * {@link ShoppingBasketSentinel}.
 */
public class PaymentGatewaySentinel {

    private static final double SLA_PASS_RATE = 0.99;

    private static final List<Charge> CHARGES = List.of(
            new Charge("tok_visa", 1500),
            new Charge("tok_mastercard", 4200),
            new Charge("tok_amex", 12_000),
            new Charge("tok_diners", 250),
            new Charge("tok_visa_debit", 7500));

    @ProbabilisticTest
    void paymentMeetsContractualSla() {
        PUnit.testing(PaymentGatewayUseCase.sampling(CHARGES, 50))
                .criterion(PassRate.meeting(SLA_PASS_RATE, ThresholdOrigin.SLA))
                .contractRef("Acme Payment SLA v3.2 §4.1")
                .assertPasses();
    }
}
