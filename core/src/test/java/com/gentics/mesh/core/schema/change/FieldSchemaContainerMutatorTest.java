package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ALLOW_KEY;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.schema.FieldTypeChange;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;

/**
 * Test for common mutator operations on a field containers.
 */
@MeshTestSetting(testSize = FULL, startServer = false)
public class FieldSchemaContainerMutatorTest extends AbstractMeshTest {

	private FieldSchemaContainerMutator mutator = new FieldSchemaContainerMutator();

	@Test
	public void testNullOperation() {
		try (Tx tx = tx()) {
			SchemaContainerVersion version = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			version.setSchema(schema);
			Schema updatedSchema = mutator.apply(version);
			assertNotNull(updatedSchema);
			assertEquals("No changes were specified. No modification should happen.", schema, updatedSchema);
		}
	}

	@Test
	public void testUpdateTypeAndAllowProperty() {
		try (Tx tx = tx()) {
			SchemaContainerVersion version = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);

			// 1. Create schema
			SchemaModelImpl schema = new SchemaModelImpl("testschema");

			NumberFieldSchema numberField = new NumberFieldSchemaImpl();
			numberField.setName("testField");
			numberField.setRequired(true);
			numberField.setLabel("originalLabel");
			schema.addField(numberField);

			version.setSchema(schema);

			FieldTypeChange fieldTypeChange = tx.getGraph().addFramedVertex(FieldTypeChangeImpl.class);
			fieldTypeChange.setFieldName("testField");
			fieldTypeChange.setRestProperty(SchemaChangeModel.TYPE_KEY, "string");
			fieldTypeChange.setRestProperty(SchemaChangeModel.ALLOW_KEY, new String[] { "testValue" });
			version.setNextChange(fieldTypeChange);

			// 3. Apply the changes
			Schema updatedSchema = mutator.apply(version);

			StringFieldSchema stringFieldSchema = updatedSchema.getField("testField", StringFieldSchemaImpl.class);
			assertNotNull(stringFieldSchema);
			assertThat(stringFieldSchema.getAllowedValues()).containsExactly("testValue");
		}
	}

	@Test
	public void testUpdateLabel() {
		try (Tx tx = tx()) {
			SchemaContainerVersion version = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);

			// 1. Create schema
			SchemaModelImpl schema = new SchemaModelImpl("testschema");

			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setAllowedValues("blub");
			stringField.setName("stringField");
			stringField.setRequired(true);
			stringField.setLabel("originalLabel");
			schema.addField(stringField);

			version.setSchema(schema);

			UpdateFieldChange stringFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			stringFieldUpdate.setFieldName("stringField");
			stringFieldUpdate.setRestProperty(SchemaChangeModel.LABEL_KEY, "UpdatedLabel");
			version.setNextChange(stringFieldUpdate);

			// 3. Apply the changes
			Schema updatedSchema = mutator.apply(version);

			StringFieldSchema stringFieldSchema = updatedSchema.getField("stringField", StringFieldSchemaImpl.class);
			assertNotNull(stringFieldSchema);
			assertEquals("UpdatedLabel", stringFieldSchema.getLabel());
		}
	}

	@Test
	public void testAUpdateFields() {
		try (Tx tx = tx()) {
			SchemaContainerVersion version = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);

			// 1. Create schema
			SchemaModelImpl schema = new SchemaModelImpl("testschema");
			
			BinaryFieldSchema binaryField = new BinaryFieldSchemaImpl();
			binaryField.setName("binaryField");
			binaryField.setAllowedMimeTypes("oldTypes");
			binaryField.setRequired(true);
			schema.addField(binaryField);

			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setAllowedValues("blub");
			stringField.setName("stringField");
			stringField.setRequired(true);
			schema.addField(stringField);

			NodeFieldSchema nodeField = new NodeFieldSchemaImpl();
			nodeField.setAllowedSchemas("blub");
			nodeField.setName("nodeField");
			nodeField.setRequired(true);
			schema.addField(nodeField);

			MicronodeFieldSchema micronodeField = new MicronodeFieldSchemaImpl();
			micronodeField.setAllowedMicroSchemas("blub");
			micronodeField.setName("micronodeField");
			micronodeField.setRequired(true);
			schema.addField(micronodeField);

			NumberFieldSchema numberField = new NumberFieldSchemaImpl();
			numberField.setName("numberField");
			numberField.setRequired(true);
			schema.addField(numberField);

			HtmlFieldSchema htmlField = new HtmlFieldSchemaImpl();
			htmlField.setName("htmlField");
			htmlField.setRequired(true);
			schema.addField(htmlField);

			BooleanFieldSchema booleanField = new BooleanFieldSchemaImpl();
			booleanField.setName("booleanField");
			booleanField.setRequired(true);
			schema.addField(booleanField);

			DateFieldSchema dateField = new DateFieldSchemaImpl();
			dateField.setName("dateField");
			dateField.setRequired(true);
			schema.addField(dateField);

			ListFieldSchema listField = new ListFieldSchemaImpl();
			listField.setName("listField");
			listField.setListType("micronode");
			listField.setRequired(true);
			schema.addField(listField);

			version.setSchema(schema);

			// 2. Create schema field update change
			UpdateFieldChange binaryFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			binaryFieldUpdate.setFieldName("binaryField");
			binaryFieldUpdate.setRestProperty("allowedMimeTypes", new String[] { "newTypes" });
			binaryFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			version.setNextChange(binaryFieldUpdate);

			UpdateFieldChange nodeFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			nodeFieldUpdate.setFieldName("nodeField");
			nodeFieldUpdate.setRestProperty(ALLOW_KEY, new String[] { "schemaA", "schemaB" });
			nodeFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			binaryFieldUpdate.setNextChange(nodeFieldUpdate);

			UpdateFieldChange stringFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			stringFieldUpdate.setRestProperty(ALLOW_KEY, new String[] { "valueA", "valueB" });
			stringFieldUpdate.setFieldName("stringField");
			stringFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			stringFieldUpdate.setIndexOptions(IndexOptionHelper.getRawFieldOption());
			nodeFieldUpdate.setNextChange(stringFieldUpdate);

			UpdateFieldChange htmlFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			htmlFieldUpdate.setFieldName("htmlField");
			htmlFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			htmlFieldUpdate.setRestProperty(SchemaChangeModel.ELASTICSEARCH_KEY, IndexOptionHelper.getRawFieldOption().encode());
			stringFieldUpdate.setNextChange(htmlFieldUpdate);

			UpdateFieldChange numberFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			numberFieldUpdate.setFieldName("numberField");
			numberFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			htmlFieldUpdate.setNextChange(numberFieldUpdate);

			UpdateFieldChange dateFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			dateFieldUpdate.setFieldName("dateField");
			dateFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			numberFieldUpdate.setNextChange(dateFieldUpdate);

			UpdateFieldChange booleanFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			booleanFieldUpdate.setFieldName("booleanField");
			booleanFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			dateFieldUpdate.setNextChange(booleanFieldUpdate);

			UpdateFieldChange micronodeFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			micronodeFieldUpdate.setFieldName("micronodeField");
			micronodeFieldUpdate.setRestProperty(SchemaChangeModel.ALLOW_KEY, new String[] { "A", "B", "C" });
			micronodeFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			booleanFieldUpdate.setNextChange(micronodeFieldUpdate);

			UpdateFieldChange listFieldUpdate = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			listFieldUpdate.setFieldName("listField");
			listFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			listFieldUpdate.setRestProperty(SchemaChangeModel.ALLOW_KEY, new String[] { "A1", "B1", "C1" });
			micronodeFieldUpdate.setNextChange(listFieldUpdate);

			// 3. Apply the changes
			Schema updatedSchema = mutator.apply(version);

			// Binary
			BinaryFieldSchema binaryFieldSchema = updatedSchema.getField("binaryField", BinaryFieldSchemaImpl.class);
			assertNotNull(binaryFieldSchema);
			assertArrayEquals(new String[] { "newTypes" }, binaryFieldSchema.getAllowedMimeTypes());
			assertFalse("The required flag should now be set to false.", binaryFieldSchema.isRequired());
			assertNull("The elasticsearch settings were not set correctly.", binaryFieldSchema.getElasticsearch());

			// Node
			NodeFieldSchema nodeFieldSchema = updatedSchema.getField("nodeField", NodeFieldSchemaImpl.class);
			assertNotNull(nodeFieldSchema);
			assertArrayEquals(new String[] { "schemaA", "schemaB" }, nodeFieldSchema.getAllowedSchemas());
			assertFalse("The required flag should now be set to false.", nodeFieldSchema.isRequired());
			assertNull("The elasticsearch settings were not set correctly.", nodeFieldSchema.getElasticsearch());

			// Microschema
			MicronodeFieldSchema micronodeFieldSchema = updatedSchema.getField("micronodeField", MicronodeFieldSchemaImpl.class);
			assertNotNull(micronodeFieldSchema);
			assertArrayEquals(new String[] { "A", "B", "C" }, micronodeFieldSchema.getAllowedMicroSchemas());
			assertFalse("The required flag should now be set to false.", micronodeFieldSchema.isRequired());
			assertNull("The elasticsearch settings were not set correctly.", micronodeFieldSchema.getElasticsearch());

			// String
			StringFieldSchema stringFieldSchema = updatedSchema.getField("stringField", StringFieldSchemaImpl.class);
			assertNotNull(stringFieldSchema);
			assertArrayEquals(new String[] { "valueA", "valueB" }, stringFieldSchema.getAllowedValues());
			assertFalse("The required flag should now be set to false.", stringFieldSchema.isRequired());
			assertTrue("The index option did not contain the raw field. {" + stringFieldSchema.getElasticsearch().encodePrettily() + "}",
					stringFieldSchema.getElasticsearch().containsKey("raw"));

			// Html
			HtmlFieldSchema htmlFieldSchema = updatedSchema.getField("htmlField", HtmlFieldSchemaImpl.class);
			assertNotNull(htmlFieldSchema);
			assertFalse("The required flag should now be set to false.", htmlFieldSchema.isRequired());
			assertTrue("The elasticsearch settings did not contain the raw field. {" + htmlFieldSchema.getElasticsearch().encodePrettily() + "}", htmlFieldSchema
					.getElasticsearch().containsKey("raw"));

			// Boolean
			BooleanFieldSchema booleanFieldSchema = updatedSchema.getField("booleanField", BooleanFieldSchemaImpl.class);
			assertFalse("The required flag should now be set to false.", booleanFieldSchema.isRequired());
			assertNull("The elasticsearch settings were not set correctly.", booleanFieldSchema.getElasticsearch());

			// Date
			DateFieldSchema dateFieldSchema = updatedSchema.getField("dateField", DateFieldSchemaImpl.class);
			assertFalse("The required flag should now be set to false.", dateFieldSchema.isRequired());
			assertNull("The index option was not set correctly.", dateFieldSchema.getElasticsearch());

			// Number
			NumberFieldSchema numberFieldSchema = updatedSchema.getField("numberField", NumberFieldSchemaImpl.class);
			assertFalse("The required flag should now be set to false.", numberFieldSchema.isRequired());
			assertNull("The elasticsearch settings were not set correctly.", numberFieldSchema.getElasticsearch());

			// List
			ListFieldSchema listFieldSchema = updatedSchema.getField("listField", ListFieldSchemaImpl.class);
			assertFalse("The required flag should now be set to false.", listFieldSchema.isRequired());
			assertNotNull(listFieldSchema.getAllowedSchemas());
			assertThat(listFieldSchema.getAllowedSchemas()).contains("A1", "B1", "C1");
			assertNull("The index option was not set correctly.", listFieldSchema.getElasticsearch());

		}
	}

}
