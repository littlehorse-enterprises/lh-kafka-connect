# Docker Images

This folder contains the Dockerfiles used to build container images for the
LittleHorse Kafka Connect sink connectors. Each image targets a specific
runtime, so the file name makes the target explicit (e.g. `Dockerfile.strimzi`).
More images may be added here in the future.

## Available images

| File                 | Base image                            | Purpose                                                                          |
|----------------------|---------------------------------------|----------------------------------------------------------------------------------|
| `Dockerfile.strimzi` | `quay.io/strimzi/kafka:<strimzi>-kafka:<kafka>` | Strimzi-compatible Kafka Connect image, for use as the `image` of a `KafkaConnect` CRD. |

## Strimzi image

The `Dockerfile.strimzi` image bundles the connector plugins on top of the
official Strimzi Kafka image. It is meant to be referenced from a custom
`KafkaConnect` custom resource.

### Published images

Images are published to the GitHub Container Registry:

```text
ghcr.io/littlehorse-enterprises/lh-kafka-connect/strimzi
```

### Configurable versions

The base image is built from two build args:

| Build arg         | Default                | Description                                                        |
|-------------------|------------------------|--------------------------------------------------------------------|
| `STRIMZI_VERSION` | `0.50.1`               | Strimzi version, first half of the base image tag.                 |
| `KAFKA_VERSION`   | `4.1.0`                | Kafka version, second half of the base image tag.                  |
| `BUNDLE_PATH`     | `connector/build/bundle` | Path to the built connector bundle within the build context.     |
| `PLUGIN_PATH`     | `/opt/kafka/plugins`   | Directory where Strimzi discovers connector plugins.               |

Together, `STRIMZI_VERSION` and `KAFKA_VERSION` resolve the base image:
`quay.io/strimzi/kafka:${STRIMZI_VERSION}-kafka-${KAFKA_VERSION}`.

## Command utilities

All commands are run from the **repository root** (the Docker build context is
the repository root).

### 1. Build the connector bundle

The image copies the built connector bundle, so build it first:

```shell
./gradlew connector:buildConfluentBundle
```

This produces the bundle under `connector/build/bundle/`.

### 2. Build the Strimzi image

Using the default versions:

```shell
docker build \
  -f docker/Dockerfile.strimzi \
  -t ghcr.io/littlehorse-enterprises/lh-kafka-connect/strimzi:local .
```

Overriding the Strimzi and/or Kafka versions:

```shell
docker build \
  -f docker/Dockerfile.strimzi \
  --build-arg STRIMZI_VERSION=0.50.1 \
  --build-arg KAFKA_VERSION=4.1.0 \
  -t ghcr.io/littlehorse-enterprises/lh-kafka-connect/strimzi:local .
```

### 3. Inspect the plugins in the image

```shell
docker run --rm \
  ghcr.io/littlehorse-enterprises/lh-kafka-connect/strimzi:local \
  ls -R /opt/kafka/plugins
```

### 4. Push the image (optional, done automatically by CI)

```shell
echo "$GITHUB_TOKEN" | docker login ghcr.io -u "$GITHUB_USER" --password-stdin
docker push ghcr.io/littlehorse-enterprises/lh-kafka-connect/strimzi:local
```

## Using the image with a KafkaConnect CRD

Reference the published image from your `KafkaConnect` custom resource so the
connector plugins are available to the cluster:

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnect
metadata:
  name: lh-connect
  annotations:
    strimzi.io/use-connector-resources: "true"
spec:
  version: 4.1.0
  replicas: 1
  bootstrapServers: my-cluster-kafka-bootstrap:9092
  image: ghcr.io/littlehorse-enterprises/lh-kafka-connect/strimzi:1.1.1
  config:
    group.id: lh-connect
    offset.storage.topic: lh-connect-offsets
    config.storage.topic: lh-connect-configs
    status.storage.topic: lh-connect-status
```
