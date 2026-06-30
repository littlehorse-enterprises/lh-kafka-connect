# WfRun Connector with JSONPath Filter

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce json messages to a kafka topic with SchemaRegistry.
- Create a WfRunSinkConnector that keeps only the records matching a JSONPath expression.
- Drop the field used to filter.

The `JsonPathFilterPredicate` evaluates a JSONPath expression against the record envelope
`{key, value, headers}` and matches when the result is truthy. Here it keeps only the `high`
priority quotes via an inline filter (`$.value[?(@.priority == 'high')]`), paired with the standard
`Filter` transform and its `negate` option. The `priority` field is then dropped with `ReplaceField`
so only the declared `quote` variable reaches the workflow.

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
--topic example-wfrun-json-path-filter
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-json-schema-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-json-path-filter \
--property schema.registry.url=http://schema-registry:8081 \
--property value.schema="$(< examples/wfrun-json-path-filter/quote.schema.json)" \
< examples/wfrun-json-path-filter/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-json-schema-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-json-path-filter \
--property schema.registry.url=http://schema-registry:8081 \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-json-path-filter:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-json-path-filter/data.txt
```

## Check Schema Registry

List Schemas:

```shell
http :8081/schemas
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-json-path-filter:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-json-path-filter/config < examples/wfrun-json-path-filter/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-json-path-filter
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-wfrun-json-path-filter
```

Only the `high` priority quotes create a `WfRun`; the `low` priority records are dropped.
