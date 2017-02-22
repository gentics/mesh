package com.gentics.mesh.core.field.number;

import static com.gentics.mesh.core.field.number.NumberFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.number.NumberFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.number.NumberFieldTestHelper.FILL;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class NumberFieldTest extends AbstractFieldTest<NumberFieldSchema> {

	private static final String NUMBER_FIELD = "numberField";

	@Override
	protected NumberFieldSchema createFieldSchema(boolean isRequired) {
		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName(NUMBER_FIELD);
		numberFieldSchema.setLabel("Some number field");
		numberFieldSchema.setRequired(isRequired);
		return numberFieldSchema;
	}

	@Test
	public void testSimpleNumber() {
		try (NoTx noTx = db().noTx()) {
			NodeGraphFieldContainerImpl container = noTx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NumberGraphFieldImpl field = new NumberGraphFieldImpl("test", container);
			assertEquals(2, container.getPropertyKeys().size());
			assertNull(container.getProperty("test-number"));
			assertEquals(2, container.getPropertyKeys().size());
			field.setNumber(42);
			assertEquals(42L, field.getNumber());
			assertEquals("42", container.getProperty("test-number"));
			assertEquals(3, container.getPropertyKeys().size());
			field.setNumber(null);
			assertNull(field.getNumber());
			assertNull(container.getProperty("test-number"));
		}
	}

	@Test
	@Override
	public void testClone() {
		try (NoTx noTx = db().noTx()) {
			NodeGraphFieldContainerImpl container = noTx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NumberGraphField testField = container.createNumber("testField");
			testField.setNumber(4711);

			NodeGraphFieldContainerImpl otherContainer = noTx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getNumber("testField")).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(testField,
					"parentContainer");
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (NoTx noTx = db().noTx()) {
			NodeGraphFieldContainerImpl container = noTx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NumberGraphField numberField = container.createNumber("numberField");
			assertEquals("numberField", numberField.getFieldKey());
			numberField.setNumber(42);
			assertEquals(42L, numberField.getNumber());
			StringGraphField bogusField1 = container.getString("bogus");
			assertNull(bogusField1);
			NumberGraphField reloadedNumberField = container.getNumber("numberField");
			assertNotNull(reloadedNumberField);
			assertEquals("numberField", reloadedNumberField.getFieldKey());
		}
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		try (NoTx noTx = db().noTx()) {
			Node node = folder("2015");

			// Update the schema
			Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
			NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
			numberFieldSchema.setName("numberField");
			//		numberFieldSchema.setMin(10);
			//		numberFieldSchema.setMax(1000);
			numberFieldSchema.setRequired(true);
			schema.addField(numberFieldSchema);
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			NumberGraphField numberField = container.createNumber("numberField");
			numberField.setNumber(100.9f);

			String json = getJson(node);
			assertTrue("Could not find number within json. Json {" + json + "}", json.indexOf("100.9") > 1);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);
			NumberFieldImpl deserializedNumberField = response.getFields().getNumberField("numberField");
			assertEquals(100.9, deserializedNumberField.getNumber());
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (NoTx noTx = db().noTx()) {
			NodeGraphFieldContainer container = noTx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			Long number = System.currentTimeMillis();
			NumberGraphField fieldA = container.createNumber(NUMBER_FIELD);
			NumberGraphField fieldB = container.createNumber(NUMBER_FIELD + "_2");
			fieldA.setNumber(number);
			fieldB.setNumber(number);
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (NoTx noTx = db().noTx()) {
			NodeGraphFieldContainer container = noTx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NumberGraphField fieldA = container.createNumber(NUMBER_FIELD);
			NumberGraphField fieldB = container.createNumber(NUMBER_FIELD + "_2");
			assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (NoTx noTx = db().noTx()) {
			NodeGraphFieldContainer container = noTx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			Long number = System.currentTimeMillis();

			// rest null - graph null
			NumberGraphField fieldA = container.createNumber(NUMBER_FIELD);
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
		try (NoTx noTx = db().noTx()) {
			invokeUpdateFromRestTestcase(NUMBER_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (NoTx noTx = db().noTx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(NUMBER_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (NoTx noTx = db().noTx()) {
			InternalActionContext ac = getMockedInternalActionContext();
			invokeRemoveFieldViaNullTestcase(NUMBER_FIELD, FETCH, FILL, (node) -> {
				updateContainer(ac, node, NUMBER_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (NoTx noTx = db().noTx()) {
			InternalActionContext ac = getMockedInternalActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(NUMBER_FIELD, FETCH, FILL, (container) -> {
				updateContainer(ac, container, NUMBER_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (NoTx noTx = db().noTx()) {
			InternalActionContext ac = getMockedInternalActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(NUMBER_FIELD, FILL, (container) -> {
				NumberField field = new NumberFieldImpl();
				field.setNumber(42L);
				updateContainer(ac, container, NUMBER_FIELD, field);
			}, (container) -> {
				NumberGraphField field = container.getNumber(NUMBER_FIELD);
				assertNotNull("The graph field {" + NUMBER_FIELD + "} could not be found.", field);
				assertEquals("The html of the field was not updated.", 42L, field.getNumber());
			});
		}
	}

}
