plugins {
    id("java-library")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.36.0"
    id("org.javai.punit")
    kotlin("jvm") version "2.2.0"
    idea
}

// Configure IDEA to download sources and javadoc
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
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Compile with -parameters flag to preserve method parameter names at runtime
// This is required for use case argument injection
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

repositories {
    mavenCentral()
}

dependencies {
    // Jackson - for JSON/CSV parsing in example app code
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.21.0")

    // Outcome - result types for contract postconditions
    implementation("org.javai:outcome:0.1.0")

    // Log4j2 - logging
    implementation("org.apache.logging.log4j:log4j-api:2.25.3")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.25.3")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.3")

    // PUnit framework
    testImplementation("org.javai:punit:0.2.0-SNAPSHOT")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.14.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.27.7")

    // ArchUnit
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    // Log4j2 core - needed by CatalogueMarkdownAppender for verdict catalog generation
    testImplementation("org.apache.logging.log4j:log4j-core:2.25.3")
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Maven Central Publishing
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

// Convenience task to build and publish locally
tasks.register("publishLocal") {
    description = "Publishes to the local Maven repository"
    group = "publishing"
    dependsOn(tasks.publishToMavenLocal)
}

// ═══════════════════════════════════════════════════════════════════════════
// Scripts source set (Kotlin) — verdict catalog generation
// ═══════════════════════════════════════════════════════════════════════════

val scripts by sourceSets.creating {
    kotlin.srcDir("src/scripts/kotlin")
}

dependencies {
    "scriptsImplementation"(kotlin("stdlib"))
}

// ═══════════════════════════════════════════════════════════════════════════
// Verdict Catalog Generation
// ═══════════════════════════════════════════════════════════════════════════

val verdictCatalogueSummary by tasks.registering(Test::class) {
    description = "Generates SUMMARY verdict catalogue (build/verdict-catalogue-SUMMARY.md)"
    group = "documentation"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    useJUnitPlatform()

    // Probabilistic tests are expected to have sample-level failures — the verdict
    // is determined by the aggregate pass rate, not individual samples.
    ignoreFailures = true

    systemProperty("punit.stats.detailLevel", "SUMMARY")
    systemProperty("junit.jupiter.conditions.deactivate", "org.junit.*DisabledCondition")

    filter {
        includeTestsMatching("*VerdictCatalogueTest*")
    }

    dependsOn("compileTestJava", "processTestResources")
}

val verdictCatalogueVerbose by tasks.registering(Test::class) {
    description = "Generates VERBOSE verdict catalogue (build/verdict-catalogue-VERBOSE.md)"
    group = "documentation"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    useJUnitPlatform()

    // Probabilistic tests are expected to have sample-level failures — the verdict
    // is determined by the aggregate pass rate, not individual samples.
    ignoreFailures = true

    systemProperty("punit.stats.detailLevel", "VERBOSE")
    systemProperty("junit.jupiter.conditions.deactivate", "org.junit.*DisabledCondition")

    filter {
        includeTestsMatching("*VerdictCatalogueTest*")
    }

    dependsOn("compileTestJava", "processTestResources")
}

val generateVerdictCatalog by tasks.registering(JavaExec::class) {
    description = "Generates docs/VERDICT-CATALOG.md from SUMMARY and VERBOSE catalogues"
    group = "documentation"

    classpath = scripts.runtimeClasspath
    mainClass.set("org.javai.punit.examples.scripts.GenerateVerdictCatalogKt")

    dependsOn(verdictCatalogueSummary, verdictCatalogueVerbose)
}
