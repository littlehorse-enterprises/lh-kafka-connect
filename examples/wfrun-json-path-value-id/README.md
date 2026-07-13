# WfRun Connector with a Custom WfRunId Derived from the Record Value

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce JSON messages to a kafka topic without SchemaRegistry.
- Create a `WfRunSinkConnector` that chains two `JsonPathMapperTransform`s:
  - A `$Headers` transform that derives the `wfRunId` header from the record value
    (`$.concat("order-", $.value.orderId)`).
  - A `$Value` transform that maps the `customer` input variable from `$.value.customer`.

Each `WfRunId` is built from a constant `order-` prefix plus the record's `orderId` field, producing
ids such as `order-a1b2c3d4e5f6478890abcdef12345678`. Because `orderId` is unique per order, this
yields a deterministic, idempotent id per record.

> [!IMPORTANT]
> Transforms run in the order listed in `transforms`. A `JsonPathMapperTransform$Value` rebuilds the
> record value **from scratch**, dropping every unmapped field. Since the `wfRunId` reads
> `$.value.orderId`, the `$Headers` transform (`WfRunIdMapper`) must be listed **before** the
> `$Value` transform (`WfRunVariablesMapper`). If the order were reversed, the value would be rebuilt
> to just `{customer}` first, `$.value.orderId` would resolve to `null`, and the id would become the
> invalid `order-`, which LittleHorse rejects with `INVALID_ARGUMENT: Optional argument 'id' must be
> a valid hostname`. Id transforms that only read record metadata (`$.partition`, `$.offset`) are not
> order-sensitive.

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
./gradlew dockerComposeUp
```

## Populate Topic

Create topic:

```shell
docker compose exec kafka-connect \
kafka-topics --create --bootstrap-server kafka1:9092 \
--replication-factor 3 \
--partitions 12 \
--topic example-wfrun-json-path-value-id
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-json-path-value-id \
< examples/wfrun-json-path-value-id/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-json-path-value-id \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-json-path-value-id:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-json-path-value-id/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-json-path-value-id:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-json-path-value-id/config < examples/wfrun-json-path-value-id/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-json-path-value-id
```

## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun example-wfrun-json-path-value-id
```

The `WfRunId` of each run matches the `order-<orderId>` of the record that started it.
