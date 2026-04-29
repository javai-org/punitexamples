package org.javai.punit.examples.typed;

import java.util.List;

import org.javai.punit.api.typed.Sampling;
import org.javai.punit.api.typed.UseCase;
import org.javai.punit.api.typed.UseCaseOutcome;
import org.javai.punit.examples.app.payment.MockPaymentGateway;
import org.javai.punit.examples.app.payment.PaymentGateway;
import org.javai.punit.examples.app.payment.PaymentResult;

/**
 * Typed-API payment-gateway use case.
 *
 * <p>Demonstrates the <b>SLA approach</b> to probabilistic testing,
 * where thresholds come from contractual agreements rather than
 * empirical baselines: the gateway's documented availability and
 * latency targets are external to the test, and the test verifies
 * conformance.
 *
 * <p>The factor record {@link Tier} carries the operating tier the
 * gateway is invoked under. In a real system this would also carry
 * region, configured limits, and other deliberate-choice dimensions
 * the test author varies; here we keep it minimal.
 *
 * <p>The input type is a {@link Charge} record bundling card token
 * and amount. The success-output type is the gateway's
 * {@link PaymentResult}; a sample fails when {@code result.success()}
 * is {@code false}.
 *
 * <h2>Latency assertions</h2>
 *
 * <p>Per-sample duration is captured automatically by the engine on
 * every {@link UseCaseOutcome}. For SLA-style latency assertions
 * ("99% of charges under 1 second"), tests pair the empirical pass-rate
 * criterion with a {@link org.javai.punit.api.typed.spec.PercentileLatency
 * PercentileLatency} criterion via {@code .reportOnly(...)} or
 * {@code .criterion(...)}.
 */
public final class PaymentGatewayUseCase
        implements UseCase<PaymentGatewayUseCase.Tier, PaymentGatewayUseCase.Charge, PaymentResult> {

    /**
     * The factor record. {@code Tier} captures the SLA tier under
     * which the gateway is being verified — different tiers may
     * have different target pass rates / latency limits, and a
     * baseline measured under one tier is not the right reference
     * for a test running under another.
     */
    public record Tier(String name) {

        public static final Tier DEFAULT = new Tier("standard");
    }

    /**
     * The per-sample input. A real gateway integration would carry
     * more fields (currency, customer reference, idempotency key);
     * we keep the worked example minimal.
     */
    public record Charge(String cardToken, long amountCents) { }

    private static final int WARMUP_INVOCATIONS = 3;

    private final PaymentGateway gateway;
    private final Tier tier;

    public PaymentGatewayUseCase(Tier tier) {
        this(MockPaymentGateway.instance(), tier);
    }

    public PaymentGatewayUseCase(PaymentGateway gateway, Tier tier) {
        this.gateway = gateway;
        this.tier = tier;
    }

    @Override
    public String id() {
        return "payment-gateway";
    }

    @Override
    public int warmup() {
        // The legacy use case declared `warmup = 3` to discard the
        // first three responses (cold-start / connection-pool warm).
        return WARMUP_INVOCATIONS;
    }

    @Override
    public UseCaseOutcome<PaymentResult> apply(Charge charge) {
        PaymentResult result;
        try {
            result = gateway.charge(charge.cardToken(), charge.amountCents());
        } catch (RuntimeException e) {
            return UseCaseOutcome.fail("gateway-error", e.getMessage());
        }
        if (!result.success()) {
            return UseCaseOutcome.fail(
                    "transaction-failed",
                    "errorCode=" + result.errorCode());
        }
        return UseCaseOutcome.ok(result);
    }

    /**
     * Builds a {@link Sampling} configured with the
     * {@link MockPaymentGateway} singleton — the worked-example
     * default. Tests that need a custom gateway implementation
     * (a real provider, an alternative mock) supply their own
     * factory closure via {@link Sampling#builder()}.
     */
    public static Sampling<Tier, Charge, PaymentResult> sampling(
            List<Charge> charges, int samples) {
        return Sampling.of(PaymentGatewayUseCase::new, samples, charges);
    }
}
