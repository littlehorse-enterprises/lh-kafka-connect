# WfRun Connector with Apicurio Registry (JSON Schema)

In this example you will:

- Register a workflow with a `JSON_OBJ` input variable.
- Register a JSON Schema in [Apicurio Registry](https://www.apicur.io/registry/).
- Produce records serialized with the Apicurio **JSON Schema** serializer.
- Create a `WfRunSinkConnector` that deserializes them with the
  `io.littlehorse.connect.converter.apicurio.JsonSchemaKafkaConverter`.

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

The producer registers the JSON Schema in Apicurio Registry and produces records
serialized with the Apicurio JSON Schema serializer:

```shell
./gradlew example-wfrun-apicurio-json-schema:run -DmainClass="io.littlehorse.example.Producer" --args="10"
```

## Check Schema Registry

List the registered artifacts:

```shell
http :8080/apis/registry/v3/search/artifacts
```

## Run Worker

```shell
./gradlew example-wfrun-apicurio-json-schema:run
```

## Create Connector

```shell
http PUT :8083/connectors/example-wfrun-apicurio-json-schema/config < examples/wfrun-apicurio-json-schema/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-apicurio-json-schema
```

## Check WfRuns

```shell
lhctl search wfRun example-wfrun-apicurio-json-schema
```
