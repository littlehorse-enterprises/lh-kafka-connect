package io.littlehorse.connect;

import io.littlehorse.connect.util.VersionExtractor;

import lombok.extern.slf4j.Slf4j;

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
        return VersionExtractor.version();
    }

    public Map<String, String> getProps() {
        if (props == null) return Map.of();
        return props;
    }
}
