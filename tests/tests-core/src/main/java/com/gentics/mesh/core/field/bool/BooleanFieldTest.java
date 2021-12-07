package com.gentics.mesh.core.field.bool;

import static com.gentics.mesh.core.field.bool.BooleanFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.bool.BooleanFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.bool.BooleanFieldTestHelper.FILLTRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.util.CoreTestUtils;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class BooleanFieldTest extends AbstractFieldTest<BooleanFieldSchema> {

	private static final String BOOLEAN_FIELD = "booleanField";

	@Override
	protected BooleanFieldSchema createFieldSchema(boolean isRequired) {
		BooleanFieldSchemaImpl schema = new BooleanFieldSchemaImpl();
		schema.setLabel("Some boolean field");
		schema.setRequired(isRequired);
		schema.setName(BOOLEAN_FIELD);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		HibNode node = folder("2015");

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			// Update the schema and add a boolean field
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			BooleanFieldSchemaImpl booleanFieldSchema = new BooleanFieldSchemaImpl();
			booleanFieldSchema.setName(BOOLEAN_FIELD);
			booleanFieldSchema.setLabel("Some boolean field");
			booleanFieldSchema.setRequired(true);
			schema.addField(booleanFieldSchema);
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			HibBooleanField field = container.createBoolean(BOOLEAN_FIELD);
			field.setBoolean(true);
			tx.success();
		}

		try (Tx tx = tx()) {
			String json = getJson(node);
			assertTrue("The json should contain the boolean field but it did not.{" + json + "}", json.indexOf("booleanField\" : true") > 1);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);

			com.gentics.mesh.core.rest.node.field.BooleanField deserializedNodeField = response.getFields().getBooleanField("booleanField");
			assertNotNull(deserializedNodeField);
			assertEquals(true, deserializedNodeField.getValue());
		}
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibBooleanField trueBooleanField = container.createBoolean("trueBooleanField");
			trueBooleanField.setBoolean(true);
			HibBooleanField falseBooleanField = container.createBoolean("falseBooleanField");
			falseBooleanField.setBoolean(false);

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer();
			trueBooleanField.cloneTo(otherContainer);
			falseBooleanField.cloneTo(otherContainer);

			assertThat(otherContainer.getBoolean("trueBooleanField")).as("cloned true field").isNotNull()
					.isEqualToIgnoringGivenFields(trueBooleanField, "parentContainer");
			assertThat(otherContainer.getBoolean("falseBooleanField")).as("cloned false field").isNotNull()
					.isEqualToIgnoringGivenFields(falseBooleanField, "parentContainer");
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();;
			HibBooleanField booleanField = container.createBoolean(BOOLEAN_FIELD);
			assertEquals(BOOLEAN_FIELD, booleanField.getFieldKey());
			booleanField.setBoolean(true);
			assertTrue("The boolean field value was not changed to true", booleanField.getBoolean());

			booleanField.setBoolean(false);
			assertFalse("The boolean field value was not changed to false", booleanField.getBoolean());

			booleanField.setBoolean(null);
			assertNull("The boolean field value was not set to null.", booleanField.getBoolean());

			HibBooleanField bogusField2 = container.getBoolean("bogus");
			assertNull("No field with the name bogus should have been found.", bogusField2);

			HibBooleanField reloadedBooleanField = container.getBoolean("booleanField");
			assertNull("The boolean field value was set to null and thus the field should have been removed.", reloadedBooleanField);
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibBooleanField fieldA = container.createBoolean("fieldA");
			HibBooleanField fieldB = container.createBoolean("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.setBoolean(true);
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.setBoolean(true);
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibBooleanField fieldA = container.createBoolean("fieldA");
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((HibBooleanField) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibBooleanField fieldA = container.createBoolean("fieldA");

			// graph empty - rest empty
			assertTrue("The field should be equal to the rest field since both fields have no value.", fieldA.equals(new BooleanFieldImpl()));

			// graph set - rest set - same value - different type
			fieldA.setBoolean(true);
			assertFalse("The field should not be equal to the rest field since the types do not match.",
					fieldA.equals(new HtmlFieldImpl().setHTML("true")));

			// graph set - rest set - different value
			assertFalse("The field should not be equal to the rest field since the rest field has a different value.",
					fieldA.equals(new BooleanFieldImpl().setValue(false)));

			// graph set - rest set - same value
			assertTrue("The field should be equal to a html rest field with the same value", fieldA.equals(new BooleanFieldImpl().setValue(true)));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(BOOLEAN_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(BOOLEAN_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(BOOLEAN_FIELD, FETCH, FILLTRUE, (node) -> {
				updateContainer(ac, node, BOOLEAN_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(BOOLEAN_FIELD, FETCH, FILLTRUE, (container) -> {
				updateContainer(ac, container, BOOLEAN_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(BOOLEAN_FIELD, FILLTRUE, (container) -> {
				BooleanField field = new BooleanFieldImpl();
				field.setValue(false);
				updateContainer(ac, container, BOOLEAN_FIELD, field);
			}, (container) -> {
				HibBooleanField field = container.getBoolean(BOOLEAN_FIELD);
				assertNotNull("The graph field {" + BOOLEAN_FIELD + "} could not be found.", field);
				assertEquals("The boolean of the field was not updated.", false, field.getBoolean());
			});
		}
	}

}
