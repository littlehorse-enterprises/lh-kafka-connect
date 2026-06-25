package io.littlehorse.connect.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;

import java.util.Map;

class VariableValueMapperTest {

    private static final StructDefId PILOT_ID =
            StructDefId.newBuilder().setName("pilot").setVersion(0).build();
    private static final StructDefId VEHICLE_ID =
            StructDefId.newBuilder().setName("vehicle").setVersion(0).build();

    private final LittleHorseBlockingStub blockingStub = mock(LittleHorseBlockingStub.class);
    private final VariableValueMapper mapper = new VariableValueMapper(blockingStub);

    @Test
    void shouldConvertRegularValueWhenTypeDefIsNull() {
        VariableValue result = mapper.toVariableValue("Luke", null);

        assertThat(result).isEqualTo(LHLibUtil.objToVarVal("Luke"));
        verifyNoInteractions(blockingStub);
    }

    @Test
    void shouldConvertRegularValueWhenTypeDefIsNotStruct() {
        TypeDefinition strType = TypeDefinition.newBuilder().build();

        VariableValue result = mapper.toVariableValue("Luke", strType);

        assertThat(result).isEqualTo(LHLibUtil.objToVarVal("Luke"));
        verifyNoInteractions(blockingStub);
    }

    @Test
    void shouldBuildStructValue() {
        when(blockingStub.getStructDef(PILOT_ID))
                .thenReturn(structDef(InlineStructDef.newBuilder()
                        .putFields("name", primitiveField())
                        .build()));

        VariableValue result = mapper.toVariableValue(Map.of("name", "Luke"), structType(PILOT_ID));

        VariableValue expected = LHLibUtil.inlineStructToVarVal(
                InlineStruct.newBuilder()
                        .putFields("name", structField(LHLibUtil.objToVarVal("Luke")))
                        .build(),
                PILOT_ID);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldBuildNestedStructValue() {
        when(blockingStub.getStructDef(PILOT_ID))
                .thenReturn(structDef(InlineStructDef.newBuilder()
                        .putFields("name", primitiveField())
                        .putFields("vehicle", structFieldDef(VEHICLE_ID))
                        .build()));
        when(blockingStub.getStructDef(VEHICLE_ID))
                .thenReturn(structDef(InlineStructDef.newBuilder()
                        .putFields("model", primitiveField())
                        .build()));

        VariableValue result = mapper.toVariableValue(
                Map.of("name", "Luke", "vehicle", Map.of("model", "X-wing")), structType(PILOT_ID));

        VariableValue expectedVehicle = LHLibUtil.inlineStructToVarVal(
                InlineStruct.newBuilder()
                        .putFields("model", structField(LHLibUtil.objToVarVal("X-wing")))
                        .build(),
                VEHICLE_ID);
        VariableValue expected = LHLibUtil.inlineStructToVarVal(
                InlineStruct.newBuilder()
                        .putFields("name", structField(LHLibUtil.objToVarVal("Luke")))
                        .putFields("vehicle", structField(expectedVehicle))
                        .build(),
                PILOT_ID);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldFetchEachStructDefOnlyOnce() {
        when(blockingStub.getStructDef(PILOT_ID))
                .thenReturn(structDef(InlineStructDef.newBuilder()
                        .putFields("name", primitiveField())
                        .build()));

        mapper.toVariableValue(Map.of("name", "Luke"), structType(PILOT_ID));
        mapper.toVariableValue(Map.of("name", "Leia"), structType(PILOT_ID));

        verify(blockingStub, times(1)).getStructDef(PILOT_ID);
    }

    @Test
    void shouldReturnEmptyVariableValueWhenStructValueIsNull() {
        VariableValue result = mapper.toVariableValue(null, structType(PILOT_ID));

        assertThat(result).isEqualTo(VariableValue.newBuilder().build());
        verifyNoInteractions(blockingStub);
    }

    @Test
    void shouldThrowWhenStructValueIsNotAMap() {
        assertThatThrownBy(() -> mapper.toVariableValue("not-a-map", structType(PILOT_ID)))
                .isInstanceOf(DataException.class)
                .hasMessageContaining("pilot");
        verifyNoInteractions(blockingStub);
    }

    private static TypeDefinition structType(StructDefId structDefId) {
        return TypeDefinition.newBuilder().setStructDefId(structDefId).build();
    }

    private static StructDef structDef(InlineStructDef inlineStructDef) {
        return StructDef.newBuilder().setStructDef(inlineStructDef).build();
    }

    private static StructFieldDef primitiveField() {
        return StructFieldDef.newBuilder()
                .setFieldType(TypeDefinition.newBuilder().build())
                .build();
    }

    private static StructFieldDef structFieldDef(StructDefId structDefId) {
        return StructFieldDef.newBuilder().setFieldType(structType(structDefId)).build();
    }

    private static StructField structField(VariableValue value) {
        return StructField.newBuilder().setValue(value).build();
    }
}
