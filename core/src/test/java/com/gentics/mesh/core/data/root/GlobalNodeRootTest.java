package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.FramedGraph;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = false)
public class GlobalNodeRootTest extends AbstractMeshTest {

	@Test
	public void testAddNode() {
		try (Tx tx = tx()) {
			FramedGraph graph = tx.getGraph();
			NodeImpl node = graph.addFramedVertex(NodeImpl.class);
			long start = project().getNodeRoot().computeCount();
			project().getNodeRoot().addItem(node);
			project().getNodeRoot().addItem(node);
			project().getNodeRoot().addItem(node);
			project().getNodeRoot().addItem(node);
			assertEquals(start + 1, project().getNodeRoot().computeCount());

			// Test the global root
			GlobalNodeRoot gr = boot().globalNodeRoot();
			assertEquals(start + 1, gr.computeCount());
			assertNotNull(gr.findByUuid(node.getUuid()));
			assertEquals(start + 1, gr.findAll().count());
		}
	}

}
