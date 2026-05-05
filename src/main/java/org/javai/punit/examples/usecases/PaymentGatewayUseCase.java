package org.javai.punit.examples.usecases;

import java.util.List;

import org.javai.outcome.Outcome;
import org.javai.punit.api.ContractBuilder;
import org.javai.punit.api.NoFactors;
import org.javai.punit.api.Sampling;
import org.javai.punit.api.TokenTracker;
import org.javai.punit.api.UseCase;
import org.javai.punit.api.UseCaseOutcome;
import org.javai.punit.examples.app.payment.MockPaymentGateway;
import org.javai.punit.examples.app.payment.PaymentGateway;
import org.javai.punit.examples.app.payment.PaymentResult;

/**
 * Payment-gateway use case demonstrating the SLA approach to
 * probabilistic testing: thresholds come from contractual agreements
 * rather than empirical baselines.
 *
 * <p>This use case has no varying factors, so {@code FT} is
 * {@link NoFactors} — punit's empty factor record, paired with the
 * no-arg {@code PUnit.testing(sampling)} / {@code PUnit.measuring(sampling)}
 * overloads at the call site. The input is a {@link Charge} record
 * bundling card token and amount; the output is the gateway's
 * {@link PaymentResult}. The contract has a
 * single postcondition — "transaction succeeds" — so a sample's
 * failure mode is attributed to a named clause in the verdict's
 * failure histogram, rather than to an undifferentiated count.
 *
 * <p>Note the split: {@code invoke} is primitive — it calls the
 * gateway and returns the result. The {@link PaymentGateway}
 * contract surfaces transactional failures as
 * {@code PaymentResult.success() == false}, never as a thrown
 * exception, so {@code invoke} doesn't need a try/catch. The
 * judgement on the returned result lives in
 * {@code postconditions(...)}.
 *
 * <p>Per-sample duration is captured automatically by the engine on
 * every {@link UseCaseOutcome}. For SLA-style latency assertions
 * ("99% of charges under 1 second"), tests pair the empirical pass-rate
 * criterion with a {@link org.javai.punit.api.spec.PercentileLatency
 * PercentileLatency} criterion via {@code .reportOnly(...)} or
 * {@code .criterion(...)}.
 */
public final class PaymentGatewayUseCase
        implements UseCase<NoFactors, PaymentGatewayUseCase.Charge, PaymentResult> {

    /** The per-sample input: a card token plus amount in cents. */
    public record Charge(String cardToken, long amountCents) { }

    private static final int WARMUP_INVOCATIONS = 3;

    private final PaymentGateway gateway;

    public PaymentGatewayUseCase() {
        this(MockPaymentGateway.instance());
    }

    public PaymentGatewayUseCase(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public String id() {
        return "payment-gateway";
    }

    @Override
    public void postconditions(ContractBuilder<PaymentResult> b) {
        // A very simple contract for the payment gateway. Either the payment succeeds or it doesn't.
        b.ensure("transaction succeeds", r -> r.success()
                ? Outcome.ok()
                : Outcome.fail("transaction-failed", "errorCode=" + r.errorCode()));
    }

    @Override
    public int warmup() {
        // Cold-start, connection-pool fill, and unwarmed caches inflate the
        // latency of the first few invocations; discarding them keeps those
        // outliers out of percentile latency measurements (e.g. P99). The
        // same discard preserves the i.i.d. assumption behind the Bernoulli
        // pass-rate criterion — cold-call failures are not identically
        // distributed with steady-state ones.
        return WARMUP_INVOCATIONS;
    }

    @Override
    public Outcome<PaymentResult> invoke(Charge charge, TokenTracker tracker) {
        return Outcome.ok(gateway.charge(charge.cardToken(), charge.amountCents()));
    }

    /**
     * Builds a {@link Sampling} configured with the
     * {@link MockPaymentGateway} singleton. Tests that need a
     * different gateway implementation supply their own factory
     * closure via {@link Sampling#builder()}.
     */
    public static Sampling<NoFactors, Charge, PaymentResult> sampling(List<Charge> charges, int samples) {
        return Sampling.of(nf -> new PaymentGatewayUseCase(), samples, charges);
    }
}
