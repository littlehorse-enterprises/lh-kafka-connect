package io.littlehorse.connect;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class LHSinkConnectorTest {

    @Test
    void shouldCreateTheRightNumberOfTasks() {
        Map<String, String> expectedMap = Map.of();
        int expectedSize = 3;

        LHSinkConnector connector = new LHSinkConnector() {
            @Override
            public Class<? extends Task> taskClass() {
                return null;
            }

            @Override
            public ConfigDef config() {
                return null;
            }
        };
        connector.start(expectedMap);
        List<Map<String, String>> result = connector.taskConfigs(expectedSize);

        assertThat(result).hasSize(expectedSize);
        assertThat(result).containsExactly(expectedMap, expectedMap, expectedMap);
    }
}
