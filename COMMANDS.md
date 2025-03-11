# Useful Commands

## Docker

Run compose:

```shell
docker compose up -d
```

Check kafka connect logs:

```shell
docker compose logs -f kafka-connect
```

Restart kafka connect:

```shell
docker compose restart kafka-connect
```

Stop containers:

```shell
docker compose down
```

## Kafka Connect

List connector plugins:

```shell
http :8083/connector-plugins
```

Modify/Create a sink connector:

```shell
http :8083/connectors < examples/basic.json
```

Get sink information:

```shell
http :8083/connectors/lh-sink
http :8083/connectors/lh-sink/status
```

List connectors:

```shell
http :8083/connectors expand==status expand==info
```

Restart connector:

```shell
http POST :8083/connectors/lh-sink/restart
```

Restart connector including tasks:

```shell
http POST :8083/connectors/lh-sink/restart includeTasks==true
```

Pause connector:

```shell
http PUT :8083/connectors/lh-sink/pause
```

Resume connector:

```shell
http PUT :8083/connectors/lh-sink/resume
```

Stop connector:

```shell
http PUT :8083/connectors/lh-sink/stop
```

Delete connector:

```shell
http DELETE :8083/connectors/lh-sink
```

List tasks:

```shell
http :8083/connectors/lh-sink/tasks
```

Restart task:

```shell
http POST :8083/connectors/lh-sink/tasks/0/restart
```

Update logger level for the connector:

```shell
http PUT :8083/admin/loggers/io.littlehorse.kafka.connect.LHSinkTask level=DEBUG
http :8083/admin/loggers/
```

## Schema Registry

List schemas:

```shell
http :8081/schemas
```
