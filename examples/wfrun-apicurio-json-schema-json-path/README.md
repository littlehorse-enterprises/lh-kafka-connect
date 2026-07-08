# WfRun Connector with Apicurio Registry (JSON Schema, custom artifact + JSONPath)

In this example you will:

- Register a workflow with `STR` and `INT` input variables.
- Register a JSON Schema in [Apicurio Registry](https://www.apicur.io/registry/) under a
  **custom group and artifact id** (not the default `<topic>-value`).
- Produce records serialized with the Apicurio **JSON Schema** serializer, resolving the schema
  through the explicit `apicurio.registry.artifact.group-id` /
  `apicurio.registry.artifact.artifact-id` coordinates.
- Create a `WfRunSinkConnector` that deserializes them with the
  `io.littlehorse.connect.converter.apicurio.JsonSchemaKafkaConverter`, resolving the schema through
  the same `apicurio.registry.artifact.group-id` / `apicurio.registry.artifact.artifact-id`
  coordinates.

> [!NOTE]
> The records carry the planet fields at the **top level** (no envelope), so the connector uses a
> [`JsonPathMapperTransform$Value`](../../CONFIGURATIONS.md) to reshape each record's value into the
> `WfSpec` input variables:
>
> | Variable     | Type  | Mapping               |
> | ------------ | ----- | --------------------- |
> | `name`       | `STR` | `$.value.name`        |
> | `climate`    | `STR` | `$.value.climate`     |
> | `terrain`    | `STR` | `$.value.terrain`     |
> | `population` | `INT` | `$.value.population`  |

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

The producer registers the JSON Schema in Apicurio Registry under the `star-wars` group with the
`planet` artifact id, then produces records serialized with the Apicurio JSON Schema serializer:

```shell
./gradlew example-wfrun-apicurio-json-schema-json-path:run -DmainClass="io.littlehorse.example.Producer" --args="10"
```

## Check Schema Registry

List the registered artifacts (you should see the `planet` artifact in the `star-wars` group):

```shell
http :8080/apis/registry/v3/search/artifacts
```

## Run Worker

```shell
./gradlew example-wfrun-apicurio-json-schema-json-path:run
```

## Create Connector

```shell
http PUT :8083/connectors/example-wfrun-apicurio-json-schema-json-path/config < examples/wfrun-apicurio-json-schema-json-path/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-apicurio-json-schema-json-path
```

## Check WfRuns

```shell
lhctl search wfRun example-wfrun-apicurio-json-schema-json-path
```
