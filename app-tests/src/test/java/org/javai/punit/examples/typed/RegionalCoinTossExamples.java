package org.javai.punit.examples.typed;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.javai.punit.api.Experiment;
import org.javai.punit.api.ProbabilisticTest;
import org.javai.punit.api.ThresholdOrigin;
import org.javai.punit.api.typed.Sampling;
import org.javai.punit.engine.criteria.BernoulliPassRate;
import org.javai.punit.junit5.Punit;

/**
 * Worked example — covariate-aware testing under JUnit.
 *
 * <p>Companion to {@link CoinTossReliabilityExamples}, demonstrating
 * the framework's covariate machinery: a use case declares
 * environmental conditions it's sensitive to, the framework
 * partitions baselines by resolved values, and tests automatically
 * select the matching baseline.
 *
 * <h2>What this demonstrates</h2>
 *
 * <ol>
 *   <li><b>Custom covariate declarations</b> — see
 *       {@link RegionalCoinTossUseCase#covariates()}: a
 *       {@code CONFIGURATION}-category {@code region} covariate
 *       captured at sample time from a system property.</li>
 *   <li><b>Automatic baseline partitioning</b> — measure runs under
 *       different region values produce separate baseline files,
 *       distinguished by per-covariate hashes in the filename
 *       (per EX09).</li>
 *   <li><b>Hard CONFIGURATION gating</b> — a probabilistic test
 *       running under a region with no recorded baseline fails as
 *       INCONCLUSIVE rather than silently using a baseline measured
 *       under a different region.</li>
 *   <li><b>Misalignment surfaced in the verdict</b> — when no
 *       baseline matches, the verdict warning explains <i>why</i>
 *       (e.g. {@code rejected …yaml — CONFIGURATION mismatch on
 *       region (current=APAC, baseline=EU)}).</li>
 * </ol>
 *
 * <h2>Running</h2>
 *
 * <p>Both phases need {@code punit.baseline.dir} pointing at a
 * shared baseline directory and {@code punit.example.region} set to
 * the region under test:
 *
 * <pre>{@code
 * # Phase 1 — establish baselines under each region.
 * ./gradlew experiment -Prun=RegionalCoinTossExamples.measureBaselineEu \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines" \
 *     -Dpunit.example.region=EU
 *
 * ./gradlew experiment -Prun=RegionalCoinTossExamples.measureBaselineApac \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines" \
 *     -Dpunit.example.region=APAC
 *
 * # Phase 2 — verify against the baseline matching the runtime region.
 * ./gradlew test --tests "RegionalCoinTossExamples.shouldMatchBaseline" \
 *     -Dpunit.baseline.dir="$PWD/build/punit/baselines" \
 *     -Dpunit.example.region=EU
 * }</pre>
 *
 * <p>A test run with {@code -Dpunit.example.region=US} (no baseline
 * recorded) demonstrates the INCONCLUSIVE-with-misalignment-notes
 * outcome — useful for showcasing what authors see when their test
 * environment drifts from the measurement environment.
 */
public class RegionalCoinTossExamples {

    private static final RegionalCoinTossUseCase.Bias BIAS_94 =
            new RegionalCoinTossUseCase.Bias(94);

    private static final java.util.List<Integer> CYCLE_1_TO_100 =
            IntStream.rangeClosed(1, 100).boxed()
                    .collect(Collectors.toUnmodifiableList());

    private static Sampling<RegionalCoinTossUseCase.Bias, Integer, String> sampling(int samples) {
        return Sampling.<RegionalCoinTossUseCase.Bias, Integer, String>builder()
                .useCaseFactory(RegionalCoinTossUseCase::new)
                .inputs(CYCLE_1_TO_100)
                .samples(samples)
                .build();
    }

    // ── Phase 1: measure baselines, one per region ─────────────────

    @Experiment
    void measureBaselineEu() {
        // Run with -Dpunit.example.region=EU. The framework resolves
        // region → "EU" once at the start of the run and stamps it
        // into the baseline file's filename + body. A subsequent
        // run under a different region writes to a different file.
        Punit.measuring(sampling(1000), BIAS_94)
                .experimentId("baseline-v1")
                .run();
    }

    @Experiment
    void measureBaselineApac() {
        // Run with -Dpunit.example.region=APAC. Same use case,
        // different region — a separate baseline file results, and
        // tests under region=APAC will match this one rather than
        // the EU baseline.
        Punit.measuring(sampling(1000), BIAS_94)
                .experimentId("baseline-v1")
                .run();
    }

    // ── Phase 2: probabilistic test resolves the matching baseline ─

    @ProbabilisticTest
    void shouldMatchBaseline() {
        // The framework reads punit.example.region at run start,
        // looks for a baseline whose covariate profile matches
        // (region=EU resolves the EU baseline; region=APAC resolves
        // the APAC baseline; region=US would be INCONCLUSIVE since
        // no baseline matches and CONFIGURATION is hard-gated).
        //
        // The criterion's confidence is loosened to 0.50 so the
        // worked example doesn't depend on a tighter Wilson margin
        // — the educational point is covariate-aware lookup, not
        // statistical sizing (see CoinTossReliabilityExamples for
        // the sizing chapter).
        Punit.testing(sampling(50), BIAS_94)
                .criterion(BernoulliPassRate.<String>empirical()
                        .atConfidence(0.50))
                .assertPasses();
    }

    // ── A contractual sibling for comparison ───────────────────────

    @ProbabilisticTest
    void shouldMeetSla() {
        // Contractual tests don't consult a baseline, so covariates
        // play no role here — the threshold is an external SLA. A
        // test running under any region uses the same threshold.
        Punit.testing(sampling(200), BIAS_94)
                .criterion(BernoulliPassRate.<String>meeting(
                        0.90, ThresholdOrigin.SLA))
                .assertPasses();
    }
}
