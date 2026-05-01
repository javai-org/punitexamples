dependencies {
    api(project(":app"))

    // PUnit — author-facing API (UseCase, Contract, Sampling, criteria),
    // engine, statistics, baselines, runtime entry point. JUnit-free so
    // sentinel-deployable classes can call PUnit.testing(...).assertPasses()
    // without dragging the test harness onto the production classpath.
    api("org.javai:punit-core:0.6.0")

    // Nullability annotations used by use case postcondition methods
    implementation("org.jspecify:jspecify:1.0.0")
}
