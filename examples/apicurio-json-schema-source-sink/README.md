# Source & Sink Connectors with Apicurio Registry (JSON Schema)

This example demonstrates the `JsonSchemaKafkaConverter` used **in both
directions** with the built-in file connectors:

- A `FileStreamSourceConnector` reads lines from a file and **serializes** them
  to a topic using the Apicurio JSON Schema converter (`fromConnectData`).
- A `FileStreamSinkConnector` reads the topic and **deserializes** the records
  back with the same converter (`toConnectData`), writing them to another file.

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

## Register the Schema

The `FileStreamSourceConnector` emits string values, so register a JSON string
schema for the topic:

```shell
./gradlew example-apicurio-json-schema-source-sink:run
```

## Create the Source Connector

Reads `source.txt` (mounted at `/data/source.txt`) and produces Apicurio JSON
Schema records to the topic:

```shell
http PUT :8083/connectors/example-apicurio-source/config < examples/apicurio-json-schema-source-sink/source-connector.json
```

## Create the Sink Connector

Consumes the topic and writes the deserialized values to `/tmp/sink.txt` inside
the Kafka Connect container:

```shell
http PUT :8083/connectors/example-apicurio-sink/config < examples/apicurio-json-schema-source-sink/sink-connector.json
```

## Verify the Round Trip

The sink file should contain the same lines as `source.txt`, proving the records
round-tripped through the Apicurio JSON Schema converter:

```shell
docker compose exec kafka-connect cat /tmp/sink.txt
```

Inspect the raw (Apicurio-framed) records on the topic:

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-apicurio-source-sink \
--from-beginning
```
