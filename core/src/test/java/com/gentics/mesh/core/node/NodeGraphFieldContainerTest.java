package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class NodeGraphFieldContainerTest extends AbstractMeshTest {

	@Test(expected = ORecordDuplicatedException.class)
	public void testConflictingWebRootPath() {
		try (Tx tx = tx()) {
			Node node = tx.getGraph().addFramedVertex(NodeImpl.class);
			NodeGraphFieldContainer containerA = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldContainer containerB = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			GraphFieldContainerEdge n1 = node.addFramedEdge(HAS_FIELD_CONTAINER, containerA, GraphFieldContainerEdgeImpl.class);
			GraphFieldContainerEdge n2 = node.addFramedEdge(HAS_FIELD_CONTAINER, containerB, GraphFieldContainerEdgeImpl.class);
			n1.setProperty(GraphFieldContainerEdge.WEBROOT_PROPERTY_KEY, "test");
			n2.setProperty(GraphFieldContainerEdge.WEBROOT_PROPERTY_KEY, "test");
			tx.success();
		}
	}

	@Test(expected = ORecordDuplicatedException.class)
	public void testConflictingPublishWebRootPath() {
		try (Tx tx = tx()) {
			Node node = tx.getGraph().addFramedVertex(NodeImpl.class);
			NodeGraphFieldContainer containerA = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldContainer containerB = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			GraphFieldContainerEdge n1 = node.addFramedEdge(HAS_FIELD_CONTAINER, containerA, GraphFieldContainerEdgeImpl.class);
			GraphFieldContainerEdge n2 = node.addFramedEdge(HAS_FIELD_CONTAINER, containerB, GraphFieldContainerEdgeImpl.class);
			n1.setProperty(GraphFieldContainerEdge.PUBLISHED_WEBROOT_PROPERTY_KEY, "test");
			n2.setProperty(GraphFieldContainerEdge.PUBLISHED_WEBROOT_PROPERTY_KEY, "test");
			tx.success();
		}
	}

	public void checkConsistency() {
		// don't run any checks
	}
}
