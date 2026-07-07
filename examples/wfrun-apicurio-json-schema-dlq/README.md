# WfRun Connector with Apicurio Registry (JSON Schema) and DLQ

In this example you will:

- Register a workflow with a `JSON_OBJ` input variable.
- Register a JSON Schema in [Apicurio Registry](https://www.apicur.io/registry/).
- Produce valid records serialized with the Apicurio **JSON Schema** serializer.
- Produce a **malformed** record (not an Apicurio JSON Schema payload, so it does
  not match a valid schema).
- Create a `WfRunSinkConnector` with the
  `io.littlehorse.connect.converter.apicurio.JsonSchemaKafkaConverter` and a DLQ.
- The malformed record fails conversion and is routed to the DLQ topic, while the
  valid records are processed into `WfRun`s.

> [!NOTE]
> Records that cannot be deserialized by the converter (because they do not match
> a valid schema) are **conversion errors**, which Kafka Connect routes to the DLQ
> when `errors.tolerance=all`. See the
> [Error Handling](../../README.md#error-handling) section for details.

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

## Create the DLQ Topic

```shell
docker compose exec kafka-connect \
kafka-topics --create --bootstrap-server kafka1:9092 \
--replication-factor 3 \
--partitions 12 \
--topic example-wfrun-apicurio-json-schema-dlq-errors
```

## Produce Valid Messages

The producer registers the JSON Schema in Apicurio Registry and produces valid
records serialized with the Apicurio JSON Schema serializer:

```shell
./gradlew example-wfrun-apicurio-json-schema-dlq:run -DmainClass="io.littlehorse.example.Producer" --args="10"
```

## Produce a Malformed Message

Produce a plain (non-Apicurio) message that the converter cannot deserialize:

```shell
echo "this is not a valid apicurio json schema message" | \
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-apicurio-json-schema-dlq
```

## Run Worker

```shell
./gradlew example-wfrun-apicurio-json-schema-dlq:run
```

## Create Connector

```shell
http PUT :8083/connectors/example-wfrun-apicurio-json-schema-dlq/config < examples/wfrun-apicurio-json-schema-dlq/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-apicurio-json-schema-dlq
```

## Check WfRuns

The valid records are processed into `WfRun`s:

```shell
lhctl search wfRun example-wfrun-apicurio-json-schema-dlq
```

## Consume from Errors Topic

The malformed record is routed to the DLQ:

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-apicurio-json-schema-dlq-errors \
--from-beginning
```
