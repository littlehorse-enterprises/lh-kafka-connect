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

Run compose:

```shell
docker compose up -d
```

Check that LH plugin was installed:

```shell
http :8083/connector-plugins connectorsOnly==false | jq -r '.[].class|select(startswith("io.littlehorse"))'
```

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
