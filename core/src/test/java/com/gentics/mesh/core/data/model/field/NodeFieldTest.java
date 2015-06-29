package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.model.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.model.node.impl.MeshNodeImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class NodeFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleNodeField() {

		MeshNode node = fg.addFramedVertexExplicit(MeshNodeImpl.class);

		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		NodeField field = container.createNode("testNodeField", node);
		MeshNode loadedNode =field.getNode();
		assertNotNull(loadedNode);
		assertEquals(node.getUuid(), loadedNode.getUuid());
		
		NodeField loadedField = container.getNode("testNodeField");
		assertNotNull(loadedField);
		assertNotNull(loadedField.getNode());
		assertEquals(node.getUuid(), loadedField.getNode());

	}
}
