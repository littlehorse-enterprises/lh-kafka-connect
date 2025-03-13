package io.littlehorse.connect;

import com.google.common.base.Strings;

import lombok.Getter;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.Map;

@Getter
public class ExternalEventSinkConnectorConfig extends LHSinkConnectorConfig {
    public static final String EXTERNAL_EVENT_NAME_KEY = "external.event.name";

    public static final ConfigDef CONFIG_DEF = new ConfigDef(LHSinkConnectorConfig.BASE_CONFIG_DEF)
            .define(
                    EXTERNAL_EVENT_NAME_KEY,
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "The name of the ExternalEventDef that this event implements.");

    private final String externalEventName;

    public ExternalEventSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
        externalEventName = extractExternalEventName();
    }

    private String extractExternalEventName() {
        String externalEventName = getString(EXTERNAL_EVENT_NAME_KEY);
        if (Strings.isNullOrEmpty(externalEventName)) {
            throw new ConfigException(EXTERNAL_EVENT_NAME_KEY, externalEventName);
        }
        return externalEventName;
    }
}
