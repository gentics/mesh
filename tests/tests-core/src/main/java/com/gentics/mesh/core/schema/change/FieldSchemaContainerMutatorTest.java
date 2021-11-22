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

import com.gentics.mesh.core.data.schema.HibFieldTypeChange;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.HibUpdateFieldChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
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
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.IndexOptionHelper;

/**
 * Test for common mutator operations on a field containers.
 */
@MeshTestSetting(testSize = FULL, startServer = false)
public class FieldSchemaContainerMutatorTest extends AbstractMeshTest {

	private FieldSchemaContainerMutator mutator = new FieldSchemaContainerMutator();

	@Test
	public void testNullOperation() throws MeshSchemaException {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();
			SchemaModelImpl schemaModel = new SchemaModelImpl();
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema);
			version.setSchema(schemaModel);
			SchemaModel updatedSchema = mutator.apply(version);
			assertNotNull(updatedSchema);
			assertEquals("No changes were specified. No modification should happen.", schemaModel, updatedSchema);
		}
	}

	@Test
	public void testUpdateTypeAndAllowProperty() throws MeshSchemaException {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();

			// 1. Create schema
			SchemaModelImpl schemaModel = new SchemaModelImpl("testschema");
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema);

			NumberFieldSchema numberField = new NumberFieldSchemaImpl();
			numberField.setName("testField");
			numberField.setRequired(true);
			numberField.setLabel("originalLabel");
			schemaModel.addField(numberField);

			version.setSchema(schemaModel);

			HibFieldTypeChange fieldTypeChange = (HibFieldTypeChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.CHANGEFIELDTYPE);
			fieldTypeChange.setFieldName("testField");
			fieldTypeChange.setRestProperty(SchemaChangeModel.TYPE_KEY, "string");
			fieldTypeChange.setRestProperty(SchemaChangeModel.ALLOW_KEY, new String[] { "testValue" });
			version.setNextChange(fieldTypeChange);

			// 3. Apply the changes
			SchemaModel updatedSchema = mutator.apply(version);

			StringFieldSchema stringFieldSchema = updatedSchema.getField("testField", StringFieldSchemaImpl.class);
			assertNotNull(stringFieldSchema);
			assertThat(stringFieldSchema.getAllowedValues()).containsExactly("testValue");
		}
	}

	@Test
	public void testUpdateLabel() throws MeshSchemaException {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();

			// 1. Create schema
			SchemaModelImpl schemaModel = new SchemaModelImpl("testschema");
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema);

			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setAllowedValues("blub");
			stringField.setName("stringField");
			stringField.setRequired(true);
			stringField.setLabel("originalLabel");
			schemaModel.addField(stringField);

			version.setSchema(schemaModel);

			HibUpdateFieldChange stringFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			stringFieldUpdate.setFieldName("stringField");
			stringFieldUpdate.setRestProperty(SchemaChangeModel.LABEL_KEY, "UpdatedLabel");
			version.setNextChange(stringFieldUpdate);

			// 3. Apply the changes
			SchemaModel updatedSchema = mutator.apply(version);

			StringFieldSchema stringFieldSchema = updatedSchema.getField("stringField", StringFieldSchemaImpl.class);
			assertNotNull(stringFieldSchema);
			assertEquals("UpdatedLabel", stringFieldSchema.getLabel());
		}
	}

	@Test
	public void testAUpdateFields() throws MeshSchemaException {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();

			// 1. Create schema
			SchemaModelImpl schemaModel = new SchemaModelImpl("testschema");
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema);

			BinaryFieldSchema binaryField = new BinaryFieldSchemaImpl();
			binaryField.setName("binaryField");
			binaryField.setAllowedMimeTypes("oldTypes");
			binaryField.setRequired(true);
			schemaModel.addField(binaryField);

			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setAllowedValues("blub");
			stringField.setName("stringField");
			stringField.setRequired(true);
			schemaModel.addField(stringField);

			NodeFieldSchema nodeField = new NodeFieldSchemaImpl();
			nodeField.setAllowedSchemas("blub");
			nodeField.setName("nodeField");
			nodeField.setRequired(true);
			schemaModel.addField(nodeField);

			MicronodeFieldSchema micronodeField = new MicronodeFieldSchemaImpl();
			micronodeField.setAllowedMicroSchemas("blub");
			micronodeField.setName("micronodeField");
			micronodeField.setRequired(true);
			schemaModel.addField(micronodeField);

			NumberFieldSchema numberField = new NumberFieldSchemaImpl();
			numberField.setName("numberField");
			numberField.setRequired(true);
			schemaModel.addField(numberField);

			HtmlFieldSchema htmlField = new HtmlFieldSchemaImpl();
			htmlField.setName("htmlField");
			htmlField.setRequired(true);
			schemaModel.addField(htmlField);

			BooleanFieldSchema booleanField = new BooleanFieldSchemaImpl();
			booleanField.setName("booleanField");
			booleanField.setRequired(true);
			schemaModel.addField(booleanField);

			DateFieldSchema dateField = new DateFieldSchemaImpl();
			dateField.setName("dateField");
			dateField.setRequired(true);
			schemaModel.addField(dateField);

			ListFieldSchema listField = new ListFieldSchemaImpl();
			listField.setName("listField");
			listField.setListType("micronode");
			listField.setRequired(true);
			schemaModel.addField(listField);

			version.setSchema(schemaModel);

			// 2. Create schema field update change
			HibUpdateFieldChange binaryFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			binaryFieldUpdate.setFieldName("binaryField");
			binaryFieldUpdate.setRestProperty("allowedMimeTypes", new String[] { "newTypes" });
			binaryFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			version.setNextChange(binaryFieldUpdate);

			HibUpdateFieldChange nodeFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			nodeFieldUpdate.setFieldName("nodeField");
			nodeFieldUpdate.setRestProperty(ALLOW_KEY, new String[] { "schemaA", "schemaB" });
			nodeFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			binaryFieldUpdate.setNextChange(nodeFieldUpdate);

			HibUpdateFieldChange stringFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			stringFieldUpdate.setRestProperty(ALLOW_KEY, new String[] { "valueA", "valueB" });
			stringFieldUpdate.setFieldName("stringField");
			stringFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			stringFieldUpdate.setIndexOptions(IndexOptionHelper.getRawFieldOption());
			nodeFieldUpdate.setNextChange(stringFieldUpdate);

			HibUpdateFieldChange htmlFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			htmlFieldUpdate.setFieldName("htmlField");
			htmlFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			htmlFieldUpdate.setRestProperty(SchemaChangeModel.ELASTICSEARCH_KEY, IndexOptionHelper.getRawFieldOption().encode());
			stringFieldUpdate.setNextChange(htmlFieldUpdate);

			HibUpdateFieldChange numberFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			numberFieldUpdate.setFieldName("numberField");
			numberFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			htmlFieldUpdate.setNextChange(numberFieldUpdate);

			HibUpdateFieldChange dateFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			dateFieldUpdate.setFieldName("dateField");
			dateFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			numberFieldUpdate.setNextChange(dateFieldUpdate);

			HibUpdateFieldChange booleanFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			booleanFieldUpdate.setFieldName("booleanField");
			booleanFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			dateFieldUpdate.setNextChange(booleanFieldUpdate);

			HibUpdateFieldChange micronodeFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			micronodeFieldUpdate.setFieldName("micronodeField");
			micronodeFieldUpdate.setRestProperty(SchemaChangeModel.ALLOW_KEY, new String[] { "A", "B", "C" });
			micronodeFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			booleanFieldUpdate.setNextChange(micronodeFieldUpdate);

			HibUpdateFieldChange listFieldUpdate = (HibUpdateFieldChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.UPDATEFIELD);
			listFieldUpdate.setFieldName("listField");
			listFieldUpdate.setRestProperty(SchemaChangeModel.REQUIRED_KEY, false);
			listFieldUpdate.setRestProperty(SchemaChangeModel.ALLOW_KEY, new String[] { "A1", "B1", "C1" });
			micronodeFieldUpdate.setNextChange(listFieldUpdate);

			// 3. Apply the changes
			SchemaModel updatedSchema = mutator.apply(version);

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
