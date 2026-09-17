# WfRun Connector with Native Maps

This example demonstrates the native, strongly typed LittleHorse `Map` capabilities described in
[Proposal 023](../../023.md):

- Declare a workflow variable as `Map<INT, STR>` with `declareMap()`.
- Convert schemaless JSON objects into native maps while coercing their string keys to `INT`.
- Read an entry selected at runtime with `get(WfRunVariable)`.
- Insert or replace an entry with `put()`.
- Construct a map from literals and runtime values with `buildMap()`.
- Receive native maps in a Java task worker with `@LHType(isLHMap = true)`.

The workflow invokes the worker before and after replacing the requested product with `"reserved"`,
so the mutation is visible in the worker logs. It also passes a map built at runtime containing the
requested product.

> [!WARNING]
> Run the commands from the repository root.

## Dependencies

- httpie
- docker
- java

## Setup Environment

Build the plugin bundle:

```shell
./gradlew buildConfluentBundle
```

Start the local environment:

```shell
./gradlew dockerComposeUp
```

## Run Worker

The worker registers the task and workflow definitions before it starts polling:

```shell
./gradlew example-wfrun-map:run
```

Keep the worker running while completing the remaining steps.

## Populate Topic

Create the topic:

```shell
docker compose exec kafka-connect \
  kafka-topics --create --bootstrap-server kafka1:9092 \
  --replication-factor 3 \
  --partitions 12 \
  --topic example-wfrun-map
```

Produce the sample records:

```shell
docker compose exec -T kafka-connect \
  kafka-console-producer --bootstrap-server kafka1:9092 \
  --topic example-wfrun-map \
  < examples/wfrun-map/data.txt
```

To generate new sample data, pass the number of records and the number of inventory entries per
record (both default to `10` and `3`, respectively):

```shell
./gradlew -q example-wfrun-map:run \
  -DmainClass="io.littlehorse.example.DataGenerator" \
  --args="10 3" \
  > examples/wfrun-map/data.txt
```

The generator always chooses a `requested-product-id` that exists in the generated inventory.

Each record follows this shape:

```json
{
  "inventory": {
    "101": "hyperdrive",
    "102": "navicomputer"
  },
  "requested-product-id": 101
}
```

JSON object keys are strings, but the workflow declares `inventory` as `Map<INT, STR>`. The
connector reads that type from the registered `WfSpec` and converts keys such as `"101"` to native
LittleHorse `INT` keys. No map-specific connector configuration is required.

## Create Connector

Create the connector after the worker has registered the workflow:

```shell
http PUT :8083/connectors/example-wfrun-map/config \
  < examples/wfrun-map/connector.json
```

Get the connector status:

```shell
http :8083/connectors/example-wfrun-map/status
```

## Check Results

List completed workflow runs:

```shell
lhctl search wfRun example-wfrun-map
```

For each input record, the worker logs two messages. The first contains the original product name;
the second shows that the requested map entry was replaced with `reserved`.

To inspect the records directly, run:

```shell
docker compose exec kafka-connect \
  kafka-console-consumer --bootstrap-server kafka1:9092 \
  --topic example-wfrun-map \
  --from-beginning
```

