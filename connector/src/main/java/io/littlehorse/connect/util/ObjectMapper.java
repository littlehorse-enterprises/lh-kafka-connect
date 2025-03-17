package io.littlehorse.connect.util;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectMapper {
    private ObjectMapper() {}

    @SuppressWarnings("unchecked")
    public static Object removeStruct(Object value) {
        if (value instanceof Struct) {
            Struct objectStruct = (Struct) value;
            Schema schema = objectStruct.schema();
            return schema.fields().stream()
                    .collect(Collectors.toMap(
                            Field::name, field -> removeStruct(objectStruct.get(field))));
        }

        if (value instanceof List) {
            List<Object> objectList = (List<Object>) value;
            return objectList.stream().map(ObjectMapper::removeStruct).collect(Collectors.toList());
        }

        if (value instanceof Map) {
            Map<String, Object> objectMap = (Map<String, Object>) value;
            return objectMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, entry -> removeStruct(entry.getValue())));
        }

        return value;
    }
}
