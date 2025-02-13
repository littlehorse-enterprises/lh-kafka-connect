# Demo

Build plugin bundle:

```shell
./gradlew connector:buildConfluentBundle
```

Run environment:

```shell
docker compose up -d
```

Check logs:

```shell
docker compose logs -f kafka-connect
```

Create topic:

```shell
kafka-topics --create --bootstrap-server localhost:19092 \
--replication-factor 3 \
--partitions 12 \
--topic demo
```

Populate topic:

```shell
./gradlew demo:run --args="producer -n 10 demo"
```

Register task and run worker:

```shell
./gradlew demo:run --args="worker"
```

Register workflow:

```shell
./gradlew demo:run --args="register"
```

Update logger level for the connector:

```shell
http PUT :8083/admin/loggers/io.littlehorse.kafka.connect.LHSinkTask level=DEBUG
```

Create connector:

```shell
http PUT :8083/connectors/littlehorse-sink/config \
"tasks.max":=2 \
"connector.class"="io.littlehorse.kafka.connect.LHSinkConnector" \
"topics"="demo" \
"key.converter"="org.apache.kafka.connect.storage.StringConverter" \
"value.converter"="org.apache.kafka.connect.json.JsonConverter" \
"value.converter.schemas.enable":=false \
"lhc.api.port":=2024 \
"lhc.api.host"="littlehorse" \
"lhc.tenant.id"="default" \
"wf.spec.name"="demo"
```

Create DLQ topic:

```shell
kafka-topics --create --bootstrap-server localhost:19092 \
--replication-factor 3 \
--partitions 12 \
--topic demo-dlq
```

Create connector with DLQ:

```shell
http PUT :8083/connectors/littlehorse-sink-dlq/config \
"tasks.max":=2 \
"connector.class"="io.littlehorse.kafka.connect.LHSinkConnector" \
"topics"="demo" \
"key.converter"="org.apache.kafka.connect.storage.StringConverter" \
"value.converter"="org.apache.kafka.connect.json.JsonConverter" \
"value.converter.schemas.enable":=false \
"lhc.api.port":=2024 \
"lhc.api.host"="littlehorse" \
"lhc.tenant.id"="default" \
"wf.spec.name"="demo" \
"errors.tolerance"="all" \
"errors.deadletterqueue.topic.name"="demo-dlq" \
"errors.deadletterqueue.context.headers.enable":=true
```

Introduce an error (converter error):

```shell
kafka-console-producer --bootstrap-server localhost:19092 \
--property parse.key=true \
--property key.separator=: \
--topic demo
```
