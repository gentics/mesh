package com.gentics.mesh.core.field.number;

import static com.gentics.mesh.core.field.number.NumberFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.number.NumberFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.number.NumberFieldTestHelper.FILL;
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
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.NoConsistencyCheck;
import com.gentics.mesh.util.CoreTestUtils;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class NumberFieldTest extends AbstractFieldTest<NumberFieldSchema> {

	private static final String NUMBER_FIELD = "numberField";

	@Override
	protected NumberFieldSchema createFieldSchema(boolean isRequired) {
		return createFieldSchema(NUMBER_FIELD, isRequired);
	}
	protected NumberFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName(fieldKey);
		numberFieldSchema.setLabel("Some number field");
		numberFieldSchema.setRequired(isRequired);
		return numberFieldSchema;
	}

	@Test
	@NoConsistencyCheck
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibNumberField testField = container.createNumber(NUMBER_FIELD);
			testField.setNumber(4711);

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer(createFieldSchema(true));
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getNumber(NUMBER_FIELD)).as("cloned field").isNotNull()
					.isEqualToIgnoringGivenFields(testField, "parentContainer", "value");
			assertEquals(otherContainer.getNumber(NUMBER_FIELD).getNumber().doubleValue(), testField.getNumber().doubleValue(), 0.0001);
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibNumberField numberField = container.createNumber(NUMBER_FIELD);
			assertEquals(NUMBER_FIELD, numberField.getFieldKey());
			numberField.setNumber(42);
			assertEquals(42, numberField.getNumber().intValue());
			HibStringField bogusField1 = container.getString("bogus");
			assertNull(bogusField1);
			HibNumberField reloadedNumberField = container.getNumber(NUMBER_FIELD);
			assertNotNull(reloadedNumberField);
			assertEquals(NUMBER_FIELD, reloadedNumberField.getFieldKey());
		}
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");

			// Update the schema
			NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
			numberFieldSchema.setName(NUMBER_FIELD);
			// numberFieldSchema.setMin(10);
			// numberFieldSchema.setMax(1000);
			numberFieldSchema.setRequired(true);
			prepareTypedSchema(node, numberFieldSchema, false);
			tx.commit();
			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			HibNumberField numberField = container.createNumber(NUMBER_FIELD);
			numberField.setNumber(100.9f);
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			String json = getJson(node);
			assertTrue("Could not find number within json. Json {" + json + "}", json.indexOf("100.9") > 1);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);
			NumberFieldImpl deserializedNumberField = response.getFields().getNumberField(NUMBER_FIELD);
			assertEquals(Double.valueOf(100.9), deserializedNumberField.getNumber().doubleValue(), 0.01);
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true), createFieldSchema(NUMBER_FIELD + "_2", true));
			Long number = System.currentTimeMillis();
			HibNumberField fieldA = container.createNumber(NUMBER_FIELD);
			HibNumberField fieldB = container.createNumber(NUMBER_FIELD + "_2");
			fieldA.setNumber(number);
			fieldB.setNumber(number);
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true), createFieldSchema(NUMBER_FIELD + "_2", true));
			HibNumberField fieldA = container.createNumber(NUMBER_FIELD);
			HibNumberField fieldB = container.createNumber(NUMBER_FIELD + "_2");
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			Long number = System.currentTimeMillis();

			// rest null - graph null
			HibNumberField fieldA = container.createNumber(NUMBER_FIELD);
			NumberFieldImpl restField = new NumberFieldImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.setNumber(number);
			restField.setNumber(number + 1L);
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.setNumber(number);
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.",
					fieldA.equals(new StringFieldImpl().setString(String.valueOf(number))));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(NUMBER_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(NUMBER_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(NUMBER_FIELD, FETCH, FILL, (node) -> {
				updateContainer(ac, node, NUMBER_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(NUMBER_FIELD, FETCH, FILL, (container) -> {
				updateContainer(ac, container, NUMBER_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(NUMBER_FIELD, FILL, (container) -> {
				NumberField field = new NumberFieldImpl();
				field.setNumber(42L);
				updateContainer(ac, container, NUMBER_FIELD, field);
			}, (container) -> {
				HibNumberField field = container.getNumber(NUMBER_FIELD);
				assertNotNull("The graph field {" + NUMBER_FIELD + "} could not be found.", field);
				assertEquals("The html of the field was not updated.", 42L, field.getNumber().longValue());
			});
		}
	}

}
