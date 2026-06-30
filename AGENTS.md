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

- `connector/` — the connectors, tasks, configs, predicates, transforms, and unit/e2e tests.
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
./gradlew dockerComposeUp
```

## Conventions

- **Java 17** (source and target compatibility).
- **Formatting**: enforced by Spotless using `palantirJavaFormat` with the `AOSP` style and removal of unused imports. Always run `./gradlew spotlessApply` before finishing.
- **Package root**: `io.littlehorse.connect`.
- **Dependency versions**: defined in `gradle.properties`; reference them via the `${...}Version` variables rather than hardcoding versions.
- **Tests**: JUnit 5 with AssertJ assertions and Mockito; e2e tests use Testcontainers.
- New example modules must be added to the list in `settings.gradle`.

## Architecture Notes

### Sink task hierarchy

- All three tasks extend the abstract `LHSinkTask`, which owns the shared `put()` loop,
  offset handling, and error classification. Per-connector tasks implement
  `executeGrpcCall(...)` and may override `afterStart()` for startup work (e.g. loading
  metadata).
- gRPC calls block the thread on purpose so records are processed sequentially per
  partition. `ALREADY_EXISTS` is treated as a successful idempotent retry and skipped.

### Error handling (transient vs. permanent)

- `LHSinkTask` classifies failures via `isRetriable(Throwable)` against a `RETRIABLE_CODES`
  set (`CANCELLED`, `DEADLINE_EXCEEDED`, `RESOURCE_EXHAUSTED`, `ABORTED`, `INTERNAL`,
  `UNAVAILABLE`).
- Retriable gRPC failures are rethrown as `RetriableException` so Kafka Connect retries the
  batch — they are never sent to the DLQ, regardless of `errors.tolerance`.
- All other failures are permanent: they honor `errors.tolerance` (fail on `none`, route to
  the DLQ on `all`).
- When adding code that calls the LittleHorse gRPC API, let `StatusRuntimeException` bubble
  up so this central classifier can act on it; do not swallow it locally.

### Struct content support

- Building `VariableValue`s from message payloads goes through `util/VariableValueMapper`,
  which resolves `STRUCT` types and caches `StructDef` lookups (including nested structs).
  Lookups use the versioned `StructDefId` carried by the `WfSpec`'s `TypeDefinition`, so the
  connector builds structs from the version the `WfSpec` pinned, not the latest registered one.
  Prefer it over calling `LHLibUtil.objToVarVal(...)` directly in tasks.
- Connectors discover whether content is a `STRUCT` by reading metadata on startup
  (`afterStart()`):
  - `WfRunSinkTask` loads the type defs of the entrypoint thread's input variables from the
    `WfSpec`, fetched via the `getWfSpec` gRPC (when `wf.spec.major.version` and
    `wf.spec.revision` are set) or `getLatestWfSpec` (resolved by `wf.spec.name` and optional
    version). Variables in child threads are not inspected.
  - `ExternalEventSinkTask` and `CorrelatedEventSinkTask` load the content type from the
    `ExternalEventDef`, fetched via the `getExternalEventDef` gRPC (resolved by
    `external.event.name`). Its `type_information` field is optional, so they guard with
    `hasTypeInformation()` before reading the return type.
- Resolution at `put()` time is best-effort: `VariableValueMapper.toVariableValue` only builds a
  struct when it receives a non-null `TypeDefinition` whose type is a `StructDef`. A message field
  with no matching `STRUCT` type def (unknown variable, missing `type_information`, or a
  non-struct type) falls back to `LHLibUtil.objToVarVal(...)` and keeps its value-inferred type.
- If the `WfSpec`/`ExternalEventDef` itself cannot be loaded at startup, the connector fails to
  start (so the metadata must be registered before the connector runs).

### Mapper transforms (SMTs)

- The `transform/` package provides two Single Message Transforms that reshape a record by
  writing into an "operating domain" (the record key, value, or headers). Both share
  `AbstractMapperTransform`, which parses the dynamic `mapping`/`mapping.<path>` properties via
  `MapperTransformConfig`, sorts mappings by path depth, and writes them into the domain. Each
  transform exposes its own documented `CONFIG_DEF` (built with
  `MapperTransformConfig.configDef(doc)`) returned from `config()` and surfaced in
  `CONFIGURATIONS.md` by `ConfigExporter`. Concrete transforms read extra config via the
  `onConfigure(MapperTransformConfig)` hook, invoked after parsing but before mappings compile.
- A `null` mapping value is preserved by `MapperTransformConfig` (not rejected) so each transform
  decides its meaning, but note the Kafka Connect REST API itself rejects a bare JSON `null` config
  value, so reaching this path means using the `null` literal text or a non-REST config source.
- `JsonPathMapperTransform` builds the domain from scratch by evaluating JSONPath expressions
  (values must start with `$`) against the record envelope `{key, value, headers}`; unmapped
  fields are dropped. Functions such as `concat`/`sum` are supported.
- `LiteralMapperTransform` injects constant values whose type is inferred (int, double,
  `true`/`false`, `null`, else string; double-quote to force a string). Setting
  `implicit.casting.enabled=false` disables inference and keeps every value as its original
  string (a bare `null` mapping still becomes a null value). Unlike the JSONPath
  transform, it merges its constants onto the existing domain, overriding fields with the same
  name (each variant overrides `initialDomain` to seed from the existing key/value/headers).
  `JsonPathMapperTransform` rejects a `null` mapping value (it cannot be a `$` expression).
- Both transforms expose `$Key`, `$Value`, and `$Headers` nested variants to choose which part
  of the record is rebuilt. For the `$Headers` variant a `mapping.<path>` is a single, flat
  header name rather than a nested path.

### End-to-end tests

- e2e tests live in `src/test/java/e2e/`, extend `e2e.configs.E2ETest`, and are matched by
  the `e2e` Gradle task (separate from `test`, which excludes them).
- Run a single e2e test with `./gradlew :connector:e2e --tests "e2e.tests.SomeTest"`. The
  `Test` task caches results, so add `--rerun-tasks` to force a re-run after non-source
  changes.
- The shared LittleHorse/Kafka containers are static across the suite — avoid tests that
  must stop or disrupt them (e.g. simulating server outages).
- `E2ETest` provides helpers such as `registerWorkflow`, `registerStructDef`, `startWorker`,
  `createTopics`, `produceValues`, `consumeRecords`, and `await(...)`.

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
