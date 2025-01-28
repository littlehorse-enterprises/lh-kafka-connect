# lh-kafka-connect

LittleHorse Sink Connector for Kafka Connect

## Dependencies:

- node (for prettier)
- httpie (for rest requests)
- docker
- java 21

## Commands:

Build plugin bundle:

```shell
./gradlew clean shadowJar
```

Run kafka connect:

```shell
docker compose up -d
```

List connect

```shell
http :8083/connector-plugins
```

Restart kafka connect:

```shell
docker compose restart kafka-connect
```
