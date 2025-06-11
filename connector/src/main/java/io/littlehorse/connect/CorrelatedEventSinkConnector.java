package io.littlehorse.connect;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;

public class CorrelatedEventSinkConnector extends LHSinkConnector {

    @Override
    public Class<? extends Task> taskClass() {
        return CorrelatedEventSinkTask.class;
    }

    @Override
    public ConfigDef config() {
        return CorrelatedEventSinkConnectorConfig.CONFIG_DEF;
    }
}
