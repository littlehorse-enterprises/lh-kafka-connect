package io.littlehorse.connect.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses a record into a JSONPath {@link DocumentContext} backed by an envelope of the record:
 * {@code {key, value, headers, partition, offset}}. {@code Struct} values are flattened to plain
 * maps/lists so they can be navigated by JSONPath, and missing paths resolve to {@code null}
 * instead of throwing.
 *
 * <p>{@code offset} is only populated for {@link SinkRecord}s (source records carry no Kafka
 * offset, so it resolves to {@code null}), and {@code partition} is {@code null} whenever the
 * record has no assigned partition.
 */
public final class JsonPathEvaluator {

    private static final String KEY_DOMAIN = "key";
    private static final String VALUE_DOMAIN = "value";
    private static final String HEADERS_DOMAIN = "headers";
    private static final String PARTITION_DOMAIN = "partition";
    private static final String OFFSET_DOMAIN = "offset";

    private static final Configuration CONFIG = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentContext parse(ConnectRecord<?> record) {
        return JsonPath.using(CONFIG).parse(buildEnvelope(record));
    }

    private Map<String, Object> buildEnvelope(ConnectRecord<?> record) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put(KEY_DOMAIN, objectMapper.removeStruct(record.key()));
        envelope.put(VALUE_DOMAIN, objectMapper.removeStruct(record.value()));
        envelope.put(HEADERS_DOMAIN, headersToMap(record.headers()));
        envelope.put(PARTITION_DOMAIN, record.kafkaPartition());
        envelope.put(
                OFFSET_DOMAIN,
                record instanceof SinkRecord ? ((SinkRecord) record).kafkaOffset() : null);
        return envelope;
    }

    private Map<String, Object> headersToMap(Iterable<Header> headers) {
        Map<String, Object> result = new HashMap<>();
        for (Header header : headers) {
            result.put(header.key(), objectMapper.removeStruct(header.value()));
        }
        return result;
    }
}
