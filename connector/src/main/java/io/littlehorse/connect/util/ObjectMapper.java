package io.littlehorse.connect.util;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectMapper {
    private ObjectMapper() {}

    @SuppressWarnings("unchecked")
    public static Object removeStruct(Object value) {
        if (value instanceof Struct objectStruct) {
            Map<String, Object> result = new HashMap<>();

            for (Field field : objectStruct.schema().fields()) {
                result.put(field.name(), removeStruct(objectStruct.get(field)));
            }

            return result;
        }

        if (value instanceof List) {
            List<Object> objectList = (List<Object>) value;
            return objectList.stream().map(ObjectMapper::removeStruct).collect(Collectors.toList());
        }

        if (value instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                result.put(entry.getKey(), removeStruct(entry.getValue()));
            }
            return result;
        }

        return value;
    }
}
