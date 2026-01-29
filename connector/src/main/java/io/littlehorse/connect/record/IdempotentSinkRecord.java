package io.littlehorse.connect.record;

import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;

public class IdempotentSinkRecord extends SinkRecord {
    public static final String WF_RUN_ID = "wfRunId";
    public static final String PARENT_WF_RUN_ID = "parentWfRunId";
    private final String connectorName;

    public IdempotentSinkRecord(String connectorName, SinkRecord base) {
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
        this.connectorName = connectorName;
    }

    public String idempotencyKey() {
        // to ensure idempotency we use: connector name + topic + partition + offset
        return String.format(
                        "%s-%s-%d-%d", connectorName(), topic(), kafkaPartition(), kafkaOffset())
                // a topic supports ".", "_" and upper case
                .toLowerCase()
                .replace("_", "-")
                .replace(".", "-");
    }

    public String connectorName() {
        return connectorName;
    }

    public String wfRunId() {
        Header wfRunId = headers().lastWithName(WF_RUN_ID);
        if (wfRunId == null) {
            return idempotencyKey();
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

    public String parentWfRunId() {
        Header parentWfRunId = headers().lastWithName(PARENT_WF_RUN_ID);
        if (parentWfRunId == null) {
            return null;
        }

        Object value = parentWfRunId.value();
        if (value == null) {
            throw new DataException("Expected not null parentWfRunId header");
        }

        if (!(value instanceof String)) {
            throw new DataException("Expected String not provided for parentWfRunId header");
        }

        return value.toString();
    }
}
