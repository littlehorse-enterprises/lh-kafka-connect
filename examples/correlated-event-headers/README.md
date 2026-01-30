# CorrelatedEvent Connector with Header

In this example you will:

- Register a task with a `JSON_OBJ` variable.
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
- jq

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
--topic example-correlated-event-headers
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-correlated-event-headers \
< examples/correlated-event-headers/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-correlated-event-headers \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-correlated-event-headers:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/correlated-event-headers/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-correlated-event-headers:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-correlated-event-headers/config < examples/correlated-event-headers/connector.json
```

Get connector:

```shell
http :8083/connectors/example-correlated-event-headers
```

## Run Workflows

Run workflows with the same id for every external event:

```shell
while read line; do \
  lhctl run example-correlated-event-headers payment-id "$(echo $line | jq -r '.id')"; \
done < examples/correlated-event-headers/data.txt
```

## Check CorrelatedEvents

List external events:

```shell
lhctl search correlatedEvent example-correlated-event-headers
```

> At this point all the external events are waiting for being claimed.

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-correlated-event-headers
```

> You can use `lhctl get externalEvent <wfRunId> <externalEventDefName> <guid>` \
> and `lhctl get wfRun <id>` to inspect the results.
