package com.gentics.mesh.core.field.date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

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

		Node node = folder("2015");
		prepareNode(node, "dateList", "date");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		DateGraphFieldList dateList = container.createDateList("dateList");
		dateList.createDate(1L);
		dateList.createDate(2L);

		NodeResponse response = transform(node);
		assertList(2, "dateList", "date", response);

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldList list = container.createDateList("dummyList");
		assertNotNull(list);
		DateGraphField dateField = list.createDate(1L);
		assertNotNull(dateField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());

	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldList testField = container.createDateList("testField");
		testField.createDate(47L);
		testField.createDate(11L);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getDateList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
	}

	@Override
	public void testEquals() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testEqualsNull() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testEqualsRestField() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestNullOnCreate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testRemoveFieldViaNullValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testDeleteRequiredFieldViaNullValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestValidSimpleValue() {
		// TODO Auto-generated method stub
		
	}

}
