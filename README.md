# lh-kafka-connect

LittleHorse Sink Connector for Kafka Connect

## Dependencies:

- node (for prettier)
- httpie (for rest requests)
- docker
- java

## Getting Started

Build plugin bundle:

```shell
./gradlew clean shadowJar
```

Run kafka connect:

```shell
docker compose up -d
```

## Other Commands

### Docker

Restart kafka connect:

```shell
docker compose restart kafka-connect
```

Stop containers:

```shell
docker compose down
```

### Rest API

List connector plugins:

```shell
http :8083/connector-plugins
```

Create a LH Sink Connector:

```shell
http :8083/connectors < examples/basic.json
```

Get sink:

```shell
http :8083/connectors/littlehorse-sink
```

List connectors:

```shell
http :8083/connectors expand==status
```

Delete connector:

```shell
http DELETE :8083/connectors/littlehorse-sink
```
