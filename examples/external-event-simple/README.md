# ExternalEvent Simple Connector

In this example you will:

- Register a workflow with a `STR` variable.
- This workflow waits for an external event.
- Produce string messages to a kafka topic without SchemaRegistry.
- Create an ExternalEventSinkConnector without transformations.

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
--topic example-external-event-simple
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-external-event-simple \
--property "parse.key=true" \
--property "key.separator=|" \
< examples/external-event-simple/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-external-event-simple \
--property "print.key=true" \
--property "key.separator=|" \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-external-event-simple:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/external-event-simple/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-external-event-simple:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-external-event-simple/config < examples/external-event-simple/connector.json
```

Get connector:

```shell
http :8083/connectors/example-external-event-simple
```

## Check ExternalEvents

List external events:

```shell
lhctl search externalEvent set-character-name
```

> At this point all the external events are waiting for being claimed.

## Run Workflows

Run workflows with the same id for every external event:

```shell
while read line; do \
  lhctl run example-external-event-simple --wfRunId "${line%|*}"; \
done < examples/external-event-simple/data.txt
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-external-event-simple
```

> You can use `lhctl get externalEvent <wfRunId> <externalEventDefName> <guid>` \
> and `lhctl get wfRun <id>` to inspect the results.
