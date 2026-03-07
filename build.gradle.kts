plugins {
    id("java-library")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.36.0"
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
version = property("punitExamplesVersion") as String

subprojects {
    apply(plugin = "java-library")

    group = rootProject.group
    version = rootProject.version

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // Compile with -parameters flag to preserve method parameter names at runtime
    // This is required for use case argument injection
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    repositories {
        mavenCentral()
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

        // 1. Validate not a SNAPSHOT
        if (ver.endsWith("-SNAPSHOT")) {
            throw GradleException(
                "Cannot release a SNAPSHOT version ($ver). " +
                "Set the release version in gradle.properties first, e.g. punitExamplesVersion=0.2.0"
            )
        }

        // 2. Validate CHANGELOG.md has an entry for this version
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

        // 3. Validate clean git state
        val statusOutput = runCommandAndCapture("git", "status", "--porcelain")
        if (statusOutput.isNotEmpty()) {
            throw GradleException(
                "Cannot release with uncommitted changes. Commit or stash them first.\n$statusOutput"
            )
        }

        // 4. Create annotated tag locally (before publish, so a successful publish always has a tag)
        val tag = "v$ver"
        logger.lifecycle("Creating tag $tag...")
        runCommand("git", "tag", "-a", tag, "-m", "Release $ver")

        // 5. Publish to Maven Central (colon prefix scopes to root project only)
        logger.lifecycle("Publishing $ver to Maven Central...")
        try {
            runCommand("./gradlew", ":publishAndReleaseToMavenCentral")
        } catch (e: Exception) {
            logger.lifecycle("Publishing failed — removing local tag $tag")
            runCommand("git", "tag", "-d", tag)
            throw e
        }

        // 6. Push tag (artifact is published, so the tag must reach the remote)
        logger.lifecycle("Pushing tag $tag to origin...")
        runCommand("git", "push", "origin", tag)

        // 7. Bump to next SNAPSHOT
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
