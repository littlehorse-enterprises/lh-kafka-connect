# CorrelatedEvent Simple Connector

In this example you will:

- Register a task with a `JASON_OBJ` variable.
- Register a workflow that waits for an external event.
- Produce string messages to a kafka topic without SchemaRegistry.
- Create an CorrelatedEventSinkConnector without transformations.
- The CorrelatedEventSinkConnector executes external events with the content of the topic.

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
docker compose up -d
```

## Populate Topic

Create topic:

```shell
docker compose exec kafka-connect \
kafka-topics --create --bootstrap-server kafka1:9092 \
--replication-factor 3 \
--partitions 12 \
--topic example-correlated-event-json
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-correlated-event-json \
--property "parse.key=true" \
--property "key.separator=|" \
< examples/correlated-event-json/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-correlated-event-json \
--property "print.key=true" \
--property "key.separator=|" \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-correlated-event-json:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/correlated-event-json/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-correlated-event-json:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-correlated-event-json/config < examples/correlated-event-json/connector.json
```

Get connector:

```shell
http :8083/connectors/example-correlated-event-json
```

## Check CorrelatedEvents

List external events:

```shell
lhctl search externalEvent example-correlated-event-json
```

> At this point all the external events are waiting for being claimed.

## Run Workflows

Run workflows with the same id for every external event:

```shell
while read line; do \
  lhctl run example-correlated-event-json payment-id "${line%|*}"; \
done < examples/correlated-event-json/data.txt
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-correlated-event-json
```

> You can use `lhctl get externalEvent <wfRunId> <externalEventDefName> <guid>` \
> and `lhctl get wfRun <id>` to inspect the results.
