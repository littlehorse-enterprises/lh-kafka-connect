# CorrelatedEvent Connector with StructDef

In this example you will:

- Define LittleHorse [StructDefs](https://littlehorse.io/docs/server/concepts/structs) (`Pilot` with a nested `Vehicle`).
- Register a workflow that waits for a correlated event whose content is a `STRUCT`.
- Produce json messages to a kafka topic without SchemaRegistry.
- Create a CorrelatedEventSinkConnector without transformations.
- The CorrelatedEventSinkConnector posts correlated events with struct content.

> [!WARNING]
> Run the commands in the root directory

> [!NOTE]
> The connector reads the `ExternalEventDef` on startup to detect that its content is a
> `STRUCT`, so it builds the LittleHorse struct from the json object automatically.
> Each record key is the `correlationId` and each record value is the `Pilot` struct.

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
--topic example-correlated-event-struct
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-correlated-event-struct \
--property "parse.key=true" \
--property "key.separator=|" \
< examples/correlated-event-struct/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-correlated-event-struct \
--property "print.key=true" \
--property "key.separator=|" \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-correlated-event-struct:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/correlated-event-struct/data.txt
```

## Run Worker

Run worker (registers the `Vehicle` and `Pilot` `StructDef`s, the `TaskDef`, and the `WfSpec`):

```shell
./gradlew example-correlated-event-struct:run
```

Verify the `StructDef`s were created:

```shell
lhctl get structDef example-correlated-event-struct-pilot 0
lhctl get structDef example-correlated-event-struct-vehicle 0
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-correlated-event-struct/config < examples/correlated-event-struct/connector.json
```

Get connector:

```shell
http :8083/connectors/example-correlated-event-struct
```

## Check CorrelatedEvents

List correlated events:

```shell
lhctl search correlatedEvent example-correlated-event-struct
```

> At this point all the correlated events are waiting for being claimed.

## Run Workflows

Run workflows with the same id for every correlated event:

```shell
while read line; do \
  lhctl run example-correlated-event-struct id "${line%|*}"; \
done < examples/correlated-event-struct/data.txt
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-correlated-event-struct
```

> You can use `lhctl get externalEvent <wfRunId> <externalEventDefName> <guid>` \
> and `lhctl get wfRun <id>` to inspect the results.
