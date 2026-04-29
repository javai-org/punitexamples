package org.javai.punit.examples.typed;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.junit5.PUnit;

/**
 * Worked example for covariate-aware testing. The use case
 * ({@link RegionalCoinTossUseCase}) declares a
 * {@code CONFIGURATION}-category {@code region} covariate captured
 * at sample time from a system property. Measure runs under
 * different regions produce separate baseline files; tests
 * automatically resolve the matching baseline. A test running
 * under a region with no recorded baseline produces INCONCLUSIVE
 * rather than silently using a baseline from a different region.
 *
 * <h2>Setup</h2>
 *
 * <p>Both phases need {@code punit.baseline.dir} pointing at a
 * shared baseline directory and {@code punit.example.region} set to
 * the region under test.
 *
 * <h2>Running</h2>
 *
 * <pre>{@code
 * # 1. Establish baselines under each region.
 * ./gradlew experiment -Prun=RegionalCoinTossExamples.measureBaselineEu \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines" \
 *     -Dpunit.example.region=EU
 *
 * ./gradlew experiment -Prun=RegionalCoinTossExamples.measureBaselineApac \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines" \
 *     -Dpunit.example.region=APAC
 *
 * # 2. Verify against the baseline matching the runtime region.
 * ./gradlew test --tests "RegionalCoinTossExamples.shouldMatchBaseline" \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines" \
 *     -Dpunit.example.region=EU
 * }</pre>
 *
 * <p>Running the test with {@code -Dpunit.example.region=US} (no
 * recorded baseline) produces an INCONCLUSIVE verdict whose
 * misalignment notes explain which baselines were rejected and why.
 */
public class RegionalCoinTossExamples {

    private static final RegionalCoinTossUseCase.Bias BIAS_94 =
            new RegionalCoinTossUseCase.Bias(94);

    private static final java.util.List<Integer> CYCLE_1_TO_100 =
            IntStream.rangeClosed(1, 100).boxed()
                    .collect(Collectors.toUnmodifiableList());

    @Experiment
    void measureBaselineEu() {
        // Run with -Dpunit.example.region=EU. The framework resolves
        // region → "EU" at run start and stamps it into the baseline
        // file's filename and body.
        PUnit.measuring(RegionalCoinTossUseCase.sampling(CYCLE_1_TO_100, 1000), BIAS_94)
                .experimentId("baseline-v1")
                .run();
    }

    @Experiment
    void measureBaselineApac() {
        // Run with -Dpunit.example.region=APAC. Writes to a separate
        // baseline file from the EU run.
        PUnit.measuring(RegionalCoinTossUseCase.sampling(CYCLE_1_TO_100, 1000), BIAS_94)
                .experimentId("baseline-v1")
                .run();
    }

    @ProbabilisticTest
    void shouldMatchBaseline() {
        // The framework reads punit.example.region at run start and
        // looks for a baseline whose covariate profile matches.
        // Confidence loosened to 0.50 so the assertion doesn't
        // depend on a tight Wilson margin — the focus here is
        // covariate-aware lookup, not statistical sizing.
        PUnit.testing(RegionalCoinTossUseCase.sampling(CYCLE_1_TO_100, 50), BIAS_94)
                // Explicit witness needed when chaining .atConfidence:
                // the chain breaks target-type inference and empirical()
                // would otherwise resolve to BernoulliPassRate<Object>.
                .criterion(BernoulliPassRate.<String>empirical()
                        .atConfidence(0.50))
                .assertPasses();
    }

    @ProbabilisticTest
    void shouldMeetSla() {
        // Contractual: threshold is an external SLA. Covariates play
        // no role — the same threshold applies regardless of region.
        PUnit.testing(RegionalCoinTossUseCase.sampling(CYCLE_1_TO_100, 200), BIAS_94)
                .criterion(BernoulliPassRate.meeting(
                        0.90, ThresholdOrigin.SLA))
                .assertPasses();
    }
}
