package io.littlehorse.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.runtime.ConnectorConfig;
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
                return LHSinkConnectorConfig.BASE_CONFIG_DEF;
            }
        };
        connector.start(expectedMap);
        List<Map<String, String>> result = connector.taskConfigs(expectedSize);

        assertThat(result).hasSize(expectedSize);
        assertThat(result).containsExactly(expectedMap, expectedMap, expectedMap);
    }

    @Test
    void shouldValidateUnderscore() {
        Map<String, String> inputMap =
                Map.of(ConnectorConfig.NAME_CONFIG, "my_invalid_connector_name");

        LHSinkConnector connector = new LHSinkConnector() {
            @Override
            public Class<? extends Task> taskClass() {
                return null;
            }

            @Override
            public ConfigDef config() {
                return LHSinkConnectorConfig.BASE_CONFIG_DEF;
            }
        };

        ConfigException configException =
                assertThrows(ConfigException.class, () -> connector.validate(inputMap));

        assertThat(configException)
                .hasMessage(
                        "Invalid value my_invalid_connector_name for configuration name: Connector name only supports lowercase alphanumeric characters and hyphens");
    }

    @Test
    void shouldValidateUpperCaseName() {
        Map<String, String> inputMap = Map.of(ConnectorConfig.NAME_CONFIG, "MyInvalidName");

        LHSinkConnector connector = new LHSinkConnector() {
            @Override
            public Class<? extends Task> taskClass() {
                return null;
            }

            @Override
            public ConfigDef config() {
                return LHSinkConnectorConfig.BASE_CONFIG_DEF;
            }
        };

        ConfigException configException =
                assertThrows(ConfigException.class, () -> connector.validate(inputMap));

        assertThat(configException)
                .hasMessage(
                        "Invalid value MyInvalidName for configuration name: Connector name only supports lowercase alphanumeric characters and hyphens");
    }

    @Test
    void shouldValidateName() {
        Map<String, String> inputMap = Map.of(ConnectorConfig.NAME_CONFIG, "my-valid-name1");

        LHSinkConnector connector = new LHSinkConnector() {
            @Override
            public Class<? extends Task> taskClass() {
                return null;
            }

            @Override
            public ConfigDef config() {
                return LHSinkConnectorConfig.BASE_CONFIG_DEF;
            }
        };
        connector.validate(inputMap);
    }
}
