package io.littlehorse.connect.record;

import lombok.Getter;

import org.apache.kafka.connect.sink.SinkRecord;

@Getter
public class IdempotentSinkRecord extends SinkRecord {
    private final String idempotencyKey;

    public IdempotentSinkRecord(String idempotencyKey, SinkRecord base) {
        super(
                base.topic(),
                base.kafkaPartition(),
                base.keySchema(),
                base.key(),
                base.valueSchema(),
                base.value(),
                base.kafkaOffset(),
                base.timestamp(),
                base.timestampType(),
                base.headers(),
                base.originalTopic(),
                base.originalKafkaPartition(),
                base.originalKafkaOffset());
        this.idempotencyKey = idempotencyKey;
    }
}
