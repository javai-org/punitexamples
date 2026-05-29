package org.mavai.punit.examples.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests specific to the example code.
 *
 * <p>The {@code app} package contains classes that represent application
 * code — payment gateways, LLM clients, domain models — and must not depend on
 * any PUnit framework packages. This mirrors the real-world constraint: your
 * application infrastructure is independent of your test framework.
 */
@DisplayName("Example Architecture Rules")
class ExampleArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .importPackages("org.mavai.punit.examples.app");
    }

    @Test
    @DisplayName("Infrastructure must not depend on PUnit framework")
    void infrastructureMustNotDependOnPUnitFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("org.mavai.punit.examples.app..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.mavai.punit.api..",
                        "org.mavai.punit.statistics..",
                        "org.mavai.punit.runtime..",
                        "org.mavai.punit.verdict..",
                        "org.mavai.punit.internal.."
                )
                .because("infrastructure represents application code that is independent of the test framework");

        rule.check(classes);
    }
}
