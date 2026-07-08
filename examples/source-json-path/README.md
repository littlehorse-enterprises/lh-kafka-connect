# JSONPath Transform on a Source Connector

This example shows that the `JsonPathMapperTransform` is a standard Single Message Transform, so it
works on **source** connectors too, not just the LittleHorse sink connectors.

- A `FileStreamSourceConnector` reads plain-text lines from a file and emits each line as a string
  value.
- A [`JsonPathMapperTransform$Value`](../../CONFIGURATIONS.md) reshapes every record's value into a
  structured object `{"quote": "<line>"}` by reading `$.value` from the record envelope.
- The `JsonConverter` then serializes that object as JSON on the topic.

> [!NOTE]
> The transform evaluates its expressions against the record envelope
> `{key, value, headers, partition, offset}`. On a **source** record `offset` is always `null` and
> `partition` is `null` unless the source sets it, so only `$.key`, `$.value` and `$.headers` are
> generally useful here. See the [transforms documentation](../../README.md#transforms) for the
> full field applicability.

> [!WARNING]
> Run the commands in the root directory

## Dependencies

- httpie
- docker

## Setup Environment

Build plugin bundle:

```shell
./gradlew buildConfluentBundle
```

Run environment:

```shell
./gradlew dockerComposeUp
```

## Create the Source Connector

Reads `quotes.txt` (mounted at `/data-json-path/quotes.txt`) and produces the reshaped JSON records
to the topic:

```shell
http PUT :8083/connectors/example-source-json-path/config < examples/source-json-path/source-connector.json
```

Get connector:

```shell
http :8083/connectors/example-source-json-path
```

## Verify the Output

Each line becomes a JSON object `{"quote":"<line>"}` on the topic:

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-source-json-path \
--from-beginning
```
