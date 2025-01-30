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

Modify/Create a sink connector:

```shell
http PUT :8083/connectors/littlehorse-sink/config \
"tasks.max"="1" \
"connector.class"="io.littlehorse.kafka.connect.LHSinkConnector" \
"topics"="littlehorse.sink.input" \
"lhc.api.port"="2024" \
"lhc.api.host"="littlehorse" \
"lhc.tenant.id"="default"
```

> Or using an example: `http :8083/connectors < examples/basic.json`

Get sink information:

```shell
http :8083/connectors/littlehorse-sink
http :8083/connectors/littlehorse-sink/status
```

List connectors:

```shell
http :8083/connectors expand==status expand==info
```

Restart connector:

```shell
http POST :8083/connectors/littlehorse-sink/restart
```

> Including tasks: `
http POST :8083/connectors/littlehorse-sink/restart includeTasks==true`

Pause connector:

```shell
http PUT :8083/connectors/littlehorse-sink/pause
```

Resume connector:

```shell
http PUT :8083/connectors/littlehorse-sink/resume
```

Stop connector:

```shell
http PUT :8083/connectors/littlehorse-sink/stop
```

Delete connector:

```shell
http DELETE :8083/connectors/littlehorse-sink
```

List tasks:

```shell
http :8083/connectors/littlehorse-sink/tasks
```

Restart task:

```shell
http POST :8083/connectors/littlehorse-sink/tasks/0/restart
```

Update logger level for the connector:

```shell
http PUT :8083/admin/loggers/io.littlehorse.kafka.connect.LHSinkTask level=DEBUG
http :8083/admin/loggers/
```

## Links

- [Kafka Sink Connector Configurations](https://docs.confluent.io/platform/current/installation/configuration/connect/sink-connect-configs.html)
- [Kafka Connect REST Interface](https://docs.confluent.io/platform/current/connect/references/restapi.html)
