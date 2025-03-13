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

Create a connector:

```shell
http :8083/connectors < my-connector.json
```

Create and update connector:

```shell
http PUT :8083/connectors/my-connector/config < my-connector.json
```

Get connector information:

```shell
http :8083/connectors/my-connector
```

Get connector status:

```shell
http :8083/connectors/my-connector/status
```

List connectors:

```shell
http :8083/connectors expand==status expand==info
```

Restart connector:

```shell
http POST :8083/connectors/my-connector/restart
```

Restart connector including tasks:

```shell
http POST :8083/connectors/my-connector/restart includeTasks==true
```

Pause connector:

```shell
http PUT :8083/connectors/my-connector/pause
```

Resume connector:

```shell
http PUT :8083/connectors/my-connector/resume
```

Stop connector:

```shell
http PUT :8083/connectors/my-connector/stop
```

Delete connector:

```shell
http DELETE :8083/connectors/my-connector
```

List tasks:

```shell
http :8083/connectors/my-connector/tasks
```

Restart task:

```shell
http POST :8083/connectors/my-connector/tasks/0/restart
```

Update logger level for the connector:

```shell
http PUT :8083/admin/loggers/io.littlehorse.connect level=DEBUG
```

Get loggers:

```shell
http :8083/admin/loggers
```

## Schema Registry

List schemas:

```shell
http :8081/schemas
```
