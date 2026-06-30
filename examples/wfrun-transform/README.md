# WfRun Connector with chained mapper transforms

In this example you will:

- Define LittleHorse [StructDefs](https://littlehorse.io/docs/server/concepts/structs) (`Film` with a nested `Director`).
- Register a workflow whose input variables are produced entirely by Single Message Transforms (SMTs).
- Produce flat json messages to a kafka topic without SchemaRegistry.
- Create a `WfRunSinkConnector` that **chains two mapper transforms** on the record **value**:
  - [`JsonPathMapperTransform$Value`](../../CONFIGURATIONS.md) builds the value (most WfRun input variables).
  - [`LiteralMapperTransform$Value`](../../CONFIGURATIONS.md) merges a constant variable on top.

> [!WARNING]
> Run the commands in the root directory

> [!NOTE]
> Both transforms write the same domain (the value). `JsonPathMapperTransform` is construct-only:
> it always builds its domain from scratch. `LiteralMapperTransform` always merges its constants
> onto the existing domain, so it adds the `franchise` variable on top of the value the JSONPath
> transform already built instead of replacing it.

## What the transforms do

The raw records are flat, e.g.:

```json
{"id":4,"title":"A New Hope","director":"George Lucas","domesticGross":307.26,"internationalGross":468.0}
```

`JsonPathMapperTransform$Value` reshapes each record's value into the WfRun input variables:

| Variable     | Type       | Mapping                                                       | Feature              |
| ------------ | ---------- | ------------------------------------------------------------ | -------------------- |
| `film`       | `STRUCT`   | `film.title` / `film.director.name`                          | nested struct        |
| `episode`    | `INT`      | `$.value.id`                                                 | direct read          |
| `cast`       | `JSON_ARR` | `$.value.cast`                                               | array read           |
| `summary`    | `STR`      | `$.concat($.value.title, " directed by ", $.value.director)` | JSONPath concat      |
| `boxOffice`  | `DOUBLE`   | `$.sum($.value.domesticGross, $.value.internationalGross)`   | JSONPath sum         |

`LiteralMapperTransform$Value` then merges one constant variable onto that value:

| Variable     | Type     | Mapping                                          | Feature             |
| ------------ | -------- | ------------------------------------------------ | ------------------- |
| `franchise`  | `STRUCT` | `franchise.name` / `franchise.producer`          | literal merge       |

Literal values are type-inferred: a whole number is an int, `true`/`false` a boolean, `null` a null
value, and anything else (like `Star Wars`) a string. Dotted targets such as `franchise.name` build
a nested object, so `franchise` becomes a `STRUCT`.

> [!NOTE]
> The connector reads the `WfSpec` on startup to detect that `film` and `franchise` are `STRUCT`s,
> so it builds the LittleHorse structs from the nested json objects the transforms produced.

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
--topic example-wfrun-transform
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-transform \
< examples/wfrun-transform/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-transform \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-transform:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-transform/data.txt
```

## Run Worker

Run worker (registers the `Director` and `Film` `StructDef`s, the `TaskDef`, and the `WfSpec`):

```shell
./gradlew example-wfrun-transform:run
```

Verify the `StructDef`s were created:

```shell
lhctl get structDef example-wfrun-transform-film 0
lhctl get structDef example-wfrun-transform-director 0
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-transform/config < examples/wfrun-transform/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-transform
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-wfrun-transform
```

Inspect a WfRun to see the variables the transforms produced:

```shell
lhctl get wfRun <wfRunId>
```
