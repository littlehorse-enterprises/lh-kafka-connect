# Developing LittleHorse Sink Connector for Kafka Connect

## Dependencies

- httpie
- docker
- java

## Getting Started

Build plugin bundle:

```shell
./gradlew connector:buildConfluentBundle
```

Run compose:

```shell
docker compose up -d
```

## Useful Commands

For more useful commands go to [COMMANDS.md](COMMANDS.md).

## Links

- [Confluent Schema Registry REST Interface](https://docs.confluent.io/platform/current/schema-registry/develop/api.html)
- [Kafka Connect REST Interface](https://docs.confluent.io/platform/current/connect/references/restapi.html)
