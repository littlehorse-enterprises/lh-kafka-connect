package io.littlehorse.connect.record;

import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;

public class IdempotentSinkRecord extends SinkRecord {
    public static final String WF_RUN_ID = "wfRunId";
    public static final String PARENT_WF_RUN_ID = "parentWfRunId";
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getWfRunId() {
        Header wfRunId = headers().lastWithName(WF_RUN_ID);
        if (wfRunId == null) {
            return getIdempotencyKey();
        }

        Object value = wfRunId.value();
        if (value == null) {
            throw new DataException("Expected not null wfRunId header");
        }

        if (!(value instanceof String)) {
            throw new DataException("Expected String not provided for wfRunId header");
        }

        return value.toString();
    }

    public String getParentWfRunId() {
        Header parentWfRunId = headers().lastWithName(PARENT_WF_RUN_ID);
        return parentWfRunId != null ? parentWfRunId.value().toString() : null;
    }
}
