# WfRun Connector with Protobuf

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce protobuf messages to a kafka topic with SchemaRegistry.
- Create a WfRunSinkConnector without transformation.

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
--topic example-wfrun-protobuf
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-protobuf-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-protobuf \
--property schema.registry.url=http://schema-registry:8081 \
--property value.schema="$(< examples/wfrun-protobuf/species.proto)" \
< examples/wfrun-protobuf/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-protobuf-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-protobuf \
--property schema.registry.url=http://schema-registry:8081 \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-protobuf:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-protobuf/data.txt
```

> [!NOTE]
> If you need to build the protobuf:

```shell
protoc --java_out=examples/wfrun-protobuf/src/main/java --proto_path=examples/wfrun-protobuf/ species.proto
```

## Check Schema Registry

List Schemas:

```shell
http :8081/schemas
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-protobuf:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-protobuf/config < examples/wfrun-protobuf/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-protobuf
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun --wfSpecName example-wfrun-protobuf
```
