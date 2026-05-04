plugins {
    id("java-library")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.36.0"
    id("org.javai.punit")
    idea
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

signing {
    useGpgCmd()
}

group = "org.javai"
version = property("punitExamplesVersion") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Compile with -parameters flag to preserve method parameter names at runtime.
// Required for use case argument injection.
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

repositories {
    mavenCentral()
}

// ═══════════════════════════════════════════════════════════════════════════
// Dependencies
// ═══════════════════════════════════════════════════════════════════════════

dependencies {
    // Domain support — Jackson for JSON parsing in domain classes;
    // Outcome for result types; Log4j2 for logging.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.21.3")
    implementation("org.javai:outcome:0.2.0")
    implementation("org.apache.logging.log4j:log4j-api:2.25.4")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.25.4")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.4")

    // PUnit — author-facing API (UseCase, Contract, Sampling, criteria),
    // engine, statistics, baselines, runtime entry point. JUnit-free so
    // sentinel-deployable classes can call PUnit.testing(...).assertPasses()
    // without dragging the test harness onto the production classpath.
    implementation("org.javai:punit-core:0.6.0")

    // Nullability annotations used by use case postcondition methods.
    implementation("org.jspecify:jspecify:1.0.0")

    // Test stack — PUnit JUnit5 integration (transitively pulls punit-core),
    // JUnit Jupiter, AssertJ, ArchUnit.
    testImplementation("org.javai:punit-junit5:0.6.0")
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Publishing
// ═══════════════════════════════════════════════════════════════════════════

tasks.javadoc {
    options {
        (this as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            charSet = "UTF-8"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "PUnit Examples",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "javai.org",
            "Automatic-Module-Name" to "org.javai.punit.examples"
        )
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("org.javai", "punit-examples", version.toString())

    pom {
        name.set("PUnit Examples")
        description.set("Example applications and probabilistic tests demonstrating the PUnit framework")
        url.set("https://github.com/javai-org/punitexamples")

        licenses {
            license {
                name.set("Attribution Required License (ARL-1.0)")
                url.set("https://github.com/javai-org/punitexamples/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                id.set("mikemannion")
                name.set("Michael Franz Mannion")
                email.set("michaelmannion@me.com")
            }
        }

        scm {
            url.set("https://github.com/javai-org/punitexamples")
            connection.set("scm:git:git://github.com/javai-org/punitexamples.git")
            developerConnection.set("scm:git:ssh://github.com/javai-org/punitexamples.git")
        }
    }
}

tasks.register("publishLocal") {
    description = "Publishes to the local Maven repository"
    group = "publishing"
    dependsOn(tasks.publishToMavenLocal)
}

// ═══════════════════════════════════════════════════════════════════════════
// Operational Flow Integration Test
// ═══════════════════════════════════════════════════════════════════════════
// Runs the full punit operational flow: explore → optimize → measure → verify → test.
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

    System.getProperties()
        .filter { (k, _) -> k.toString().startsWith("punit.") }
        .forEach { (k, v) -> systemProperty(k.toString(), v.toString()) }

    systemProperty("junit.jupiter.conditions.deactivate", "org.junit.*DisabledCondition")

    ignoreFailures = false
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

    System.getProperties()
        .filter { (k, _) -> k.toString().startsWith("punit.") }
        .forEach { (k, v) -> systemProperty(k.toString(), v.toString()) }

    ignoreFailures = false
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

// ═══════════════════════════════════════════════════════════════════════════
// Release Lifecycle
// ═══════════════════════════════════════════════════════════════════════════

fun runCommand(vararg args: String) {
    val process = ProcessBuilder(*args)
        .directory(projectDir)
        .inheritIO()
        .start()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        throw GradleException("Command failed (exit $exitCode): ${args.joinToString(" ")}")
    }
}

fun runCommandAndCapture(vararg args: String): String {
    val process = ProcessBuilder(*args)
        .directory(projectDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    return output.trim()
}

tasks.register("release") {
    description = "Validates, publishes to Maven Central, tags the release, and bumps to next SNAPSHOT"
    group = "publishing"

    doLast {
        val ver = project.property("punitExamplesVersion") as String

        if (ver.endsWith("-SNAPSHOT")) {
            throw GradleException(
                "Cannot release a SNAPSHOT version ($ver). " +
                "Set the release version in gradle.properties first, e.g. punitExamplesVersion=0.2.0"
            )
        }

        val changelog = file("CHANGELOG.md")
        if (!changelog.exists()) {
            throw GradleException("CHANGELOG.md not found. Create it before releasing.")
        }
        val changelogText = changelog.readText()
        if (!changelogText.contains("## [$ver]")) {
            throw GradleException(
                "CHANGELOG.md has no entry for version $ver. " +
                "Add a '## [$ver]' section before releasing."
            )
        }

        val statusOutput = runCommandAndCapture("git", "status", "--porcelain")
        if (statusOutput.isNotEmpty()) {
            throw GradleException(
                "Cannot release with uncommitted changes. Commit or stash them first.\n$statusOutput"
            )
        }

        val tag = "v$ver"
        logger.lifecycle("Creating tag $tag...")
        runCommand("git", "tag", "-a", tag, "-m", "Release $ver")

        logger.lifecycle("Publishing $ver to Maven Central...")
        try {
            runCommand("./gradlew", ":publishAndReleaseToMavenCentral")
        } catch (e: Exception) {
            logger.lifecycle("Publishing failed — removing local tag $tag")
            runCommand("git", "tag", "-d", tag)
            throw e
        }

        logger.lifecycle("Pushing tag $tag to origin...")
        runCommand("git", "push", "origin", tag)

        val parts = ver.split(".")
        val nextPatch = parts[2].toInt() + 1
        val nextVersion = "${parts[0]}.${parts[1]}.$nextPatch-SNAPSHOT"
        logger.lifecycle("Bumping version to $nextVersion...")

        val rootProps = file("gradle.properties")
        rootProps.writeText(rootProps.readText().replace("punitExamplesVersion=$ver", "punitExamplesVersion=$nextVersion"))

        runCommand("git", "add", "gradle.properties")
        runCommand("git", "commit", "-m", "Bump version to $nextVersion")
        runCommand("git", "push")

        logger.lifecycle("Release $ver complete. Version bumped to $nextVersion.")
    }
}

tasks.register("tagRelease") {
    description = "Creates and pushes a release tag for a given version (e.g. -PreleaseVersion=0.1.0)"
    group = "publishing"

    doLast {
        val ver = project.findProperty("releaseVersion") as String?
            ?: throw GradleException("Specify -PreleaseVersion=<version>, e.g. ./gradlew tagRelease -PreleaseVersion=0.1.0")

        val tag = "v$ver"
        val commitish = (project.findProperty("commitish") as String?) ?: "HEAD"

        logger.lifecycle("Creating tag $tag at $commitish...")
        runCommand("git", "tag", "-a", tag, commitish, "-m", "Release $ver")

        logger.lifecycle("Pushing tag $tag to origin...")
        runCommand("git", "push", "origin", tag)

        logger.lifecycle("Tag $tag created and pushed.")
    }
}
