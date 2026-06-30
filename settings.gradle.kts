pluginManagement {
    // Resolve the PUnit Gradle plugin from the local punit build when available
    val punitPluginDir = file("../punit/punit-gradle-plugin")
    if (punitPluginDir.isDirectory) {
        includeBuild(punitPluginDir)
    }

    // Allow resolving the PUnit plugin from Maven Central (used when local build is absent)
    plugins {
        id("org.mavai.punit") version "0.9.3"
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "punitexamples"

// Use local punit when available (sibling folder), Maven Central otherwise
val punitDir = file("../punit")
if (punitDir.isDirectory) {
    includeBuild(punitDir) {
        dependencySubstitution {
            substitute(module("org.mavai:punit")).using(project(":"))
            substitute(module("org.mavai:punit-core")).using(project(":punit-core"))
            substitute(module("org.mavai:punit-sentinel")).using(project(":punit-sentinel"))
            substitute(module("org.mavai:punit-report")).using(project(":punit-report"))
        }
    }
}

// Use local outcome when available
val outcomeDir = file("../outcome")
if (outcomeDir.isDirectory) {
    includeBuild(outcomeDir) {
        dependencySubstitution {
            substitute(module("org.mavai:outcome")).using(project(":"))
        }
    }
}
