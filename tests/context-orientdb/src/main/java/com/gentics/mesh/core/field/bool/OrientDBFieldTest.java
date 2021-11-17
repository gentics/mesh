package com.gentics.mesh.core.field.bool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class OrientDBFieldTest extends AbstractMeshTest {

	@Test
	public void testSimpleBoolean() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			BooleanGraphFieldImpl field = new BooleanGraphFieldImpl("test", container);
			assertEquals(2, container.getPropertyKeys().size());
			assertNull(container.getProperty("test-boolean"));
			field.setBoolean(new Boolean(true));

			assertEquals("true", container.getProperty("test-boolean"));
			// assertEquals(5, container.getPropertyKeys().size());
			field.setBoolean(new Boolean(false));
			assertEquals("false", container.getProperty("test-boolean"));
			field.setBoolean(null);
			assertNull(container.getProperty("test-boolean"));
			assertNull(field.getBoolean());
		}
	}
}
