package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

@MeshTestSetting(testSize = FULL, startServer = false)
public class NodeGraphFieldContainerTest extends AbstractMeshTest {

	@Test(expected = ORecordDuplicatedException.class)
	public void testConflictingWebRootPath() {
		try (Tx tx = tx()) {
			Node node = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeImpl.class);
			HibNodeFieldContainer containerA = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			HibNodeFieldContainer containerB = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			GraphFieldContainerEdge n1 = node.addFramedEdge(HAS_FIELD_CONTAINER, (NodeGraphFieldContainer) containerA, GraphFieldContainerEdgeImpl.class);
			GraphFieldContainerEdge n2 = node.addFramedEdge(HAS_FIELD_CONTAINER, (NodeGraphFieldContainer) containerB, GraphFieldContainerEdgeImpl.class);
			n1.setSegmentInfo("test");
			n1.setBranchUuid("branch");
			n1.setType(DRAFT);

			n2.setSegmentInfo("test");
			n2.setBranchUuid("branch");
			n2.setType(DRAFT);
			tx.success();
		}
	}

	public void checkConsistency() {
		// don't run any checks
	}
}
