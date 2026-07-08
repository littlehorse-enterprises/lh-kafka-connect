# WfRun Connector with a Custom WfRunId from Partition and Offset

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce JSON messages to a kafka topic without SchemaRegistry.
- Create a `WfRunSinkConnector` with a `JsonPathMapperTransform$Headers` transform that derives the
  `wfRunId` header from the record's Kafka `partition` and `offset`.

The connector builds each `WfRunId` from a constant connector-name prefix plus the JSONPath
envelope fields `$.partition` and `$.offset`, producing ids such as
`example-wfrun-json-path-id-p<partition>-o<offset>` (for example
`example-wfrun-json-path-id-p3-o42`). Because a partition/offset pair is unique per topic, this
yields a deterministic, idempotent id per record.

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
./gradlew dockerComposeUp
```

## Populate Topic

Create topic:

```shell
docker compose exec kafka-connect \
kafka-topics --create --bootstrap-server kafka1:9092 \
--replication-factor 3 \
--partitions 12 \
--topic example-wfrun-json-path-id
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-json-path-id \
< examples/wfrun-json-path-id/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-json-path-id \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-json-path-id:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-json-path-id/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-json-path-id:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-json-path-id/config < examples/wfrun-json-path-id/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-json-path-id
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-wfrun-json-path-id
```

The `WfRunId` of each run matches the `example-wfrun-json-path-id-p<partition>-o<offset>` of the
record that started it.
