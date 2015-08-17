package com.gentics.mesh.core.field.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractDBTest;

public class NodeGraphFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleNodeField() {
		try (Trx tx = new Trx(database)) {
			Node node = tx.getGraph().addFramedVertex(NodeImpl.class);

			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
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
}
