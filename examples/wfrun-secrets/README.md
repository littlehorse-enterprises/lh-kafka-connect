# WfRun Secrets

In this example you will:

- Register a workflow with variable types: `STR`.
- Produce messages to a kafka topic without SchemaRegistry.
- Create a WfRunSinkConnector with a simple transformation.
- Read secrets with config providers ([Externalize Secrets](https://docs.confluent.io/platform/current/connect/security.html#externalize-secrets))

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
--topic example-wfrun-secrets
```

Produce:

```shell
docker compose exec -T kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-secrets \
< examples/wfrun-secrets/data.txt
```

Consume:

> [!NOTE]
> In case you need to verify the messages in the topic.

```shell
docker compose exec kafka-connect \
kafka-console-consumer --bootstrap-server kafka1:9092 \
--topic example-wfrun-secrets \
--from-beginning
```

> [!NOTE]
> If you need to generate new data run:

```shell
./gradlew -q example-wfrun-secrets:run -DmainClass="io.littlehorse.example.DataGenerator" --args="10" > examples/wfrun-secrets/data.txt
```

## Run Worker

Run worker:

```shell
./gradlew example-wfrun-secrets:run
```

## Create Connector

Create connector:

```shell
http PUT :8083/connectors/example-wfrun-secrets/config < examples/wfrun-secrets/connector.json
```

Get connector:

```shell
http :8083/connectors/example-wfrun-secrets
```

Check the configurations:

```json
{
  "tasks.max": 2,
  "connector.class": "io.littlehorse.connect.WfRunSinkConnector",
  "topics": "example-wfrun-secrets",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.storage.StringConverter",
  "transforms": "HoistField",
  "transforms.HoistField.type": "org.apache.kafka.connect.transforms.HoistField$Value",
  "transforms.HoistField.field": "name",
  "config.providers": "file,env",
  "config.providers.file.class": "org.apache.kafka.common.config.provider.FileConfigProvider",
  "config.providers.env.class": "org.apache.kafka.common.config.provider.EnvVarConfigProvider",
  "lhc.api.port": 2024,
  "lhc.api.host": "littlehorse",
  "lhc.tenant.id": "${file:/home/appuser/secrets.properties:lhc.tenant.id}",
  "wf.spec.name": "${env:WF_SPEC_NAME}"
}
```

We added two configs providers: `"config.providers": "file,env"`.
The `FileConfigProvider` will extract the `lhc.tenant.id` config from `/home/appuser/secrets.properties`.
`EnvVarConfigProvider` will do the same with the variable `WF_SPEC_NAME` for `wf.spec.name`.


## Check WfRuns

List WfRuns:

```shell
lhctl search wfRun --wfSpecName example-wfrun-secrets
```

## Produce in Interactive Shell

```shell
docker compose exec kafka-connect \
kafka-console-producer --bootstrap-server kafka1:9092 \
--topic example-wfrun-secrets
```
