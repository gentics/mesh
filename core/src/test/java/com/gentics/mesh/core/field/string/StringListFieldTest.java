package com.gentics.mesh.core.field.string;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class StringListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String STRING_LIST = "stringList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("string");
		schema.setName(STRING_LIST);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {

		Node node = folder("2015");
		prepareNode(node, "stringList", "string");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		StringGraphFieldList stringList = container.createStringList("stringList");
		stringList.createString("dummyString1");
		stringList.createString("dummyString2");

		NodeResponse response = transform(node);
		assertList(2, "stringList", "string", response);

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		StringGraphFieldList list = container.createStringList("dummyList");

		list.createString("1");
		assertEquals("dummyList", list.getFieldKey());
		assertNotNull(list.getList());

		assertEquals(1, list.getList().size());
		assertEquals(list.getSize(), list.getList().size());
		list.createString("2");
		assertEquals(2, list.getList().size());
		list.createString("3").setString("Some string 3");
		assertEquals(3, list.getList().size());
		assertEquals("Some string 3", list.getList().get(2).getString());

		StringGraphFieldList loadedList = container.getStringList("dummyList");
		assertNotNull(loadedList);
		assertEquals(3, loadedList.getSize());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());

	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		StringGraphFieldList testField = container.createStringList("testField");
		testField.createString("one");
		testField.createString("two");
		testField.createString("three");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getStringList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
	}

	@Test
	@Override
	public void testEquals() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testEqualsNull() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testEqualsRestField() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRemoveFieldViaNullValue() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDeleteRequiredFieldViaNullValue() {
		// TODO Auto-generated method stub

	}

	@Override
	public void testUpdateFromRestValidSimpleValue() {
		// TODO Auto-generated method stub

	}

}
