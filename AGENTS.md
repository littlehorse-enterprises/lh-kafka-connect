# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project Overview

`lh-kafka-connect` provides [Kafka Connect](https://docs.confluent.io/platform/current/connect/index.html)
sink connectors that transfer data from Apache Kafka into [LittleHorse](https://littlehorse.io/).

Three sink connectors are provided:

- `WfRunSinkConnector` — runs `WfRun`s from Kafka records.
- `ExternalEventSinkConnector` — posts external events.
- `CorrelatedEventSinkConnector` — posts correlated events.

## Repository Layout

- `connector/` — the connectors, tasks, configs, predicates, and unit/e2e tests.
  - `src/main/java/io/littlehorse/connect/` — production code.
  - `src/test/java/io/littlehorse/connect/` — unit tests.
  - `src/test/java/e2e/` — end-to-end tests (Testcontainers + Kafka Connect).
- `common/` — shared serializers used by examples and tests.
- `examples/` — runnable example modules, each registered in `settings.gradle` as `example-<name>`.
- `build.gradle`, `settings.gradle`, `gradle.properties` — build configuration; dependency versions live in `gradle.properties`.

## Build & Test

Use the Gradle wrapper. Common commands:

```shell
./gradlew buildConfluentBundle   # build the plugin bundle
./gradlew test                   # run unit tests
./gradlew e2e                    # run end-to-end tests (requires docker)
./gradlew spotlessApply          # apply code formatting
```

Local stack:

```shell
docker compose up -d
```

## Conventions

- **Java 17** (source and target compatibility).
- **Formatting**: enforced by Spotless using `palantirJavaFormat` with the `AOSP` style and removal of unused imports. Always run `./gradlew spotlessApply` before finishing.
- **Package root**: `io.littlehorse.connect`.
- **Dependency versions**: defined in `gradle.properties`; reference them via the `${...}Version` variables rather than hardcoding versions.
- **Tests**: JUnit 5 with AssertJ assertions and Mockito; e2e tests use Testcontainers.
- New example modules must be added to the list in `settings.gradle`.

## Commits

- Do not create commits. Instead, draft the commit message(s) and the corresponding git command(s) for the user to review and run.
- Commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) format (e.g. `feat(connector): add struct variable support`).
- End each drafted commit message with an `Assisted-by:` trailer naming the model used (e.g. `Assisted-by: Claude Opus 4.8`).

## Requirements

- `docker` and `java` are required.
- `pre-commit` hooks are used; install with `pre-commit install`.
- Utilities `httpie` and `jq` are helpful for interacting with the Kafka Connect and Schema Registry REST APIs.

## Documentation

- `README.md` — connector usage and message structure.
- `DEVELOPMENT.md` — local development workflow.
- `CONFIGURATIONS.md` — connector configuration reference.
- `COMMANDS.md` — useful commands.
