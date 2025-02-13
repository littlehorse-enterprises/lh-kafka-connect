package io.littlehorse.kafka.connect;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

@Getter
@Slf4j
public class LHSinkConnector extends SinkConnector {

    private Map<String, String> props;

    @Override
    public void start(Map<String, String> props) {
        log.debug("Starting LHSinkConnector");
        this.props = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return LHSinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        return Stream
            .generate(this::getProps)
            .limit(maxTasks)
            .collect(Collectors.toList());
    }

    @Override
    public void stop() {
        log.debug("Closing LHSinkConnector");
    }

    @Override
    public ConfigDef config() {
        return LHSinkConnectorConfig.CONFIG_DEF;
    }

    @Override
    public String version() {
        return LHSinkConnectorVersion.version();
    }
}
