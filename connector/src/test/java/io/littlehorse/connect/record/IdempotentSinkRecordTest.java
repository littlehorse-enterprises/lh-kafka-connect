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
        String idempotencyKey = UUID.randomUUID().toString();
        SinkRecord base = mock();
        IdempotentSinkRecord record = new IdempotentSinkRecord(idempotencyKey, base);

        assertThat(record.getWfRunId()).isEqualTo(idempotencyKey);
    }

    @Test
    void shouldReturnHeaderIfWfRunIdHeaderExists() {
        String idempotencyKey = UUID.randomUUID().toString();
        String wfRunId = UUID.randomUUID().toString();

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(wfRunId);

        IdempotentSinkRecord record = new IdempotentSinkRecord(idempotencyKey, base);

        assertThat(record.getWfRunId()).isEqualTo(wfRunId);
    }

    @Test
    void shouldThrowsExceptionIfWfRunIdHeaderIsNull() {
        String idempotencyKey = UUID.randomUUID().toString();

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(null);

        IdempotentSinkRecord record = new IdempotentSinkRecord(idempotencyKey, base);

        assertThrows(DataException.class, record::getWfRunId);
    }

    @Test
    void shouldThrowsExceptionIfWfRunIdHeaderIsNotString() {
        String idempotencyKey = UUID.randomUUID().toString();

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(faker.number().positive());

        IdempotentSinkRecord record = new IdempotentSinkRecord(idempotencyKey, base);

        assertThrows(DataException.class, record::getWfRunId);
    }

    @Test
    void shouldReturnNullIfParentWfRunIdHeaderDoesNotExist() {
        String idempotencyKey = UUID.randomUUID().toString();
        SinkRecord base = mock();
        IdempotentSinkRecord record = new IdempotentSinkRecord(idempotencyKey, base);

        assertThat(record.getParentWfRunId()).isNull();
    }

    @Test
    void shouldReturnHeaderIfParentWfRunIdHeaderExists() {
        String idempotencyKey = UUID.randomUUID().toString();
        String parentWfRunIdfRunId = UUID.randomUUID().toString();

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(PARENT_WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(parentWfRunIdfRunId);

        IdempotentSinkRecord record = new IdempotentSinkRecord(idempotencyKey, base);

        assertThat(record.getParentWfRunId()).isEqualTo(parentWfRunIdfRunId);
    }

    @Test
    void shouldThrowsExceptionIfParentWfRunIdHeaderIsNull() {
        String idempotencyKey = UUID.randomUUID().toString();

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(PARENT_WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(null);

        IdempotentSinkRecord record = new IdempotentSinkRecord(idempotencyKey, base);

        assertThrows(DataException.class, record::getParentWfRunId);
    }

    @Test
    void shouldThrowsExceptionIfParentWfRunIdHeaderIsNotString() {
        String idempotencyKey = UUID.randomUUID().toString();

        SinkRecord base = mock();
        ConnectHeaders headers = mock();
        Header header = mock();
        when(base.headers()).thenReturn(headers);
        when(headers.lastWithName(PARENT_WF_RUN_ID)).thenReturn(header);
        when(header.value()).thenReturn(faker.number().positive());

        IdempotentSinkRecord record = new IdempotentSinkRecord(idempotencyKey, base);

        assertThrows(DataException.class, record::getParentWfRunId);
    }
}
