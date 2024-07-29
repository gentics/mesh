package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.assertj.core.api.AbstractObjectArrayAssert;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.HibAddFieldChange;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = FULL, startServer = false)
public class AddFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);

			change.setFieldName("fieldName");
			assertEquals("fieldName", change.getFieldName());

			change.setRestProperty("key1", "value1");
			change.setRestProperty("key2", "value2");

			assertEquals("value1", change.getRestProperty("key1"));

		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			change.setFieldName("name");
			change.setType("html");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("name");
		}
	}

	@Test
	public void testApplyStringFieldAtEndPosition() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaVersionModel schema = new SchemaModelImpl();
			schema.addField(FieldUtil.createStringFieldSchema("firstField"));
			schema.addField(FieldUtil.createStringFieldSchema("secondField"));
			schema.addField(FieldUtil.createStringFieldSchema("thirdField"));

			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			change.setFieldName("stringField");
			change.setType("string");
			change.setInsertAfterPosition("thirdField");
			version.setSchema(schema);
			version.setNextChange(change);

			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertArrayEquals(new String[] { "firstField", "secondField", "thirdField", "stringField" },
				updatedSchema.getFields().stream().map(field -> field.getName()).toArray());
			assertThat(updatedSchema).hasField("stringField");
			assertTrue("The created field was not of the string string field.", updatedSchema.getField("stringField") instanceof StringFieldSchema);
		}
	}

	@Test
	public void testApplyStringFieldAtPosition() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			schema.addField(FieldUtil.createStringFieldSchema("firstField"));
			schema.addField(FieldUtil.createStringFieldSchema("secondField"));
			schema.addField(FieldUtil.createStringFieldSchema("thirdField"));

			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			change.setFieldName("stringField");
			change.setType("string");
			change.setInsertAfterPosition("firstField");
			version.setSchema(schema);
			version.setNextChange(change);

			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertArrayEquals(new String[] { "firstField", "stringField", "secondField", "thirdField" },
				updatedSchema.getFields().stream().map(FieldSchema::getName).toArray());
			assertThat(updatedSchema).hasField("stringField");
			assertTrue("The created field was not of the string string field.", updatedSchema.getField("stringField") instanceof StringFieldSchema);
		}
	}

	@Test
	public void testApplyStringField() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			change.setFieldName("stringField");
			change.setType("string");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("stringField");
			assertTrue("The created field was not of the string string field.", updatedSchema.getField("stringField") instanceof StringFieldSchema);
		}
	}

	@Test
	public void testApplyNodeField() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			change.setFieldName("nodeField");
			change.setType("node");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("nodeField");
			assertTrue("The created field was not of the type node field." + updatedSchema.getField("nodeField").getClass(),
				updatedSchema.getField("nodeField") instanceof NodeFieldSchema);
		}
	}

	@Test
	public void testApplyMicronodeField() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("micronodeField");
			change.setType("micronode");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("micronodeField");
			assertTrue("The created field was not of the type micronode field.",
				updatedSchema.getField("micronodeField") instanceof MicronodeFieldSchema);
		}
	}

	@Test
	public void testApplyDateField() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("dateField");
			change.setType("date");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("dateField");
			assertTrue("The created field was not of the type date field.", updatedSchema.getField("dateField") instanceof DateFieldSchema);
		}
	}

	@Test
	public void testApplyNumberField() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("numberField");
			change.setType("number");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("numberField");
			assertTrue("The created field was not of the type number field.", updatedSchema.getField("numberField") instanceof NumberFieldSchema);
		}
	}

	@Test
	public void testApplyBinaryField() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("binaryField");
			change.setType("binary");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("binaryField");
			assertTrue("The created field was not of the type binary field.", updatedSchema.getField("binaryField") instanceof BinaryFieldSchema);
		}
	}

	@Test
	public void testApplyListField() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("listField");
			change.setType("list");
			change.setListType("html");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("listField");
			assertTrue("The created field was not of the type binary field.", updatedSchema.getField("listField") instanceof ListFieldSchema);
			ListFieldSchema list = (ListFieldSchema) updatedSchema.getField("listField");
			assertEquals("The list type was incorrect.", "html", list.getListType());
		}
	}

	@Test
	public void testApplyNoIndexTrue() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("noIndexField");
			change.setType("string");
			change.setRestProperty(SchemaChangeModel.NO_INDEX_KEY, true);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("noIndexField");
			assertThat(updatedSchema.getField("noIndexField").isNoIndex()).as("No Index flag").isTrue();
		}
	}

	@Test
	public void testApplyNoIndexFalse() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("indexedField");
			change.setType("string");
			change.setRestProperty(SchemaChangeModel.NO_INDEX_KEY, false);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("indexedField");
			assertThat(updatedSchema.getField("indexedField").isNoIndex()).as("No Index flag").isFalse();
		}
	}

	@Test
	public void testApplyNoIndexNull() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("defaultIndexedField");
			change.setType("string");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("defaultIndexedField");
			assertThat(updatedSchema.getField("defaultIndexedField").isRequired()).as("No Index flag").isFalse();
		}
	}

	@Test
	public void testApplyRequiredTrue() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("requiredField");
			change.setType("string");
			change.setRestProperty(SchemaChangeModel.REQUIRED_KEY, true);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("requiredField");
			assertThat(updatedSchema.getField("requiredField").isRequired()).as("Required flag").isTrue();
		}
	}

	@Test
	public void testApplyRequiredFalse() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("optionalField");
			change.setType("string");
			change.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("optionalField");
			assertThat(updatedSchema.getField("optionalField").isRequired()).as("Required flag").isFalse();
		}
	}

	@Test
	public void testApplyRequiredNull() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("defaultRequiredField");
			change.setType("string");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("defaultRequiredField");
			assertThat(updatedSchema.getField("defaultRequiredField").isRequired()).as("Required flag").isFalse();
		}
	}

	@Test
	public void testApplyStringFieldAllow() {
		testApplyStringFieldAllowance(true);
	}

	@Test
	public void testApplyNodeFieldAllow() {
		testApplyNodeFieldAllowance(true);
	}

	@Test
	public void testApplyNodeListFieldAllow() {
		testApplyNodeListFieldAllowance(true);
	}

	@Test
	public void testApplyMicronodeFieldAllow() {
		testApplyMicronodeFieldAllowance(true);
	}

	@Test
	public void testApplyMicronodeListFieldAllow() {
		testApplyMicronodeListFieldAllowance(true);
	}
	
	@Test
	public void testApplyStringFieldDisallow() {
		testApplyStringFieldAllowance(true);
		testApplyStringFieldAllowance(false);
	}

	@Test
	public void testApplyNodeFieldDisallow() {
		testApplyNodeFieldAllowance(true);
		testApplyNodeFieldAllowance(false);
	}

	@Test
	public void testApplyNodeListFieldDisallow() {
		testApplyNodeListFieldAllowance(true);
		testApplyNodeListFieldAllowance(false);
	}

	@Test
	public void testApplyMicronodeFieldDisallow() {
		testApplyMicronodeFieldAllowance(true);
		testApplyMicronodeFieldAllowance(false);
	}

	@Test
	public void testApplyMicronodeListFieldDisallow() {
		testApplyMicronodeListFieldAllowance(true);
		testApplyMicronodeListFieldAllowance(false);
	}
	
	private void testApplyStringFieldAllowance(boolean allow) {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("stringAllowField");
			change.setType("string");
			change.setRestProperty(SchemaChangeModel.ALLOW_KEY, allow ? new String[] {"one", "two", "three"} : null);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("stringAllowField");
			AbstractObjectArrayAssert<?, String> assertion = assertThat(updatedSchema.getField("stringAllowField", StringFieldSchema.class).getAllowedValues()).as("Allowed values");
			if (allow) {
				assertion.containsExactly("one", "two", "three");
			} else {
				assertion.isNullOrEmpty();
			}
		}
	}

	private void testApplyNodeFieldAllowance(boolean allow) {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("nodeAllowField");
			change.setType("node");
			change.setRestProperty(SchemaChangeModel.ALLOW_KEY, allow ? new String[] {"content"} : null);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("nodeAllowField");			
			AbstractObjectArrayAssert<?, String> assertion = assertThat(updatedSchema.getField("nodeAllowField", NodeFieldSchema.class).getAllowedSchemas()).as("Allowed schemas");
			if (allow) {
				assertion.containsExactly("content");
			} else {
				assertion.isNullOrEmpty();
			}
		}
	}

	private void testApplyNodeListFieldAllowance(boolean allow) {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("nodeListFieldAllow");
			change.setType("list");
			change.setListType("node");
			change.setRestProperty(SchemaChangeModel.ALLOW_KEY, allow ? new String[] {"content"} : null);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("nodeListFieldAllow");
			AbstractObjectArrayAssert<?, String> assertion = assertThat(updatedSchema.getField("nodeListFieldAllow", ListFieldSchema.class).getAllowedSchemas()).as("Allowed schemas");
			if (allow) {
				assertion.containsExactly("content");
			} else {
				assertion.isNullOrEmpty();
			}
		}
	}

	private void testApplyMicronodeFieldAllowance(boolean allow) {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("micronodeAllowField");
			change.setType("micronode");
			change.setRestProperty(SchemaChangeModel.ALLOW_KEY, allow ? new String[] {"content"} : null);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("micronodeAllowField");
			AbstractObjectArrayAssert<?, String> assertion = assertThat(updatedSchema.getField("micronodeAllowField", MicronodeFieldSchema.class).getAllowedMicroSchemas()).as("Allowed schemas");
			if (allow) {
				assertion.containsExactly("content");
			} else {
				assertion.isNullOrEmpty();
			}
		}
	}

	private void testApplyMicronodeListFieldAllowance(boolean allow) {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			SchemaModelImpl schema = new SchemaModelImpl();
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("micronodeListFieldAllow");
			change.setType("list");
			change.setListType("micronode");
			change.setRestProperty(SchemaChangeModel.ALLOW_KEY, allow ? new String[] {"content"} : null);
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("micronodeListFieldAllow");
			AbstractObjectArrayAssert<?, String> assertion = assertThat(updatedSchema.getField("micronodeListFieldAllow", ListFieldSchema.class).getAllowedSchemas()).as("Allowed schemas");
			if (allow) {
				assertion.containsExactly("content");
			} else {
				assertion.isNullOrEmpty();
			}
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			JsonObject elasticSearch = new JsonObject().put("test", "test");
			SchemaChangeModel model = SchemaChangeModel.createAddFieldChange("testField", "html", "test123", elasticSearch);

			HibAddFieldChange change = (HibAddFieldChange) schemaDao(tx).createChange(version, model);
			
			assertEquals(change.getType(), model.getProperties().get(SchemaChangeModel.TYPE_KEY));
			assertEquals(change.getFieldName(), model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
			assertEquals(change.getLabel(), model.getProperty(SchemaChangeModel.LABEL_KEY));
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			HibSchemaVersion version = createVersion(schemaDao(tx));
			HibAddFieldChange change = createChange(schemaDao(tx), version, ADDFIELD);
			
			change.setFieldName("name");
			change.setType("html");
			change.setRestProperty("someProperty", "test");

			SchemaChangeModel model = change.transformToRest();
			assertEquals(change.getUuid(), model.getUuid());
			assertEquals(change.getType(), model.getProperty(SchemaChangeModel.TYPE_KEY));
			assertEquals(change.getFieldName(), model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
			assertEquals("The generic rest property from the change should have been set for the rest model.", "test",
				change.getRestProperty("someProperty"));
		}
	}

}
