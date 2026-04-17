dependencies {
    api(project(":app"))

    // PUnit core — sentinel specs are production artifacts (JUnit-free)
    api("org.javai:punit-core:0.6.0")

    // Nullability annotations used by use case postcondition methods
    implementation("org.jspecify:jspecify:1.0.0")
}
