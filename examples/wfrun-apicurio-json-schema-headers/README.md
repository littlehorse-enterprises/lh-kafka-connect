# WfRun Connector with Apicurio Registry (JSON Schema, schema coordinates in headers)

In this example you will:

- Register a workflow with a `JSON_OBJ` input variable.
- Register a JSON Schema for Star Wars Force wielders (Sith and Jedi) in
  [Apicurio Registry](https://www.apicur.io/registry/).
- Produce records serialized with the Apicurio **JSON Schema** serializer configured with
  `apicurio.registry.headers.enabled=true`, so the schema coordinates travel in the **Kafka record
  headers** instead of the message payload.
- Create a `WfRunSinkConnector` that deserializes them with the
  `io.littlehorse.connect.converter.apicurio.JsonSchemaKafkaConverter`, configured the same way so
  it reads the coordinates back from the headers.

> [!NOTE]
> The producer and the converter must agree on `apicurio.registry.headers.enabled`: the serializer
> decides whether the schema coordinates go in the record headers or the payload, and the
> deserializer must look in the same place.

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

## Produce Messages

The producer registers the JSON Schema in Apicurio Registry and produces records serialized with
the Apicurio JSON Schema serializer, writing the schema coordinates to the Kafka record headers:

```shell
./gradlew example-wfrun-apicurio-json-schema-headers:run -DmainClass="io.littlehorse.example.Producer" --args="10"
```

## Check Schema Registry

List the registered artifacts:

```shell
http :8080/apis/registry/v3/search/artifacts
```

## Run Worker

```shell
./gradlew example-wfrun-apicurio-json-schema-headers:run
```

## Create Connector

```shell
http PUT :8083/connectors/example-wfrun-apicurio-json-schema-headers/config < examples/wfrun-apicurio-json-schema-headers/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-apicurio-json-schema-headers
```

## Check WfRuns

```shell
lhctl search wfRun example-wfrun-apicurio-json-schema-headers
```
