# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

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

[Unreleased]: https://github.com/javai-org/punitexamples/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/javai-org/punitexamples/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/javai-org/punitexamples/releases/tag/v0.1.0
