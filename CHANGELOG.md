# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed
- Updated optimize experiments (`ShoppingBasketOptimizePrompt`, `ShoppingBasketOptimizeTemperature`) to the renamed `initialFactor` attribute on `@OptimizeExperiment`. Tracks punit's first experiment DX refactor PR (`refactor/experiment-dx` on `javai-org/punit`), which renamed `initialControlFactorSource` → `initialFactor` and removed the inline `initialControlFactorValue` attribute.

## [0.4.0] - 2026-04-17

### Changed
- Upgraded PUnit dependency from 0.4.0 to 0.6.0
- Reworked use cases to be immutable — factor settings (model, temperature, system prompt) are baked into the instance at construction, preserving the i.i.d. assumption required by the Bernoulli model
- Simplified `ShoppingBasketExplore`: the use case instance *is* the factor specification, eliminating manual factor maps and trial-closure enrichment. Now combines `@ConfigSource` (named immutable use case instances) with `@InputSource` (curated instructions)
- `@CovariateSource` annotations on `ShoppingBasketUseCase` accessors allow the framework to extract factors from the instance rather than a separate declaration
- Flow exploration/optimisation output relocated from `src/test/resources/punit/` to `build/punit/` so generated artefacts no longer pollute the source tree
- Enabled JUnit Jupiter extension autodetection on verdict catalogue and flow test tasks

### Added
- Warmup on `PaymentGatewayUseCase` to stabilise latency measurements
- `skipWarmup` option on explore/optimize experiments for the shopping basket (LLM costs)
- Log4j2 configuration so example runs produce visible log output

### Fixed
- Flow clean task deletes generated artefacts from the build directory rather than from version-controlled resources

## [0.3.1] - 2026-03-10

### Changed
- Upgraded Outcome dependency from 0.1.0 to 0.2.0
- Adapted `AnthropicChatLlm` and `OpenAiChatLlm` to Outcome 0.2.0 API (`Failure.exception()` now returns `Optional<Throwable>`)

### Added
- GitHub Actions CI workflow, Claude Code workflows, and Dependabot configuration
- PUnit Gradle plugin resolution from Maven Central for CI (composite build still takes priority locally)

## [0.3.0] - 2026-03-10

### Changed
- Upgraded PUnit dependency from 0.3.0 to 0.4.0
- Restructured into three modules: `app` (domain classes), `app-usecases` (use case definitions and `@Sentinel` reliability specs), and `app-tests` (probabilistic tests and experiments)

### Added
- Sentinel reliability specs: `PaymentGatewayReliability` and `ShoppingBasketReliability` in `app-usecases`
- `PaymentGatewayReliabilityTest` and `PaymentGatewayReliabilityExtendedTest` for sentinel-based testing
- `ShoppingBasketReliabilityTest` for sentinel-based shopping basket testing
- Latency example in `PaymentGatewayReliability`
- PUnit Gradle plugin (`org.javai.punit`) applied to `app-tests`

## [0.2.0] - 2026-03-02

### Changed
- Upgraded PUnit dependency from 0.2.0 to 0.3.0, picking up enhanced verdict text (particularly latency reporting)
- Regenerated verdict catalog (`docs/VERDICT-CATALOG.md`) against PUnit 0.3.0

### Added
- Operational flow verification test (`OperationalFlowVerificationTest`) validating the full punit lifecycle: explore → optimize → measure → verify → test
- Gradle tasks for running the operational flow end-to-end (`operationalFlowTest` and individual stage tasks)

### Fixed
- Use `assumeTrue` for missing directories in verification test to skip gracefully rather than fail

## [0.1.1] - 2026-02-15

### Added
- Release lifecycle tasks (`release`, `tagRelease`) mirroring the punit project workflow
- Version externalized to `gradle.properties`
- CHANGELOG.md validation gate in release task

### Fixed
- Scoped publish task to root project to avoid triggering included composite builds

## [0.1.0] - 2026-02-15

Initial release of PUnit Examples — example applications and probabilistic
tests demonstrating the PUnit framework.

### Added
- Example applications with probabilistic tests
- Golden dataset fixture for experiment configuration
- Verdict catalogue generation (summary and verbose)
- User guide documentation

[Unreleased]: https://github.com/javai-org/punitexamples/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/javai-org/punitexamples/compare/v0.3.1...v0.4.0
[0.3.1]: https://github.com/javai-org/punitexamples/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/javai-org/punitexamples/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/javai-org/punitexamples/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/javai-org/punitexamples/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/javai-org/punitexamples/releases/tag/v0.1.0
