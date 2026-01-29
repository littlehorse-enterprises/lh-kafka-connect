package io.littlehorse.connect.record;

import static io.littlehorse.connect.record.IdempotentSinkRecord.PARENT_WF_RUN_ID;
import static io.littlehorse.connect.record.IdempotentSinkRecord.WF_RUN_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.datafaker.Faker;

import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class IdempotentSinkRecordTest {

    Faker faker = new Faker();

    @Test
    void shouldReturnIdempotencyKeyIfWfRunIdHeaderDoesNotExist() {
        String connectorName = "test-connector";
        String topic = "my-topic";
        int partition = faker.number().positive();
        long offset = faker.number().positive();
        SinkRecord base = mock();
        when(base.topic()).thenReturn(topic);
        when(base.kafkaPartition()).thenReturn(partition);
        when(base.kafkaOffset()).thenReturn(offset);
        IdempotentSinkRecord record = new IdempotentSinkRecord(connectorName, base);

        assertThat(record.wfRunId())
                .isEqualTo(connectorName + "-" + topic + "-" + partition + "-" + offset);
    }

    @Test
    void shouldReturnHeaderIfWfRunIdHeaderExists() {
        String connectorName = "test-connector";
        String wfRunId = UUID.randomUUID().toString();

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(wfRunId);

        IdempotentSinkRecord record = new IdempotentSinkRecord(connectorName, base);

        assertThat(record.wfRunId()).isEqualTo(wfRunId);
    }

    @Test
    void shouldThrowsExceptionIfWfRunIdHeaderIsNull() {
        String connectorName = "test-connector";

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(null);

        IdempotentSinkRecord record = new IdempotentSinkRecord(connectorName, base);

        assertThrows(DataException.class, record::wfRunId);
    }

    @Test
    void shouldThrowsExceptionIfWfRunIdHeaderIsNotString() {
        String connectorName = "test-connector";

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(faker.number().positive());

        IdempotentSinkRecord record = new IdempotentSinkRecord(connectorName, base);

        assertThrows(DataException.class, record::wfRunId);
    }

    @Test
    void shouldReturnNullIfParentWfRunIdHeaderDoesNotExist() {
        String connectorName = "test-connector";
        SinkRecord base = mock();
        IdempotentSinkRecord record = new IdempotentSinkRecord(connectorName, base);

        assertThat(record.parentWfRunId()).isNull();
    }

    @Test
    void shouldReturnHeaderIfParentWfRunIdHeaderExists() {
        String connectorName = "test-connector";
        String parentWfRunIdfRunId = UUID.randomUUID().toString();

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(PARENT_WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(parentWfRunIdfRunId);

        IdempotentSinkRecord record = new IdempotentSinkRecord(connectorName, base);

        assertThat(record.parentWfRunId()).isEqualTo(parentWfRunIdfRunId);
    }

    @Test
    void shouldThrowsExceptionIfParentWfRunIdHeaderIsNull() {
        String connectorName = "test-connector";

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(PARENT_WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(null);

        IdempotentSinkRecord record = new IdempotentSinkRecord(connectorName, base);

        assertThrows(DataException.class, record::parentWfRunId);
    }

    @Test
    void shouldThrowsExceptionIfParentWfRunIdHeaderIsNotString() {
        String connectorName = "test-connector";

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(PARENT_WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(faker.number().positive());

        IdempotentSinkRecord record = new IdempotentSinkRecord(connectorName, base);

        assertThrows(DataException.class, record::parentWfRunId);
    }
}
