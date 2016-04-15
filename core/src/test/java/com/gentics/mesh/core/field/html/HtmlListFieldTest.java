package com.gentics.mesh.core.field.html;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class HtmlListFieldTest extends AbstractFieldTest {

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		//		setupData();
		Node node = folder("2015");
		Schema schema = prepareNode(node, "nodeList", "node");

		ListFieldSchema htmlListFieldSchema = new ListFieldSchemaImpl();
		htmlListFieldSchema.setName("htmlList");
		htmlListFieldSchema.setListType("html");
		schema.addField(htmlListFieldSchema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		HtmlGraphFieldList htmlList = container.createHTMLList("htmlList");
		htmlList.createHTML("some<b>html</b>");
		htmlList.createHTML("some<b>more html</b>");

		NodeResponse response = transform(node);
		assertList(2, "htmlList", "html", response);

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphFieldList list = container.createHTMLList("dummyList");
		assertNotNull(list);
		HtmlGraphField htmlField = list.createHTML("HTML 1");
		assertNotNull(htmlField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	@Override
	public void testEqualsRestField() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphFieldList fieldA = container.createHTMLList("htmlListField");
		assertFalse(fieldA.equals((Field) null));
		assertFalse(fieldA.equals((GraphField) null));

	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphFieldList testField = container.createHTMLList("testField");
		testField.createHTML("<b>One</b>");
		testField.createHTML("<i>Two</i>");
		testField.createHTML("<u>Three</u>");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getHTMLList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
	}

}
