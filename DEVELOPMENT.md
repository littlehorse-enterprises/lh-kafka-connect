# LittleHorse Connectors for Kafka Connect

## Dependencies

- docker
- java

## Utilities

- httpie
- jq
- pre-commit

## Getting Started

Install pre-commit hooks:

```shell
pre-commit install
```

Build plugin bundle:

```shell
./gradlew buildConfluentBundle
```

Run compose (builds the plugin bundle first):

```shell
./gradlew dockerComposeUp
```

Rebuild the plugin bundle and restart Kafka Connect to load the new build:

```shell
./gradlew updateConfluentBundle
```

Check that LH plugin was installed:

```shell
http :8083/connector-plugins connectorsOnly==false | jq -r '.[].class|select(startswith("io.littlehorse"))'
```

## Services

The `dockerComposeUp` task starts the following services, available on `localhost`:

| Service           | Port    | Description                                 |
|-------------------|---------|---------------------------------------------|
| `kafka1`          | `19092` | Kafka broker (external listener)            |
| `kafka2`          | `29092` | Kafka broker (external listener)            |
| `kafka3`          | `39092` | Kafka broker (external listener)            |
| `schema-registry` | `8081`  | Confluent Schema Registry REST API          |
| `kafka-connect`   | `8083`  | Kafka Connect REST API                      |
| `kafka-ui`        | `8090`  | Kafka & Kafka Connect web UI                |
| `apicurio`        | `8080`  | Apicurio Registry v3 REST API               |
| `apicurio-ui`     | `8888`  | Apicurio Registry web UI                    |
| `littlehorse`     | `2023`  | LittleHorse gRPC API                        |
| `littlehorse-ui`  | `3000`  | LittleHorse dashboard                       |

## Tests

Run unit tests:

```shell
./gradlew test
```

Run e2e tests:

```shell
./gradlew e2e
```

## Code Style

Apply code style:

```shell
./gradlew spotlessApply
```

## Useful Commands

For more useful commands go to [COMMANDS.md](COMMANDS.md).

## Links

- [Confluent Schema Registry REST Interface](https://docs.confluent.io/platform/current/schema-registry/develop/api.html)
- [Kafka Connect REST Interface](https://docs.confluent.io/platform/current/connect/references/restapi.html)
