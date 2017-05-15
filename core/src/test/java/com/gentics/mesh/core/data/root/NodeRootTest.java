package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.FramedGraph;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT_AND_NODE, startServer = false)
public class NodeRootTest extends AbstractMeshTest {

	@Test
	public void testAddNode() {
		try (NoTx noTx = db().noTx()) {
			FramedGraph graph = Database.getThreadLocalGraph();
			NodeImpl node = graph.addFramedVertex(NodeImpl.class);
			int start = boot().nodeRoot().findAll().size();
			boot().nodeRoot().addItem(node);
			boot().nodeRoot().addItem(node);
			boot().nodeRoot().addItem(node);
			boot().nodeRoot().addItem(node);
			assertEquals(start + 1, boot().nodeRoot().findAll().size());
		}
	}

}
