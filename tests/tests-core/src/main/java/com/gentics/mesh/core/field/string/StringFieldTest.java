package com.gentics.mesh.core.field.string;

import static com.gentics.mesh.core.field.string.StringFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.string.StringFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.string.StringFieldTestHelper.FILLTEXT;
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
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.NoConsistencyCheck;
import com.gentics.mesh.util.CoreTestUtils;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class StringFieldTest extends AbstractFieldTest<StringFieldSchema> {

	private static final String STRING_FIELD = "stringField";

	@Override
	protected StringFieldSchema createFieldSchema(boolean isRequired) {
		return createFieldSchema(STRING_FIELD, isRequired);
	}
	protected StringFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
		StringFieldSchema schema = new StringFieldSchemaImpl();
		schema.setLabel("Some string field");
		schema.setRequired(isRequired);
		schema.setName(fieldKey);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");

			// Add a new string field to the schema
			StringFieldSchemaImpl stringFieldSchema = new StringFieldSchemaImpl();
			stringFieldSchema.setName(STRING_FIELD);
			stringFieldSchema.setLabel("Some string field");
			stringFieldSchema.setRequired(true);
			prepareTypedSchema(node, stringFieldSchema, false);
			tx.commit();

			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			HibStringField field = container.createString(STRING_FIELD);
			field.setString("someString");
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			String json = getJson(node);
			assertTrue("The json should contain the string but it did not.{" + json + "}", json.indexOf("someString") > 1);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);

			com.gentics.mesh.core.rest.node.field.StringField deserializedNodeField = response.getFields().getStringField(STRING_FIELD);
			assertNotNull(deserializedNodeField);
			assertEquals("someString", deserializedNodeField.getString());
		}
	}

	@Test
	@NoConsistencyCheck
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibStringField testField = container.createString(STRING_FIELD);
			testField.setString("this is the string");

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer(createFieldSchema(true));
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getString(STRING_FIELD)).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(testField,
					"parentContainer");
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibStringField stringField = container.createString(STRING_FIELD);
			assertEquals(STRING_FIELD, stringField.getFieldKey());
			stringField.setString("dummyString");
			assertEquals("dummyString", stringField.getString());
			HibStringField bogusField1 = container.getString("bogus");
			assertNull(bogusField1);
			HibStringField reloadedStringField = container.getString(STRING_FIELD);
			assertNotNull(reloadedStringField);
			assertEquals(STRING_FIELD, reloadedStringField.getFieldKey());
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true), createFieldSchema(STRING_FIELD + "_2", false));
			String testValue = "test123";
			HibStringField fieldA = container.createString(STRING_FIELD);
			HibStringField fieldB = container.createString(STRING_FIELD + "_2");
			fieldA.setString(testValue);
			fieldB.setString(testValue);
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true), createFieldSchema(STRING_FIELD + "_2", false));
			HibStringField fieldA = container.createString(STRING_FIELD);
			HibStringField fieldB = container.createString(STRING_FIELD + "_2");
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			String dummyValue = "test123";

			// rest null - graph null
			HibStringField fieldA = container.createString(STRING_FIELD);
			StringFieldImpl restField = new StringFieldImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.setString(dummyValue);
			restField.setString(dummyValue + 1L);
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.setString(dummyValue);
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(new HtmlFieldImpl().setHTML(dummyValue)));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(STRING_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(STRING_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(STRING_FIELD, FETCH, FILLTEXT, (node) -> {
				updateContainer(ac, node, STRING_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(STRING_FIELD, FETCH, FILLTEXT, (container) -> {
				updateContainer(ac, container, STRING_FIELD, null);
			});
		}
	}

	@Test
	public void testRemoveSegmentField() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveSegmentFieldViaNullTestcase(STRING_FIELD, FETCH, FILLTEXT, (container) -> {
				updateContainer(ac, container, STRING_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(STRING_FIELD, FILLTEXT, (container) -> {
				StringField field = new StringFieldImpl();
				field.setString("someValue");
				updateContainer(ac, container, STRING_FIELD, field);
			}, (container) -> {
				HibStringField field = container.getString(STRING_FIELD);
				assertNotNull("The graph field {" + STRING_FIELD + "} could not be found.", field);
				assertEquals("The string of the field was not updated.", "someValue", field.getString());
			});
		}
	}
}
