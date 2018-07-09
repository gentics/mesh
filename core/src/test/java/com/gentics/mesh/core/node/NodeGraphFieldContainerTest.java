package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class NodeGraphFieldContainerTest extends AbstractMeshTest {

	@Test(expected = ORecordDuplicatedException.class)
	public void testConflictingWebRootPath() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer containerA = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldContainer containerB = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			containerA.getElement().property(NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY, "test");
			containerB.getElement().property(NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY, "test");
			tx.success();
		}
	}

	@Test(expected = ORecordDuplicatedException.class)
	public void testConflictingPublishWebRootPath() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer containerA = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldContainer containerB = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			containerA.getElement().property(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, "test");
			containerB.getElement().property(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, "test");
			tx.success();
		}
	}
}
