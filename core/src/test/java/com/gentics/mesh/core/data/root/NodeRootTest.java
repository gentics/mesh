package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.FramedGraph;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = false)
public class NodeRootTest extends AbstractMeshTest {

	@Test
	public void testAddNode() {
		try (Tx tx = tx()) {
			FramedGraph graph = tx.getGraph();
			NodeImpl node = graph.addFramedVertex(NodeImpl.class);
			long start = boot().nodeRoot().computeCount();
			boot().nodeRoot().addItem(node);
			boot().nodeRoot().addItem(node);
			boot().nodeRoot().addItem(node);
			boot().nodeRoot().addItem(node);
			assertEquals(start + 1, boot().nodeRoot().computeCount());
		}
	}

}
