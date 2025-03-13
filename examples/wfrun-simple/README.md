# WfRn Simple Connector

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce messages to a kafka topic without SchemaRegistry.
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
./gradlew connector:buildConfluentBundle
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
--topic example-wfrun-simple
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-simple \
< examples/wfrun-simple/data.txt
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-simple:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-simple/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-simple:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-simple/config < examples/wfrun-simple/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-simple
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun --wfSpecName example-wfrun-simple
```

## Produce in Interactive Shell

```shell
docker compose exec kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-simple
```
