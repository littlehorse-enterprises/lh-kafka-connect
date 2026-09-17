package io.littlehorse.connect.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.Array;
import io.littlehorse.sdk.common.proto.InlineArrayDef;
import io.littlehorse.sdk.common.proto.InlineMapDef;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.Struct;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;

import org.apache.kafka.connect.errors.DataException;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds {@link VariableValue}s from deserialized Kafka record values according to their
 * LittleHorse type definitions. Referenced {@link StructDef}s are fetched lazily and cached, so a
 * given definition is only loaded once per task.
 */
public class VariableValueMapper {
    private final LittleHorseBlockingStub blockingStub;
    private final Map<StructDefId, StructDef> structDefCache = new HashMap<>();

    public VariableValueMapper(LittleHorseBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    /**
     * Converts a value into a {@link VariableValue} according to its expected type. Structured and
     * collection values are mapped recursively; a missing type keeps the value-inferred behavior.
     */
    public VariableValue toVariableValue(Object value, TypeDefinition typeDef) {
        if (value == null) {
            return VariableValue.newBuilder().build();
        }
        if (typeDef == null) {
            return LHLibUtil.objToVarVal(value);
        }
        if (typeDef.hasStructDefId()) {
            return buildStructValue(value, typeDef.getStructDefId());
        }
        if (typeDef.hasInlineStructDef()) {
            return buildInlineStructValue(value, typeDef.getInlineStructDef());
        }
        if (typeDef.hasInlineArrayDef()) {
            return buildArrayValue(value, typeDef.getInlineArrayDef());
        }
        if (typeDef.hasInlineMapDef()) {
            return buildMapValue(value, typeDef.getInlineMapDef());
        }
        if (typeDef.hasPrimitiveType()) {
            return buildPrimitiveValue(value, typeDef.getPrimitiveType());
        }
        return LHLibUtil.objToVarVal(value);
    }

    private VariableValue buildStructValue(Object value, StructDefId structDefId) {
        if (!(value instanceof Map<?, ?>)) {
            throw new DataException("Expected schema structure not provided, struct variable '"
                    + structDefId.getName() + "' should be a key-value pair data set");
        }
        InlineStructDef structDef = getStructDef(structDefId).getStructDef();
        InlineStruct inlineStruct = buildInlineStruct(value, structDef, structDefId.getName());
        return LHLibUtil.inlineStructToVarVal(inlineStruct, structDefId);
    }

    private VariableValue buildInlineStructValue(Object value, InlineStructDef structDef) {
        InlineStruct inlineStruct = buildInlineStruct(value, structDef, "inline struct");
        return VariableValue.newBuilder()
                .setStruct(Struct.newBuilder().setStruct(inlineStruct))
                .build();
    }

    private InlineStruct buildInlineStruct(
            Object value, InlineStructDef structDef, String structName) {
        if (!(value instanceof Map<?, ?> fields)) {
            throw new DataException("Expected schema structure not provided, struct variable '"
                    + structName + "' should be a key-value pair data set");
        }

        InlineStruct.Builder inlineStruct = InlineStruct.newBuilder();

        for (Map.Entry<String, StructFieldDef> field : structDef.getFieldsMap().entrySet()) {
            String fieldName = field.getKey();
            TypeDefinition fieldType = field.getValue().getFieldType();
            Object fieldValue = fields.get(fieldName);
            inlineStruct.putFields(
                    fieldName,
                    StructField.newBuilder()
                            .setValue(toVariableValue(fieldValue, fieldType))
                            .build());
        }

        return inlineStruct.build();
    }

    private VariableValue buildArrayValue(Object value, InlineArrayDef arrayDef) {
        TypeDefinition elementType = arrayDef.getArrayType();
        Array.Builder array = Array.newBuilder().setElementType(elementType);

        if (value instanceof Collection<?> values) {
            values.forEach(item -> array.addItems(toVariableValue(item, elementType)));
        } else if (value.getClass().isArray()) {
            for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++) {
                array.addItems(toVariableValue(java.lang.reflect.Array.get(value, i), elementType));
            }
        } else {
            throw new DataException(
                    "Expected schema structure not provided, array variable should be a collection");
        }

        return VariableValue.newBuilder().setArray(array).build();
    }

    private VariableValue buildMapValue(Object value, InlineMapDef mapDef) {
        if (!(value instanceof Map<?, ?> values)) {
            throw new DataException(
                    "Expected schema structure not provided, map variable should be a key-value pair data set");
        }
        if (!mapDef.getKeyType().hasPrimitiveType()
                || mapDef.getKeyType().getPrimitiveType() == VariableType.JSON_OBJ
                || mapDef.getKeyType().getPrimitiveType() == VariableType.JSON_ARR) {
            throw new DataException("Native map keys must have a non-JSON primitive type");
        }

        io.littlehorse.sdk.common.proto.Map.Builder map =
                io.littlehorse.sdk.common.proto.Map.newBuilder().setMapType(mapDef);
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            map.addEntries(io.littlehorse.sdk.common.proto.Map.Entry.newBuilder()
                    .setKey(toVariableValue(entry.getKey(), mapDef.getKeyType()))
                    .setValue(toVariableValue(entry.getValue(), mapDef.getValueType())));
        }
        return VariableValue.newBuilder().setMap(map).build();
    }

    private VariableValue buildPrimitiveValue(Object value, VariableType type) {
        try {
            return switch (type) {
                case STR ->
                    VariableValue.newBuilder().setStr(String.valueOf(value)).build();
                case INT -> VariableValue.newBuilder().setInt(toLong(value)).build();
                case DOUBLE ->
                    VariableValue.newBuilder().setDouble(toDouble(value)).build();
                case BOOL ->
                    VariableValue.newBuilder().setBool(toBoolean(value)).build();
                case BYTES ->
                    VariableValue.newBuilder().setBytes(toByteString(value)).build();
                case WF_RUN_ID ->
                    VariableValue.newBuilder()
                            .setWfRunId(
                                    value instanceof io.littlehorse.sdk.common.proto.WfRunId wfRunId
                                            ? wfRunId
                                            : LHLibUtil.wfRunIdFromString(String.valueOf(value)))
                            .build();
                case TIMESTAMP ->
                    VariableValue.newBuilder()
                            .setUtcTimestamp(toTimestamp(value))
                            .build();
                case JSON_OBJ, JSON_ARR -> LHLibUtil.objToVarVal(value);
                case UNRECOGNIZED ->
                    throw new DataException("Unsupported LittleHorse variable type " + type);
            };
        } catch (DataException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DataException(
                    "Value '" + value + "' cannot be converted to LittleHorse type " + type,
                    exception);
        }
    }

    private long toLong(Object value) {
        if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
            return ((Number) value).longValue();
        }
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            long longValue = number.longValue();
            if (!Double.isFinite(doubleValue) || doubleValue != longValue) {
                throw new DataException("Value '" + value + "' is not an integer");
            }
            return longValue;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private double toDouble(Object value) {
        return value instanceof Number number
                ? number.doubleValue()
                : Double.parseDouble(String.valueOf(value));
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String stringValue = String.valueOf(value);
        if ("true".equalsIgnoreCase(stringValue)) {
            return true;
        }
        if ("false".equalsIgnoreCase(stringValue)) {
            return false;
        }
        throw new DataException("Value '" + value + "' is not a boolean");
    }

    private ByteString toByteString(Object value) {
        if (value instanceof byte[] bytes) {
            return ByteString.copyFrom(bytes);
        }
        if (value instanceof ByteBuffer buffer) {
            return ByteString.copyFrom(buffer.duplicate());
        }
        return ByteString.copyFrom(Base64.getDecoder().decode(String.valueOf(value)));
    }

    private Timestamp toTimestamp(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }

        Instant instant;
        if (value instanceof Date date) {
            instant = date.toInstant();
        } else if (value instanceof Instant instantValue) {
            instant = instantValue;
        } else if (value instanceof Number number) {
            instant = Instant.ofEpochMilli(number.longValue());
        } else {
            try {
                instant = Instant.parse(String.valueOf(value));
            } catch (DateTimeParseException exception) {
                throw new DataException(
                        "Value '" + value + "' is not an ISO-8601 timestamp", exception);
            }
        }
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private StructDef getStructDef(StructDefId structDefId) {
        return structDefCache.computeIfAbsent(structDefId, blockingStub::getStructDef);
    }
}
