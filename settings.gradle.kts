pluginManagement {
    // Resolve the PUnit Gradle plugin from the local punit build when available
    val punitPluginDir = file("../punit/punit-gradle-plugin")
    if (punitPluginDir.isDirectory) {
        includeBuild(punitPluginDir)
    }

    // Allow resolving the PUnit plugin from Maven Central (used when local build is absent)
    plugins {
        id("org.javai.punit") version "0.6.0"
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "punitexamples"

include("app", "app-usecases", "app-tests")

// Use local punit when available (sibling folder), Maven Central otherwise
val punitDir = file("../punit")
if (punitDir.isDirectory) {
    includeBuild(punitDir) {
        dependencySubstitution {
            substitute(module("org.javai:punit")).using(project(":"))
            substitute(module("org.javai:punit-core")).using(project(":punit-core"))
            substitute(module("org.javai:punit-junit5")).using(project(":punit-junit5"))
            substitute(module("org.javai:punit-runtime")).using(project(":punit-runtime"))
            substitute(module("org.javai:punit-sentinel")).using(project(":punit-sentinel"))
            substitute(module("org.javai:punit-report")).using(project(":punit-report"))
        }
    }
}

// Use local outcome when available
val outcomeDir = file("../outcome")
if (outcomeDir.isDirectory) {
    includeBuild(outcomeDir) {
        dependencySubstitution {
            substitute(module("org.javai:outcome")).using(project(":"))
        }
    }
}
