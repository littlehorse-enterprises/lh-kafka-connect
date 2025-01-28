package io.littlehorse.kafka.connect;

import io.littlehorse.sdk.common.config.LHConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Slf4j
public class LHSinkConnector extends SinkConnector {

    public static final String MANIFEST_FILENAME = "META-INF/MANIFEST.MF";

    @Override
    public void start(Map<String, String> map) {
        LHConfig lhConfig = new LHConfig();
    }

    @Override
    public Class<? extends Task> taskClass() {
        return null;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int i) {
        return List.of();
    }

    @Override
    public void stop() {

    }

    @Override
    public ConfigDef config() {
        return null;
    }

    @Override
    public String version() {
        URL resource = getClass().getClassLoader().getResource(MANIFEST_FILENAME);
        if (resource != null) {
            try {
                Manifest manifest = new Manifest(resource.openStream());
                return manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            } catch (IOException e) {
                log.error("Error trying to reach {}", MANIFEST_FILENAME, e);
                return null;
            }
        }
        return null;
    }
}
