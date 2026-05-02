package org.javai.punit.examples.usecases;

import java.util.List;

import org.javai.outcome.Outcome;
import org.javai.punit.api.ContractBuilder;
import org.javai.punit.api.Sampling;
import org.javai.punit.api.TokenTracker;
import org.javai.punit.api.UseCase;

/**
 * Minimal teaching example — the simplest use case the framework
 * can host, intentionally stripped of domain detail so the focus
 * stays on PUnit mechanics.
 *
 * <p>Think of a biased coin that lands heads N% of the time. A real
 * coin toss takes no input — it is a thunk — but here we thread an
 * input through purely to keep the example deterministic: each input
 * value picks the resulting face. The bias is a threshold in
 * {@code [0, 100]} and an input returns {@code "heads"} when
 * {@code input % 100 < threshold}, so inputs cycling 1..100 land
 * heads on exactly N of every 100 calls.
 *
 * <p>The contract under test is "the result is {@code "heads"}" — so
 * the observed pass rate is the fraction of inputs falling under the
 * threshold. Determinism makes the worked example reproducible: the
 * baseline records an exact observed rate, and the empirical-paired
 * test observes the same rate when run over the same inputs.
 */
public final class CoinTossUseCase implements UseCase<CoinTossUseCase.CoinBias, Integer, String> {

    /**
     * The asymmetry of the (hypothetical) coin — a threshold in
     * {@code [0, 100]} giving the percentage of inputs for which
     * the coin lands heads ({@code input % 100 < threshold}).
     *
     * <p>This refers to the coin's own physical bias — the property
     * that makes an unfair coin favour one face — and not to
     * statistical bias in the estimator sense.
     */
    public record CoinBias(int threshold) {
        public CoinBias {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("threshold must be in [0, 100], got " + threshold);
            }
        }
    }

    private final int threshold;

    public CoinTossUseCase(CoinBias bias) {
        this.threshold = bias.threshold();
    }

    /**
     * Builds a {@link Sampling} for this use case with the triple
     * {@code <CoinBias, Integer, String>} baked in. Tests can call
     * this without spelling out the type parameters in their own
     * signatures.
     */
    public static Sampling<CoinBias, Integer, String> sampling(
            List<Integer> inputs, int samples) {
        return Sampling.of(CoinTossUseCase::new, samples, inputs);
    }

    @Override
    public String id() {
        return "coin-toss";
    }

    @Override
    public void postconditions(ContractBuilder<String> b) { /* none */ }

    @Override
    public Outcome<String> invoke(Integer input, TokenTracker tracker) {
        boolean heads = (input % 100) < threshold;
        return heads
                ? Outcome.ok("heads")
                : Outcome.fail("tails", "expected heads, got tails for input " + input);
    }
}
