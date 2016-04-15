package com.gentics.mesh.core.field.bool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;

public class BooleanListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	@Test
	@Override
	public void testFieldTransformation() throws Exception {

		Node node = folder("2015");
		prepareNode(node, "booleanList", "boolean");
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());

		BooleanGraphFieldList booleanList = container.createBooleanList("booleanList");
		booleanList.createBoolean(true);
		booleanList.createBoolean(null);
		booleanList.createBoolean(false);

		NodeResponse response = transform(node);

		assertList(2, "booleanList", "boolean", response);

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BooleanGraphFieldList list = container.createBooleanList("dummyList");
		list.createBoolean(true);
		list.createBoolean(false);
		list.createBoolean(null);
		assertEquals("Only non-null values are persisted.", 2, list.getList().size());
		assertEquals(2, list.getSize());
		assertNotNull(list.getBoolean(1));
		assertTrue(list.getBoolean(1).getBoolean());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BooleanGraphFieldList testField = container.createBooleanList("testField");
		testField.createBoolean(true);
		testField.createBoolean(false);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getBooleanList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
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

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		// TODO Auto-generated method stub
		return null;
	}

}
