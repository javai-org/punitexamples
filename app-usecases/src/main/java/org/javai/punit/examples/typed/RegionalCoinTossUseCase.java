package org.javai.punit.examples.typed;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.javai.punit.api.CovariateCategory;
import org.javai.punit.api.typed.Sampling;
import org.javai.punit.api.typed.UseCase;
import org.javai.punit.api.typed.UseCaseOutcome;
import org.javai.punit.api.typed.covariate.Covariate;

/**
 * A region-aware variant of {@link CoinTossUseCase} that declares a
 * custom {@code region} covariate.
 *
 * <p>Behaviour is the same as {@link CoinTossUseCase}: deterministic
 * {@code "heads"}/{@code "tails"} based on the configured threshold.
 * The point of this class is to demonstrate the
 * <em>covariate-aware authoring pattern</em>: the use case knows
 * what environmental conditions it is sensitive to ({@code region}
 * here, but in real use cases this is typically the LLM model, the
 * deployment region, or a feature flag), and the framework
 * automatically partitions baselines by the resolved values.
 *
 * <p>The {@code region} value is captured at sample time from the
 * {@code punit.example.region} system property — a stand-in for any
 * runtime resolution mechanism (environment variable, configuration
 * service, deployment metadata).
 *
 * <p>{@link CovariateCategory#CONFIGURATION} is the right category
 * here: the region is a deliberate choice that explains behaviour
 * differences. The framework hard-gates CONFIGURATION mismatches —
 * a baseline measured under {@code region=EU} cannot silently match
 * a test running under {@code region=APAC}.
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
    public List<Covariate> covariates() {
        return List.of(Covariate.custom("region", CovariateCategory.CONFIGURATION));
    }

    @Override
    public Map<String, Supplier<String>> customCovariateResolvers() {
        return Map.of("region",
                () -> System.getProperty(REGION_PROPERTY, UNSET_REGION));
    }

    @Override
    public UseCaseOutcome<String> apply(Integer input) {
        boolean heads = (input % 100) < threshold;
        return heads
                ? UseCaseOutcome.ok("heads")
                : UseCaseOutcome.fail("tails",
                        "expected heads, got tails for input " + input);
    }
}
