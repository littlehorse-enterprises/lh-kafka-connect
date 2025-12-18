package io.littlehorse.connect.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ObjectMapperTest {

    @Test
    void shouldAllowNullInStruct() {
        String expectedFieldName = "my-null-input";
        Struct input = mock();
        Schema schema = mock();
        Field field = mock();
        doReturn(schema).when(input).schema();
        doReturn(List.of(field)).when(schema).fields();
        doReturn(expectedFieldName).when(field).name();
        doReturn(null).when(input).get(field);

        Map<String, Object> expected = new HashMap<>();
        expected.put(expectedFieldName, null);

        assertThat(ObjectMapper.removeStruct(input)).isEqualTo(expected);
    }

    @Test
    void shouldAllowNullInLists() {
        List<Object> input = new ArrayList<>();
        input.add(null);
        assertThat(ObjectMapper.removeStruct(input)).isEqualTo(input);
    }

    @Test
    void shouldAllowNullInMaps() {
        Map<String, Object> input = new HashMap<>();
        input.put("my-null-input", null);
        assertThat(ObjectMapper.removeStruct(input)).isEqualTo(input);
    }
}
