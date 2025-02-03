# Demo

Build plugin bundle:

```shell
./gradlew connector:buildBundle
```

Run environment:

```shell
docker compose up -d
```

Create topic:

```shell
kafka-topics --create --bootstrap-server localhost:19092 \
             --replication-factor 3 \
             --partitions 12 \
             --topic demo
```

Populate topic:

```shell
./gradlew demo:run --args="produce -n 10 demo"
```
