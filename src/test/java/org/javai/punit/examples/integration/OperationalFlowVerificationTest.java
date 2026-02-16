package org.javai.punit.examples.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Verifies that the operational flow (explore → optimize → measure) produced
 * the expected artifacts before the probabilistic test runs.
 *
 * <p>This test is NOT an experiment and NOT a probabilistic test — it is a plain
 * JUnit test that inspects generated YAML files. It runs as part of the
 * {@code operationalFlowTest} task, after the explore/optimize/measure steps
 * and before the final probabilistic test.
 *
 * <p>By verifying artifacts exist and contain plausible values, this test
 * confirms that each prior step in the flow executed successfully.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OperationalFlowVerificationTest {

    private static final Path EXPLORATIONS_DIR =
            Path.of("src/test/resources/punit/explorations/ShoppingBasketUseCase");
    private static final Path OPTIMIZATIONS_DIR =
            Path.of("src/test/resources/punit/optimizations/ShoppingBasketUseCase");
    private static final Path SPECS_DIR =
            Path.of("src/test/resources/punit/specs");

    @Test
    @Order(1)
    void verifyExplorationFilesGenerated() throws IOException {
        assertThat(EXPLORATIONS_DIR)
                .as("Explorations directory should exist")
                .isDirectory();

        try (Stream<Path> yamlFiles = Files.list(EXPLORATIONS_DIR)
                .filter(p -> p.toString().endsWith(".yaml"))) {
            var files = yamlFiles.toList();
            assertThat(files)
                    .as("At least one exploration YAML should be generated")
                    .isNotEmpty();

            // Check one file contains a resultProjection section
            String content = Files.readString(files.getFirst());
            assertThat(content)
                    .as("Exploration YAML should contain resultProjection")
                    .contains("resultProjection");
        }
    }

    @Test
    @Order(2)
    void verifyOptimizationFilesGenerated() throws IOException {
        assertThat(OPTIMIZATIONS_DIR)
                .as("Optimizations directory should exist")
                .isDirectory();

        try (Stream<Path> yamlFiles = Files.list(OPTIMIZATIONS_DIR)
                .filter(p -> p.toString().endsWith(".yaml"))) {
            var files = yamlFiles.toList();
            assertThat(files)
                    .as("At least one optimization YAML should be generated")
                    .isNotEmpty();

            // Check one file contains an iterations section
            String content = Files.readString(files.getFirst());
            assertThat(content)
                    .as("Optimization YAML should contain iterations")
                    .contains("iterations");
        }
    }

    @Test
    @Order(3)
    void verifyBaselineSpecGenerated() throws IOException {
        assertThat(SPECS_DIR)
                .as("Specs directory should exist")
                .isDirectory();

        try (Stream<Path> specFiles = Files.list(SPECS_DIR)
                .filter(p -> p.getFileName().toString().startsWith("ShoppingBasketUseCase-"))
                .filter(p -> p.toString().endsWith(".yaml"))) {
            var files = specFiles.toList();
            assertThat(files)
                    .as("At least one spec YAML matching ShoppingBasketUseCase-*.yaml should exist")
                    .isNotEmpty();

            String content = Files.readString(files.getFirst());

            // Check minPassRate exists and is in (0, 1)
            double minPassRate = extractDouble(content, "minPassRate");
            assertThat(minPassRate)
                    .as("minPassRate should be between 0 and 1 (exclusive)")
                    .isGreaterThan(0.0)
                    .isLessThan(1.0);

            // Check execution.samplesExecuted = 1000
            int samplesExecuted = extractInt(content, "samplesExecuted");
            assertThat(samplesExecuted)
                    .as("Baseline should have executed 1000 samples")
                    .isEqualTo(1000);

            // Check statistics.successRate.observed > 0
            double observed = extractDouble(content, "observed");
            assertThat(observed)
                    .as("Observed success rate should be greater than 0")
                    .isGreaterThan(0.0);
        }
    }

    @Test
    @Order(4)
    void verifyBaselineMinPassRateIsReasonable() throws IOException {
        try (Stream<Path> specFiles = Files.list(SPECS_DIR)
                .filter(p -> p.getFileName().toString().startsWith("ShoppingBasketUseCase-"))
                .filter(p -> p.toString().endsWith(".yaml"))) {
            var files = specFiles.toList();
            assertThat(files).isNotEmpty();

            String content = Files.readString(files.getFirst());
            double minPassRate = extractDouble(content, "minPassRate");

            // With mock LLM at default temperature, expected pass rate ~43%
            // The minPassRate (lower bound of 95% CI) should be in a plausible range
            assertThat(minPassRate)
                    .as("minPassRate should be in plausible range for mock LLM (0.30–0.55)")
                    .isBetween(0.30, 0.55);
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
