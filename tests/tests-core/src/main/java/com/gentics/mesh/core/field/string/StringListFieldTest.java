package com.gentics.mesh.core.field.string;

import static com.gentics.mesh.core.field.string.StringListFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.string.StringListFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.string.StringListFieldTestHelper.FILLTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.util.CoreTestUtils;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class StringListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String STRING_LIST = "stringList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		return createFieldSchema(STRING_LIST, isRequired);
	}
	protected ListFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("string");
		schema.setName(fieldKey);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			ContentDao contentDao = tx.contentDao();
			prepareNode(node, STRING_LIST, "string");
			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			HibStringFieldList stringList = container.createStringList(STRING_LIST);
			stringList.createString("dummyString1");
			stringList.createString("dummyString2");
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			NodeResponse response = transform(node);
			assertList(2, STRING_LIST, "string", response);
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibStringFieldList list = container.createStringList(STRING_LIST);

			list.createString("1");
			assertEquals(STRING_LIST, list.getFieldKey());
			assertNotNull(list.getList());

			assertEquals(1, list.getList().size());
			assertEquals(list.getSize(), list.getList().size());
			list.createString("2");
			assertEquals(2, list.getList().size());
			list.createString("3").setString("Some string 3");
			assertEquals(3, list.getList().size());
			assertEquals("Some string 3", list.getList().get(2).getString());

			HibStringFieldList loadedList = container.getStringList(STRING_LIST);
			assertNotNull(loadedList);
			assertEquals(3, loadedList.getSize());
			list.removeAll();
			assertEquals(0, list.getSize());
			assertEquals(0, list.getList().size());
		}
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibStringFieldList testField = container.createStringList(STRING_LIST);
			testField.createString("one");
			testField.createString("two");
			testField.createString("three");

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer(createFieldSchema(true));
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getStringList(STRING_LIST).equals(testField));
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema("fieldA", true), createFieldSchema("fieldB", true));
			HibStringFieldList fieldA = container.createStringList("fieldA");
			HibStringFieldList fieldB = container.createStringList("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.addItem(fieldA.createString("testString"));
			assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.addItem(fieldB.createString("testString"));
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			HibStringFieldList fieldA = container.createStringList(STRING_LIST);
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((HibStringFieldList) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer(createFieldSchema(true));
			String dummyValue = "test123";

			// rest null - graph null
			HibStringFieldList fieldA = container.createStringList(STRING_LIST);

			StringFieldListImpl restField = new StringFieldListImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.addItem(fieldA.createString(dummyValue));
			restField.add(dummyValue + 1L);
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.getItems().clear();
			restField.add(dummyValue);
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			HtmlFieldListImpl otherTypeRestField = new HtmlFieldListImpl();
			otherTypeRestField.add(dummyValue);
			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(otherTypeRestField));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(STRING_LIST, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(STRING_LIST, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(STRING_LIST, FETCH, FILLTEXT, (node) -> {
				updateContainer(ac, node, STRING_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(STRING_LIST, FETCH, FILLTEXT, (container) -> {
				updateContainer(ac, container, STRING_LIST, null);
			});
		}
	}

	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(STRING_LIST, FILLTEXT, (container) -> {
				StringFieldListImpl field = new StringFieldListImpl();
				field.getItems().add("someValue");
				field.getItems().add("someValue2");
				updateContainer(ac, container, STRING_LIST, field);
			}, (container) -> {
				HibStringFieldList field = container.getStringList(STRING_LIST);
				assertNotNull("The graph field {" + STRING_LIST + "} could not be found.", field);
				assertEquals("The list of the field was not updated.", 2, field.getList().size());
				assertEquals("The list item of the field was not updated.", "someValue", field.getList().get(0).getString());
				assertEquals("The list item of the field was not updated.", "someValue2", field.getList().get(1).getString());
			});
		}
	}

}
