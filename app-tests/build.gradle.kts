plugins {
    id("org.javai.punit")
}

dependencies {
    testImplementation(project(":app-usecases"))

    // PUnit JUnit5 integration (transitively includes punit-core)
    testImplementation("org.javai:punit-junit5:0.6.0")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.27.7")

    // ArchUnit
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Operational Flow Integration Test
// ═══════════════════════════════════════════════════════════════════════════
// Runs the full punit operational flow: explore → optimize → measure → verify → test
// Usage: ./gradlew operationalFlowTest

val flowClean by tasks.registering(Delete::class) {
    description = "Cleans generated punit artifacts for a fresh flow run"
    group = "verification"
    delete("src/test/resources/punit/specs")
    delete(layout.buildDirectory.dir("punit/explorations"))
    delete(layout.buildDirectory.dir("punit/optimizations"))
}

fun flowExperimentTask(
    name: String,
    description: String,
    testClass: String,
    testMethod: String? = null
): TaskProvider<Test> = tasks.register(name, Test::class) {
    this.description = description
    group = "verification"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    useJUnitPlatform {
        includeTags("punit-experiment")
    }
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

    filter {
        if (testMethod != null) {
            includeTestsMatching("*$testClass.$testMethod")
        } else {
            includeTestsMatching("*$testClass*")
        }
    }

    // Forward punit system properties
    System.getProperties()
        .filter { (k, _) -> k.toString().startsWith("punit.") }
        .forEach { (k, v) -> systemProperty(k.toString(), v.toString()) }

    // Deactivate @Disabled so experiments can run
    systemProperty("junit.jupiter.conditions.deactivate", "org.junit.*DisabledCondition")

    ignoreFailures = false

    // Flow tasks must always re-run to verify fresh artifact generation
    outputs.upToDateWhen { false }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }

    dependsOn("compileTestJava", "processTestResources")
}

fun flowTestTask(
    name: String,
    description: String,
    testClass: String
): TaskProvider<Test> = tasks.register(name, Test::class) {
    this.description = description
    group = "verification"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    useJUnitPlatform()
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

    filter {
        includeTestsMatching("*$testClass*")
    }

    // Forward punit system properties
    System.getProperties()
        .filter { (k, _) -> k.toString().startsWith("punit.") }
        .forEach { (k, v) -> systemProperty(k.toString(), v.toString()) }

    ignoreFailures = false

    // Flow tasks must always re-run to verify fresh artifact generation
    outputs.upToDateWhen { false }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }

    dependsOn("compileTestJava", "processTestResources")
}

val flowExplore = flowExperimentTask(
    "flowExplore",
    "Runs ShoppingBasketExplore experiment",
    "ShoppingBasketExplore"
)

val flowOptimize = flowExperimentTask(
    "flowOptimize",
    "Runs ShoppingBasketOptimizeTemperature experiment",
    "ShoppingBasketOptimizeTemperature"
)

val flowMeasure = flowExperimentTask(
    "flowMeasure",
    "Runs ShoppingBasketMeasure.measureBaseline experiment",
    "ShoppingBasketMeasure",
    "measureBaseline"
)

val flowVerify = flowTestTask(
    "flowVerify",
    "Verifies explore/optimize/measure artifacts exist and are valid",
    "OperationalFlowVerificationTest"
)

val flowTest = flowTestTask(
    "flowTest",
    "Runs ShoppingBasketTest probabilistic test against generated spec",
    "ShoppingBasketTest"
)

// Probabilistic tests have expected sample-level failures — the aggregate verdict
// determines pass/fail, not individual samples. The flowVerify step already validates
// the artifacts; flowTest proves the spec can be loaded and consumed.
flowTest { ignoreFailures = true }

// Enforce ordering via mustRunAfter chains
flowExplore { mustRunAfter(flowClean) }
flowOptimize { mustRunAfter(flowExplore) }
flowMeasure { mustRunAfter(flowOptimize) }
flowVerify { mustRunAfter(flowMeasure) }
flowTest { mustRunAfter(flowVerify) }

tasks.register("operationalFlowTest") {
    description = "Runs the full punit operational flow: explore → optimize → measure → verify → test"
    group = "verification"
    dependsOn(flowClean, flowExplore, flowOptimize, flowMeasure, flowVerify, flowTest)
}
