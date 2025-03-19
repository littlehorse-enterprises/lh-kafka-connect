# ExternalEvent Connector with Transformation ValueToKey

In this example you will:

- Register a workflow with a `STR` variable.
- This workflow waits for an external event.
- Produce string messages to a kafka topic without SchemaRegistry.
- Create an ExternalEventSinkConnector with transformations.

> [!WARNING]
> Run the commands in the root directory

## Dependencies

- jq
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
--topic example-external-event-value-to-key
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-external-event-value-to-key \
< examples/external-event-value-to-key/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-external-event-value-to-key \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-external-event-value-to-key:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/external-event-value-to-key/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-external-event-value-to-key:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-external-event-value-to-key/config < examples/external-event-value-to-key/connector.json
```

Get connector:

```shell
http :8083/connectors/example-external-event-value-to-key
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
  lhctl run example-external-event-value-to-key --wfRunId "$(jq -r .wfRunId <<< "$line")"; \
done < examples/external-event-value-to-key/data.txt
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun --wfSpecName example-external-event-value-to-key
```

> You can use `lhctl get externalEvent <wfRunId> <externalEventDefName> <guid>` \
> and `lhctl get wfRun <id>` to inspect the results.
