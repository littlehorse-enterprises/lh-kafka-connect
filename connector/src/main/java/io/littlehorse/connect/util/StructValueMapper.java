package io.littlehorse.connect.util;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableValue;

import org.apache.kafka.connect.errors.DataException;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds {@link VariableValue}s from deserialized Kafka record values, resolving LittleHorse
 * {@code STRUCT} types against their {@link StructDef}s. Each {@link StructDef} is fetched lazily
 * and cached, so a given definition is only loaded once per task.
 */
public class StructValueMapper {
    private final LittleHorseBlockingStub blockingStub;
    private final Map<StructDefId, StructDef> structDefCache = new HashMap<>();

    public StructValueMapper(LittleHorseBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    /**
     * Converts a value into a {@link VariableValue} according to its expected type. When the type
     * is backed by a {@link StructDefId}, the value is expected to be a key-value pair data set and
     * is mapped recursively; otherwise it is converted as a regular value.
     */
    public VariableValue toVariableValue(Object value, TypeDefinition typeDef) {
        if (typeDef != null && typeDef.hasStructDefId()) {
            return buildStructValue(value, typeDef.getStructDefId());
        }
        return LHLibUtil.objToVarVal(value);
    }

    @SuppressWarnings("unchecked")
    private VariableValue buildStructValue(Object value, StructDefId structDefId) {
        if (value == null) {
            return VariableValue.newBuilder().build();
        }

        if (!(value instanceof Map)) {
            throw new DataException("Expected schema structure not provided, struct variable '"
                    + structDefId.getName() + "' should be a key-value pair data set");
        }

        Map<String, Object> fields = (Map<String, Object>) value;
        InlineStructDef structDef = getStructDef(structDefId).getStructDef();
        InlineStruct.Builder inlineStruct = InlineStruct.newBuilder();

        for (Map.Entry<String, StructFieldDef> field : structDef.getFieldsMap().entrySet()) {
            String fieldName = field.getKey();
            TypeDefinition fieldType = field.getValue().getFieldType();
            Object fieldValue = fields.get(fieldName);

            VariableValue fieldVarVal = fieldType.hasStructDefId()
                    ? buildStructValue(fieldValue, fieldType.getStructDefId())
                    : LHLibUtil.objToVarVal(fieldValue);

            inlineStruct.putFields(
                    fieldName, StructField.newBuilder().setValue(fieldVarVal).build());
        }

        return LHLibUtil.inlineStructToVarVal(inlineStruct.build(), structDefId);
    }

    private StructDef getStructDef(StructDefId structDefId) {
        return structDefCache.computeIfAbsent(structDefId, blockingStub::getStructDef);
    }
}
