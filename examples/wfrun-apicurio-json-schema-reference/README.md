# WfRun Connector with Apicurio Registry (JSON Schema references)

In this example you will:

- Register a workflow with a `JSON_OBJ` input variable.
- Register a JSON Schema in [Apicurio Registry](https://www.apicur.io/registry/)
  whose value schema `$ref`s a separate `vehicle` schema artifact.
- Produce records serialized with the Apicurio **JSON Schema** serializer.
- Create a `WfRunSinkConnector` that deserializes them with the
  `io.littlehorse.connect.converter.apicurio.JsonSchemaKafkaConverter`.

> [!NOTE]
> The records carry the pilot fields at the **top level** (no envelope), so the connector uses a
> [`JsonPathMapperTransform$Value`](../../CONFIGURATIONS.md) to rebuild the `pilot` `JSON_OBJ`
> variable from the flat value:
>
> | Path                   | Mapping                    |
> | ---------------------- | -------------------------- |
> | `pilot.name`           | `$.value.name`             |
> | `pilot.vehicle.model`  | `$.value.vehicle.model`    |

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

The producer registers the `vehicle` schema, then the value schema that
references it, and produces records serialized with the Apicurio JSON Schema
serializer:

```shell
./gradlew example-wfrun-apicurio-json-schema-reference:run -DmainClass="io.littlehorse.example.Producer" --args="10"
```

## Check Schema Registry

List the registered artifacts (you should see both `example-vehicle` and the
value schema):

```shell
http :8080/apis/registry/v3/search/artifacts
```

## Run Worker

```shell
./gradlew example-wfrun-apicurio-json-schema-reference:run
```

## Create Connector

```shell
http PUT :8083/connectors/example-wfrun-apicurio-json-schema-reference/config < examples/wfrun-apicurio-json-schema-reference/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-apicurio-json-schema-reference
```

## Check WfRuns

```shell
lhctl search wfRun example-wfrun-apicurio-json-schema-reference
```
