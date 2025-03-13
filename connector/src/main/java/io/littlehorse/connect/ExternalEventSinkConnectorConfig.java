package io.littlehorse.connect;

import com.google.common.base.Strings;

import lombok.Getter;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.Map;

@Getter
public class ExternalEventSinkConnectorConfig extends LHSinkConnectorConfig {
    public static final String EXTERNAL_EVENT_NAME_KEY = "external.event.name";
    public static final String THREAD_RUN_NUMBER = "thread.run.number";
    public static final String NODE_RUN_POSITION = "node.run.position";

    public static final ConfigDef CONFIG_DEF = new ConfigDef(LHSinkConnectorConfig.BASE_CONFIG_DEF)
            .define(
                    EXTERNAL_EVENT_NAME_KEY,
                    ConfigDef.Type.STRING,
                    ConfigDef.Importance.HIGH,
                    "The name of the ExternalEventDef that this event implements.")
            .define(
                    THREAD_RUN_NUMBER,
                    ConfigDef.Type.INT,
                    null,
                    ConfigDef.Importance.LOW,
                    "Optionally specify that this ExternalEvent may only be claimed by a specific ThreadRun.")
            .define(
                    NODE_RUN_POSITION,
                    ConfigDef.Type.INT,
                    null,
                    ConfigDef.Importance.LOW,
                    "Optionally specify that this ExternalEvent may only be claimed by a specific NodeRun. In order for this to be set, you must also set \""
                            + THREAD_RUN_NUMBER + "\"");

    private final String externalEventName;
    private final Integer threadRunNumber;
    private final Integer nodeRunPosition;

    public ExternalEventSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
        externalEventName = extractExternalEventName();
        threadRunNumber = getInt(THREAD_RUN_NUMBER);
        nodeRunPosition = getInt(NODE_RUN_POSITION);
    }

    private String extractExternalEventName() {
        String externalEventName = getString(EXTERNAL_EVENT_NAME_KEY);
        if (Strings.isNullOrEmpty(externalEventName)) {
            throw new ConfigException(EXTERNAL_EVENT_NAME_KEY, externalEventName);
        }
        return externalEventName;
    }
}
