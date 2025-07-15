# WfRun Connector with DLQ

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce json messages to a kafka topic without SchemaRegistry.
- Create a WfRunSinkConnector with DLQ and without transformations.
- Introduce errors, this error will be redirected to a DLQ topic.

> [!WARNING]
> Run the commands in the root directory

## Dependencies

- httpie
- docker
- java

## Setup Environment

Build plugin bundle:

```shell
./gradlew buildConfluentBundle
```

Run environment:

```shell
docker compose up -d
```

## Populate Topic

Create topics:

```shell
docker compose exec kafka-connect \
kafka-topics --create --bootstrap-server kafka1:9092 \
--replication-factor 3 \
--partitions 12 \
--topic example-wfrun-dlq


docker compose exec kafka-connect \
kafka-topics --create --bootstrap-server kafka1:9092 \
--replication-factor 3 \
--partitions 12 \
--topic example-wfrun-dlq-errors
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-dlq \
< examples/wfrun-dlq/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-dlq \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-dlq:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-dlq/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-dlq:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-dlq/config < examples/wfrun-dlq/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-dlq
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-wfrun-dlq
```

## Consume from Errors Topic

Run consumer:

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-dlq-errors \
--from-beginning
```
