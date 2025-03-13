package io.littlehorse.connect;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;

public class ExternalEventSinkConnector extends LHSinkConnector {

    @Override
    public Class<? extends Task> taskClass() {
        return ExternalEventSinkTask.class;
    }

    @Override
    public ConfigDef config() {
        return ExternalEventSinkConnectorConfig.CONFIG_DEF;
    }
}
