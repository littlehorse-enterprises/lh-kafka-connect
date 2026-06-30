# ExternalEvent Connector with JSONPath transform (struct content)

In this example you will:

- Define LittleHorse [StructDefs](https://littlehorse.io/docs/server/concepts/structs) (`Pilot` with a nested `Vehicle`).
- Register a workflow that waits for an external event whose content is a `STRUCT`.
- Produce **flat** json messages to a kafka topic without SchemaRegistry.
- Create an `ExternalEventSinkConnector` with a [`JsonPathMapperTransform$Value`](../../CONFIGURATIONS.md)
  that reshapes each flat record into the nested struct the connector posts.

> [!WARNING]
> Run the commands in the root directory

> [!NOTE]
> The connector reads the `ExternalEventDef` on startup to detect that its content is a
> `STRUCT`, so it builds the LittleHorse struct from the json object the transform produced.
> Each record key is the `wfRunId` and each record value is reshaped into the `Pilot` struct.

## What the transform does

The raw records are flat, e.g.:

```json
{"name":"Luke Skywalker","model":"Snowspeeder"}
```

`JsonPathMapperTransform$Value` reshapes each record's value into the nested `Pilot` struct:

| Target          | Mapping          | Feature       |
| --------------- | ---------------- | ------------- |
| `name`          | `$.value.name`   | direct read   |
| `vehicle.model` | `$.value.model`  | nested struct |

The dotted target `vehicle.model` builds a nested object, so the value becomes
`{"name":"Luke Skywalker","vehicle":{"model":"Snowspeeder"}}`, matching the `Pilot` struct.

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
--topic example-external-event-json-path
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-external-event-json-path \
--property "parse.key=true" \
--property "key.separator=|" \
< examples/external-event-json-path/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-external-event-json-path \
--property "print.key=true" \
--property "key.separator=|" \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-external-event-json-path:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/external-event-json-path/data.txt
```

## Run Worker

Run worker (registers the `Vehicle` and `Pilot` `StructDef`s, the `TaskDef`, and the `WfSpec`):

```shell
./gradlew example-external-event-json-path:run
```

Verify the `StructDef`s were created:

```shell
lhctl get structDef example-external-event-json-path-pilot 0
lhctl get structDef example-external-event-json-path-vehicle 0
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-external-event-json-path/config < examples/external-event-json-path/connector.json
```

Get connector:

```shell
http :8083/connectors/example-external-event-json-path
```

## Check ExternalEvents

List external events:

```shell
lhctl search externalEvent example-external-event-json-path
```

> At this point all the external events are waiting for being claimed.

## Run Workflows

Run workflows with the same id for every external event:

```shell
while read line; do \
  lhctl run example-external-event-json-path --wfRunId "${line%|*}"; \
done < examples/external-event-json-path/data.txt
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-external-event-json-path
```

> You can use `lhctl get externalEvent <wfRunId> <externalEventDefName> <guid>` \
> and `lhctl get wfRun <id>` to inspect the results.
