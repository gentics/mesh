package com.gentics.mesh.core.field.number;

import static com.gentics.mesh.core.field.number.NumberListFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.number.NumberListFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.number.NumberListFieldTestHelper.FILLNUMBERS;
import static com.gentics.mesh.util.DateUtils.toISO8601;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.NoConsistencyCheck;
import com.gentics.mesh.util.CoreTestUtils;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class NumberListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String NUMBER_LIST = "numberList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		return createFieldSchema(NUMBER_LIST, isRequired);
	}
	protected ListFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("number");
		schema.setName(fieldKey);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");
			prepareNode(node, NUMBER_LIST, "number");

			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			HibNumberFieldList numberList = container.createNumberList(NUMBER_LIST);
			numberList.createNumber(1);
			numberList.createNumber(1.11);
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			NodeResponse response = transform(node);
			assertList(2, NUMBER_LIST, "number", response);
		}

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibNumberFieldList list = container.createNumberList(NUMBER_LIST);

			list.createNumber(1);
			assertEquals(1, list.getList().size());

			list.createNumber(2);
			assertEquals(2, list.getList().size());
			list.removeAll();
			assertEquals(0, list.getSize());
			assertEquals(0, list.getList().size());
		}
	}

	@Test
	@NoConsistencyCheck
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibNumberFieldList testField = container.createNumberList(NUMBER_LIST);
			testField.createNumber(47);
			testField.createNumber(11);

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer(createFieldSchema(true));
			testField.cloneTo(otherContainer);

			assertTrue(otherContainer.getNumberList(NUMBER_LIST).equals(testField));
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema("fieldA", true), createFieldSchema("fieldB", true));
			HibNumberFieldList fieldA = container.createNumberList("fieldA");
			HibNumberFieldList fieldB = container.createNumberList("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.addItem(fieldA.createNumber(42L));
			assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.addItem(fieldB.createNumber(42L));
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibNumberFieldList fieldA = container.createNumberList(NUMBER_LIST);
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((HibNumberFieldList) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			Long dummyValue = 4200L;

			// rest null - graph null
			HibNumberFieldList fieldA = container.createNumberList(NUMBER_LIST);

			NumberFieldListImpl restField = new NumberFieldListImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.addItem(fieldA.createNumber(dummyValue));
			restField.add(dummyValue + 1L);
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.getItems().clear();
			restField.add(dummyValue);
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			DateFieldListImpl otherTypeRestField = new DateFieldListImpl();
			otherTypeRestField.add(toISO8601(dummyValue));
			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(otherTypeRestField));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(NUMBER_LIST, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(NUMBER_LIST, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(NUMBER_LIST, FETCH, FILLNUMBERS, (node) -> {
				updateContainer(ac, node, NUMBER_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(NUMBER_LIST, FETCH, FILLNUMBERS, (container) -> {
				updateContainer(ac, container, NUMBER_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(NUMBER_LIST, FILLNUMBERS, (container) -> {
				NumberFieldListImpl field = new NumberFieldListImpl();
				field.getItems().add(42L);
				field.getItems().add(43L);
				updateContainer(ac, container, NUMBER_LIST, field);
			}, (container) -> {
				HibNumberFieldList field = container.getNumberList(NUMBER_LIST);
				assertNotNull("The graph field {" + NUMBER_LIST + "} could not be found.", field);
				assertEquals("The list of the field was not updated.", 2, field.getList().size());
				assertEquals("The list item of the field was not updated.", 42L, field.getList().get(0).getNumber().longValue());
				assertEquals("The list item of the field was not updated.", 43L, field.getList().get(1).getNumber().longValue());
			});
		}
	}

}
