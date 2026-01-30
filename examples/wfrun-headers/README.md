# WfRun Simple Connector with Headers

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce messages to a kafka topic with a wfRunId header and without SchemaRegistry.
- Create a WfRunSinkConnector with a simple transformation.

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
--topic example-wfrun-headers
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--property parse.headers=true \
--topic example-wfrun-headers \
< examples/wfrun-headers/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-headers \
--property print.headers=true \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-headers:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-headers/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-headers:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-headers/config < examples/wfrun-headers/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-headers
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-wfrun-headers
```
