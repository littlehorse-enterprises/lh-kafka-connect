package io.littlehorse.connect;

import static org.apache.kafka.connect.runtime.ConnectorConfig.NAME_CONFIG;

import com.google.common.base.Strings;

import io.littlehorse.connect.util.VersionReader;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class LHSinkConnector extends SinkConnector {

    private Map<String, String> props;

    @Override
    public void start(Map<String, String> props) {
        log.debug("Starting {}", getClass().getSimpleName());
        this.props = props;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        return Stream.generate(this::getProps).limit(maxTasks).collect(Collectors.toList());
    }

    @Override
    public void stop() {
        log.debug("Closing {}", getClass().getSimpleName());
    }

    @Override
    public String version() {
        return VersionReader.version();
    }

    public Map<String, String> getProps() {
        if (props == null) return Map.of();
        return props;
    }

    @Override
    public Config validate(Map<String, String> connectorConfigs) {
        String connectorName = connectorConfigs.get(NAME_CONFIG);
        if (Strings.isNullOrEmpty(connectorName)) {
            throw new ConfigException(NAME_CONFIG, connectorName, "Empty name is not allowed");
        }
        if (!connectorName.matches("[a-zA-Z0-9-]+")) {
            throw new ConfigException(
                    NAME_CONFIG,
                    connectorName,
                    "Connector name only supports alphanumeric characters and hyphens");
        }
        return super.validate(connectorConfigs);
    }
}
