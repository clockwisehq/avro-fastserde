package com.rtbhouse.utils.avro;

import static com.rtbhouse.utils.avro.FastSerdeTestsSupport.createUnionSchema;
import static com.rtbhouse.utils.avro.FastSerdeTestsSupport.deserializeSpecific;
import static com.rtbhouse.utils.avro.FastSerdeTestsSupport.emptyTestRecord;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.util.Utf8;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rtbhouse.utils.generated.avro.SubRecord;
import com.rtbhouse.utils.generated.avro.TestEnum;
import com.rtbhouse.utils.generated.avro.TestFixed;
import com.rtbhouse.utils.generated.avro.TestRecord;

public class FastSpecificSerializerGeneratorTest {

    private File tempDir;
    private ClassLoader classLoader;

    @Before
    public void prepare() throws Exception {
        Path tempPath = Files.createTempDirectory("generated");
        tempDir = tempPath.toFile();

        classLoader = URLClassLoader.newInstance(new URL[] { tempDir.toURI().toURL() },
                FastSpecificSerializerGeneratorTest.class.getClassLoader());
    }

    @Test
    public void shouldWritePrimitives() {
        TestRecord record = emptyTestRecord();
        record.put("testInt", 1);
        record.put("testIntUnion", 1);
        record.put("testString", "aaa");
        record.put("testStringUnion", "aaa");
        record.put("testLong", 1l);
        record.put("testLongUnion", 1l);
        record.put("testDouble", 1.0);
        record.put("testDoubleUnion", 1.0);
        record.put("testFloat", 1.0f);
        record.put("testFloatUnion", 1.0f);
        record.put("testBoolean", true);
        record.put("testBooleanUnion", true);
        record.put("testBytes", ByteBuffer.wrap(new byte[] { 0x01, 0x02 }));
        record.put("testBytesUnion", ByteBuffer.wrap(new byte[] { 0x01, 0x02 }));

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertEquals(1, record.get("testInt"));
        Assert.assertEquals(1, record.get("testIntUnion"));
        Assert.assertEquals("aaa", record.get("testString"));
        Assert.assertEquals("aaa", record.get("testStringUnion"));
        Assert.assertEquals(1l, record.get("testLong"));
        Assert.assertEquals(1l, record.get("testLongUnion"));
        Assert.assertEquals(1.0, record.get("testDouble"));
        Assert.assertEquals(1.0, record.get("testDoubleUnion"));
        Assert.assertEquals(1.0f, record.get("testFloat"));
        Assert.assertEquals(1.0f, record.get("testFloatUnion"));
        Assert.assertEquals(true, record.get("testBoolean"));
        Assert.assertEquals(true, record.get("testBooleanUnion"));
        Assert.assertEquals(ByteBuffer.wrap(new byte[] { 0x01, 0x02 }), record.get("testBytes"));
        Assert.assertEquals(ByteBuffer.wrap(new byte[] { 0x01, 0x02 }), record.get("testBytesUnion"));

    }

    @Test
    public void shouldWriteFixed() {
        // given
        TestRecord record = emptyTestRecord();

        record.put("testFixed", new TestFixed(new byte[] { 0x01 }));
        record.put("testFixedUnion", new TestFixed(new byte[] { 0x02 }));
        record.put("testFixedArray", Arrays.asList(new TestFixed(new byte[] { 0x03 })));
        record.put("testFixedUnionArray", Arrays.asList(new TestFixed(new byte[] { 0x04 })));

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertArrayEquals(new byte[] { 0x01 }, record.getTestFixed().bytes());
        Assert.assertArrayEquals(new byte[] { 0x02 }, record.getTestFixedUnion().bytes());
        Assert.assertArrayEquals(new byte[] { 0x03 }, record.getTestFixedArray().get(0).bytes());
        Assert.assertArrayEquals(new byte[] { 0x04 }, record.getTestFixedUnionArray().get(0).bytes());
    }

    @Test
    public void shouldWriteEnum() {
        // given
        TestRecord record = emptyTestRecord();

        record.put("testEnum", TestEnum.A);
        record.put("testEnumUnion", TestEnum.A);
        record.put("testEnumArray", Arrays.asList(TestEnum.A));
        record.put("testEnumUnionArray", Arrays.asList(TestEnum.A));

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertEquals(TestEnum.A, record.getTestEnum());
        Assert.assertEquals(TestEnum.A, record.getTestEnumUnion());
        Assert.assertEquals(TestEnum.A, record.getTestEnumArray().get(0));
        Assert.assertEquals(TestEnum.A, record.getTestEnumUnionArray().get(0));
    }

    @Test
    public void shouldWriteSubRecordField() {
        // given
        TestRecord record = emptyTestRecord();
        SubRecord subRecord = new SubRecord();
        subRecord.put("subField", "abc");

        record.put("subRecordUnion", subRecord);
        record.put("subRecord", subRecord);

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertEquals("abc",
                record.getSubRecordUnion().getSubField());
        Assert.assertEquals("abc",
                record.getSubRecord().getSubField());
    }

    @Test
    public void shouldWriteSubRecordCollectionsField() {

        // given
        TestRecord record = emptyTestRecord();
        SubRecord subRecord = new SubRecord();
        subRecord.put("subField", "abc");

        List<SubRecord> recordsArray = new ArrayList<>();
        recordsArray.add(subRecord);
        record.put("recordsArray", recordsArray);
        record.put("recordsArrayUnion", recordsArray);
        Map<String, SubRecord> recordsMap = new HashMap<>();
        recordsMap.put("1", subRecord);
        record.put("recordsMap", recordsMap);
        record.put("recordsMapUnion", recordsMap);

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertEquals("abc",
                record.getRecordsArray().get(0).getSubField());
        Assert.assertEquals("abc",
                record.getRecordsArrayUnion().get(0).getSubField());
        Assert.assertEquals("abc",
                record.getRecordsMap().get("1").getSubField());
        Assert.assertEquals("abc",
                record.getRecordsMapUnion().get("1").getSubField());
    }

    @Test
    public void shouldWriteSubRecordComplexCollectionsField() {
        // given
        TestRecord record = emptyTestRecord();
        SubRecord subRecord = new SubRecord();
        subRecord.put("subField", "abc");

        List<Map<String, SubRecord>> recordsArrayMap = new ArrayList<>();
        Map<String, SubRecord> recordMap = new HashMap<>();
        recordMap.put("1", subRecord);
        recordsArrayMap.add(recordMap);

        record.put("recordsArrayMap", recordsArrayMap);
        record.put("recordsArrayMapUnion", recordsArrayMap);

        Map<String, List<SubRecord>> recordsMapArray = new HashMap<>();
        List<SubRecord> recordList = new ArrayList<>();
        recordList.add(subRecord);
        recordsMapArray.put("1", recordList);

        record.put("recordsMapArray", recordsMapArray);
        record.put("recordsMapArrayUnion", recordsMapArray);

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertEquals("abc", record.getRecordsArrayMap().get(0).get("1").getSubField());
        Assert.assertEquals("abc",
                record.getRecordsMapArray().get("1").get(0).getSubField());
        Assert.assertEquals("abc",
                record.getRecordsArrayMapUnion().get(0).get("1").getSubField());
        Assert.assertEquals("abc",
                record.getRecordsMapArrayUnion().get("1").get(0).getSubField());

    }

    @Test
    public void shouldWriteMultipleChoiceUnion() {
        // given
        TestRecord record = emptyTestRecord();
        SubRecord subRecord = new SubRecord();
        subRecord.put("subField", "abc");
        record.put("union", subRecord);

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertEquals("abc", ((SubRecord) record.getUnion()).getSubField());

        // given
        record.put("union", "abc");

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertEquals("abc", record.getUnion());

        // given
        record.put("union", 1);

        // when
        record = deserializeSpecific(TestRecord.getClassSchema(), serializeSpecificFast(record));

        // then
        Assert.assertEquals(1, record.getUnion());
    }

    @Test
    public void shouldWriteArrayOfRecords() {
        // given
        Schema arrayRecordSchema = Schema.createArray(TestRecord.getClassSchema());

        TestRecord testRecord = emptyTestRecord();
        testRecord.put("testString", "abc");

        List<TestRecord> recordsArray = new ArrayList<>();
        recordsArray.add(testRecord);
        recordsArray.add(testRecord);

        // when
        List<TestRecord> array = deserializeSpecific(arrayRecordSchema,
                serializeSpecificFast(recordsArray, arrayRecordSchema));

        // then
        Assert.assertEquals(2, array.size());
        Assert.assertEquals("abc", array.get(0).get("testString"));
        Assert.assertEquals("abc", array.get(1).get("testString"));

        // given
        testRecord = emptyTestRecord();
        testRecord.put("testString", "abc");

        arrayRecordSchema = Schema.createArray(createUnionSchema(TestRecord
                .getClassSchema()));

        recordsArray = new ArrayList<>();
        recordsArray.add(testRecord);
        recordsArray.add(testRecord);

        // when
        array = deserializeSpecific(arrayRecordSchema, serializeSpecificFast(recordsArray, arrayRecordSchema));

        // then
        Assert.assertEquals(2, array.size());
        Assert.assertEquals("abc", array.get(0).get("testString"));
        Assert.assertEquals("abc", array.get(1).get("testString"));
    }

    @Test
    public void shouldWriteMapOfRecords() {
        // given
        Schema mapRecordSchema = Schema.createMap(TestRecord.getClassSchema());

        TestRecord testRecord = emptyTestRecord();
        testRecord.put("testString", "abc");

        Map<String, TestRecord> recordsMap = new HashMap<>();
        recordsMap.put("1", testRecord);
        recordsMap.put("2", testRecord);

        // when
        Map<Utf8, TestRecord> map = deserializeSpecific(mapRecordSchema,
                serializeSpecificFast(recordsMap, mapRecordSchema));

        // then
        Assert.assertEquals(2, map.size());
        Assert.assertEquals("abc", map.get(new Utf8("1")).get("testString"));
        Assert.assertEquals("abc", map.get(new Utf8("2")).get("testString"));

        // given
        mapRecordSchema = Schema.createMap(createUnionSchema(TestRecord
                .getClassSchema()));

        testRecord = emptyTestRecord();
        testRecord.put("testString", "abc");

        recordsMap = new HashMap<>();
        recordsMap.put("1", testRecord);
        recordsMap.put("2", testRecord);

        // when
        map = deserializeSpecific(mapRecordSchema, serializeSpecificFast(recordsMap, mapRecordSchema));

        // then
        Assert.assertEquals(2, map.size());
        Assert.assertEquals("abc", map.get(new Utf8("1")).get("testString"));
        Assert.assertEquals("abc", map.get(new Utf8("2")).get("testString"));
    }

    @Test
    public void shouldSerializeNullElementInMap() {
        // given
        Schema mapRecordSchema = Schema.createMap(Schema.createUnion(
                Schema.create(Schema.Type.STRING), Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.INT)));

        Map<String, Object> records = new HashMap<>();
        records.put("0", "0");
        records.put("1", null);
        records.put("2", 2);

        // when
        Map<Utf8, Object> map = deserializeSpecific(mapRecordSchema, serializeSpecificFast(records, mapRecordSchema));

        // then
        Assert.assertEquals(3, map.size());
        Assert.assertEquals(new Utf8("0"), map.get(new Utf8("0")));
        Assert.assertNull(map.get(new Utf8("1")));
        Assert.assertEquals(2, map.get(new Utf8("2")));
    }

    @Test
    public void shouldSerializeNullElementInArray() {
        // given
        Schema arrayRecordSchema = Schema.createArray(Schema.createUnion(
                Schema.create(Schema.Type.STRING), Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.INT)));

        List<Object> records = new ArrayList<>();
        records.add("0");
        records.add(null);
        records.add(2);

        // when
        List<Object> array = deserializeSpecific(arrayRecordSchema, serializeSpecificFast(records, arrayRecordSchema));

        // then
        Assert.assertEquals(3, array.size());
        Assert.assertEquals(new Utf8("0"), array.get(0));
        Assert.assertNull(array.get(1));
        Assert.assertEquals(2, array.get(2));
    }

    private <T extends GenericContainer> Decoder serializeSpecificFast(T data) {
        return serializeSpecificFast(data, data.getSchema());
    }

    private <T> Decoder serializeSpecificFast(T data, Schema schema) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder binaryEncoder = EncoderFactory.get().directBinaryEncoder(baos, null);

        try {
            FastSpecificSerializerGenerator<T> fastSpecificSerializerGenerator = new FastSpecificSerializerGenerator<>(
                    schema, tempDir, classLoader, null);
            FastSerializer<T> fastSerializer = fastSpecificSerializerGenerator.generateSerializer();
            fastSerializer.serialize(data, binaryEncoder);
            binaryEncoder.flush();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return DecoderFactory.get().binaryDecoder(baos.toByteArray(), null);
    }

}
