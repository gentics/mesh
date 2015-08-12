package com.gentics.mesh.core.field.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class NodeFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleNodeField() {

		Node node = fg.addFramedVertex(NodeImpl.class);

		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
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
