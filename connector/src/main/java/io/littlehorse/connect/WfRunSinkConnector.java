package io.littlehorse.connect;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;

public class WfRunSinkConnector extends LHSinkConnector {

    @Override
    public Class<? extends Task> taskClass() {
        return WfRunSinkTask.class;
    }

    @Override
    public ConfigDef config() {
        return WfRunSinkConnectorConfig.CONFIG_DEF;
    }
}
