# WfRun Connector with JSON Value and Header

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce JSON messages to a kafka topic without SchemaRegistry.
- Create a WfRunSinkConnector with a HeaderFrom transformation.

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

Create topic:

```shell
docker compose exec kafka-connect \
kafka-topics --create --bootstrap-server kafka1:9092 \
--replication-factor 3 \
--partitions 12 \
--topic example-wfrun-value-to-header
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-value-to-header \
< examples/wfrun-value-to-header/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-value-to-header \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-value-to-header:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-value-to-header/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-value-to-header:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-value-to-header/config < examples/wfrun-value-to-header/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-value-to-header
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-wfrun-value-to-header
```
