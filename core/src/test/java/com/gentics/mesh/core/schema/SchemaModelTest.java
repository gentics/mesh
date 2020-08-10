package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.json.MeshJsonException;

import io.vertx.core.json.JsonObject;

public class SchemaModelTest {

	private void expectErrorOnValidate(FieldSchemaContainer container, String bodyMessageI18nKey, String... i18nParams) {
		try {
			container.validate();
			fail("No exception was thrown but we would expect a {" + bodyMessageI18nKey + "} error.");
		} catch (GenericRestException e) {
			assertEquals("The exception did not contain the expected message.", bodyMessageI18nKey, e.getI18nKey());
			assertArrayEquals(i18nParams, e.getI18nParameters());
			// Lets check english translation
			Locale en = Locale.ENGLISH;
			String text = I18NUtil.get(en, bodyMessageI18nKey, i18nParams);
			assertNotEquals("English translation for key " + bodyMessageI18nKey + " not found", text, bodyMessageI18nKey);

			// Lets check german translation
			Locale de = Locale.GERMAN;
			text = I18NUtil.get(de, bodyMessageI18nKey, i18nParams);
			assertNotEquals("German translation for key " + bodyMessageI18nKey + " not found", text, bodyMessageI18nKey);
		}
	}

	@Test
	public void testSimpleSchema() throws IOException {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("dummySchema");
		schema.setContainer(true);
		schema.addField(new HtmlFieldSchemaImpl().setLabel("Label").setName("Name").setRequired(true));
		validateSchema(schema);
	}

	@Test
	public void testSchemaWithNoFieldType() throws IOException {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("dummySchema");
		schema.setContainer(true);
		schema.addField(new HtmlFieldSchemaImpl().setLabel("Label").setName("Name").setRequired(true));
		JsonObject json = new JsonObject(schema.toJson());
		// Remove the type
		json.getJsonArray("fields").getJsonObject(0).remove("type");

		try {
			JsonUtil.readValue(json.encodePrettily(), SchemaModelImpl.class);
			fail("An error should have been thrown");
		} catch (GenericRestException e) {
			assertThat(e).matches("error_json_structure_invalid", "8", "4", "fields", "Missing type property for field {Name}");
		}
	}

	@Test
	public void testSchemaNameValidation() {
		SchemaModel schema = new SchemaModelImpl();
		schema.setContainer(true);
		schema.addField(new HtmlFieldSchemaImpl().setLabel("Label").setName("Name").setRequired(true));

		schema.setName("dummySchema");
		schema.validate();

		schema.setName("dummy-name");
		expectErrorOnValidate(schema, "schema_error_invalid_name", schema.getName());

		schema.setName("DummyName");
		schema.validate();

		schema.setName("DömmyNäme");
		expectErrorOnValidate(schema, "schema_error_invalid_name", schema.getName());

		schema.setName("0.9");
		expectErrorOnValidate(schema, "schema_error_invalid_name", schema.getName());

		schema.setName("a0");
		schema.validate();

		schema.setName("0");
		expectErrorOnValidate(schema, "schema_error_invalid_name", schema.getName());

		schema.setName("ab cd");
		expectErrorOnValidate(schema, "schema_error_invalid_name", schema.getName());
	}

	@Test
	public void testComplexSchema() throws IOException {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("dummySchema");
		schema.setDisplayField("name");
		schema.setSegmentField("name_2");
		schema.setContainer(true);
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		schema.addField(FieldUtil.createHtmlFieldSchema("name_1").setLabel("label_1").setRequired(true));
		schema.addField(FieldUtil.createStringFieldSchema("name_2").setLabel("label_2").setRequired(true));
		schema.addField(FieldUtil.createNumberFieldSchema("name_3").setLabel("label_3").setRequired(true));
		schema.addField(FieldUtil.createDateFieldSchema("name_4").setLabel("label_4").setRequired(true));
		schema.addField(FieldUtil.createBooleanFieldSchema("name_5").setLabel("label_5").setRequired(true));

		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setLabel("label_7").setName("name_7").setRequired(true);
		listFieldSchema.setAllowedSchemas(new String[] { "folder", "videos" });
		listFieldSchema.setListType("node");
		// listFieldSchema.setMax(10);
		// listFieldSchema.setMin(3);
		schema.addField(listFieldSchema);

		// MicroschemaFieldSchema microschemaFieldSchema = new MicroschemaFieldSchemaImpl();
		// microschemaFieldSchema.setLabel("label_8").setName("name_8").setRequired(true);
		// microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "content", "folder" });
		//
		// StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
		// stringFieldSchema.setName("field1").setLabel("label1");
		// microschemaFieldSchema.getFields().add(stringFieldSchema);
		// schema.addField(microschemaFieldSchema);

		schema.validate();
		validateSchema(schema);
	}

	private void validateSchema(SchemaModel schema) throws JsonParseException, JsonMappingException, IOException {
		assertNotNull(schema);
		String json = schema.toJson();
		System.out.println(json);
		assertNotNull(json);
		SchemaModel deserializedSchema = JsonUtil.readValue(json, SchemaModelImpl.class);
		assertEquals(schema.getFields().size(), deserializedSchema.getFields().size());
		assertNotNull(deserializedSchema);
	}

	@Test
	public void testNoNameInvalid() throws MeshJsonException {
		SchemaModel schema = new SchemaModelImpl();
		expectErrorOnValidate(schema, "schema_error_no_name");
	}

	@Test
	public void testNoFields() throws MeshJsonException {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("test");
		schema.validate();
	}

	@Test
	public void testSegmentFieldNotSet() throws MeshJsonException {
		SchemaModel schema = FieldUtil.createMinimalValidSchema();
		schema.setSegmentField(null);
		schema.validate();
	}

	@Test
	public void testSegmentFieldInvalid() throws MeshJsonException {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("test");
		schema.setSegmentField("invalid");
		schema.setDisplayField("name");
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		expectErrorOnValidate(schema, "schema_error_segmentfield_invalid", "invalid");
	}

	@Test
	public void testMinimalSchemaValid() throws MeshJsonException {
		SchemaModel schema = FieldUtil.createMinimalValidSchema();
		schema.validate();
	}

	@Test
	public void testDisplayFieldNotSet() throws MeshJsonException {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("test");
		schema.setSegmentField("name");
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		schema.validate();
	}

	@Test
	public void testDuplicateLabelCheckWithNullValues() {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("test");
		schema.setSegmentField("fieldA");
		schema.setDisplayField("fieldB");
		StringFieldSchema fieldA = FieldUtil.createStringFieldSchema("fieldA");
		StringFieldSchema fieldB = FieldUtil.createStringFieldSchema("fieldB");
		// Both labels are not set. Thus no conflict should occur.
		fieldA.setLabel(null);
		fieldB.setLabel(null);
		schema.addField(fieldA);
		schema.addField(fieldB);
		schema.validate();
	}

	@Test
	public void testInvalidListType() {
		SchemaModel schema = FieldUtil.createMinimalValidSchema();
		ListFieldSchema listField = FieldUtil.createListFieldSchema("listField");
		listField.setListType("blabla");
		schema.addField(listField);
		expectErrorOnValidate(schema, "schema_error_list_type_invalid", "blabla", "listField");
	}

	@Test
	public void testMissingListType() {
		SchemaModel schema = FieldUtil.createMinimalValidSchema();
		ListFieldSchema listField = FieldUtil.createListFieldSchema("listField");
		listField.setListType(null);
		schema.addField(listField);
		expectErrorOnValidate(schema, "schema_error_list_type_missing", "listField");
	}

	@Test
	public void testDisplayFieldInvalid() {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("test");
		schema.setSegmentField("name");
		schema.setDisplayField("invalid");
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		expectErrorOnValidate(schema, "schema_error_displayfield_invalid", "invalid");
	}

	@Test
	public void testBinaryDisplayField() {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("test");
		schema.setDisplayField("binary");
		schema.addField(FieldUtil.createBinaryFieldSchema("binary"));
		schema.validate();
	}

	@Test
	public void testDuplicateFieldSchemaName() {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("test");
		schema.setSegmentField("name");
		schema.setDisplayField("name");
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		schema.addField(FieldUtil.createStringFieldSchema("name"));
		expectErrorOnValidate(schema, "schema_error_duplicate_field_name", "name");
	}

	/**
	 * The display field must always point to a string field.
	 */
	@Test
	public void testDisplayFieldInvalidType() {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName("test");
		schema.setSegmentField("name");
		schema.setDisplayField("name");
		schema.addField(FieldUtil.createNumberFieldSchema("name"));
		expectErrorOnValidate(schema, "schema_error_displayfield_type_invalid", "name");
	}

	@Test
	public void testSegmentFieldBinaryField() {
		SchemaModel schema = FieldUtil.createMinimalValidSchema();
		schema.addField(FieldUtil.createBinaryFieldSchema("binaryField"));
		schema.setSegmentField("binaryField");
		schema.validate();
	}

	/**
	 * The segment field must always point to a string or binary field.
	 */
	@Test
	public void testSegmentFieldToNoStringOrBinaryFieldInvalid() {
		SchemaModel schema = FieldUtil.createMinimalValidSchema();
		schema.addField(FieldUtil.createNumberFieldSchema("numberField"));
		schema.setSegmentField("numberField");
		expectErrorOnValidate(schema, "schema_error_segmentfield_type_invalid", "number");
	}

	@Test
	public void testMicroschemaUnsupportedFieldTypeBinary() {
		MicroschemaModel schema = new MicroschemaModelImpl();
		schema.setName("test");
		schema.setDescription("some blub");
		schema.addField(FieldUtil.createBinaryFieldSchema("binary"));
		expectErrorOnValidate(schema, "microschema_error_field_type_not_allowed", "binary", "binary");
	}

	@Test
	public void testMicroschemaUnsupportedFieldTypeMicronode() {
		MicroschemaModel schema = new MicroschemaModelImpl();
		schema.setName("test");
		schema.setDescription("some blub");
		schema.addField(FieldUtil.createMicronodeFieldSchema("micronode"));
		expectErrorOnValidate(schema, "microschema_error_field_type_not_allowed", "micronode", "micronode");
	}

	@Test
	public void testMicroschemaUnsupportedFieldTypeMicronodeList() {
		MicroschemaModel schema = new MicroschemaModelImpl();
		schema.setName("test");
		schema.setDescription("some blub");
		schema.addField(FieldUtil.createListFieldSchema("list").setListType("micronode"));
		expectErrorOnValidate(schema, "microschema_error_field_type_not_allowed", "list", "list:micronode");
	}

	@Test
	public void testMicroschemaUnsupportedFieldTypeBinaryList() {
		MicroschemaModel schema = new MicroschemaModelImpl();
		schema.setName("test");
		schema.setDescription("some blub");
		schema.addField(FieldUtil.createListFieldSchema("list").setListType("binary"));
		expectErrorOnValidate(schema, "microschema_error_field_type_not_allowed", "list", "list:binary");
	}

}
