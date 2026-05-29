package org.mavai.punit.examples.servicecontracts;

/**
 * Sample-size policy for the {@link ShoppingBasketServiceContract}. One set
 * of values, shared across the service's measure experiments,
 * probabilistic tests, EXPLORE / OPTIMIZE experiments, and the
 * production sentinel. Keeping the policy in one place means a dev
 * run, a CI run, and a sentinel run all sample at the same scale —
 * the recorded baselines and the verifications against them stay
 * commensurate.
 *
 * <h2>Why are these so small?</h2>
 *
 * <p>The values here are deliberately tiny — far below any
 * statistical-rigour threshold — for one reason: this is an
 * examples project. A reader exploring the project should be able
 * to run any of the experiments or tests end-to-end against a real
 * LLM without burning through a tier of API credits or waiting
 * minutes per call. Three samples in a probabilistic test is
 * statistically meaningless; the example demonstrates the
 * <em>configuration</em>, not the verdict.
 *
 * <h2>Realistic values</h2>
 *
 * <p>For a real shopping-basket service, scale these up
 * substantially:
 *
 * <ul>
 *   <li><b>Measure experiments / sentinel baselines:</b> 200+
 *       samples for an in-CI baseline; 1000+ for a sentinel
 *       baseline that many later verifications resolve through.
 *       The baseline's statistical power directly bounds the
 *       precision of every downstream test.</li>
 *   <li><b>Probabilistic tests / sentinel verifications:</b> 20+
 *       samples for a workable Wilson-score margin against an
 *       empirical baseline; SLA verification with a high target
 *       (e.g. 99% pass rate) often needs hundreds — derive the
 *       minimum from {@code PowerAnalysis.sampleSize(...)} or let
 *       the framework's feasibility gate tell you.</li>
 *   <li><b>EXPLORE experiments:</b> 50+ samples per grid cell so
 *       per-configuration pass rates separate cleanly above
 *       noise.</li>
 *   <li><b>OPTIMIZE experiments:</b> 30+ samples per iteration so
 *       the per-iteration score has enough signal to drive the
 *       stepper toward a real optimum rather than chasing sample
 *       noise.</li>
 * </ul>
 *
 * <p>A handful of probabilistic-test examples deliberately keep
 * their literal sample counts — they exist to demonstrate something
 * about that specific number (e.g. a 5-sample configuration that
 * the feasibility gate rejects). Those sites comment the literal in
 * place.
 */
public final class ShoppingBasketSampleSizes {

    /**
     * Sample size for ordinary measure experiments — recording a
     * pass-rate / latency baseline that paired probabilistic tests
     * later verify against. Big enough that an occasional transient
     * LLM failure surfaces in the recorded rate (so confidence-first
     * planning has signal to plan against), small enough that a
     * curious reader can run the experiment end-to-end without
     * burning a tier of API credits. A real shopping-basket baseline
     * uses 200+.
     */
    public static final int MEASURE_EXPERIMENT_SAMPLE_SIZE = 30;

    /**
     * Sample size for ordinary probabilistic tests — verifying the
     * service contract against either an empirical baseline or a contractual
     * threshold. Kept small so the example runs cheaply against a
     * real LLM; a real verification uses 20+.
     */
    public static final int PROBABILISTIC_TEST_SAMPLE_SIZE = 3;

    /**
     * Sample size per grid cell in EXPLORE experiments. Kept small
     * so the example runs cheaply against a real LLM; a real EXPLORE
     * run uses 50+ per cell.
     */
    public static final int EXPLORE_PER_CONFIG_SAMPLE_SIZE = 5;

    /**
     * Sample size per iteration in OPTIMIZE experiments. Kept small
     * so the example runs cheaply against a real LLM; a real
     * OPTIMIZE run uses 30+ per iteration.
     */
    public static final int OPTIMIZE_PER_ITERATION_SAMPLE_SIZE = 5;

    private ShoppingBasketSampleSizes() {}
}
