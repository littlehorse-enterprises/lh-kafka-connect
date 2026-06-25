# LittleHorse Connectors for Kafka Connect

<a href="https://github.com/littlehorse-enterprises/lh-kafka-connect"><img alt="github" src="https://img.shields.io/badge/GitHub-purple?logo=github&logoColor=white"></a>
<a href="https://docs.confluent.io/platform/current/connect/index.html"><img alt="confluent" src="https://raw.githubusercontent.com/littlehorse-enterprises/lh-kafka-connect/refs/heads/main/assets/confluent-badge.svg"/></a>
<a href="https://littlehorse.io/"><img alt="littlehorse" src="https://raw.githubusercontent.com/littlehorse-enterprises/littlehorse/refs/heads/master/img/badges/blue.svg"/></a>

These connectors allow data transfer between Apache Kafka and LittleHorse.

## Table Of Content

<!-- TOC -->
* [LittleHorse Connectors for Kafka Connect](#littlehorse-connectors-for-kafka-connect)
  * [Table Of Content](#table-of-content)
  * [WfRunSinkConnector](#wfrunsinkconnector)
    * [Expected Message Structure](#expected-message-structure)
    * [Additional Metadata](#additional-metadata)
    * [Quick Example](#quick-example)
    * [Struct Variables](#struct-variables)
  * [ExternalEventSinkConnector](#externaleventsinkconnector)
    * [Expected Message Structure](#expected-message-structure-1)
    * [Additional Metadata](#additional-metadata-1)
    * [Quick Example](#quick-example-1)
    * [Struct Content](#struct-content)
  * [CorrelatedEventSinkConnector](#correlatedeventsinkconnector)
    * [Expected Message Structure](#expected-message-structure-2)
    * [Additional Metadata](#additional-metadata-2)
    * [Quick Example](#quick-example-2)
    * [Struct Content](#struct-content-1)
  * [Idempotent Writes](#idempotent-writes)
  * [Multiple Tasks](#multiple-tasks)
  * [Dead Letter Queue](#dead-letter-queue)
  * [Data Types](#data-types)
  * [Converters](#converters)
  * [External Secrets](#external-secrets)
  * [Configurations](#configurations)
  * [Download](#download)
  * [Versioning](#versioning)
  * [Examples](#examples)
  * [Development](#development)
  * [Dependencies](#dependencies)
  * [License](#license)
<!-- TOC -->

## WfRunSinkConnector

This connector allows you to execute [WfRuns](https://littlehorse.io/docs/server/concepts/workflows#the-wfrun) into LittleHorse.
It supports all the [Variable Types](https://littlehorse.io/docs/server/concepts/variables) provided by LittleHorse.

More about running workflows at [LittleHorse Quickstart](https://littlehorse.io/docs/server/getting-started/quickstart).

### Expected Message Structure

| Message Part | Description                                                                                                              | Type   | Valid Values       |
|--------------|--------------------------------------------------------------------------------------------------------------------------|--------|--------------------|
| `key`        | Optional, custom wfRunId. The connector genererates an id if not present, check [Idempotent Writes](#idempotent-writes). | string | hostname format    |
| `value`      | Define the `variables` field of the workflow.                                                                            | map    | key-value not null |

More about run workflow fields at [RunWfRequest](https://littlehorse.io/docs/server/api#runwfrequest).

You can manipulate the message structure with [Single Message Transformations (SMTs)](https://docs.confluent.io/kafka-connectors/transforms/current/overview.html).

> `wfRunId` precedence: 1. Look for metadata header, 2. Look for key message, 3. Generates an idempotency key.

### Additional Metadata

Optionally this sink connector uses the record headers to configure the wf runs:

| Header Key      | Description                                                                                                              | Type   | Valid Values    |
|-----------------|--------------------------------------------------------------------------------------------------------------------------|--------|-----------------|
| `wfRunId`       | Optional, custom wfRunId. The connector genererates an id if not present, check [Idempotent Writes](#idempotent-writes). | string | hostname format |
| `parentWfRunId` | Optional, sets a parent wf run.                                                                                          | string | hostname format |

### Quick Example

Next workflow executes a task that receives a `String` as parameter called `name`:

```java
Workflow workflow = Workflow.newWorkflow("greetings", wf -> {
    WfRunVariable name = wf.declareStr("name");
    wf.execute("greet", name);
});
```

There is a topic `names`, with this data in the topic:

```text
key: null, value: {"name":"Anakin Skywalker"}
key: null, value: {"name":"Luke Skywalker"}
key: null, value: {"name":"Leia Organa"}
key: null, value: {"name":"Padme Amidala"}
```

Next connector configuration will execute `WfRuns` with the variable `name`.

```json
{
  "tasks.max": 2,
  "topics": "names",
  "connector.class": "io.littlehorse.connect.WfRunSinkConnector",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.json.JsonConverter",
  "value.converter.schemas.enable": false,
  "lhc.api.port": 2023,
  "lhc.api.host": "localhost",
  "lhc.tenant.id": "default",
  "wf.spec.name": "greetings"
}
```

More configurations at [WfRun Sink Connector Configurations](https://github.com/littlehorse-enterprises/lh-kafka-connect/blob/main/CONFIGURATIONS.md#wfrunsinkconnector-configurations).

### Struct Variables

This connector supports [StructDef](https://littlehorse.io/docs/server/concepts/structs) variables.
On startup it reads the `WfSpec` referenced by `wf.spec.name` (honoring `wf.spec.major.version`
and `wf.spec.revision` when provided) to discover which input variables are `STRUCT` types.
Variables backed by a `StructDef` are built from the matching nested object in the message value,
while every other variable keeps its regular type. No additional configuration is required.

Given a workflow with a `pilot` `STRUCT` variable:

```java
Workflow workflow = Workflow.newWorkflow("greetings", wf -> {
    WfRunVariable pilot = wf.declareStruct("pilot", Pilot.class);
    wf.execute("greet", pilot);
});
```

Produce a message whose value contains the struct fields under the variable name:

```text
key: null, value: {"pilot":{"name":"Anakin Skywalker","vehicle":{"model":"Podracer"}}}
```

> If the `WfSpec` cannot be loaded at startup, the connector fails to start, so make sure the
> `WfSpec` is registered before the connector runs.

## ExternalEventSinkConnector

This connector allows you to execute [External Events](https://littlehorse.io/docs/server/concepts/external-events) into LittleHorse.

More about running external events at [LittleHorse External Events](https://littlehorse.io/docs/server/concepts/external-events#in-practice).

### Expected Message Structure

| Message Part | Description                                                                                     | Type   | Valid Values    |
|--------------|-------------------------------------------------------------------------------------------------|--------|-----------------|
| `key`        | Optional, define the associated `wfRunId`. Precedence: 1. `wfRunId` header key, 2. message key. | string | hostname format |
| `value`      | Define the `content` of the external event.                                                     | any    | any not null    |

More about external event fields at [PutExternalEventRequest](https://littlehorse.io/docs/server/api#putexternaleventrequest).

You can manipulate the message structure with [Single Message Transformations (SMTs)](https://docs.confluent.io/kafka-connectors/transforms/current/overview.html).

### Additional Metadata

Optionally this sink connector uses the record headers to configure the external event:

| Header Key | Description                                                                                                                                            | Type   | Valid Values    |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|--------|-----------------|
| `wfRunId`  | Associated `wfRunId`. It looks for the `wfRunId` in the message key if it is not provided in the headers.                                              | string | hostname format |
| `guid`     | Optional, sets the unique guid for the external event.  The connector genererates an id if not present, check [Idempotent Writes](#idempotent-writes). | string | hostname format |

### Quick Example

Next workflow waits for the event `set-name` to assign the variable `name` and then execute the task `greet`.

```java
Workflow workflow = Workflow.newWorkflow("greetings", wf -> {
    WfRunVariable name = wf.declareStr("name");
    name.assign(wf.waitForEvent("set-name"));
    wf.execute("greet", name);
});
```

There is a topic `names` with this data:

```text
key: 64512de2a4b5470a9a8a2846b9a8a444, value: Anakin Skywalker
key: 79af0ae572bb4c19842c19dd7cad6598, value: Luke Skywalker
key: 30e1afe9a30748339594cadc3d537ecd, value: Leia Organa
key: e01547de3d294efdb6417abf35f3c960, value: Padme Amidala
```

Next configuration will execute external events where the message key will be the `WfRunId` and
the message value will be the `Content` (more at [PutExternalEventRequest](https://littlehorse.io/docs/server/api#putexternaleventrequest)):

```json
{
  "tasks.max": 2,
  "topics": "names",
  "connector.class": "io.littlehorse.connect.ExternalEventSinkConnector",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.storage.StringConverter",
  "lhc.api.port": 2023,
  "lhc.api.host": "localhost",
  "lhc.tenant.id": "default",
  "external.event.name": "set-name"
}
```

More configurations at [ExternalEvent Sink Connector Configurations](https://github.com/littlehorse-enterprises/lh-kafka-connect/blob/main/CONFIGURATIONS.md#externaleventsinkconnector-configurations).

### Struct Content

This connector supports [StructDef](https://littlehorse.io/docs/server/concepts/structs) content.
On startup it reads the `ExternalEventDef` referenced by `external.event.name` to discover whether
its content is a `STRUCT` type. When it is, the connector builds the LittleHorse struct from the
message value automatically; otherwise the content keeps its regular type. No additional
configuration is required.

Given a workflow that waits for an event whose content is a `pilot` `STRUCT`:

```java
Workflow workflow = Workflow.newWorkflow("greetings", wf -> {
    wf.execute("greet", wf.waitForEvent("set-pilot").registeredAs(Pilot.class));
});
```

Produce a message whose value contains the struct fields:

```text
key: 64512de2a4b5470a9a8a2846b9a8a444, value: {"name":"Anakin Skywalker","vehicle":{"model":"Podracer"}}
```

> If the `ExternalEventDef` cannot be loaded at startup, the connector fails to start, so make
> sure the `ExternalEventDef` is registered before the connector runs.

## CorrelatedEventSinkConnector

###  Expected Message Structure

| Message Part | Description                                                                                                 | Type   | Valid Values     |
|--------------|-------------------------------------------------------------------------------------------------------------|--------|------------------|
| `key`        | Optional, define the associated `correlationId`. Precedence: 1. `correlationId` header key, 2. message key. | string | non-empty string |
| `value`      | Define the `content` of the correlated event.                                                               | any    | any not null     |

More about correlated event fields at [PutCorrelatedEventRequest](https://littlehorse.io/docs/server/api#putcorrelatedeventrequest).

You can manipulate the message structure with [Single Message Transformations (SMTs)](https://docs.confluent.io/kafka-connectors/transforms/current/overview.html).

### Additional Metadata

Optionally this sink connector uses the record headers to configure the external event:

| Header Key      | Description                                                                                                           | Type   | Valid Values     |
|-----------------|-----------------------------------------------------------------------------------------------------------------------|--------|------------------|
| `correlationId` | Associated `correlationId`. It looks for the `correlationId` in the message key if it is not provided in the headers. | string | non-empty string |

### Quick Example

Next workflow waits for the event `payment-id` with a specific id (`CorrelationId`),
when the correlated event is trigger with the same id the workflow is allowed to continue.

```java
Workflow workflow = Workflow.newWorkflow("process-payment", wf -> {
    WfRunVariable paymentId = wf.declareStr("payment-id");
    wf.execute("process-payment", wf.waitForEvent("payment-id").withCorrelationId(paymentId));
});
```

There is a topic `payments` with this data:

```text
key: d1e912b0cffe40138e452d413dc8ab53, value: {"name":"R2-D2","credits":6279.0}
key: 8f8e36ef6cb7476fafcd95493d5a183d, value: {"name":"C-3PO","credits":6286.0}
key: b31289d3b1484ef4945b31baf6df58f3, value: {"name":"BB-8","credits":5047.0}
key: 9aa240b59cd74590a01939fa4c87ebea, value: {"name":"Super Battle Droid","credits":9607.0}
```

Next configuration will execute external events where the message `key` will be the `CorrelationId` and
the message `value` will be the `Content` (more at [PutCorrelatedEventRequest](https://littlehorse.io/docs/server/api#putcorrelatedeventrequest)):

```json
{
  "tasks.max": 2,
  "connector.class": "io.littlehorse.connect.CorrelatedEventSinkConnector",
  "topics": "payments",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.json.JsonConverter",
  "value.converter.schemas.enable": false,
  "lhc.api.port": 2024,
  "lhc.api.host": "littlehorse",
  "lhc.tenant.id": "default",
  "external.event.name": "payment-id"
}
```

More configurations at [CorrelatedEvent Sink Connector Configurations](https://github.com/littlehorse-enterprises/lh-kafka-connect/blob/main/CONFIGURATIONS.md#correlatedeventsinkconnector-configurations).

### Struct Content

This connector supports [StructDef](https://littlehorse.io/docs/server/concepts/structs) content.
On startup it reads the `ExternalEventDef` referenced by `external.event.name` to discover whether
its content is a `STRUCT` type. When it is, the connector builds the LittleHorse struct from the
message value automatically; otherwise the content keeps its regular type. No additional
configuration is required.

Given a workflow that waits for a correlated event whose content is a `pilot` `STRUCT`:

```java
Workflow workflow = Workflow.newWorkflow("greetings", wf -> {
    WfRunVariable pilotId = wf.declareStr("pilot-id");
    wf.execute("greet", wf.waitForEvent("set-pilot")
            .withCorrelationId(pilotId)
            .registeredAs(Pilot.class));
});
```

Produce a message whose key is the `correlationId` and whose value contains the struct fields:

```text
key: 64512de2a4b5470a9a8a2846b9a8a444, value: {"name":"Anakin Skywalker","vehicle":{"model":"Podracer"}}
```

> If the `ExternalEventDef` cannot be loaded at startup, the connector fails to start, so make
> sure the `ExternalEventDef` is registered before the connector runs.

## Idempotent Writes

To ensure idempotency, we generate a unique id
for each request to LH in **lowercase** and with the next format:

`{connector name}-{topic name}-{partition}-{offset}`

The **connector name** must be a valid hostname format, example `my-littlehorse-connector1`.
The **topic name** will be changed to a valid hostname format, example: `My_Topic` to `my-topic`.
A **hostname** is a lowercase alphanumeric string separated by a `-`.

LH does not support uppercase letters for defining WfRunIds, and the only special character allowed is `-`.
More at [LittleHorse Variables](https://littlehorse.io/docs/server/developer-guide/wfspec-development/basics#defining-a-wfrunvariable).

> If two topics generate the same unique id (example: `My_Topic` and `My.Topic` generate `my-topic`)
> it is recommended to create two different connectors.

## Multiple Tasks

These connectors support parallelism by running more than one task.
Specify the number of tasks in the `tasks.max` configuration parameter.

More configurations at [Configure Sink Connector](https://docs.confluent.io/platform/current/installation/configuration/connect/sink-connect-configs.html).

## Dead Letter Queue

These connectors support Dead Letter Queue (DLQ).

More about DLQs at [Kafka Connect Dead Letter Queue](https://docs.confluent.io/platform/current/connect/index.html#dead-letter-queue).

### Error Handling

These connectors distinguish between two kinds of failures so that transient
problems do not cause data loss:

- **Transient errors** are retried. When a gRPC call to LittleHorse fails with a
  retriable status code (`CANCELLED`, `DEADLINE_EXCEEDED`, `RESOURCE_EXHAUSTED`,
  `ABORTED`, `INTERNAL` or `UNAVAILABLE` — e.g. network issues or the server
  being temporarily unavailable), the record is **not** sent to the DLQ. Instead
  the connector asks Kafka Connect to retry, since the record is valid and may
  succeed on a later attempt. This happens regardless of the `errors.tolerance`
  setting.
- **Permanent errors** are treated as bad records. Any other failure (e.g.
  `INVALID_ARGUMENT`, `FAILED_PRECONDITION`, a malformed payload, or a missing
  required field) cannot succeed on retry, so it is handled according to
  `errors.tolerance`:
  - `errors.tolerance=none` (default): the task fails immediately.
  - `errors.tolerance=all`: the record is sent to the DLQ (when configured) and
    its offset is committed so processing continues.

> [!NOTE]
> The DLQ only captures permanent errors raised while delivering records to
> LittleHorse. Deserialization and transformation errors are routed to the DLQ
> by Kafka Connect itself.

Retry timing is controlled by the standard Kafka Connect configurations
`errors.retry.timeout` and `errors.retry.delay.max.ms`.

See the [wfrun-dlq example](examples/wfrun-dlq/README.md) for a runnable setup.

## Data Types

Note that LittleHorse kernel is data type aware.  When reading data from the Kafka topic with either [WfRunSinkConnector](#wfrunsinkconnector) or [ExternalEventSinkConnector](#externaleventsinkconnector) the data types in the topic correlate with the data LittleHorse kernel expects.

A common issue is with the Boolean data type.  If LittleHorse kernel expects a Boolean type "True" or "False", this must match Boolean data type in the schema of the topic.

For testing, it is common to use `kafka-console-producer.sh` tool provided by Apache Kafka, this tool can only produce String or Integer values. In order to accuratly send a primitive type other than String or Interger you must use a converter in the Kafka Connect connector configuration.

Example:
```json
{
  "name": "external-identity-verified",
  "config": {
    "tasks.max": 2,
    "topics": "names",
    "connector.class": "io.littlehorse.connect.ExternalEventSinkConnector",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "org.apache.kafka.connect.storage.StringConverter",
    "lhc.api.port": 2023,
    "lhc.api.host": "localhost",
    "lhc.tenant.id": "default",
    "transforms": "Cast",
    "transforms.Cast.type": "org.apache.kafka.connect.transforms.Cast$Value",
    "transforms.Cast.spec": "boolean",
    "external.event.name": "identity-verified"
  }
}
```

Note the lines that begin with "transforms", with those we are casting the String data type sent by `kafka-console-producer.sh` to the primitive Boolean.

For more information:

- [Single Message Transformations (SMTs)](https://docs.confluent.io/kafka-connectors/transforms/current/overview.html).
- [Cast SMT](https://docs.confluent.io/kafka-connectors/transforms/current/cast.html).

## Converters

These connectors support `Protobuf`, `Json` and `Avro` through converters.

More about converters at [Kafka Connect Converters](https://docs.confluent.io/platform/current/connect/index.html#converters)

## External Secrets

Kafka connect ensures provisioning secrets through the [ConfigProvider](https://kafka.apache.org/20/javadoc/org/apache/kafka/common/config/provider/ConfigProvider.html) interface, so these connectors support external secrets by default.

More about secrets at [Externalize Secrets](https://docs.confluent.io/platform/current/connect/security.html#externalize-secrets).

## Configurations

- [WfRun Sink Connector Configurations](https://github.com/littlehorse-enterprises/lh-kafka-connect/blob/main/CONFIGURATIONS.md#wfrunsinkconnector-configurations).
- [ExternalEvent Sink Connector Configurations](https://github.com/littlehorse-enterprises/lh-kafka-connect/blob/main/CONFIGURATIONS.md#externaleventsinkconnector-configurations).
- [Kafka Sink Connector Configurations](https://docs.confluent.io/platform/current/installation/configuration/connect/sink-connect-configs.html).
- [LittleHorse Client Configurations](https://littlehorse.io/docs/server/developer-guide/client-configuration#client-config-options).

## Download

<a href="https://github.com/littlehorse-enterprises/lh-kafka-connect/releases"><img alt="github" src="https://img.shields.io/badge/releases-orange?logo=github&logoColor=white"></a>
<a href="https://github.com/littlehorse-enterprises/lh-kafka-connect/releases"><img alt="GitHub Release" src="https://img.shields.io/github/v/release/littlehorse-enterprises/lh-kafka-connect?label=latest"></a>

For all available versions go to [GitHub Releases](https://github.com/littlehorse-enterprises/lh-kafka-connect/releases).

## Versioning

These connectors keep the same versioning as [LittleHorse](https://github.com/littlehorse-enterprises/littlehorse/releases).

## Examples

For more examples go to [examples](https://github.com/littlehorse-enterprises/lh-kafka-connect/tree/main/examples).

## Development

For development instructions go to [DEVELOPMENT.md](https://github.com/littlehorse-enterprises/lh-kafka-connect/blob/main/DEVELOPMENT.md).

## Dependencies

- Java version 17 or greater.
- Apache Kafka version 3.8 or greater, equivalent to Confluent Platform 7.8 or greater ([Interoperability for Confluent Platform](https://docs.confluent.io/platform/current/installation/versions-interoperability.html#cp-and-apache-ak-compatibility)).
- LittleHorse version 1.0 or greater.

## License

<a href="https://spdx.org/licenses/SSPL-1.0.html"><img alt="SSPL LICENSE" src="https://img.shields.io/badge/covered%20by-SSPL%201.0-blue"></a>

All code in this repository is covered by the [Server Side Public License, Version 1](https://spdx.org/licenses/SSPL-1.0.html) and is copyright of LittleHorse Enterprises LLC.
