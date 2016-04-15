package com.gentics.mesh.core.field.number;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class NumberListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String NUMBER_LIST = "numberList";

	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("number");
		schema.setName(NUMBER_LIST);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {

		Node node = folder("2015");
		prepareNode(node, "numberList", "number");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		NumberGraphFieldList numberList = container.createNumberList("numberList");
		numberList.createNumber(1);
		numberList.createNumber(1.11);

		NodeResponse response = transform(node);
		assertList(2, "numberList", "number", response);

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NumberGraphFieldList list = container.createNumberList("dummyList");

		list.createNumber(1);
		assertEquals(1, list.getList().size());

		list.createNumber(2);
		assertEquals(2, list.getList().size());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NumberGraphFieldList testField = container.createNumberList("testField");
		testField.createNumber(47);
		testField.createNumber(11);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getNumberList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
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
