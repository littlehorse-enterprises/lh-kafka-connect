# Developing LittleHorse Sink Connector for Kafka Connect

## Dependencies

- docker
- java

## Utilities

- httpie
- jq
- pre-commit

## Getting Started

Build plugin bundle:

```shell
./gradlew connector:buildConfluentBundle
```

Run compose:

```shell
docker compose up -d
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

## Useful Commands

For more useful commands go to [COMMANDS.md](COMMANDS.md).

## Links

- [Confluent Schema Registry REST Interface](https://docs.confluent.io/platform/current/schema-registry/develop/api.html)
- [Kafka Connect REST Interface](https://docs.confluent.io/platform/current/connect/references/restapi.html)
