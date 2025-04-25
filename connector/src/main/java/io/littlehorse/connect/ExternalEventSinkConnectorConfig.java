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
                    "The name of the ExternalEventDef.");

    private final String externalEventName;

    public ExternalEventSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
        externalEventName = getString(EXTERNAL_EVENT_NAME_KEY);
    }
}
