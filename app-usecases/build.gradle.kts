dependencies {
    api(project(":app"))

    // PUnit core — engine, statistics, baselines, verdict types (JUnit-free)
    api("org.javai:punit-core:0.6.0")

    // PUnit runtime — typed authoring entry point PUnit + supporting
    // classes, JUnit-free so sentinel-deployable classes can call
    // PUnit.testing(...).assertPasses() without dragging the test
    // harness onto the production classpath.
    api("org.javai:punit-runtime:0.6.0")

    // Nullability annotations used by use case postcondition methods
    implementation("org.jspecify:jspecify:1.0.0")
}
