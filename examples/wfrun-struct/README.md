# WfRun Connector with StructDef

In this example you will:

- Define LittleHorse [StructDefs](https://littlehorse.io/docs/server/concepts/structs) (`Pilot` with a nested `Vehicle`).
- Register a workflow with a `STRUCT` variable type.
- Produce json messages to a kafka topic without SchemaRegistry.
- Create a WfRunSinkConnector with a `HoistField` transformation.

> [!WARNING]
> Run the commands in the root directory

> [!NOTE]
> The connector reads the `WfSpec` on startup to detect that the `pilot` variable is a
> `STRUCT`, so it builds the LittleHorse struct from the nested json object automatically.
> The `data.txt` records hold the struct fields at the top level (`{"name":...,"vehicle":...}`),
> so the `connector.json` uses a [`HoistField`](https://docs.confluent.io/kafka-connectors/transforms/current/hoistfield.html)
> transformation to wrap each record under the `pilot` field before it reaches LittleHorse.
> See the upstream
> [struct-def example](https://github.com/littlehorse-enterprises/littlehorse/tree/master/examples/java/struct-def).


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
--topic example-wfrun-struct
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-struct \
< examples/wfrun-struct/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-struct \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-struct:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-struct/data.txt
```

## Run Worker

Run worker (registers the `Vehicle` and `Pilot` `StructDef`s, the `TaskDef`, and the `WfSpec`):

```shell
./gradlew example-wfrun-struct:run
```

Verify the `StructDef`s were created:

```shell
lhctl get structDef example-wfrun-struct-pilot 0
lhctl get structDef example-wfrun-struct-vehicle 0
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-struct/config < examples/wfrun-struct/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-struct
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-wfrun-struct
```
