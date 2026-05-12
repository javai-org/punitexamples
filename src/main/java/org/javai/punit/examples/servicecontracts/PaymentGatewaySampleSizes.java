package org.javai.punit.examples.servicecontracts;

/**
 * Reliability policy for the {@link PaymentGatewayServiceContract} — the
 * sample sizes and contractual pass-rate targets that the service's
 * probabilistic tests and production sentinel assert against. One
 * file, one source of truth: a dev test and a sentinel run cannot
 * drift onto different SLO numbers, and a reader looking for "what
 * does the gateway actually promise" finds it in one place.
 *
 * <h2>Sample sizes — why are these so small?</h2>
 *
 * <p>The default sample size is deliberately tiny for the examples
 * project — three samples is statistically meaningless for any
 * contractual SLA, but lets a reader run the sentinel end-to-end
 * without waiting on a real payment-gateway round-trip per sample.
 * For a real payment-gateway sentinel verifying against an SLA,
 * derive the minimum sample count from the SLA target rather than
 * picking by feel. {@code PowerAnalysis.sampleSize(...)} computes
 * the floor for a given (pass-rate, MDE, power) triple; the
 * framework's pre-flight feasibility gate also reports the
 * threshold. For an SLA in the 99–99.99% range, expect minimums in
 * the hundreds.
 *
 * <p>Two of the sample-size constants below — the SLO verification
 * floor and the SLA SMOKE size — are kept at the values (268 and
 * 50) that make the example work, rather than scaled down with the
 * rest. The specific numbers are what each example demonstrates:
 * 268 is the smallest sample count the framework will accept for a
 * 99% / 95% verification, and 50 is well below that floor — change
 * either and the example no longer shows what it's meant to.
 *
 * <h2>Pass-rate targets — SLO vs SLA</h2>
 *
 * <p>The two pass-rate constants distinguish an internal Service
 * Level Objective from a contractual Service Level Agreement:
 *
 * <ul>
 *   <li>{@link #INTERNAL_SLO_PASS_RATE} (0.99) — the team's
 *       internal target. Set above the contractual SLA so that a
 *       breach is caught internally before it shows up to the
 *       customer. Tagged {@code ThresholdOrigin.SLO} on the
 *       criterion to record provenance for audit.</li>
 *   <li>{@link #CONTRACTUAL_SLA_PASS_RATE} (0.9999) — the headline
 *       four-nines guarantee in the customer contract. Tagged
 *       {@code ThresholdOrigin.SLA} on the criterion. Verifying a
 *       four-nines target requires sample sizes in the high
 *       hundreds; the example sidesteps this with SMOKE intent (see
 *       {@link #CONTRACTUAL_SLA_SMOKE_SAMPLE_SIZE}).</li>
 * </ul>
 */
public final class PaymentGatewaySampleSizes {

    /**
     * Sample size for ordinary probabilistic tests — verifying the
     * gateway against a contractual SLA threshold. Kept small so the
     * example runs cheaply; a real sentinel derives the minimum from
     * the SLA target via {@code PowerAnalysis.sampleSize(...)}.
     */
    public static final int PROBABILISTIC_TEST_SAMPLE_SIZE = 3;

    /**
     * Minimum sample count to support a verification-grade claim
     * against a 99% pass-rate SLO at the framework's default 95%
     * confidence — i.e. the smallest n that the pre-flight
     * feasibility gate accepts for that target / confidence pair.
     * One less, and the gate would reject the configuration.
     *
     * <p>The {@code verifiesAgainstInternalSlo} example in
     * {@code PaymentGatewaySlaTest} uses this exact value because
     * demonstrating the feasibility floor is the point of that
     * example — replacing 268 with a smaller, "cheaper" sample
     * count would make the example fail the gate instead of
     * showing what the gate accepts.
     */
    public static final int INTERNAL_SLO_VERIFICATION_FLOOR_SAMPLE_SIZE = 268;

    /**
     * Sample count for a SMOKE check against the 99.99% contractual
     * SLA — intentionally too few to support a verification-grade
     * claim. The {@code smokeTestsAgainstSla} example uses this to
     * demonstrate that SMOKE intent bypasses the feasibility gate
     * and records the sizing gap on the verdict.
     *
     * <p>The specific value isn't load-bearing — anything well below
     * the SLA-verification floor would do — but is kept at 50 so a
     * reader can see at a glance that it's an order of magnitude
     * short of what a 99.99% target would require.
     */
    public static final int CONTRACTUAL_SLA_SMOKE_SAMPLE_SIZE = 50;

    /**
     * The team's internal Service Level Objective for the gateway —
     * 99% of charge attempts must succeed. Asserted with
     * {@code ThresholdOrigin.SLO} so the verdict records the
     * provenance for audit. Set above the contractual SLA so an
     * internal monitor flags a regression before the customer-facing
     * guarantee is at risk.
     */
    public static final double INTERNAL_SLO_PASS_RATE = 0.99;

    /**
     * The customer-facing Service Level Agreement for the gateway —
     * the headline four-nines (99.99%) availability guarantee.
     * Asserted with {@code ThresholdOrigin.SLA} so the verdict
     * records the provenance for audit. Verifying this target
     * requires sample sizes in the high hundreds; the example
     * sidesteps the cost with SMOKE intent (see
     * {@link #CONTRACTUAL_SLA_SMOKE_SAMPLE_SIZE}).
     */
    public static final double CONTRACTUAL_SLA_PASS_RATE = 0.9999;

    private PaymentGatewaySampleSizes() {}
}
