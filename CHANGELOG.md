# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Notes — punit baseline schema bumped to `punit-baseline-3`

- **punit#167 bumps the MEASURE baseline YAML schema from
  `punit-baseline-2` to `punit-baseline-3`** (per-methodology-
  criterion shape). The shopping-basket baseline directory
  (`src/test/resources/punit/baselines/`) is gitignored — files
  here regenerate on each `./gradlew exp -Prun=ShoppingBasketMeasure.measureBaseline`
  run, so no committed baseline file is involved in the migration.
- **Locally-cached `punit-baseline-2` files are rejected** by the
  upgraded `BaselineReader` with a re-MEASURE diagnostic. Anyone
  with an old baseline on disk should delete it and re-run
  `ShoppingBasketMeasure.measureBaseline` (LLM credentials
  required). New baselines emit in `punit-baseline-3` shape
  automatically.
- **No code or test changes in punitexamples.** The empirical
  TEST path against a freshly-regenerated baseline behaves
  identically — the `BaselineResolver`'s K=1 unwrap (in punit)
  yields the same `PassRateStatistics` the old shape carried
  directly.

## [0.5.0-alpha6] - 2026-05-12

> **🧪 Experimental release.** Tracks [punit 0.7.0-alpha6](https://github.com/javai-org/punit/blob/main/CHANGELOG.md#070-alpha6---2026-05-12),
> which renames the authoring-surface interface `UseCase` →
> `ServiceContract`. Worked examples rename in lockstep; see
> *Changed (breaking)* for the consumer-side migration.

### Changed (breaking)

- **Examples renamed `UseCase` → `ServiceContract` in lockstep with
  punit.** `ShoppingBasketUseCase` → `ShoppingBasketServiceContract`,
  `PaymentGatewayUseCase` → `PaymentGatewayServiceContract`. Package
  moves from `org.javai.punit.examples.usecases` to
  `org.javai.punit.examples.servicecontracts`. Sample-size constant
  holders move with them.
- Test classes and method names referencing `UseCase` follow the
  same rename. Imports and prose throughout the project are swept.
- punit-core / punit-report pins bump from 0.7.0-alpha5 to
  0.7.0-alpha6. The Gradle plugin pin in `settings.gradle.kts`
  bumps in lockstep.

## [0.5.0-alpha5] - 2026-05-11

> **🧪 Experimental release.** Tracks [punit 0.7.0-alpha5](https://github.com/javai-org/punit/blob/main/CHANGELOG.md#070-alpha5---2026-05-11) — punit's adoption of `org.javai:outcome` 0.3.0 (which now ships its own `module-info`) and removal of the `extra-java-module-info` shim. No source changes on this side; mechanical version bump only.

### Changed
- **Upgraded PUnit dependency from 0.7.0-alpha4 to 0.7.0-alpha5.** Plugin pin in `settings.gradle.kts` and `punit-core` / `punit-report` coordinates in `build.gradle.kts` bumped in lockstep.

## [0.5.0-alpha4] - 2026-05-11

> **🧪 Experimental release.** Tracks [punit 0.7.0-alpha4](https://github.com/javai-org/punit/blob/main/CHANGELOG.md#070-alpha4---2026-05-11) — the public-surface consolidation arc: JPMS module declarations on every published library module, the `punit-junit5` bundler artifact retired, four verdict-side types promoted out of `internal.*` (breaking FQN change), and the orphan `@Latency` annotation retired in favour of the `PercentileLatency` criterion. No source changes were needed on this side — the project never depended on `punit-junit5`, the retired `@Latency` annotation, or the promoted-type FQNs.

### Changed
- **Upgraded PUnit dependency from 0.7.0-alpha3 to 0.7.0-alpha4.** Plugin pin in `settings.gradle.kts` and `punit-core` / `punit-report` coordinates in `build.gradle.kts` bumped in lockstep. See punit's CHANGELOG for the full set of changes pulled in.

## [0.5.0-alpha3] - 2026-05-10

> **🧪 Experimental release.** Tracks [punit 0.7.0-alpha3](https://github.com/javai-org/punit/blob/main/CHANGELOG.md#070-alpha3---2026-05-10) — the structural-cleanup arc release. Mechanical migration on this side: every consumer-side import that touched a relocated punit package was rewritten.

### Changed
- **Upgraded PUnit dependency from 0.7.0-alpha2 to 0.7.0-alpha3.** Pulls in the four-PR cleanup arc: the `contract/*` parallel stack removal, the RP07 verdict XML alignment (`ci-lower`/`ci-upper` → `wilson-lower`), the package-drift collapse (`model/`, `controls/`, `power/`, `engine/output/`, top-level `spec/` all dispersed), and the internal-namespace move that relocated every framework-internal package under `org.javai.punit.internal.*`. See punit's CHANGELOG for the full list.
- **Follow punit's package-drift fix.** Imports of `org.javai.punit.power.PowerAnalysis` updated to `org.javai.punit.engine.baseline.PowerAnalysis` (then `internal.engine.baseline.PowerAnalysis` after the namespace move). Dead `model..` and `controls..` package entries dropped from `ExampleArchitectureTest`'s prohibited-packages list.
- **Follow punit's internal-namespace move.** Every import of `engine.criteria.PassRate`, `engine.baseline.PowerAnalysis`, and `engine.*` generally rewritten to `internal.engine.*` across sentinel classes and probabilistic tests. The `ExampleArchitectureTest`'s "infrastructure does not depend on the PUnit framework" rule replaces its enumerated internal-package list with the structural marker `org.javai.punit.internal..` plus the public surfaces — the post-namespace layout makes the rule noticeably tighter.

## [0.5.0-alpha2] - 2026-05-10

> **🧪 Experimental release.** Tracks [punit 0.7.0-alpha2](https://github.com/javai-org/punit/blob/main/CHANGELOG.md#070-alpha2---2026-05-10).

### Changed
- **Upgraded PUnit dependency from 0.7.0-alpha to 0.7.0-alpha2.** Pulls in
  the empirical-threshold-derivation methodology fix, PowerAnalysis covariate
  resolution + perfect-baseline support, INCONCLUSIVE console diagnostics,
  the empirical-no-baseline preflight short-circuit, and PT08 soundness-floor
  enforcement. See punit's CHANGELOG for the full list.
- **Sample-size tuning** in `ShoppingBasketThresholdApproachesTest` so the
  threshold-approach examples actually verify against the post-fix
  feasibility maths. Sample-size constants centralised so curious developers
  can tweak them in one place.
- **Generated-baseline directory renamed `specs/` → `baselines/`** to match
  punit 0.7.x's `BaselineResolver` convention. The README under that
  directory and the project's `.gitignore` were updated in lockstep.

## [0.5.0-alpha] - 2026-05-07

> **🧪 Experimental release.** Tracks the experimental [punit 0.7.0-alpha](https://github.com/javai-org/punit/blob/main/CHANGELOG.md#070-alpha---2026-05-07) typed-builder API. Pin to this exact version if you depend on its surface today; v0.x means breaking changes are still possible.

### Changed
- **Upgraded PUnit dependency from 0.6.0 to 0.7.0-alpha.** The 0.7 line replaces the annotation-driven authoring style of 0.6.x with a typed, builder-based one. See punit's [MIGRATION-0.6-to-0.7.md](https://github.com/javai-org/punit/blob/main/docs/MIGRATION-0.6-to-0.7.md). Every service contract, experiment, and probabilistic test in this project has been migrated.
- **Service contracts are typed `ServiceContract<FT, I, O>` implementations.** `ShoppingBasketServiceContract` and `PaymentGatewayServiceContract` now implement the typed interface; metadata (`description`, `warmup`, `pacing`, `covariates`, `customCovariateResolvers`) is expressed as method overrides. The factor record is passed at construction time, preserving immutability. `ShoppingBasketServiceContract.O` is `String`; its `Contract` is folded into the service contract and uses `deriving(...)` to combine parsing and validation in one expression. `PaymentGatewayServiceContract` declares `NoFactors` since it has no tunable factors. The legacy `@ServiceContract` annotation, parameter-level annotations (`@Factor`, `@FactorSource`, `@Input`, `@InputSource`, `@Config`, `@ConfigSource`, `@ControlFactor`, `@DayGroup`, `@RegionGroup`, `@Latency`, `@ExperimentDesign`, `@ExperimentGoal`), and the reflection-based factor accessors (`@FactorSetter`, `@FactorGetter`) are gone — see punit 0.7.0-alpha.
- **Probabilistic tests use `PUnit.testing(...)`.** `@ProbabilisticTest` survives only as a bare marker; configuration moves into the body via the typed builder. Migrated tests: `ShoppingBasketTest`, `ShoppingBasketBudgetTest`, `ShoppingBasketCovariateTest`, `ShoppingBasketDiagnosticsTest`, `ShoppingBasketExceptionTest`, `ShoppingBasketPacingTest`, `ShoppingBasketThresholdApproachesTest`, `SlaTest`, `InvalidProbabilisticTestExamplesTest`.
- **Experiments use the unified `@Experiment` marker plus `PUnit.measuring(...)` / `.exploring(...)` / `.optimizing(...)`.** Migrated: `ShoppingBasketMeasure`, `ShoppingBasketExplore`, `ShoppingBasketOptimizePrompt`, `ShoppingBasketOptimizeTemperature`. The three legacy experiment annotations (`@MeasureExperiment`, `@ExploreExperiment`, `@OptimizeExperiment`) are gone.
- **Covariate redesign adopted.** Covariate examples now consume the sealed `Covariate` hierarchy (`DayOfWeekCovariate`, `RegionCovariate`, `TimeOfDayCovariate`, `Covariate.custom(...)`) declared on `ServiceContract.covariates()`, with `customCovariateResolvers()` supplying the project-defined resolver. The `@Covariate`, `@CovariateSource`, `@DayGroup`, `@RegionGroup` annotations are gone.
- **Sentinel reliability examples** rebuilt against the typed surface. `@Sentinel` annotation removed — `punit-sentinel` scans typed `ServiceContract` implementations directly. `CoinTossReliabilityExamples` added as a worked example.
- **Optimize experiments use `FactorsStepper` returning `NextFactor`** (sealed `Continue` / `Stop`). `PromptEngineerStepper` extracted as a reusable component; `ShoppingBasketOptimizePrompt` now models a realistic LLM-as-prompt-engineer iteration loop instead of a scripted prompt progression. `ShoppingBasketOptimizeTemperature` calls `disableEarlyTermination()` on the optimize builder.
- **Config record renames.** Shopping-basket factor record `Config` → `LlmTuning`; `Config.defaults()` → `Config.DEFAULT`. The `with` prefix is dropped from `LlmTuning` builder methods. `ShoppingActionValidator.ValidationResult` → `BasketTranslation`.
- **Statistic rename.** `BernoulliPassRate` → `PassRate` throughout (matches punit).
- **`Punit` → `PUnit`** to match the brand convention.
- **Contract postcondition shape.** Postconditions now consume `ContractBuilder<O>` and are evaluated against `ServiceContractOutcome`; success checks moved out of `invoke(...)` and into postconditions where they belong (`PaymentGatewayServiceContract`).
- **Anticipated transport failures travel as `ChatLlmException`** — a checked exception declared on `ChatLlm` — rather than wrapped runtime exceptions. Aligns with the project's Outcome/exception convention: thrown exceptions signal defects, not anticipated failure modes.
- **Collapsed to a single Gradle module.** The previous three-module split (`app/` + `app-usecases/` + `app-tests/`) mirrored punit's internal multi-module layout for no real benefit; a developer reading the examples ought to see a normal `src/main/java` + `src/test/java` project, not a multi-module Gradle setup. Domain code, service contracts, and sentinels now live under `src/main/java/org/javai/punit/examples/{app,usecases,sentinels}/`; tests under `src/test/java/...`. Sentinel-deployability is preserved by scoping the test stack (`punit-junit5`, JUnit, AssertJ, ArchUnit) to `testImplementation`. No package renames.
- **Single input source per service contract.** `ShoppingBasketServiceContract` exposes one canonical input source consumed by both `ShoppingBasketMeasure` and `ShoppingBasketTest`, ensuring the measure baseline and the probabilistic test draw from the same population.
- **Drop punit-runtime dependency.** Runtime entry point now ships in `punit-core`; the `punit-runtime` declaration is removed.
- **EXPLORE retuned for meaningful model comparison.** Sample sizes and instruction set adjusted so model differences are statistically resolvable.

### Added
- `disableEarlyTermination()` use in the temperature optimisation example, demonstrating the explicit opt-out for the optimize surface.
- Markdown code-fence stripping before parsing LLM JSON responses, for robustness against models that wrap JSON in fenced blocks.
- `PaymentGatewayServiceContract` warmup retained from 0.4.0 and adapted to the typed surface.

### Removed
- Legacy service contracts, sentinel specs, and JUnit wrappers from the pre-typed era.
- Verdict catalogue test (deferred — port pending in a follow-up).
- `Tier` from `PaymentGatewayServiceContract` (no factor variation needed; `NoFactors` suffices).
- `BASELINE_DIR` constant — `PowerAnalysis` no-Path overload (punit#85) supersedes it.

### Fixed
- `PaymentGatewayServiceContract.invoke` no longer wraps the call in a dead `try/catch`; checked exceptions now propagate per the Outcome convention.

## [0.4.0] - 2026-04-17

### Changed
- Upgraded PUnit dependency from 0.4.0 to 0.6.0
- Reworked service contracts to be immutable — factor settings (model, temperature, system prompt) are baked into the instance at construction, preserving the i.i.d. assumption required by the Bernoulli model
- Simplified `ShoppingBasketExplore`: the service contract instance *is* the factor specification, eliminating manual factor maps and trial-closure enrichment. Now combines `@ConfigSource` (named immutable service contract instances) with `@InputSource` (curated instructions)
- `@CovariateSource` annotations on `ShoppingBasketServiceContract` accessors allow the framework to extract factors from the instance rather than a separate declaration
- Flow exploration/optimisation output relocated from `src/test/resources/punit/` to `build/punit/` so generated artefacts no longer pollute the source tree
- Enabled JUnit Jupiter extension autodetection on verdict catalogue and flow test tasks

### Added
- Warmup on `PaymentGatewayServiceContract` to stabilise latency measurements
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
- Restructured into three modules: `app` (domain classes), `app-usecases` (service contract definitions and `@Sentinel` reliability specs), and `app-tests` (probabilistic tests and experiments)

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

[Unreleased]: https://github.com/javai-org/punitexamples/compare/v0.5.0-alpha5...HEAD
[0.5.0-alpha5]: https://github.com/javai-org/punitexamples/compare/v0.5.0-alpha4...v0.5.0-alpha5
[0.5.0-alpha4]: https://github.com/javai-org/punitexamples/compare/v0.5.0-alpha3...v0.5.0-alpha4
[0.5.0-alpha3]: https://github.com/javai-org/punitexamples/compare/v0.5.0-alpha2...v0.5.0-alpha3
[0.5.0-alpha2]: https://github.com/javai-org/punitexamples/compare/v0.5.0-alpha...v0.5.0-alpha2
[0.5.0-alpha]: https://github.com/javai-org/punitexamples/compare/v0.4.0...v0.5.0-alpha
[0.4.0]: https://github.com/javai-org/punitexamples/compare/v0.3.1...v0.4.0
[0.3.1]: https://github.com/javai-org/punitexamples/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/javai-org/punitexamples/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/javai-org/punitexamples/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/javai-org/punitexamples/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/javai-org/punitexamples/releases/tag/v0.1.0
