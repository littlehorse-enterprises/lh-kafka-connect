# LittleHorse Connectors for Kafka Connect

These connectors allow data transfer between Apache Kafka and LittleHorse.

<!-- TOC -->
* [LittleHorse Connectors for Kafka Connect](#littlehorse-connectors-for-kafka-connect)
  * [WfRunSinkConnector](#wfrunsinkconnector)
    * [Features](#features)
      * [Idempotent Writes](#idempotent-writes)
      * [Multiple Tasks](#multiple-tasks)
      * [Dead Letter Queue](#dead-letter-queue)
      * [Message Structure](#message-structure)
      * [Configurations](#configurations)
    * [Quick Example](#quick-example)
  * [ExternalEventSinkConnector](#externaleventsinkconnector)
    * [Features](#features-1)
      * [Idempotent Writes](#idempotent-writes-1)
      * [Multiple Tasks](#multiple-tasks-1)
      * [Dead Letter Queue](#dead-letter-queue-1)
      * [Message Structure](#message-structure-1)
      * [Configurations](#configurations-1)
    * [Quick Example](#quick-example-1)
  * [Configurations](#configurations-2)
  * [Examples](#examples)
  * [Development](#development)
  * [Licencing](#licencing)
<!-- TOC -->

## WfRunSinkConnector

This connector allows you to execute [WfRuns](https://littlehorse.io/docs/server/concepts/workflows#the-wfrun) into LittleHorse.
It supports all the [Variable Types](https://littlehorse.io/docs/server/concepts/variables) provided by LittleHorse.
More about running workflows at [LittleHorse Quickstart](https://littlehorse.io/docs/server/getting-started/quickstart).

### Features

#### Idempotent Writes

To ensure idempotency, this connector generates unique [WfRunIds](https://littlehorse.io/docs/server/developer-guide/grpc/running-workflows#passing-the-id)
with the format: `{connector name}-{topic name}-{partition}-{offset}`.

#### Multiple Tasks

The `WfRunSinkConnector` supports running one or more tasks.
Specify the number of tasks in the `tasks.max` configuration parameter.

#### Dead Letter Queue

This connector supports the Dead Letter Queue (DLQ) functionality.
More about DLQs at [Kafka Connect Dead Letter Queue](https://docs.confluent.io/platform/current/connect/index.html#dead-letter-queue).

#### Message Structure

| Message Part | Description                                  | Type | Valid Values       |
|--------------|----------------------------------------------|------|--------------------|
| `key`        | Ignored                                      | any  | any                |
| `value`      | Define the `variables` field of the workflow | map  | key-value not null |

More about run workflow fields at [RunWfRequest](https://littlehorse.io/docs/server/api#runwfrequest).

#### Configurations

Configurations at [WfRunSinkConnector Configurations](CONFIGURATIONS.md#wfrunsinkconnector-configurations).

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

## ExternalEventSinkConnector

This connector allows you to execute [External Events](https://littlehorse.io/docs/server/concepts/external-events) into LittleHorse.

More about running external events at [LittleHorse External Events](https://littlehorse.io/docs/server/concepts/external-events).

### Features

#### Idempotent Writes

To ensure idempotency, this connector generates unique [GUID](https://littlehorse.io/docs/server/concepts/external-events#posting-externalevents)
for external events with the format: `{connector name}-{topic name}-{partition}-{offset}`.

#### Multiple Tasks

The `ExternalEventSinkConnector` supports running one or more tasks.
Specify the number of tasks in the `tasks.max` configuration parameter.

#### Dead Letter Queue

This connector supports the Dead Letter Queue (DLQ) functionality.
More about DLQs at [Kafka Connect Dead Letter Queue](https://docs.confluent.io/platform/current/connect/index.html#dead-letter-queue).

#### Message Structure

| Message Part | Description                                | Type   | Valid Values     |
|--------------|--------------------------------------------|--------|------------------|
| `key`        | Define the associated `wf_run_id`          | string | non-empty string |
| `value`      | Define the `content` of the external event | any    | any not null     |

More about external event fields at [PutExternalEventRequest](https://littlehorse.io/docs/server/api#putexternaleventrequest).

#### Configurations

Configurations at [ExternalEventSinkConnector Configurations](CONFIGURATIONS.md#externaleventsinkconnector-configurations).

### Quick Example

Next workflow waits for the event `set-name` to assign the variable `name` and then execute the task `greet`.

```java
Workflow workflow = Workflow.newWorkflow("greetings", wf -> {
    WfRunVariable name = wf.declareStr("name");
    name.assign(wf.waitForEvent("set-name"));
    wf.execute("greet", name);
});
```

There is a topic `name` with this data:

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

## Configurations

- [LittleHorse Sink Connector Configurations](CONFIGURATIONS.md).
- [Kafka Sink Connector Configurations](https://docs.confluent.io/platform/current/installation/configuration/connect/sink-connect-configs.html).
- [LittleHorse Client Configurations](https://littlehorse.io/docs/server/developer-guide/client-configuration#client-config-options).

## Examples

For more examples go to [examples](examples).

## Development

For development instructions go to [DEVELOPMENT.md](DEVELOPMENT.md).

## Licencing

<a href="https://spdx.org/licenses/SSPL-1.0.html"><img alt="SSPL LICENSE" src="https://img.shields.io/badge/covered%20by-SSPL%201.0-blue"></a>

All code in this repository is covered by the [Server Side Public License, Version 1](https://spdx.org/licenses/SSPL-1.0.html) and is copyright of LittleHorse Enterprises LLC.
