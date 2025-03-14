# WfRn Simple Connector

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce json messages to a kafka topic with SchemaRegistry.
- Create a WfRunSinkConnector filters transformation.
- Drop a used field.

> [!WARNING]
> Run the commands in the root directory

## Dependencies

- httpie
- docker
- java

## Setup Environment

Build plugin bundle:

```shell
./gradlew connector:buildConfluentBundle
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
--topic example-wfrun-filter
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-json-schema-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-filter \
--property schema.registry.url=http://schema-registry:8081 \
--property key.schema="$(< examples/wfrun-filter/quote_key.schema.json)" \
--property value.schema="$(< examples/wfrun-filter/quote.schema.json)" \
--property "parse.key=true" \
--property "key.separator=|" \
< examples/wfrun-filter/data.txt
```

Consume:

> In case you need to verify the messages in the topic.

```shell
docker compose exec -T kafka-connect \
kafka-json-schema-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-filter \
--property schema.registry.url=http://schema-registry:8081 \
--property "print.key=true" \
--property "key.separator=|" \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-filter:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-filter/data.txt
```

## Check Schema Registry

List Schemas:

```shell
http :8081/schemas
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-filter:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-filter/config < examples/wfrun-filter/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-filter
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun --wfSpecName example-wfrun-filter
```
