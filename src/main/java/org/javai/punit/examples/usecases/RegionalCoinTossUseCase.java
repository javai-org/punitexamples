package org.javai.punit.examples.usecases;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.javai.outcome.Outcome;
import org.javai.punit.api.CovariateCategory;
import org.javai.punit.api.ContractBuilder;
import org.javai.punit.api.Sampling;
import org.javai.punit.api.TokenTracker;
import org.javai.punit.api.UseCase;
import org.javai.punit.api.covariate.Covariate;

/**
 * A region-aware variant of {@link CoinTossUseCase} that declares a
 * custom {@code region} covariate. Behaviour is the same as
 * {@link CoinTossUseCase}: deterministic {@code "heads"}/{@code "tails"}
 * based on the configured threshold.
 *
 * <p>The {@code region} value is captured at sample time from the
 * {@code punit.example.region} system property. The covariate is
 * declared with category {@link CovariateCategory#CONFIGURATION},
 * which the framework hard-gates: a baseline measured under
 * {@code region=EU} cannot silently match a test running under
 * {@code region=APAC}.
 */
public final class RegionalCoinTossUseCase
        implements UseCase<RegionalCoinTossUseCase.Bias, Integer, String> {

    /** System property the resolver reads to capture the current region. */
    public static final String REGION_PROPERTY = "punit.example.region";

    /** Default value when the property is unset. */
    public static final String UNSET_REGION = "UNSET";

    public record Bias(int threshold) {
        public Bias {
            if (threshold < 0 || threshold > 100) {
                throw new IllegalArgumentException(
                        "threshold must be in [0, 100], got " + threshold);
            }
        }
    }

    private final int threshold;

    public RegionalCoinTossUseCase(Bias bias) {
        this.threshold = bias.threshold();
    }

    /**
     * Builds a {@link Sampling} for this use case with the triple
     * {@code <Bias, Integer, String>} baked in.
     */
    public static Sampling<Bias, Integer, String> sampling(
            List<Integer> inputs, int samples) {
        return Sampling.of(RegionalCoinTossUseCase::new, samples, inputs);
    }

    @Override
    public String id() {
        return "regional-coin-toss";
    }

    @Override
    public void postconditions(ContractBuilder<String> b) { /* none */ }

    @Override
    public List<Covariate> covariates() {
        return List.of(Covariate.custom("region", CovariateCategory.CONFIGURATION));
    }

    @Override
    public Map<String, Supplier<String>> customCovariateResolvers() {
        return Map.of("region",
                () -> System.getProperty(REGION_PROPERTY, UNSET_REGION));
    }

    @Override
    public Outcome<String> invoke(Integer input, TokenTracker tracker) {
        boolean heads = (input % 100) < threshold;
        return heads
                ? Outcome.ok("heads")
                : Outcome.fail("tails",
                        "expected heads, got tails for input " + input);
    }
}
