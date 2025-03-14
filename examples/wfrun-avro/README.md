# WfRun Connector with AVRO

In this example you will:

- Register a workflow with variable types: `JSON_OBJ`.
- Produce avro messages to a kafka topic with SchemaRegistry.
- Create a WfRunSinkConnector with a HoistField transformation.

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
--topic example-wfrun-avro
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-avro-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-avro \
--property schema.registry.url=http://schema-registry:8081 \
--property value.schema="$(< examples/wfrun-avro/planet.avsc)" \
< examples/wfrun-avro/data.txt
```

Consume:

> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-avro-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-avro \
--property schema.registry.url=http://schema-registry:8081 \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-avro:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-avro/data.txt
```

## Check Schema Registry

List Schemas:

```shell
http :8081/schemas
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-avro:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-avro/config < examples/wfrun-avro/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-avro
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun --wfSpecName example-wfrun-avro
```
