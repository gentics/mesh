package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.core.field.date.DateListFieldHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.date.DateListFieldHelper.FETCH;
import static com.gentics.mesh.core.field.date.DateListFieldHelper.FILL;
import static com.gentics.mesh.util.DateUtils.toISO8601;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.db.GraphDBTx;
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

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class DateListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String DATE_LIST = "dateList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("date");
		schema.setName(DATE_LIST);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		HibNode node = folder("2015");
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			prepareNode(node, "dateList", "date");

			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			HibDateFieldList dateList = container.createDateList("dateList");
			dateList.createDate(1L);
			dateList.createDate(2L);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = transform(node);
			assertList(2, "dateList", "date", response);
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			HibDateFieldList list = container.createDateList("dummyList");
			assertNotNull(list);
			HibDateField dateField = list.createDate(1L);
			assertNotNull(dateField);
			assertEquals(1, list.getSize());
			assertEquals(1, list.getList().size());
			list.removeAll();
			assertEquals(0, list.getSize());
			assertEquals(0, list.getList().size());
		}
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			HibDateFieldList testField = container.createDateList("testField");
			testField.createDate(47L);
			testField.createDate(11L);

			NodeGraphFieldContainerImpl otherContainer = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getDateList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			HibDateFieldList fieldA = container.createDateList("fieldA");
			HibDateFieldList fieldB = container.createDateList("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.addItem(fieldA.createDate(42L));
			assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.addItem(fieldB.createDate(42L));
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			HibDateFieldList fieldA = container.createDateList("fieldA");
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((GraphField) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			Long dummyValue = 4200L;

			// rest null - graph null
			HibDateFieldList fieldA = container.createDateList(DATE_LIST);

			DateFieldListImpl restField = new DateFieldListImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.addItem(fieldA.createDate(dummyValue));
			restField.add(toISO8601(dummyValue + 1000L));
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.getItems().clear();
			restField.add(toISO8601(dummyValue));
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			NumberFieldListImpl otherTypeRestField = new NumberFieldListImpl();
			otherTypeRestField.add(dummyValue);
			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(otherTypeRestField));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(DATE_LIST, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(DATE_LIST, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(DATE_LIST, FETCH, FILL, (node) -> {
				updateContainer(ac, node, DATE_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(DATE_LIST, FETCH, FILL, (container) -> {
				updateContainer(ac, container, DATE_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(DATE_LIST, FILL, (container) -> {
				DateFieldListImpl field = new DateFieldListImpl();
				field.getItems().add(toISO8601(42000L));
				field.getItems().add(toISO8601(43000L));
				updateContainer(ac, container, DATE_LIST, field);
			}, (container) -> {
				HibDateFieldList field = container.getDateList(DATE_LIST);
				assertNotNull("The graph field {" + DATE_LIST + "} could not be found.", field);
				assertEquals("The list of the field was not updated.", 2, field.getList().size());
				assertEquals("The list item of the field was not updated.", 42000L, field.getList().get(0).getDate().longValue());
				assertEquals("The list item of the field was not updated.", 43000L, field.getList().get(1).getDate().longValue());
			});
		}
	}

}
