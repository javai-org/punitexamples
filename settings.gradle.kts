pluginManagement {
    // Resolve the PUnit Gradle plugin from the local punit build when available
    val punitPluginDir = file("../punit/punit-gradle-plugin")
    if (punitPluginDir.isDirectory) {
        includeBuild(punitPluginDir)
    }
}

rootProject.name = "punitexamples"

// Use local punit when available (sibling folder), Maven Central otherwise
val punitDir = file("../punit")
if (punitDir.isDirectory) {
    includeBuild(punitDir) {
        dependencySubstitution {
            substitute(module("org.javai:punit")).using(project(":"))
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
