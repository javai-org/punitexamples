dependencies {
    // Jackson - for JSON parsing in domain classes
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.21.1")

    // Outcome - result types used by domain classes
    implementation("org.javai:outcome:0.2.0")

    // Log4j2 - logging
    implementation("org.apache.logging.log4j:log4j-api:2.25.3")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.25.3")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.3")
}
