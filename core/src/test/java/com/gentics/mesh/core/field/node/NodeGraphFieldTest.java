package com.gentics.mesh.core.field.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class NodeGraphFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleNodeField() {

		Node node = fg.addFramedVertex(NodeImpl.class);

		NodeGraphFieldContainerImpl container = fg.addFramedVertex(NodeGraphFieldContainerImpl.class);
		GraphNodeField field = container.createNode("testNodeField", node);
		assertNotNull(field);
		assertEquals("testNodeField", field.getFieldKey());
		Node loadedNode = field.getNode();
		assertNotNull(loadedNode);
		assertEquals(node.getUuid(), loadedNode.getUuid());

		GraphNodeField loadedField = container.getNode("testNodeField");
		assertNotNull(loadedField);
		assertNotNull(loadedField.getNode());
		assertEquals(node.getUuid(), loadedField.getNode().getUuid());

	}
}
