package org.javai.punit.examples.usecases;

import java.util.List;

import org.javai.outcome.Outcome;
import org.javai.punit.api.ContractBuilder;
import org.javai.punit.api.Sampling;
import org.javai.punit.api.TokenTracker;
import org.javai.punit.api.UseCase;

/**
 * A deterministic stand-in for a probabilistic service: returns
 * {@code "heads"} when the input value satisfies the configured
 * threshold, otherwise {@code "tails"}. The contract under test is
 * "the result is {@code "heads"}" — so the observed pass rate is
 * the fraction of inputs falling under the threshold. Determinism
 * makes the worked example reproducible: the baseline records an
 * exact observed rate, and the empirical-paired test observes the
 * same rate when run over the same inputs.
 */
public final class CoinTossUseCase implements UseCase<CoinTossUseCase.Bias, Integer, String> {

    /**
     * The use-case factor: a threshold in {@code [0, 100]}
     * controlling which inputs return {@code "heads"}
     * ({@code input % 100 < threshold}).
     */
    public record Bias(int threshold) {
        public Bias {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException("threshold must be in [0, 100], got " + threshold);
            }
        }
    }

    private final int threshold;

    public CoinTossUseCase(Bias bias) {
        this.threshold = bias.threshold();
    }

    /**
     * Builds a {@link Sampling} for this use case with the triple
     * {@code <Bias, Integer, String>} baked in. Tests can call this
     * without spelling out the type parameters in their own
     * signatures.
     */
    public static Sampling<Bias, Integer, String> sampling(
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
