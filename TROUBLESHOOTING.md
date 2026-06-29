# Troubleshooting

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
