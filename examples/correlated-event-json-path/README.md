# CorrelatedEvent Connector with JSONPath transform (string content)

In this example you will:

- Register a task with a `STR` external event content.
- Register a workflow that waits for a correlated event whose content is a `STR`.
- Produce **flat** json messages to a kafka topic without SchemaRegistry.
- Create a `CorrelatedEventSinkConnector` with a [`JsonPathMapperTransform$Value`](../../CONFIGURATIONS.md)
  that builds the string content the connector posts.

> [!WARNING]
> Run the commands in the root directory

> [!NOTE]
> Each record key is the `correlationId` and each record value is reshaped by the transform
> into a single `STR` (built with the JSONPath `concat()` function) before it is posted.

## What the transform does

The raw records are flat, e.g.:

```json
{"firstName":"Boba","lastName":"Fett"}
```

`JsonPathMapperTransform$Value` rebuilds the whole record value into a single string:

| Target | Mapping                                              | Feature         |
| ------ | --------------------------------------------------- | --------------- |
| (root) | `$.concat($.value.firstName, " ", $.value.lastName)` | JSONPath concat |

A bare `mapping` (no `mapping.<path>` suffix) replaces the entire value, so the record value
becomes the string `"Boba Fett"`, which the connector posts as the correlated event content.

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
--topic example-correlated-event-json-path
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-correlated-event-json-path \
--property "parse.key=true" \
--property "key.separator=|" \
< examples/correlated-event-json-path/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-correlated-event-json-path \
--property "print.key=true" \
--property "key.separator=|" \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-correlated-event-json-path:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/correlated-event-json-path/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-correlated-event-json-path:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-correlated-event-json-path/config < examples/correlated-event-json-path/connector.json
```

Get connector:

```shell
http :8083/connectors/example-correlated-event-json-path
```

## Check CorrelatedEvents

List correlated events:

```shell
lhctl search correlatedEvent example-correlated-event-json-path
```

> At this point all the correlated events are waiting for being claimed.

## Run Workflows

Run workflows with the same id for every correlated event:

```shell
while read line; do \
  lhctl run example-correlated-event-json-path id "${line%|*}"; \
done < examples/correlated-event-json-path/data.txt
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-correlated-event-json-path
```

> You can use `lhctl get externalEvent <wfRunId> <externalEventDefName> <guid>` \
> and `lhctl get wfRun <id>` to inspect the results.
