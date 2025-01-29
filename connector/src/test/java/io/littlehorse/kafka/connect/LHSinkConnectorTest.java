package io.littlehorse.kafka.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LHSinkConnectorTest {

    @Test
    void generateTaskConfigs() {
        Map<String, String> expectedMap = Map.of();
        int expectedSize = 2;

        LHSinkConnector connector = new LHSinkConnector();
        connector.start(expectedMap);
        List<Map<String, String>> result = connector.taskConfigs(expectedSize);

        assertThat(result).hasSize(expectedSize);
        assertThat(result).containsExactly(expectedMap, expectedMap);
    }
}
