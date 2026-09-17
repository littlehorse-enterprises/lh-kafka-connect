package io.littlehorse.connect.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import io.littlehorse.sdk.common.LHLibUtil;
import io.littlehorse.sdk.common.proto.Array;
import io.littlehorse.sdk.common.proto.InlineArrayDef;
import io.littlehorse.sdk.common.proto.InlineMapDef;
import io.littlehorse.sdk.common.proto.InlineStruct;
import io.littlehorse.sdk.common.proto.InlineStructDef;
import io.littlehorse.sdk.common.proto.LittleHorseGrpc.LittleHorseBlockingStub;
import io.littlehorse.sdk.common.proto.StructDef;
import io.littlehorse.sdk.common.proto.StructDefId;
import io.littlehorse.sdk.common.proto.StructField;
import io.littlehorse.sdk.common.proto.StructFieldDef;
import io.littlehorse.sdk.common.proto.TypeDefinition;
import io.littlehorse.sdk.common.proto.VariableType;
import io.littlehorse.sdk.common.proto.VariableValue;

import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    @Test
    void shouldBuildNativeMapAndCoerceJsonObjectKeys() {
        InlineMapDef mapDef = mapDef(VariableType.INT, primitiveType(VariableType.STR));

        VariableValue result =
                mapper.toVariableValue(Map.of("1", "one", "2", "two"), mapType(mapDef));

        assertThat(result.getMap().getMapType()).isEqualTo(mapDef);
        assertThat(result.getMap().getEntriesList())
                .containsExactlyInAnyOrder(
                        mapEntry(
                                VariableValue.newBuilder().setInt(1).build(),
                                VariableValue.newBuilder().setStr("one").build()),
                        mapEntry(
                                VariableValue.newBuilder().setInt(2).build(),
                                VariableValue.newBuilder().setStr("two").build()));
    }

    @Test
    void shouldBuildTypedEmptyMap() {
        InlineMapDef mapDef = mapDef(VariableType.STR, primitiveType(VariableType.INT));

        VariableValue result = mapper.toVariableValue(Map.of(), mapType(mapDef));

        assertThat(result.getMap().getEntriesCount()).isZero();
        assertThat(result.getMap().getMapType()).isEqualTo(mapDef);
    }

    @ParameterizedTest(name = "{1} key \"{0}\"")
    @MethodSource("allowedPrimitiveMapKeys")
    void shouldConvertEveryAllowedPrimitiveMapKeyType(
            Object key, VariableType keyType, VariableValue expected) {
        assertThat(mappedKey(key, keyType)).isEqualTo(expected);
    }

    private static Stream<Arguments> allowedPrimitiveMapKeys() {
        return Stream.of(
                Arguments.of(
                        "key",
                        VariableType.STR,
                        VariableValue.newBuilder().setStr("key").build()),
                Arguments.of(
                        "42",
                        VariableType.INT,
                        VariableValue.newBuilder().setInt(42).build()),
                Arguments.of(
                        "3.5",
                        VariableType.DOUBLE,
                        VariableValue.newBuilder().setDouble(3.5).build()),
                Arguments.of(
                        "true",
                        VariableType.BOOL,
                        VariableValue.newBuilder().setBool(true).build()),
                Arguments.of(
                        "aGVsbG8=",
                        VariableType.BYTES,
                        VariableValue.newBuilder()
                                .setBytes(ByteString.copyFromUtf8("hello"))
                                .build()),
                Arguments.of(
                        "parent_child",
                        VariableType.WF_RUN_ID,
                        VariableValue.newBuilder()
                                .setWfRunId(LHLibUtil.wfRunIdFromString("parent_child"))
                                .build()),
                Arguments.of(
                        "2026-01-01T00:00:00Z",
                        VariableType.TIMESTAMP,
                        VariableValue.newBuilder()
                                .setUtcTimestamp(Timestamp.newBuilder().setSeconds(1767225600))
                                .build()));
    }

    @Test
    void shouldAllowNullMapKeysAndValues() {
        InlineMapDef mapDef = mapDef(VariableType.STR, primitiveType(VariableType.INT));
        Map<Object, Object> input = new LinkedHashMap<>();
        input.put(null, 1);
        input.put("empty", null);

        VariableValue result = mapper.toVariableValue(input, mapType(mapDef));

        assertThat(result.getMap().getEntriesList())
                .containsExactly(
                        mapEntry(
                                VariableValue.newBuilder().build(),
                                VariableValue.newBuilder().setInt(1).build()),
                        mapEntry(
                                VariableValue.newBuilder().setStr("empty").build(),
                                VariableValue.newBuilder().build()));
    }

    @Test
    void shouldBuildMapWithNestedStructValues() {
        when(blockingStub.getStructDef(PILOT_ID))
                .thenReturn(structDef(InlineStructDef.newBuilder()
                        .putFields("name", primitiveField(VariableType.STR))
                        .build()));
        InlineMapDef mapDef = mapDef(VariableType.STR, structType(PILOT_ID));

        VariableValue result =
                mapper.toVariableValue(Map.of("red-five", Map.of("name", "Luke")), mapType(mapDef));

        VariableValue pilot = LHLibUtil.inlineStructToVarVal(
                InlineStruct.newBuilder()
                        .putFields(
                                "name",
                                structField(VariableValue.newBuilder()
                                        .setStr("Luke")
                                        .build()))
                        .build(),
                PILOT_ID);
        assertThat(result.getMap().getEntriesList())
                .containsExactly(
                        mapEntry(VariableValue.newBuilder().setStr("red-five").build(), pilot));
    }

    @Test
    void shouldBuildMapWithNestedArrayAndMapValues() {
        InlineMapDef nestedMapDef = mapDef(VariableType.INT, primitiveType(VariableType.BOOL));
        TypeDefinition nestedMapType = mapType(nestedMapDef);
        TypeDefinition arrayType = TypeDefinition.newBuilder()
                .setInlineArrayDef(InlineArrayDef.newBuilder().setArrayType(nestedMapType))
                .build();
        InlineMapDef outerMapDef = mapDef(VariableType.STR, arrayType);

        VariableValue result = mapper.toVariableValue(
                Map.of("flags", List.of(Map.of("7", "true"))), mapType(outerMapDef));

        VariableValue nestedMap = VariableValue.newBuilder()
                .setMap(io.littlehorse.sdk.common.proto.Map.newBuilder()
                        .setMapType(nestedMapDef)
                        .addEntries(mapEntry(
                                VariableValue.newBuilder().setInt(7).build(),
                                VariableValue.newBuilder().setBool(true).build())))
                .build();
        VariableValue nestedArray = VariableValue.newBuilder()
                .setArray(Array.newBuilder().setElementType(nestedMapType).addItems(nestedMap))
                .build();
        assertThat(result.getMap().getEntriesList())
                .containsExactly(
                        mapEntry(VariableValue.newBuilder().setStr("flags").build(), nestedArray));
    }

    @Test
    void shouldBuildMapNestedInStruct() {
        InlineMapDef mapDef = mapDef(VariableType.STR, primitiveType(VariableType.INT));
        when(blockingStub.getStructDef(PILOT_ID))
                .thenReturn(structDef(InlineStructDef.newBuilder()
                        .putFields(
                                "scores",
                                StructFieldDef.newBuilder()
                                        .setFieldType(mapType(mapDef))
                                        .build())
                        .build()));

        VariableValue result = mapper.toVariableValue(
                Map.of("scores", Map.of("target", 10)), structType(PILOT_ID));

        assertThat(result.getStruct()
                        .getStruct()
                        .getFieldsOrThrow("scores")
                        .getValue()
                        .getMap()
                        .getMapType())
                .isEqualTo(mapDef);
    }

    @Test
    void shouldReturnNullForNullMap() {
        InlineMapDef mapDef = mapDef(VariableType.STR, primitiveType(VariableType.INT));

        VariableValue result = mapper.toVariableValue(null, mapType(mapDef));

        assertThat(result.getValueCase()).isEqualTo(VariableValue.ValueCase.VALUE_NOT_SET);
    }

    @Test
    void shouldRejectNonMapInputAndComplexKeyType() {
        InlineMapDef validMapDef = mapDef(VariableType.STR, primitiveType(VariableType.INT));
        InlineMapDef invalidMapDef = InlineMapDef.newBuilder()
                .setKeyType(structType(PILOT_ID))
                .setValueType(primitiveType(VariableType.INT))
                .build();

        assertThatThrownBy(() -> mapper.toVariableValue("not-a-map", mapType(validMapDef)))
                .isInstanceOf(DataException.class)
                .hasMessageContaining("map variable");
        assertThatThrownBy(() -> mapper.toVariableValue(Map.of(), mapType(invalidMapDef)))
                .isInstanceOf(DataException.class)
                .hasMessageContaining("primitive type");
    }

    @Test
    void shouldRejectInvalidPrimitiveConversion() {
        InlineMapDef mapDef = mapDef(VariableType.INT, primitiveType(VariableType.STR));

        assertThatThrownBy(() ->
                        mapper.toVariableValue(Map.of("not-an-int", "value"), mapType(mapDef)))
                .isInstanceOf(DataException.class)
                .hasMessageContaining("cannot be converted")
                .hasMessageContaining("INT");
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

    private static StructFieldDef primitiveField(VariableType type) {
        return StructFieldDef.newBuilder().setFieldType(primitiveType(type)).build();
    }

    private static TypeDefinition primitiveType(VariableType type) {
        return TypeDefinition.newBuilder().setPrimitiveType(type).build();
    }

    private static TypeDefinition mapType(InlineMapDef mapDef) {
        return TypeDefinition.newBuilder().setInlineMapDef(mapDef).build();
    }

    private static InlineMapDef mapDef(VariableType keyType, TypeDefinition valueType) {
        return InlineMapDef.newBuilder()
                .setKeyType(primitiveType(keyType))
                .setValueType(valueType)
                .build();
    }

    private static io.littlehorse.sdk.common.proto.Map.Entry mapEntry(
            VariableValue key, VariableValue value) {
        return io.littlehorse.sdk.common.proto.Map.Entry.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
    }

    private VariableValue mappedKey(Object key, VariableType keyType) {
        InlineMapDef mapDef = mapDef(keyType, primitiveType(VariableType.STR));
        return mapper.toVariableValue(Map.of(key, "value"), mapType(mapDef))
                .getMap()
                .getEntries(0)
                .getKey();
    }

    private static StructFieldDef structFieldDef(StructDefId structDefId) {
        return StructFieldDef.newBuilder().setFieldType(structType(structDefId)).build();
    }

    private static StructField structField(VariableValue value) {
        return StructField.newBuilder().setValue(value).build();
    }
}
