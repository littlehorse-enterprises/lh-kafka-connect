# WfRun Connector with List Variable

In this example you will:

- Register a workflow with variable types: `JSON_ARR`.
- Produce avro messages to a kafka topic with SchemaRegistry.
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
--topic example-wfrun-list
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-avro-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-list \
--property schema.registry.url=http://schema-registry:8081 \
--property value.schema="$(< examples/wfrun-list/droids.avsc)" \
< examples/wfrun-list/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-avro-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-list \
--property schema.registry.url=http://schema-registry:8081 \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-list:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10 2" > examples/wfrun-list/data.txt
```

## Check Schema Registry

List Schemas:

```shell
http :8081/schemas
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-list:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-list/config < examples/wfrun-list/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-list
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun --wfSpecName example-wfrun-list
```
