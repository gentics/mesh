package com.gentics.mesh.core.field.json;

import static com.gentics.mesh.core.field.json.JsonFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.json.JsonFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.json.JsonFieldTestHelper.FILLTEXT;
import static com.gentics.mesh.core.field.json.JsonFieldTestHelper.make;
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
import com.gentics.mesh.core.data.node.field.HibJsonField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.JsonContent;
import com.gentics.mesh.core.rest.node.field.JsonField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.JsonFieldImpl;
import com.gentics.mesh.core.rest.schema.JsonFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.JsonFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.NoConsistencyCheck;
import com.gentics.mesh.util.CoreTestUtils;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class JsonFieldTest extends AbstractFieldTest<JsonFieldSchema> {

	private static final String JSON_FIELD = "jsonField";

	@Override
	protected JsonFieldSchema createFieldSchema(boolean isRequired) {
		return createFieldSchema(JSON_FIELD, isRequired);
	}
	protected JsonFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
		JsonFieldSchema schema = new JsonFieldSchemaImpl();
		schema.setLabel("Some JSON field");
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
			JsonFieldSchemaImpl jsonFieldSchema = new JsonFieldSchemaImpl();
			jsonFieldSchema.setName(JSON_FIELD);
			jsonFieldSchema.setLabel("Some string field");
			jsonFieldSchema.setRequired(true);
			prepareTypedSchema(node, jsonFieldSchema, false);
			tx.commit();

			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			HibJsonField field = container.createJson(JSON_FIELD);
			field.setJson(make("someString"));
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			String json = getJson(node);
			assertTrue("The json should contain the string but it did not.{" + json + "}", json.indexOf("someString") > 1);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);

			com.gentics.mesh.core.rest.node.field.JsonField deserializedNodeField = response.getFields().getJsonField(JSON_FIELD);
			assertNotNull(deserializedNodeField);
			assertEquals(make("someString"), deserializedNodeField.getJson());
		}
	}

	@Test
	@NoConsistencyCheck
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibJsonField testField = container.createJson(JSON_FIELD);
			testField.setJson(JsonContent.fromObject(new JsonObject()));

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer(createFieldSchema(true));
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getJson(JSON_FIELD)).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(testField,
					"parentContainer");
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibJsonField jsonField = container.createJson(JSON_FIELD);
			assertEquals(JSON_FIELD, jsonField.getFieldKey());
			jsonField.setJson(make("dummyString"));
			assertEquals(make("dummyString"), jsonField.getJson());
			HibJsonField bogusField1 = container.getJson("bogus");
			assertNull(bogusField1);
			HibJsonField reloadedJsonField = container.getJson(JSON_FIELD);
			assertNotNull(reloadedJsonField);
			assertEquals(JSON_FIELD, reloadedJsonField.getFieldKey());
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true), createFieldSchema(JSON_FIELD + "_2", false));
			String testValue = "test123";
			HibJsonField fieldA = container.createJson(JSON_FIELD);
			HibJsonField fieldB = container.createJson(JSON_FIELD + "_2");
			fieldA.setJson(make(testValue));
			fieldB.setJson(make(testValue));
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true), createFieldSchema(JSON_FIELD + "_2", false));
			HibJsonField fieldA = container.createJson(JSON_FIELD);
			HibJsonField fieldB = container.createJson(JSON_FIELD + "_2");
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			String dummyValue = "test123";

			// rest null - graph null
			HibJsonField fieldA = container.createJson(JSON_FIELD);
			JsonFieldImpl restField = new JsonFieldImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.setJson(make(dummyValue));
			restField.setJson(make(dummyValue + 1L));
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.setJson(make(dummyValue));
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(new HtmlFieldImpl().setHTML(JsonUtil.toJson(make(dummyValue)))));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(JSON_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(JSON_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(JSON_FIELD, FETCH, FILLTEXT, (node) -> {
				updateContainer(ac, node, JSON_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(JSON_FIELD, FETCH, FILLTEXT, (container) -> {
				updateContainer(ac, container, JSON_FIELD, null);
			});
		}
	}

	@Test
	public void testRemoveSegmentField() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveSegmentFieldViaNullTestcase(JSON_FIELD, FETCH, FILLTEXT, (container) -> {
				updateContainer(ac, container, JSON_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(JSON_FIELD, FILLTEXT, (container) -> {
				JsonField field = new JsonFieldImpl();
				field.setJson(make("someValue"));
				updateContainer(ac, container, JSON_FIELD, field);
			}, (container) -> {
				HibJsonField field = container.getJson(JSON_FIELD);
				assertNotNull("The graph field {" + JSON_FIELD + "} could not be found.", field);
				assertEquals("The string of the field was not updated.", make("someValue"), field.getJson());
			});
		}
	}
}
