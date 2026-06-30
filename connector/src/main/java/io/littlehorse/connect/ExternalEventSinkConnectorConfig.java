package io.littlehorse.connect;

import lombok.Getter;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;

@Getter
public class ExternalEventSinkConnectorConfig extends LHSinkConnectorConfig {
    public static final String EXTERNAL_EVENT_NAME_KEY = "external.event.name";

    public static final ConfigDef CONFIG_DEF = new ConfigDef(LHSinkConnectorConfig.BASE_CONFIG_DEF)
            .define(
                    EXTERNAL_EVENT_NAME_KEY,
                    Type.STRING,
                    Importance.HIGH,
                    "The name of the ExternalEventDef.")
            .define(
                    AUTO_IDEMPOTENCY_KEY_ENABLED_KEY,
                    Type.BOOLEAN,
                    true,
                    Importance.MEDIUM,
                    "When true (default) the connector automatically generates a deterministic"
                            + " idempotency key (derived from the connector name, topic, partition"
                            + " and offset) and uses it as the ExternalEvent guid when the record"
                            + " provides no explicit guid (via the 'guid' header). When false no"
                            + " idempotency key is generated, so a record without an explicit guid"
                            + " fails with a non-retriable error.");

    private final String externalEventName;
    private final boolean autoIdempotencyKeyEnabled;

    public ExternalEventSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
        externalEventName = getString(EXTERNAL_EVENT_NAME_KEY);
        autoIdempotencyKeyEnabled = getBoolean(AUTO_IDEMPOTENCY_KEY_ENABLED_KEY);
    }
}
