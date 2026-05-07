package org.javai.punit.examples.integration;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Verifies that the operational flow (explore, optimize, measure) produced
 * the expected artefacts before the probabilistic test runs. Plain JUnit
 * test that inspects generated YAML files; runs as part of the
 * {@code operationalFlowTest} task.
 *
 *
 * <p>Each method skips when no matching artefacts are present — so a
 * standalone {@code ./gradlew test} run (which doesn't drive the
 * operational flow) reports four skipped methods rather than failures
 * against artefacts that never get a chance to be produced.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OperationalFlowVerificationTest {

    private static final String USE_CASE_ID = "shopping-basket";

    private static final Path EXPLORATIONS_DIR =
            Path.of("build/punit/explorations/" + USE_CASE_ID);
    private static final Path OPTIMIZATIONS_DIR =
            Path.of("build/punit/optimizations/" + USE_CASE_ID);
    private static final Path BASELINES_DIR =
            Path.of("src/test/resources/punit/baselines");

    @Test
    @Order(1)
    void verifyExplorationFilesGenerated() throws IOException {
        List<Path> files = listYamls(EXPLORATIONS_DIR, p -> true);
        assumeTrue(!files.isEmpty(),
                "No exploration YAMLs yet — run the EXPLORE experiment first");

        String content = Files.readString(files.getFirst());
        assertThat(content)
                .as("Exploration YAML should contain resultProjection")
                .contains("resultProjection");
    }

    @Test
    @Order(2)
    void verifyOptimizationFilesGenerated() throws IOException {
        List<Path> files = listYamls(OPTIMIZATIONS_DIR, p -> true);
        assumeTrue(!files.isEmpty(),
                "No optimization YAMLs yet — run the OPTIMIZE experiment first");

        String content = Files.readString(files.getFirst());
        assertThat(content)
                .as("Optimization YAML should contain iterations")
                .contains("iterations");
    }

    @Test
    @Order(3)
    void verifyBaselineSpecGenerated() throws IOException {
        List<Path> files = listYamls(BASELINES_DIR,
                p -> p.getFileName().toString().startsWith(USE_CASE_ID + "."));
        assumeTrue(!files.isEmpty(),
                "No baseline YAMLs yet — run the MEASURE experiment first");

        String content = Files.readString(files.getFirst());

        int sampleCount = extractInt(content, "sampleCount");
        assertThat(sampleCount)
                .as("Baseline should record a positive sample count")
                .isGreaterThan(0);

        double observedPassRate = extractDouble(content, "observedPassRate");
        assertThat(observedPassRate)
                .as("Observed pass rate should be in [0, 1]")
                .isBetween(0.0, 1.0);
    }

    @Test
    @Order(4)
    void verifyBaselineObservedPassRateIsReasonable() throws IOException {
        List<Path> files = listYamls(BASELINES_DIR,
                p -> p.getFileName().toString().startsWith(USE_CASE_ID + "."));
        assumeTrue(!files.isEmpty(),
                "No baseline YAMLs yet — run the MEASURE experiment first");

        String content = Files.readString(files.getFirst());
        double observedPassRate = extractDouble(content, "observedPassRate");

        // Mock LLM ≈ 43% pass rate; real LLM (gpt-4o-mini, temp 0.3) ≈ 77%.
        // Either should sit comfortably in [0.30, 1.00].
        assertThat(observedPassRate)
                .as("observedPassRate should be in plausible range for mock or real LLM")
                .isBetween(0.30, 1.00);
    }

    private static List<Path> listYamls(Path dir, java.util.function.Predicate<Path> filter)
            throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.toString().endsWith(".yaml"))
                    .filter(filter)
                    .toList();
        }
    }

    private static double extractDouble(String yaml, String key) {
        Matcher matcher = Pattern.compile(key + ":\\s+([0-9.]+)").matcher(yaml);
        assertThat(matcher.find())
                .as("YAML should contain key: " + key)
                .isTrue();
        return Double.parseDouble(matcher.group(1));
    }

    private static int extractInt(String yaml, String key) {
        Matcher matcher = Pattern.compile(key + ":\\s+(\\d+)").matcher(yaml);
        assertThat(matcher.find())
                .as("YAML should contain key: " + key)
                .isTrue();
        return Integer.parseInt(matcher.group(1));
    }
}
